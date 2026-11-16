package dev.marston.randomloot.fabric.mixin;

import dev.marston.randomloot.platform.GameHooks;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric's shim for ANVIL_COMBINE, where NeoForge cancels its AnvilUpdateEvent: after
 * vanilla worked out a result, empty it when common says this pair must not combine.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

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
