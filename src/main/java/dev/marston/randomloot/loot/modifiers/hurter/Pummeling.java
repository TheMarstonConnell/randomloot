package dev.marston.randomloot.loot.modifiers.hurter;


import dev.marston.randomloot.loot.LootItem.ToolType;
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

    private static final String NAME = "name";

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
    public String color() {
        return ChatFormatting.DARK_RED.getName();
    }

    @Override
    public String description() {
        return "Slams enemies into the ground";
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString(NAME, name);
        return tag;
    }

    @Override
    public Modifier fromNBT(CompoundTag tag) {
        return new Pummeling(tag.getStringOr(NAME, "Pummeling"));
    }

    @Override
    public Modifier clone() {
        return new Pummeling();
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
