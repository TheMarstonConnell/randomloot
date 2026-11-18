package dev.marston.randomloot.loot.modifiers.wearers;

import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.WearerHurtModifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Armor trait: chance to block half of any incoming hit, with a shield clang.
 */
public class Bulwark extends AbstractModifier implements WearerHurtModifier {
	/** Chance to halve a hit: 10% at level 0, +10% per level, 50% when maxed. */
	private static final float CHANCE_PER_LEVEL = 0.1f;

	@Override
	public String description() {
		return "Has a " + String.format("%.0f", chance() * 100) + "% chance of halving damage taken.";
	}

	private float chance() {
		return CHANCE_PER_LEVEL * (float) (traitLevel() + 1);
	}

	@Override
	public float onWearerHurt(ItemStack stack, LivingEntity wearer, DamageSource source, float damage) {

		if (wearer.level().getRandom().nextFloat() > chance()) {
			return damage;
		}

		wearer.level().playSound(null, wearer.getX(), wearer.getY(), wearer.getZ(), SoundEvents.SHIELD_BLOCK.value(),
				wearer.getSoundSource(), 1.0f, 1.0f);

		return damage / 2.0f;
	}
}
