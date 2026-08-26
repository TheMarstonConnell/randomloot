package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.NbtCompat;


import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class Pummeling extends AbstractModifier implements EntityHurtModifier {

    public Pummeling() {
        this.name = "Pummeling";
    }

    public Pummeling(String name) {
        this.name = name;
    }

    @Override
    public String tagName() {
        return "pummeling";
    }

    @Override
    public ChatFormatting color() {
        return ChatFormatting.DARK_RED;
    }

    @Override
    public String description() {
        return "Slams enemies into the ground";
    }

    @Override
    public Modifier fromNBT(CompoundTag tag) {
        return new Pummeling(NbtCompat.getStringOr(tag, NAME, "Pummeling"));
    }

    @Override
    public boolean forTool(ToolType type) {
        return isWeapon(type);
    }

    @Override
    public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
        if (hurtee.level().isClientSide()) {
            return false;
        }

        Level level = hurtee.level();
        Vec3 pos = hurtee.position();
        
        // Check if there's air 2 blocks below
        BlockPos twoBelow = BlockPos.containing(pos.x, pos.y - 2, pos.z);
        boolean airBelow = !level.getBlockState(twoBelow).isSolid();
        
        // If air below, slam through floor (1.5 blocks), otherwise partial slam (0.9 blocks)
        double slamDepth = airBelow ? 1.5 : 0.9;
        
        hurtee.teleportTo(pos.x, pos.y - slamDepth, pos.z);
        hurtee.setDeltaMovement(0, 0, 0);
        hurtee.hurtMarked = true;

        return false;
    }
}
