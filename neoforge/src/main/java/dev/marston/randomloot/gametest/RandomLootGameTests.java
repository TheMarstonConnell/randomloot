package dev.marston.randomloot.gametest;

import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.LootItem.ToolType;
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
 * <p>Shared test bodies live in {@link GameTestBodies} (common); this class holds the
 * NeoForge registration plumbing plus the NeoForge-only tests (global loot modifiers,
 * the supportsEnchantment item-extension paths).
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

		register(event, env, "modifier_roundtrip", GameTestBodies::modifierRoundTrip);
		register(event, env, "gen_tool", GameTestBodies::genTool);
		register(event, env, "gen_tool_dispenser", GameTestBodies::genToolDispenser);
		register(event, env, "dispenser_opens_case", GameTestBodies::dispenserOpensCase);
		register(event, env, "case_opens_into_hand", GameTestBodies::caseOpensIntoHand);
		register(event, env, "case_roll_reveal", GameTestBodies::caseRollReveal);
		register(event, env, "deterministic_roll_lifecycles", GameTestBodies::deterministicRollLifecycles);
		register(event, env, "break_block", GameTestBodies::breakBlock);
		register(event, env, "loot_modifiers_load", RandomLootGameTests::lootModifiersLoad);
		register(event, env, "advancements_load", GameTestBodies::advancementsLoad);
		register(event, env, "xp_level_curve", GameTestBodies::xpLevelCurve);
		register(event, env, "kill_trait_hooks", GameTestBodies::killTraitHooks);
		register(event, env, "catalyst_extends_effects", GameTestBodies::catalystExtendsEffects);
		register(event, env, "stench_debuffs_mobs", GameTestBodies::stenchDebuffsMobs);
		register(event, env, "new_trait_recipes_load", GameTestBodies::newTraitRecipesLoad);
		register(event, env, "enchant_type_filtering", RandomLootGameTests::enchantTypeFiltering);
		register(event, env, "tool_repairable", GameTestBodies::toolRepairable);
		register(event, env, "armor_components", GameTestBodies::armorComponents);
		register(event, env, "forger_world_constant", GameTestBodies::forgerWorldConstant);
		register(event, env, "dirt_place_world_forger", GameTestBodies::dirtPlaceWorldForger);
		register(event, env, "armor_xp_on_damage", GameTestBodies::armorXpOnDamage);
		register(event, env, "thorny_reflects_for_player", GameTestBodies::thornyReflectsForPlayer);
		register(event, env, "migration_restores_derived_components", GameTestBodies::migrationRestoresDerivedComponents);
		register(event, env, "featherweight_softens_fall_damage", GameTestBodies::featherweightSoftensFallDamage);
		register(event, env, "adrenaline_grants_speed", GameTestBodies::adrenalineGrantsSpeed);
		register(event, env, "bulwark_blocks_some_hits", GameTestBodies::bulwarkBlocksSomeHits);
		register(event, env, "unbreaking_skips_armor_durability", GameTestBodies::unbreakingSkipsArmorDurability);
		register(event, env, "soulbound_owner_mines_faster", GameTestBodies::soulboundOwnerMinesFaster);
		register(event, env, "enchanting_table_filters_by_type", GameTestBodies::enchantingTableFiltersByType);
		register(event, env, "axe_tool_actions", GameTestBodies::axeToolActions);
		register(event, env, "shovel_flattens", GameTestBodies::shovelFlattens);
		register(event, env, "loot_injection_adds_cases", GameTestBodies::lootInjectionAddsCases);
		register(event, env, "anvil_cannot_combine_loot_gear", GameTestBodies::anvilCannotCombineLootGear);
		register(event, env, "armor_enchant_filtering", RandomLootGameTests::armorEnchantFiltering);
		register(event, env, "armor_repairable", GameTestBodies::armorRepairable);
		register(event, env, "admin_commands", GameTestBodies::adminCommands);
		register(event, env, "smithing_trait_gating", GameTestBodies::smithingTraitGating);
		register(event, env, "smithing_craft_sequence", GameTestBodies::smithingCraftSequence);
		register(event, env, "clone_preserves_enchantments", GameTestBodies::clonePreservesEnchantments);
	}

	private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env,
			String name, Consumer<GameTestHelper> body) {
		TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(env, STRUCTURE, 200, 0, true);
		event.registerTest(Identifier.fromNamespaceAndPath(RandomLoot.MODID, name), new RLTestInstance(body, data));
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
