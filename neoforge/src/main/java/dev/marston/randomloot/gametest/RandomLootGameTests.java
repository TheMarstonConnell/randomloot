package dev.marston.randomloot.gametest;

import java.util.Collection;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.loot.CaseLootModifier;
import dev.marston.randomloot.loot.LootInjection;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
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
 * <p>This is the NeoForge adapter for the gametest table ({@link GameTestTable}, common):
 * {@link #shared()} hands the framework every row of the table, and the NeoForge-only
 * tests are the annotated methods below (global loot modifiers, the supportsEnchantment
 * item-extension paths). A shared test is never declared here - its body goes in
 * {@link GameTestBodies} and its row in the table.
 *
 * <p>1.21.1 uses the classic annotation-based framework (26.x's data-driven
 * GameTestInstance doesn't exist here); each test runs in the empty structure at
 * {@code data/randomloot/structure/empty.nbt}.
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

	/** Shared tests: whatever the common table lists, so both loaders run the same set. */
	@GameTestGenerator
	public static Collection<TestFunction> shared() {
		return GameTestTable.testFunctions();
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void loot_modifiers_load(GameTestHelper helper) {
		RandomLootGameTests.lootModifiersLoad(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void enchant_type_filtering(GameTestHelper helper) {
		RandomLootGameTests.enchantTypeFiltering(helper);
	}

	@GameTest(template = TEMPLATE, templateNamespace = RandomLoot.MODID, timeoutTicks = 200)
	public void armor_enchant_filtering(GameTestHelper helper) {
		RandomLootGameTests.armorEnchantFiltering(helper);
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
