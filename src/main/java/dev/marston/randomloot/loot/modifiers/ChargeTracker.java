package dev.marston.randomloot.loot.modifiers;

import net.minecraft.world.level.Level;

public final class ChargeTracker {

	private ChargeTracker() {
	}

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

		return rate;
	}
}
