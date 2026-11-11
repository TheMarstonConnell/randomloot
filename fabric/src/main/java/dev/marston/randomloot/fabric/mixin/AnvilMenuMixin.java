package dev.marston.randomloot.fabric.mixin;

import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric stand-in for Forge's AnvilUpdateEvent cancel: every Random
 * Tool/Armor piece is unique (type, traits, XP live on the stack), so
 * vanilla's combine-two-of-the-same-item recipe would destroy the right
 * item's identity and act as a cheap repair loop.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    /**
     * The anvil/book half of the enchant gate: vanilla asks Enchantment.canEnchant,
     * which bottoms out in an EnchantmentCategory instanceof check the single gear item
     * can never pass. Non-gear stacks keep the vanilla answer.
     */
    @Redirect(method = "createResult", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/Enchantment;canEnchant(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean randomloot$gearEnchantGate(Enchantment enchantment, ItemStack stack) {
        if (LootUtils.isLootGear(stack)) {
            return LootUtils.gearAcceptsEnchantment(stack, enchantment);
        }
        return enchantment.canEnchant(stack);
    }

    @Inject(method = "createResult", at = @At("TAIL"))
    private void randomloot$blockLootGearCombine(CallbackInfo ci) {
        AnvilMenu self = (AnvilMenu) (Object) this;
        ItemStack left = self.getSlot(0).getItem();
        ItemStack right = self.getSlot(1).getItem();

        if (LootUtils.isLootGear(left) && LootUtils.isLootGear(right)) {
            self.getSlot(2).set(ItemStack.EMPTY);
        }
    }
}
