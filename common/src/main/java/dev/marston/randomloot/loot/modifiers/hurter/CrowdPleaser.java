package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

public class CrowdPleaser extends AbstractModifier implements EntityHurtModifier {
    private static final double SEARCH_RADIUS = 10.0;
    private static final float BONUS_PER_MOB = 0.05f; // 5% per mob
    private static final float MAX_BONUS = 0.50f; // 50% max bonus

    @Override
    public String description() {
        return "Deals bonus damage based on nearby mobs of the same type";
    }

    @Override
    public void hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
        EntityType<?> targetType = hurtee.getType();

        // Count nearby entities of the same type (excluding the target itself)
        int nearby = hurtee.level().getEntitiesOfClass(LivingEntity.class,
            AABB.ofSize(hurtee.position(), 2 * SEARCH_RADIUS, 2 * SEARCH_RADIUS, 2 * SEARCH_RADIUS),
            e -> e.getType().equals(targetType) && e != hurtee && e.isAlive()).size();

        dealBonusDamage(hurtee, hurter, attackDamage(itemstack) * Math.min(nearby * BONUS_PER_MOB, MAX_BONUS));
    }
}
