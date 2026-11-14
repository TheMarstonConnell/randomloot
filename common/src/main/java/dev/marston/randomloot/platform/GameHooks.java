package dev.marston.randomloot.platform;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.mojang.brigadier.CommandDispatcher;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.commands.ModCommands;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.LootInjection;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.ArmorDispatcher;
import dev.marston.randomloot.loot.modifiers.KillDispatcher;
import dev.marston.randomloot.loot.modifiers.holders.BlockHighlighter;
import dev.marston.randomloot.loot.modifiers.hurter.Soulbound;
import dev.marston.randomloot.recipes.TraitAdditionRecipe;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;

/**
 * The loader hook contract: one method per {@link GameHook}, and the only thing in common
 * a loader shim calls when the game does something.
 *
 * <p>Every method takes vanilla types, so a shim method is an unpacking of its loader's
 * event and one call here. What the event means for gear is decided behind this class,
 * once, for both loaders. Each method's javadoc is the contract for its hook: the
 * <b>stage</b> of the game event the shim must call at, the <b>side</b>, and the
 * <b>scope</b> (which entities, stacks or tables it must be called for). Where the two
 * loaders cannot deliver the same thing, the difference is written down at the method,
 * with the gametest that pins what they do share.
 *
 * <p>Calling a method records its hook as delivered. Nothing is declared by hand: the
 * {@code loader_hooks_all_bound} gametest makes the game do the thing behind each hook
 * and fails on any that never arrived, so a hook passes only when the loader's own wiring
 * (an event listener, a mixin, an item override or a data file) really reached common.
 */
public final class GameHooks {

	private static final boolean[] DELIVERED = new boolean[GameHook.values().length];

	private GameHooks() {
	}

	private static void delivered(GameHook hook) {
		DELIVERED[hook.ordinal()] = true;
	}

	/** Hooks the running loader has not delivered since the game started. */
	public static Set<GameHook> undelivered() {
		Set<GameHook> undelivered = EnumSet.noneOf(GameHook.class);
		for (GameHook hook : GameHook.values()) {
			if (!DELIVERED[hook.ordinal()]) {
				undelivered.add(hook);
			}
		}
		return undelivered;
	}

	/**
	 * {@link GameHook#KILL}: a living entity died.
	 *
	 * <p><b>Stage</b>: inside the entity's {@code die}, once per death, with the damage
	 * source that killed it. Whether the victim's drops have spawned yet is not promised:
	 * NeoForge's {@code LivingDeathEvent} comes before them, Fabric's {@code AFTER_DEATH}
	 * after. <b>Side</b>: server. NeoForge also fires its event on the client, with a
	 * generic source, when the death animation replays; common ignores that.
	 * <b>Scope</b>: every living entity, players included.
	 *
	 * <p>Pinned by {@code kill_trait_hooks}.
	 */
	public static void kill(LivingEntity victim, DamageSource source) {
		delivered(GameHook.KILL);
		KillDispatcher.onLivingDeath(victim, source);
	}

	/**
	 * {@link GameHook#DAMAGE_PRE}: a living entity is about to lose health. Returns the
	 * damage to apply instead.
	 *
	 * <p><b>Stage</b>: inside {@code actuallyHurt}, after armor, protection enchantments,
	 * Resistance and any reduction of the entity's own (a Witch's against magic); before
	 * absorption hearts soak any of it and before health changes. So {@code damage} is
	 * what is about to be taken, not the hit that was dealt, and it is 0 for a hit a
	 * shield blocked whole. <b>Side</b>: server. <b>Scope</b>: every living entity,
	 * players included, for every hit that gets as far as {@code actuallyHurt}.
	 *
	 * <p>Pinned by {@code thorny_reflects_for_player}: a player, a zombie and a witch
	 * wearer each reflect 75% of the damage they took.
	 */
	public static float damagePre(LivingEntity wearer, DamageSource source, float damage) {
		delivered(GameHook.DAMAGE_PRE);
		return ArmorDispatcher.onLivingDamagePre(wearer, source, damage);
	}

