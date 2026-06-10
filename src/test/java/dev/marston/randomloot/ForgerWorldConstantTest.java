package dev.marston.randomloot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.marston.randomloot.loot.NameGenerator;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Forgers are world-constant: a world seed plus a temperature band must always
 * resolve to the same smith, drawn from that band's name list.
 */
class ForgerWorldConstantTest {

	private static final float COLD = 0.0f;
	private static final float TEMPERATE = 0.7f;
	private static final float HOT = 2.0f;

	@Test
	void sameSeedAlwaysPicksTheSameForger() {
		for (float temp : new float[] { COLD, TEMPERATE, HOT }) {
			assertEquals(NameGenerator.forgerForWorld(12345L, temp), NameGenerator.forgerForWorld(12345L, temp));
		}
	}

	@Test
	void forgerComesFromTheMatchingTemperatureList() {
		long seed = 987654321L;
		assertTrue(Arrays.asList(NameGenerator.ColdNames).contains(NameGenerator.forgerForWorld(seed, COLD)));
		assertTrue(Arrays.asList(NameGenerator.TemperateNames).contains(NameGenerator.forgerForWorld(seed, TEMPERATE)));
		assertTrue(Arrays.asList(NameGenerator.HotNames).contains(NameGenerator.forgerForWorld(seed, HOT)));
	}

	@Test
	void differentSeedsCanRollDifferentForgers() {
		// Not guaranteed for any single pair of seeds, so scan a few: if every seed
		// produced the same temperate forger, the seed wouldn't be mixed in at all.
		String first = NameGenerator.forgerForWorld(0L, TEMPERATE);
		boolean anyDifferent = false;
		for (long seed = 1; seed <= 32 && !anyDifferent; seed++) {
			anyDifferent = !first.equals(NameGenerator.forgerForWorld(seed, TEMPERATE));
		}
		assertTrue(anyDifferent, "forger should depend on the world seed");
	}
}
