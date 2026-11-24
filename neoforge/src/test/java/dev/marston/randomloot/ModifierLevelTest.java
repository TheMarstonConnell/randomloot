package dev.marston.randomloot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Guards the consolidated leveled-trait NBT key and its legacy fallback, so a future
 * refactor that drops the back-compat read would fail loudly instead of silently
 * resetting every existing tool's level. Tools from before the consolidation stored their
 * level under the legacy {@code "level"} key. What each trait then loads and writes back
 * under either key is pinned in {@code TraitFixtureTest}'s {@code loads.txt}.
 */
class ModifierLevelTest {

	@ParameterizedTest(name = "level={0} legacy={1} default={2} -> {3}")
	@CsvSource({ "3,,1,3", ",5,1,5", "2,9,1,2", ",,7,7" })
	void getLevelReadsCanonicalThenLegacyThenDefault(Integer level, Integer legacy, int fallback, int expected) {
		CompoundTag tag = new CompoundTag();
		if (level != null) {
			tag.putInt(ModifierConstants.LEVEL, level);
		}
		if (legacy != null) {
			tag.putInt(ModifierConstants.LEGACY_LEVEL, legacy);
		}
		assertEquals(expected, ModifierConstants.getLevel(tag, fallback));
	}
}
