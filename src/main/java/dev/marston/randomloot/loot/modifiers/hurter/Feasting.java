package dev.marston.randomloot.loot.modifiers.hurter;


import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class Feasting extends AbstractModifier implements EntityHurtModifier, HoldModifier {

    private static final int HUNGER_THRESHOLD = 10; // 50% of max hunger (20)
    private static final float DAMAGE_BONUS = 0.10f; // 10% bonus/penalty

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
    public ChatFormatting color() {
        return ChatFormatting.GOLD;
    }

    @Override
    public String description() {
        return "Performance scales with hunger level";
    }

    @Override
    public Modifier fromNBT(CompoundTag tag) {
        return new Feasting(tag.getStringOr(NAME, "Feasting"));
    }

    @Override
    public boolean forTool(ToolType type) {
        // Its payoff lives in tool-only hooks, so on armor it would be a confusing no-op.
        return !type.isArmor();
    }

    private boolean isWellFed(LivingEntity entity) {
        if (entity instanceof Player player) {
            return player.getFoodData().getFoodLevel() >= HUNGER_THRESHOLD;
        }
        return true; // Non-players are always considered "well fed"
    }

    @Override
    public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
        if (hurtee.level().isClientSide()) {
            return false;
        }

        // Only a well-fed attacker deals bonus melee damage. A "hungry penalty" cannot
        // be applied as a post-hit negative hurt (that would heal the target already
        // damaged by the primary swing), so the hungry case is handled purely by the
        // mining-fatigue effect in hold().
        if (!isWellFed(hurter)) {
            return false;
        }

        float baseDamage = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));
        dealBonusDamage(hurtee, hurter, baseDamage * DAMAGE_BONUS);

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
