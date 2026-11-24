package dev.marston.randomloot.loot;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.loot.modifiers.TraitDispatch;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.equipment.Equippable;

import java.util.*;
import java.util.function.Consumer;

/**
 * The wearable counterpart to {@link LootItem}. One registered item covers all four
 * armor pieces: the piece type lives in the "info" tag (like tool types) and the
 * equipment slot + worn texture live in the per-stack EQUIPPABLE component, kept in
 * sync by {@link LootUtils#updateEquippable(ItemStack)}.
 */
public class LootArmorItem extends LootGearItem {

	public LootArmorItem(Properties p) {
		super(p);
	}

	public static float getDefense(ItemStack stack, ToolType type) {
		return GearStats.defense(LootUtils.getStats(stack), type, TraitDispatch.statMultiplier(stack));
	}

	public static float getToughness(ItemStack stack, ToolType type) {
		return GearStats.toughness(LootUtils.getStats(stack), type);
	}

	/**
	 * The piece's defense/toughness attributes, derived from its stats and
	 * StatsModifier traits. Stored on the stack as the vanilla
	 * ATTRIBUTE_MODIFIERS component by {@link LootUtils#refreshDerivedComponents}.
	 */
	@Override
	public ItemAttributeModifiers buildAttributeModifiers(ItemStack stack) {
		ToolType tt = LootUtils.getToolType(stack);
		EquipmentSlot slot = tt.armorSlot();

		if (slot == null) {
			return ItemAttributeModifiers.builder().build();
		}

		EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(slot);
		Identifier modifierId = Identifier.fromNamespaceAndPath(RandomLoot.MODID,
				"armor." + tt.name().toLowerCase(Locale.ROOT));

		return ItemAttributeModifiers.builder()
				.add(Attributes.ARMOR,
						new AttributeModifier(modifierId, getDefense(stack, tt), AttributeModifier.Operation.ADD_VALUE),
						group)
				.add(Attributes.ARMOR_TOUGHNESS,
						new AttributeModifier(modifierId, getToughness(stack, tt), AttributeModifier.Operation.ADD_VALUE),
						group)
				.build();
	}

	/** Hold-style traits run only while the piece is actually worn in its own slot. */
	@Override
	protected EquipmentSlot holdSlot(ItemStack stack) {
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		return equippable == null ? null : equippable.slot();
	}

	@Override
	protected void appendStatLines(ItemStack item, ToolType tt, Consumer<Component> tips) {
		statLine(tips, "tooltip.randomloot.armor", "Armor: %s", getDefense(item, tt));
		statLine(tips, "tooltip.randomloot.toughness", "Toughness: %s", getToughness(item, tt));
	}

	// Datapack-editable enchantment groups; see data/randomloot/tags/enchantment/.
	private static final TagKey<Enchantment> ALL_ARMOR_ENCHANTS = enchantTag("all_armor");
	// An EnumMap, so the pieces are checked in declaration order: an enchantment in two
	// piece tags goes by the first, as it always has.
	private static final Map<ToolType, TagKey<Enchantment>> PIECE_ENCHANTS = new EnumMap<>(Map.of(
			ToolType.HELMET, enchantTag("helmets"),
			ToolType.CHESTPLATE, enchantTag("chestplates"),
			ToolType.LEGGINGS, enchantTag("leggings"),
			ToolType.BOOTS, enchantTag("boots")));

	/**
	 * Mirrors {@link LootItem#supportsEnchantment}: the single armor item sits in every
	 * minecraft:enchantable/*_armor tag, so per-piece filtering has to happen here for
	 * both the enchanting table and the anvil/book path.
	 */
	@Override
	public Boolean supportsEnchantmentCommon(ItemStack stack, Holder<Enchantment> enchantment) {
		ToolType type = LootUtils.getToolType(stack);

		if (enchantment.is(ALL_ARMOR_ENCHANTS)) {
			return type.isArmor();
		}

		for (Map.Entry<ToolType, TagKey<Enchantment>> piece : PIECE_ENCHANTS.entrySet()) {
			if (enchantment.is(piece.getValue())) {
				return type == piece.getKey();
			}
		}

		// Enchantments in none of the randomloot tags (e.g. modded ones) follow their own
		// supported-items definition; null = defer to the loader's default check.
		return null;
	}

}
