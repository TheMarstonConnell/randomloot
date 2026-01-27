package dev.marston.randomloot.loot.modifiers.hurter;

import java.util.List;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class Clunky implements EntityHurtModifier, HoldModifier {

    private static final double KNOCKBACK_STRENGTH = 1.5;
    
    private String name;

    public Clunky() {
        this.name = "Clunky";
    }

    public Clunky(String name) {
        this.name = name;
    }

    @Override
    public String tagName() {
        return "clunky";
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String color() {
        return ChatFormatting.DARK_GRAY.getName();
    }

    @Override
    public String description() {
        return "Applies slowness to holder but extra knockback on hit";
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString(NAME, name);
        return tag;
    }

    @Override
    public Modifier fromNBT(CompoundTag tag) {
        return new Clunky(tag.getStringOr(NAME, "Clunky"));
    }

    @Override
    public Modifier clone() {
        return new Clunky();
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
    public void hold(ItemStack stack, Level level, Entity holder) {
        if (!(holder instanceof LivingEntity living)) {
            return;
        }

        living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, false, false));
    }

    @Override
    public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
        if (hurtee.level().isClientSide()) {
            return false;
        }

        Vec3 knockbackDir = hurtee.position().subtract(hurter.position()).normalize();
        
        Vec3 currentVelocity = hurtee.getDeltaMovement();
        hurtee.setDeltaMovement(
            currentVelocity.x + knockbackDir.x * KNOCKBACK_STRENGTH,
            currentVelocity.y + 0.3,
            currentVelocity.z + knockbackDir.z * KNOCKBACK_STRENGTH
        );
        
        hurtee.hurtMarked = true;

        return false;
    }
}
