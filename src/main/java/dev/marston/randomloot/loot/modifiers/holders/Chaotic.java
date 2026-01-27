package dev.marston.randomloot.loot.modifiers.holders;

import java.util.List;
import java.util.Random;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.StatsModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class Chaotic implements StatsModifier, EntityHurtModifier {

    private static final String SEED = "chaoticSeed";
    private static final long UPDATE_INTERVAL = 100L; // 5 seconds = 100 ticks (20 ticks/sec)
    private static final float MAX_MODIFIER = 0.50f; // +/- 50% max

    private String name;
    private long seed;

    public Chaotic() {
        this.name = "Chaotic";
        this.seed = new Random().nextLong();
    }

    public Chaotic(String name, long seed) {
        this.name = name;
        this.seed = seed;
    }

    @Override
    public String tagName() {
        return "chaotic";
    }

    @Override
    public String name() {
        return this.name;
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
        tag.putLong(SEED, seed);
        return tag;
    }

    @Override
    public Modifier fromNBT(CompoundTag tag) {
        String name = tag.getStringOr(NAME, "Chaotic");
        long s = tag.getLongOr(SEED, 12345L); // Fixed default seed
        return new Chaotic(name, s);
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

    /**
     * Calculate current state based on game time and seed.
     * This works on both client and server since it's deterministic.
     */
    private float getCurrentState(Level level) {
        if (level == null) {
            return 0f;
        }
        // Get which "period" we're in (changes every UPDATE_INTERVAL ticks)
        long period = level.getGameTime() / UPDATE_INTERVAL;
        // Use seed XOR'd with period * large prime for better distribution
        Random rand = new Random(seed ^ (period * 6364136223846793005L));
        return (rand.nextFloat() * 2 * MAX_MODIFIER) - MAX_MODIFIER;
    }

    @Override
    public Component writeDetailsToLore(Level level) {
        if (level == null) {
            return Modifier.makeComp("Unknown", ChatFormatting.GRAY);
        }
        
        float currentState = getCurrentState(level);
        int percentage = Math.round(currentState * 100);
        
        // Calculate time until next update
        long gameTime = level.getGameTime();
        long ticksUntilUpdate = UPDATE_INTERVAL - (gameTime % UPDATE_INTERVAL);
        int secondsUntilUpdate = (int) Math.ceil(ticksUntilUpdate / 20.0);
        
        String stateText;
        ChatFormatting stateColor;
        if (percentage > 0) {
            stateText = "+" + percentage + "%";
            stateColor = ChatFormatting.GREEN;
        } else if (percentage < 0) {
            stateText = percentage + "%";
            stateColor = ChatFormatting.RED;
        } else {
            stateText = "0%";
            stateColor = ChatFormatting.GRAY;
        }
        
        return Modifier.makeComp(stateText + " (" + secondsUntilUpdate + "s)", stateColor);
    }

    @Override
    public float getStats(ItemStack itemstack) {
        // Can't get level here, so return neutral
        // The actual effect happens in hurtEnemy
        return 1.0f;
    }

    @Override
    public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
        if (hurtee.level().isClientSide()) {
            return false;
        }

        float currentState = getCurrentState(hurtee.level());

        // Apply damage modifier based on current state
        if (currentState != 0) {
            float baseDamage = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));
            float bonusDamage = baseDamage * currentState;
            // Reset invulnerability to apply bonus damage
            hurtee.invulnerableTime = 0;
            hurtee.hurt(hurter.damageSources().mobAttack(hurter), bonusDamage);
        }

        return false;
    }
}
