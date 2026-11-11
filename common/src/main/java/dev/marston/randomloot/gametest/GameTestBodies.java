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
import dev.marston.randomloot.loot.LootGearItem;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.GearTags;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.NameGenerator;
import dev.marston.randomloot.loot.modifiers.ArmorDispatcher;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import dev.marston.randomloot.loot.modifiers.users.DirtPlace;
import dev.marston.randomloot.platform.GameHook;
import dev.marston.randomloot.platform.GameHooks;
import dev.marston.randomloot.recipes.TraitAdditionRecipe;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.commands.Commands;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SmithingMenu;
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
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

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
		ServerPlayer player = mockPlayer(helper);
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
		ServerPlayer player = mockPlayer(helper);
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
		ServerPlayer player = mockPlayer(helper);
		player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CASE.get()));

		player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);

		ItemStack held = player.getMainHandItem();
		helper.assertTrue(LootUtils.isRolling(held), "freshly opened gear should be rolling");
		helper.assertTrue(!held.hasCustomHoverName(), "rolling gear should hide its name");
		helper.assertTrue(LootUtils.wearableSlot(held) == null, "rolling gear should not be equippable");
		helper.assertTrue(!hasStampedAttributes(held),
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
			helper.assertTrue(inHand.hasCustomHoverName(),
					"revealed gear should get its name back");
			if (LootUtils.getToolType(inHand).isArmor()) {
				helper.assertTrue(LootUtils.wearableSlot(inHand) != null,
						"revealed armor should become equippable");
			}
			helper.assertFalse(!hasStampedAttributes(inHand),
					"revealed gear should grant attributes again");
		});
	}

	/** Both item implementations reveal through their production inventoryTick lifecycle. */
	public static void deterministicRollLifecycles(GameTestHelper helper) {
		ServerPlayer player = mockPlayer(helper);
		ItemStack sword = LootUtils.buildTool(player, helper.getLevel(), player.blockPosition(),
				ToolType.SWORD, 4.0f);
		ItemStack chestplate = LootUtils.buildTool(player, helper.getLevel(), player.blockPosition(),
				ToolType.CHESTPLATE, 4.0f);

		helper.assertTrue(sword.hasCustomHoverName(),
				"the deterministic tool fixture should start named");
		helper.assertTrue(chestplate.hasCustomHoverName(),
				"the deterministic armor fixture should start named");

		long revealTime = helper.getLevel().getGameTime() + 5;
		LootUtils.startRoll(sword, revealTime);
		LootUtils.startRoll(chestplate, revealTime);
		player.setItemInHand(InteractionHand.MAIN_HAND, sword);
		player.setItemInHand(InteractionHand.OFF_HAND, chestplate);

		helper.assertTrue(!hasStampedAttributes(sword),
				"rolling tool should have no attributes");
		helper.assertTrue(LootUtils.wearableSlot(chestplate) == null,
				"rolling armor should not be equippable");
		helper.assertTrue(!hasStampedAttributes(chestplate),
				"rolling armor should have no attributes");

		helper.onEachTick(() -> {
			sword.inventoryTick(helper.getLevel(), player, player.getInventory().selected, true);
			chestplate.inventoryTick(helper.getLevel(), player, 0, false);
		});

		helper.succeedWhen(() -> {
			helper.assertFalse(LootUtils.isRolling(sword), "tool roll should finish through inventoryTick");
			helper.assertFalse(LootUtils.isRolling(chestplate), "armor roll should finish through inventoryTick");
			helper.assertTrue(sword.hasCustomHoverName(),
					"revealed tool should restore its name");
			helper.assertFalse(!hasStampedAttributes(sword),
					"revealed tool should restore attributes");
			helper.assertTrue(chestplate.hasCustomHoverName(),
					"revealed armor should restore its name");
			helper.assertTrue(LootUtils.wearableSlot(chestplate) != null,
					"revealed armor should restore its equippable state");
			helper.assertFalse(!hasStampedAttributes(chestplate),
					"revealed armor should restore attributes");
		});
	}

	/**
	 * Whether LootUtils.refreshDerivedComponents has stamped attributes onto the stack.
	 * On 1.20.1 they live in the vanilla AttributeModifiers stack NBT (a data component
	 * on 1.21.1+).
	 */
	public static boolean hasStampedAttributes(ItemStack stack) {
		var tag = stack.getTag();
		return tag != null && !tag.getList("AttributeModifiers", net.minecraft.nbt.Tag.TAG_COMPOUND).isEmpty();
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
		ServerPlayer player = mockPlayer(helper);

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
	/**
	 * A creative mock player placed in the level; vanilla's makeMockServerPlayerInLevel
	 * with one change. Vanilla hands PlayerList a Connection that was never attached to a
	 * channel, and Forge 1.20.1's login hooks dereference that channel, so every test
	 * using a mock player died with a NullPointerException there. An EmbeddedChannel
	 * gives the connection a real (in-memory) channel; packets sent to it go nowhere.
	 */
	public static ServerPlayer mockPlayer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = new ServerPlayer(level.getServer(), level,
				new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "test-mock-player")) {
			@Override
			public boolean isSpectator() {
				return false;
			}

			@Override
			public boolean isCreative() {
				return true;
			}
		};
		net.minecraft.network.Connection connection = new net.minecraft.network.Connection(
				net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
		new io.netty.channel.embedded.EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player);
		return player;
	}

	private static ServerPlayer mockVulnerablePlayer(GameTestHelper helper) {
		ServerPlayer player = mockPlayer(helper);
		player.getAbilities().instabuild = false;
		player.getAbilities().invulnerable = false;
		// 1.20.1: ServerPlayer.hurt ignores damage while the private
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
		ServerPlayer player = mockPlayer(helper);

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
				!hasStampedAttributes(tool),
				"pre-migration item should have no stamped attributes");

		tool.getItem().inventoryTick(tool, helper.getLevel(), player, player.getInventory().selected, true);

		helper.assertFalse(
				!hasStampedAttributes(tool),
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
		ServerPlayer player = mockPlayer(helper);

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
	 * loader wiring end to end: Forge's canApplyAtEnchantingTable item extension vs
	 * Fabric's EnchantmentHelperMixin.
	 */
	public static void enchantingTableFiltersByType(GameTestHelper helper) {
		Enchantment efficiency = Enchantments.BLOCK_EFFICIENCY;
		Enchantment sharpness = Enchantments.SHARPNESS;

		ItemStack sword = newTool(ToolType.SWORD);
		ItemStack pickaxe = newTool(ToolType.PICKAXE);

		// Regression net: 1.20.1's EnchantmentCategory would refuse both (the gear item is
		// neither a SwordItem nor a DiggerItem), so a pass here proves the gate is wired.
		helper.assertTrue(tableOffers(sword, sharpness, efficiency), "sword should roll sharpness at the table");
		helper.assertFalse(tableOffers(sword, efficiency, sharpness), "sword must not roll efficiency at the table");
		helper.assertTrue(tableOffers(pickaxe, efficiency, sharpness), "pickaxe should roll efficiency at the table");
		helper.assertFalse(tableOffers(pickaxe, sharpness, efficiency), "pickaxe must not roll sharpness at the table");

		helper.succeed();
	}

	/**
	 * Enchantment compatibility is filtered per tool type via the randomloot enchantment
	 * tags. The regression this guards on the newer branches was a sword accepting an
	 * efficiency book; on 1.20.1 the failure mode is the opposite one - vanilla's
	 * EnchantmentCategory refuses everything, since the gear item is no SwordItem.
	 */
	public static void enchantTypeFiltering(GameTestHelper helper) {
		ItemStack sword = newTool(ToolType.SWORD);
		ItemStack pickaxe = newTool(ToolType.PICKAXE);

		helper.assertFalse(LootUtils.gearAcceptsEnchantment(sword, Enchantments.BLOCK_EFFICIENCY),
				"sword must not accept efficiency");
		helper.assertTrue(LootUtils.gearAcceptsEnchantment(sword, Enchantments.SHARPNESS),
				"sword should accept sharpness");
		helper.assertTrue(LootUtils.gearAcceptsEnchantment(sword, Enchantments.SWEEPING_EDGE),
				"sword should accept sweeping edge");
		helper.assertTrue(LootUtils.gearAcceptsEnchantment(pickaxe, Enchantments.BLOCK_EFFICIENCY),
				"pickaxe should accept efficiency");
		helper.assertFalse(LootUtils.gearAcceptsEnchantment(pickaxe, Enchantments.SHARPNESS),
				"pickaxe must not accept sharpness");
		helper.assertTrue(LootUtils.gearAcceptsEnchantment(sword, Enchantments.UNBREAKING)
				&& LootUtils.gearAcceptsEnchantment(pickaxe, Enchantments.UNBREAKING),
				"unbreaking should fit every tool");

		helper.succeed();
	}

	/** Armor enchantment compatibility is filtered per piece, mirroring the per-tool-type filtering. */
	public static void armorEnchantFiltering(GameTestHelper helper) {
		ItemStack helmet = newArmor(ToolType.HELMET);
		ItemStack boots = newArmor(ToolType.BOOTS);

		helper.assertTrue(LootUtils.gearAcceptsEnchantment(helmet, Enchantments.ALL_DAMAGE_PROTECTION),
				"helmet should accept protection");
		helper.assertTrue(LootUtils.gearAcceptsEnchantment(boots, Enchantments.ALL_DAMAGE_PROTECTION),
				"boots should accept protection");
		helper.assertTrue(LootUtils.gearAcceptsEnchantment(helmet, Enchantments.RESPIRATION),
				"helmet should accept respiration");
		helper.assertFalse(LootUtils.gearAcceptsEnchantment(boots, Enchantments.RESPIRATION),
				"boots must not accept respiration");
		helper.assertTrue(LootUtils.gearAcceptsEnchantment(boots, Enchantments.FALL_PROTECTION),
				"boots should accept feather falling");
		helper.assertFalse(LootUtils.gearAcceptsEnchantment(helmet, Enchantments.FALL_PROTECTION),
				"helmet must not accept feather falling");
		helper.assertFalse(LootUtils.gearAcceptsEnchantment(helmet, Enchantments.SHARPNESS),
				"armor must not accept sharpness");

		helper.succeed();
	}

	/**
	 * The anvil/book path runs the same gate, end to end through a real AnvilMenu. This
	 * is each loader's AnvilMenuMixin: vanilla asks Enchantment.canEnchant, which has no
	 * per-item hook on 1.20.1.
	 */
	public static void anvilBookRespectsEnchantGate(GameTestHelper helper) {
		ServerPlayer player = mockPlayer(helper);
		// Vanilla lets a creative player force any enchantment on in the anvil, which
		// would make the refusal below vacuous.
		player.getAbilities().instabuild = false;
		var menu = new AnvilMenu(1, player.getInventory(),
				ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(BlockPos.ZERO)));

		menu.getSlot(0).set(newTool(ToolType.SWORD));
		menu.getSlot(1).set(net.minecraft.world.item.EnchantedBookItem.createForEnchantment(
				new net.minecraft.world.item.enchantment.EnchantmentInstance(Enchantments.SHARPNESS, 1)));
		menu.createResult();
		ItemStack result = menu.getSlot(2).getItem();
		helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, result) == 1,
				"a sharpness book should apply to a Random Sword in the anvil, got " + result);

		menu.getSlot(0).set(newTool(ToolType.SWORD));
		menu.getSlot(1).set(net.minecraft.world.item.EnchantedBookItem.createForEnchantment(
				new net.minecraft.world.item.enchantment.EnchantmentInstance(Enchantments.BLOCK_EFFICIENCY, 1)));
		menu.createResult();
		helper.assertTrue(menu.getSlot(2).getItem().isEmpty(),
				"an efficiency book must not apply to a Random Sword, got " + menu.getSlot(2).getItem());

		helper.succeed();
	}

	private static boolean tableOffers(ItemStack stack, Enchantment wanted, Enchantment other) {
		return EnchantmentHelper
				.getAvailableEnchantmentResults(30, stack, false).stream()
				.anyMatch(instance -> instance.enchantment == wanted);
	}

	/** A Random Axe strips logs (preserving the axis), scrapes and un-waxes copper. */
	public static void axeToolActions(GameTestHelper helper) {
		ServerPlayer player = mockPlayer(helper);
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
		ServerPlayer player = mockPlayer(helper);
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
		ServerPlayer player = mockPlayer(helper);
		var menu = new AnvilMenu(1, player.getInventory(),
				ContainerLevelAccess.create(helper.getLevel(),
						helper.absolutePos(BlockPos.ZERO)));

		// Damaged left + enchanted right: without the block, vanilla's combine path
		// definitely produces a repaired, enchant-merged result (two pristine tools
		// would yield nothing anyway, making the assert vacuous).
		Enchantment sharpness = Enchantments.SHARPNESS;
		ItemStack left = newTool(ToolType.SWORD);
		left.setDamageValue(50);
		ItemStack right = newTool(ToolType.SWORD);
		right.enchant(sharpness, 1);

		menu.getSlot(0).set(left);
		menu.getSlot(1).set(right);
		menu.createResult();
		helper.assertTrue(menu.getSlot(2).getItem().isEmpty(),
				"combining two Random Tools in an anvil must yield nothing, got " + menu.getSlot(2).getItem());

		// Control: the tag-driven material repair path must still work.
		ItemStack damaged = newTool(ToolType.SWORD);
		damaged.setDamageValue(50);
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
		var table = helper.getLevel().getServer().getLootData().getLootTable(new ResourceLocation("chests/simple_dungeon"));
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
		var table = helper.getLevel().getServer().getLootData().getLootTable(new ResourceLocation("blocks/chest"));
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
		ServerPlayer player = mockPlayer(helper);
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
		ServerPlayer player = mockPlayer(helper);
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
		// Not helper.assertEntityPresent: 1.20.1 measures the test bounds from the
		// structure block (one below the structure) and stops a block short at the top,
		// so an entity standing at y=2 in this 3-high structure is "outside" it.
		helper.assertFalse(helper.getLevel().getEntities(EntityType.BEE, zombie.getBoundingBox().inflate(4.0),
				net.minecraft.world.entity.Entity::isAlive).isEmpty(), "Hailey's Wrath should spawn a bee on the kill");

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
		ServerPlayer player = mockPlayer(helper);

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
				new Expected("catalyst", new ResourceLocation("redstone")),
				new Expected("stench", new ResourceLocation("rotten_flesh")),
		};

		for (Expected e : expected) {
			helper.assertTrue(BuiltInRegistries.ITEM.containsKey(e.ingredient()),
					"ingredient item should exist: " + e.ingredient());

			ResourceLocation recipeId = new ResourceLocation(RandomLoot.MODID, "trait_" + e.trait());
			boolean present = recipes.stream().anyMatch(
					h -> h.getId().equals(recipeId) && h instanceof TraitAdditionRecipe);
			helper.assertTrue(present, "trait recipe should load: " + recipeId);
		}

		helper.succeed();
	}

	/** Stench (sulfur) afflicts hostile mobs inside its radius with Slowness and Weakness. */
	public static void stenchDebuffsMobs(GameTestHelper helper) {
		ServerPlayer player = mockPlayer(helper);

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
		ServerPlayer player = mockPlayer(helper);
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
			helper.assertTrue(advancements.getAdvancement(new ResourceLocation(RandomLoot.MODID, id)) != null,
					"advancement " + id + " should be loaded");
		}

		helper.succeed();
	}

	private static void assertAdvancementDone(GameTestHelper helper, ServerPlayer player, String id) {
		var advancement = helper.getLevel().getServer().getAdvancements()
				.getAdvancement(new ResourceLocation(RandomLoot.MODID, id));
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
		ServerPlayer player = mockPlayer(helper);
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
		// Counts of one: this test is about the type gate. smithingTakeConsumesCount covers the price.
		TraitAdditionRecipe thorny = new TraitAdditionRecipe(
				new ResourceLocation(RandomLoot.MODID, "gametest_thorny"), Items.CACTUS, 1, "thorny");
		TraitAdditionRecipe critical = new TraitAdditionRecipe(
				new ResourceLocation(RandomLoot.MODID, "gametest_critical"), Items.GHAST_TEAR, 1, "critical");

		ItemStack sword = newTool(ToolType.SWORD);
		ItemStack chestplate = newArmor(ToolType.CHESTPLATE);

		ItemStack addTemplate = new ItemStack(ModItems.MOD_ADD.get());
		ItemStack subTemplate = new ItemStack(ModItems.MOD_SUB.get());
		ItemStack cactus = new ItemStack(Items.CACTUS);
		ItemStack ghastTear = new ItemStack(Items.GHAST_TEAR);
		Level level = helper.getLevel();

		helper.assertFalse(thorny.matches(new TraitAdditionRecipe.Input(addTemplate, sword, cactus).toContainer(), level),
				"armor-only trait must not smith onto a sword");
		helper.assertTrue(thorny.matches(new TraitAdditionRecipe.Input(addTemplate, chestplate, cactus).toContainer(), level),
				"thorny should smith onto a chestplate");
		helper.assertFalse(critical.matches(new TraitAdditionRecipe.Input(addTemplate, chestplate, ghastTear).toContainer(), level),
				"weapon-only trait must not smith onto a chestplate");
		helper.assertTrue(critical.matches(new TraitAdditionRecipe.Input(addTemplate, sword, ghastTear).toContainer(), level),
				"critical should smith onto a sword");
		// The subtraction template is ungated by type so a mismatched trait can always be
		// stripped - but only when the trait is actually on the gear. Matching without it
		// consumed the template and the addition and handed back an identical item.
		helper.assertFalse(thorny.matches(new TraitAdditionRecipe.Input(subTemplate, sword, cactus).toContainer(), level),
				"the subtraction template must not match gear that lacks the trait");
		ItemStack mismatched = newTool(ToolType.SWORD);
		LootUtils.addModifier(mismatched, ModifierRegistry.getModifier("thorny"));
		helper.assertTrue(thorny.matches(new TraitAdditionRecipe.Input(subTemplate, mismatched, cactus).toContainer(), level),
				"the subtraction template should still strip a mismatched trait");

		helper.succeed();
	}

	/** A real SmithingMenu resolves the datapack recipe, crafts it, and runs the take-result hooks. */
	public static void smithingCraftSequence(GameTestHelper helper) {
		ServerPlayer player = mockPlayer(helper);
		BlockPos tablePos = new BlockPos(1, 1, 1);
		helper.setBlock(tablePos, Blocks.SMITHING_TABLE);

		Enchantment sharpness = Enchantments.SHARPNESS;
		ItemStack sword = LootUtils.buildTool(player, helper.getLevel(), helper.absolutePos(tablePos),
				ToolType.SWORD, 0.0f);
		sword.enchant(sharpness, 3);
		sword.setRepairCost(5);
		sword.setDamageValue(7);
		LootUtils.addModifier(sword, new DirtPlace("Nobody's Grace", 1));

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
		helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(sharpness, crafted) == 3,
				"smithing should preserve Sharpness III");
		helper.assertTrue(crafted.getBaseRepairCost() == 5,
				"smithing should preserve the anvil repair cost");
		helper.assertTrue(crafted.getDamageValue() == 7,
				"smithing should preserve durability damage");
		helper.assertTrue(menu.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem().isEmpty()
					&& menu.getSlot(SmithingMenu.BASE_SLOT).getItem().isEmpty()
					&& menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem().isEmpty(),
				"taking the result should consume all three smithing inputs");
		assertWorldForger(helper, crafted);
		assertAdvancementDone(helper, player, "add_trait");

		helper.succeed();
	}

	/**
	 * The gear's DirtPlace is named after this world's forger. Callers smith gear that
	 * starts with a made-up forger: the registry prototype's random one matches the
	 * world's one time in six, which would let a missing stamp pass.
	 */
	private static void assertWorldForger(GameTestHelper helper, ItemStack gear) {
		String expected = NameGenerator.forgerForWorld(helper.getLevel().getSeed(), 0.5f) + "'s Grace";
		String forged = LootUtils.getModifiers(gear).stream().filter(m -> m.tagName().equals("dirt_place"))
				.map(Modifier::name).findFirst().orElse("nothing");
		helper.assertTrue(expected.equals(forged),
				"smithed gear should credit this world's forger, got " + forged + " expected " + expected);
	}

	/**
	 * The {@code count} in a trait recipe's JSON survives parsing. It was read and thrown
	 * away, so every recipe cost one item whatever the file said. Also the net for a count
	 * nobody could pay: the codec cannot check it against the item's max stack size
	 * (components are not bound at datapack load), and a count above it would leave the
	 * trait unobtainable with nothing in the log.
	 */
	public static void traitRecipeCountsLoad(GameTestHelper helper) {
		helper.assertTrue(traitRecipe(helper, "torch_place").additionCount() == 64,
				"Spelunking should cost the 64 torches its recipe lists");
		helper.assertTrue(traitRecipe(helper, "thorny").additionCount() == 4,
				"Thorny should cost the 4 cactus its recipe lists");
		helper.assertTrue(traitRecipe(helper, "critical").additionCount() == 1,
				"a recipe with no count should cost one item");

		for (var holder : helper.getLevel().getRecipeManager().getRecipes()) {
			if (holder instanceof TraitAdditionRecipe recipe) {
				int stackSize = new ItemStack(recipe.additionItem()).getMaxStackSize();
				helper.assertTrue(recipe.additionCount() <= stackSize, holder.getId() + " asks for "
						+ recipe.additionCount() + " but only " + stackSize + " fit in the smithing slot");
			}
		}

		helper.succeed();
	}

	/** The datapack's recipe for a trait, as loaded. */
	private static TraitAdditionRecipe traitRecipe(GameTestHelper helper, String trait) {
		ResourceLocation id = new ResourceLocation(RandomLoot.MODID, "trait_" + trait);
		return helper.getLevel().getRecipeManager().getRecipes().stream()
				.filter(h -> h.getId().equals(id) && h instanceof TraitAdditionRecipe)
				.map(h -> (TraitAdditionRecipe) h)
				.findFirst()
				.orElseThrow(() -> new GameTestAssertException("trait recipe should load: " + id));
	}

	/** A smithing table with the given inputs in it, opened by the player. */
	private static SmithingMenu openSmithing(GameTestHelper helper, ServerPlayer player, ItemStack template,
			ItemStack base, ItemStack addition) {
		BlockPos tablePos = new BlockPos(1, 1, 1);
		helper.setBlock(tablePos, Blocks.SMITHING_TABLE);

		SmithingMenu menu = new SmithingMenu(1, player.getInventory(),
				ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(tablePos)));
		menu.getSlot(SmithingMenu.TEMPLATE_SLOT).set(template);
		menu.getSlot(SmithingMenu.BASE_SLOT).set(base);
		menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(addition);
		return menu;
	}

	/**
	 * Taking the result uses up the recipe's whole count. Vanilla's onTake shrinks each
	 * input by one, so the other three cactus here only go if the loader's SMITHING_TAKE
	 * hook really runs - loader_hooks_all_bound can only say it was declared. Also checks
	 * the preview, which has to vanish the moment the slot can no longer pay.
	 */
	public static void smithingTakeConsumesCount(GameTestHelper helper) {
		ServerPlayer player = mockPlayer(helper);
		SmithingMenu menu = openSmithing(helper, player, new ItemStack(ModItems.MOD_ADD.get()),
				newArmor(ToolType.CHESTPLATE), new ItemStack(Items.CACTUS, 3));

		helper.assertTrue(menu.getSlot(SmithingMenu.RESULT_SLOT).getItem().isEmpty(),
				"3 cactus should not preview a trait that costs 4");

		menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(new ItemStack(Items.CACTUS, 4));
		helper.assertTrue(hasTrait(menu.getSlot(SmithingMenu.RESULT_SLOT).getItem(), "thorny"),
				"4 cactus should preview thorny");

		menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).remove(1);
		helper.assertTrue(menu.getSlot(SmithingMenu.RESULT_SLOT).getItem().isEmpty(),
				"the preview should clear when the stack drops below the count");

		menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(new ItemStack(Items.CACTUS, 10));
		menu.clicked(SmithingMenu.RESULT_SLOT, 0, ClickType.PICKUP, player);

		helper.assertTrue(hasTrait(menu.getCarried(), "thorny"), "the taken result should contain thorny");
		ItemStack left = menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem();
		helper.assertTrue(left.is(Items.CACTUS) && left.getCount() == 6,
				"taking the result should consume exactly 4 of 10 cactus, left " + left.getCount());
		helper.assertTrue(menu.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem().isEmpty()
						&& menu.getSlot(SmithingMenu.BASE_SLOT).getItem().isEmpty(),
				"the template and the gear should still be consumed once each");

		helper.succeed();
	}

	/**
	 * Shift-clicking the result pays the same price and has the same effects as a click:
	 * the gear credits this world's forger and counts as a crafted trait. It is its own
	 * path through the menu: the result is moved into the inventory first and onTake is
	 * then handed an empty stack, so anything hung off the crafted item never runs for it.
	 */
	public static void smithingShiftClickConsumesCount(GameTestHelper helper) {
		ServerPlayer player = mockPlayer(helper);
		// DirtPlace never rolls on armor; it is here as the one trait named after a forger.
		ItemStack chestplate = newArmor(ToolType.CHESTPLATE);
		LootUtils.addModifier(chestplate, new DirtPlace("Nobody's Grace", 1));
		SmithingMenu menu = openSmithing(helper, player, new ItemStack(ModItems.MOD_ADD.get()), chestplate,
				new ItemStack(Items.CACTUS, 10));

		menu.clicked(SmithingMenu.RESULT_SLOT, 0, ClickType.QUICK_MOVE, player);

		ItemStack taken = ItemStack.EMPTY;
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize() && !hasTrait(taken, "thorny"); i++) {
			taken = inventory.getItem(i);
		}
		helper.assertTrue(hasTrait(taken, "thorny"),
				"shift-clicking should move the thorny chestplate into the inventory");
		ItemStack left = menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem();
		helper.assertTrue(left.is(Items.CACTUS) && left.getCount() == 6,
				"a shift-click take should consume exactly 4 of 10 cactus, left " + left.getCount());
		helper.assertTrue(menu.getSlot(SmithingMenu.RESULT_SLOT).getItem().isEmpty(),
				"nothing should be left to take once the gear is gone");
		assertWorldForger(helper, taken);
		assertAdvancementDone(helper, player, "add_trait");

		helper.succeed();
	}

	/**
	 * Through a real menu, the removal template previews with one item and takes one.
	 * Stripping a trait for the full price again would make undoing Spelunking cost a
	 * second stack of torches.
	 */
	public static void smithingRemovalCostsOne(GameTestHelper helper) {
		ServerPlayer player = mockPlayer(helper);
		ItemStack thornyChestplate = newArmor(ToolType.CHESTPLATE);
		LootUtils.addModifier(thornyChestplate, ModifierRegistry.getModifier("thorny"));

		SmithingMenu menu = openSmithing(helper, player, new ItemStack(ModItems.MOD_SUB.get()), thornyChestplate,
				new ItemStack(Items.CACTUS));

		ItemStack preview = menu.getSlot(SmithingMenu.RESULT_SLOT).getItem();
		helper.assertTrue(!preview.isEmpty() && !hasTrait(preview, "thorny"),
				"one cactus should be enough to preview the removal");

		menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(new ItemStack(Items.CACTUS, 10));
		menu.clicked(SmithingMenu.RESULT_SLOT, 0, ClickType.PICKUP, player);

		helper.assertTrue(!menu.getCarried().isEmpty() && !hasTrait(menu.getCarried(), "thorny"),
				"the taken result should have lost thorny");
		ItemStack left = menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem();
		helper.assertTrue(left.is(Items.CACTUS) && left.getCount() == 9,
				"removing a trait should consume exactly 1 of 10 cactus, left " + left.getCount());

		helper.succeed();
	}

	/**
	 * Blinding and Heat Resistant have a single level: vanilla only asks whether Blindness
	 * and Fire Resistance are present and never reads their amplifier, so a second level
	 * would take the recipe's items again and change nothing. Poisonous and Healing are the
	 * controls - the same setup has to keep leveling a trait whose amplifier does count.
	 */
	public static void singleLevelEffectsStopAtLevelOne(GameTestHelper helper) {
		ServerPlayer player = mockPlayer(helper);

		for (Modifier trait : List.of(ModifierRegistry.BLINDING, ModifierRegistry.FIRE_RESISTANT)) {
			ItemStack once = previewAdd(helper, player, newTool(gearFor(trait)), trait);
			helper.assertTrue(hasTrait(once, trait.tagName()), "fresh gear should still take " + trait.name());
			helper.assertTrue(previewAdd(helper, player, once, trait).isEmpty(),
					"gear that already has " + trait.name() + " should not preview a second level");

			// Gear that bought levels before the cap existed reads back at level I.
			var saved = trait.toNBT();
			saved.putInt(ModifierConstants.POWER, 3);
			Modifier restored = trait.fromNBT(saved);
			helper.assertTrue(restored.name().equals(trait.name()) && !restored.canLevel(),
					trait.name() + " saved at a higher level should read back at level I, got " + restored.name());
		}

		for (Modifier trait : List.of(ModifierRegistry.POISONOUS, ModifierRegistry.REGENERATING)) {
			ItemStack once = previewAdd(helper, player, newTool(gearFor(trait)), trait);
			helper.assertTrue(!previewAdd(helper, player, once, trait).isEmpty(),
					trait.name() + " should still preview its next level");
		}

		helper.succeed();
	}

	/** Hurt effects go on weapons, held effects on mining tools. */
	private static ToolType gearFor(Modifier trait) {
		return trait.forTool(ToolType.SWORD) ? ToolType.SWORD : ToolType.PICKAXE;
	}

	/** The smithing preview for adding the trait to this gear, paid with a full stack so the price does not matter. */
	private static ItemStack previewAdd(GameTestHelper helper, ServerPlayer player, ItemStack gear, Modifier trait) {
		ItemStack payment = new ItemStack(traitRecipe(helper, trait.tagName()).additionItem(), 64);
		return openSmithing(helper, player, new ItemStack(ModItems.MOD_ADD.get()), gear, payment)
				.getSlot(SmithingMenu.RESULT_SLOT).getItem().copy();
	}

	/**
	 * CloneItem (smithing/retexture output) must carry over enchantments and repair cost,
	 * and must arrive with its derived components already stamped: it builds a fresh
	 * stack rather than going through GearTags.mutate, so it has to refresh explicitly.
	 * Without that the smithing output carried default attributes and durability until
	 * its first inventoryTick.
	 */
	public static void clonePreservesEnchantments(GameTestHelper helper) {
		Enchantment sharpness = Enchantments.SHARPNESS;

		ItemStack sword = newTool(ToolType.SWORD);
		LootUtils.setStats(sword, 5.0f);
		sword.enchant(sharpness, 3);
		sword.setRepairCost(5);
		sword.setDamageValue(7);

		ItemStack clone = LootUtils.CloneItem(sword);

		helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(sharpness, clone) == 3,
				"clone should keep Sharpness III");
		helper.assertTrue(clone.getBaseRepairCost() == 5,
				"clone should keep the anvil repair cost");
		helper.assertTrue(clone.getDamageValue() == 7,
				"clone should keep its durability damage");

		helper.assertFalse(
				!hasStampedAttributes(clone),
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

	/** A survival mock player holding a fresh tool of the given type with the given traits. */
	private static ServerPlayer survivalPlayerHolding(GameTestHelper helper, ItemStack tool, String... traits) {
		ServerPlayer player = mockPlayer(helper);
		player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
		for (String trait : traits) {
			LootUtils.addModifier(tool, ModifierRegistry.getModifier(trait));
		}
		player.setItemInHand(InteractionHand.MAIN_HAND, tool);
		return player;
	}

	/**
	 * Lumbering fells the logs above the one the player broke, through the real
	 * game-mode path. NeoForge calls Item.mineBlock before it removes the block and
	 * vanilla/Fabric after, so a trait that re-read the broken position from the world
	 * saw air on Fabric and silently never fired.
	 */
	public static void lumberingFellsTree(GameTestHelper helper) {
		for (int y = 0; y < 3; y++) {
			helper.setBlock(new BlockPos(1, y, 1), Blocks.OAK_LOG);
		}
		ServerPlayer player = survivalPlayerHolding(helper, newTool(ToolType.AXE), "lumbering");

		helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(1, 0, 1))),
				"the bottom log should break");
		helper.assertBlockNotPresent(Blocks.OAK_LOG, new BlockPos(1, 1, 1));
		helper.assertBlockNotPresent(Blocks.OAK_LOG, new BlockPos(1, 2, 1));

		helper.succeed();
	}

	/** Veiny follows the vein out from the block the player broke; same ordering trap as Lumbering. */
	public static void veinyBreaksVein(GameTestHelper helper) {
		for (int x = 0; x < 3; x++) {
			helper.setBlock(new BlockPos(x, 1, 1), Blocks.IRON_ORE);
		}
		ItemStack pick = newTool(ToolType.PICKAXE);
		ServerPlayer player = survivalPlayerHolding(helper, pick, "veiny");
		// Veiny only spreads while crouching; a mock player never ticks its own pose.
		player.setShiftKeyDown(true);
		player.setPose(Pose.CROUCHING);

		helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(0, 1, 1))),
				"the first ore should break");
		helper.assertBlockNotPresent(Blocks.IRON_ORE, new BlockPos(1, 1, 1));
		helper.assertBlockNotPresent(Blocks.IRON_ORE, new BlockPos(2, 1, 1));
		helper.assertTrue(pick.getDamageValue() == 3,
				"each ore in the vein should cost one durability, got " + pick.getDamageValue());

		helper.succeed();
	}

	/**
	 * Melting smelts the whole dropped stack. A smelting recipe's result is for one
	 * input item, so replacing the drop with it turned copper ore's 2-5 raw copper (or
	 * any Fortune stack) into a single ingot.
	 */
	public static void meltingKeepsStackCount(GameTestHelper helper) {
		BlockPos relative = new BlockPos(1, 1, 1);
		helper.setBlock(new BlockPos(1, 0, 1), Blocks.STONE);
		BlockPos absolute = helper.absolutePos(relative);
		ItemStack pick = newTool(ToolType.PICKAXE);
		ServerPlayer player = survivalPlayerHolding(helper, pick);

		ItemEntity raw = new ItemEntity(helper.getLevel(), absolute.getX() + 0.5, absolute.getY() + 0.5,
				absolute.getZ() + 0.5, new ItemStack(Items.RAW_COPPER, 5));
		raw.setDeltaMovement(Vec3.ZERO);
		// Melting waits 50 ms of wall-clock time and then skips drops older than 10
		// ticks. The gametest server ticks flat out, so that can be hundreds of ticks
		// and the pass came down to how long the rest of the batch took. An ageless
		// item takes the tick rate out of it.
		raw.setUnlimitedLifetime();
		helper.getLevel().addFreshEntity(raw);

		((BlockBreakModifier) ModifierRegistry.getModifier("melting")).startBreak(pick,
				Blocks.COPPER_ORE.defaultBlockState(), absolute, player);

		// Melting runs a moment after the break so the drops exist; poll for it.
		helper.succeedWhen(() -> {
			int ingots = 0;
			int stillRaw = 0;
			for (ItemEntity item : helper.getEntities(EntityType.ITEM, relative, 2.0)) {
				if (item.getItem().is(Items.COPPER_INGOT)) {
					ingots += item.getItem().getCount();
				} else if (item.getItem().is(Items.RAW_COPPER)) {
					stillRaw += item.getItem().getCount();
				}
			}
			helper.assertTrue(stillRaw == 0, "the raw copper should have been smelted");
			helper.assertTrue(ingots == 5, "5 raw copper should smelt into 5 ingots, got " + ingots);
		});
	}

	/**
	 * Blocks broken on the player's behalf (Excavator, Veiny, Lumbering) hand their
	 * block entity to the drop logic the way vanilla does. A shulker box's loot copies
	 * its contents from the block entity, so without it the box dropped empty.
	 */
	public static void areaBreakKeepsShulkerContents(GameTestHelper helper) {
		BlockPos relative = new BlockPos(1, 1, 1);
		helper.setBlock(new BlockPos(1, 0, 1), Blocks.STONE);
		helper.setBlock(relative, Blocks.SHULKER_BOX);
		ShulkerBoxBlockEntity box = (ShulkerBoxBlockEntity) helper.getBlockEntity(relative);
		box.setItem(0, new ItemStack(Items.DIAMOND, 3));

		ItemStack pick = newTool(ToolType.PICKAXE);
		ServerPlayer player = survivalPlayerHolding(helper, pick);
		BlockPos absolute = helper.absolutePos(relative);

		helper.assertTrue(LootUtils.breakBlockAsPlayer(pick, absolute, player, helper.getLevel(),
				helper.getLevel().getBlockState(absolute)), "the shulker box should break");

		ItemStack dropped = ItemStack.EMPTY;
		for (ItemEntity item : helper.getEntities(EntityType.ITEM, relative, 2.0)) {
			if (item.getItem().is(Items.SHULKER_BOX)) {
				dropped = item.getItem();
			}
		}
		helper.assertFalse(dropped.isEmpty(), "the shulker box should drop as an item");
		var blockEntityData = net.minecraft.world.item.BlockItem.getBlockEntityData(dropped);
		helper.assertTrue(blockEntityData != null
				&& !blockEntityData.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND).isEmpty(),
				"the dropped shulker box should still hold its contents");

		helper.succeed();
	}

	/**
	 * A Detecting marker is tagged-ore driven, is never saved, and is discarded outright
	 * when its block goes away. Markers used to be "killed" by teleporting them to
	 * (0, -256, 0) with zero health, where - that chunk being unloaded - they stopped
	 * ticking and were written to disk instead.
	 */
	public static void detectingMarkersAreDiscarded(GameTestHelper helper) {
		BlockPos relative = new BlockPos(1, 1, 1);
		helper.setBlock(relative, Blocks.COAL_ORE);
		ItemStack pick = newTool(ToolType.PICKAXE);
		ServerPlayer player = survivalPlayerHolding(helper, pick, "detecting");
		player.setPos(helper.absoluteVec(new Vec3(1.5, 2.0, 1.5)));

		ServerLevel level = helper.getLevel();
		HoldModifier detecting = (HoldModifier) ModifierRegistry.getModifier("detecting");
		boolean[] scanning = { true };
		// hold() only scans on every 20th game tick, so offer it every tick.
		helper.onEachTick(() -> {
			if (scanning[0]) {
				detecting.hold(pick, level, player);
			}
		});

		helper.runAfterDelay(21, () -> {
			List<Shulker> markers = helper.getEntities(EntityType.SHULKER, relative, 1.0);
			helper.assertTrue(markers.size() == 1, "one marker should highlight the ore, got " + markers.size());
			Shulker marker = markers.get(0);
			helper.assertFalse(marker.shouldBeSaved(), "markers must never be written to disk");

			scanning[0] = false;
			helper.setBlock(relative, Blocks.AIR);
			// The lifecycle sweep runs every 10 ticks.
			helper.runAfterDelay(12, () -> {
				helper.assertTrue(marker.isRemoved(), "the marker should be discarded once its ore is gone");
				helper.succeed();
			});
		});
	}

	/**
	 * Armor XP per hit is capped at the wearer's max health. The reported damage is not
	 * capped at the wearer's health - /kill and Executioner pass Float.MAX_VALUE - which
	 * used to round to Integer.MAX_VALUE XP: ~22 levels at once, or a negative total.
	 */
	public static void armorXpIsCapped(GameTestHelper helper) {
		ItemStack chest = newArmor(ToolType.CHESTPLATE);
		LootUtils.setLevelAndXP(chest, 0, 5);

		LivingEntity zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
		zombie.setItemSlot(EquipmentSlot.CHEST, chest);

		// genericKill is what /kill deals; it bypasses armor, so only the XP is in play here.
		ArmorDispatcher.onLivingDamagePost(zombie, zombie.damageSources().genericKill(), Float.MAX_VALUE);

		ItemStack worn = zombie.getItemBySlot(EquipmentSlot.CHEST);
		int expected = 5 + Math.round(zombie.getMaxHealth());
		helper.assertTrue(LootUtils.getLevel(worn) == 0 && LootUtils.getXP(worn) == expected,
				"a huge hit should grant at most a health bar of XP (" + expected + "), got level "
						+ LootUtils.getLevel(worn) + " xp " + LootUtils.getXP(worn));

		helper.succeed();
	}

	/**
	 * A maxed ("Unbreakable") Unbreaking trait covers every durability cost a tool
	 * pays, not just block breaks and hits: here the shared helper and a log strip.
	 */
	public static void unbreakableToolKeepsDurability(GameTestHelper helper) {
		ItemStack axe = newTool(ToolType.AXE);
		Modifier unbreaking = ModifierRegistry.getModifier("unbreaking");
		// First add lands at the base level; every repeat levels it up until maxed.
		for (int i = 0; i < 16; i++) {
			LootUtils.addModifier(axe, unbreaking);
		}
		ServerPlayer player = survivalPlayerHolding(helper, axe);

		LootUtils.damageGear(axe, 25, player, EquipmentSlot.MAINHAND);
		helper.assertTrue(axe.getDamageValue() == 0, "damageGear should spend nothing on an Unbreakable tool");

		BlockPos logPos = new BlockPos(1, 1, 1);
		helper.setBlock(logPos, Blocks.OAK_LOG);
		useOnBlock(helper, player, logPos);
		helper.assertBlockPresent(Blocks.STRIPPED_OAK_LOG, logPos);
		helper.assertTrue(player.getMainHandItem().getDamageValue() == 0,
				"stripping a log should not wear an Unbreakable axe");

		helper.succeed();
	}

	/**
	 * A chest-type table that rolls other chest-type tables still gets one injection
	 * roll. On Fabric every nested table carries its own injected pool, and each used to
	 * roll. Vanilla 1.20.1 has no chest table that rolls another (the newer branches use
	 * the trial vault's reward table), so this uses the mod's own
	 * gametest/chest_outer -> gametest/chest_inner pair.
	 */
	public static void lootInjectionRollsOncePerChest(GameTestHelper helper) {
		var table = helper.getLevel().getServer().getLootData().getLootTable(new ResourceLocation(RandomLoot.MODID, "gametest/chest_outer"));
		helper.assertTrue(table != LootTable.EMPTY, "nested chest gametest loot table should exist");

		var params = new LootParams.Builder(helper.getLevel())
				.create(LootContextParamSets.EMPTY);

		for (int i = 0; i < 300; i++) {
			int cases = 0;
			int templates = 0;
			for (ItemStack drop : table.getRandomItems(params)) {
				if (drop.is(ModItems.CASE.get())) {
					cases += drop.getCount();
				} else if (drop.is(ModItems.MOD_ADD.get())) {
					templates += drop.getCount();
				}
			}
			helper.assertTrue(cases <= 1 && templates <= 1,
					"one vault roll should inject at most one case and one template, got " + cases + " and "
							+ templates);
		}

		helper.succeed();
	}

	/**
	 * Early Bird keys off the target's health before the hit, read from the pre-damage
	 * hook. It used to add the tool's base damage back onto the post-hit health, which
	 * undercounts any hit bigger than that (a crit, Sharpness, Strength) and so missed
	 * genuinely untouched targets.
	 */
	public static void earlyBirdReadsPreHitHealth(GameTestHelper helper) {
		ItemStack sword = newTool(ToolType.SWORD);
		LootUtils.setStats(sword, 4.0f);
		ServerPlayer player = survivalPlayerHolding(helper, sword);
		EntityHurtModifier earlyBird = (EntityHurtModifier) ModifierRegistry.getModifier("early_bird");

		// Untouched target, hit for far more than the tool's own base damage.
		LivingEntity fresh = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
		fresh.hurt(fresh.damageSources().playerAttack(player), 12.0f);
		float freshAfterHit = fresh.getHealth();
		earlyBird.hurtEnemy(sword, fresh, player);
		helper.assertTrue(fresh.getHealth() < freshAfterHit,
				"Early Bird should add bonus damage to a target that was at full health before the hit");

		// Already-wounded target: no bonus, even when it is only a scratch short of full.
		LivingEntity wounded = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
		wounded.setHealth(wounded.getMaxHealth() * 0.97f);
		wounded.hurt(wounded.damageSources().playerAttack(player), 4.0f);
		float woundedAfterHit = wounded.getHealth();
		earlyBird.hurtEnemy(sword, wounded, player);
		helper.assertTrue(wounded.getHealth() == woundedAfterHit,
				"Early Bird should not trigger on a target that was already wounded");

		// A full-health hit earlier in the same tick must not vouch for a later hit: the
		// record describes the latest hit, and by then the target is wounded.
		LivingEntity hitTwice = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
		hitTwice.hurt(hitTwice.damageSources().generic(), 2.0f);
		hitTwice.hurt(hitTwice.damageSources().playerAttack(player), 8.0f);
		float hitTwiceAfter = hitTwice.getHealth();
		helper.assertTrue(hitTwiceAfter < hitTwice.getMaxHealth() - 2.0f, "the second hit should have landed");
		earlyBird.hurtEnemy(sword, hitTwice, player);
		helper.assertTrue(hitTwice.getHealth() == hitTwiceAfter,
				"Early Bird should not reuse an earlier same-tick full-health hit for a wounded target");

		// Another trait's bonus hit overwrites that record too, so the tool runs Early
		// Bird before its other traits. Through the real LootItem.hurtEnemy: a sword with
		// Early Bird on top of three bonus-damage traits must out-damage one with just
		// those three. Three, because the trait tag iterates in hash order and on 1.21.1
		// Early Bird happens to come first next to Feasting alone, which would let a
		// missing sort pass; here Feasting and Fragile iterate ahead of it.
		float[] lost = new float[2];
		String[][] traitSets = { { "feasting", "fragile", "munchies", "early_bird" },
				{ "feasting", "fragile", "munchies" } };
		for (int i = 0; i < traitSets.length; i++) {
			ItemStack traited = newTool(ToolType.SWORD);
			LootUtils.setStats(traited, 4.0f);
			for (String trait : traitSets[i]) {
				LootUtils.addModifier(traited, ModifierRegistry.getModifier(trait));
			}
			// Munchies may have eaten into the hunger bar that Feasting reads.
			player.getFoodData().setFoodLevel(20);
			LivingEntity target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
			target.hurt(target.damageSources().playerAttack(player), 6.0f);
			float afterHit = target.getHealth();
			((LootItem) traited.getItem()).hurtEnemy(traited, target, player);
			lost[i] = afterHit - target.getHealth();
		}
		helper.assertTrue(lost[1] > 0.0f, "the other traits should add their own bonus damage");
		helper.assertTrue(lost[0] > lost[1],
				"Early Bird should still trigger when the tool has another bonus-damage trait, lost "
						+ lost[0] + " vs " + lost[1]);

		helper.succeed();
	}

	/**
	 * Fragile ("25% more damage and speed") and Munchies ("15% stat boost") pay out in
	 * melee too. Their stat multiplier only reaches mining speed, so on a sword they
	 * used to be pure penalties.
	 */
	public static void statBoostTraitsBoostMelee(GameTestHelper helper) {
		ItemStack sword = newTool(ToolType.SWORD);
		LootUtils.setStats(sword, 4.0f);
		ServerPlayer player = survivalPlayerHolding(helper, sword);

		for (String trait : new String[] { "fragile", "munchies" }) {
			LivingEntity zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
			float before = zombie.getHealth();
			((EntityHurtModifier) ModifierRegistry.getModifier(trait)).hurtEnemy(sword, zombie, player);
			helper.assertTrue(zombie.getHealth() < before, trait + " should add melee damage on a sword");
		}

		helper.succeed();
	}

	/**
	 * A right-click-anywhere trait on the main-hand tool gives the off-hand item the
	 * click first and fires only when that item really did nothing with it - decided by
	 * the off-hand use's actual result, not by what kind of item it is. Driven the way
	 * the client sends a click; Flame Thrower costs durability when it fires, so the
	 * sword's damage says which way the click went.
	 */
	public static void rightClickTraitsYieldToUsableOffhand(GameTestHelper helper) {
		ItemStack sword = newTool(ToolType.SWORD);
		ServerPlayer player = survivalPlayerHolding(helper, sword, "flame_thrower");
		BlockPos floor = new BlockPos(1, 0, 1);
		helper.setBlock(floor, Blocks.STONE);
		Vec3 onFloor = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
		// High above the test, so no block is within reach in any direction.
		Vec3 inTheAir = onFloor.add(0.0, 60.0, 0.0);

		player.setPos(inTheAir);
		helper.assertTrue(flameThrowerFires(helper, player, sword, null),
				"Flame Thrower should fire with an empty off hand");

		// Off-hand items that act on a click in the air take it; the trait stays quiet.
		player.getFoodData().setFoodLevel(10);
		for (Item item : List.of(Items.SHIELD, Items.BREAD, Items.ENDER_PEARL, Items.SNOWBALL, Items.EGG,
				Items.FISHING_ROD)) {
			player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(item));
			helper.assertFalse(flameThrowerFires(helper, player, sword, null),
					"a right-click trait should yield to an off-hand " + item);
		}

		// Off-hand items that do nothing with this click leave it to the trait - including
		// usable ones that have nothing to act on: an empty bucket aimed at the sky, bread
		// on a full stomach, a bow with no arrows.
		player.getFoodData().setFoodLevel(20);
		for (Item item : List.of(Items.TOTEM_OF_UNDYING, Items.STICK, Items.FIREWORK_ROCKET, Items.BUCKET,
				Items.BREAD, Items.BOW, Items.TORCH)) {
			player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(item));
			helper.assertTrue(flameThrowerFires(helper, player, sword, null),
					"a right-click trait should fire when an off-hand " + item + " does nothing");
		}

		// On a targeted block the same rule holds: the torch and the flint and steel act on
		// the stone, so the trait yields; bone meal and a loot axe cannot, so it fires.
		player.setPos(onFloor);
		for (ItemStack acts : List.of(new ItemStack(Items.TORCH), new ItemStack(Items.FLINT_AND_STEEL))) {
			player.setItemInHand(InteractionHand.OFF_HAND, acts);
			helper.assertFalse(flameThrowerFires(helper, player, sword, floor),
					"a right-click trait should yield while an off-hand " + acts.getItem() + " acts on the block");
			helper.setBlock(floor.above(), Blocks.AIR);
		}
		for (ItemStack cannot : List.of(new ItemStack(Items.BONE_MEAL), newTool(ToolType.AXE))) {
			player.setItemInHand(InteractionHand.OFF_HAND, cannot);
			helper.assertTrue(flameThrowerFires(helper, player, sword, floor),
					"a right-click trait should fire when an off-hand " + cannot.getItem()
							+ " cannot act on the block");
		}
		// ...and the same loot axe does take the click on a log it can strip.
		helper.setBlock(floor, Blocks.OAK_LOG);
		helper.assertFalse(flameThrowerFires(helper, player, sword, floor),
				"a right-click trait should yield while an off-hand loot axe strips the log");
		helper.setBlock(floor, Blocks.STONE);

		// Held in the off hand, the tool has nothing to yield to once the main hand passes.
		player.setPos(inTheAir);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
		player.setItemInHand(InteractionHand.OFF_HAND, sword);
		helper.assertTrue(flameThrowerFires(helper, player, sword, null),
				"a right-click trait used from the off hand should fire");

		// A click the off hand settled on a block leaves nothing pending: an unrelated
		// off-hand item use right behind it, in the same tick, must not set the trait off.
		player.setItemInHand(InteractionHand.MAIN_HAND, sword);
		player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH));
		player.setPos(onFloor);
		helper.assertFalse(flameThrowerFires(helper, player, sword, floor), "the torch should take the click");
		helper.setBlock(floor.above(), Blocks.AIR);
		player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STICK));
		player.gameMode.useItem(player, helper.getLevel(), player.getOffhandItem(), InteractionHand.OFF_HAND);
		helper.assertTrue(sword.getDamageValue() == 0,
				"a click the off hand used on a block must not leave the trait pending");

		// And a deferral nothing answers at all goes stale: the main-hand use arrives
		// alone, then an unrelated off-hand use a few ticks later must not fire it.
		player.setPos(inTheAir);
		player.gameMode.useItem(player, helper.getLevel(), sword, InteractionHand.MAIN_HAND);
		helper.runAfterDelay(3, () -> {
			player.gameMode.useItem(player, helper.getLevel(), player.getOffhandItem(), InteractionHand.OFF_HAND);
			helper.assertTrue(sword.getDamageValue() == 0, "a stale deferral must not fire the trait later");
			helper.succeed();
		});
	}

	/**
	 * Right-clicks the way the client sends it and reports whether the Flame Thrower
	 * sword fired. For each hand in turn: the block use when a block is targeted, then
	 * the item use, stopping at the first that succeeds (or, for a block use, fails).
	 */
	private static boolean flameThrowerFires(GameTestHelper helper, ServerPlayer player, ItemStack sword,
			@Nullable BlockPos target) {
		sword.setDamageValue(0);
		rightClick(helper, player, target);
		boolean fired = sword.getDamageValue() > 0;

		// Leave nothing behind for the next case or the neighbouring tests: thrown
		// projectiles, fireballs, a raised shield.
		player.stopUsingItem();
		helper.getLevel().getEntities(player, player.getBoundingBox().inflate(8.0), e -> !(e instanceof ServerPlayer))
				.forEach(e -> e.discard());
		return fired;
	}

	private static void rightClick(GameTestHelper helper, ServerPlayer player, @Nullable BlockPos target) {
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack held = player.getItemInHand(hand);

			if (target != null) {
				BlockPos absolute = helper.absolutePos(target);
				var hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
				InteractionResult onBlock = player.gameMode.useItemOn(player, helper.getLevel(), held, hand, hit);
				if (onBlock.consumesAction() || onBlock == InteractionResult.FAIL) {
					return;
				}
			}

			if (!held.isEmpty() && player.gameMode.useItem(player, helper.getLevel(), held, hand).consumesAction()) {
				return;
			}
		}
	}

	/**
	 * 1.21.1 only. The client replays the hand re-equip animation when a component on
	 * the held stack changes, and this version has no per-component opt-out (26.x flags
	 * tool_mod with ignoreSwapAnimation), so each loader's gear item asks
	 * {@link LootGearItem#sameApartFromToolMod} instead. XP and trait changes alone must
	 * read as the same gear; durability and a different piece of gear must not. Also the
	 * net for GearTags.mutate editing a copy rather than the stored tag.
	 */
	public static void toolModAloneDoesNotReequip(GameTestHelper helper) {
		ItemStack held = newTool(ToolType.PICKAXE);
		held.setHoverName(Component.literal("Old Reliable"));
		LootUtils.setLevelAndXP(held, 0, 1);

		// The copy shares its component values with the original, as the server's
		// record of what it last sent a client does. GearTags.mutate used to edit the
		// shared tag in place, which changed both and hid the change from that diff.
		ItemStack gainedXp = held.copy();
		LootUtils.setLevelAndXP(gainedXp, 0, 3);
		helper.assertTrue(LootUtils.getXP(held) == 1 && !ItemStack.matches(held, gainedXp),
				"an XP gain should change the stack it was made on and leave its copies alone");

		LootUtils.addModifier(gainedXp, ModifierRegistry.getModifier("veiny"));
		helper.assertTrue(LootGearItem.sameApartFromToolMod(held, gainedXp),
				"an XP or trait change alone should not replay the re-equip animation");
		helper.assertTrue(LootGearItem.sameApartFromToolMod(gainedXp, held), "the check should not depend on order");

		ItemStack worn = held.copy();
		worn.setDamageValue(1);
		helper.assertFalse(LootGearItem.sameApartFromToolMod(held, worn),
				"durability loss should still animate, as it does for vanilla tools on 1.21.1");

		ItemStack other = held.copy();
		other.setHoverName(Component.literal("Something Else"));
		helper.assertFalse(LootGearItem.sameApartFromToolMod(held, other),
				"swapping to a different piece of gear should still animate");

		ItemStack unnamed = held.copy();
		unnamed.resetHoverName();
		helper.assertFalse(LootGearItem.sameApartFromToolMod(held, unnamed)
				|| LootGearItem.sameApartFromToolMod(unnamed, held),
				"a component present on only one side should count as a difference");

		helper.succeed();
	}
}
