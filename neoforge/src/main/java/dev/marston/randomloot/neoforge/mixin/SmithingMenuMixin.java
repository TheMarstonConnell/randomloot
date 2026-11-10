package dev.marston.randomloot.neoforge.mixin;

import dev.marston.randomloot.recipes.TraitAdditionRecipe;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Trait recipes cost more than the one addition item vanilla's onTake consumes and
 * grant the crafted-trait advancements, both at the head of it, while the inputs are
 * still whole. onTake runs once for a plain click and once for a shift-click (with an
 * empty carried stack, which is why this cannot hang off the result item).
 *
 * <p>26.x routes this through PlayerEvent.ItemSmithedEvent; NeoForge 21.1 has no such
 * event, so it carries the same mixin Fabric does.
 */
@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin {

    @Inject(method = "onTake", at = @At("HEAD"))
    private void randomloot$traitRecipeTake(Player player, ItemStack carried, CallbackInfo ci) {
        TraitAdditionRecipe.onTake((SmithingMenu) (Object) this, player);
    }
}
