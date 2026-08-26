package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.NbtCompat;

import java.util.List;

import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.LeveledModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Sulfur-themed holder trait: the choking reek of brimstone. While the item is held or worn,
 * hostile mobs within range are afflicted with Slowness and Weakness. Levels widen the radius
 * (6 / 8 / 10 blocks).
 */
public class Stench extends LeveledModifier implements HoldModifier {

	/** Re-applied each tick at a short duration so the cloud fades once mobs leave range. */
	private static final int EFFECT_DURATION = 40; // 2 seconds

	public Stench() {
		this("Stench", 1);
	}

	public Stench(String name, int level) {
		this.name = name;
		this.level = level;
	}

	@Override
	protected int minLevel() {
		return 1;
	}

	@Override
	protected int maxLevel() {
		return 3;
	}

	/** Aura radius in blocks: 6 / 8 / 10 by level. */
	private double getRadius() {
		return 4.0 + (level * 2.0);
	}

	@Override
	public String tagName() {
		return "stench";
	}

	@Override
	public ChatFormatting color() {
		return ChatFormatting.YELLOW;
	}

	@Override
	public String description() {
		return "Hostile mobs within " + String.format("%.0f", getRadius())
				+ " blocks are afflicted with Slowness and Weakness.";
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Stench(NbtCompat.getStringOr(tag, NAME, "Stench"), ModifierConstants.getLevel(tag, 1));
	}

	@Override
	public boolean forTool(ToolType type) {
		return true;
	}

	@Override
	public void hold(ItemStack stack, Level level, Entity holder) {
		if (level.isClientSide()) {
			return;
		}

		double radius = getRadius();
		AABB searchBox = new AABB(
				holder.getX() - radius, holder.getY() - radius, holder.getZ() - radius,
				holder.getX() + radius, holder.getY() + radius, holder.getZ() + radius);

		// Monster covers the hostile mobs; mirrors Hunter's targeting.
		List<Monster> hostileMobs = level.getEntitiesOfClass(Monster.class, searchBox);

		for (Monster mob : hostileMobs) {
			mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_DURATION, 0, false, true));
			mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EFFECT_DURATION, 0, false, true));
		}
	}
}
