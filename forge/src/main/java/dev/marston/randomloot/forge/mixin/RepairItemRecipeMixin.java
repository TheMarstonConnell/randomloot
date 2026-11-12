package dev.marston.randomloot.forge.mixin;

import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps Random Loot gear out of crafting-grid repair. Vanilla answers two damaged
 * stacks of the same item with a brand-new stack of that item, which for gear means a
 * blank piece: no type, traits or XP, and both originals gone. (26.x blocks this with
 * NeoForge's isCombineRepairable; 1.20.1 has no hook that covers both loaders.)
 */
@Mixin(RepairItemRecipe.class)
public abstract class RepairItemRecipeMixin {

    @Inject(method = "matches(Lnet/minecraft/world/inventory/CraftingContainer;Lnet/minecraft/world/level/Level;)Z",
            at = @At("HEAD"), cancellable = true)
    private void randomloot$noGearRepair(CraftingContainer container, Level level,
            CallbackInfoReturnable<Boolean> cir) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (LootUtils.isLootGear(container.getItem(i))) {
                cir.setReturnValue(false);
                return;
            }
        }
    }
}
