package dev.marston.randomloot.loot.modifiers.wearers;

import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.WearerHurtModifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Armor trait: reflects a share of incoming damage back at the attacker.
 */
public class Thorny extends AbstractModifier implements WearerHurtModifier {
	/** Share of damage reflected: 15% at level 0, +15% per level, 75% when maxed. */
	private static final float REFLECT_PER_LEVEL = 0.15f;

	@Override
	public String description() {
		return "Reflects " + String.format("%.0f", reflectShare() * 100) + "% of damage taken back at the attacker.";
	}

	private float reflectShare() {
		return REFLECT_PER_LEVEL * (float) (traitLevel() + 1);
	}

	@Override
	public float onWearerHurt(ItemStack stack, LivingEntity wearer, DamageSource source, float damage) {

		if (source.getEntity() instanceof LivingEntity attacker && attacker != wearer) {
			attacker.hurt(wearer.damageSources().thorns(wearer), damage * reflectShare());
		}

		return damage;
	}
}
