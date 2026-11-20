package dev.marston.randomloot.platform;

/**
 * Every game event the mod needs a loader to deliver into common. The names only: what a
 * loader shim has to hand over for a hook - stage, side and scope - is the javadoc of the
 * {@link GameHooks} method that records it, which is also the one method the shim calls.
 * A constant's usages lead to that method.
 *
 * <p>Adding a hook: add the constant here, its method to {@link GameHooks}, a call from
 * each loader shim, and a step in the {@code loader_hooks_all_bound} gametest that makes
 * the game do the thing. That gametest fails for a hook either loader never delivers.
 */
public enum GameHook {
	KILL,
	DAMAGE_PRE,
	DAMAGE_POST,
	BREAK_SPEED,
	AFTER_USE_ITEM,
	SERVER_TICK,
	SERVER_STOPPING,
	COMMANDS,
	CONFIG,
	CREATIVE_TAB,
	ANVIL_COMBINE,
	ENCHANT_GATE,
	LOOT_INJECTION,
	SMITHING_TAKE
}
