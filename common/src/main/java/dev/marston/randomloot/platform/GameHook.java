package dev.marston.randomloot.platform;

/**
 * Every game event the mod needs a loader to route into a common dispatcher.
 *
 * <p>Adding a hook is one method on NeoForge, where {@code NeoForgeEvents} lists them all
 * in a single class. On Fabric the same hook may need a Fabric API callback, or a mixin
 * class <em>plus</em> an entry in {@code randomloot.fabric.mixins.json} - three places to
 * remember, spread across six files. Nothing in {@code common} named the required set, so
 * the only way to notice a missed one was to play the game.
 *
 * <p>Naming them here means a new hook that is wired on one loader and forgotten on the
 * other fails {@code loader_hooks_all_bound} instead.
 */
public enum GameHook {

	/** An entity died: EntityKillModifier traits and kill XP. */
	KILL,
	/** A wearer is about to take damage: WearerHurtModifier traits may reduce it. */
	DAMAGE_PRE,
	/** A wearer took damage: worn armor earns XP. */
	DAMAGE_POST,
	/** An armor piece is about to lose durability: Unbreaking may skip it. */
	ARMOR_HURT,
	/** Block-breaking speed: Soulbound's owner bonus. */
	BREAK_SPEED,
	/** Per-tick server work: the block-highlighter finder traits. */
	SERVER_TICK,
	/** Server shutdown: clean up highlighter markers. */
	SERVER_STOPPING,
	/** Register /randomloot. */
	COMMANDS,
	/** Config load and reload. */
	CONFIG,
	/** Add the mod's items to the creative tab. */
	CREATIVE_TAB,
	/** Block anvil-combining two pieces of loot gear. */
	ANVIL_COMBINE,
	/** Per-type/per-piece enchantment filtering. */
	ENCHANT_GATE,
	/** Inject cases and templates into chest loot. */
	LOOT_INJECTION
}
