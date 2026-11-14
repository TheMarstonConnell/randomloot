package dev.marston.randomloot.loot.modifiers;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Armor traits that react to the wearer taking damage. Dispatched from
 * {@link ArmorDispatcher}, once per worn piece carrying the trait, after vanilla's
 * armor, protection-enchantment and Resistance reductions and before absorption hearts:
 * NeoForge's {@code LivingDamageEvent.Pre}, and the same point by mixin on Fabric. So
 * {@code damage} is what is about to be taken, not the hit that was dealt.
 */
public interface WearerHurtModifier extends Modifier {

	/**
	 * Reacts to the wearer being hurt and returns the (possibly reduced) damage that
	 * should be applied to the wearer's health. Return {@code damage} unchanged for
	 * purely reactive traits.
	 */
	float onWearerHurt(ItemStack stack, LivingEntity wearer, DamageSource source, float damage);
}
