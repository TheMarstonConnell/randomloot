package dev.marston.randomloot.gametest;

import java.util.List;
import java.util.function.Consumer;

import dev.marston.randomloot.RandomLoot;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;

/**
 * The gametest table: one row per shared gametest, enumerated by both loaders.
 *
 * <p>To add a shared gametest, write its body in {@link GameTestBodies} and add one
 * row to {@link #ROWS}. Neither loader needs touching: each has an adapter whose
 * {@code @GameTestGenerator} method returns {@link #testFunctions()} (NeoForge
 * {@code RandomLootGameTests.shared}, Fabric {@code RandomLootFabricGameTests.shared}),
 * so a test cannot exist on one loader and be missing on the other. (26.x registers the
 * rows as data-driven test instances; 1.20.1 has the annotation framework, whose
 * generator methods do the same job.)
 *
 * <p>A test that only makes sense on one loader does not belong here; it stays in
 * that loader's adapter class.
 */
public final class GameTestTable {

	private GameTestTable() {
	}

	/** Ticks a test may run before it fails, unless its row says otherwise. */
	public static final int DEFAULT_MAX_TICKS = 200;

	/** The empty 3x3x3 structure every shared test runs in ({@code data/randomloot/structures/empty.nbt}). */
	public static final String STRUCTURE = RandomLoot.MODID + ":empty";

	/** The batch every shared test runs in: vanilla's default, the one an annotated test gets. */
	public static final String BATCH = "defaultBatch";

	/**
	 * One shared gametest.
	 *
	 * @param name     snake_case name of the test; its test name is {@code randomloot.<name>} on both loaders
	 * @param body     the test, a static method of {@link GameTestBodies}
	 * @param maxTicks ticks the test may run before it fails
	 */
	public record Row(String name, Consumer<GameTestHelper> body, int maxTicks) {

		/** The test name, the same on both loaders. */
		public String id() {
			return RandomLoot.MODID + "." + this.name;
		}

		/** The row as the test function 1.21.1's gametest framework runs. */
		public TestFunction testFunction() {
			return new TestFunction(BATCH, id(), STRUCTURE, this.maxTicks, 0L, true, this.body);
		}
	}

	/** Every row as a test function: what a loader's {@code @GameTestGenerator} method returns. */
	public static List<TestFunction> testFunctions() {
		return ROWS.stream().map(Row::testFunction).toList();
	}

	private static Row row(String name, Consumer<GameTestHelper> body) {
		return new Row(name, body, DEFAULT_MAX_TICKS);
	}

	private static final List<Row> ROWS = List.of(
			row("enchant_type_filtering", GameTestBodies::enchantTypeFiltering),
			row("armor_enchant_filtering", GameTestBodies::armorEnchantFiltering),
			row("anvil_book_respects_enchant_gate", GameTestBodies::anvilBookRespectsEnchantGate),
			row("gear_cannot_combine_outside_anvil", GameTestBodies::gearCannotCombineOutsideAnvil),
			row("tool_mod_alone_does_not_reequip", GameTestBodies::toolModAloneDoesNotReequip),
			row("trait_grant_adds_then_levels", GameTestBodies::traitGrantAddsThenLevels),
			row("trait_grant_refuses_what_the_gate_refuses", GameTestBodies::traitGrantRefusesWhatTheGateRefuses),
			row("trait_grant_removes", GameTestBodies::traitGrantRemoves),
			row("trait_grant_stamps_world_forger", GameTestBodies::traitGrantStampsWorldForger),
			row("trait_dispatch_runs_pre_hit_traits_first", GameTestBodies::traitDispatchRunsPreHitTraitsFirst),
			row("trait_dispatch_leaves_rolling_gear_inert", GameTestBodies::traitDispatchLeavesRollingGearInert),
			row("trait_dispatch_saves_only_changed_traits", GameTestBodies::traitDispatchSavesOnlyChangedTraits),
			row("unbreaking_rolls_per_durability_point", GameTestBodies::unbreakingRollsPerDurabilityPoint),
			row("stamp_writes_what_the_gear_roll_decided", GameTestBodies::stampWritesWhatTheGearRollDecided),
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
			row("armor_xp_per_hit", GameTestBodies::armorXpPerHit),
			row("anvil_books_filter_by_type", GameTestBodies::anvilBooksFilterByType),
			row("creative_tab_lists_mod_items", GameTestBodies::creativeTabListsModItems),
			// Also what writes the docs, in a run started with RL_PROD=false (see GenWiki).
			row("wiki_generates", GameTestBodies::wikiGenerates),
			row("wiki_needs_every_trait_recipe", GameTestBodies::wikiNeedsEveryTraitRecipe));

	/** Every shared gametest, in the order the loaders register them. */
	public static List<Row> rows() {
		return ROWS;
	}
}
