package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.advancements.ModCriteria;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.EntityKillModifier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class HaileysWrath extends AbstractModifier implements EntityKillModifier {
    @Override
    public String description() {
        return "Spawns a bee when the target is killed";
    }

    @Override
    public void onKill(ItemStack itemstack, LivingEntity victim, LivingEntity killer) {
        ServerLevel serverLevel = (ServerLevel) victim.level();

        // Spawn a bee at the victim's location
        BlockPos pos = victim.blockPosition();
        Entity bee = EntityTypes.BEE.create(serverLevel, null, pos, EntitySpawnReason.MOB_SUMMONED, false, false);
        if (bee != null) {
            bee.setPos(victim.getX(), victim.getY(), victim.getZ());
            serverLevel.addFreshEntity(bee);
            ModCriteria.traitUsed(killer, this);
        }
    }
}
