package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class Clunky extends AbstractModifier implements EntityHurtModifier, HoldModifier {
    private static final double KNOCKBACK_STRENGTH = 0.4;

    @Override
    public String description() {
        return "Applies slowness to holder but extra knockback on hit";
    }

    @Override
    public void hold(ItemStack stack, Level level, Entity holder) {
        if (!(holder instanceof LivingEntity living)) {
            return;
        }

        living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, false, false));
    }

    @Override
    public void hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
        Vec3 knockbackDir = hurtee.position().subtract(hurter.position()).normalize();
        
        Vec3 currentVelocity = hurtee.getDeltaMovement();
        hurtee.setDeltaMovement(
            currentVelocity.x + knockbackDir.x * KNOCKBACK_STRENGTH,
            currentVelocity.y + 0.3,
            currentVelocity.z + knockbackDir.z * KNOCKBACK_STRENGTH
        );
        
        hurtee.syncVelocity = true;
    }
}
