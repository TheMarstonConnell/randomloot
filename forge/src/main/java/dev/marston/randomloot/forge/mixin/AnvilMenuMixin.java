package dev.marston.randomloot.forge.mixin;

import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The anvil/book half of the enchant gate. 1.20.1's anvil asks
 * {@code Enchantment.canEnchant(ItemStack)}, which bottoms out in an EnchantmentCategory
 * {@code instanceof} check the single gear item can never pass (sharpness wants a
 * SwordItem) and has no per-item hook; Forge's canApplyAtEnchantingTable only covers the
 * table. Non-gear stacks keep the vanilla answer.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Redirect(method = "createResult", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/Enchantment;canEnchant(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean randomloot$gearEnchantGate(Enchantment enchantment, ItemStack stack) {
        if (LootUtils.isLootGear(stack)) {
            return LootUtils.gearAcceptsEnchantment(stack, enchantment);
        }
        return enchantment.canEnchant(stack);
    }
}
