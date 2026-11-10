package dev.marston.randomloot.loot;

import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.marston.randomloot.component.ModDataComponents;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.platform.Services;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.Level;
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

	/**
	 * True when two stacks are the same piece of gear apart from its {@code tool_mod}
	 * component, which is each loader's cue to skip the hand re-equip animation
	 * (NeoForge {@code shouldCauseReequipAnimation}, Fabric
	 * {@code allowComponentsUpdateAnimation}). {@code tool_mod} changes on nearly every
	 * use - XP per block and per hit, trait state - and vanilla 1.21.1 replays the
	 * animation for any component change on the held stack. Durability is deliberately
	 * still compared: a tool that takes damage dips exactly like a vanilla tool does on
	 * this version. Two different pieces of gear differ in name, attributes or max
	 * damage, so swapping between them still animates.
	 */
	public static boolean sameApartFromToolMod(ItemStack oldStack, ItemStack newStack) {
		if (oldStack == newStack) {
			return true;
		}
		if (!ItemStack.isSameItem(oldStack, newStack) || oldStack.getCount() != newStack.getCount()) {
			return false;
		}

		DataComponentType<?> toolMod = ModDataComponents.TOOL_MODIFIER.get();
		DataComponentMap oldComponents = oldStack.getComponents();
		DataComponentMap newComponents = newStack.getComponents();
		for (TypedDataComponent<?> component : oldComponents) {
			if (component.type() != toolMod && !component.value().equals(newComponents.get(component.type()))) {
				return false;
			}
		}
		for (DataComponentType<?> type : newComponents.keySet()) {
			if (type != toolMod && !oldComponents.has(type)) {
				return false;
			}
		}
		return true;
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
