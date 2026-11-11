package dev.marston.randomloot.gametest;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.loot.CaseLootModifier;
import dev.marston.randomloot.loot.LootInjection;
import dev.marston.randomloot.items.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * In-world Forge GameTests for server-side logic that plain unit tests can't reach
 * (item data-components aren't bound outside a running server). Run headless with
 * {@code ./gradlew :forge:runGameTestServer}.
 *
 * <p>Shared test bodies live in {@link GameTestBodies} (common); this class holds the
 * Forge registration plumbing plus the Forge-only test (global loot modifiers).
 * 1.20.1 uses the classic
 * annotation-based framework (26.x's data-driven GameTestInstance doesn't exist here);
 * each test runs in the empty structure at {@code data/randomloot/structures/empty.nbt}.
 */
@PrefixGameTestTemplate(false)
public class RandomLootGameTests {

	private static final String TEMPLATE = "empty";

	public static void init(IEventBus modEventBus) {
		modEventBus.addListener(RandomLootGameTests::onRegisterGameTests);
	}

	public static void onRegisterGameTests(RegisterGameTestsEvent event) {
		event.register(RandomLootGameTests.class);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void modifier_roundtrip(GameTestHelper helper) {
		GameTestBodies.modifierRoundTrip(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void gen_tool(GameTestHelper helper) {
		GameTestBodies.genTool(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void gen_tool_dispenser(GameTestHelper helper) {
		GameTestBodies.genToolDispenser(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void dispenser_opens_case(GameTestHelper helper) {
		GameTestBodies.dispenserOpensCase(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void case_opens_into_hand(GameTestHelper helper) {
		GameTestBodies.caseOpensIntoHand(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void case_roll_reveal(GameTestHelper helper) {
		GameTestBodies.caseRollReveal(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void deterministic_roll_lifecycles(GameTestHelper helper) {
		GameTestBodies.deterministicRollLifecycles(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void break_block(GameTestHelper helper) {
		GameTestBodies.breakBlock(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void loot_modifiers_load(GameTestHelper helper) {
		RandomLootGameTests.lootModifiersLoad(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void advancements_load(GameTestHelper helper) {
		GameTestBodies.advancementsLoad(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void xp_level_curve(GameTestHelper helper) {
		GameTestBodies.xpLevelCurve(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void kill_trait_hooks(GameTestHelper helper) {
		GameTestBodies.killTraitHooks(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void catalyst_extends_effects(GameTestHelper helper) {
		GameTestBodies.catalystExtendsEffects(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void stench_debuffs_mobs(GameTestHelper helper) {
		GameTestBodies.stenchDebuffsMobs(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void new_trait_recipes_load(GameTestHelper helper) {
		GameTestBodies.newTraitRecipesLoad(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void enchant_type_filtering(GameTestHelper helper) {
		GameTestBodies.enchantTypeFiltering(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void tool_repairable(GameTestHelper helper) {
		GameTestBodies.toolRepairable(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void armor_components(GameTestHelper helper) {
		GameTestBodies.armorComponents(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void forger_world_constant(GameTestHelper helper) {
		GameTestBodies.forgerWorldConstant(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void dirt_place_world_forger(GameTestHelper helper) {
		GameTestBodies.dirtPlaceWorldForger(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void armor_xp_on_damage(GameTestHelper helper) {
		GameTestBodies.armorXpOnDamage(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void thorny_reflects_for_player(GameTestHelper helper) {
		GameTestBodies.thornyReflectsForPlayer(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void migration_restores_derived_components(GameTestHelper helper) {
		GameTestBodies.migrationRestoresDerivedComponents(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void featherweight_softens_fall_damage(GameTestHelper helper) {
		GameTestBodies.featherweightSoftensFallDamage(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void adrenaline_grants_speed(GameTestHelper helper) {
		GameTestBodies.adrenalineGrantsSpeed(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void bulwark_blocks_some_hits(GameTestHelper helper) {
		GameTestBodies.bulwarkBlocksSomeHits(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void unbreaking_skips_armor_durability(GameTestHelper helper) {
		GameTestBodies.unbreakingSkipsArmorDurability(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void soulbound_owner_mines_faster(GameTestHelper helper) {
		GameTestBodies.soulboundOwnerMinesFaster(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void enchanting_table_filters_by_type(GameTestHelper helper) {
		GameTestBodies.enchantingTableFiltersByType(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void axe_tool_actions(GameTestHelper helper) {
		GameTestBodies.axeToolActions(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void shovel_flattens(GameTestHelper helper) {
		GameTestBodies.shovelFlattens(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void loot_injection_adds_cases(GameTestHelper helper) {
		GameTestBodies.lootInjectionAddsCases(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void loot_injection_skips_block_drops(GameTestHelper helper) {
		GameTestBodies.lootInjectionSkipsBlockDrops(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void anvil_cannot_combine_loot_gear(GameTestHelper helper) {
		GameTestBodies.anvilCannotCombineLootGear(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void armor_enchant_filtering(GameTestHelper helper) {
		GameTestBodies.armorEnchantFiltering(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void anvil_book_respects_enchant_gate(GameTestHelper helper) {
		GameTestBodies.anvilBookRespectsEnchantGate(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void armor_repairable(GameTestHelper helper) {
		GameTestBodies.armorRepairable(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void admin_commands(GameTestHelper helper) {
		GameTestBodies.adminCommands(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void smithing_trait_gating(GameTestHelper helper) {
		GameTestBodies.smithingTraitGating(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void smithing_craft_sequence(GameTestHelper helper) {
		GameTestBodies.smithingCraftSequence(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void trait_recipe_counts_load(GameTestHelper helper) {
		GameTestBodies.traitRecipeCountsLoad(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void smithing_take_consumes_count(GameTestHelper helper) {
		GameTestBodies.smithingTakeConsumesCount(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void smithing_shift_click_consumes_count(GameTestHelper helper) {
		GameTestBodies.smithingShiftClickConsumesCount(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void smithing_removal_costs_one(GameTestHelper helper) {
		GameTestBodies.smithingRemovalCostsOne(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void single_level_effects_stop_at_level_one(GameTestHelper helper) {
		GameTestBodies.singleLevelEffectsStopAtLevelOne(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void clone_preserves_enchantments(GameTestHelper helper) {
		GameTestBodies.clonePreservesEnchantments(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void loader_hooks_all_bound(GameTestHelper helper) {
		GameTestBodies.loaderHooksAllBound(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void lumbering_fells_tree(GameTestHelper helper) {
		GameTestBodies.lumberingFellsTree(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void veiny_breaks_vein(GameTestHelper helper) {
		GameTestBodies.veinyBreaksVein(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void melting_keeps_stack_count(GameTestHelper helper) {
		GameTestBodies.meltingKeepsStackCount(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void area_break_keeps_shulker_contents(GameTestHelper helper) {
		GameTestBodies.areaBreakKeepsShulkerContents(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void detecting_markers_are_discarded(GameTestHelper helper) {
		GameTestBodies.detectingMarkersAreDiscarded(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void armor_xp_is_capped(GameTestHelper helper) {
		GameTestBodies.armorXpIsCapped(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void unbreakable_tool_keeps_durability(GameTestHelper helper) {
		GameTestBodies.unbreakableToolKeepsDurability(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void loot_injection_rolls_once_per_chest(GameTestHelper helper) {
		GameTestBodies.lootInjectionRollsOncePerChest(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void early_bird_reads_pre_hit_health(GameTestHelper helper) {
		GameTestBodies.earlyBirdReadsPreHitHealth(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void stat_boost_traits_boost_melee(GameTestHelper helper) {
		GameTestBodies.statBoostTraitsBoostMelee(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void right_click_traits_yield_to_usable_offhand(GameTestHelper helper) {
		GameTestBodies.rightClickTraitsYieldToUsableOffhand(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void tool_mod_alone_does_not_reequip(GameTestHelper helper) {
		GameTestBodies.toolModAloneDoesNotReequip(helper);
	}

	// --- Forge-only test bodies -------------------------------------------

	/** Both global loot modifiers (which inject cases/templates into chest loot) actually loaded. */
	private static void lootModifiersLoad(GameTestHelper helper) {
		// 1.20.1 has no public handle on LootModifierManager; LOADED_ITEMS is filled by
		// each modifier's constructor at datapack load, so it proves both JSONs parsed.
		helper.assertTrue(CaseLootModifier.LOADED_ITEMS.contains(ModItems.CASE.get()),
				"case_dungeon loot modifier should be loaded");
		helper.assertTrue(CaseLootModifier.LOADED_ITEMS.contains(ModItems.MOD_ADD.get()),
				"trait_dungeon loot modifier should be loaded");

		// Fabric enumerates LootInjection.entries(); Forge needs one hand-written JSON
		// per entry, so an entry added without its modifier would ship on Fabric only.
		for (LootInjection.Entry entry : LootInjection.entries()) {
			helper.assertTrue(CaseLootModifier.LOADED_ITEMS.contains(entry.item()),
					"no loot modifier json injects " + entry.item()
							+ "; add one to data/randomloot/loot_modifiers/");
		}

		helper.succeed();
	}
}
