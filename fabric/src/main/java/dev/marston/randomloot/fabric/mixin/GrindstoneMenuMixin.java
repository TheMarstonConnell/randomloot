package dev.marston.randomloot.fabric.mixin;

import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The grindstone half of "two pieces of gear never combine" (the anvil half is
 * AnvilMenuMixin / AnvilUpdateEvent). With two stacks of the same item vanilla keeps
 * the top one, merges durability into it and eats the bottom one, destroying a unique
 * piece for a cheap repair. A single piece still disenchants as normal.
 */
@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin {

    @Inject(method = "createResult", at = @At("TAIL"))
    private void randomloot$blockLootGearCombine(CallbackInfo ci) {
        GrindstoneMenu self = (GrindstoneMenu) (Object) this;
        if (LootUtils.isLootGear(self.getSlot(0).getItem()) && LootUtils.isLootGear(self.getSlot(1).getItem())) {
            self.getSlot(2).set(ItemStack.EMPTY);
            self.broadcastChanges();
        }
    }
}
