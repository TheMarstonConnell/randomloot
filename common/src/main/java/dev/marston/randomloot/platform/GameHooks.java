package dev.marston.randomloot.platform;

import java.util.EnumSet;
import java.util.Set;

/**
 * Which {@link GameHook}s the running loader has wired up.
 *
 * <p>Each loader's event shim calls {@link #bind} as it registers, and the
 * {@code loader_hooks_all_bound} gametest asserts nothing is {@link #missing()}. Two
 * adapters make this seam real: NeoForge binds from {@code NeoForgeEvents}, Fabric from
 * {@code RandomLootFabric}.
 *
 * <p>This checks that a hook was <em>declared</em> wired, not that the wiring works - a
 * Fabric mixin that fails to apply still counts as bound here. The behavioral gametests
 * cover that half. What this catches is the cheap, common mistake: a hook added to one
 * loader's shim and forgotten in the other's.
 */
public final class GameHooks {

	private static final Set<GameHook> BOUND = EnumSet.noneOf(GameHook.class);

	private GameHooks() {
	}

	/** Declares that the running loader has routed {@code hook} into its common dispatcher. */
	public static void bind(GameHook hook) {
		BOUND.add(hook);
	}

	public static boolean isBound(GameHook hook) {
		return BOUND.contains(hook);
	}

	/** Hooks this loader never bound. Empty on a correctly wired loader. */
	public static Set<GameHook> missing() {
		Set<GameHook> missing = EnumSet.allOf(GameHook.class);
		missing.removeAll(BOUND);
		return missing;
	}
}
