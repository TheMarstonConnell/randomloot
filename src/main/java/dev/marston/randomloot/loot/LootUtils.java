package dev.marston.randomloot.loot;

import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import dev.marston.randomloot.Config;
import dev.marston.randomloot.component.ModDataComponents;
import dev.marston.randomloot.component.ToolModifier;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.BiomeRestrictedModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
			return LootUtils.getTexture(stack);
		}

		@Override
		public MapCodec<? extends RangeSelectItemModelProperty> type() {
			return MAP_CODEC;
		}
	}

	private static int PICKAXE_COUNT = 18;
	private static int AXE_COUNT = 14;
	private static int SHOVEL_COUNT = 9;
	private static int SWORD_COUNT = 50;

	public static ItemStack CloneItem(ItemStack stack) {
		ItemStack copy = new ItemStack(ModItems.TOOL.asItem());

		// Preserve the tool's durability so texture/trait changes don't repair it.
		copy.set(DataComponents.DAMAGE, stack.get(DataComponents.DAMAGE));

		@Nullable ToolModifier mods = stack.get(ModDataComponents.TOOL_MODIFIER);
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
		copy.set(ModDataComponents.TOOL_MODIFIER, copyMods);

		copy.set(DataComponents.CUSTOM_NAME, stack.get(DataComponents.CUSTOM_NAME));

		return copy;
	}
	
	/**
	 * Breaks a block as if {@code player} had mined it (drops + block-break stats),
	 * but only when the player can actually harvest it. Returns {@code true} when the
	 * block was destroyed so callers only spend durability on blocks they really broke.
	 */
	public static boolean breakBlockAsPlayer(ItemStack itemstack, BlockPos pos, ServerPlayer player, Level level, BlockState state) {
		if (!state.canHarvestBlock(level, pos, player)) {
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
		CompoundTag tag = getOrCreateTagElement(stack, "itemLore");

		tag.putString("itemLore", lore);

		addTagElement(stack, "itemLore", tag);
	}

	public static String getItemLore(ItemStack stack) {
		CompoundTag tag = getOrCreateTagElement(stack, "itemLore");
        return tag.getStringOr("itemLore", "");
	}

	public static int getMaxXP(int level) {
		int starting = 500;

        return (int) (starting * Math.pow(2, level));
	}

	public static ItemStack levelUp(ItemStack item, LivingEntity holder) {

		float stats = getStats(item);
		stats = stats * 1.1f;
		setStats(item, stats);

		holder.level().playSound(null, holder.getX(), holder.getY(), holder.getZ(), SoundEvents.PLAYER_LEVELUP,
				holder.getSoundSource(), 1.0f, 1.0f);

		return item;
	}
	
	public static CompoundTag getOrCreateTagElement(ItemStack stack, String tagName) {

		@Nullable ToolModifier mods = stack.get(ModDataComponents.TOOL_MODIFIER);
		if (mods == null) {
			return new CompoundTag();
		}

        Map<String, CompoundTag> tags = mods.getTags();
		CompoundTag tag = tags.get(tagName);
		if (tag == null) {
			tag = new CompoundTag();
		}
		return tag;
	}



	public static void addTagElement(ItemStack stack, String tagName, CompoundTag tag) {

		@Nullable ToolModifier mods = stack.get(ModDataComponents.TOOL_MODIFIER);
		if (mods == null) {
			mods = new ToolModifier(new HashMap<>());
		}

		Map<String, CompoundTag> tags = mods.getTags();
		HashMap<String, CompoundTag> mutTags = new HashMap<>(tags);

		mutTags.put(tagName, tag);

		mods = new ToolModifier(mutTags);

		stack.set(ModDataComponents.TOOL_MODIFIER, mods);
	}

	public static void removeTagKey(ItemStack stack, String tagName) {
		@Nullable ToolModifier mods = stack.get(ModDataComponents.TOOL_MODIFIER);
		if (mods == null) {
			mods = new ToolModifier(new HashMap<>());
		}

		Map<String, CompoundTag> tags = mods.getTags();


		HashMap<String, CompoundTag> mutTags = new HashMap<>(tags);

		mutTags.remove(tagName);

		mods = new ToolModifier(mutTags);

		stack.set(ModDataComponents.TOOL_MODIFIER, mods);
	}

	public static void addXp(ItemStack item, LivingEntity holder, int amount) {

		CompoundTag tag = getOrCreateTagElement(item,"XP");

		int level = tag.getIntOr("level", 0);

		int xp = tag.getIntOr("xp", 0);

		xp += amount;

		int max = getMaxXP(level);

		while (xp >= max) {
			xp = xp - max;
			level++;
			levelUp(item, holder);
		}

		tag.putInt("level", level);
		tag.putInt("xp", xp);

		addTagElement(item,"XP", tag);

	}

	public static int getLevel(ItemStack item) {

		CompoundTag tag = getOrCreateTagElement(item,"XP");

        return tag.getIntOr("level", 0);
	}

	public static int getXP(ItemStack item) {

		CompoundTag tag = getOrCreateTagElement(item,"XP");

        return tag.getIntOr("xp", 0);
	}

	public static ItemStack setLevelAndXP(ItemStack item, int level, int xp) {

		CompoundTag tag = getOrCreateTagElement(item,"XP");

		tag.putInt("level", level);
		tag.putInt("xp", xp);

		addTagElement(item,"XP", tag);

		return item;
	}

	public static List<Modifier> getModifiers(ItemStack item) {

		ArrayList<Modifier> tags = new ArrayList<Modifier>();

		CompoundTag modifiers = getOrCreateTagElement(item,Modifier.MODTAG);

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

		CompoundTag modifiers = getOrCreateTagElement(item,Modifier.MODTAG);
		CompoundTag newTag = mod.toNBT();

		boolean oldModFound = modifiers.contains(mod.tagName()); // does that tag already exist on the tool?

		if (!oldModFound) { // tag doesn't exist so we're adding a new trait.

			modifiers.put(mod.tagName(), newTag);
			addTagElement(item,Modifier.MODTAG, modifiers);
			return;

		}

		CompoundTag oldmod = modifiers.getCompoundOrEmpty(mod.tagName());
		Modifier oldModifier = mod.fromNBT(oldmod);

		if (!oldModifier.canLevel()) {
			return;
		}

		oldModifier.levelUp();
		modifiers.put(oldModifier.tagName(), oldModifier.toNBT());

		addTagElement(item,Modifier.MODTAG, modifiers);

	}

	public static void updateModifier(ItemStack item, Modifier mod) {

		CompoundTag modifiers = getOrCreateTagElement(item,Modifier.MODTAG);

		boolean oldModFound = modifiers.contains(mod.tagName()); // does that tag already exist on the tool?

		if (!oldModFound) { // tag doesn't exist so we're adding a new trait.
			return;
		}

		modifiers.put(mod.tagName(), mod.toNBT());

		addTagElement(item,Modifier.MODTAG, modifiers);
	}

	public static ItemStack removeModifier(ItemStack item, Modifier mod) {

		CompoundTag modifiers = getOrCreateTagElement(item,Modifier.MODTAG);

		modifiers.remove(mod.tagName());

		addTagElement(item,Modifier.MODTAG, modifiers);

		return item;
	}

	public static void setStats(ItemStack stack, float goodness) {
		CompoundTag statTag = getOrCreateTagElement(stack,"itemStats");

		statTag.putFloat("goodness", goodness);

		addTagElement(stack,"itemStats", statTag);
	}

	public static float getStats(ItemStack stack) {
		CompoundTag statTag = getOrCreateTagElement(stack,"itemStats");

        return statTag.getFloatOr("goodness", 0f);
	}

	public static void setBiomeTemperature(ItemStack stack, float temperature) {
		CompoundTag infoTag = getOrCreateTagElement(stack, "info");
		infoTag.putFloat("biomeTemp", temperature);
		addTagElement(stack, "info", infoTag);
	}

	public static float getBiomeTemperature(ItemStack stack) {
		CompoundTag infoTag = getOrCreateTagElement(stack, "info");
		return infoTag.getFloatOr("biomeTemp", 0.7f);
	}

	public static void setBiomeKey(ItemStack stack, String biomeKey) {
		CompoundTag infoTag = getOrCreateTagElement(stack, "info");
		infoTag.putString("biomeKey", biomeKey);
		addTagElement(stack, "info", infoTag);
	}

	public static String getBiomeKey(ItemStack stack) {
		CompoundTag infoTag = getOrCreateTagElement(stack, "info");
		return infoTag.getStringOr("biomeKey", "");
	}

	public static void setDimension(ItemStack stack, String dimension) {
		CompoundTag infoTag = getOrCreateTagElement(stack, "info");
		infoTag.putString("dimension", dimension);
		addTagElement(stack, "info", infoTag);
	}

	public static String getDimension(ItemStack stack) {
		CompoundTag infoTag = getOrCreateTagElement(stack, "info");
		return infoTag.getStringOr("dimension", "minecraft:overworld");
	}

	public static void setOwnerUUID(ItemStack stack, String uuid) {
		CompoundTag infoTag = getOrCreateTagElement(stack, "info");
		infoTag.putString("ownerUUID", uuid);
		addTagElement(stack, "info", infoTag);
	}

	public static String getOwnerUUID(ItemStack stack) {
		CompoundTag infoTag = getOrCreateTagElement(stack, "info");
		return infoTag.getStringOr("ownerUUID", "");
	}

	public static void setOwnerName(ItemStack stack, String name) {
		CompoundTag infoTag = getOrCreateTagElement(stack, "info");
		infoTag.putString("ownerName", name);
		addTagElement(stack, "info", infoTag);
	}

	public static String getOwnerName(ItemStack stack) {
		CompoundTag infoTag = getOrCreateTagElement(stack, "info");
		return infoTag.getStringOr("ownerName", "");
	}

	public static void setTexture(ItemStack stack, int texture) {
		CompoundTag cosmeticTag = getOrCreateTagElement(stack,"cosmetics");

		cosmeticTag.putInt("texture", texture);

		addTagElement(stack,"cosmetics", cosmeticTag);
	}

	public static float getTexture(ItemStack stack) {
		CompoundTag cosmeticTag = getOrCreateTagElement(stack,"cosmetics");

		int texture = cosmeticTag.getIntOr("texture", 0);

		float index = ((float) texture) / 10000.0f;

		ToolType type = getToolType(stack);

		switch (type) {

		case PICKAXE:
			index += 0.1f;
			break;
		case SHOVEL:
			index += 0.2f;
			break;
		case AXE:
			index += 0.3f;
			break;
		case SWORD:
			index += 0.4f;
			break;
		case NULL:
		default:
			break;

		}

		return index;
	}

	public static int getTextureIndex(ItemStack stack) {
		CompoundTag cosmeticTag = getOrCreateTagElement(stack,"cosmetics");

        return cosmeticTag.getIntOr("texture", 0);
	}

	private static void storeBiomeData(ItemStack lootItem, Level level, Player player) {
		float temp = 0.7f;

		if (player != null) {
			Holder<Biome> biome = level.getBiome(player.blockPosition());
			Biome b = biome.value();
			temp = b.getBaseTemperature();
		}

		setBiomeTemperature(lootItem, temp);

		// Store biome key for modifier filtering
		if (player != null) {
			Holder<Biome> biomeHolder = level.getBiome(player.blockPosition());

			// Get biome key from registry
			String biomeKey = "unknown";
			Registry<Biome> biomeRegistry = level.registryAccess().lookupOrThrow(Registries.BIOME);

			// Try unwrapKey first
			if (biomeHolder.unwrapKey().isPresent()) {
				biomeKey = biomeHolder.unwrapKey().get().identifier().toString();
			} else {
				// Fallback: search registry for this biome
				Biome biome = biomeHolder.value();
				for (var entry : biomeRegistry.entrySet()) {
					if (entry.getValue() == biome) {
						biomeKey = entry.getKey().identifier().toString();
						break;
					}
				}
			}

			setBiomeKey(lootItem, biomeKey);

			// Store dimension
			String dim = level.dimension().identifier().toString();
			setDimension(lootItem, dim);
		}
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

		if (player != null) {
			if (level.dimension().equals(Level.NETHER)) {
				nameColor = "#FF8C19";
			} else if (level.dimension().equals(Level.END)) {
				nameColor = "#C419FF";
			}
		}

		LootUtils.setItemName(lootItem, NameGenerator.generateNameWPrefix(level.getRandom(), temp, level.isRaining()), nameColor);

		String forger = NameGenerator.generateForger(level.getRandom(), temp);

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
		CompoundTag toolType = getOrCreateTagElement(item,"info");
		String type = toolType.getStringOr("type", "");
		if (type.isEmpty()) {
			return ToolType.NULL;
		}
		return ToolType.valueOf(type);
	}

	public static ItemStack setToolType(ItemStack item, ToolType type) {
		CompoundTag toolInfo = getOrCreateTagElement(item,"info");
		toolInfo.putString("type", type.name());
		addTagElement(item,"info", toolInfo);
		return item;
	}

	public static void generateNewTrait(ItemStack stack, ToolType type, RandomSource random) {

		List<Modifier> mods = getModifiers(stack);

		ArrayList<Modifier> allowedMods = new ArrayList<Modifier>();

		for (Entry<String, Modifier> entry : ModifierRegistry.getModifiers().entrySet()) {
			Modifier newMod = entry.getValue();

			if (!Config.traitEnabled(newMod.tagName())) {
				continue;
			}

			// Filter by biome restrictions (for natural spawning only)
			if (newMod instanceof BiomeRestrictedModifier biomeRestricted) {
				String biomeKey = getBiomeKey(stack);
				float temp = getBiomeTemperature(stack);
				String dimension = getDimension(stack);

				if (!biomeRestricted.canSpawnInBiome(biomeKey, temp, dimension)) {
					continue; // Skip this modifier if biome doesn't match
				}
			}

			if (!newMod.forTool(type)) {
				continue;
			}

			boolean compatible = true;

			for (Modifier modifier : mods) {

				if (modifier.tagName().equals(newMod.tagName())) {
					if (!modifier.canLevel()) {
						compatible = false;
						break;
					}
				}

				if (!modifier.compatible(newMod)) {
					compatible = false;
					break;
				}

			}

			if (compatible) {
				allowedMods.add(newMod);
			}

		}

		int size = allowedMods.size();

		if (size == 0) {
			return;
		}

		int choice = random.nextInt(size);

		Modifier m = allowedMods.get(choice);

		addModifier(stack, m);

	}

	public static void generateInitialTraits(ItemStack stack, ToolType type, int count, RandomSource random) {
		for (int i = 0; i < count; i++) {
			generateNewTrait(stack, getToolType(stack), random);
		}
	}

	public static int getToolMaxTextures(ItemStack stack) {
		ToolType m = getToolType(stack);

		return switch (m) {
		case PICKAXE: {
			yield PICKAXE_COUNT;
		}
		case AXE: {
			yield AXE_COUNT;
		}
		case SHOVEL: {
			yield SHOVEL_COUNT;
		}
		case SWORD: {
			yield SWORD_COUNT;
		}
		default:
			yield 0;
		};

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
		ItemStack lootItem = new ItemStack(ModItems.TOOL.get());

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
		int count = 0;
		if (level.isClientSide()) {
			return ItemStack.EMPTY;
		}

		if (player != null) {
			ServerPlayer sPlayer = (ServerPlayer) player;
			StatType<Item> itemUsed = Stats.ITEM_USED;
			count = sPlayer.getStats().getValue(itemUsed.get(ModItems.CASE.get()));
		}

		float goodness = (float) (Math.sqrt(count + 1) * Config.Goodness); // keeping track of items stats through a
																			// "goodness" curve

		int traits = (int) (Math.floor(goodness / 2.0f)); // how many traits the tool should be created with

		LootUtils.setStats(lootItem, goodness);

		int toolType = level.getRandom().nextInt(4);
		ToolType m = switch (toolType) {
		case 0: {
			yield ToolType.PICKAXE;
		}
		case 1: {
			yield ToolType.AXE;
		}
		case 2: {
			yield ToolType.SHOVEL;
		}
		case 3: {
			yield ToolType.SWORD;
		}
		default: {
			yield ToolType.PICKAXE;
		}
		};

		int textureCount = switch (m) {
		case PICKAXE: {
			yield PICKAXE_COUNT;
		}
		case AXE: {
			yield AXE_COUNT;
		}
		case SHOVEL: {
			yield SHOVEL_COUNT;
		}
		case SWORD: {
			yield SWORD_COUNT;
		}
		default:
			yield 0;
		};

		lootItem = setToolType(lootItem, m);

		// Store biome data BEFORE generating traits (so biome-restricted modifiers work)
		storeBiomeData(lootItem, level, player);

		// Store owner data for Soulbound modifier
		if (player != null) {
			setOwnerUUID(lootItem, player.getStringUUID());
			setOwnerName(lootItem, player.getDisplayName().getString());
		}

		generateInitialTraits(lootItem, m, traits, level.getRandom());

		generateLore(lootItem, level, player);

		if (textureCount > 0) {
			LootUtils.setTexture(lootItem, level.getRandom().nextInt(textureCount));
		}

		return lootItem;
	}

	public static boolean generateTool(ServerPlayer player, Level level) {

		ItemStack lootItem = genTool(player, level);

		boolean added = player.getInventory().add(lootItem);
		if (!added) {
			ItemEntity dropItem = new ItemEntity(EntityType.ITEM, level);
			dropItem.setItem(lootItem);
			dropItem.setPos(player.position());

			level.addFreshEntity(dropItem);
		}
		return true;
	}

	public static String roman(int input) {
		if (input < 1 || input > 3999)
			return "Invalid Roman Number Value";
		StringBuilder s = new StringBuilder();
		while (input >= 1000) {
			s.append("M");
			input -= 1000;
		}
		while (input >= 900) {
			s.append("CM");
			input -= 900;
		}
		while (input >= 500) {
			s.append("D");
			input -= 500;
		}
		while (input >= 400) {
			s.append("CD");
			input -= 400;
		}
		while (input >= 100) {
			s.append("C");
			input -= 100;
		}
		while (input >= 90) {
			s.append("XC");
			input -= 90;
		}
		while (input >= 50) {
			s.append("L");
			input -= 50;
		}
		while (input >= 40) {
			s.append("XL");
			input -= 40;
		}
		while (input >= 10) {
			s.append("X");
			input -= 10;
		}
		while (input >= 9) {
			s.append("IX");
			input -= 9;
		}
		while (input >= 5) {
			s.append("V");
			input -= 5;
		}
		while (input >= 4) {
			s.append("IV");
			input -= 4;
		}
		while (input >= 1) {
			s.append("I");
			input -= 1;
		}
		return s.toString();

	}

}
