package dev.marston.randomloot.gametest;

import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import net.minecraft.core.BlockPos;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.LootModifierManager;
import net.neoforged.neoforge.resource.NeoForgeReloadListeners;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * In-world NeoForge GameTests for server-side logic that plain unit tests can't reach
 * (item data-components aren't bound outside a running server). Run headless with
 * {@code ./gradlew runGameTestServer}.
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

	private static final Identifier STRUCTURE = Identifier.fromNamespaceAndPath(RandomLoot.MODID, "empty");

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
		Holder<TestEnvironmentDefinition<?>> env =
				event.registerEnvironment(Identifier.fromNamespaceAndPath(RandomLoot.MODID, "default_env"));

		register(event, env, "modifier_roundtrip", RandomLootGameTests::modifierRoundTrip);
		register(event, env, "gen_tool", RandomLootGameTests::genTool);
		register(event, env, "break_block", RandomLootGameTests::breakBlock);
		register(event, env, "loot_modifiers_load", RandomLootGameTests::lootModifiersLoad);
	}

	private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env,
			String name, Consumer<GameTestHelper> body) {
		TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(env, STRUCTURE, 200, 0, true);
		event.registerTest(Identifier.fromNamespaceAndPath(RandomLoot.MODID, name), new RLTestInstance(body, data));
	}

	// --- Test bodies ---------------------------------------------------------

	/** Adding then removing a trait round-trips through the tool's data component (the smithing core). */
	private static void modifierRoundTrip(GameTestHelper helper) {
		ItemStack tool = new ItemStack(ModItems.TOOL.get());
		LootUtils.setToolType(tool, ToolType.SWORD);

		Modifier critical = ModifierRegistry.getModifier("critical");
		LootUtils.addModifier(tool, critical);
		helper.assertTrue(hasTrait(tool, "critical"), "trait should be present after addModifier");

		LootUtils.removeModifier(tool, critical);
		helper.assertFalse(hasTrait(tool, "critical"), "trait should be gone after removeModifier");

		helper.succeed();
	}

	/** genTool() produces a real, typed tool with positive goodness (covers the RNG/biome path). */
	private static void genTool(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack tool = LootUtils.genTool(player, helper.getLevel());

		helper.assertTrue(tool.is(ModItems.TOOL.get()), "genTool should produce the RandomLoot tool");
		helper.assertTrue(LootUtils.getToolType(tool) != ToolType.NULL, "generated tool should have a real tool type");
		helper.assertTrue(LootUtils.getStats(tool) > 0f, "generated tool should have positive goodness");

		helper.succeed();
	}

	/** breakBlockAsPlayer destroys a harvestable block and reports success. */
	private static void breakBlock(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos relative = new BlockPos(1, 1, 1);
		helper.setBlock(relative, Blocks.DIRT);

		BlockPos absolute = helper.absolutePos(relative);
		ItemStack tool = new ItemStack(ModItems.TOOL.get());
		LootUtils.setToolType(tool, ToolType.SHOVEL);

		boolean destroyed = LootUtils.breakBlockAsPlayer(tool, absolute, player, helper.getLevel(),
				helper.getLevel().getBlockState(absolute));

		helper.assertTrue(destroyed, "breakBlockAsPlayer should destroy a harvestable block");
		helper.assertBlockNotPresent(Blocks.DIRT, relative);

		helper.succeed();
	}

	/** Both global loot modifiers (which inject cases/templates into chest loot) actually loaded. */
	private static void lootModifiersLoad(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		LootModifierManager manager = server.getServerResources().managers()
				.getListener(NeoForgeReloadListeners.LOOT_MODIFIERS_KEY);

		helper.assertTrue(manager.getModifier(Identifier.fromNamespaceAndPath(RandomLoot.MODID, "case_dungeon")) != null,
				"case_dungeon loot modifier should be loaded");
		helper.assertTrue(manager.getModifier(Identifier.fromNamespaceAndPath(RandomLoot.MODID, "trait_dungeon")) != null,
				"trait_dungeon loot modifier should be loaded");

		helper.succeed();
	}

	private static boolean hasTrait(ItemStack tool, String tagName) {
		return LootUtils.getModifiers(tool).stream().anyMatch(m -> m.tagName().equals(tagName));
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
