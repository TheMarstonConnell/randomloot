package dev.marston.randomloot.gametest;

import java.util.Collection;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.loot.CaseLootModifier;
import dev.marston.randomloot.loot.LootInjection;
import dev.marston.randomloot.items.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * In-world Forge GameTests for server-side logic that plain unit tests can't reach
 * (gear state only exists on a stack in a running server). Run headless with
 * {@code ./gradlew :forge:runGameTestServer}.
 *
 * <p>This is the Forge adapter for the gametest table ({@link GameTestTable}, common):
 * {@link #shared()} hands the framework every row of the table, and the Forge-only test
 * is the annotated method below (global loot modifiers). A shared test is never declared
 * here - its body goes in {@link GameTestBodies} and its row in the table.
 *
 * <p>1.20.1 uses the classic annotation-based framework (26.x's data-driven
 * GameTestInstance doesn't exist here); each test runs in the empty structure at
 * {@code data/randomloot/structures/empty.nbt}.
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
