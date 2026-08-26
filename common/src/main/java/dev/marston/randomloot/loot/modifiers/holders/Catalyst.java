package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.NbtCompat;

import java.util.ArrayList;

import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.LeveledModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Cinnabar-themed holder trait: the mod's alchemical "catalyst". While the item is held or
 * worn, the holder's beneficial status effects tick down more slowly, so potions and buffs
 * linger. Levels raise how much longer they last (the bare trait ~2x, II ~3x, III ~4x).
 */
public class Catalyst extends LeveledModifier implements HoldModifier {

	public Catalyst() {
		this("Catalyst", 1);
	}

	public Catalyst(String name, int level) {
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

	/** Beneficial effects last this many times longer at the current level (2x / 3x / 4x). */
	private int getMultiplier() {
		return level + 1;
	}

	/**
	 * Per-tick chance to refund the duration vanilla just drained from a beneficial effect.
	 * A refund rate of {@code 1 - 1/multiplier} drops the net decay to {@code 1/multiplier},
	 * so the effect lasts {@code multiplier} times as long. Because we only ever add back the
	 * single tick just lost, the duration trends down and never exceeds its current value.
	 */
	private float getRefundChance() {
		return 1.0f - (1.0f / getMultiplier());
	}

	@Override
	public String tagName() {
		return "catalyst";
	}

	@Override
	public ChatFormatting color() {
		return ChatFormatting.RED;
	}

	@Override
	public String description() {
		return "While held or worn, your beneficial status effects last up to " + getMultiplier() + "x longer.";
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Catalyst(NbtCompat.getStringOr(tag, NAME, "Catalyst"), ModifierConstants.getLevel(tag, 1));
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

		if (!(holder instanceof LivingEntity livingHolder)) {
			return;
		}

		float refundChance = getRefundChance();

		// Copy first: addEffect below mutates the live active-effects collection.
		for (MobEffectInstance active : new ArrayList<>(livingHolder.getActiveEffects())) {
			Holder<MobEffect> effect = active.getEffect();

			if (effect.value().getCategory() != MobEffectCategory.BENEFICIAL) {
				continue;
			}

			int duration = active.getDuration();
			// Skip expired and infinite (negative duration) effects — nothing to slow.
			if (duration <= 0) {
				continue;
			}

			if (level.getRandom().nextFloat() < refundChance) {
				// Re-add one tick longer; vanilla extends the duration when the amplifier matches.
				livingHolder.addEffect(new MobEffectInstance(effect, duration + 1, active.getAmplifier(),
						active.isAmbient(), active.isVisible(), active.showIcon()));
			}
		}
	}
}
