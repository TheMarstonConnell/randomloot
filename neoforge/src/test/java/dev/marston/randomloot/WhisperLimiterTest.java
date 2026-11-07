package dev.marston.randomloot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.marston.randomloot.loot.modifiers.holders.WhisperLimiter;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The Living trait's per-player whisper budget (issue #182). */
class WhisperLimiterTest {

	private static final UUID ALEX = UUID.randomUUID();
	private static final UUID STEVE = UUID.randomUUID();

	@Test
	void zeroRateNeverWhispers() {
		WhisperLimiter limiter = new WhisperLimiter();
		assertFalse(limiter.tryWhisper(ALEX, 0, 0));
		assertFalse(limiter.tryWhisper(ALEX, 600_000, 0));
	}

	@Test
	void secondItemSharesTheFirstItemsBudget() {
		// A full suit calls tryWhisper once per Living item; only the first may speak.
		WhisperLimiter limiter = new WhisperLimiter();
		assertTrue(limiter.tryWhisper(ALEX, 1_000, 1));
		assertFalse(limiter.tryWhisper(ALEX, 1_000, 1));
		assertFalse(limiter.tryWhisper(ALEX, 1_050, 1));
	}

	@Test
	void ratePerMinuteSetsTheSpacing() {
		WhisperLimiter limiter = new WhisperLimiter();
		assertTrue(limiter.tryWhisper(ALEX, 0, 4));
		assertFalse(limiter.tryWhisper(ALEX, 14_999, 4));
		assertTrue(limiter.tryWhisper(ALEX, 15_000, 4));
		assertFalse(limiter.tryWhisper(ALEX, 29_999, 4));
		assertTrue(limiter.tryWhisper(ALEX, 30_000, 4));
	}

	@Test
	void playersHaveSeparateBudgets() {
		WhisperLimiter limiter = new WhisperLimiter();
		assertTrue(limiter.tryWhisper(ALEX, 0, 1));
		assertTrue(limiter.tryWhisper(STEVE, 0, 1));
		assertFalse(limiter.tryWhisper(ALEX, 59_999, 1));
		assertTrue(limiter.tryWhisper(ALEX, 60_000, 1));
	}
}
