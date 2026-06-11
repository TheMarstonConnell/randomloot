package dev.marston.randomloot.gametest;

import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.LootArmorItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.NameGenerator;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.Equippable;
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
		register(event, env, "gen_tool_dispenser", RandomLootGameTests::genToolDispenser);
		register(event, env, "break_block", RandomLootGameTests::breakBlock);
		register(event, env, "loot_modifiers_load", RandomLootGameTests::lootModifiersLoad);
		register(event, env, "advancements_load", RandomLootGameTests::advancementsLoad);
		register(event, env, "xp_level_curve", RandomLootGameTests::xpLevelCurve);
		register(event, env, "kill_trait_hooks", RandomLootGameTests::killTraitHooks);
		register(event, env, "enchant_type_filtering", RandomLootGameTests::enchantTypeFiltering);
		register(event, env, "tool_repairable", RandomLootGameTests::toolRepairable);
		register(event, env, "armor_components", RandomLootGameTests::armorComponents);
		register(event, env, "forger_world_constant", RandomLootGameTests::forgerWorldConstant);
		register(event, env, "dirt_place_world_forger", RandomLootGameTests::dirtPlaceWorldForger);
		register(event, env, "armor_xp_on_damage", RandomLootGameTests::armorXpOnDamage);
		register(event, env, "armor_enchant_filtering", RandomLootGameTests::armorEnchantFiltering);
		register(event, env, "armor_repairable", RandomLootGameTests::armorRepairable);
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

	/** genTool() produces a real, typed item with positive goodness (covers the RNG/biome path). */
	private static void genTool(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack tool = LootUtils.genTool(player, helper.getLevel());

		ToolType type = LootUtils.getToolType(tool);
		helper.assertTrue(type != ToolType.NULL, "generated item should have a real type");
		if (type.isArmor()) {
			helper.assertTrue(tool.is(ModItems.ARMOR.get()), "armor rolls should produce the RandomLoot armor item");
			helper.assertTrue(tool.get(DataComponents.EQUIPPABLE) != null, "generated armor should be equippable");
		} else {
			helper.assertTrue(tool.is(ModItems.TOOL.get()), "tool rolls should produce the RandomLoot tool");
		}
		helper.assertTrue(LootUtils.getStats(tool) > 0f, "generated item should have positive goodness");

		helper.succeed();
	}

	/**
	 * genTool() with no player (the dispenser path) records the dispense position's
	 * biome and still rolls positive goodness off the machine curve.
	 */
	private static void genToolDispenser(GameTestHelper helper) {
		ItemStack tool = LootUtils.genTool(null, helper.getLevel(), helper.absolutePos(BlockPos.ZERO));

		helper.assertTrue(LootUtils.getToolType(tool) != ToolType.NULL, "dispensed item should have a real type");
		helper.assertTrue(LootUtils.getStats(tool) > 0f, "dispensed item should have positive goodness");

		String biomeKey = LootUtils.getBiomeKey(tool);
		helper.assertTrue(!biomeKey.isEmpty() && !biomeKey.equals("unknown"),
				"dispensed item should record the dispenser's biome, got: " + biomeKey);

		helper.succeed();
	}

	/**
	 * Building an armor piece wires up the per-stack EQUIPPABLE component (slot + worn
	 * texture asset) and produces positive defense and durability.
	 */
	private static void armorComponents(GameTestHelper helper) {
		ItemStack helmet = new ItemStack(ModItems.ARMOR.get());
		LootUtils.setStats(helmet, 4.0f);
		LootUtils.setToolType(helmet, ToolType.HELMET);
		LootUtils.setTexture(helmet, 3);

		Equippable equippable = helmet.get(DataComponents.EQUIPPABLE);
		helper.assertTrue(equippable != null, "armor should carry an EQUIPPABLE component after setTexture");
		helper.assertTrue(equippable.slot() == EquipmentSlot.HEAD, "helmet should equip in the head slot");
		helper.assertTrue(equippable.assetId().isPresent()
						&& equippable.assetId().get().identifier().toString().equals("randomloot:set4"),
				"texture index 3 should map to equipment asset randomloot:set4");

		helper.assertTrue(LootArmorItem.getDefense(helmet, ToolType.HELMET) > 0f,
				"helmet should have positive defense");
		helper.assertTrue(helmet.getMaxDamage() > 0, "helmet should have positive durability");

		// Texture cycling wraps within the armor set count and keeps the asset in sync.
		LootUtils.addTexture(helmet, LootUtils.ARMOR_SET_COUNT - 3);
		Equippable wrapped = helmet.get(DataComponents.EQUIPPABLE);
		helper.assertTrue(wrapped != null && wrapped.assetId().isPresent()
						&& wrapped.assetId().get().identifier().toString().equals("randomloot:set1"),
				"texture cycling should wrap back to equipment asset randomloot:set1");

		helper.succeed();
	}

	/** Every item generated in the same world (same temperature band) credits the same forger. */
	private static void forgerWorldConstant(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();

		String firstForger = null;
		for (int i = 0; i < 4; i++) {
			ItemStack item = LootUtils.genTool(player, helper.getLevel());
			String lore = LootUtils.getItemLore(item);

			int at = lore.indexOf("forged by ");
			helper.assertTrue(at >= 0, "item lore should credit a forger, got: " + lore);
			String forger = lore.substring(at + "forged by ".length()).replace(".", "");

			if (firstForger == null) {
				firstForger = forger;
			}
			helper.assertTrue(firstForger.equals(forger),
					"forger should be world-constant, got " + forger + " and " + firstForger);
		}

		helper.succeed();
	}

	/** DirtPlace's "<Forger>'s Grace" name resolves to this world's temperate forger. */
	private static void dirtPlaceWorldForger(GameTestHelper helper) {
		long seed = helper.getLevel().getSeed();

		ItemStack tool = new ItemStack(ModItems.TOOL.get());
		LootUtils.setToolType(tool, ToolType.SHOVEL);
		LootUtils.addModifier(tool, ModifierRegistry.getModifier("dirt_place").forWorld(seed));

		Modifier dirtPlace = LootUtils.getModifiers(tool).stream()
				.filter(m -> m.tagName().equals("dirt_place")).findFirst().orElse(null);
		helper.assertTrue(dirtPlace != null, "dirt_place trait should be on the tool");

		String expected = NameGenerator.forgerForWorld(seed, 0.5f) + "'s Grace";
		helper.assertTrue(dirtPlace.name().equals(expected),
				"dirt_place should be named after the world's forger, got " + dirtPlace.name()
						+ " expected " + expected);

		helper.succeed();
	}

	/** Worn Random Armor gains XP when its wearer takes damage (the armor leveling path). */
	private static void armorXpOnDamage(GameTestHelper helper) {
		ItemStack chest = new ItemStack(ModItems.ARMOR.get());
		LootUtils.setToolType(chest, ToolType.CHESTPLATE);
		LootUtils.setTexture(chest, 0);

		LivingEntity zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
		zombie.setItemSlot(EquipmentSlot.CHEST, chest);

		helper.hurt(zombie, zombie.damageSources().generic(), 6.0f);

		ItemStack worn = zombie.getItemBySlot(EquipmentSlot.CHEST);
		helper.assertTrue(LootUtils.getXP(worn) > 0 || LootUtils.getLevel(worn) > 0,
				"worn armor should gain XP from its wearer taking damage");

		helper.succeed();
	}

	/**
	 * Armor enchantment compatibility is filtered per piece via the randomloot enchantment
	 * tags, mirroring the per-tool-type filtering.
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

	/** The anvil's material-repair path accepts items from the armor_repair_materials tag. */
	private static void armorRepairable(GameTestHelper helper) {
		ItemStack armor = new ItemStack(ModItems.ARMOR.get());

		helper.assertTrue(armor.isValidRepairItem(new ItemStack(Items.DIAMOND)),
				"diamond should repair Random Armor (armor_repair_materials tag)");
		helper.assertFalse(armor.isValidRepairItem(new ItemStack(Items.STICK)),
				"stick should not repair Random Armor");

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

	/**
	 * Kill traits fire through the LivingDeathEvent dispatcher. Hailey's Wrath spawns a
	 * bee and Nemesis records the kill on the tool's NBT. The regression was both traits
	 * checking victim health inside hurtEnemy, which never sees indirect kills and made
	 * Hailey's Wrath effectively dead.
	 */
	private static void killTraitHooks(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack tool = new ItemStack(ModItems.TOOL.get());
		LootUtils.setToolType(tool, ToolType.SWORD);
		LootUtils.addModifier(tool, ModifierRegistry.getModifier("haileys_wrath"));
		LootUtils.addModifier(tool, ModifierRegistry.getModifier("nemesis"));
		player.setItemInHand(InteractionHand.MAIN_HAND, tool);

		LivingEntity zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
		helper.hurt(zombie, player.damageSources().playerAttack(player), 1000.0f);

		helper.assertTrue(zombie.isDeadOrDying(), "zombie should die to the test hit");
		helper.assertEntityPresent(EntityType.BEE);

		Modifier nemesis = LootUtils.getModifiers(player.getMainHandItem()).stream()
				.filter(m -> m.tagName().equals("nemesis")).findFirst().orElse(null);
		helper.assertTrue(nemesis != null, "nemesis trait should still be on the tool");
		int zombieKills = nemesis.toNBT().getCompoundOrEmpty("killCounts").getIntOr("minecraft:zombie", 0);
		helper.assertTrue(zombieKills == 1, "nemesis should record the zombie kill, got " + zombieKills);

		helper.succeed();
	}

	/**
	 * A single XP dump crosses each level threshold at that level's own cost. Level 0->1
	 * costs 500 and 1->2 costs 1000, so 1600 XP lands at exactly level 2 with 100 left
	 * over. The regression was the loop reusing the starting level's threshold, which
	 * turned the same 1600 XP into three 500-cost levels.
	 */
	private static void xpLevelCurve(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack tool = new ItemStack(ModItems.TOOL.get());
		LootUtils.setToolType(tool, ToolType.PICKAXE);

		LootUtils.addXp(tool, player, 1600);

		helper.assertTrue(LootUtils.getLevel(tool) == 2,
				"1600 XP should reach exactly level 2, got " + LootUtils.getLevel(tool));
		helper.assertTrue(LootUtils.getXP(tool) == 100,
				"100 XP should remain after leveling, got " + LootUtils.getXP(tool));

		helper.succeed();
	}

	/**
	 * Every shipped advancement parsed and loaded. A malformed advancement JSON (or an
	 * unregistered criterion trigger id) is silently dropped at datapack load with only a
	 * log line, so this is the regression net for the whole advancement tab.
	 */
	private static void advancementsLoad(GameTestHelper helper) {
		ServerAdvancementManager advancements = helper.getLevel().getServer().getAdvancements();

		String[] ids = { "root", "open_case", "open_cases_10", "open_cases_25", "all_tool_types",
				"tool_level_1", "tool_level_5", "tool_level_10", "get_template", "swap_template",
				"add_trait", "trait_count_4", "biome_trait", "void_teleport", "executioner_kill",
				"lightning_strike", "beekeeper" };

		for (String id : ids) {
			helper.assertTrue(advancements.get(Identifier.fromNamespaceAndPath(RandomLoot.MODID, id)) != null,
					"advancement " + id + " should be loaded");
		}

		helper.succeed();
	}

	/**
	 * Enchantment compatibility is filtered per tool type via the randomloot enchantment
	 * tags. supportsEnchantment is what the anvil checks - the regression here was a sword
	 * accepting an efficiency book because the single tool item is in every
	 * minecraft:enchantable/* tag.
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

	/** The anvil's material-repair path accepts items from the tool_repair_materials tag. */
	private static void toolRepairable(GameTestHelper helper) {
		ItemStack tool = new ItemStack(ModItems.TOOL.get());

		helper.assertTrue(tool.isValidRepairItem(new ItemStack(Items.DIAMOND)),
				"diamond should repair Random Tools (tool_repair_materials tag)");
		helper.assertFalse(tool.isValidRepairItem(new ItemStack(Items.STICK)),
				"stick should not repair Random Tools");

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
