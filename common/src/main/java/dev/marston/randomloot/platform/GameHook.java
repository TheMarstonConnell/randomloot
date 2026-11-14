package dev.marston.randomloot.platform;

/**
 * Every game event the mod needs a loader to deliver into common. The names only; what a
 * loader shim has to hand over for each one - stage, side and scope - is the javadoc of
 * the {@link GameHooks} method named beside it, which is also the one method the shim
 * calls.
 *
 * <p>Adding a hook: add the constant here, its method to {@link GameHooks}, a call from
 * each loader shim, and a step in the {@code loader_hooks_all_bound} gametest that makes
 * the game do the thing. That gametest fails for a hook either loader never delivers.
 */
public enum GameHook {

	/** An entity died: EntityKillModifier traits. {@link GameHooks#kill} */
	KILL,
	/** A wearer is about to take damage: WearerHurtModifier traits may change it. {@link GameHooks#damagePre} */
	DAMAGE_PRE,
	/** A wearer took damage: worn armor earns XP. {@link GameHooks#damagePost} */
	DAMAGE_POST,
	/** A worn piece is about to lose durability to a hit: Unbreaking may skip it. {@link GameHooks#armorHurt} */
	ARMOR_HURT,
	/** Block-breaking speed: Soulbound's owner bonus. {@link GameHooks#breakSpeed} */
	BREAK_SPEED,
	/**
	 * The server finished an item or block use: a right-click trait deferred behind the
	 * off hand may fire. {@link GameHooks#afterUseItem}, {@link GameHooks#afterUseItemOn}
	 */
	AFTER_USE_ITEM,
	/** Per-tick server work: the block-highlighter finder traits. {@link GameHooks#serverTick} */
	SERVER_TICK,
	/** Server shutdown: clean up highlighter markers. {@link GameHooks#serverStopping} */
	SERVER_STOPPING,
	/** Register /randomloot. {@link GameHooks#commands} */
	COMMANDS,
	/** Config load and reload. {@link GameHooks#config} */
	CONFIG,
	/** Add the mod's items to the creative tab. {@link GameHooks#creativeTab} */
	CREATIVE_TAB,
	/** Refuse anvil-combining two pieces of gear. {@link GameHooks#blocksAnvilCombine} */
	ANVIL_COMBINE,
	/** Per-type and per-piece enchantment filtering. {@link GameHooks#enchantGate} */
	ENCHANT_GATE,
	/**
	 * Inject cases and templates into chest loot. {@link GameHooks#lootInjection(net.minecraft.resources.Identifier,
	 * net.minecraft.world.level.storage.loot.LootContext)} or its per-item form, whichever
	 * matches what the loader's hook sees.
	 */
	LOOT_INJECTION,
	/**
	 * A smithing result is being taken: trait recipes charge the rest of their item count
	 * and count as a crafted trait for advancements. {@link GameHooks#smithingTake}
	 */
	SMITHING_TAKE
}
