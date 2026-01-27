package dev.marston.randomloot.loot.modifiers.hurter;

import java.util.List;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class HaileysWrath implements EntityHurtModifier {

    private String name;

    public HaileysWrath() {
        this.name = "Hailey's Wrath";
    }

    public HaileysWrath(String name) {
        this.name = name;
    }

    @Override
    public String tagName() {
        return "haileys_wrath";
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String color() {
        return ChatFormatting.GOLD.getName();
    }

    @Override
    public String description() {
        return "Spawns a bee when the target is killed";
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString(NAME, name);
        return tag;
    }

    @Override
    public Modifier fromNBT(CompoundTag tag) {
        return new HaileysWrath(tag.getStringOr(NAME, "Hailey's Wrath"));
    }

    @Override
    public Modifier clone() {
        return new HaileysWrath();
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

        // Check if the target is killed (health <= 0 after damage)
        if (hurtee.getHealth() <= 0 && level instanceof ServerLevel serverLevel) {
            // Spawn a bee at the target's location
            BlockPos pos = hurtee.blockPosition();
            Entity bee = EntityType.BEE.create(serverLevel, null, pos, EntitySpawnReason.MOB_SUMMONED, false, false);
            if (bee != null) {
                bee.setPos(hurtee.getX(), hurtee.getY(), hurtee.getZ());
                level.addFreshEntity(bee);
            }
        }

        return false;
    }
}
