package dev.marston.randomloot.loot;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.advancements.ModCriteria;
import dev.marston.randomloot.advancements.TraitObtainedTrigger;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

/**
 * The wearable counterpart to {@link LootItem}. One registered item covers all four
 * armor pieces: the piece type lives in the "info" tag (like tool types) and the
 * equipment slot + worn texture are derived per stack on demand by
 * {@link LootUtils#wearableSlot(ItemStack)} and {@link LootUtils#wornSetIndex(ItemStack)}.
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

	// One fixed modifier id per piece, like vanilla's per-slot armor UUIDs.
	private static final Map<ToolType, UUID> ARMOR_MODIFIER_IDS = Map.of(
			ToolType.HELMET, UUID.fromString("5f3a1c2e-8b7d-4e61-9a10-000000000001"),
			ToolType.CHESTPLATE, UUID.fromString("5f3a1c2e-8b7d-4e61-9a10-000000000002"),
			ToolType.LEGGINGS, UUID.fromString("5f3a1c2e-8b7d-4e61-9a10-000000000003"),
			ToolType.BOOTS, UUID.fromString("5f3a1c2e-8b7d-4e61-9a10-000000000004"));

	/**
	 * The piece's defense/toughness attributes, derived from its stats and
	 * StatsModifier traits. Stamped onto the stack as the vanilla
	 * {@code AttributeModifiers} NBT by {@link LootUtils#refreshDerivedComponents}.
	 */
	@Override
	public List<GearAttribute> buildAttributeModifiers(ItemStack stack) {

		// No attributes while rolling: hides the tooltip lines until the reveal.
		if (LootUtils.isRolling(stack)) {
			return List.of();
		}

		ToolType tt = LootUtils.getToolType(stack);
		EquipmentSlot slot = tt.armorSlot();
		UUID modifierId = ARMOR_MODIFIER_IDS.get(tt);

		if (slot == null || modifierId == null) {
			return List.of();
		}

		return List.of(
				new GearAttribute(Attributes.ARMOR, new AttributeModifier(modifierId, "Armor modifier",
						getDefense(stack, tt), AttributeModifier.Operation.ADDITION), slot),
				new GearAttribute(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(modifierId, "Armor toughness",
						getToughness(stack, tt), AttributeModifier.Operation.ADDITION), slot));
	}

	/** Hold-style traits run only while the piece is actually worn in its own slot. */
	@Override
	protected boolean isInHoldSlot(ItemStack stack, Entity holder, int slotId, boolean selected) {
		EquipmentSlot slot = LootUtils.wearableSlot(stack);
		return slot != null && holder instanceof LivingEntity living && living.getItemBySlot(slot) == stack;
	}

	/**
	 * Right-click to equip. 26.x got this for free from the EQUIPPABLE component;
	 * on 1.20.1 we swap into the piece's slot by hand (mirroring Equipable's
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
		if ((!net.minecraft.world.item.enchantment.EnchantmentHelper.hasBindingCurse(worn)
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
			new ResourceLocation(RandomLoot.MODID, "all_armor"));
	private static final TagKey<Enchantment> HELMET_ENCHANTS = TagKey.create(Registries.ENCHANTMENT,
			new ResourceLocation(RandomLoot.MODID, "helmets"));
	private static final TagKey<Enchantment> CHESTPLATE_ENCHANTS = TagKey.create(Registries.ENCHANTMENT,
			new ResourceLocation(RandomLoot.MODID, "chestplates"));
	private static final TagKey<Enchantment> LEGGINGS_ENCHANTS = TagKey.create(Registries.ENCHANTMENT,
			new ResourceLocation(RandomLoot.MODID, "leggings"));
	private static final TagKey<Enchantment> BOOTS_ENCHANTS = TagKey.create(Registries.ENCHANTMENT,
			new ResourceLocation(RandomLoot.MODID, "boots"));

	/**
	 * Mirrors {@link LootItem#supportsEnchantmentCommon}: one item covers all four pieces,
	 * so per-piece filtering has to happen here for both the enchanting table and the
	 * anvil/book path.
	 */
	@Override
	public Boolean supportsEnchantmentCommon(ItemStack stack, Enchantment enchantment) {
		if (LootUtils.isRolling(stack)) {
			return false;
		}

		ToolType type = LootUtils.getToolType(stack);

		if (LootUtils.enchantIn(enchantment, ALL_ARMOR_ENCHANTS)) {
			return type.isArmor();
		}

		if (LootUtils.enchantIn(enchantment, HELMET_ENCHANTS)) {
			return type == ToolType.HELMET;
		}

		if (LootUtils.enchantIn(enchantment, CHESTPLATE_ENCHANTS)) {
			return type == ToolType.CHESTPLATE;
		}

		if (LootUtils.enchantIn(enchantment, LEGGINGS_ENCHANTS)) {
			return type == ToolType.LEGGINGS;
		}

		if (LootUtils.enchantIn(enchantment, BOOTS_ENCHANTS)) {
			return type == ToolType.BOOTS;
		}

		// Enchantments in none of the randomloot tags (e.g. modded ones) follow their own
		// EnchantmentCategory; null = defer to the vanilla check.
		return null;
	}

}
