package dev.marston.randomloot.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The enchanting-table half of the enchant gate (Forge has the
 * canApplyAtEnchantingTable item hook for this). The table builds its candidate list by
 * asking each enchantment's EnchantmentCategory whether it fits the item, an
 * {@code instanceof} check the single gear item can never pass. Non-gear stacks keep the
 * vanilla answer.
 */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

    @WrapOperation(method = "getAvailableEnchantmentResults", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/EnchantmentCategory;canEnchant(Lnet/minecraft/world/item/Item;)Z"))
    private static boolean randomloot$gearEnchantGate(EnchantmentCategory category, Item item,
            Operation<Boolean> original, @Local(argsOnly = true) ItemStack stack, @Local Enchantment enchantment) {
        if (LootUtils.isLootGear(stack)) {
            return LootUtils.gearAcceptsEnchantment(stack, enchantment);
        }
        return original.call(category, item);
    }
}
