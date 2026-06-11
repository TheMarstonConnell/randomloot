package dev.marston.randomloot.loot;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.advancements.ModCriteria;
import dev.marston.randomloot.advancements.TraitObtainedTrigger;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.StatsModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.equipment.Equippable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

/**
 * The wearable counterpart to {@link LootItem}. One registered item covers all four
 * armor pieces: the piece type lives in the "info" tag (like tool types) and the
 * equipment slot + worn texture live in the per-stack EQUIPPABLE component, kept in
 * sync by {@link LootUtils#updateEquippable(ItemStack)}.
 */
public class LootArmorItem extends Item {

	public LootArmorItem(Properties p) {
		super(p.stacksTo(1).durability(100));
	}

	@Override
	public void onCraftedBy(@NotNull ItemStack stack, @NotNull Player player) {
		super.onCraftedBy(stack, player);
		// Taking armor out of a smithing table means a trait recipe ran; crafting-table
		// takes are the texture-change recipe, which doesn't alter traits.
		if (player instanceof ServerPlayer sPlayer && player.containerMenu instanceof SmithingMenu) {
			if (player.level() instanceof ServerLevel serverLevel) {
				LootUtils.applyWorldForgers(stack, serverLevel.getSeed());
			}
			ModCriteria.traitsObtained(sPlayer, stack, TraitObtainedTrigger.SOURCE_CRAFTED);
		}
	}

	public static float getDefense(ItemStack stack, ToolType type) {

		float statMod = 1.0f;

		// getModifiers already filters out config-disabled traits.
		for (Modifier mod : LootUtils.getModifiers(stack)) {
			if (mod instanceof StatsModifier sm) {
				statMod *= sm.getStats(stack);
			}
		}

		float base = LootUtils.getStats(stack) + 1.0f;

		float scale = switch (type) {
		case HELMET -> 0.6f;
		case CHESTPLATE -> 1.4f;
		case LEGGINGS -> 1.1f;
		case BOOTS -> 0.6f;
		default -> 0.0f;
		};

		return base * scale * statMod;
	}

	public static float getToughness(ItemStack stack, ToolType type) {
		if (!type.isArmor()) {
			return 0.0f;
		}

		return LootUtils.getStats(stack) * 0.25f;
	}

	@Override
	public @NotNull ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {

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

	@Override
	public int getMaxDamage(@NotNull ItemStack stack) {

		ToolType tt = LootUtils.getToolType(stack);

		// Per-piece durability weights follow vanilla's ArmorType unit durabilities.
		int unit = switch (tt) {
		case HELMET -> 11;
		case CHESTPLATE -> 16;
		case LEGGINGS -> 15;
		case BOOTS -> 13;
		default -> 10;
		};

		return (int) ((LootUtils.getStats(stack) + 10.0f) * unit * 3.0f);
	}

	@Override
	public boolean isCombineRepairable(ItemStack stack) {
		return false;
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity holder, EquipmentSlot slot) {

		// Hold-style traits run while the piece is actually worn in its slot.
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		if (equippable == null || slot != equippable.slot()) {
			return;
		}

		List<Modifier> mods = LootUtils.getModifiers(stack);

		for (Modifier mod : mods) {

			if (mod instanceof HoldModifier holdMod) {

				holdMod.hold(stack, level, holder);
			}

		}

	}

	@Override
	public void appendHoverText(ItemStack item, TooltipContext pContext, TooltipDisplay display, Consumer<Component> tipList, TooltipFlag pTooltipFlag) {
		LootTooltips.appendHoverText(item, pContext.level(), tipList, (tt, tips) -> {
			float defense = getDefense(item, tt);
			tips.accept(Component.translatableWithFallback("tooltip.randomloot.armor", "Armor: %s",
					String.format("%.2f", defense)).withStyle(ChatFormatting.GRAY));

			float toughness = getToughness(item, tt);
			tips.accept(Component.translatableWithFallback("tooltip.randomloot.toughness", "Toughness: %s",
					String.format("%.2f", toughness)).withStyle(ChatFormatting.GRAY));
		});
	}

	// Datapack-editable enchantment groups; see data/randomloot/tags/enchantment/.
	private static final TagKey<Enchantment> ALL_ARMOR_ENCHANTS = TagKey.create(Registries.ENCHANTMENT,
			Identifier.fromNamespaceAndPath(RandomLoot.MODID, "all_armor"));
	private static final TagKey<Enchantment> HELMET_ENCHANTS = TagKey.create(Registries.ENCHANTMENT,
			Identifier.fromNamespaceAndPath(RandomLoot.MODID, "helmets"));
	private static final TagKey<Enchantment> CHESTPLATE_ENCHANTS = TagKey.create(Registries.ENCHANTMENT,
			Identifier.fromNamespaceAndPath(RandomLoot.MODID, "chestplates"));
	private static final TagKey<Enchantment> LEGGINGS_ENCHANTS = TagKey.create(Registries.ENCHANTMENT,
			Identifier.fromNamespaceAndPath(RandomLoot.MODID, "leggings"));
	private static final TagKey<Enchantment> BOOTS_ENCHANTS = TagKey.create(Registries.ENCHANTMENT,
			Identifier.fromNamespaceAndPath(RandomLoot.MODID, "boots"));

	/**
	 * Mirrors {@link LootItem#supportsEnchantment}: the single armor item sits in every
	 * minecraft:enchantable/*_armor tag, so per-piece filtering has to happen here for
	 * both the enchanting table and the anvil/book path.
	 */
	@Override
	public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
		ToolType type = LootUtils.getToolType(stack);

		if (enchantment.is(ALL_ARMOR_ENCHANTS)) {
			return type.isArmor();
		}

		if (enchantment.is(HELMET_ENCHANTS)) {
			return type == ToolType.HELMET;
		}

		if (enchantment.is(CHESTPLATE_ENCHANTS)) {
			return type == ToolType.CHESTPLATE;
		}

		if (enchantment.is(LEGGINGS_ENCHANTS)) {
			return type == ToolType.LEGGINGS;
		}

		if (enchantment.is(BOOTS_ENCHANTS)) {
			return type == ToolType.BOOTS;
		}

		// Enchantments in none of the randomloot tags (e.g. modded ones) follow their own
		// supported-items definition; add them to a randomloot tag to piece-restrict them.
		return super.supportsEnchantment(stack, enchantment);
	}

}
