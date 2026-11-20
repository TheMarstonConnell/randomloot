package dev.marston.randomloot.loot.modifiers.holders;

import java.util.List;

import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class Hunter extends AbstractModifier implements HoldModifier {
    private static final double SEARCH_RADIUS = 10.0;

    @Override
    public String description() {
        return "Nearby hostile mobs get the glowing effect";
    }

    @Override
    public void hold(ItemStack stack, Level level, Entity holder) {
        // Create search box around holder
        AABB searchBox = new AABB(
            holder.getX() - SEARCH_RADIUS, holder.getY() - SEARCH_RADIUS, holder.getZ() - SEARCH_RADIUS,
            holder.getX() + SEARCH_RADIUS, holder.getY() + SEARCH_RADIUS, holder.getZ() + SEARCH_RADIUS
        );

        // Find all hostile mobs (Monster class covers most hostile mobs)
        List<Monster> hostileMobs = level.getEntitiesOfClass(Monster.class, searchBox);

        // Apply glowing effect to each hostile mob
        for (Monster mob : hostileMobs) {
            // Short duration (40 ticks = 2 seconds) so it fades when player moves away
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
        }
    }
}
