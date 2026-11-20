package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class Fire extends AbstractModifier implements EntityHurtModifier {
	/** Seconds of fire. It is the trait's level, stored as "points": 2 when granted, one more per level. */
	private int seconds() {
		return traitLevel();
	}

	@Override
	public String description() {
		return "Sets enemy on fire for " + seconds() + " seconds.";
	}

	@Override
	public void hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		hurtee.setRemainingFireTicks(seconds() * 20);
	}
}
