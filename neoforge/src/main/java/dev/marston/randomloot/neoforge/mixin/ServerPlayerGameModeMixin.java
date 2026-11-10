package dev.marston.randomloot.neoforge.mixin;

import dev.marston.randomloot.loot.LootItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Reports the result of every server-side item use and block use, so a right-click
 * trait that deferred to the off-hand item learns whether that item actually did anything.
 * NeoForge's RightClickItem event fires before the use and cannot see its result,
 * so this is the one hook the mod wires by mixin here; Fabric carries the same one.
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

    @Inject(method = "useItem", at = @At("RETURN"))
    private void randomloot$afterUseItem(ServerPlayer player, Level level, ItemStack itemStack, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {
        LootItem.afterUseItem(player, hand, cir.getReturnValue());
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void randomloot$afterUseItemOn(ServerPlayer player, Level level, ItemStack itemStack, InteractionHand hand,
            BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        LootItem.afterUseItemOn(player, hand, cir.getReturnValue());
    }
}
