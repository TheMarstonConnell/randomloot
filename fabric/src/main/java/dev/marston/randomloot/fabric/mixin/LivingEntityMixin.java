package dev.marston.randomloot.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.marston.randomloot.loot.modifiers.ArmorDispatcher;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fabric stand-in for NeoForge's LivingDamageEvent.Pre (wearer-hurt armor traits).
 *
 * <p>NeoForge fires that event inside actuallyHurt once armor, protection enchantments
 * and Resistance have reduced the hit, and before absorption hearts soak any of it.
 * getDamageAfterMagicAbsorb is the last of those reductions, so its result is the same
 * number. This hook used to wrap the actuallyHurt call in hurtServer instead, which
 * handed the traits the hit before any reduction: Thorny reflected a share of the raw
 * hit on Fabric and of the damage taken on NeoForge.
 *
 * <p>Player has its own actuallyHurt and never runs this one, so PlayerMixin wraps the
 * same call there. A wrap on one of the two silently skips the other's entities.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float randomloot$wearerHurtTraits(LivingEntity instance, DamageSource source, float damage,
            Operation<Float> original) {
        return ArmorDispatcher.onLivingDamagePre(instance, source, original.call(instance, source, damage));
    }
}
