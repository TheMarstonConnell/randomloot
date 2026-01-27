package dev.marston.randomloot.loot.modifiers.hurter;

import java.util.List;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class Feasting implements EntityHurtModifier, HoldModifier {

    private static final int HUNGER_THRESHOLD = 10; // 50% of max hunger (20)
    private static final float DAMAGE_BONUS = 0.10f; // 10% bonus/penalty

    private String name;

    public Feasting() {
        this.name = "Feasting";
    }

    public Feasting(String name) {
        this.name = name;
    }

    @Override
    public String tagName() {
        return "feasting";
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
        return "Performance scales with hunger level";
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString(NAME, name);
        return tag;
    }

    @Override
    public Modifier fromNBT(CompoundTag tag) {
        return new Feasting(tag.getStringOr(NAME, "Feasting"));
    }

    @Override
    public Modifier clone() {
        return new Feasting();
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

    private boolean isWellFed(LivingEntity entity) {
        if (entity instanceof Player player) {
            return player.getFoodData().getFoodLevel() >= HUNGER_THRESHOLD;
        }
        return true; // Non-players are always considered "well fed"
    }

    @Override
    public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
        float baseDamage = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));
        float modifier;

        if (isWellFed(hurter)) {
            modifier = DAMAGE_BONUS; // +10%
        } else {
            modifier = -DAMAGE_BONUS; // -10%
        }

        float bonusDamage = baseDamage * modifier;
        if (bonusDamage != 0) {
            if (hurter instanceof Player p) {
                hurtee.hurt(hurter.damageSources().playerAttack(p), bonusDamage);
            } else {
                hurtee.hurt(hurter.damageSources().mobAttack(hurter), bonusDamage);
            }
        }

        return false;
    }

    @Override
    public void hold(ItemStack stack, Level level, Entity holder) {
        if (!(holder instanceof Player player)) {
            return;
        }

        if (isWellFed(player)) {
            // Well fed: Apply haste for faster mining
            MobEffectInstance haste = new MobEffectInstance(MobEffects.HASTE, 2, 0, true, false);
            player.addEffect(haste);
        } else {
            // Hungry: Apply mining fatigue for slower mining
            MobEffectInstance fatigue = new MobEffectInstance(MobEffects.MINING_FATIGUE, 2, 0, true, false);
            player.addEffect(fatigue);
        }
    }
}
