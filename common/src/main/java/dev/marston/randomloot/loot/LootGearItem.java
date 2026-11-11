package dev.marston.randomloot.loot;

import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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

	/** One attribute modifier and the slot it applies in; see {@link #buildAttributeModifiers}. */
	public record GearAttribute(Attribute attribute, AttributeModifier modifier, EquipmentSlot slot) {
	}

	/**
	 * The stack's attributes, derived from its goodness, type and traits.
	 * {@link LootUtils#refreshDerivedComponents} stamps them into the vanilla
	 * {@code AttributeModifiers} stack NBT (1.20.1 has no ATTRIBUTE_MODIFIERS component).
	 */
	public abstract java.util.List<GearAttribute> buildAttributeModifiers(ItemStack stack);

	/**
	 * Whether hold-style traits should run this tick: the tool is the selected hotbar
	 * item, or the armor piece is actually worn in its own slot.
	 */
	protected abstract boolean isInHoldSlot(ItemStack stack, Entity holder, int slotId, boolean selected);

	/** The type-specific stat lines in the shift-expanded tooltip. */
	protected abstract void appendStatLines(ItemStack stack, ToolType type, Consumer<Component> tips);

	/**
	 * The mod's per-type enchant filter: {@code true}/{@code false} to decide, or
	 * {@code null} to defer to vanilla's {@code EnchantmentCategory} check. 1.20.1
	 * categories gate on {@code instanceof SwordItem/DiggerItem/ArmorItem}, which the
	 * single gear item can never satisfy, so the per-type split has to happen here.
	 */
	public abstract Boolean supportsEnchantmentCommon(ItemStack stack, Enchantment enchantment);

	/** 26.x sets this via Properties.enchantable(15); 1.20.1 reads the item method. */
	@Override
	public int getEnchantmentValue() {
		return 15;
	}

	/** 26.x sets this via Properties.repairable(tag); 1.20.1 reads the item method. */
	@Override
	public boolean isValidRepairItem(@NotNull ItemStack stack, @NotNull ItemStack repairCandidate) {
		return repairCandidate.is(this instanceof LootArmorItem
				? dev.marston.randomloot.items.ModItems.ARMOR_REPAIR_MATERIALS
				: dev.marston.randomloot.items.ModItems.TOOL_REPAIR_MATERIALS);
	}

	/** Durability derived from goodness; cached on the stack by refreshDerivedComponents. */
	public static int computeMaxDamage(ItemStack stack) {
		return GearStats.maxDamage(LootUtils.getStats(stack), LootUtils.getToolType(stack));
	}

	/**
	 * The stack's max durability. 1.20.1 has no MAX_DAMAGE component and vanilla reads a
	 * per-item constant, so each loader routes {@code ItemStack.getMaxDamage()} here:
	 * Forge through its stack-aware {@code Item#getMaxDamage(ItemStack)}, Fabric through
	 * {@code ItemStackMixin}. Reads the value {@link LootUtils#refreshDerivedComponents}
	 * cached, so this stays a tag lookup rather than a trait fold.
	 */
	public static int maxDamageOf(ItemStack stack) {
		CompoundTag derived = GearTags.read(stack, GearTags.DERIVED);
		int cached = derived.getInt("maxDamage");
		return cached > 0 ? cached : computeMaxDamage(stack);
	}

	// Vanilla's durability bar divides by the per-item constant; use the stack's value.
	@Override
	public int getBarWidth(@NotNull ItemStack stack) {
		return Math.round(13.0F - (float) stack.getDamageValue() * 13.0F / (float) maxDamageOf(stack));
	}

	@Override
	public int getBarColor(@NotNull ItemStack stack) {
		float max = maxDamageOf(stack);
		float f = Math.max(0.0F, (max - (float) stack.getDamageValue()) / max);
		return net.minecraft.util.Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
	}

	/**
	 * True when two stacks are the same piece of gear apart from the mod's own
	 * {@link GearTags#ROOT} NBT, which is each loader's cue to skip the hand re-equip
	 * animation (Forge {@code shouldCauseReequipAnimation}, Fabric
	 * {@code allowNbtUpdateAnimation}). That tag changes on nearly every use - XP per
	 * block and per hit, trait state - and vanilla replays the animation for any NBT
	 * change on the held stack. Durability is deliberately still compared: a tool that
	 * takes damage dips exactly like a vanilla tool does on this version. Two different
	 * pieces of gear differ in name or attributes, so swapping between them still animates.
	 */
	public static boolean sameApartFromToolMod(ItemStack oldStack, ItemStack newStack) {
		if (oldStack == newStack) {
			return true;
		}
		if (!ItemStack.isSameItem(oldStack, newStack) || oldStack.getCount() != newStack.getCount()) {
			return false;
		}

		CompoundTag oldTag = oldStack.getTag();
		CompoundTag newTag = newStack.getTag();
		if (oldTag == null || newTag == null) {
			return oldTag == newTag;
		}
		for (String key : oldTag.getAllKeys()) {
			if (!key.equals(GearTags.ROOT) && !oldTag.get(key).equals(newTag.get(key))) {
				return false;
			}
		}
		for (String key : newTag.getAllKeys()) {
			if (!key.equals(GearTags.ROOT) && !oldTag.contains(key)) {
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

		// Stamps gear that reached the world without its derived stats (hand-built or
		// edited stacks); a no-op once the DERIVED marker exists.
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
	public void appendHoverText(ItemStack item, @Nullable Level level, java.util.List<Component> tipList,
			TooltipFlag pTooltipFlag) {
		LootTooltips.appendHoverText(item, level, tipList::add,
				(type, tips) -> appendStatLines(item, type, tips));
	}
}
