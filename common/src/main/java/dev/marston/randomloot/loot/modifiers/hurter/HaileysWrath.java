package dev.marston.randomloot.loot.modifiers.hurter;


import dev.marston.randomloot.advancements.ModCriteria;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.EntityKillModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class HaileysWrath extends AbstractModifier implements EntityKillModifier {

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
    public ChatFormatting color() {
        return ChatFormatting.GOLD;
    }

    @Override
    public String description() {
        return "Spawns a bee when the target is killed";
    }

    @Override
    public Modifier fromNBT(CompoundTag tag) {
        return new HaileysWrath(tag.getStringOr(NAME, "Hailey's Wrath"));
    }

    @Override
    public boolean forTool(ToolType type) {
        return isWeapon(type);
    }

    @Override
    public void onKill(ItemStack itemstack, LivingEntity victim, LivingEntity killer) {
        Level level = victim.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // Spawn a bee at the victim's location
        BlockPos pos = victim.blockPosition();
        Entity bee = EntityTypes.BEE.create(serverLevel, null, pos, EntitySpawnReason.MOB_SUMMONED, false, false);
        if (bee != null) {
            bee.setPos(victim.getX(), victim.getY(), victim.getZ());
            level.addFreshEntity(bee);
            ModCriteria.traitUsed(killer, this);
        }
    }
}
