package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.StatsModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class Fierce extends AbstractModifier implements EntityHurtModifier, StatsModifier {
	@Override
	public String description() {
		return "Deals more damage as durability decreases";
	}

	/** How much of its durability the gear has used up, from 0 to 1. */
	private static float wornShare(ItemStack s) {
		float max = s.getMaxDamage();
		return max <= 0 ? 0.0f : s.getDamageValue() / max;
	}

	@Override
	public void hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		// Rounded through 1 + x and back, as before the shrink, so the bonus stays the same float.
		float multiplier = 1.0f + wornShare(itemstack) * 0.5f;
		dealBonusDamage(hurtee, hurter, attackDamage(itemstack) * (multiplier - 1.0f));
	}

	@Override
	public float getStats(ItemStack itemstack) {
		return 1.0f + wornShare(itemstack) * 0.25f;
	}
}
