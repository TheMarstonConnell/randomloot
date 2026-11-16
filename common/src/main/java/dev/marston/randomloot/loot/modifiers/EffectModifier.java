package dev.marston.randomloot.loot.modifiers;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Base for traits that apply a vanilla {@link MobEffect} and level by raising its
 * amplifier ("power"). Which effect and for how long are the rule; the name, colour and
 * level range of each of the eight are declared in {@link ModifierRegistry}.
 */
public abstract class EffectModifier extends AbstractModifier {
	/** Effects level from power 0 (shown as I) to 4 (shown as V). */
	private static final int MAX_POWER = 4;

	protected final Holder<MobEffect> effect;
	/** In seconds. */
	protected final int duration;

	protected EffectModifier(Holder<MobEffect> effect, int duration) {
		this.effect = effect;
		this.duration = duration;
	}

	/**
	 * Highest power a trait for this effect levels to. Vanilla only calls {@code hasEffect}
	 * for Blindness and Fire Resistance and never reads their amplifier, so those stay at
	 * level I: another level would be paid for and change nothing.
	 */
	public static int maxPower(Holder<MobEffect> effect) {
		return effect.is(MobEffects.BLINDNESS) || effect.is(MobEffects.FIRE_RESISTANCE) ? 0 : MAX_POWER;
	}

	/** The effect's amplifier: the trait's level, which effects store as their power. */
	protected final int power() {
		return traitLevel();
	}

	/** The effect instance this modifier applies: duration is in seconds, no particles. */
	protected MobEffectInstance makeInstance() {
		return new MobEffectInstance(effect, duration * 20, power(), false, false);
	}
}
