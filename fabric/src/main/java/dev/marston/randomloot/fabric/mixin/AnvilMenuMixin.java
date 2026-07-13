package dev.marston.randomloot.fabric.mixin;

import dev.marston.randomloot.loot.LootArmorItem;
import dev.marston.randomloot.loot.LootItem;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric stand-in for NeoForge's isCombineRepairable=false: every Random
 * Tool/Armor piece is unique (type, traits, XP live on the stack), so
 * vanilla's combine-two-of-the-same-item recipe would destroy the right
 * item's identity and act as a cheap repair loop.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Inject(method = "createResult", at = @At("TAIL"))
    private void randomloot$blockLootGearCombine(CallbackInfo ci) {
        AnvilMenu self = (AnvilMenu) (Object) this;
        ItemStack left = self.getSlot(0).getItem();
        ItemStack right = self.getSlot(1).getItem();

        if (isLootGear(left) && isLootGear(right)) {
            self.getSlot(2).set(ItemStack.EMPTY);
        }
    }

    private static boolean isLootGear(ItemStack stack) {
        return stack.getItem() instanceof LootItem || stack.getItem() instanceof LootArmorItem;
    }
}
