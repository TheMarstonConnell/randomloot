package dev.marston.randomloot.loot.modifiers.wearers;

import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.WearerHurtModifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Armor trait: taking a hit triggers a burst of speed.
 */
public class Adrenaline extends AbstractModifier implements WearerHurtModifier {
	/** Speed buff duration in ticks (5 seconds). */
	private static final int DURATION = 100;

	@Override
	public String description() {
		return "Taking damage grants speed " + LootUtils.roman(traitLevel() + 1) + " for 5 seconds.";
	}

	@Override
	public float onWearerHurt(ItemStack stack, LivingEntity wearer, DamageSource source, float damage) {

		wearer.addEffect(new MobEffectInstance(MobEffects.SPEED, DURATION, traitLevel(), false, false));

		return damage;
	}
}
