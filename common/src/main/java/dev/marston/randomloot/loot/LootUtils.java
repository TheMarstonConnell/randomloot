package dev.marston.randomloot.loot;

import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import dev.marston.randomloot.Config;
import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.advancements.ModCriteria;
import dev.marston.randomloot.advancements.TraitObtainedTrigger;
import dev.marston.randomloot.component.ModDataComponents;
import dev.marston.randomloot.component.ToolModifier;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import dev.marston.randomloot.platform.Services;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class LootUtils {

	/**
	 * The {@code randomloot:cosmetic} item-model property value (1.21.1 uses classic
	 * {@code ItemProperties} predicates + model {@code overrides}; each loader's client
	 * init registers a lambda that calls this).
	 */
	public static float modelTexture(ItemStack stack, @Nullable ClientLevel level, int seed) {
		if (level != null && LootUtils.isRolling(stack)) {
			long ticksLeft = LootUtils.getRollEnd(stack) - level.getGameTime();
			if (ticksLeft > 0) {
				return LootUtils.rollingTexture(stack, ticksLeft, seed);
			}
		}
		return LootUtils.getTexture(stack);
	}

	private static int PICKAXE_COUNT = 22;
	private static int AXE_COUNT = 18;
	private static int SHOVEL_COUNT = 12;
	private static int SWORD_COUNT = 53;
	/** Number of armor texture sets; each set is one helmet/chestplate/leggings/boots look. */
	public static final int ARMOR_SET_COUNT = 19;

	/* ---- Case-opening roll ----
	 * Freshly opened gear starts "rolling": the name, traits, stats and the worn-armor
	 * component are withheld while the client spins the item's texture through every
	 * variant, slot-machine style. The final item is fully decided at open time - the
	 * roll only delays the reveal. State lives in the ROLLING tag: the reveal game-time
	 * plus the stashed display name.
	 */

	/** How long a freshly opened item rolls before revealing itself. */
	public static final int ROLL_TICKS = 60;
	// Steps remaining with t ticks left = t^2 / ROLL_CURVE: one texture per tick at the
	// start of a 60-tick roll, decelerating to ~8 ticks on the final step.
	private static final long ROLL_CURVE = 60;

	/** Every rollable type in wheel order; rolling items spin through all of them. */
	private static final ToolType[] ROLL_WHEEL = { ToolType.PICKAXE, ToolType.SHOVEL, ToolType.AXE, ToolType.SWORD,
			ToolType.HELMET, ToolType.CHESTPLATE, ToolType.LEGGINGS, ToolType.BOOTS };

	/** Puts finished gear into the rolling state until {@code revealTime} (game time). */
	public static void startRoll(ItemStack stack, long revealTime) {
		CompoundTag tag = new CompoundTag();
		tag.putLong("endTime", revealTime);

		Component name = stack.get(DataComponents.CUSTOM_NAME);
		if (name != null) {
			tag.putString("name", name.getString());
			TextColor color = name.getStyle().getColor();
			tag.putString("nameColor", color != null ? color.serialize() : "#FFFFFF");
			stack.remove(DataComponents.CUSTOM_NAME);
		}

		GearTags.write(stack, GearTags.ROLLING, tag);
		// No identity yet: wearableSlot() returns null while rolling, so rolling
		// armor can't be worn either.
		// Rolling gear shows no attributes until the reveal.
		refreshDerivedComponents(stack);
	}

	public static boolean isRolling(ItemStack stack) {
		return GearTags.has(stack, GearTags.ROLLING);
	}

	/** True for the mod's gear items (tools and armor). */
	public static boolean isLootGear(ItemStack stack) {
		return stack.getItem() instanceof LootGearItem;
	}

	/**
	 * The mod's per-type/per-piece enchant gate for either gear item, or null when
	 * the stack isn't loot gear / the enchant isn't in a randomloot group tag -
	 * callers fall back to the loader's default check. Single dispatch point for
	 * Fabric's ALLOW_ENCHANTING event and NeoForge's supportsEnchantment overrides.
	 */
	@Nullable
	public static Boolean gearEnchantGate(ItemStack stack, Holder<Enchantment> enchantment) {
		if (stack.getItem() instanceof LootGearItem gear) {
			return gear.supportsEnchantmentCommon(stack, enchantment);
		}
		return null;
	}

	/** Game time at which a rolling item settles; 0 when not rolling. */
	public static long getRollEnd(ItemStack stack) {
		return NbtCompat.getLongOr(GearTags.read(stack, GearTags.ROLLING), "endTime", 0L);
	}

	/** Reveals the finished item: restores the stashed name and the worn-armor component. */
	public static void finishRoll(ItemStack stack) {
		CompoundTag tag = GearTags.read(stack, GearTags.ROLLING);
		GearTags.clear(stack, GearTags.ROLLING);

		if (tag.contains("name")) {
			setItemName(stack, NbtCompat.getStringOr(tag, "name", ""), NbtCompat.getStringOr(tag, "nameColor", "#FFFFFF"));
		}
		updateEquippable(stack);
		refreshDerivedComponents(stack);
	}

	/**
	 * Rebuilds the stack's derived vanilla components - attributes
	 * (attack/armor) and max damage - from its stats, type and traits. Called
	 * from every mutator that changes one of those inputs. Both loaders read
	 * the plain components, replacing NeoForge's dynamic item-method
	 * extensions.
	 */
	public static void refreshDerivedComponents(ItemStack stack) {
		if (stack.getItem() instanceof LootGearItem gear) {
			stack.set(DataComponents.ATTRIBUTE_MODIFIERS, gear.buildAttributeModifiers(stack));
			stack.set(DataComponents.MAX_DAMAGE, LootGearItem.computeMaxDamage(stack));
		}
	}

	/**
	 * One-time migration for stacks from before attributes/durability lived in
	 * data components: if the stack has never had the components stamped on
	 * (no patch entry), derive them now. Called from inventoryTick.
	 */
	public static void migrateDerivedComponents(ItemStack stack) {
		// Plain loop: this runs from inventoryTick for every loot item, every tick.
		for (Map.Entry<DataComponentType<?>, Optional<?>> entry : stack.getComponentsPatch().entrySet()) {
			if (entry.getKey() == DataComponents.ATTRIBUTE_MODIFIERS) {
				return;
			}
		}
		refreshDerivedComponents(stack);
	}

	/**
	 * Server-side driver for rolling items, called from inventoryTick. Returns true
	 * while the roll is running (callers skip their normal tick), revealing the item
	 * with a sound once its time is up.
	 */
	public static boolean tickRoll(ItemStack stack, ServerLevel level, Entity holder) {
		if (!isRolling(stack)) {
			return false;
		}

		long ticksLeft = getRollEnd(stack) - level.getGameTime();
		if (ticksLeft <= 0) {
			finishRoll(stack);
			level.playSound(null, holder.getX(), holder.getY(), holder.getZ(), SoundEvents.PLAYER_LEVELUP,
					SoundSource.PLAYERS, 0.8f, 1.3f);
			return true;
		}

		// Click exactly when the client-side wheel advances a step (same curve).
		if (rollSteps(ticksLeft) != rollSteps(ticksLeft + 1)) {
			float pitch = 1.6f - 0.8f * ((float) ticksLeft / ROLL_TICKS);
			level.playSound(null, holder.getX(), holder.getY(), holder.getZ(), SoundEvents.ITEM_PICKUP,
					SoundSource.PLAYERS, 0.3f, pitch);
		}
		return true;
	}

	private static long rollSteps(long ticksLeft) {
		return (ticksLeft * ticksLeft) / ROLL_CURVE;
	}

	/**
	 * The texture shown while rolling: each step is a hash-scrambled jump across
	 * every variant of every type (tools flash armor looks and vice versa, which is
	 * why items/tool.json and items/armor.json carry identical entries), decelerating
	 * via {@link #rollSteps} and settling on the real texture for the final steps.
	 * A sequential walk would telegraph the outcome - the wheel would park on the
	 * final texture's same-type neighbors right before the reveal.
	 */
	public static float rollingTexture(ItemStack stack, long ticksLeft, int seed) {
		long step = rollSteps(ticksLeft);
		if (step <= 0) {
			return getTexture(stack);
		}

		int total = 0;
		for (ToolType t : ROLL_WHEEL) {
			total += textureCount(t);
		}

		// SplitMix64-style mix of the step and the stack's display seed, so the
		// sequence looks random but is stable within a tick and differs per stack.
		long h = step * 0x9E3779B97F4A7C15L + seed * 0xC2B2AE3D27D4EB4FL;
		h ^= h >>> 27;
		h *= 0x94D049BB133111EBL;
		h ^= h >>> 31;
		int shown = (int) Math.floorMod(h, total);

		for (ToolType t : ROLL_WHEEL) {
			int count = textureCount(t);
			if (shown < count) {
				return typeOffset(t) + shown / 10000.0f;
			}
			shown -= count;
		}
		return getTexture(stack); // unreachable: shown < total by construction
	}

	public static ItemStack CloneItem(ItemStack stack) {
		ItemStack copy = new ItemStack(stack.getItem());

		// Preserve the tool's durability so texture/trait changes don't repair it.
		copy.set(DataComponents.DAMAGE, stack.get(DataComponents.DAMAGE));

		@Nullable ToolModifier mods = stack.get(ModDataComponents.TOOL_MODIFIER.get());
		if (mods == null) {
			return copy;
		}

		// Build the new map first, then create ToolModifier with it
		Map<String, CompoundTag> newTags = new HashMap<>();
		Map<String, CompoundTag> tags = mods.getTags();
		tags.forEach((s, compoundTag) -> {
			newTags.put(s, compoundTag.copy());
		});

		ToolModifier copyMods = new ToolModifier(newTags);
		copy.set(ModDataComponents.TOOL_MODIFIER.get(), copyMods);

		copy.set(DataComponents.CUSTOM_NAME, stack.get(DataComponents.CUSTOM_NAME));

		// Smithing and Essence-retexturing must not strip table/anvil enchantments.
		copy.set(DataComponents.ENCHANTMENTS, stack.get(DataComponents.ENCHANTMENTS));
		copy.set(DataComponents.REPAIR_COST, stack.get(DataComponents.REPAIR_COST));

		// Armor carries its slot + worn-texture in the per-stack EQUIPPABLE component.
		updateEquippable(copy);

		// The copy's tags were set wholesale rather than through GearTags.mutate, so
		// stamp the derived components here. Without this the recipe output carried
		// default attributes until its first inventoryTick.
		refreshDerivedComponents(copy);

		return copy;
	}
	
	/**
	 * Breaks a block as if {@code player} had mined it (drops + block-break stats),
	 * but only when the player can actually harvest it. Returns {@code true} when the
	 * block was destroyed so callers only spend durability on blocks they really broke.
	 */
	public static boolean breakBlockAsPlayer(ItemStack itemstack, BlockPos pos, ServerPlayer player, Level level, BlockState state) {
		if (!Services.PLATFORM.canHarvestBlock(state, level, pos, player)) {
			return false;
		}

		// Pass the block entity like vanilla does: block loot that copies from it
		// (a shulker box's contents, a banner's pattern) drops empty without one.
		state.getBlock().playerDestroy(level, player, pos, state, level.getBlockEntity(pos), itemstack);
		level.removeBlock(pos, false);
		return true;
	}

	public static void addLoreLine(ListTag lore, String text, String color) {
		JsonObject value = new JsonObject();

		value.addProperty("text", text);
		value.addProperty("color", color);
		value.addProperty("italic", false);

		StringTag nbtName = StringTag.valueOf(value.toString());

		lore.add(nbtName);
	}

	public static void setItemName(ItemStack stack, String name, String color) {
		MutableComponent comp = Component.literal(name);

		Style style = Style.EMPTY;
		style = style.withItalic(false);
		DataResult<TextColor> col = TextColor.parseColor(color);
		style = style.withColor(col.getOrThrow());

		comp = comp.withStyle(style);

		stack.set(DataComponents.CUSTOM_NAME, comp);
	}

	public static void setItemLore(ItemStack stack, String lore) {
		GearTags.mutate(stack, GearTags.LORE, tag -> tag.putString("itemLore", lore));
	}

	public static String getItemLore(ItemStack stack) {
		return NbtCompat.getStringOr(GearTags.read(stack, GearTags.LORE), "itemLore", "");
	}

	public static int getMaxXP(int level) {
		return GearStats.maxXp(level);
	}

	/**
	 * One level-up's effects, in order: the goodness bump, the sound, and on the server
	 * the chance of a bonus trait with its chat line and criteria.
	 */
	private static void levelUp(ItemStack item, LivingEntity holder, double bonusTraitChance) {

		float stats = GearStats.leveledGoodness(getStats(item));
		setStats(item, stats);

		holder.level().playSound(null, holder.getX(), holder.getY(), holder.getZ(), SoundEvents.PLAYER_LEVELUP,
				holder.getSoundSource(), 1.0f, 1.0f);

		if (!(holder.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		Modifier bonus = GearRoll.bonusTrait(TraitEligibility.contextOf(item), bonusTraitChance, holder.getRandom());
		if (bonus == null) {
			return;
		}

		TraitGrant.Outcome rolled = TraitGrant.grant(item, bonus, serverLevel.getSeed());
		if (holder instanceof ServerPlayer player) {
			String key = rolled.kind() == TraitGrant.Kind.LEVELED
					? "message.randomloot.level_up_trait_leveled"
					: "message.randomloot.level_up_trait";
			Component trait = rolled.trait().displayName().copy().withStyle(rolled.trait().color());
			player.sendSystemMessage(Component.translatable(key, item.getHoverName(), trait));

			rolled.obtainedBy(player, TraitObtainedTrigger.SOURCE_LEVELED);
		}
	}

	/**
	 * Cheap "does this stack carry an enabled trait" check - a key lookup on the raw
	 * component, without deserializing the whole modifier list like getModifiers does.
	 */
	public static boolean hasEnabledModifier(ItemStack stack, String tagName) {
		if (!Config.traitEnabled(tagName)) {
			return false;
		}
		return hasRawTrait(stack, tagName);
	}

	/**
	 * Whether the trait is written on the gear at all, ignoring the config toggle -
	 * config-disabled traits are still on the item and still removable.
	 */
	public static boolean hasRawTrait(ItemStack stack, String tagName) {
		return GearTags.read(stack, Modifier.MODTAG).contains(tagName);
	}

	/** Every trait written on the gear, including config-disabled ones. */
	public static Set<String> rawTraitNames(ItemStack stack) {
		return GearTags.read(stack, Modifier.MODTAG).getAllKeys();
	}

	/**
	 * Gives the gear XP. {@link GearStats#xpStep} decides where that leaves it; each level
	 * gained is then applied as a level-up, in order, before the new level is written.
	 */
	public static void addXp(ItemStack item, LivingEntity holder, int amount) {
		GearStats.XpStep step = GearStats.xpStep(getLevel(item), getXP(item), amount);

		double bonusTraitChance = Config.LEVEL_UP_TRAIT_CHANCE.get();
		for (int i = 0; i < step.levelsGained(); i++) {
			levelUp(item, holder, bonusTraitChance);
		}

		if (step.levelsGained() > 0) {
			ModCriteria.toolLeveled(holder, step.level());
		}

		setLevelAndXP(item, step.level(), step.xp());
	}

	public static int getLevel(ItemStack item) {
		return NbtCompat.getIntOr(GearTags.read(item, GearTags.XP), "level", 0);
	}

	public static int getXP(ItemStack item) {
		return NbtCompat.getIntOr(GearTags.read(item, GearTags.XP), "xp", 0);
	}

	public static ItemStack setLevelAndXP(ItemStack item, int level, int xp) {
		GearTags.mutate(item, GearTags.XP, tag -> {
			tag.putInt("level", level);
			tag.putInt("xp", xp);
		});

		return item;
	}

	public static List<Modifier> getModifiers(ItemStack item) {
		return getModifiers(GearTags.read(item, Modifier.MODTAG));
	}

	/**
	 * The enabled traits a tag of stored traits holds: what {@link Modifier#MODTAG} is
	 * on a stack, or what a {@link GearRoll} has decided so far.
	 */
	public static List<Modifier> getModifiers(CompoundTag modifiers) {

		ArrayList<Modifier> tags = new ArrayList<Modifier>();

        Set<String> mods = modifiers.getAllKeys();

        for (String string : mods) {
            CompoundTag modTag = NbtCompat.getCompoundOrEmpty(modifiers, string);

            Modifier finalModifier = ModifierRegistry.loadModifier(string, modTag);
            if (finalModifier == null) {
                continue;
            }
			if (!Config.traitEnabled(finalModifier.tagName())) {
				continue;
			}

            tags.add(finalModifier);

        }

		return tags;
	}

	/**
	 * Rewrites a trait the gear already carries, so a trait can save its own state.
	 * Putting a trait on or taking one off is {@link TraitGrant}'s.
	 */
	public static void updateModifier(ItemStack item, Modifier mod) {

		if (!hasRawTrait(item, mod.tagName())) { // not on the gear, nothing to update
			return;
		}

		GearTags.mutate(item, Modifier.MODTAG, modifiers -> modifiers.put(mod.tagName(), mod.toNBT()));
	}

	public static void setStats(ItemStack stack, float goodness) {
		GearTags.mutate(stack, GearTags.STATS, tag -> tag.putFloat("goodness", goodness));
	}

	public static float getStats(ItemStack stack) {
		return NbtCompat.getFloatOr(GearTags.read(stack, GearTags.STATS), "goodness", 0f);
	}

	public static float getBiomeTemperature(ItemStack stack) {
		return NbtCompat.getFloatOr(GearTags.read(stack, GearTags.INFO), "biomeTemp", 0.7f);
	}

	public static String getBiomeKey(ItemStack stack) {
		return NbtCompat.getStringOr(GearTags.read(stack, GearTags.INFO), "biomeKey", "");
	}

	public static String getDimension(ItemStack stack) {
		return NbtCompat.getStringOr(GearTags.read(stack, GearTags.INFO), "dimension", "minecraft:overworld");
	}

	public static void setOwnerUUID(ItemStack stack, String uuid) {
		GearTags.mutate(stack, GearTags.INFO, tag -> tag.putString("ownerUUID", uuid));
	}

	public static String getOwnerUUID(ItemStack stack) {
		return NbtCompat.getStringOr(GearTags.read(stack, GearTags.INFO), "ownerUUID", "");
	}

	public static String getOwnerName(ItemStack stack) {
		return NbtCompat.getStringOr(GearTags.read(stack, GearTags.INFO), "ownerName", "");
	}

	public static void setTexture(ItemStack stack, int texture) {
		GearTags.mutate(stack, GearTags.COSMETICS, tag -> tag.putInt("texture", texture));

		// Worn armor looks come from the EQUIPPABLE asset id, which tracks the texture index.
		updateEquippable(stack);
	}

	/**
	 * The slot this stack is worn in, or {@code null} when it can't be worn (hand
	 * tools, and armor that is still rolling). 1.21.1 has no per-stack EQUIPPABLE
	 * component, so both loaders' equipment-slot hooks and the worn-armor renderers
	 * derive the slot (and texture set, via {@link #wornSetIndex}) on demand.
	 */
	@Nullable
	public static EquipmentSlot wearableSlot(ItemStack stack) {
		ToolType type = getToolType(stack);
		if (!type.isArmor() || isRolling(stack)) {
			return null;
		}
		return type.armorSlot();
	}

	/** The 1-based worn-texture set index for armor, tracking the cosmetic texture index. */
	public static int wornSetIndex(ItemStack stack) {
		return (getTextureIndex(stack) % ARMOR_SET_COUNT) + 1;
	}

	/**
	 * 26.x rebuilt the per-stack EQUIPPABLE component here; on 1.21.1 the slot and
	 * worn look are derived on demand instead (see {@link #wearableSlot}), so this
	 * is a no-op kept so mutators stay line-for-line portable across branches.
	 */
	public static void updateEquippable(ItemStack stack) {
	}

	public static float getTexture(ItemStack stack) {
		int texture = getTextureIndex(stack);

		return typeOffset(getToolType(stack)) + ((float) texture) / 10000.0f;
	}

	/** range_dispatch offset per type; must match the entries in items/tool.json + items/armor.json. */
	public static float typeOffset(ToolType type) {
		return switch (type) {
		case PICKAXE -> 0.1f;
		case SHOVEL -> 0.2f;
		case AXE -> 0.3f;
		case SWORD -> 0.4f;
		case HELMET -> 0.5f;
		case CHESTPLATE -> 0.6f;
		case LEGGINGS -> 0.7f;
		case BOOTS -> 0.8f;
		default -> 0.0f;
		};
	}

	/** How many texture variants exist for the given type. */
	public static int textureCount(ToolType type) {
		return switch (type) {
		case PICKAXE -> PICKAXE_COUNT;
		case AXE -> AXE_COUNT;
		case SHOVEL -> SHOVEL_COUNT;
		case SWORD -> SWORD_COUNT;
		case HELMET, CHESTPLATE, LEGGINGS, BOOTS -> ARMOR_SET_COUNT;
		default -> 0;
		};
	}

	public static int getTextureIndex(ItemStack stack) {
		return NbtCompat.getIntOr(GearTags.read(stack, GearTags.COSMETICS), "texture", 0);
	}

	public static ToolType getToolType(ItemStack item) {
		String type = NbtCompat.getStringOr(GearTags.read(item, GearTags.INFO), "type", "");
		if (type.isEmpty()) {
			return ToolType.NULL;
		}
		return ToolType.valueOf(type);
	}

	public static ItemStack setToolType(ItemStack item, ToolType type) {
		GearTags.mutate(item, GearTags.INFO, tag -> tag.putString("type", type.name()));
		return item;
	}

	public static int getToolMaxTextures(ItemStack stack) {
		return textureCount(getToolType(stack));
	}

	public static int addToolTextures(ItemStack stack, int count) {
		int max = getToolMaxTextures(stack);

		if (max == 0) {
			return 0;
		}

		int current = getTextureIndex(stack);

		int newTexture = (current + count) % max;

		return newTexture;

	}

	public static void addTexture(ItemStack stack, int count) {
		setTexture(stack, addToolTextures(stack, count));
	}

	/**
	 * A player opens a case: gear rolled at their progression, with the case criteria
	 * fired. The caller decides where the item goes (hand slot, inventory, ...).
	 *
	 * <p>We want the loot to get better over time for players, and the opener's
	 * cases-opened stat is what says how far along they are. Goodness rides a square-root
	 * curve of it ({@link GearStats#goodnessForCases}): gear keeps improving without
	 * getting out of hand, a new piece is comparable to the leveled one the player already
	 * carries, and it comes with a clean XP slate so it levels faster again.
	 */
	public static ItemStack openCase(ServerPlayer player) {
		// The case's use stat is awarded after this runs, so this is the cases opened
		// before the one in hand. The curve counts that one itself; the criteria wants
		// it counted, hence the + 1 below.
		int openedBefore = player.getStats().getValue(Stats.ITEM_USED.get(ModItems.CASE.get()));
		float goodness = GearStats.goodnessForCases(openedBefore, Config.GOODNESS.get());

		ItemStack gear = rollAt(player.level(), player.blockPosition(), goodness, player);

		ModCriteria.caseOpened(player, openedBefore + 1, getToolType(gear));
		ModCriteria.traitsObtained(player, gear, TraitObtainedTrigger.SOURCE_GENERATED);

		return gear;
	}

	/**
	 * A machine (a dispenser) opens a case. There is no opener stat to read, so the gear
	 * rides the curve of the most progressed online player, scaled down by config.
	 * Deliberately awards no stats and fires no criteria: dispensers must not farm
	 * progression.
	 */
	public static ItemStack dispenseCase(ServerLevel level, BlockPos pos) {
		int best = 0;
		for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
			best = Math.max(best, p.getStats().getValue(Stats.ITEM_USED.get(ModItems.CASE.get())));
		}
		float goodness = GearStats.goodnessForCases(best, Config.GOODNESS.get() * Config.DISPENSER_GOODNESS.get());

		return rollAt(level, pos, goodness, null);
	}

	private static ItemStack rollAt(Level level, BlockPos pos, float goodness, @Nullable Player opener) {
		return stamp(GearRoll.roll(goodness, sceneAt(level, pos, opener), Config.ARMOR_CHANCE.get(), level.getRandom()));
	}

	/**
	 * Builds a tool/armor piece of the given type and goodness: stats, biome + owner
	 * data, name/lore, random texture, and {@code floor(goodness / 2)} starting traits.
	 * At goodness 0 that is base gear with no traits, which is what
	 * {@code /randomloot give} hands out.
	 */
	public static ItemStack buildTool(@Nullable Player player, Level level, BlockPos pos, ToolType type,
			float goodness) {

		if (level.isClientSide()) {
			return ItemStack.EMPTY;
		}

		return stamp(GearRoll.ofType(type, goodness, sceneAt(level, pos, player), level.getRandom()));
	}

	/** Reads what a {@link GearRoll} needs to know about where, and for whom, gear is being made. */
	private static GearRoll.Scene sceneAt(Level level, BlockPos pos, @Nullable Player opener) {
		Holder<Biome> biomeHolder = level.getBiome(pos);

		String biomeKey = "unknown";
		if (biomeHolder.unwrapKey().isPresent()) {
			biomeKey = biomeHolder.unwrapKey().get().location().toString();
		} else {
			// Fallback: search registry for this biome
			Registry<Biome> biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);
			for (var entry : biomeRegistry.entrySet()) {
				if (entry.getValue() == biomeHolder.value()) {
					biomeKey = entry.getKey().location().toString();
					break;
				}
			}
		}

		GearRoll.Place place = new GearRoll.Place(biomeKey, biomeHolder.value().getBaseTemperature(),
				level.dimension().location().toString());
		long worldSeed = level instanceof ServerLevel serverLevel ? serverLevel.getSeed() : 0L;
		GearRoll.Owner owner = opener == null ? null
				: new GearRoll.Owner(opener.getStringUUID(), opener.getDisplayName().getString());

		return new GearRoll.Scene(place, worldSeed, level.isRaining(), owner);
	}

	/**
	 * Makes the item a {@link GearRoll} decided on. This is the one place generated gear
	 * is written: every namespace goes on raw, and the vanilla components that derive
	 * from them are built once at the end instead of once per field.
	 */
	public static ItemStack stamp(GearRoll.Rolled rolled) {
		ToolType type = rolled.type();
		ItemStack gear = new ItemStack(type.isArmor() ? ModItems.ARMOR.get() : ModItems.TOOL.get());

		CompoundTag stats = new CompoundTag();
		stats.putFloat("goodness", rolled.goodness());
		GearTags.write(gear, GearTags.STATS, stats);

		CompoundTag info = new CompoundTag();
		info.putString("type", type.name());
		info.putFloat("biomeTemp", rolled.place().temperature());
		info.putString("biomeKey", rolled.place().biomeKey());
		info.putString("dimension", rolled.place().dimension());
		// Owner data is what Soulbound checks.
		if (rolled.owner() != null) {
			info.putString("ownerUUID", rolled.owner().uuid());
			info.putString("ownerName", rolled.owner().name());
		}
		GearTags.write(gear, GearTags.INFO, info);

		if (!rolled.traits().isEmpty()) {
			GearTags.write(gear, Modifier.MODTAG, rolled.traits().copy());
		}

		CompoundTag lore = new CompoundTag();
		lore.putString("itemLore", rolled.lore());
		GearTags.write(gear, GearTags.LORE, lore);

		CompoundTag cosmetics = new CompoundTag();
		cosmetics.putInt("texture", rolled.texture());
		GearTags.write(gear, GearTags.COSMETICS, cosmetics);

		setItemName(gear, rolled.name(), rolled.nameColor());

		// Worn armor's slot and look come from the type and texture just written.
		updateEquippable(gear);
		refreshDerivedComponents(gear);

		return gear;
	}

	private static final int[] ROMAN_VALUES = { 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };
	private static final String[] ROMAN_SYMBOLS = { "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV",
			"I" };

	public static String roman(int input) {
		if (input < 1 || input > 3999)
			return "Invalid Roman Number Value";
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < ROMAN_VALUES.length; i++) {
			while (input >= ROMAN_VALUES[i]) {
				s.append(ROMAN_SYMBOLS[i]);
				input -= ROMAN_VALUES[i];
			}
		}
		return s.toString();
	}

}
