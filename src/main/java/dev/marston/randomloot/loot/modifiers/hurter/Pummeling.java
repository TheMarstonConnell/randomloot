package dev.marston.randomloot.loot.modifiers.hurter;

import java.util.List;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class Pummeling implements EntityHurtModifier {

    private static final String NAME = "name";
    private String name;

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
    public String name() {
        return this.name;
    }

    @Override
    public String color() {
        return ChatFormatting.GRAY.getName();
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
        return type.equals(ToolType.SWORD) || type.equals(ToolType.AXE);
    }

    @Override
    public void writeToLore(List<Component> list, boolean shift) {
        list.add(Modifier.makeComp(this.name(), this.color()));
    }

    @Override
    public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
        if (hurtee.level().isClientSide()) {
            return false;
        }

        // Get current velocity and add downward force
        Vec3 currentVelocity = hurtee.getDeltaMovement();

        // Push the entity down by adding negative Y velocity (-0.5 blocks worth of force)
        // Also add a small amount of the attacker's facing direction for realism
        Vec3 lookVec = hurter.getLookAngle();
        double downwardForce = -0.5;
        double horizontalForce = 0.1;

        hurtee.setDeltaMovement(
            currentVelocity.x + (lookVec.x * horizontalForce),
            downwardForce,
            currentVelocity.z + (lookVec.z * horizontalForce)
        );

        // Mark velocity as changed so it syncs to client
        hurtee.hurtMarked = true;

        return false;
    }
}
