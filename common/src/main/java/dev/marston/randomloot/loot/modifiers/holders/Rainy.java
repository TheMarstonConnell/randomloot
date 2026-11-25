package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class Rainy extends AbstractModifier implements HoldModifier {
	@Override
	public String description() {
		return "While holding or wearing this item in the rain, mine faster!";
	}

	@Override
	public void hold(ItemStack stack, ServerLevel level, LivingEntity holder) {
		if (level.isRainingAt(holder.blockPosition())) {
			// hold() runs every tick; a too-short duration makes the effect timer flicker.
			holder.addEffect(new MobEffectInstance(MobEffects.HASTE, 40, 2, false, false));
		}

	}
}
