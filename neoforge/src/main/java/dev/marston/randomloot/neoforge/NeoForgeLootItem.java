package dev.marston.randomloot.neoforge;

import dev.marston.randomloot.loot.LootItem;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

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
}
