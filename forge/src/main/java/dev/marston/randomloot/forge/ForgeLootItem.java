package dev.marston.randomloot.forge;

import dev.marston.randomloot.loot.LootGearItem;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.common.ToolAction;

/**
 * LootItem with Forge's stack-aware item extension hooks. The stack-aware
 * {@code isCorrectToolForDrops(ItemStack, BlockState)} needs no override here: LootItem
 * already declares that exact signature, which is IForgeItem's.
 */
public class ForgeLootItem extends LootItem {

    public ForgeLootItem(Properties p) {
        super(p);
    }

    /** The enchanting table's per-item hook; the anvil path is mixin/AnvilMenuMixin. */
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return LootUtils.gearAcceptsEnchantment(stack, enchantment);
    }

    /** The mod's NBT changes on nearly every use (XP); it alone should not replay the re-equip animation. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !LootGearItem.sameApartFromToolMod(oldStack, newStack);
    }

    /** Per-stack durability: vanilla 1.20.1 reads a per-item constant (26.x has the MAX_DAMAGE component). */
    @Override
    public int getMaxDamage(ItemStack stack) {
        return LootGearItem.maxDamageOf(stack);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        dev.marston.randomloot.platform.ToolAction action = ForgePlatformHelper.fromForgeAction(toolAction);
        return action != null && canPerform(stack, action);
    }
}
