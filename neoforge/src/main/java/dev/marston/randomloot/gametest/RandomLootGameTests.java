package dev.marston.randomloot.gametest;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.loot.CaseLootModifier;
import dev.marston.randomloot.loot.LootInjection;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * In-world NeoForge GameTests for server-side logic that plain unit tests can't reach
 * (item data-components aren't bound outside a running server). Run headless with
 * {@code ./gradlew :neoforge:runGameTestServer}.
 *
 * <p>Shared test bodies live in {@link GameTestBodies} (common); this class holds the
 * NeoForge registration plumbing plus the NeoForge-only tests (global loot modifiers,
 * the supportsEnchantment item-extension paths). 1.21.1 uses the classic
 * annotation-based framework (26.x's data-driven GameTestInstance doesn't exist here);
 * each test runs in the empty structure at {@code data/randomloot/structure/empty.nbt}.
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
		RandomLootGameTests.enchantTypeFiltering(helper);
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
		RandomLootGameTests.armorEnchantFiltering(helper);
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
	public void clone_preserves_enchantments(GameTestHelper helper) {
		GameTestBodies.clonePreservesEnchantments(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void loader_hooks_all_bound(GameTestHelper helper) {
		GameTestBodies.loaderHooksAllBound(helper);
	}

	// --- NeoForge-only test bodies -------------------------------------------

	/** Both global loot modifiers (which inject cases/templates into chest loot) actually loaded. */
	private static void lootModifiersLoad(GameTestHelper helper) {
		// 1.21.1 has no public handle on LootModifierManager; LOADED_ITEMS is filled by
		// each modifier's constructor at datapack load, so it proves both JSONs parsed.
		helper.assertTrue(CaseLootModifier.LOADED_ITEMS.contains(ModItems.CASE.get()),
				"case_dungeon loot modifier should be loaded");
		helper.assertTrue(CaseLootModifier.LOADED_ITEMS.contains(ModItems.MOD_ADD.get()),
				"trait_dungeon loot modifier should be loaded");

		// Fabric enumerates LootInjection.entries(); NeoForge needs one hand-written JSON
		// per entry, so an entry added without its modifier would ship on Fabric only.
		for (LootInjection.Entry entry : LootInjection.entries()) {
			helper.assertTrue(CaseLootModifier.LOADED_ITEMS.contains(entry.item()),
					"no loot modifier json injects " + entry.item()
							+ "; add one to data/randomloot/loot_modifiers/");
		}

		helper.succeed();
	}

	/**
	 * Enchantment compatibility is filtered per tool type via the randomloot enchantment
	 * tags. supportsEnchantment (a NeoForge item extension; Fabric goes through
	 * EnchantmentEvents.ALLOW_ENCHANTING instead) is what the anvil checks - the
	 * regression here was a sword accepting an efficiency book because the single tool
	 * item is in every minecraft:enchantable/* tag.
	 */
	private static void enchantTypeFiltering(GameTestHelper helper) {
		var enchants = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		Holder<Enchantment> efficiency = enchants.getOrThrow(Enchantments.EFFICIENCY);
		Holder<Enchantment> sharpness = enchants.getOrThrow(Enchantments.SHARPNESS);
		Holder<Enchantment> unbreaking = enchants.getOrThrow(Enchantments.UNBREAKING);

		ItemStack sword = new ItemStack(ModItems.TOOL.get());
		LootUtils.setToolType(sword, ToolType.SWORD);
		ItemStack pickaxe = new ItemStack(ModItems.TOOL.get());
		LootUtils.setToolType(pickaxe, ToolType.PICKAXE);

		helper.assertFalse(sword.supportsEnchantment(efficiency), "sword must not accept efficiency (anvil path)");
		helper.assertTrue(sword.supportsEnchantment(sharpness), "sword should accept sharpness");
		helper.assertTrue(pickaxe.supportsEnchantment(efficiency), "pickaxe should accept efficiency");
		helper.assertFalse(pickaxe.supportsEnchantment(sharpness), "pickaxe must not accept sharpness");
		helper.assertTrue(sword.supportsEnchantment(unbreaking) && pickaxe.supportsEnchantment(unbreaking),
				"unbreaking should fit every tool");

		helper.assertFalse(sword.isPrimaryItemFor(efficiency), "sword must not roll efficiency at the table");
		helper.assertTrue(pickaxe.isPrimaryItemFor(efficiency), "pickaxe should roll efficiency at the table");

		helper.succeed();
	}

	/**
	 * Armor enchantment compatibility is filtered per piece via the randomloot enchantment
	 * tags, mirroring the per-tool-type filtering (NeoForge supportsEnchantment path).
	 */
	private static void armorEnchantFiltering(GameTestHelper helper) {
		var enchants = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		Holder<Enchantment> protection = enchants.getOrThrow(Enchantments.PROTECTION);
		Holder<Enchantment> featherFalling = enchants.getOrThrow(Enchantments.FEATHER_FALLING);
		Holder<Enchantment> respiration = enchants.getOrThrow(Enchantments.RESPIRATION);
		Holder<Enchantment> sharpness = enchants.getOrThrow(Enchantments.SHARPNESS);

		ItemStack helmet = new ItemStack(ModItems.ARMOR.get());
		LootUtils.setToolType(helmet, ToolType.HELMET);
		ItemStack boots = new ItemStack(ModItems.ARMOR.get());
		LootUtils.setToolType(boots, ToolType.BOOTS);

		helper.assertTrue(helmet.supportsEnchantment(protection), "helmet should accept protection");
		helper.assertTrue(boots.supportsEnchantment(protection), "boots should accept protection");
		helper.assertTrue(helmet.supportsEnchantment(respiration), "helmet should accept respiration");
		helper.assertFalse(boots.supportsEnchantment(respiration), "boots must not accept respiration");
		helper.assertTrue(boots.supportsEnchantment(featherFalling), "boots should accept feather falling");
		helper.assertFalse(helmet.supportsEnchantment(featherFalling), "helmet must not accept feather falling");
		helper.assertFalse(helmet.supportsEnchantment(sharpness), "armor must not accept sharpness");

		helper.succeed();
	}
}
