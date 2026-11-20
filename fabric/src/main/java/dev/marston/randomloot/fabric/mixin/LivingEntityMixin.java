package dev.marston.randomloot.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.marston.randomloot.fabric.DamageTakenCarrier;
import dev.marston.randomloot.platform.GameHooks;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric's shim for DAMAGE_PRE and DAMAGE_POST, where Forge has LivingDamageEvent.
 *
 * <p><b>DAMAGE_PRE.</b> getDamageAfterMagicAbsorb is the last reduction in actuallyHurt
 * (armor, then protection enchantments and Resistance), so the value its call returns is
 * the damage about to be taken. This hook used to wrap the actuallyHurt call in hurt
 * instead, which handed the traits the hit before any reduction: Thorny reflected a
 * share of the raw hit on Fabric and of the reduced one on Forge.
 *
 * <p>It wraps the call, not the method. Witch overrides getDamageAfterMagicAbsorb and
 * takes her own 85% off magic after super returns; an injection into the method runs
 * before that, the call's result comes after it.
 *
 * <p>Player has its own actuallyHurt and never runs this one, so PlayerMixin wraps the
 * same call there. A wrap on one of the two silently skips the other's entities.
 *
 * <p><b>DAMAGE_POST.</b> Fabric API for 1.20.1 has no event after damage. The amount
 * DAMAGE_PRE answered is kept on the entity and handed over at the tail of hurt, which
 * is past the point where a dying entity has died. Every path that gets as far as
 * actuallyHurt reaches that tail, and a hit a trait deals back to this entity from
 * inside DAMAGE_PRE has run its whole hurt before the amount is stored.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements DamageTakenCarrier {

    @Unique
    private float randomloot$damageTaken;

    @Override
    public void randomloot$setDamageTaken(float damage) {
        this.randomloot$damageTaken = damage;
    }

    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float randomloot$wearerHurtTraits(LivingEntity instance, DamageSource source, float damage,
            Operation<Float> original) {
        float taken = GameHooks.damagePre(instance, source, original.call(instance, source, damage));
        this.randomloot$damageTaken = taken;
        return taken;
    }

    @Inject(method = "hurt", at = @At("TAIL"))
    private void randomloot$afterHurt(DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        float taken = this.randomloot$damageTaken;
        this.randomloot$damageTaken = 0.0f;
        GameHooks.damagePost((LivingEntity) (Object) this, source, taken);
    }
}
