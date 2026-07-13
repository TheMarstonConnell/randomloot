package dev.marston.randomloot.neoforge;

import dev.marston.randomloot.loot.LootArmorItem;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/** LootArmorItem with NeoForge's item extension hooks (enchant filtering, anvil-combine block). */
public class NeoForgeLootArmorItem extends LootArmorItem {

    public NeoForgeLootArmorItem(Properties p) {
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
