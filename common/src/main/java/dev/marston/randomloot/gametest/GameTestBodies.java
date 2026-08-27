package dev.marston.randomloot.gametest;

import dev.marston.randomloot.loot.NbtCompat;

import java.util.List;
import java.util.stream.Stream;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.LootArmorItem;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.GearTags;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.NameGenerator;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import dev.marston.randomloot.platform.GameHook;
import dev.marston.randomloot.platform.GameHooks;
import dev.marston.randomloot.recipes.TraitAdditionRecipe;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.commands.Commands;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Loader-neutral gametest bodies, shared by the NeoForge registration
 * (RandomLootGameTests) and the Fabric one (RandomLootFabricGameTests).
 * Everything here uses only vanilla + common APIs.
 */
public final class GameTestBodies {

	private GameTestBodies() {
	}

	/** Adding then removing a trait round-trips through the tool's data component (the smithing core). */
	public static void modifierRoundTrip(GameTestHelper helper) {
		ItemStack tool = newTool(ToolType.SWORD);

		Modifier critical = ModifierRegistry.getModifier("critical");
		LootUtils.addModifier(tool, critical);
		helper.assertTrue(hasTrait(tool, "critical"), "trait should be present after addModifier");

		LootUtils.removeModifier(tool, critical);
		helper.assertFalse(hasTrait(tool, "critical"), "trait should be gone after removeModifier");

		helper.succeed();
	}

	/** genTool() produces a real, typed item with positive goodness (covers the RNG/biome path). */
	public static void genTool(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack tool = LootUtils.genTool(player, helper.getLevel());

		ToolType type = LootUtils.getToolType(tool);
		helper.assertTrue(type != ToolType.NULL, "generated item should have a real type");
		if (type.isArmor()) {
			helper.assertTrue(tool.is(ModItems.ARMOR.get()), "armor rolls should produce the RandomLoot armor item");
			helper.assertTrue(LootUtils.wearableSlot(tool) != null, "generated armor should be equippable");
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
	public static void genToolDispenser(GameTestHelper helper) {
		ItemStack tool = LootUtils.genTool(null, helper.getLevel(), helper.absolutePos(BlockPos.ZERO));

		helper.assertTrue(LootUtils.getToolType(tool) != ToolType.NULL, "dispensed item should have a real type");
		helper.assertTrue(LootUtils.getStats(tool) > 0f, "dispensed item should have positive goodness");

		String biomeKey = LootUtils.getBiomeKey(tool);
		helper.assertTrue(!biomeKey.isEmpty() && !biomeKey.equals("unknown"),
				"dispensed item should record the dispenser's biome, got: " + biomeKey);

		helper.succeed();
	}

	/**
	 * A redstone-fired dispenser consumes a Loot Case and ejects generated gear instead
	 * of the case itself. The regression was initDispenser() never being called, which
	 * left the vanilla item-eject behavior in place.
	 */
	public static void dispenserOpensCase(GameTestHelper helper) {
		BlockPos dispenserPos = new BlockPos(1, 1, 1);
		helper.setBlock(dispenserPos,
				Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.UP));

		DispenserBlockEntity dispenser = (DispenserBlockEntity) helper.getLevel()
				.getBlockEntity(helper.absolutePos(dispenserPos));
		dispenser.setItem(0, new ItemStack(ModItems.CASE.get()));

		helper.setBlock(new BlockPos(0, 1, 1), Blocks.REDSTONE_BLOCK);

		helper.succeedWhen(() -> {
			Vec3 center = Vec3.atCenterOf(helper.absolutePos(dispenserPos));
			List<ItemEntity> items = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
					new AABB(center, center).inflate(8.0));
			helper.assertFalse(items.stream().anyMatch(e -> e.getItem().is(ModItems.CASE.get())),
					"dispenser should consume the case, not eject it");
			helper.assertTrue(
					items.stream().anyMatch(
							e -> e.getItem().is(ModItems.TOOL.get()) || e.getItem().is(ModItems.ARMOR.get())),
					"dispenser should eject generated loot gear");
		});
	}

	/**
	 * Opening a Loot Case puts the generated gear in the hand that held the case,
	 * even when every other inventory slot is full. The regression was the tool
	 * being added to the first free inventory slot instead of the freed hand slot.
	 */
	public static void caseOpensIntoHand(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);

