package dev.marston.randomloot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

/**
 * Guards the consolidated leveled-trait NBT key and its legacy fallback, so a future
 * refactor that drops the back-compat read would fail loudly instead of silently
 * resetting every existing tool's level. What each trait then loads and writes back
 * under either key is pinned in {@code TraitFixtureTest}'s {@code loads.txt}.
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
}
