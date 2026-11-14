package dev.marston.randomloot.gametest;

import java.util.List;
import java.util.function.Consumer;

import dev.marston.randomloot.RandomLoot;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;

/**
 * The gametest table: one row per shared gametest, enumerated by both loaders.
 *
 * <p>To add a shared gametest, write its body in {@link GameTestBodies} and add one
 * row to {@link #ROWS}. Neither loader needs touching: each has an adapter that
 * registers whatever {@link #rows()} returns (NeoForge
 * {@code RandomLootGameTests.onRegisterGameTests}, Fabric
 * {@code RandomLootFabricGameTests.registerShared}), so a test cannot exist on one
 * loader and be missing on the other.
 *
 * <p>A test that only makes sense on one loader does not belong here; it stays in
 * that loader's adapter class.
 */
public final class GameTestTable {

	private GameTestTable() {
	}

	/** Ticks a test may run before it fails, unless its row says otherwise. */
	public static final int DEFAULT_MAX_TICKS = 200;

	/** The empty 3x3x3 structure every shared test runs in ({@code data/randomloot/structure/empty.nbt}). */
	public static final Identifier STRUCTURE = Identifier.fromNamespaceAndPath(RandomLoot.MODID, "empty");

	/** The test environment every shared test runs in. Each loader registers it, empty, under this id. */
	public static final Identifier ENVIRONMENT = Identifier.fromNamespaceAndPath(RandomLoot.MODID, "default_env");

	/**
	 * One shared gametest.
	 *
	 * @param name     snake_case path of the test id, which is {@code randomloot:<name>} on both loaders
	 * @param body     the test, a static method of {@link GameTestBodies}
	 * @param maxTicks ticks the test may run before it fails
	 */
	public record Row(String name, Consumer<GameTestHelper> body, int maxTicks) {

		/** The test id, the same on both loaders. */
		public Identifier id() {
			return Identifier.fromNamespaceAndPath(RandomLoot.MODID, this.name);
		}

		/** How the test is set up and run, given the loader's holder for {@link GameTestTable#ENVIRONMENT}. */
		public TestData<Holder<TestEnvironmentDefinition<?>>> data(Holder<TestEnvironmentDefinition<?>> environment) {
			return new TestData<>(environment, STRUCTURE, this.maxTicks, 0, true);
		}
	}

	private static Row row(String name, Consumer<GameTestHelper> body) {
		return new Row(name, body, DEFAULT_MAX_TICKS);
	}

