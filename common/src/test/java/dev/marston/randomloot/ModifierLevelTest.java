package dev.marston.randomloot;

import dev.marston.randomloot.loot.NbtCompat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import dev.marston.randomloot.loot.modifiers.holders.Hasty;
import dev.marston.randomloot.loot.modifiers.hurter.Fragile;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

/**
 * Guards the consolidated leveled-trait NBT key and its legacy fallback, so a future
 * refactor that drops the back-compat read would fail loudly instead of silently
 * resetting every existing tool's level.
 */
class ModifierLevelTest {

	@Test
	void getLevelReadsCanonicalKey() {
		CompoundTag tag = new CompoundTag();
		tag.putInt(ModifierConstants.LEVEL, 3);
		assertEquals(3, ModifierConstants.getLevel(tag, 1));
	}

	@Test
	void getLevelFallsBackToLegacyKey() {
		CompoundTag tag = new CompoundTag();
		tag.putInt(ModifierConstants.LEGACY_LEVEL, 5); // pre-consolidation tools stored "level"
		assertEquals(5, ModifierConstants.getLevel(tag, 1));
	}

	@Test
	void getLevelPrefersCanonicalOverLegacy() {
		CompoundTag tag = new CompoundTag();
		tag.putInt(ModifierConstants.LEVEL, 2);
		tag.putInt(ModifierConstants.LEGACY_LEVEL, 9);
		assertEquals(2, ModifierConstants.getLevel(tag, 1));
	}

	@Test
	void getLevelUsesDefaultWhenAbsent() {
		assertEquals(7, ModifierConstants.getLevel(new CompoundTag(), 7));
	}

	@Test
	void fragileRoundTripsLevelUnderCanonicalKey() {
		Fragile f = new Fragile();
		f.levelUp(); // 1 -> 2
		CompoundTag tag = f.toNBT();
		assertEquals(2, NbtCompat.getIntOr(tag, ModifierConstants.LEVEL, -1));

		Modifier restored = new Fragile().fromNBT(tag);
		assertEquals(2, NbtCompat.getIntOr(restored.toNBT(), ModifierConstants.LEVEL, -1));
	}

	@Test
	void fragileMigratesLegacyLevelKeyToCanonical() {
		CompoundTag legacy = new CompoundTag();
		legacy.putString(ModifierConstants.NAME, "Fragile");
		legacy.putInt(ModifierConstants.LEGACY_LEVEL, 2); // tool saved before the key was unified

		Modifier restored = new Fragile().fromNBT(legacy);
		// Re-serializes under the canonical key with the level preserved.
		assertEquals(2, NbtCompat.getIntOr(restored.toNBT(), ModifierConstants.LEVEL, -1));
	}

	@Test
	void hastyMigratesLegacyLevelAndKeepsPower() {
		CompoundTag legacy = new CompoundTag();
		legacy.putString(ModifierConstants.NAME, "Hasty");
		legacy.putInt(ModifierConstants.POWER, 1);
		legacy.putInt(ModifierConstants.LEGACY_LEVEL, 1);

		CompoundTag out = new Hasty().fromNBT(legacy).toNBT();
		assertEquals(1, NbtCompat.getIntOr(out, ModifierConstants.LEVEL, -1));
		assertEquals(1, NbtCompat.getIntOr(out, ModifierConstants.POWER, -1));
	}
}
