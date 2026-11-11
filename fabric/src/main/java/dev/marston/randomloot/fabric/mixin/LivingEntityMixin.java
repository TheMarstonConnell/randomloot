package dev.marston.randomloot.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.marston.randomloot.loot.modifiers.ArmorDispatcher;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fabric stand-in for Forge's LivingDamageEvent: the wearer-hurt armor traits before
 * the damage lands, and the armor XP/durability hook after it.
 *
 * <p>Wraps the actuallyHurt CALL SITES in hurt rather than injecting into
 * actuallyHurt itself: Player overrides actuallyHurt, so a HEAD injection into the
 * LivingEntity method never fires for players (the common case for armor). Both call
 * sites (the i-frames delta path and the fresh-hit path) are wrapped.
 *
 * <p>The post hook reports what the hit actually cost - health plus absorption - which
 * is what Fabric API's AFTER_DAMAGE event reports on 1.21+ (it does not exist for
 * 1.20.1). A shield block reaches actuallyHurt with 0 damage and so reports nothing.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @WrapOperation(method = "hurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void randomloot$wearerHurtTraits(LivingEntity instance, DamageSource source,
            float damage, Operation<Void> original) {
        float adjusted = ArmorDispatcher.onLivingDamagePre(instance, source, damage);

        float before = instance.getHealth() + instance.getAbsorptionAmount();
        original.call(instance, source, adjusted);
        float taken = before - (instance.getHealth() + instance.getAbsorptionAmount());

        if (taken > 0.0f) {
            ArmorDispatcher.onLivingDamagePost(instance, source, taken);
        }
    }
}
