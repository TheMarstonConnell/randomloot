package dev.marston.randomloot.loot;

import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * What a piece of Random Loot gear is: one of the four hand tools or the four armor
 * pieces. Stored per stack in the {@link GearTags#INFO} tag and the input to almost every
 * derived value - stats, textures, enchant filtering and trait eligibility.
 *
 * <p>This used to be nested inside {@link LootItem}, which meant the armor item, the
 * recipes and all ~60 modifiers imported their shared vocabulary out of the tool class.
 */
public enum ToolType {
	PICKAXE, SHOVEL, AXE, SWORD, HELMET, CHESTPLATE, LEGGINGS, BOOTS, NULL;

	@Override
	public String toString() {
		return switch (this) {
		case PICKAXE -> "Pickaxe";
		case SHOVEL -> "Shovel";
		case AXE -> "Axe";
		case SWORD -> "Sword";
		case HELMET -> "Helmet";
		case CHESTPLATE -> "Chestplate";
		case LEGGINGS -> "Leggings";
		case BOOTS -> "Boots";
		default -> "Null";
		};
	}

	/** Translatable display name for tooltips, falling back to the English toString(). */
	public Component displayName() {
		return Component.translatableWithFallback(
				"tooltip.randomloot.type." + name().toLowerCase(Locale.ROOT), toString());
	}

	/** True for the four wearable piece types. */
	public boolean isArmor() {
		return this == HELMET || this == CHESTPLATE || this == LEGGINGS || this == BOOTS;
	}

	/** The equipment slot an armor piece occupies, or null for hand tools. */
	public EquipmentSlot armorSlot() {
		return switch (this) {
		case HELMET -> EquipmentSlot.HEAD;
		case CHESTPLATE -> EquipmentSlot.CHEST;
		case LEGGINGS -> EquipmentSlot.LEGS;
		case BOOTS -> EquipmentSlot.FEET;
		default -> null;
		};
	}
}
