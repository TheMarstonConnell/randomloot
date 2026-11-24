package dev.marston.randomloot.loot.modifiers;

import net.minecraft.world.level.Level;

public class Unbreaking extends AbstractModifier {
	/** Chance to skip durability loss: 20% at level 0, +20% per level, 100% when maxed. */
	private static final float CHANCE_PER_LEVEL = 0.2f;

	public String description() {
		return "This tool has a " + String.format("%.0f", chance() * 100) + "% chance of not taking damage.";
	}

	private float chance() {
		return CHANCE_PER_LEVEL * (float) (traitLevel() + 1);
	}

	/**
	 * How many of {@code points} of durability the gear still has to pay: each point gets
	 * its own roll to be let off. The one rule for tools and worn armor alike.
	 */
	public int remaining(int points, Level level) {
		int owed = 0;
		for (int i = 0; i < points; i++) {
			if (level.getRandom().nextFloat() > chance()) {
				owed++;
			}
		}
		return owed;
	}
}
