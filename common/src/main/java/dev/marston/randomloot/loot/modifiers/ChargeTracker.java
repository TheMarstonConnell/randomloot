package dev.marston.randomloot.loot.modifiers;

import net.minecraft.world.level.Level;

/**
 * Utility class for tracking charge-based modifier mechanics.
 * Used by modifiers that need to track time since last activation.
 */
public final class ChargeTracker {

	private ChargeTracker() {
	}

	/**
	 * Calculates the charge level based on time elapsed since last trigger.
	 *
	 * @param level         The world level (for game time)
	 * @param lastTriggered The game time when the charge was last reset
	 * @param chargeTime    The number of seconds to fully charge (multiplied by 20 for ticks)
	 * @return A value from 0.0 to 1.0 representing charge percentage
	 */
	public static float getCharge(Level level, long lastTriggered, int chargeTime) {
		if (level == null) {
			return 0.0f;
		}

		long time = level.getGameTime();
		long diff = time - lastTriggered;

		float rate = (float) diff / (float) (chargeTime * 20);
		if (rate > 1.0f) {
			rate = 1.0f;
		}
		// A tool carried into a world with a lower total game time can produce a
		// negative diff; clamp so the charge meter never shows a negative percentage.
		if (rate < 0.0f) {
			rate = 0.0f;
		}

		return rate;
	}
}
