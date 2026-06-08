package dev.marston.randomloot.loot.modifiers;

import net.minecraft.nbt.CompoundTag;

public final class ModifierConstants {
	public static final String NAME = "name";
	public static final String LEVEL = "trait_level";
	/** Key leveled traits used before {@link #LEVEL} was unified; still read for back-compat. */
	public static final String LEGACY_LEVEL = "level";
	public static final String POWER = "power";
	public static final String POINTS = "points";
	public static final String CHARGED = "charged";
	public static final String COUNT = "count";

	private ModifierConstants() {
	}

	/**
	 * Reads a trait's stored level from its NBT, falling back to the legacy
	 * {@code "level"} key so tools created before the key was unified keep their level.
	 *
	 * @param tag          the modifier's saved tag
	 * @param defaultLevel value to use when neither key is present
	 */
	public static int getLevel(CompoundTag tag, int defaultLevel) {
		return tag.getIntOr(LEVEL, tag.getIntOr(LEGACY_LEVEL, defaultLevel));
	}
}
