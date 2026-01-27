package dev.marston.randomloot.loot.modifiers.holders;

import java.util.List;
import java.util.Random;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.StatsModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class Chaotic implements HoldModifier, StatsModifier, EntityHurtModifier {

    private static final String LAST_UPDATE = "lastUpdate";
    private static final String CURRENT_STATE = "currentState";
    private static final long UPDATE_INTERVAL = 100; // 5 seconds = 100 ticks
    private static final float MODIFIER_AMOUNT = 0.50f; // 50%

    private String name;
    private long lastUpdateTick;
    private int currentState; // -1 = debuff, 0 = neutral, 1 = buff
    private Random random = new Random();

    public Chaotic() {
        this.name = "Chaotic";
        this.lastUpdateTick = 0;
        this.currentState = 0;
    }

    public Chaotic(String name, long lastUpdateTick, int currentState) {
        this.name = name;
        this.lastUpdateTick = lastUpdateTick;
        this.currentState = currentState;
    }

    @Override
    public String tagName() {
        return "chaotic";
    }

    @Override
    public String name() {
        String suffix = "";
        if (currentState > 0) {
            suffix = " (+)";
        } else if (currentState < 0) {
            suffix = " (-)";
        }
        return this.name + suffix;
    }

    @Override
    public String color() {
        return ChatFormatting.DARK_PURPLE.getName();
    }

    @Override
    public String description() {
        return "Stats randomly fluctuate every 5 seconds";
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString(NAME, name);
        tag.putLong(LAST_UPDATE, lastUpdateTick);
        tag.putInt(CURRENT_STATE, currentState);
        return tag;
    }

    @Override
    public Modifier fromNBT(CompoundTag tag) {
        String name = tag.getStringOr(NAME, "Chaotic");
        long lastUpdate = tag.getLongOr(LAST_UPDATE, 0L);
        int state = tag.getIntOr(CURRENT_STATE, 0);
        return new Chaotic(name, lastUpdate, state);
    }

    @Override
    public Modifier clone() {
        return new Chaotic();
    }

    @Override
    public boolean forTool(ToolType type) {
        return true; // Works with all tool types
    }

    @Override
    public void writeToLore(List<Component> list, boolean shift) {
        MutableComponent comp = Modifier.makeComp(this.name(), this.color());
        list.add(comp);
    }

    private void updateState(ItemStack itemstack, long currentTick) {
        if (currentTick - lastUpdateTick >= UPDATE_INTERVAL) {
            // Randomly choose new state: -1, 0, or 1 (weighted towards extremes)
            int roll = random.nextInt(100);
            if (roll < 40) {
                currentState = 1; // 40% chance buff
            } else if (roll < 80) {
                currentState = -1; // 40% chance debuff
            } else {
                currentState = 0; // 20% chance neutral
            }
            lastUpdateTick = currentTick;
            LootUtils.updateModifier(itemstack, this);
        }
    }

    @Override
    public void hold(ItemStack stack, Level level, Entity holder) {
        if (level.isClientSide()) {
            return;
        }

        long currentTick = level.getGameTime();
        updateState(stack, currentTick);
    }

    @Override
    public float getStats(ItemStack itemstack) {
        // Mining speed modifier: 1.0 = neutral, 1.5 = buff, 0.5 = debuff
        return 1.0f + (currentState * MODIFIER_AMOUNT);
    }

    @Override
    public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
        if (hurtee.level().isClientSide()) {
            return false;
        }

        // Apply damage modifier based on current state
        if (currentState != 0) {
            float baseDamage = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));
            float bonusDamage = baseDamage * (currentState * MODIFIER_AMOUNT);
            hurtee.hurt(hurter.damageSources().mobAttack(hurter), bonusDamage);
        }

        return false;
    }
}
