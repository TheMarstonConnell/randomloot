package dev.marston.randomloot.forge;

import dev.marston.randomloot.loot.LootArmorItem;
import dev.marston.randomloot.loot.LootGearItem;
import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/** LootArmorItem with Forge's stack-aware item extension hooks. */
public class ForgeLootArmorItem extends LootArmorItem {

    public ForgeLootArmorItem(Properties p) {
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

    /**
     * Per-stack equipment slot (26.x carries this in the EQUIPPABLE component).
     * Covers inventory equip, dispensers and mob pickup; null while rolling.
     */
    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return LootUtils.wearableSlot(stack);
    }
}
