package dev.marston.randomloot.loot.modifiers.wearers;

import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.WearerHurtModifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Boots trait: softens fall damage, removing it entirely when maxed.
 */
public class Featherweight extends AbstractModifier implements WearerHurtModifier {
	/** Fall damage reduction: 25% at level 0, +25% per level, 100% when maxed. */
	private static final float REDUCTION_PER_LEVEL = 0.25f;

	@Override
	public String description() {
		return "Reduces fall damage by " + String.format("%.0f", reduction() * 100) + "%.";
	}

	@Override
	public String name() {
		if (traitLevel() > 0 && !this.canLevel()) {
			return "Weightless";
		}
		return super.name();
	}

	private float reduction() {
		return REDUCTION_PER_LEVEL * (float) (traitLevel() + 1);
	}

	@Override
	public float onWearerHurt(ItemStack stack, LivingEntity wearer, DamageSource source, float damage) {

		if (!source.is(DamageTypeTags.IS_FALL)) {
			return damage;
		}

		return damage * (1.0f - reduction());
	}
}
