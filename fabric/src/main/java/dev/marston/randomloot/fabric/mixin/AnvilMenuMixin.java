package dev.marston.randomloot.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.marston.randomloot.platform.GameHooks;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric's shim for ANVIL_COMBINE, where Forge cancels its AnvilUpdateEvent: after
 * vanilla worked out a result, empty it when common says this pair must not combine.
 * Also the anvil half of its shim for ENCHANT_GATE.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    /**
     * The anvil/book half of the enchant gate: vanilla asks Enchantment.canEnchant,
     * which bottoms out in an EnchantmentCategory instanceof check the single gear item
     * can never pass. Non-gear stacks keep the vanilla answer. A wrap rather than a
     * redirect so it stacks with other mods that touch the same call.
     */
    @WrapOperation(method = "createResult", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/Enchantment;canEnchant(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean randomloot$gearEnchantGate(Enchantment enchantment, ItemStack stack, Operation<Boolean> original) {
        return GameHooks.enchantGate(stack, enchantment, true, false, () -> original.call(enchantment, stack));
    }

    @Inject(method = "createResult", at = @At("TAIL"))
    private void randomloot$blockLootGearCombine(CallbackInfo ci) {
        AnvilMenu self = (AnvilMenu) (Object) this;
        ItemStack left = self.getSlot(0).getItem();
        ItemStack right = self.getSlot(1).getItem();

        if (GameHooks.blocksAnvilCombine(left, right)) {
            self.getSlot(2).set(ItemStack.EMPTY);
        }
    }
}