	/**
	 * {@link GameHook#DAMAGE_POST}: a living entity was hit.
	 *
	 * <p><b>Side</b>: server. <b>Scope</b>: every living entity, players included.
	 *
	 * <p><b>Stage: the two loaders do not deliver the same thing yet.</b> Which one is
	 * meant is an open question for the mod's author, so both are kept as they were:
	 * <ul>
	 * <li>NeoForge ({@code LivingDamageEvent.Post}) hands over the damage at the
	 * {@link #damagePre} stage, after that hook's traits changed it: what was taken,
	 * before absorption. It delivers at the end of {@code hurtServer}, for a killing
	 * hit too.</li>
	 * <li>Fabric ({@code ServerLivingEntityEvents.AFTER_DAMAGE}) hands over the damage
	 * after shield blocking and before armor: what was dealt. It does not deliver a
	 * killing hit, and its shim drops any hit a shield blocked part of.</li>
	 * </ul>
	 * Measured as the armor XP one hit earns:
	 * <pre>
	 *                                                      NeoForge  Fabric
	 * hit for 10 that nothing mitigates, 10 taken              10      10
	 * hit for 10, shield blocks all of it                       0       0
	 * hit for 10 against 20 armor, 4 taken                      4      10
	 * the same hit with a shield blocking half, 1.5 taken       2       0
	 * fall for 6, maxed Featherweight cancels it                0       6
	 * killing hit on a wearer with 20 max health               20       0
	 * </pre>
	 * {@code armor_xp_per_hit} pins the first two rows.
	 */
	public static void damagePost(LivingEntity wearer, float damage) {
		delivered(GameHook.DAMAGE_POST);
		ArmorDispatcher.onLivingDamagePost(wearer, damage);
	}

	/**
	 * {@link GameHook#ARMOR_HURT}: an equipped stack is about to lose durability. Returns
	 * the durability to take instead; 0 skips the loss.
	 *
	 * <p><b>Stage</b>: before the loss is applied. <b>Side</b>: server. <b>Scope</b>:
	 * at least every piece in an armor slot that is about to be damaged by a hit its
	 * wearer took. A shim may deliver more: Fabric's stand-in sees every
	 * {@code ItemStack.hurtAndBreak} that has an owner and a slot, a tool in the hand
	 * included. Common keeps the armor slots, which is what NeoForge's
	 * {@code ArmorHurtEvent} covers.
	 *
	 * <p>Pinned by {@code unbreaking_skips_armor_durability}: none lost with the trait
	 * maxed, some lost without it.
	 */
	public static float armorHurt(LivingEntity wearer, ItemStack stack, EquipmentSlot slot, float durabilityDamage) {
		delivered(GameHook.ARMOR_HURT);
		return ArmorDispatcher.onArmorHurt(wearer, stack, slot, durabilityDamage);
	}

	/**
	 * {@link GameHook#BREAK_SPEED}: a player's block-breaking speed is being worked out.
	 * Returns the speed to use instead.
	 *
	 * <p><b>Stage</b>: the end of {@code Player.getDestroySpeed}, so {@code speed}
	 * already has the tool, Efficiency, Haste, Mining Fatigue and the in-water and
	 * in-air penalties in it. <b>Side</b>: both. <b>Scope</b>: every query, for any
	 * player and any block.
	 *
	 * <p>Pinned by {@code soulbound_owner_mines_faster}: 1.15 times the speed.
	 */
	public static float breakSpeed(Player player, float speed) {
		delivered(GameHook.BREAK_SPEED);
		return Soulbound.modifyBreakSpeed(player, speed);
	}

	/**
	 * {@link GameHook#AFTER_USE_ITEM}: the server finished an item use.
	 *
	 * <p><b>Stage</b>: on return from {@code ServerPlayerGameMode.useItem}, with its
	 * result, on every return path. <b>Side</b>: server. <b>Scope</b>: every use, either
	 * hand. No loader has an event for the result, so both shims are the same mixin.
	 *
	 * <p>Pinned by {@code right_click_traits_yield_to_usable_offhand}.
	 */
	public static void afterUseItem(ServerPlayer player, InteractionHand hand, InteractionResult result) {
		delivered(GameHook.AFTER_USE_ITEM);
		LootItem.afterUseItem(player, hand, result);
	}

