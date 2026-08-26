package dev.marston.randomloot.loot;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * 1.21.1 backport shim for the defaulted CompoundTag getters that vanilla only
 * gained in later versions (getIntOr etc.). Keeps call sites close to the 26.x
 * branch so cross-porting changes stays mechanical.
 */
public final class NbtCompat {
	private NbtCompat() {
	}

	public static int getIntOr(CompoundTag tag, String key, int def) {
		return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : def;
	}

	public static long getLongOr(CompoundTag tag, String key, long def) {
		return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getLong(key) : def;
	}

	public static float getFloatOr(CompoundTag tag, String key, float def) {
		return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getFloat(key) : def;
	}

	public static double getDoubleOr(CompoundTag tag, String key, double def) {
		return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getDouble(key) : def;
	}

	public static boolean getBooleanOr(CompoundTag tag, String key, boolean def) {
		return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getBoolean(key) : def;
	}

	public static String getStringOr(CompoundTag tag, String key, String def) {
		return tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : def;
	}

	public static CompoundTag getCompoundOrEmpty(CompoundTag tag, String key) {
		return tag.getCompound(key);
	}
}
