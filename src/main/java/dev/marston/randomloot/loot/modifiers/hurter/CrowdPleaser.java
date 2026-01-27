package dev.marston.randomloot.loot.modifiers.hurter;

import java.util.List;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class CrowdPleaser implements EntityHurtModifier {

    private static final String NAME = "name";
    private static final double SEARCH_RADIUS = 10.0;
    private static final float BONUS_PER_MOB = 0.05f; // 5% per mob
    private static final float MAX_BONUS = 0.50f; // 50% max bonus
    
    private String name;

    public CrowdPleaser() {
        this.name = "Crowd Pleaser";
    }

    public CrowdPleaser(String name) {
        this.name = name;
    }

    @Override
    public String tagName() {
        return "crowd_pleaser";
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String color() {
        return ChatFormatting.LIGHT_PURPLE.getName();
    }

    @Override
    public String description() {
        return "Deals bonus damage based on nearby mobs of the same type";
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString(NAME, name);
        return tag;
    }

    @Override
    public Modifier fromNBT(CompoundTag tag) {
        return new CrowdPleaser(tag.getStringOr(NAME, "Crowd Pleaser"));
    }

    @Override
    public Modifier clone() {
        return new CrowdPleaser();
    }

    @Override
    public boolean forTool(ToolType type) {
        return type.equals(ToolType.SWORD) || type.equals(ToolType.AXE);
    }

    @Override
    public void writeToLore(List<Component> list, boolean shift) {
        list.add(Modifier.makeComp(this.name(), this.color()));
    }

    @Override
    public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
        Level level = hurtee.level();
        if (level.isClientSide()) {
            return false;
        }

        // Get the type of entity being attacked
        EntityType<?> targetType = hurtee.getType();
        
        // Create bounding box for search area
        AABB searchBox = new AABB(
            hurtee.getX() - SEARCH_RADIUS, hurtee.getY() - SEARCH_RADIUS, hurtee.getZ() - SEARCH_RADIUS,
            hurtee.getX() + SEARCH_RADIUS, hurtee.getY() + SEARCH_RADIUS, hurtee.getZ() + SEARCH_RADIUS
        );
        
        // Count nearby entities of the same type (excluding the target itself)
        List<? extends LivingEntity> nearbyEntities = level.getEntitiesOfClass(
            LivingEntity.class, 
            searchBox, 
            entity -> entity.getType().equals(targetType) && entity != hurtee && entity.isAlive()
        );
        
        int nearbyCount = nearbyEntities.size();
        
        if (nearbyCount > 0) {
            // Calculate bonus damage
            float bonusPercent = Math.min(nearbyCount * BONUS_PER_MOB, MAX_BONUS);
            float baseDamage = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));
            float bonusDamage = baseDamage * bonusPercent;
            
            // Apply bonus damage
            if (hurter instanceof Player player) {
                hurtee.hurt(hurter.damageSources().playerAttack(player), bonusDamage);
            } else {
                hurtee.hurt(hurter.damageSources().mobAttack(hurter), bonusDamage);
            }
        }

        return false;
    }
}
