package dev.marston.randomloot.loot.modifiers.hurter;


import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class Executioner extends AbstractModifier implements EntityHurtModifier {

    private static final String LEVEL = "trait_level";
    private static final int MAX_LEVEL = 5;

    private int level;

    public Executioner() {
        this.name = "Executioner";
        this.level = 1;
    }

    public Executioner(String name, int level) {
        this.name = name;
        this.level = level;
    }

    @Override
    public Modifier clone() {
        return new Executioner();
    }

    @Override
    public boolean canLevel() {
        return level < MAX_LEVEL;
    }

    @Override
    public void levelUp() {
        this.level++;
    }

    private float getHealthThreshold() {
        // Level 1: 20%, Level 2: 25%, Level 3: 30%, Level 4: 35%, Level 5: 40%
        return 0.15f + (level * 0.05f);
    }

    @Override
    public String tagName() {
        return "executioner";
    }

    @Override
    public String name() {
        if (level == 1) {
            return this.name;
        }
        return this.name + " " + LootUtils.roman(level);
    }

    @Override
    public String color() {
        return ChatFormatting.DARK_RED.getName();
    }

    @Override
    public String description() {
        return "Instantly kills mobs below " + String.format("%.0f", getHealthThreshold() * 100) + "% health";
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString(NAME, name);
        tag.putInt(LEVEL, level);
        return tag;
    }

    @Override
    public Modifier fromNBT(CompoundTag tag) {
        return new Executioner(tag.getStringOr(NAME, "Executioner"), tag.getIntOr(LEVEL, tag.getIntOr("level", 1)));
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

        float healthPercent = hurtee.getHealth() / hurtee.getMaxHealth();
        
        if (healthPercent <= getHealthThreshold()) {
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
