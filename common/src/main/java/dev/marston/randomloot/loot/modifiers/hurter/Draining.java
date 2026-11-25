package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class Draining extends AbstractModifier implements EntityHurtModifier {
	@Override
	public String description() {
		return "Heals " + String.format("%.0f", drain() * 100) + "% of damage dealt to target.";
	}

	/** Share of the damage healed: 5% per point, and the points are the trait's level. */
	public float drain() {
		return ((float) traitLevel()) * 0.05f;
	}

	@Override
	public void hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		hurter.heal(attackDamage(itemstack) * drain());
	}
}
