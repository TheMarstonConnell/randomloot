package dev.marston.randomloot.loot.modifiers;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface EntityHurtModifier extends Modifier {
	boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter);

	/** Nesting depth of {@link #followUp} calls. Server thread only. */
	final class FollowUp {
		private static int depth;

		private FollowUp() {
		}
	}

	/**
	 * Runs damage a trait deals from inside {@link #hurtEnemy} - bonus hits, Charged's
	 * lightning - marked as a follow-up to the hit being reacted to. Anything that keeps
	 * state about "the hit" from the pre-damage hook (Early Bird's pre-hit health) checks
	 * {@link #isFollowUp()} so these don't overwrite it. Route any new hurtEnemy-time
	 * damage through here.
	 */
	static void followUp(Runnable damage) {
		FollowUp.depth++;
		try {
			damage.run();
		} finally {
			FollowUp.depth--;
		}
	}

	/** True while damage started by {@link #followUp} is being applied. */
	static boolean isFollowUp() {
		return FollowUp.depth > 0;
	}

	/**
	 * Applies follow-up bonus damage from a {@link #hurtEnemy} handler.
	 *
	 * <p>The primary melee hit has already set the target's invulnerability frames and
	 * {@code lastHurt} to the full attack damage; vanilla only lets a second hit land
	 * during i-frames if it exceeds {@code lastHurt}, so any smaller bonus is silently
	 * swallowed unless the i-frames are reset first. The damage source mirrors the
	 * attacker (player vs. mob) so kill credit, knockback and death messages stay correct.
	 * Non-positive amounts are ignored (a negative {@code hurt} would heal the target).
	 */
	default void dealBonusDamage(LivingEntity hurtee, LivingEntity hurter, float amount) {
		if (amount <= 0.0f) {
			return;
		}
		hurtee.damageCooldownTime = 0;
		followUp(() -> {
			if (hurter instanceof Player player) {
				hurtee.hurt(hurter.damageSources().playerAttack(player), amount);
			} else {
				hurtee.hurt(hurter.damageSources().mobAttack(hurter), amount);
			}
		});
	}
}
