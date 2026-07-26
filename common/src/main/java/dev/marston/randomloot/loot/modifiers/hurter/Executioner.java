package dev.marston.randomloot.loot.modifiers.hurter;


import dev.marston.randomloot.advancements.ModCriteria;
import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.LeveledModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class Executioner extends LeveledModifier implements EntityHurtModifier {

    public Executioner() {
        this("Executioner", 1);
    }

    public Executioner(String name, int level) {
        this.name = name;
        this.level = level;
    }

    @Override
    protected int minLevel() {
        return 1;
    }

    @Override
    protected int maxLevel() {
        return 5;
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
    public ChatFormatting color() {
        return ChatFormatting.DARK_RED;
    }

    @Override
    public String description() {
        return "Instantly kills mobs below " + String.format("%.0f", getHealthThreshold() * 100) + "% health";
    }

    @Override
    public Modifier fromNBT(CompoundTag tag) {
        return new Executioner(tag.getStringOr(NAME, "Executioner"), ModifierConstants.getLevel(tag, 1));
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
            dealBonusDamage(hurtee, hurter, Float.MAX_VALUE);

            ModCriteria.traitUsed(hurter, this);

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
