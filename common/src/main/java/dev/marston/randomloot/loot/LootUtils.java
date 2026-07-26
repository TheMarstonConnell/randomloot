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
import dev.marston.randomloot.loot.modifiers.StatsModifier;
import dev.marston.randomloot.platform.Services;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;

public class LootUtils {

	public record TextureProperty() implements RangeSelectItemModelProperty {
		public static final MapCodec<TextureProperty> MAP_CODEC = MapCodec.unit(new TextureProperty());

		@Override
		public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
			if (level != null && LootUtils.isRolling(stack)) {
				long ticksLeft = LootUtils.getRollEnd(stack) - level.getGameTime();
				if (ticksLeft > 0) {
					return LootUtils.rollingTexture(stack, ticksLeft, seed);
				}
			}
			return LootUtils.getTexture(stack);
		}

		@Override
		public MapCodec<? extends RangeSelectItemModelProperty> type() {
			return MAP_CODEC;
		}
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
		// No identity yet: without EQUIPPABLE, rolling armor can't be worn either.
		stack.remove(DataComponents.EQUIPPABLE);
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
		return GearTags.read(stack, GearTags.ROLLING).getLongOr("endTime", 0L);
	}

	/** Reveals the finished item: restores the stashed name and the worn-armor component. */
	public static void finishRoll(ItemStack stack) {
		CompoundTag tag = GearTags.read(stack, GearTags.ROLLING);
		GearTags.clear(stack, GearTags.ROLLING);

		if (tag.contains("name")) {
			setItemName(stack, tag.getStringOr("name", ""), tag.getStringOr("nameColor", "#FFFFFF"));
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

		state.getBlock().playerDestroy(level, player, pos, state, null, itemstack);
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
		return GearTags.read(stack, GearTags.LORE).getStringOr("itemLore", "");
	}

	public static int getMaxXP(int level) {
		return GearStats.maxXp(level);
	}

	/**
	 * The combined multiplier the stack's StatsModifier traits apply to dig speed and
	 * defense. getModifiers already filters out config-disabled traits.
	 */
	public static float statMultiplier(ItemStack stack) {
		float statMod = 1.0f;
		for (Modifier mod : getModifiers(stack)) {
			if (mod instanceof StatsModifier sm) {
				statMod *= sm.getStats(stack);
			}
		}
		return statMod;
	}

	public static ItemStack levelUp(ItemStack item, LivingEntity holder) {

		float stats = GearStats.leveledGoodness(getStats(item));
		setStats(item, stats);

		holder.level().playSound(null, holder.getX(), holder.getY(), holder.getZ(), SoundEvents.PLAYER_LEVELUP,
				holder.getSoundSource(), 1.0f, 1.0f);

		return item;
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
		return GearTags.read(stack, Modifier.MODTAG).keySet();
	}

	public static void addXp(ItemStack item, LivingEntity holder, int amount) {

		CompoundTag tag = GearTags.read(item, GearTags.XP);

		int level = tag.getIntOr("level", 0);

		int xp = tag.getIntOr("xp", 0);

		xp += amount;

		int max = getMaxXP(level);

		boolean leveled = false;
		while (xp >= max) {
			xp = xp - max;
			level++;
			levelUp(item, holder);
			leveled = true;
			max = getMaxXP(level);
		}

		if (leveled) {
			ModCriteria.toolLeveled(holder, level);
		}

		setLevelAndXP(item, level, xp);
	}

	public static int getLevel(ItemStack item) {
		return GearTags.read(item, GearTags.XP).getIntOr("level", 0);
	}

	public static int getXP(ItemStack item) {
		return GearTags.read(item, GearTags.XP).getIntOr("xp", 0);
	}

	public static ItemStack setLevelAndXP(ItemStack item, int level, int xp) {
		GearTags.mutate(item, GearTags.XP, tag -> {
			tag.putInt("level", level);
			tag.putInt("xp", xp);
		});

		return item;
	}

	public static List<Modifier> getModifiers(ItemStack item) {

		ArrayList<Modifier> tags = new ArrayList<Modifier>();

		CompoundTag modifiers = GearTags.read(item, Modifier.MODTAG);

        Set<String> mods = modifiers.keySet();

        for (String string : mods) {
            CompoundTag modTag = modifiers.getCompoundOrEmpty(string);

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

	public static void addModifier(ItemStack item, Modifier mod) {

		CompoundTag existing = GearTags.read(item, Modifier.MODTAG);

		if (!existing.contains(mod.tagName())) { // tag doesn't exist so we're adding a new trait.
			GearTags.mutate(item, Modifier.MODTAG, modifiers -> modifiers.put(mod.tagName(), mod.toNBT()));
			return;
		}

		Modifier oldModifier = mod.fromNBT(existing.getCompoundOrEmpty(mod.tagName()));

		if (!oldModifier.canLevel()) {
			return;
		}

		oldModifier.levelUp();
		GearTags.mutate(item, Modifier.MODTAG,
				modifiers -> modifiers.put(oldModifier.tagName(), oldModifier.toNBT()));
	}

	public static void updateModifier(ItemStack item, Modifier mod) {

		if (!hasRawTrait(item, mod.tagName())) { // not on the gear, nothing to update
			return;
		}

		GearTags.mutate(item, Modifier.MODTAG, modifiers -> modifiers.put(mod.tagName(), mod.toNBT()));
	}

	public static ItemStack removeModifier(ItemStack item, Modifier mod) {
		GearTags.mutate(item, Modifier.MODTAG, modifiers -> modifiers.remove(mod.tagName()));

		return item;
	}

	public static void setStats(ItemStack stack, float goodness) {
		GearTags.mutate(stack, GearTags.STATS, tag -> tag.putFloat("goodness", goodness));
	}

	public static float getStats(ItemStack stack) {
		return GearTags.read(stack, GearTags.STATS).getFloatOr("goodness", 0f);
	}

	public static void setBiomeTemperature(ItemStack stack, float temperature) {
		GearTags.mutate(stack, GearTags.INFO, tag -> tag.putFloat("biomeTemp", temperature));
	}

	public static float getBiomeTemperature(ItemStack stack) {
		return GearTags.read(stack, GearTags.INFO).getFloatOr("biomeTemp", 0.7f);
	}

	public static void setBiomeKey(ItemStack stack, String biomeKey) {
		GearTags.mutate(stack, GearTags.INFO, tag -> tag.putString("biomeKey", biomeKey));
	}

	public static String getBiomeKey(ItemStack stack) {
		return GearTags.read(stack, GearTags.INFO).getStringOr("biomeKey", "");
	}

	public static void setDimension(ItemStack stack, String dimension) {
		GearTags.mutate(stack, GearTags.INFO, tag -> tag.putString("dimension", dimension));
	}

	public static String getDimension(ItemStack stack) {
		return GearTags.read(stack, GearTags.INFO).getStringOr("dimension", "minecraft:overworld");
	}

	public static void setOwnerUUID(ItemStack stack, String uuid) {
		GearTags.mutate(stack, GearTags.INFO, tag -> tag.putString("ownerUUID", uuid));
	}

	public static String getOwnerUUID(ItemStack stack) {
		return GearTags.read(stack, GearTags.INFO).getStringOr("ownerUUID", "");
	}

	public static void setOwnerName(ItemStack stack, String name) {
		GearTags.mutate(stack, GearTags.INFO, tag -> tag.putString("ownerName", name));
	}

	public static String getOwnerName(ItemStack stack) {
		return GearTags.read(stack, GearTags.INFO).getStringOr("ownerName", "");
	}

	public static void setTexture(ItemStack stack, int texture) {
		GearTags.mutate(stack, GearTags.COSMETICS, tag -> tag.putInt("texture", texture));

		// Worn armor looks come from the EQUIPPABLE asset id, which tracks the texture index.
		updateEquippable(stack);
	}

	/**
	 * Rebuilds the per-stack EQUIPPABLE component for armor pieces so the equipment
	 * slot and worn texture (equipment asset {@code randomloot:setN}) match the piece
	 * type and cosmetic texture index. No-op for hand tools.
	 */
	public static void updateEquippable(ItemStack stack) {
		ToolType type = getToolType(stack);
		if (!type.isArmor()) {
			return;
		}

		// Rolling gear has no identity yet; finishRoll() re-runs this at the reveal.
		if (isRolling(stack)) {
			return;
		}

		int set = (getTextureIndex(stack) % ARMOR_SET_COUNT) + 1;
		ResourceKey<EquipmentAsset> asset = ResourceKey.create(EquipmentAssets.ROOT_ID,
				Identifier.fromNamespaceAndPath(RandomLoot.MODID, "set" + set));

		stack.set(DataComponents.EQUIPPABLE, Equippable.builder(type.armorSlot()).setAsset(asset).build());
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
		return GearTags.read(stack, GearTags.COSMETICS).getIntOr("texture", 0);
	}

	private static void storeBiomeData(ItemStack lootItem, Level level, @Nullable BlockPos pos) {
		if (pos == null) {
			setBiomeTemperature(lootItem, 0.7f);
			return;
		}

		Holder<Biome> biomeHolder = level.getBiome(pos);
		setBiomeTemperature(lootItem, biomeHolder.value().getBaseTemperature());

		// Store biome key for modifier filtering
		String biomeKey = "unknown";
		if (biomeHolder.unwrapKey().isPresent()) {
			biomeKey = biomeHolder.unwrapKey().get().identifier().toString();
		} else {
			// Fallback: search registry for this biome
			Registry<Biome> biomeRegistry = level.registryAccess().lookupOrThrow(Registries.BIOME);
			for (var entry : biomeRegistry.entrySet()) {
				if (entry.getValue() == biomeHolder.value()) {
					biomeKey = entry.getKey().identifier().toString();
					break;
				}
			}
		}

		setBiomeKey(lootItem, biomeKey);
		setDimension(lootItem, level.dimension().identifier().toString());
	}

	private static String biomeKeyToReadableName(String biomeKey) {
		if (biomeKey == null || biomeKey.isEmpty() || biomeKey.equals("unknown")) {
			return "an Unknown Biome";
		}

		// Remove namespace (minecraft:desert -> desert)
		String biomeName = biomeKey.contains(":") ? biomeKey.substring(biomeKey.indexOf(":") + 1) : biomeKey;

		// Replace underscores with spaces and capitalize each word
		String[] words = biomeName.split("_");
		StringBuilder result = new StringBuilder();
		for (String word : words) {
			if (word.isEmpty()) continue;  // Skip empty strings from split
			if (result.length() > 0) result.append(" ");
			result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}

		return "a " + result.toString();
	}

	private static void generateLore(ItemStack lootItem, Level level, Player player) {
		String nameColor = "#50ab4b";
		float temp = getBiomeTemperature(lootItem); // Get stored temperature

		if (level.dimension().equals(Level.NETHER)) {
			nameColor = "#FF8C19";
		} else if (level.dimension().equals(Level.END)) {
			nameColor = "#C419FF";
		}

		LootUtils.setItemName(lootItem, NameGenerator.generateNameWPrefix(level.getRandom(), temp, level.isRaining()), nameColor);

		// Forgers are world-constant per temperature band, not rolled per item.
		long worldSeed = level instanceof ServerLevel serverLevel ? serverLevel.getSeed() : 0L;
		String forger = NameGenerator.forgerForWorld(worldSeed, temp);

		String name = "a machine";
		if (player != null) {
			name = player.getDisplayName().getString();
		}

		// Get biome name for lore
		String biomeKey = getBiomeKey(lootItem);
		String biomeName = biomeKeyToReadableName(biomeKey);

		String loreText = "Discovered by " + name + " in " + biomeName + ", forged by " + forger + ".";

		LootUtils.setItemLore(lootItem, loreText);

	}

	public static ToolType getToolType(ItemStack item) {
		String type = GearTags.read(item, GearTags.INFO).getStringOr("type", "");
		if (type.isEmpty()) {
			return ToolType.NULL;
		}
		return ToolType.valueOf(type);
	}

	public static ItemStack setToolType(ItemStack item, ToolType type) {
		GearTags.mutate(item, GearTags.INFO, tag -> tag.putString("type", type.name()));
		return item;
	}

	/** Rolls one eligible trait onto the gear, or does nothing when none are eligible. */
	public static void generateNewTrait(ItemStack stack, RandomSource random, long worldSeed) {

		// One context for the whole sweep: the gear doesn't change while we pick.
		TraitEligibility.GearContext ctx = TraitEligibility.contextOf(stack);

		ArrayList<Modifier> allowedMods = new ArrayList<Modifier>();

		for (Entry<String, Modifier> entry : ModifierRegistry.getModifiers().entrySet()) {
			Modifier newMod = entry.getValue();

			if (TraitEligibility.check(ctx, newMod).allowed()) {
				allowedMods.add(newMod);
			}

		}

		int size = allowedMods.size();

		if (size == 0) {
			return;
		}

		int choice = random.nextInt(size);

		Modifier m = allowedMods.get(choice).forWorld(worldSeed);

		addModifier(stack, m);

	}

	public static void generateInitialTraits(ItemStack stack, int count, RandomSource random, long worldSeed) {
		for (int i = 0; i < count; i++) {
			generateNewTrait(stack, random, worldSeed);
		}
	}

	/**
	 * Re-resolves world-dependent traits (see {@link Modifier#forWorld}) against this
	 * world. Called when gear leaves a smithing table, where the recipe itself has no
	 * world access.
	 */
	public static void applyWorldForgers(ItemStack stack, long worldSeed) {
		for (Modifier mod : getModifiers(stack)) {
			Modifier resolved = mod.forWorld(worldSeed);
			if (resolved != mod) {
				updateModifier(stack, resolved);
			}
		}
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

	public static ItemStack genTool(Player player, Level level) {
		return genTool(player, level, player != null ? player.blockPosition() : null);
	}

	public static ItemStack genTool(Player player, Level level, @Nullable BlockPos pos) {

		/**
		 * We want to be able to make the loot get better over time for players. This is
		 * pretty trivial with the statistics of a player since we can just check how
		 * many cases they've opened. The more cases they've opened, the better their
		 * tools will be. We do this on a SQRT curve to ensure that tools get better
		 * over time without getting out of hand. Since the tools level pretty closely
		 * to this curve we can expect that users won't constantly feel like they should
		 * use the same tool since it's already leveled up to be better than what these
		 * new tools start at. But at the same time, players won't feel sad abandoning
		 * their old tools in favor of these new ones since they're comparable AND these
		 * new ones have a clean XP slate so they can level faster again.
		 */
		if (level.isClientSide()) {
			return ItemStack.EMPTY;
		}

		float goodness;
		if (player != null) {
			ServerPlayer sPlayer = (ServerPlayer) player;
			StatType<Item> itemUsed = Stats.ITEM_USED;
			int count = sPlayer.getStats().getValue(itemUsed.get(ModItems.CASE.get()));
			goodness = GearStats.goodnessForCases(count, Config.Goodness);
		} else {
			goodness = machineGoodness(level);
		}

		ToolType m;
		if (level.getRandom().nextFloat() < Config.ArmorChance) {
			int armorType = level.getRandom().nextInt(4);
			m = switch (armorType) {
			case 0: {
				yield ToolType.HELMET;
			}
			case 1: {
				yield ToolType.CHESTPLATE;
			}
			case 2: {
				yield ToolType.LEGGINGS;
			}
			default: {
				yield ToolType.BOOTS;
			}
			};
		} else {
			int toolType = level.getRandom().nextInt(4);
			m = switch (toolType) {
			case 0: {
				yield ToolType.PICKAXE;
			}
			case 1: {
				yield ToolType.AXE;
			}
			case 2: {
				yield ToolType.SHOVEL;
			}
			default: {
				yield ToolType.SWORD;
			}
			};
		}

		return buildTool(player, level, pos, m, goodness);
	}

	/**
	 * Builds a tool/armor piece of the given type and goodness: stats, biome + owner
	 * data, name/lore, random texture, and {@code floor(goodness / 2)} starting traits.
	 */
	public static ItemStack buildTool(@Nullable Player player, Level level, @Nullable BlockPos pos, ToolType type,
			float goodness) {

		if (level.isClientSide()) {
			return ItemStack.EMPTY;
		}

		int traits = GearStats.startingTraits(goodness);

		ItemStack lootItem = new ItemStack(type.isArmor() ? ModItems.ARMOR.get() : ModItems.TOOL.get());

		LootUtils.setStats(lootItem, goodness);

		lootItem = setToolType(lootItem, type);

		int textureCount = getToolMaxTextures(lootItem);

		// Store biome data BEFORE generating traits (so biome-restricted modifiers work)
		storeBiomeData(lootItem, level, pos);

		// Store owner data for Soulbound modifier
		if (player != null) {
			setOwnerUUID(lootItem, player.getStringUUID());
			setOwnerName(lootItem, player.getDisplayName().getString());
		}

		long worldSeed = level instanceof ServerLevel serverLevel ? serverLevel.getSeed() : 0L;
		generateInitialTraits(lootItem, traits, level.getRandom(), worldSeed);

		generateLore(lootItem, level, player);

		if (textureCount > 0) {
			LootUtils.setTexture(lootItem, level.getRandom().nextInt(textureCount));
		}

		return lootItem;
	}

	/**
	 * Goodness for caseless openers (dispensers). There is no opener stat to read,
	 * so ride the curve of the most progressed online player, scaled down by
	 * config. Deliberately awards no stats: dispensers must not farm progression.
	 */
	private static float machineGoodness(Level level) {
		int best = 0;
		MinecraftServer server = level.getServer();
		if (server != null) {
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				best = Math.max(best, p.getStats().getValue(Stats.ITEM_USED.get(ModItems.CASE.get())));
			}
		}
		return GearStats.goodnessForCases(best, Config.Goodness * Config.DispenserGoodness);
	}

	/**
	 * Generates a tool for the player and fires the case-opened criteria. The
	 * caller decides where the item goes (hand slot, inventory, ...).
	 */
	public static ItemStack generateTool(ServerPlayer player, Level level) {

		ItemStack lootItem = genTool(player, level);

		// The ITEM_USED stat for the case is awarded after this runs, so +1
		// counts the case being opened right now.
		int casesOpened = player.getStats().getValue(Stats.ITEM_USED.get(ModItems.CASE.get())) + 1;
		ModCriteria.caseOpened(player, casesOpened, getToolType(lootItem));
		ModCriteria.traitsObtained(player, lootItem, TraitObtainedTrigger.SOURCE_GENERATED);

		return lootItem;
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
