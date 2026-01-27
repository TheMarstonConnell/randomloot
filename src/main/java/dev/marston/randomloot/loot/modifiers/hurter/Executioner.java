package dev.marston.randomloot.loot.modifiers.hurter;

import java.util.List;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class Executioner implements EntityHurtModifier {

    private static final float HEALTH_THRESHOLD = 0.30f;
    
    private String name;

    public Executioner() {
        this.name = "Executioner";
    }

    public Executioner(String name) {
        this.name = name;
    }

    @Override
    public Modifier clone() {
        return new Executioner();
    }

    @Override
    public String tagName() {
        return "executioner";
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String color() {
        return ChatFormatting.DARK_RED.getName();
    }

    @Override
    public String description() {
        return "Instantly kills mobs below 30% health";
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString(NAME, name);
        return tag;
    }

    @Override
    public Modifier fromNBT(CompoundTag tag) {
        return new Executioner(tag.getStringOr(NAME, "Executioner"));
    }

    @Override
    public boolean forTool(ToolType type) {
        return type.equals(ToolType.SWORD) || type.equals(ToolType.AXE);
    }

    @Override
    public void writeToLore(List<Component> list, boolean shift) {
        MutableComponent comp = Modifier.makeComp(this.name(), this.color());
        list.add(comp);
    }

    @Override
    public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
        if (hurtee.level().isClientSide()) {
            return false;
        }

        float healthPercent = hurtee.getHealth() / hurtee.getMaxHealth();
        
        if (healthPercent <= HEALTH_THRESHOLD) {
            if (hurter instanceof Player player) {
                hurtee.hurt(hurter.damageSources().playerAttack(player), Float.MAX_VALUE);
            } else {
                hurtee.hurt(hurter.damageSources().mobAttack(hurter), Float.MAX_VALUE);
            }
            
            if (hurtee.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                    ParticleTypes.CRIT,
                    hurtee.getX(), hurtee.getY() + hurtee.getBbHeight() / 2, hurtee.getZ(),
                    20, 0.5, 0.5, 0.5, 0.1
                );
                
                serverLevel.playSound(
                    null, hurtee.getX(), hurtee.getY(), hurtee.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS,
                    1.0f, 0.5f
                );
            }
        }

        return false;
    }
}
