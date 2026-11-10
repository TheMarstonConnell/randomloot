package dev.marston.randomloot.neoforge;

import dev.marston.randomloot.loot.LootArmorItem;
import dev.marston.randomloot.loot.LootGearItem;
import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
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

    /** tool_mod changes on nearly every use (XP); it alone should not replay the re-equip animation. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !LootGearItem.sameApartFromToolMod(oldStack, newStack);
    }

    /**
     * Per-stack equipment slot (26.x carries this in the EQUIPPABLE component).
     * Covers inventory equip, dispensers and mob pickup; null while rolling.
     */
    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return LootUtils.wearableSlot(stack);
    }
}
