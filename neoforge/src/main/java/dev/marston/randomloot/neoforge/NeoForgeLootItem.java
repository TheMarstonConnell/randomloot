package dev.marston.randomloot.neoforge;

import dev.marston.randomloot.loot.LootGearItem;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.platform.ToolAction;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.common.ItemAbility;

/** LootItem with NeoForge's item extension hooks (abilities, enchant filtering, anvil-combine block). */
public class NeoForgeLootItem extends LootItem {

    public NeoForgeLootItem(Properties p) {
        super(p);
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        Boolean common = supportsEnchantmentCommon(stack, enchantment);
        return common != null ? common : super.supportsEnchantment(stack, enchantment);
    }

    /** tool_mod changes on nearly every use (XP); it alone should not replay the re-equip animation. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !LootGearItem.sameApartFromToolMod(oldStack, newStack);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        ToolAction action = NeoForgePlatformHelper.fromItemAbility(itemAbility);
        return action != null && canPerform(stack, action);
    }
}
