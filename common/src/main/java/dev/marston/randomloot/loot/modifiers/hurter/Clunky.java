package dev.marston.randomloot.loot.modifiers.hurter;


import dev.marston.randomloot.loot.LootItem.ToolType;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class Clunky extends AbstractModifier implements EntityHurtModifier, HoldModifier {

    private static final double KNOCKBACK_STRENGTH = 0.4;

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
    public ChatFormatting color() {
        return ChatFormatting.DARK_GRAY;
    }

    @Override
    public String description() {
        return "Applies slowness to holder but extra knockback on hit";
    }

    @Override
    public Modifier fromNBT(CompoundTag tag) {
        return new Clunky(tag.getStringOr(NAME, "Clunky"));
    }

    @Override
    public boolean forTool(ToolType type) {
        return isWeapon(type);
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
