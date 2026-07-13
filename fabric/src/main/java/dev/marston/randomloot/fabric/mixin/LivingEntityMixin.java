package dev.marston.randomloot.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.marston.randomloot.loot.modifiers.ArmorDispatcher;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fabric stand-in for NeoForge's LivingDamageEvent.Pre (wearer-hurt armor traits).
 *
 * <p>Wraps the actuallyHurt CALL SITES in hurtServer rather than injecting into
 * actuallyHurt itself: Player overrides actuallyHurt, so a HEAD injection into
 * the LivingEntity method never fires for players (the common case for armor).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @WrapOperation(method = "hurtServer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void randomloot$wearerHurtTraits(LivingEntity instance, ServerLevel level, DamageSource source,
            float damage, Operation<Void> original) {
        original.call(instance, level, source, ArmorDispatcher.onLivingDamagePre(instance, source, damage));
    }
}
