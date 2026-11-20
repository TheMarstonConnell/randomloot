package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class Rainy extends AbstractModifier implements HoldModifier {
	@Override
	public String description() {
		return "While holding or wearing this item in the rain, mine faster!";
	}

	@Override
	public void hold(ItemStack stack, Level level, Entity holder) {
		if (level.isRainingAt(holder.blockPosition())) {
			// hold() runs every tick; a too-short duration makes the effect timer flicker.
			MobEffectInstance haste = new MobEffectInstance(MobEffects.DIG_SPEED, 40, 2, false, false);

			if (holder instanceof LivingEntity le) {
				le.addEffect(haste);
			}
		}

	}
}
