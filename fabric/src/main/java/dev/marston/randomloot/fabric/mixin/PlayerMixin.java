package dev.marston.randomloot.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.marston.randomloot.platform.GameHooks;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fabric stand-ins for NeoForge's PlayerEvent.BreakSpeed (Soulbound owner mining bonus)
 * and, for players, LivingDamageEvent.Pre (wearer-hurt armor traits).
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    @ModifyReturnValue(method = "getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;)F", at = @At("RETURN"))
    private float randomloot$soulboundBreakSpeed(float original) {
        return GameHooks.breakSpeed((Player) (Object) this, original);
    }

    /**
     * LivingEntityMixin's wearer-hurt hook, repeated: Player re-implements actuallyHurt
     * without calling super, so the wrap on LivingEntity's copy never runs for a player.
     */
    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float randomloot$playerWearerHurtTraits(Player instance, DamageSource source, float damage,
            Operation<Float> original) {
        return GameHooks.damagePre(instance, source, original.call(instance, source, damage));
    }
}
