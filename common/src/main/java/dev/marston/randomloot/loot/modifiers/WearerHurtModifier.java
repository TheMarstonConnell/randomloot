package dev.marston.randomloot.loot.modifiers;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Armor traits that react to the wearer taking damage. Dispatched from
 * {@link ArmorDispatcher} during {@code LivingDamageEvent.Pre}, once per worn
 * piece carrying the trait, after vanilla armor/potion reductions.
 */
public interface WearerHurtModifier extends Modifier {

	/**
	 * Reacts to the wearer being hurt and returns the (possibly reduced) damage that
	 * should be applied to the wearer's health. Return {@code damage} unchanged for
	 * purely reactive traits.
	 */
	float onWearerHurt(ItemStack stack, LivingEntity wearer, DamageSource source, float damage);
}
