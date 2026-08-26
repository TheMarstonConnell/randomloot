package dev.marston.randomloot.loot;

import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.marston.randomloot.advancements.ModCriteria;
import dev.marston.randomloot.advancements.TraitObtainedTrigger;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.platform.Services;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * What {@link LootItem} and {@link LootArmorItem} have in common: a piece of Random Loot
 * gear with a type, goodness, traits, a reveal roll and derived vanilla components.
 *
 * <p>The two used to be unrelated subclasses of {@code Item} that had drifted into ten
 * duplicated members - identical constructors, {@code getName}, {@code onCraftedBy}, the
 * StatsModifier fold, and near-identical {@code inventoryTick} and {@code appendHoverText}
 * skeletons. {@link LootUtils#refreshDerivedComponents} and
 * {@link LootUtils#gearEnchantGate} both carried an {@code instanceof} ladder that existed
 * only to paper over the split; both now dispatch through this one type.
 *
 * <p>Subclasses supply what genuinely differs: the attribute set (attack vs armor), the
 * slot hold-traits run in, the tooltip stat lines and the enchantment groups.
 */
public abstract class LootGearItem extends Item {

	protected LootGearItem(Properties p) {
		super(p.stacksTo(1).durability(100));
	}

	/** The stack's attributes, derived from its goodness, type and traits. */
	public abstract ItemAttributeModifiers buildAttributeModifiers(ItemStack stack);

	/**
	 * Whether hold-style traits should run this tick: the tool is the selected hotbar
	 * item, or the armor piece is actually worn in its own slot.
	 */
	protected abstract boolean isInHoldSlot(ItemStack stack, Entity holder, int slotId, boolean selected);

	/** The type-specific stat lines in the shift-expanded tooltip. */
	protected abstract void appendStatLines(ItemStack stack, ToolType type, Consumer<Component> tips);

	/**
	 * The mod's per-type enchant filter: {@code true}/{@code false} to decide, or
	 * {@code null} to defer to the loader's default check. Both gear items sit in every
	 * relevant {@code minecraft:enchantable/*} tag, so the per-type split has to happen
	 * here rather than in the tags.
	 */
	public abstract Boolean supportsEnchantmentCommon(ItemStack stack, Holder<Enchantment> enchantment);

	/** 26.x sets this via Properties.enchantable(15); 1.21.1 reads the item method. */
	@Override
	public int getEnchantmentValue() {
		return 15;
	}

	/** 26.x sets this via Properties.repairable(tag); 1.21.1 reads the item method. */
	@Override
	public boolean isValidRepairItem(@NotNull ItemStack stack, @NotNull ItemStack repairCandidate) {
		return repairCandidate.is(this instanceof LootArmorItem
				? dev.marston.randomloot.items.ModItems.ARMOR_REPAIR_MATERIALS
				: dev.marston.randomloot.items.ModItems.TOOL_REPAIR_MATERIALS);
	}

	/** Durability derived from goodness; stored as the vanilla MAX_DAMAGE component. */
	public static int computeMaxDamage(ItemStack stack) {
		return GearStats.maxDamage(LootUtils.getStats(stack), LootUtils.getToolType(stack));
	}

	@Override
	public void onCraftedBy(@NotNull ItemStack stack, @NotNull Level level, @NotNull Player player) {
		super.onCraftedBy(stack, level, player);
		// Taking gear out of a smithing table means a trait recipe ran; crafting-table
		// takes are the texture-change recipe, which doesn't alter traits.
		if (player instanceof ServerPlayer sPlayer && player.containerMenu instanceof SmithingMenu) {
			if (player.level() instanceof ServerLevel serverLevel) {
				LootUtils.applyWorldForgers(stack, serverLevel.getSeed());
			}
			ModCriteria.traitsObtained(sPlayer, stack, TraitObtainedTrigger.SOURCE_CRAFTED);
		}
	}

	@Override
	public @NotNull Component getName(@NotNull ItemStack stack) {
		if (LootUtils.isRolling(stack)) {
			return LootTooltips.rollingName();
		}
		return super.getName(stack);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level lvl, Entity holder, int slotId, boolean selected) {
		if (!(lvl instanceof ServerLevel level)) {
			return;
		}

		// Migrates items from before attributes/durability moved to data components
		// (they were dynamic NeoForge item-method overrides).
		LootUtils.migrateDerivedComponents(stack);

		// Runs in any slot, unlike the trait ticking below.
		if (LootUtils.tickRoll(stack, level, holder)) {
			return;
		}

		// The GearTags.has guard skips the per-tick trait deserialization for
		// trait-less gear.
		if (!isInHoldSlot(stack, holder, slotId, selected) || !GearTags.has(stack, Modifier.MODTAG)) {
			return;
		}

		for (Modifier mod : LootUtils.getModifiers(stack)) {
			if (mod instanceof HoldModifier holdMod) {
				holdMod.hold(stack, level, holder);
			}
		}
	}

	@Override
	public void appendHoverText(ItemStack item, TooltipContext pContext, java.util.List<Component> tipList,
			TooltipFlag pTooltipFlag) {
		LootTooltips.appendHoverText(item, Services.PLATFORM.tooltipLevel(pContext), tipList::add,
				(type, tips) -> appendStatLines(item, type, tips));
	}
}