	/**
	 * {@link GameHook#AFTER_USE_ITEM}, for a use on a block: the same contract as
	 * {@link #afterUseItem}, on return from {@code ServerPlayerGameMode.useItemOn}.
	 */
	public static void afterUseItemOn(ServerPlayer player, InteractionHand hand, InteractionResult result) {
		delivered(GameHook.AFTER_USE_ITEM);
		LootItem.afterUseItemOn(player, hand, result);
	}

	/**
	 * {@link GameHook#SERVER_TICK}: a server tick ended.
	 *
	 * <p><b>Stage</b>: after the levels ticked. <b>Side</b>: server, on its thread.
	 * <b>Scope</b>: once per tick.
	 */
	public static void serverTick() {
		delivered(GameHook.SERVER_TICK);
		BlockHighlighter.onServerTick();
	}

	/**
	 * {@link GameHook#SERVER_STOPPING}: the server is shutting down.
	 *
	 * <p><b>Stage</b>: while its levels are still loaded. <b>Side</b>: server.
	 * <b>Scope</b>: once per server, integrated or dedicated.
	 *
	 * <p>The one hook no gametest can see, because a gametest ends before the server
	 * stops: {@code loader_hooks_all_bound} leaves it out.
	 */
	public static void serverStopping() {
		delivered(GameHook.SERVER_STOPPING);
		BlockHighlighter.onServerStopping();
	}

	/**
	 * {@link GameHook#COMMANDS}: the server is building its command tree.
	 *
	 * <p><b>Stage</b>: while {@code dispatcher} still accepts registrations.
	 * <b>Side</b>: server. <b>Scope</b>: every time the tree is built, which is at
	 * startup and again on each datapack reload.
	 *
	 * <p>Pinned by {@code admin_commands}, which runs /randomloot through the server's
	 * own dispatcher.
	 */
	public static void commands(CommandDispatcher<CommandSourceStack> dispatcher) {
		delivered(GameHook.COMMANDS);
		ModCommands.register(dispatcher);
	}

	/**
	 * {@link GameHook#CONFIG}: the config file's values changed.
	 *
	 * <p><b>Stage</b>: after {@link Config#SPEC} holds the new values. <b>Side</b>:
	 * both. <b>Scope</b>: the first load and every reload of {@code randomloot-common.toml}.
	 *
	 * <p><b>Fabric does not deliver the first load.</b> Forge Config API Port reads the
	 * file inside {@code ConfigRegistry.register}, and the Fabric shim adds its loading
	 * listener after that call, so the values in the file only arrive if the file is
	 * edited while the game runs. Measured with {@code caseChance = 0.5} in the file:
	 * NeoForge runs with 0.5, Fabric with the default 0.25. Left as found, because
	 * closing it changes what Fabric players get; {@code loader_hooks_all_bound} excuses
	 * this one hook on Fabric until it is closed.
	 */
	public static void config() {
		delivered(GameHook.CONFIG);
		Config.onLoad();
	}

	/**
	 * {@link GameHook#CREATIVE_TAB}: a creative tab's contents are being built.
	 *
	 * <p><b>Stage</b>: after vanilla filled the tab, so a stack given to {@code output}
	 * lands at its end, shown in the tab and in the search tab. <b>Side</b>: wherever
	 * tabs are built, which in play is the client. <b>Scope</b>: at least the tab
	 * {@link ModItems#CREATIVE_TAB}; a shim may deliver every tab and common ignores the
	 * others.
	 *
	 * <p>Pinned by {@code creative_tab_lists_mod_items}.
	 */
	public static void creativeTab(ResourceKey<CreativeModeTab> tab, Consumer<ItemStack> output) {
		delivered(GameHook.CREATIVE_TAB);
		ModItems.addToCreativeTab(tab, output);
	}

