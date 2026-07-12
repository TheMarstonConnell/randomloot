package dev.marston.randomloot.neoforge;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.platform.ToolAction;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemInstance;
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

    @Override
    public boolean isCombineRepairable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canPerformAction(ItemInstance stack, ItemAbility itemAbility) {
        if (!(stack instanceof ItemStack itemStack)) {
            return false;
        }

        for (ToolAction action : ToolAction.values()) {
            if (NeoForgePlatformHelper.toItemAbility(action) == itemAbility) {
                return canPerform(itemStack, action);
            }
        }
        return false;
    }
}
