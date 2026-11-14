package dev.marston.randomloot.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.marston.randomloot.loot.modifiers.hurter.Soulbound;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Fabric stand-in for NeoForge's PlayerEvent.BreakSpeed (Soulbound owner mining bonus). */
@Mixin(Player.class)
public abstract class PlayerMixin {

    @ModifyReturnValue(method = "getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;)F", at = @At("RETURN"))
    private float randomloot$soulboundBreakSpeed(float original) {
        return Soulbound.modifyBreakSpeed((Player) (Object) this, original);
    }
}