	/**
	 * {@link GameHook#ANVIL_COMBINE}: an anvil worked out a result for its two inputs.
	 * Returns true when it must offer nothing, which the shim then makes so.
	 *
	 * <p>Every piece of gear is one of a kind, so vanilla's combining of two of the same
	 * item would destroy the right one's identity and repair the left one for free.
	 * Repairing gear with its repair material is a different right-hand input and is not
	 * refused.
	 *
	 * <p><b>Stage</b>: after vanilla computed the result. <b>Side</b>: both.
	 * <b>Scope</b>: every time an anvil with a left input recomputes.
	 *
	 * <p>Pinned by {@code anvil_cannot_combine_loot_gear}.
	 */
	public static boolean blocksAnvilCombine(ItemStack left, ItemStack right) {
		delivered(GameHook.ANVIL_COMBINE);
		return LootUtils.isLootGear(left) && LootUtils.isLootGear(right);
	}

	/**
	 * {@link GameHook#ENCHANT_GATE}: the game asks whether an enchantment may go on a
	 * stack. Returns {@code allow} or {@code deny} when the mod's per-type filter
	 * decides, and {@code loaderDefault} when it has no opinion: the stack is not gear,
	 * or the enchantment is in none of the mod's enchantment tags.
	 *
	 * <p>The three answers are passed in because each loader spells them its own way:
	 * NeoForge wants a boolean and its default is the item's inherited check, Fabric
	 * wants a tri-state whose third value hands the question back.
	 *
	 * <p><b>Stage</b>: in place of the loader's own answer. <b>Side</b>: both.
	 * <b>Scope</b>: the enchanting table, the anvil with a book, and every other path
	 * the loader routes through its enchantability check. A shim may ask for stacks that
	 * are not gear (Fabric's event does); those get {@code loaderDefault}.
	 *
	 * <p>Pinned by {@code enchanting_table_filters_by_type} and
	 * {@code anvil_books_filter_by_type}.
	 */
	public static <T> T enchantGate(ItemStack stack, Holder<Enchantment> enchantment, T allow, T deny,
			Supplier<T> loaderDefault) {
		delivered(GameHook.ENCHANT_GATE);
		Boolean allowed = LootUtils.gearEnchantGate(stack, enchantment);
		if (allowed == null) {
			return loaderDefault.get();
		}
		return allowed ? allow : deny;
	}

	/**
	 * {@link GameHook#LOOT_INJECTION}, for a loader whose hook sees the tables as they
	 * roll: returns the stacks to add to this table's drops.
	 *
	 * <p><b>Stage</b>: after {@code table} produced its own drops. <b>Side</b>: server.
	 * <b>Scope</b>: every table with an id that rolls, a table rolled from inside
	 * another included. {@link LootInjection} keeps that to one roll per loot context.
	 * This is Fabric's shape; see {@link LootInjection} for how the two differ.
	 */
	public static List<ItemStack> lootInjection(Identifier table, LootContext context) {
		delivered(GameHook.LOOT_INJECTION);
		return LootInjection.rollOncePerContext(table, context);
	}

	/**
	 * {@link GameHook#LOOT_INJECTION}, for a loader whose hook runs once per injected
	 * item on a finished roll: returns the stacks of {@code item} to add.
	 *
	 * <p><b>Stage</b>: after the whole roll, nested tables included, produced its drops.
	 * <b>Side</b>: server. <b>Scope</b>: once per top-level roll and injected item, with
	 * the id of the table that was asked for. This is NeoForge's shape, one global loot
	 * modifier per item; see {@link LootInjection} for how the two differ.
	 */
	public static List<ItemStack> lootInjection(Identifier queriedTable, LootContext context, Item item) {
		delivered(GameHook.LOOT_INJECTION);
		return LootInjection.rollItem(queriedTable, context, item);
	}

	/**
	 * {@link GameHook#SMITHING_TAKE}: a result is leaving a smithing table.
	 *
	 * <p><b>Stage</b>: before vanilla shrinks the three inputs. <b>Side</b>: both;
	 * only the server knows which recipe was used, so the client does nothing.
	 * <b>Scope</b>: once per take, for a click and for a shift-click, for any recipe.
	 *
	 * <p>Pinned by {@code smithing_take_consumes_count} and
	 * {@code smithing_shift_click_consumes_count}.
	 */
	public static void smithingTake(SmithingMenu menu, Player player) {
		delivered(GameHook.SMITHING_TAKE);
		TraitAdditionRecipe.onTake(menu, player);
	}
}