		Inventory inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			inv.setItem(i, new ItemStack(Items.STONE, 64));
		}
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CASE.get()));

		player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);

		ItemStack held = player.getMainHandItem();
		helper.assertTrue(held.is(ModItems.TOOL.get()) || held.is(ModItems.ARMOR.get()),
				"opening a case should put the generated gear in the hand that held it, got: " + held);
		helper.assertTrue(player.getStats().getValue(Stats.ITEM_USED.get(ModItems.CASE.get())) == 1,
				"opening one case through the server interaction path should award exactly one use stat");
		assertAdvancementDone(helper, player, "open_case");

		helper.succeed();
	}

	/**
	 * Case-opened gear starts in the rolling state - no name, no worn-armor component,
	 * no attributes - and reveals all three once the roll's game time elapses. The
	 * production inventoryTick entry point is driven manually because gametest mock
	 * players don't reliably tick their inventories.
	 */
	public static void caseRollReveal(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CASE.get()));

		player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);

		ItemStack held = player.getMainHandItem();
		helper.assertTrue(LootUtils.isRolling(held), "freshly opened gear should be rolling");
		helper.assertTrue(held.get(DataComponents.CUSTOM_NAME) == null, "rolling gear should hide its name");
		helper.assertTrue(LootUtils.wearableSlot(held) == null, "rolling gear should not be equippable");
		helper.assertTrue(heldAttributes(held).modifiers().isEmpty(),
				"rolling gear should grant no attributes");
		helper.assertTrue(LootUtils.getRollEnd(held) == helper.getLevel().getGameTime() + LootUtils.ROLL_TICKS,
				"the roll should end ROLL_TICKS after opening");

		helper.onEachTick(() -> {
			ItemStack inHand = player.getMainHandItem();
			if (LootUtils.isRolling(inHand)) {
				inHand.inventoryTick(helper.getLevel(), player, player.getInventory().selected, true);
			}
		});

		helper.succeedWhen(() -> {
			ItemStack inHand = player.getMainHandItem();
			helper.assertFalse(LootUtils.isRolling(inHand), "the roll should finish");
			helper.assertTrue(inHand.get(DataComponents.CUSTOM_NAME) != null,
					"revealed gear should get its name back");
			if (LootUtils.getToolType(inHand).isArmor()) {
				helper.assertTrue(LootUtils.wearableSlot(inHand) != null,
						"revealed armor should become equippable");
			}
			helper.assertFalse(heldAttributes(inHand).modifiers().isEmpty(),
					"revealed gear should grant attributes again");
		});
	}

	/** Both item implementations reveal through their production inventoryTick lifecycle. */
	public static void deterministicRollLifecycles(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack sword = LootUtils.buildTool(player, helper.getLevel(), player.blockPosition(),
				ToolType.SWORD, 4.0f);
		ItemStack chestplate = LootUtils.buildTool(player, helper.getLevel(), player.blockPosition(),
				ToolType.CHESTPLATE, 4.0f);

		helper.assertTrue(sword.get(DataComponents.CUSTOM_NAME) != null,
				"the deterministic tool fixture should start named");
		helper.assertTrue(chestplate.get(DataComponents.CUSTOM_NAME) != null,
				"the deterministic armor fixture should start named");

		long revealTime = helper.getLevel().getGameTime() + 5;
		LootUtils.startRoll(sword, revealTime);
		LootUtils.startRoll(chestplate, revealTime);
		player.setItemInHand(InteractionHand.MAIN_HAND, sword);
		player.setItemInHand(InteractionHand.OFF_HAND, chestplate);

		helper.assertTrue(heldAttributes(sword).modifiers().isEmpty(),
				"rolling tool should have no attributes");
		helper.assertTrue(LootUtils.wearableSlot(chestplate) == null,
				"rolling armor should not be equippable");
		helper.assertTrue(heldAttributes(chestplate).modifiers().isEmpty(),
				"rolling armor should have no attributes");

		helper.onEachTick(() -> {
			sword.inventoryTick(helper.getLevel(), player, player.getInventory().selected, true);
			chestplate.inventoryTick(helper.getLevel(), player, 0, false);
		});

		helper.succeedWhen(() -> {
			helper.assertFalse(LootUtils.isRolling(sword), "tool roll should finish through inventoryTick");
			helper.assertFalse(LootUtils.isRolling(chestplate), "armor roll should finish through inventoryTick");
			helper.assertTrue(sword.get(DataComponents.CUSTOM_NAME) != null,
					"revealed tool should restore its name");
			helper.assertFalse(heldAttributes(sword).modifiers().isEmpty(),
					"revealed tool should restore attributes");
			helper.assertTrue(chestplate.get(DataComponents.CUSTOM_NAME) != null,
					"revealed armor should restore its name");
			helper.assertTrue(LootUtils.wearableSlot(chestplate) != null,
					"revealed armor should restore its equippable state");
			helper.assertFalse(heldAttributes(chestplate).modifiers().isEmpty(),
					"revealed armor should restore attributes");
		});
	}

	/** Attributes live in the vanilla component now, refreshed by LootUtils.refreshDerivedComponents. */
	public static ItemAttributeModifiers heldAttributes(ItemStack stack) {
		return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
	}

	/**
	 * Building an armor piece wires up its equip slot + worn texture set and produces
	 * positive defense and durability.
	 */
	public static void armorComponents(GameTestHelper helper) {
		ItemStack helmet = new ItemStack(ModItems.ARMOR.get());
		LootUtils.setStats(helmet, 4.0f);
		LootUtils.setToolType(helmet, ToolType.HELMET);
		LootUtils.setTexture(helmet, 3);

		helper.assertTrue(LootUtils.wearableSlot(helmet) != null, "armor should be equippable after setTexture");
		helper.assertTrue(LootUtils.wearableSlot(helmet) == EquipmentSlot.HEAD, "helmet should equip in the head slot");
		helper.assertTrue(LootUtils.wornSetIndex(helmet) == 4,
				"texture index 3 should map to worn texture set 4");

		helper.assertTrue(LootArmorItem.getDefense(helmet, ToolType.HELMET) > 0f,
				"helmet should have positive defense");
		helper.assertTrue(helmet.getMaxDamage() > 0, "helmet should have positive durability");

		// Texture cycling wraps within the armor set count and keeps the worn set in sync.
		LootUtils.addTexture(helmet, LootUtils.ARMOR_SET_COUNT - 3);
		helper.assertTrue(LootUtils.wornSetIndex(helmet) == 1,
				"texture cycling should wrap back to worn texture set 1");

		helper.succeed();
	}

	/** Every item generated in the same world (same temperature band) credits the same forger. */
	public static void forgerWorldConstant(GameTestHelper helper) {
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
	public static void dirtPlaceWorldForger(GameTestHelper helper) {
		long seed = helper.getLevel().getSeed();

		ItemStack tool = newTool(ToolType.SHOVEL);
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

	/**
	 * Wearer-hurt traits fire when a PLAYER takes damage. Regression: on Fabric the
	 * armor-trait hook was a mixin into LivingEntity.actuallyHurt, which Player
	 * overrides - so Thorny (and every WearerHurtModifier) silently did nothing for
	 * players while passing the zombie-based tests.
	 */
	public static void thornyReflectsForPlayer(GameTestHelper helper) {
		ServerPlayer player = mockVulnerablePlayer(helper);

		ItemStack chest = newArmor(ToolType.CHESTPLATE);
		LootUtils.addModifier(chest, ModifierRegistry.getModifier("thorny"));
		player.setItemSlot(EquipmentSlot.CHEST, chest);

		// A player attacker: mob damage scales with difficulty and zeroes out on the
		// gametest server's Peaceful setting before it ever reaches the armor hooks.
		// PvP (a game rule in 26.x) is off by default and gates ServerPlayer.hurtServer.
		enablePvp(helper);
		ServerPlayer attacker = mockVulnerablePlayer(helper);
		float attackerHealth = attacker.getHealth();
		float wearerHealth = player.getHealth();

		player.hurt(player.damageSources().playerAttack(attacker), 6.0f);

		helper.assertTrue(player.getHealth() < wearerHealth, "the wearer should take damage");
		helper.assertTrue(attacker.getHealth() < attackerHealth,
				"thorny should reflect damage back at the attacker when the wearer is a player");

		helper.succeed();
	}

	/** A mock player that can actually be hurt: survival-ish abilities. */
	private static ServerPlayer mockVulnerablePlayer(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.getAbilities().instabuild = false;
		player.getAbilities().invulnerable = false;
		// 1.21.1: ServerPlayer.hurt ignores damage while the private
		// spawnInvulnerableTime (60 ticks) is positive; it only decrements in
		// tick(). Tick it down so same-tick test hits actually land - each
		// iteration decrements before anything that could throw on a mock player.
		for (int i = 0; i < 61; i++) {
			try {
				player.tick();
			} catch (Throwable ignored) {
			}
		}
		player.setHealth(player.getMaxHealth());
		return player;
	}

	/** A fresh Random Tool of the given type. */
	private static ItemStack newTool(ToolType type) {
		ItemStack tool = new ItemStack(ModItems.TOOL.get());
		LootUtils.setToolType(tool, type);
		return tool;
	}

	/** A fresh Random Armor piece of the given type (first texture set). */
	private static ItemStack newArmor(ToolType type) {
		ItemStack piece = new ItemStack(ModItems.ARMOR.get());
		LootUtils.setToolType(piece, type);
		LootUtils.setTexture(piece, 0);
		return piece;
	}

	/** PvP is off by default on the gametest server and gates player-vs-player damage. */
	private static void enablePvp(GameTestHelper helper) {
		helper.getLevel().getServer().setPvpAllowed(true);
	}

	/** An armor piece of the given type wearing the given trait leveled to its max. */
	private static ItemStack maxedTraitArmor(ToolType type, String trait) {
		ItemStack piece = new ItemStack(ModItems.ARMOR.get());
		LootUtils.setToolType(piece, type);
		LootUtils.setTexture(piece, 0);
		Modifier mod = ModifierRegistry.getModifier(trait);
		// First add lands at the base level; every repeat levels it up until maxed.
		for (int i = 0; i < 16; i++) {
			LootUtils.addModifier(piece, mod);
		}
		return piece;
	}

	/** Hurts the wearer once with full effect: no i-frames, health reset so they never die. */
	private static void freshHit(GameTestHelper helper, ServerPlayer wearer, DamageSource source, float damage) {
		wearer.setHealth(wearer.getMaxHealth());
		wearer.invulnerableTime = 0;
		wearer.hurt(source, damage);
	}

	/**
	 * Items from before the multiloader port have no ATTRIBUTE_MODIFIERS /
	 * MAX_DAMAGE component patch (attributes were dynamic NeoForge item-method
	 * overrides); inventoryTick must stamp them on first contact.
	 */
	public static void migrationRestoresDerivedComponents(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();

		// Build an "old" item through GearTags.write, the raw layer that skips the
		// derived-component refresh GearTags.mutate performs.
		ItemStack tool = new ItemStack(ModItems.TOOL.get());
		var info = GearTags.read(tool, GearTags.INFO);
		info.putString("type", ToolType.SWORD.name());
		GearTags.write(tool, GearTags.INFO, info);
		var stats = GearTags.read(tool, GearTags.STATS);
		stats.putFloat("goodness", 5.0f);
		GearTags.write(tool, GearTags.STATS, stats);

		helper.assertTrue(
				tool.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers().isEmpty(),
				"pre-migration item should have no stamped attributes");

		tool.getItem().inventoryTick(tool, helper.getLevel(), player, player.getInventory().selected, true);

		helper.assertFalse(
				tool.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers().isEmpty(),
				"inventoryTick should stamp attributes onto a pre-component item");
		helper.assertTrue(tool.getMaxDamage() == LootItem.computeMaxDamage(tool),
				"inventoryTick should stamp the stats-derived max damage, got " + tool.getMaxDamage());

		helper.succeed();
	}

	/** Featherweight (maxed = 100%) cancels fall damage for a player wearer. */
	public static void featherweightSoftensFallDamage(GameTestHelper helper) {
		ServerPlayer traited = mockVulnerablePlayer(helper);
		traited.setItemSlot(EquipmentSlot.FEET, maxedTraitArmor(ToolType.BOOTS, "featherweight"));

		ServerPlayer plain = mockVulnerablePlayer(helper);
		ItemStack plainBoots = newArmor(ToolType.BOOTS);
		plain.setItemSlot(EquipmentSlot.FEET, plainBoots);

		freshHit(helper, plain, plain.damageSources().fall(), 6.0f);
		helper.assertTrue(plain.getHealth() < plain.getMaxHealth(),
				"control wearer should take fall damage");

		freshHit(helper, traited, traited.damageSources().fall(), 6.0f);
		helper.assertTrue(traited.getHealth() == traited.getMaxHealth(),
				"maxed featherweight should cancel fall damage, health " + traited.getHealth());

		helper.succeed();
	}

	/** Adrenaline grants a speed burst when the wearer is hit. */
	public static void adrenalineGrantsSpeed(GameTestHelper helper) {
		ServerPlayer player = mockVulnerablePlayer(helper);
		ItemStack chest = newArmor(ToolType.CHESTPLATE);
		LootUtils.addModifier(chest, ModifierRegistry.getModifier("adrenaline"));
		player.setItemSlot(EquipmentSlot.CHEST, chest);

		helper.assertFalse(player.hasEffect(MobEffects.MOVEMENT_SPEED), "no speed before the hit");
		freshHit(helper, player, player.damageSources().generic(), 4.0f);
		helper.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SPEED), "adrenaline should grant speed on hit");

		helper.succeed();
	}

	/** Bulwark (maxed = 50% chance to halve a hit) measurably reduces damage over many hits. */
	public static void bulwarkBlocksSomeHits(GameTestHelper helper) {
		ServerPlayer player = mockVulnerablePlayer(helper);
		player.setItemSlot(EquipmentSlot.CHEST, maxedTraitArmor(ToolType.CHESTPLATE, "bulwark"));

		int hits = 60;
		float perHit = 4.0f;
		float taken = 0.0f;
		for (int i = 0; i < hits; i++) {
			freshHit(helper, player, player.damageSources().generic(), perHit);
			taken += player.getMaxHealth() - player.getHealth();
		}

		// Expected factor 0.75 of raw damage; 0.92 leaves ~4 sigma of headroom, and
		// the floor catches a hook that suddenly zeroes everything.
		helper.assertTrue(taken < hits * perHit * 0.92f,
				"maxed bulwark should halve ~50% of hits, took " + taken + "/" + hits * perHit);
		helper.assertTrue(taken > hits * perHit * 0.4f,
				"bulwark should not block more than half of all damage, took " + taken);

		helper.succeed();
	}

	/** Unbreaking (maxed = 100%) skips all armor durability loss from soaked hits. */
	public static void unbreakingSkipsArmorDurability(GameTestHelper helper) {
		enablePvp(helper);
		ServerPlayer attacker = mockVulnerablePlayer(helper);

		// Control: plain armor must lose durability on this path, or the assert below is vacuous.
		ServerPlayer plain = mockVulnerablePlayer(helper);
		ItemStack plainChest = newArmor(ToolType.CHESTPLATE);
		plain.setItemSlot(EquipmentSlot.CHEST, plainChest);
		for (int i = 0; i < 5; i++) {
			freshHit(helper, plain, plain.damageSources().playerAttack(attacker), 8.0f);
		}
		helper.assertTrue(plain.getItemBySlot(EquipmentSlot.CHEST).getDamageValue() > 0,
				"control armor should lose durability from soaked hits");

		ServerPlayer player = mockVulnerablePlayer(helper);
		player.setItemSlot(EquipmentSlot.CHEST, maxedTraitArmor(ToolType.CHESTPLATE, "unbreaking"));
		for (int i = 0; i < 5; i++) {
			freshHit(helper, player, player.damageSources().playerAttack(attacker), 8.0f);
		}
		helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).getDamageValue() == 0,
				"maxed unbreaking should skip all armor durability loss, lost "
						+ player.getItemBySlot(EquipmentSlot.CHEST).getDamageValue());

		helper.succeed();
	}

	/** Soulbound gives its original owner a mining speed bonus (break-speed hook on both loaders). */
	public static void soulboundOwnerMinesFaster(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();

		ItemStack pick = newTool(ToolType.PICKAXE);
		LootUtils.addModifier(pick, ModifierRegistry.getModifier("soulbound"));
		LootUtils.setOwnerUUID(pick, player.getStringUUID());
		player.setItemInHand(InteractionHand.MAIN_HAND, pick);

		float owned = player.getDestroySpeed(Blocks.STONE.defaultBlockState());

		LootUtils.setOwnerUUID(pick, "00000000-0000-0000-0000-000000000000");
		float unowned = player.getDestroySpeed(Blocks.STONE.defaultBlockState());

		helper.assertTrue(owned > unowned,
				"soulbound should mine faster for its owner: owned=" + owned + " unowned=" + unowned);

		helper.succeed();
	}

	/**
	 * The enchanting-table candidate list is filtered per tool type. This exercises the
	 * loader wiring end to end: NeoForge's supportsEnchantment item extension vs
	 * Fabric's EnchantmentEvents.ALLOW_ENCHANTING hook into EnchantmentHelper.
	 */
	public static void enchantingTableFiltersByType(GameTestHelper helper) {
		var enchants = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		Holder<Enchantment> efficiency = enchants.getOrThrow(Enchantments.EFFICIENCY);
		Holder<Enchantment> sharpness = enchants.getOrThrow(Enchantments.SHARPNESS);

		ItemStack sword = newTool(ToolType.SWORD);
		ItemStack pickaxe = newTool(ToolType.PICKAXE);

		// Regression net: the table path needs the item in the enchant's primary_items
		// tag (sharpness: #minecraft:enchantable/melee_weapon since 26.x), on top of the
		// per-type filtering.
		helper.assertTrue(tableOffers(sword, sharpness, efficiency), "sword should roll sharpness at the table");
		helper.assertFalse(tableOffers(sword, efficiency, sharpness), "sword must not roll efficiency at the table");
		helper.assertTrue(tableOffers(pickaxe, efficiency, sharpness), "pickaxe should roll efficiency at the table");
		helper.assertFalse(tableOffers(pickaxe, sharpness, efficiency), "pickaxe must not roll sharpness at the table");

		helper.succeed();
	}

	private static boolean tableOffers(ItemStack stack, Holder<Enchantment> wanted, Holder<Enchantment> other) {
		return EnchantmentHelper
				.getAvailableEnchantmentResults(30, stack, Stream.of(wanted, other)).stream()
				.anyMatch(instance -> instance.enchantment == wanted);
	}

	/** A Random Axe strips logs (preserving the axis), scrapes and un-waxes copper. */
	public static void axeToolActions(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack axe = newTool(ToolType.AXE);
		player.setItemInHand(InteractionHand.MAIN_HAND, axe);

		BlockPos logPos = new BlockPos(1, 1, 1);
		helper.setBlock(logPos, Blocks.OAK_LOG.defaultBlockState()
				.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		useOnBlock(helper, player, logPos);
		helper.assertBlockPresent(Blocks.STRIPPED_OAK_LOG, logPos);
		helper.assertTrue(helper.getBlockState(logPos)
						.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X,
				"stripping should preserve the log's axis");

		BlockPos copperPos = new BlockPos(2, 1, 1);
		helper.setBlock(copperPos, Blocks.OXIDIZED_COPPER);
		useOnBlock(helper, player, copperPos);
		helper.assertBlockPresent(Blocks.WEATHERED_COPPER, copperPos);

		BlockPos waxedPos = new BlockPos(3, 1, 1);
		helper.setBlock(waxedPos, Blocks.WAXED_COPPER_BLOCK);
		useOnBlock(helper, player, waxedPos);
		helper.assertBlockPresent(Blocks.COPPER_BLOCK, waxedPos);

		helper.succeed();
	}

	/** A Random Shovel flattens grass into dirt path. */
	public static void shovelFlattens(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack shovel = newTool(ToolType.SHOVEL);
		player.setItemInHand(InteractionHand.MAIN_HAND, shovel);

		BlockPos grassPos = new BlockPos(1, 1, 1);
		helper.setBlock(grassPos, Blocks.GRASS_BLOCK);
		useOnBlock(helper, player, grassPos);
		helper.assertBlockPresent(Blocks.DIRT_PATH, grassPos);

		helper.succeed();
	}

	private static void useOnBlock(GameTestHelper helper, ServerPlayer player, BlockPos relative) {
		BlockPos absolute = helper.absolutePos(relative);
		var hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
		player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(),
				InteractionHand.MAIN_HAND, hit);
	}

	/**
	 * The anvil must refuse to combine two Random Loot items (each is unique;
	 * combining destroys the right item's identity and is a cheap repair loop) -
	 * NeoForge via isCombineRepairable=false, Fabric via AnvilMenuMixin - while
	 * the material-repair path (diamond) keeps working.
	 */
	public static void anvilCannotCombineLootGear(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		var menu = new AnvilMenu(1, player.getInventory(),
				ContainerLevelAccess.create(helper.getLevel(),
						helper.absolutePos(BlockPos.ZERO)));

		// Damaged left + enchanted right: without the block, vanilla's combine path
		// definitely produces a repaired, enchant-merged result (two pristine tools
		// would yield nothing anyway, making the assert vacuous).
		var sharpness = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
				.getOrThrow(Enchantments.SHARPNESS);
		ItemStack left = newTool(ToolType.SWORD);
		left.set(DataComponents.DAMAGE, 50);
		ItemStack right = newTool(ToolType.SWORD);
		right.enchant(sharpness, 1);

		menu.getSlot(0).set(left);
		menu.getSlot(1).set(right);
		menu.createResult();
		helper.assertTrue(menu.getSlot(2).getItem().isEmpty(),
				"combining two Random Tools in an anvil must yield nothing, got " + menu.getSlot(2).getItem());

		// Control: the tag-driven material repair path must still work.
		ItemStack damaged = newTool(ToolType.SWORD);
		damaged.set(DataComponents.DAMAGE, 50);
		menu.getSlot(0).set(damaged);
		menu.getSlot(1).set(new ItemStack(Items.DIAMOND));
		menu.createResult();
		helper.assertFalse(menu.getSlot(2).getItem().isEmpty(),
				"diamond material repair should still produce an anvil result");

		helper.succeed();
	}

	/**
	 * Chest-type loot tables actually produce cases/templates at the configured
	 * chance: NeoForge via the case_item global loot modifier, Fabric via the
	 * LootTableEvents.MODIFY pools. 200 rolls at the default 25% miss with
	 * probability ~1e-25.
	 */
	public static void lootInjectionAddsCases(GameTestHelper helper) {
		var key = ResourceKey.create(Registries.LOOT_TABLE,
				ResourceLocation.withDefaultNamespace("chests/simple_dungeon"));
		var table = helper.getLevel().getServer().reloadableRegistries().getLootTable(key);
		helper.assertTrue(table != LootTable.EMPTY,
				"simple_dungeon loot table should exist");

		var params = new LootParams.Builder(helper.getLevel())
				.create(LootContextParamSets.EMPTY);

		boolean found = false;
		for (int i = 0; i < 200 && !found; i++) {
			found = table.getRandomItems(params).stream()
					.anyMatch(s -> s.is(ModItems.CASE.get()) || s.is(ModItems.MOD_ADD.get()));
		}
		helper.assertTrue(found, "chest loot should contain injected cases/templates");

		helper.succeed();
	}

	/**
	 * Breaking a chest drops only the chest. Its block-drop table is named
	 * "blocks/chest", which contains the default "chest" injection match, so the
	 * injection used to fire on every chest broken - empty or player-placed alike -
	 * handing out a case and a template for a block the player had just placed.
	 * 200 rolls: a regression survives with probability ~1e-38.
	 */
	public static void lootInjectionSkipsBlockDrops(GameTestHelper helper) {
		var key = ResourceKey.create(Registries.LOOT_TABLE,
				ResourceLocation.withDefaultNamespace("blocks/chest"));
		var table = helper.getLevel().getServer().reloadableRegistries().getLootTable(key);
		helper.assertTrue(table != LootTable.EMPTY, "chest block loot table should exist");

		var params = new LootParams.Builder(helper.getLevel())
				.withParameter(LootContextParams.ORIGIN, helper.absoluteVec(Vec3.ZERO))
				.withParameter(LootContextParams.BLOCK_STATE, Blocks.CHEST.defaultBlockState())
				.withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
				.create(LootContextParamSets.BLOCK);

		for (int i = 0; i < 200; i++) {
			for (ItemStack drop : table.getRandomItems(params)) {
				helper.assertFalse(drop.is(ModItems.CASE.get()) || drop.is(ModItems.MOD_ADD.get()),
						"breaking a chest should not drop " + drop.getItem());
			}
		}

		helper.succeed();
	}

	/** Worn Random Armor gains XP when its wearer takes damage (the armor leveling path). */
	public static void armorXpOnDamage(GameTestHelper helper) {
		ItemStack chest = newArmor(ToolType.CHESTPLATE);

		LivingEntity zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
		zombie.setItemSlot(EquipmentSlot.CHEST, chest);

		zombie.hurt(zombie.damageSources().generic(), 6.0f);

		ItemStack worn = zombie.getItemBySlot(EquipmentSlot.CHEST);
		helper.assertTrue(LootUtils.getXP(worn) > 0 || LootUtils.getLevel(worn) > 0,
				"worn armor should gain XP from its wearer taking damage");

		helper.succeed();
	}

	/** The anvil's material-repair path accepts items from the armor_repair_materials tag. */
	public static void armorRepairable(GameTestHelper helper) {
		ItemStack armor = new ItemStack(ModItems.ARMOR.get());

		helper.assertTrue(armor.getItem().isValidRepairItem(armor, new ItemStack(Items.DIAMOND)),
				"diamond should repair Random Armor (armor_repair_materials tag)");
		helper.assertFalse(armor.getItem().isValidRepairItem(armor, new ItemStack(Items.STICK)),
				"stick should not repair Random Armor");

		helper.succeed();
	}

	/** A survival player mines with Random Loot gear through the real server game-mode path. */
	public static void breakBlock(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
		BlockPos relative = new BlockPos(1, 1, 1);
		helper.setBlock(relative, Blocks.DIRT);

		BlockPos absolute = helper.absolutePos(relative);
		ItemStack tool = newTool(ToolType.SHOVEL);
		player.setItemInHand(InteractionHand.MAIN_HAND, tool);
		int damageBefore = tool.getDamageValue();
		int xpBefore = LootUtils.getXP(tool);

		boolean destroyed = player.gameMode.destroyBlock(absolute);

		helper.assertTrue(destroyed, "the server player game mode should destroy a harvestable block");
		helper.assertBlockNotPresent(Blocks.DIRT, relative);
		helper.assertTrue(LootUtils.getXP(tool) == xpBefore + 1,
				"a real mined block should award one tool XP");
		helper.assertTrue(tool.getDamageValue() == damageBefore + 1,
				"a real mined block should consume one durability");

		helper.succeed();
	}

	/**
	 * Kill traits fire through the LivingDeathEvent dispatcher. Hailey's Wrath spawns a
	 * bee and Nemesis records the kill on the tool's NBT. The regression was both traits
	 * checking victim health inside hurtEnemy, which never sees indirect kills and made
	 * Hailey's Wrath effectively dead.
	 */
	public static void killTraitHooks(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
		ItemStack tool = newTool(ToolType.SWORD);
		LootUtils.addModifier(tool, ModifierRegistry.getModifier("haileys_wrath"));
		LootUtils.addModifier(tool, ModifierRegistry.getModifier("nemesis"));
		player.setItemInHand(InteractionHand.MAIN_HAND, tool);
		int damageBefore = tool.getDamageValue();
		int xpBefore = LootUtils.getXP(tool);

		LivingEntity zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
		zombie.setHealth(0.1f);
		player.attack(zombie);

		helper.assertTrue(zombie.isDeadOrDying(), "zombie should die to the player's attack");
		helper.assertTrue(LootUtils.getXP(tool) == xpBefore + 1,
				"a real weapon hit should award one tool XP");
		helper.assertTrue(tool.getDamageValue() == damageBefore + 1,
				"a real weapon hit should consume one durability");
		helper.assertEntityPresent(EntityType.BEE);

		Modifier nemesis = LootUtils.getModifiers(player.getMainHandItem()).stream()
				.filter(m -> m.tagName().equals("nemesis")).findFirst().orElse(null);
		helper.assertTrue(nemesis != null, "nemesis trait should still be on the tool");
		int zombieKills = NbtCompat.getIntOr(NbtCompat.getCompoundOrEmpty(nemesis.toNBT(), "killCounts"), "minecraft:zombie", 0);
		helper.assertTrue(zombieKills == 1, "nemesis should record the zombie kill, got " + zombieKills);
		assertAdvancementDone(helper, player, "beekeeper");

		helper.succeed();
	}

	/**
	 * Catalyst (cinnabar) slows the decay of beneficial effects and leaves harmful ones
	 * alone. Driving the held tool's inventoryTick many times without vanilla's per-tick
	 * decrement lets the beneficial duration climb past its start while the harmful one
	 * stays put -- exercising both the holder dispatch and beneficial-effect filter.
	 */
	public static void catalystExtendsEffects(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();

		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0, false, false));

		ItemStack tool = newTool(ToolType.SWORD);
		LootUtils.addModifier(tool, ModifierRegistry.getModifier("catalyst"));
		player.setItemInHand(InteractionHand.MAIN_HAND, tool);

		for (int i = 0; i < 200; i++) {
			tool.inventoryTick(helper.getLevel(), player, player.getInventory().selected, true);
		}

		MobEffectInstance speed = player.getEffect(MobEffects.MOVEMENT_SPEED);
		MobEffectInstance poison = player.getEffect(MobEffects.POISON);
		helper.assertTrue(speed != null && speed.getDuration() > 100,
				"catalyst should extend the beneficial speed effect, got "
						+ (speed == null ? "none" : speed.getDuration()));
		helper.assertTrue(poison != null && poison.getDuration() == 100,
				"catalyst must not touch the harmful poison effect, got "
						+ (poison == null ? "none" : poison.getDuration()));

		helper.succeed();
	}

	/**
	 * The cinnabar/sulfur trait recipes load with valid ingredient ids. A bad item id makes
	 * the whole recipe fail to parse at datapack load and drop out of the recipe set (which
	 * is exactly what left them as "n/a" in the wiki), so presence here proves both the JSON
	 * and the ingredient ids resolve (redstone/rotten flesh on 1.21.1 - cinnabar and
	 * sulfur don't exist before 26.x).
	 */
	public static void newTraitRecipesLoad(GameTestHelper helper) {
		var recipes = helper.getLevel().getRecipeManager().getRecipes();

		record Expected(String trait, ResourceLocation ingredient) {
		}
		Expected[] expected = {
				new Expected("catalyst", ResourceLocation.withDefaultNamespace("redstone")),
				new Expected("stench", ResourceLocation.withDefaultNamespace("rotten_flesh")),
		};

		for (Expected e : expected) {
			helper.assertTrue(BuiltInRegistries.ITEM.containsKey(e.ingredient()),
					"ingredient item should exist: " + e.ingredient());

			ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(RandomLoot.MODID, "trait_" + e.trait());
			boolean present = recipes.stream().anyMatch(
					h -> h.id().equals(recipeId) && h.value() instanceof TraitAdditionRecipe);
			helper.assertTrue(present, "trait recipe should load: " + recipeId);
		}

		helper.succeed();
	}

	/** Stench (sulfur) afflicts hostile mobs inside its radius with Slowness and Weakness. */
	public static void stenchDebuffsMobs(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();

		LivingEntity zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
		// Stand the holder on the zombie so it's well within the aura radius.
		player.setPos(zombie.getX(), zombie.getY(), zombie.getZ());

		ItemStack tool = newTool(ToolType.SWORD);
		LootUtils.addModifier(tool, ModifierRegistry.getModifier("stench"));
		player.setItemInHand(InteractionHand.MAIN_HAND, tool);

		tool.inventoryTick(helper.getLevel(), player, player.getInventory().selected, true);

		helper.assertTrue(zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "stench should slow nearby hostiles");
		helper.assertTrue(zombie.hasEffect(MobEffects.WEAKNESS), "stench should weaken nearby hostiles");

		helper.succeed();
	}

	/**
	 * A single XP dump crosses each level threshold at that level's own cost. Level 0->1
	 * costs 500 and 1->2 costs 1000, so 1600 XP lands at exactly level 2 with 100 left
	 * over. The regression was the loop reusing the starting level's threshold, which
	 * turned the same 1600 XP into three 500-cost levels.
	 */
	public static void xpLevelCurve(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack tool = newTool(ToolType.PICKAXE);

		LootUtils.addXp(tool, player, 1600);

		helper.assertTrue(LootUtils.getLevel(tool) == 2,
				"1600 XP should reach exactly level 2, got " + LootUtils.getLevel(tool));
		helper.assertTrue(LootUtils.getXP(tool) == 100,
				"100 XP should remain after leveling, got " + LootUtils.getXP(tool));
		assertAdvancementDone(helper, player, "tool_level_1");

		helper.succeed();
	}

	/**
	 * Every shipped advancement parsed and loaded. A malformed advancement JSON (or an
	 * unregistered criterion trigger id) is silently dropped at datapack load with only a
	 * log line, so this is the regression net for the whole advancement tab.
	 */
	public static void advancementsLoad(GameTestHelper helper) {
		ServerAdvancementManager advancements = helper.getLevel().getServer().getAdvancements();

		String[] ids = { "root", "salvage_gear", "craft_case", "open_case", "open_cases_10",
				"open_cases_25", "all_tool_types",
				"tool_level_1", "tool_level_5", "tool_level_10", "get_template", "swap_template",
				"add_trait", "trait_count_4", "biome_trait", "void_teleport", "executioner_kill",
				"lightning_strike", "beekeeper" };

		for (String id : ids) {
			helper.assertTrue(advancements.get(ResourceLocation.fromNamespaceAndPath(RandomLoot.MODID, id)) != null,
					"advancement " + id + " should be loaded");
		}

		helper.succeed();
	}

	private static void assertAdvancementDone(GameTestHelper helper, ServerPlayer player, String id) {
		var advancement = helper.getLevel().getServer().getAdvancements()
				.get(ResourceLocation.fromNamespaceAndPath(RandomLoot.MODID, id));
		helper.assertTrue(advancement != null, "advancement should be loaded: " + id);
		helper.assertTrue(player.getAdvancements().getOrStartProgress(advancement).isDone(),
				"gameplay sequence should complete advancement: " + id);
	}

	/** The anvil's material-repair path accepts items from the tool_repair_materials tag. */
	public static void toolRepairable(GameTestHelper helper) {
		ItemStack tool = new ItemStack(ModItems.TOOL.get());

		helper.assertTrue(tool.getItem().isValidRepairItem(tool, new ItemStack(Items.DIAMOND)),
				"diamond should repair Random Tools (tool_repair_materials tag)");
		helper.assertFalse(tool.getItem().isValidRepairItem(tool, new ItemStack(Items.STICK)),
				"stick should not repair Random Tools");

		helper.succeed();
	}

	public static boolean hasTrait(ItemStack tool, String tagName) {
		return LootUtils.getModifiers(tool).stream().anyMatch(m -> m.tagName().equals(tagName));
	}

	/** The /randomloot admin commands: give, trait add/remove (with gating), and xp. */
	public static void adminCommands(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		CommandSourceStack source = player.createCommandSourceStack().withPermission(Commands.LEVEL_OWNERS);
		CommandDispatcher<CommandSourceStack> commands = helper.getLevel().getServer().getCommands().getDispatcher();

		helper.assertTrue(run(commands, source, "randomloot give sword") == 1, "give should succeed");
		ItemStack given = ItemStack.EMPTY;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			if (player.getInventory().getItem(i).is(ModItems.TOOL.get())) {
				given = player.getInventory().getItem(i);
				break;
			}
		}
		helper.assertTrue(!given.isEmpty(), "give should put a tool in the player's inventory");
		helper.assertTrue(LootUtils.getToolType(given) == ToolType.SWORD, "give should respect the requested type");
		helper.assertTrue(LootUtils.getStats(given) == 0f, "given tool should have 0 goodness");
		helper.assertTrue(LootUtils.getModifiers(given).isEmpty(), "given tool should start with no traits");

		player.setItemInHand(InteractionHand.MAIN_HAND, given);

		helper.assertTrue(run(commands, source, "randomloot trait add critical") == 1, "trait add should succeed");
		helper.assertTrue(hasTrait(player.getMainHandItem(), "critical"), "trait add should apply the trait");

		// Biome gating: Void-Touched is End-only and this tool was forged in the test level.
		helper.assertTrue(run(commands, source, "randomloot trait add void_touched") == 0,
				"biome-restricted trait should be rejected");
		helper.assertFalse(hasTrait(player.getMainHandItem(), "void_touched"),
				"rejected trait must not land on the tool");

		// Tool gating: Thorny is armor-only.
		helper.assertTrue(run(commands, source, "randomloot trait add thorny") == 0,
				"armor-only trait should be rejected on a sword");

		helper.assertTrue(run(commands, source, "randomloot trait remove critical") == 1,
				"trait remove should succeed");
		helper.assertFalse(hasTrait(player.getMainHandItem(), "critical"), "trait remove should strip the trait");

		// Tab-completion for `trait add` only offers traits the command would accept.
		ParseResults<CommandSourceStack> parse = commands.parse("randomloot trait add ", source);
		List<String> suggested = commands.getCompletionSuggestions(parse).join().getList().stream()
				.map(Suggestion::getText).toList();
		helper.assertTrue(suggested.contains("critical"), "addable trait should be suggested");
		helper.assertFalse(suggested.contains("thorny"), "armor-only trait must not be suggested for a sword");
		helper.assertFalse(suggested.contains("void_touched"),
				"biome-restricted trait must not be suggested outside its biome");

		helper.assertTrue(run(commands, source, "randomloot xp 500") == 1, "xp should succeed");
		helper.assertTrue(LootUtils.getLevel(player.getMainHandItem()) == 1, "500 XP should level the tool to 1");

		// The whole tree is gamemaster-gated, so an unprivileged source can't even see it.
		CommandSourceStack noPerms = player.createCommandSourceStack()
				.withMaximumPermission(Commands.LEVEL_ALL);
		boolean denied;
		try {
			commands.execute("randomloot xp 1", noPerms);
			denied = false;
		} catch (CommandSyntaxException e) {
			denied = true;
		}
		helper.assertTrue(denied, "command should require gamemaster permissions");

		helper.succeed();
	}

	/**
	 * The smithing add-template only matches traits that work on the base gear's type
	 * (mirrors the case-roll and command gates); the subtraction template stays ungated.
	 */
	public static void smithingTraitGating(GameTestHelper helper) {
		TraitAdditionRecipe thorny = new TraitAdditionRecipe(
				BuiltInRegistries.ITEM.wrapAsHolder(Items.CACTUS), "thorny");
		TraitAdditionRecipe critical = new TraitAdditionRecipe(
				BuiltInRegistries.ITEM.wrapAsHolder(Items.GHAST_TEAR), "critical");

		ItemStack sword = newTool(ToolType.SWORD);
		ItemStack chestplate = newArmor(ToolType.CHESTPLATE);

		ItemStack addTemplate = new ItemStack(ModItems.MOD_ADD.get());
		ItemStack subTemplate = new ItemStack(ModItems.MOD_SUB.get());
		ItemStack cactus = new ItemStack(Items.CACTUS);
		ItemStack ghastTear = new ItemStack(Items.GHAST_TEAR);
		Level level = helper.getLevel();

		helper.assertFalse(thorny.matches(new SmithingRecipeInput(addTemplate, sword, cactus), level),
				"armor-only trait must not smith onto a sword");
		helper.assertTrue(thorny.matches(new SmithingRecipeInput(addTemplate, chestplate, cactus), level),
				"thorny should smith onto a chestplate");
		helper.assertFalse(critical.matches(new SmithingRecipeInput(addTemplate, chestplate, ghastTear), level),
				"weapon-only trait must not smith onto a chestplate");
		helper.assertTrue(critical.matches(new SmithingRecipeInput(addTemplate, sword, ghastTear), level),
				"critical should smith onto a sword");
		helper.assertTrue(thorny.matches(new SmithingRecipeInput(subTemplate, sword, cactus), level),
				"the subtraction template should still strip a mismatched trait");

		helper.succeed();
	}

	/** A real SmithingMenu resolves the datapack recipe, crafts it, and runs the take-result hooks. */
	public static void smithingCraftSequence(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos tablePos = new BlockPos(1, 1, 1);
		helper.setBlock(tablePos, Blocks.SMITHING_TABLE);

		var enchants = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		Holder<Enchantment> sharpness = enchants.getOrThrow(Enchantments.SHARPNESS);
		ItemStack sword = LootUtils.buildTool(player, helper.getLevel(), helper.absolutePos(tablePos),
				ToolType.SWORD, 0.0f);
		sword.enchant(sharpness, 3);
		sword.set(DataComponents.REPAIR_COST, 5);
		sword.set(DataComponents.DAMAGE, 7);

		SmithingMenu menu = new SmithingMenu(1, player.getInventory(),
				ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(tablePos)));
		player.containerMenu = menu;
		menu.getSlot(SmithingMenu.TEMPLATE_SLOT).set(new ItemStack(ModItems.MOD_ADD.get()));
		menu.getSlot(SmithingMenu.BASE_SLOT).set(sword);
		menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(new ItemStack(Items.GHAST_TEAR));

		ItemStack preview = menu.getSlot(SmithingMenu.RESULT_SLOT).getItem();
		helper.assertFalse(preview.isEmpty(), "the loaded critical recipe should produce a smithing result");
		helper.assertTrue(hasTrait(preview, "critical"), "the smithing preview should contain critical");

		menu.clicked(SmithingMenu.RESULT_SLOT, 0, ClickType.PICKUP, player);
		ItemStack crafted = menu.getCarried();
		helper.assertFalse(crafted.isEmpty(), "taking the smithing result should put it on the cursor");
		helper.assertTrue(hasTrait(crafted, "critical"), "the taken result should contain critical");
		helper.assertTrue(crafted.getEnchantments().getLevel(sharpness) == 3,
				"smithing should preserve Sharpness III");
		helper.assertTrue(crafted.getOrDefault(DataComponents.REPAIR_COST, 0) == 5,
				"smithing should preserve the anvil repair cost");
		helper.assertTrue(crafted.getOrDefault(DataComponents.DAMAGE, 0) == 7,
				"smithing should preserve durability damage");
		helper.assertTrue(menu.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem().isEmpty()
					&& menu.getSlot(SmithingMenu.BASE_SLOT).getItem().isEmpty()
					&& menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem().isEmpty(),
				"taking the result should consume all three smithing inputs");
		assertAdvancementDone(helper, player, "add_trait");

		helper.succeed();
	}

	/**
	 * CloneItem (smithing/retexture output) must carry over enchantments and repair cost,
	 * and must arrive with its derived components already stamped: it builds a fresh
	 * stack rather than going through GearTags.mutate, so it has to refresh explicitly.
	 * Without that the smithing output carried default attributes and durability until
	 * its first inventoryTick.
	 */
	public static void clonePreservesEnchantments(GameTestHelper helper) {
		var enchants = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		Holder<Enchantment> sharpness = enchants.getOrThrow(Enchantments.SHARPNESS);

		ItemStack sword = newTool(ToolType.SWORD);
		LootUtils.setStats(sword, 5.0f);
		sword.enchant(sharpness, 3);
		sword.set(DataComponents.REPAIR_COST, 5);
		sword.set(DataComponents.DAMAGE, 7);

		ItemStack clone = LootUtils.CloneItem(sword);

		helper.assertTrue(clone.getEnchantments().getLevel(sharpness) == 3,
				"clone should keep Sharpness III");
		helper.assertTrue(clone.getOrDefault(DataComponents.REPAIR_COST, 0) == 5,
				"clone should keep the anvil repair cost");
		helper.assertTrue(clone.getOrDefault(DataComponents.DAMAGE, 0) == 7,
				"clone should keep its durability damage");

		helper.assertFalse(
				clone.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers()
						.isEmpty(),
				"clone should arrive with its attributes already stamped");
		helper.assertTrue(clone.getMaxDamage() == LootItem.computeMaxDamage(sword),
				"clone should arrive with the right durability, not the item default");

		helper.succeed();
	}

	/**
	 * Every GameHook is wired on the loader under test. Adding a hook is one method on
	 * NeoForge and up to three edits on Fabric (callback or mixin class, plus a
	 * mixins.json entry), so a hook wired on one loader and forgotten on the other used
	 * to be invisible until someone played the game.
	 */
	public static void loaderHooksAllBound(GameTestHelper helper) {
		java.util.Set<GameHook> missing = GameHooks.missing();

		helper.assertTrue(missing.isEmpty(), "this loader never bound these hooks: " + missing);

		helper.succeed();
	}

	public static int run(CommandDispatcher<CommandSourceStack> commands, CommandSourceStack source, String command) {
		try {
			return commands.execute(command, source);
		} catch (CommandSyntaxException e) {
			throw new IllegalStateException("command failed to parse: " + command, e);
		}
	}
}
