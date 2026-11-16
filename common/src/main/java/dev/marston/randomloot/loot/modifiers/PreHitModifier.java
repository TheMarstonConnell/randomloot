package dev.marston.randomloot.loot.modifiers;

import net.minecraft.world.entity.LivingEntity;

/**
 * A hit trait that judges its target by how it was before the hit landed.
 *
 * <p>{@link #hurtEnemy} runs after the damage, so the health it sees is already reduced.
 * A trait that needs the earlier value takes a note from the pre-damage hook instead.
 * Implementing this interface is the whole declaration: {@link TraitDispatch} calls
 * {@link #beforeDamage} for every damage event, and on a hit it runs these traits ahead
 * of the gear's other hit traits, whose bonus damage is a damage event too and would
 * overwrite the note.
 */
public interface PreHitModifier extends EntityHurtModifier {

	/**
	 * Called on the server before any living entity takes damage. Set or clear the note
	 * on every call, so it describes the latest hit and not an earlier one in the same
	 * tick. It is called on the registry's instance of the trait, not on one read from
	 * a piece of gear, so keep the note in static state.
	 */
	void beforeDamage(LivingEntity victim);
}
