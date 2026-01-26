package dev.marston.randomloot.loot.modifiers;

import net.minecraft.world.World;

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
     * @param world         The world (for game time)
     * @param lastTriggered The game time when the charge was last reset
     * @param chargeTime    The number of seconds to fully charge (multiplied by 20 for ticks)
     * @return A value from 0.0 to 1.0 representing charge percentage
     */
    public static float getCharge(World world, long lastTriggered, int chargeTime) {
        if (world == null) {
            return 0.0f;
        }

        long time = world.getTotalWorldTime();
        long diff = time - lastTriggered;

        float rate = (float) diff / (float) (chargeTime * 20);
        if (rate > 1.0f) {
            rate = 1.0f;
        }

        return rate;
    }
}
