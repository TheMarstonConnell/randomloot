package dev.marston.randomloot.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
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
 * getDamageAfterMagicAbsorb is the last of those reductions and only actuallyHurt calls
 * it, so its return value is the same number. Hooking the method rather than its call
 * sites covers Player too: Player re-implements actuallyHurt without calling super, but
 * it inherits this.
 *
 * <p>The hook used to wrap the actuallyHurt call in hurtServer instead, which handed the
 * traits the hit before any reduction: Thorny reflected a share of the raw hit on Fabric
 * and of the damage taken on NeoForge.
 *
 * <p>One difference remains: a subclass that reduces the result of super further (Witch,
 * against magic) does so after this hook, where NeoForge's event comes after it.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @ModifyReturnValue(method = "getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F",
            at = @At("RETURN"))
    private float randomloot$wearerHurtTraits(float original, DamageSource source, float damage) {
        return ArmorDispatcher.onLivingDamagePre((LivingEntity) (Object) this, source, original);
    }
}