	private static final List<Row> ROWS = List.of(
			row("modifier_roundtrip", GameTestBodies::modifierRoundTrip),
			row("gen_tool", GameTestBodies::genTool),
			row("gen_tool_dispenser", GameTestBodies::genToolDispenser),
			row("dispenser_opens_case", GameTestBodies::dispenserOpensCase),
			row("case_opens_into_hand", GameTestBodies::caseOpensIntoHand),
			row("case_roll_reveal", GameTestBodies::caseRollReveal),
			row("deterministic_roll_lifecycles", GameTestBodies::deterministicRollLifecycles),
			row("break_block", GameTestBodies::breakBlock),
			row("advancements_load", GameTestBodies::advancementsLoad),
			row("xp_level_curve", GameTestBodies::xpLevelCurve),
			row("level_up_rolls_trait", GameTestBodies::levelUpRollsTrait),
			row("disabled_trait_is_filtered", GameTestBodies::disabledTraitIsFiltered),
			row("kill_trait_hooks", GameTestBodies::killTraitHooks),
			row("catalyst_extends_effects", GameTestBodies::catalystExtendsEffects),
			row("stench_debuffs_mobs", GameTestBodies::stenchDebuffsMobs),
			row("new_trait_recipes_load", GameTestBodies::newTraitRecipesLoad),
			row("tool_repairable", GameTestBodies::toolRepairable),
			row("armor_components", GameTestBodies::armorComponents),
			row("forger_world_constant", GameTestBodies::forgerWorldConstant),
			row("dirt_place_world_forger", GameTestBodies::dirtPlaceWorldForger),
			row("armor_xp_on_damage", GameTestBodies::armorXpOnDamage),
			row("thorny_reflects_for_player", GameTestBodies::thornyReflectsForPlayer),
			row("migration_restores_derived_components", GameTestBodies::migrationRestoresDerivedComponents),
			row("featherweight_softens_fall_damage", GameTestBodies::featherweightSoftensFallDamage),
			row("adrenaline_grants_speed", GameTestBodies::adrenalineGrantsSpeed),
			row("bulwark_blocks_some_hits", GameTestBodies::bulwarkBlocksSomeHits),
			row("unbreaking_skips_armor_durability", GameTestBodies::unbreakingSkipsArmorDurability),
			row("soulbound_owner_mines_faster", GameTestBodies::soulboundOwnerMinesFaster),
			row("enchanting_table_filters_by_type", GameTestBodies::enchantingTableFiltersByType),
			row("axe_tool_actions", GameTestBodies::axeToolActions),
			row("shovel_flattens", GameTestBodies::shovelFlattens),
			row("loot_injection_adds_cases", GameTestBodies::lootInjectionAddsCases),
			row("loot_injection_skips_block_drops", GameTestBodies::lootInjectionSkipsBlockDrops),
			row("anvil_cannot_combine_loot_gear", GameTestBodies::anvilCannotCombineLootGear),
			row("armor_repairable", GameTestBodies::armorRepairable),
			row("admin_commands", GameTestBodies::adminCommands),
			row("smithing_trait_gating", GameTestBodies::smithingTraitGating),
			row("smithing_craft_sequence", GameTestBodies::smithingCraftSequence),
			row("trait_recipe_counts_load", GameTestBodies::traitRecipeCountsLoad),
			row("smithing_take_consumes_count", GameTestBodies::smithingTakeConsumesCount),
			row("smithing_shift_click_consumes_count", GameTestBodies::smithingShiftClickConsumesCount),
			row("smithing_removal_costs_one", GameTestBodies::smithingRemovalCostsOne),
			row("chaotic_grants_roll_own_seeds", GameTestBodies::chaoticGrantsRollOwnSeeds),
			row("single_level_effects_stop_at_level_one", GameTestBodies::singleLevelEffectsStopAtLevelOne),
			row("clone_preserves_enchantments", GameTestBodies::clonePreservesEnchantments),
			row("loader_hooks_all_bound", GameTestBodies::loaderHooksAllBound),
			row("lumbering_fells_tree", GameTestBodies::lumberingFellsTree),
			row("veiny_breaks_vein", GameTestBodies::veinyBreaksVein),
			row("melting_keeps_stack_count", GameTestBodies::meltingKeepsStackCount),
			row("area_break_keeps_shulker_contents", GameTestBodies::areaBreakKeepsShulkerContents),
			row("detecting_markers_are_discarded", GameTestBodies::detectingMarkersAreDiscarded),
			row("armor_xp_is_capped", GameTestBodies::armorXpIsCapped),
			row("unbreakable_tool_keeps_durability", GameTestBodies::unbreakableToolKeepsDurability),
			row("loot_injection_rolls_once_per_chest", GameTestBodies::lootInjectionRollsOncePerChest),
			row("early_bird_reads_pre_hit_health", GameTestBodies::earlyBirdReadsPreHitHealth),
			row("stat_boost_traits_boost_melee", GameTestBodies::statBoostTraitsBoostMelee),
			row("right_click_traits_yield_to_usable_offhand", GameTestBodies::rightClickTraitsYieldToUsableOffhand),
			// Also what writes the docs, in a run started with RL_PROD=false (see GenWiki).
			row("wiki_generates", GameTestBodies::wikiGenerates),
			row("wiki_needs_every_trait_recipe", GameTestBodies::wikiNeedsEveryTraitRecipe));

	/** Every shared gametest, in the order the loaders register them. */
	public static List<Row> rows() {
		return ROWS;
	}
}
