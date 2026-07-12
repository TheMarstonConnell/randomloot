package dev.marston.randomloot.fabric.mixin;

import dev.marston.randomloot.loot.modifiers.ArmorDispatcher;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Fabric stand-in for NeoForge's LivingDamageEvent.Pre (wearer-hurt armor traits). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @ModifyVariable(method = "actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V",
            at = @At("HEAD"), argsOnly = true)
    private float randomloot$wearerHurtTraits(float damage, ServerLevel level, DamageSource source) {
        return ArmorDispatcher.onLivingDamagePre((LivingEntity) (Object) this, source, damage);
    }
}
