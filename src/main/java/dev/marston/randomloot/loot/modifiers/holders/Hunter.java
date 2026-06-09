package dev.marston.randomloot.loot.modifiers.holders;

import java.util.List;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class Hunter extends AbstractModifier implements HoldModifier {

    private static final String NAME = "name";
    private static final double SEARCH_RADIUS = 10.0;

    public Hunter() {
        this.name = "Hunter";
    }

    public Hunter(String name) {
        this.name = name;
    }

    @Override
    public String tagName() {
        return "hunter";
    }

    @Override
    public String color() {
        return ChatFormatting.DARK_RED.getName();
    }

    @Override
    public String description() {
        return "Nearby hostile mobs get the glowing effect";
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString(NAME, name);
        return tag;
    }

    @Override
    public Modifier fromNBT(CompoundTag tag) {
        return new Hunter(tag.getStringOr(NAME, "Hunter"));
    }

    @Override
    public Modifier clone() {
        return new Hunter();
    }

    @Override
    public boolean forTool(ToolType type) {
        return type.equals(ToolType.SWORD);
    }

    @Override
    public void hold(ItemStack stack, Level level, Entity holder) {
        if (level.isClientSide()) {
            return;
        }

        // Create search box around holder
        AABB searchBox = new AABB(
            holder.getX() - SEARCH_RADIUS, holder.getY() - SEARCH_RADIUS, holder.getZ() - SEARCH_RADIUS,
            holder.getX() + SEARCH_RADIUS, holder.getY() + SEARCH_RADIUS, holder.getZ() + SEARCH_RADIUS
        );

        // Find all hostile mobs (Monster class covers most hostile mobs)
        List<Monster> hostileMobs = level.getEntitiesOfClass(Monster.class, searchBox);

        // Apply glowing effect to each hostile mob
        for (Monster mob : hostileMobs) {
            // Short duration (40 ticks = 2 seconds) so it fades when player moves away
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
        }
    }
}
