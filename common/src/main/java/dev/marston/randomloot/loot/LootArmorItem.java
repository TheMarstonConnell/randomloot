package dev.marston.randomloot.loot;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.advancements.ModCriteria;
import dev.marston.randomloot.advancements.TraitObtainedTrigger;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.NotNull;

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
		return GearStats.defense(LootUtils.getStats(stack), type, LootUtils.statMultiplier(stack));
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

		// No attributes while rolling: hides the tooltip lines until the reveal.
		if (LootUtils.isRolling(stack)) {
			return ItemAttributeModifiers.builder().build();
		}

		ToolType tt = LootUtils.getToolType(stack);
		EquipmentSlot slot = tt.armorSlot();

		if (slot == null) {
			return ItemAttributeModifiers.builder().build();
		}

		EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(slot);
		ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(RandomLoot.MODID,
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
	protected boolean isInHoldSlot(ItemStack stack, Entity holder, int slotId, boolean selected) {
		EquipmentSlot slot = LootUtils.wearableSlot(stack);
		return slot != null && holder instanceof LivingEntity living && living.getItemBySlot(slot) == stack;
	}

	/**
	 * Right-click to equip. 26.x got this for free from the EQUIPPABLE component;
	 * on 1.21.1 we swap into the piece's slot by hand (mirroring Equipable's
	 * swapWithEquipmentSlot, which can't be used - its slot is item-level while
	 * ours is per-stack).
	 */
	@Override
	public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		EquipmentSlot slot = LootUtils.wearableSlot(stack);
		if (slot == null) {
			return InteractionResultHolder.pass(stack);
		}

		ItemStack worn = player.getItemBySlot(slot);
		if ((!net.minecraft.world.item.enchantment.EnchantmentHelper.has(worn,
				net.minecraft.world.item.enchantment.EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)
				|| player.isCreative()) && !ItemStack.matches(stack, worn)) {
			if (!level.isClientSide()) {
				player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
			}
			ItemStack swapped = worn.isEmpty() ? stack : worn.copyAndClear();
			ItemStack equipped = player.isCreative() ? stack.copy() : stack.copyAndClear();
			player.setItemSlot(slot, equipped);
			return InteractionResultHolder.sidedSuccess(swapped, level.isClientSide());
		}
		return InteractionResultHolder.fail(stack);
	}

	@Override
	protected void appendStatLines(ItemStack item, ToolType tt, Consumer<Component> tips) {
		float defense = getDefense(item, tt);
		tips.accept(Component.translatableWithFallback("tooltip.randomloot.armor", "Armor: %s",
				String.format("%.2f", defense)).withStyle(ChatFormatting.GRAY));

		float toughness = getToughness(item, tt);
		tips.accept(Component.translatableWithFallback("tooltip.randomloot.toughness", "Toughness: %s",
				String.format("%.2f", toughness)).withStyle(ChatFormatting.GRAY));
	}

	// Datapack-editable enchantment groups; see data/randomloot/tags/enchantment/.
	private static final TagKey<Enchantment> ALL_ARMOR_ENCHANTS = TagKey.create(Registries.ENCHANTMENT,
			ResourceLocation.fromNamespaceAndPath(RandomLoot.MODID, "all_armor"));
	private static final TagKey<Enchantment> HELMET_ENCHANTS = TagKey.create(Registries.ENCHANTMENT,
			ResourceLocation.fromNamespaceAndPath(RandomLoot.MODID, "helmets"));
	private static final TagKey<Enchantment> CHESTPLATE_ENCHANTS = TagKey.create(Registries.ENCHANTMENT,
			ResourceLocation.fromNamespaceAndPath(RandomLoot.MODID, "chestplates"));
	private static final TagKey<Enchantment> LEGGINGS_ENCHANTS = TagKey.create(Registries.ENCHANTMENT,
			ResourceLocation.fromNamespaceAndPath(RandomLoot.MODID, "leggings"));
	private static final TagKey<Enchantment> BOOTS_ENCHANTS = TagKey.create(Registries.ENCHANTMENT,
			ResourceLocation.fromNamespaceAndPath(RandomLoot.MODID, "boots"));

	/**
	 * Mirrors {@link LootItem#supportsEnchantment}: the single armor item sits in every
	 * minecraft:enchantable/*_armor tag, so per-piece filtering has to happen here for
	 * both the enchanting table and the anvil/book path.
	 */
	@Override
	public Boolean supportsEnchantmentCommon(ItemStack stack, Holder<Enchantment> enchantment) {
		if (LootUtils.isRolling(stack)) {
			return false;
		}

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
		// supported-items definition; null = defer to the loader's default check.
		return null;
	}

}
