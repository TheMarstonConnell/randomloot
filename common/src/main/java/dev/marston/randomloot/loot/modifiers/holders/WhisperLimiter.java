package dev.marston.randomloot.loot.modifiers.holders;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player budget for the Living trait's whispers ("pssst...", "hi..."). Every Living item a
 * player holds or wears draws from the same budget, so a full suit whispers no more
 * often than a single item. Whispers are spaced at least {@code 60s / perMinute}
 * apart; a rate of 0 silences them.
 *
 * <p>Only touched from the server thread (hold traits tick in {@code inventoryTick}),
 * so a plain map is enough.
 */
public class WhisperLimiter {

	private static final long MINUTE_MILLIS = 60_000L;

	private final Map<UUID, Long> lastWhisper = new HashMap<>();

	/** True, and the player's budget spent, if they may be whispered to at {@code nowMillis}. */
	public boolean tryWhisper(UUID player, long nowMillis, int perMinute) {
		if (perMinute <= 0) {
			return false;
		}

		Long last = lastWhisper.get(player);
		if (last != null && nowMillis - last < MINUTE_MILLIS / perMinute) {
			return false;
		}

		lastWhisper.put(player, nowMillis);
		return true;
	}
}
