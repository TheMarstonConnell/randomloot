package dev.marston.randomloot.gametest;

import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.loot.CaseLootModifier;
import dev.marston.randomloot.loot.LootInjection;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.LootModifierManager;
import net.neoforged.neoforge.resource.NeoForgeReloadListeners;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * In-world NeoForge GameTests for server-side logic that plain unit tests can't reach
 * (item data-components aren't bound outside a running server). Run headless with
 * {@code ./gradlew :neoforge:runGameTestServer}.
 *
 * <p>This is the NeoForge adapter for the gametest table ({@link GameTestTable}, common):
 * it registers every row of the table, then the NeoForge-only tests written below
 * (global loot modifiers, the supportsEnchantment item-extension paths). A shared test
 * is never declared here - its body goes in {@link GameTestBodies} and its row in the table.
 *
 * <p>26.1's gametest system is data-driven. Vanilla's {@code FunctionGameTestInstance}
 * resolves test bodies from the {@code TEST_FUNCTION} registry, which is bootstrapped
 * before mods load and has no mod hook -- so a mod can't add functions there. Instead we
 * register a small custom {@link GameTestInstance} ({@link RLTestInstance}) that holds the
 * test body directly, and bind it through {@link RegisterGameTestsEvent}. Each test uses
 * the empty structure at {@code data/randomloot/structure/empty.nbt}.
 */
public final class RandomLootGameTests {

	private RandomLootGameTests() {
	}

	// GameTestInstance.codec() must resolve to an entry in this registry.
	private static final DeferredRegister<MapCodec<? extends GameTestInstance>> TEST_INSTANCE_TYPES =
			DeferredRegister.create(Registries.TEST_INSTANCE_TYPE, RandomLoot.MODID);
	static {
		TEST_INSTANCE_TYPES.register("rl_test", () -> RLTestInstance.CODEC);
	}

	public static void init(IEventBus modEventBus) {
		TEST_INSTANCE_TYPES.register(modEventBus);
		modEventBus.addListener(RandomLootGameTests::onRegisterGameTests);
	}

	public static void onRegisterGameTests(RegisterGameTestsEvent event) {
		Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(GameTestTable.ENVIRONMENT);

		// Shared tests: whatever the common table lists, so both loaders run the same set.
		for (GameTestTable.Row row : GameTestTable.rows()) {
			register(event, env, row);
		}

		// NeoForge-only tests, same structure and tick limit as the shared ones.
		register(event, env, neoForgeOnly("loot_modifiers_load", RandomLootGameTests::lootModifiersLoad));
		register(event, env, neoForgeOnly("enchant_type_filtering", RandomLootGameTests::enchantTypeFiltering));
		register(event, env, neoForgeOnly("armor_enchant_filtering", RandomLootGameTests::armorEnchantFiltering));
	}

	private static GameTestTable.Row neoForgeOnly(String name, Consumer<GameTestHelper> body) {
		return new GameTestTable.Row(name, body, GameTestTable.DEFAULT_MAX_TICKS);
	}

	private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env,
			GameTestTable.Row row) {
		event.registerTest(row.id(), new RLTestInstance(row.body(), row.data(env)));
	}

	// --- NeoForge-only test bodies -------------------------------------------

	/** Both global loot modifiers (which inject cases/templates into chest loot) actually loaded. */
	private static void lootModifiersLoad(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		LootModifierManager manager = server.getServerResources().managers()
				.getListener(NeoForgeReloadListeners.LOOT_MODIFIERS_KEY);

		helper.assertTrue(manager.getModifier(Identifier.fromNamespaceAndPath(RandomLoot.MODID, "case_dungeon")) != null,
				"case_dungeon loot modifier should be loaded");
		helper.assertTrue(manager.getModifier(Identifier.fromNamespaceAndPath(RandomLoot.MODID, "trait_dungeon")) != null,
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

	/** A code-defined test instance: holds its body directly instead of a TEST_FUNCTION key. */
	public static final class RLTestInstance extends GameTestInstance {
		// Encodes only the TestData; decode yields a no-op body. These instances are registered
		// BUILT_IN and run in-process, so the codec is never actually round-tripped.
		public static final MapCodec<RLTestInstance> CODEC = RecordCodecBuilder.mapCodec(
				i -> i.group(TestData.CODEC.forGetter(inst -> inst.info()))
						.apply(i, info -> new RLTestInstance(helper -> {}, info)));

		private final Consumer<GameTestHelper> body;

		RLTestInstance(Consumer<GameTestHelper> body, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
			super(info);
			this.body = body;
		}

		@Override
		public void run(GameTestHelper helper) {
			this.body.accept(helper);
		}

		@Override
		public MapCodec<? extends GameTestInstance> codec() {
			return CODEC;
		}

		@Override
		protected MutableComponent typeDescription() {
			return Component.literal("RandomLoot GameTest");
		}
	}
}
