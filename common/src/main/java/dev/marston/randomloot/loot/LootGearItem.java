package dev.marston.randomloot.loot;

import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.loot.modifiers.TraitDispatch;
import dev.marston.randomloot.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
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

	/**
	 * The stack's attributes, derived from its goodness, type and traits. Never asked of
	 * gear in its reveal roll: {@link LootUtils#refreshDerivedComponents} gives that none.
	 */
	public abstract ItemAttributeModifiers buildAttributeModifiers(ItemStack stack);

	/**
	 * The slot in which hold-style traits run, or {@code null} when they should not run
	 * at all (armor that isn't currently worn).
	 */
	@Nullable
	protected abstract EquipmentSlot holdSlot(ItemStack stack);

	/** The type-specific stat lines in the shift-expanded tooltip. */
	protected abstract void appendStatLines(ItemStack stack, ToolType type, Consumer<Component> tips);

	/**
	 * The mod's per-type enchant filter: {@code true}/{@code false} to decide, or
	 * {@code null} to defer to the loader's default check. Both gear items sit in every
	 * relevant {@code minecraft:enchantable/*} tag, so the per-type split has to happen
	 * here rather than in the tags. Never asked of gear in its reveal roll:
	 * {@link LootUtils#gearEnchantGate} refuses every enchantment for that.
	 */
	public abstract Boolean supportsEnchantmentCommon(ItemStack stack, Holder<Enchantment> enchantment);

	/** A datapack-editable enchantment group; see data/randomloot/tags/enchantment/. */
	protected static TagKey<Enchantment> enchantTag(String path) {
		return TagKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(RandomLoot.MODID, path));
	}

	/** One gray "Name: value" line of the shift-expanded tooltip, to two decimals. */
	protected static void statLine(Consumer<Component> tips, String key, String fallback, float value) {
		tips.accept(Component.translatableWithFallback(key, fallback, String.format("%.2f", value))
				.withStyle(ChatFormatting.GRAY));
	}

	/** Durability derived from goodness; stored as the vanilla MAX_DAMAGE component. */
	public static int computeMaxDamage(ItemStack stack) {
		return GearStats.maxDamage(LootUtils.getStats(stack), LootUtils.getToolType(stack));
	}

	@Override
	public @NotNull Component getName(@NotNull ItemStack stack) {
		if (LootUtils.isRolling(stack)) {
			return LootTooltips.rollingName();
		}
		return super.getName(stack);
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity holder, EquipmentSlot slot) {

		// Migrates items from before attributes/durability moved to data components
		// (they were dynamic NeoForge item-method overrides).
		LootUtils.migrateDerivedComponents(stack);

		// Runs in any slot, unlike the trait ticking below.
		if (LootUtils.tickRoll(stack, level, holder)) {
			return;
		}

		if (slot == holdSlot(stack)) {
			TraitDispatch.tick(stack, level, holder);
		}
	}

	@Override
	public void appendHoverText(ItemStack item, TooltipContext pContext, TooltipDisplay display,
			Consumer<Component> tipList, TooltipFlag pTooltipFlag) {
		LootTooltips.appendHoverText(item, Services.PLATFORM.tooltipLevel(pContext), tipList,
				(type, tips) -> appendStatLines(item, type, tips));
	}
}
