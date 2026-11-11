package dev.marston.randomloot.fabric.mixin;

import dev.marston.randomloot.loot.LootGearItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Per-stack max durability for Random Loot gear. Vanilla 1.20.1 answers
 * {@code ItemStack.getMaxDamage()} from a per-item constant; Forge has a stack-aware
 * {@code Item#getMaxDamage(ItemStack)} to override, Fabric has nothing, hence this.
 * (26.x and 1.21.1 carry the value in the MAX_DAMAGE data component instead.)
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "getMaxDamage", at = @At("HEAD"), cancellable = true)
    private void randomloot$gearMaxDamage(CallbackInfoReturnable<Integer> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (self.getItem() instanceof LootGearItem) {
            cir.setReturnValue(LootGearItem.maxDamageOf(self));
        }
    }
}
