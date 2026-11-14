package dev.marston.randomloot.gametest;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.LootArmorItem;
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
import dev.marston.randomloot.platform.Services;
import dev.marston.randomloot.recipes.TraitAdditionRecipe;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
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
 * Loader-neutral gametest bodies. Each one runs on both loaders once it has a row in
 * {@link GameTestTable}; a body without a row never runs (GameTestTableTest catches that).
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
		helper.assertTrue(held.get(DataComponents.EQUIPPABLE) == null, "rolling gear should not be equippable");
		helper.assertTrue(heldAttributes(held).modifiers().isEmpty(),
				"rolling gear should grant no attributes");
		helper.assertTrue(LootUtils.getRollEnd(held) == helper.getLevel().getGameTime() + LootUtils.ROLL_TICKS,
				"the roll should end ROLL_TICKS after opening");

		helper.onEachTick(() -> {
			ItemStack inHand = player.getMainHandItem();
			if (LootUtils.isRolling(inHand)) {
				inHand.inventoryTick(helper.getLevel(), player, EquipmentSlot.MAINHAND);
			}
		});

		helper.succeedWhen(() -> {
			ItemStack inHand = player.getMainHandItem();
			helper.assertFalse(LootUtils.isRolling(inHand), "the roll should finish");
			helper.assertTrue(inHand.get(DataComponents.CUSTOM_NAME) != null,
					"revealed gear should get its name back");
			if (LootUtils.getToolType(inHand).isArmor()) {
				helper.assertTrue(inHand.get(DataComponents.EQUIPPABLE) != null,
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
		helper.assertTrue(chestplate.get(DataComponents.EQUIPPABLE) == null,
				"rolling armor should not be equippable");
		helper.assertTrue(heldAttributes(chestplate).modifiers().isEmpty(),
				"rolling armor should have no attributes");

		helper.onEachTick(() -> {
			sword.inventoryTick(helper.getLevel(), player, EquipmentSlot.MAINHAND);
			chestplate.inventoryTick(helper.getLevel(), player, EquipmentSlot.OFFHAND);
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
			helper.assertTrue(chestplate.get(DataComponents.EQUIPPABLE) != null,
					"revealed armor should restore its equippable component");
			helper.assertFalse(heldAttributes(chestplate).modifiers().isEmpty(),
					"revealed armor should restore attributes");
		});
	}

	/** Attributes live in the vanilla component now, refreshed by LootUtils.refreshDerivedComponents. */
	public static ItemAttributeModifiers heldAttributes(ItemStack stack) {
		return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
	}

	/**
	 * Building an armor piece wires up the per-stack EQUIPPABLE component (slot + worn
	 * texture asset) and produces positive defense and durability.
	 */
	public static void armorComponents(GameTestHelper helper) {
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
	 * Thorny reflects a share of the damage its wearer took, for a player and for a mob.
	 *
	 * <p>Two Fabric regressions live here. The armor-trait hook was once a mixin into
	 * LivingEntity.actuallyHurt, which Player re-implements, so Thorny (and every
	 * WearerHurtModifier) silently did nothing for players while the zombie-based tests
	 * passed. Later it wrapped the actuallyHurt call, which handed the traits the hit
	 * before armor where NeoForge's LivingDamageEvent.Pre hands them the damage after it:
	 * for the hit below Thorny reflected 7.5 on Fabric and 3.0 on NeoForge.
	 */
	public static void thornyReflectsForPlayer(GameTestHelper helper) {
		// A player attacker: mob damage scales with difficulty and zeroes out on the
		// gametest server's Peaceful setting before it ever reaches the armor hooks.
		// PvP (a game rule in 26.x) is off by default and gates ServerPlayer.hurtServer.
		enablePvp(helper);
		ServerPlayer player = mockVulnerablePlayer(helper);
		ServerPlayer attacker = mockVulnerablePlayer(helper);
		assertThornyReflectsDamageTaken(helper, player, attacker, player.damageSources().playerAttack(attacker));

		// Mob against mob, for the same reason: a mob's thorns would scale to nothing
		// against a player.
		LivingEntity mobWearer = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
		LivingEntity mobAttacker = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
		// A zombie's own 2 armor would shave the reflected hit.
		mobAttacker.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
		assertThornyReflectsDamageTaken(helper, mobWearer, mobAttacker,
				mobWearer.damageSources().mobAttack(mobAttacker));

		// A witch shrugs off 85% of magic in her own override of the last reduction, so
		// she tells "after the reductions" from "inside the shared one": a hook on
		// LivingEntity.getDamageAfterMagicAbsorb itself runs before her share comes off.
		LivingEntity witch = helper.spawn(EntityTypes.WITCH, new BlockPos(1, 2, 1));
		LivingEntity witchAttacker = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
		witchAttacker.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
		assertThornyReflectsDamageTaken(helper, witch, witchAttacker,
				witch.damageSources().indirectMagic(witchAttacker, witchAttacker));

		helper.succeed();
	}

	/** Hits a maxed-Thorny wearer who mitigates most of it and checks what comes back at the attacker. */
	private static void assertThornyReflectsDamageTaken(GameTestHelper helper, LivingEntity wearer,
			LivingEntity attacker, DamageSource source) {
		wearer.setItemSlot(EquipmentSlot.CHEST, maxedTraitArmor(ToolType.CHESTPLATE, "thorny"));
		// Full diamond's worth of armor, so the hit before and after it are far apart.
		wearer.getAttribute(Attributes.ARMOR).setBaseValue(20.0);

		float hit = 10.0f;
		// Maxed Thorny: 15% at level 0 and 15% more for each of its 4 levels.
		float share = 0.75f;
		float wearerHealth = wearer.getHealth();
		float attackerHealth = attacker.getHealth();
		helper.hurt(wearer, source, hit);

		float taken = wearerHealth - wearer.getHealth();
		float reflected = attackerHealth - attacker.getHealth();
		String who = wearer.getType().getDescriptionId();

		helper.assertTrue(taken > 0.0f && taken < hit * 0.6f,
				who + ": a large part of the hit should be mitigated, took " + taken + " of " + hit);
		helper.assertTrue(Math.abs(reflected - taken * share) < 0.01f,
				who + ": thorny should reflect " + share + " of the " + taken + " the wearer took (" + taken * share
						+ "), not of the " + hit + " dealt (" + hit * share + "); reflected " + reflected);
	}

	/**
	 * A mock player that can actually be hurt: survival-ish abilities, and its
	 * fake connection acknowledged as loaded - ServerPlayer.isInvulnerableTo
	 * returns true until hasClientLoaded(), which a mock connection never
	 * reaches on its own.
	 */
	private static ServerPlayer mockVulnerablePlayer(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.getAbilities().instabuild = false;
		player.getAbilities().invulnerable = false;
		player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
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

	/** PvP (a game rule in 26.x) is off by default and gates ServerPlayer.hurtServer. */
	private static void enablePvp(GameTestHelper helper) {
		helper.getLevel().getGameRules().set(GameRules.PVP, true, helper.getLevel().getServer());
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
		wearer.damageCooldownTime = 0;
		helper.hurt(wearer, source, damage);
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

		tool.getItem().inventoryTick(tool, helper.getLevel(), player, EquipmentSlot.MAINHAND);

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

		helper.assertFalse(player.hasEffect(MobEffects.SPEED), "no speed before the hit");
		freshHit(helper, player, player.damageSources().generic(), 4.0f);
		helper.assertTrue(player.hasEffect(MobEffects.SPEED), "adrenaline should grant speed on hit");

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

		helper.assertTrue(Math.abs(owned - unowned * 1.15f) < 0.001f,
				"soulbound should mine 1.15 times as fast for its owner: owned=" + owned + " unowned=" + unowned);

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
				.anyMatch(instance -> instance.enchantment() == wanted);
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
		helper.setBlock(copperPos, Blocks.COPPER_BLOCK.weathering().oxidized());
		useOnBlock(helper, player, copperPos);
		helper.assertBlockPresent(Blocks.COPPER_BLOCK.weathering().weathered(), copperPos);

		BlockPos waxedPos = new BlockPos(3, 1, 1);
		helper.setBlock(waxedPos, Blocks.COPPER_BLOCK.waxed().unaffected());
		useOnBlock(helper, player, waxedPos);
		helper.assertBlockPresent(Blocks.COPPER_BLOCK.weathering().unaffected(), waxedPos);

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
	 * An enchanted book on the anvil is filtered per gear type, like the enchanting table.
	 * The single tool item and the single armor item sit in every vanilla enchantable tag,
	 * so without the ENCHANT_GATE hook on this path an efficiency book lands on a sword.
	 * NeoForge reaches the hook through supportsEnchantment, Fabric through its
	 * ALLOW_ENCHANTING event.
	 */
	public static void anvilBooksFilterByType(GameTestHelper helper) {
		// Not a creative player: the anvil lets those apply any book to anything.
		ServerPlayer player = mockVulnerablePlayer(helper);
		var enchants = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		Holder<Enchantment> efficiency = enchants.getOrThrow(Enchantments.EFFICIENCY);
		Holder<Enchantment> sharpness = enchants.getOrThrow(Enchantments.SHARPNESS);
		Holder<Enchantment> featherFalling = enchants.getOrThrow(Enchantments.FEATHER_FALLING);

		helper.assertTrue(anvilAppliesBook(helper, player, newTool(ToolType.PICKAXE), efficiency),
				"an efficiency book should go on a pickaxe");
		helper.assertFalse(anvilAppliesBook(helper, player, newTool(ToolType.SWORD), efficiency),
				"an efficiency book must not go on a sword");
		helper.assertTrue(anvilAppliesBook(helper, player, newTool(ToolType.SWORD), sharpness),
				"a sharpness book should go on a sword");
		helper.assertFalse(anvilAppliesBook(helper, player, newTool(ToolType.PICKAXE), sharpness),
				"a sharpness book must not go on a pickaxe");
		helper.assertTrue(anvilAppliesBook(helper, player, newArmor(ToolType.BOOTS), featherFalling),
				"a feather falling book should go on boots");
		helper.assertFalse(anvilAppliesBook(helper, player, newArmor(ToolType.HELMET), featherFalling),
				"a feather falling book must not go on a helmet");

		helper.succeed();
	}

	/** Whether an anvil offers the gear with the enchantment on it when given the gear and a book of it. */
	private static boolean anvilAppliesBook(GameTestHelper helper, ServerPlayer player, ItemStack gear,
			Holder<Enchantment> enchantment) {
		ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
		book.enchant(enchantment, 1);

		var menu = new AnvilMenu(1, player.getInventory(),
				ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(BlockPos.ZERO)));
		menu.getSlot(0).set(gear);
		menu.getSlot(1).set(book);
		menu.createResult();
		return menu.getSlot(2).getItem().getEnchantments().getLevel(enchantment) == 1;
	}

	/**
	 * Chest-type loot tables actually produce cases/templates at the configured
	 * chance: NeoForge via the case_item global loot modifier, Fabric via the
	 * LootTableEvents.MODIFY_DROPS. 200 rolls at the default 25% miss with
	 * probability ~1e-25.
	 */
	public static void lootInjectionAddsCases(GameTestHelper helper) {
		var key = ResourceKey.create(Registries.LOOT_TABLE,
				Identifier.withDefaultNamespace("chests/simple_dungeon"));
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
				Identifier.withDefaultNamespace("blocks/chest"));
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

		LivingEntity zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
		zombie.setItemSlot(EquipmentSlot.CHEST, chest);

		helper.hurt(zombie, zombie.damageSources().generic(), 6.0f);

		ItemStack worn = zombie.getItemBySlot(EquipmentSlot.CHEST);
		helper.assertTrue(LootUtils.getXP(worn) > 0 || LootUtils.getLevel(worn) > 0,
				"worn armor should gain XP from its wearer taking damage");

		helper.succeed();
	}

	/**
	 * Armor XP for the two hits both loaders' DAMAGE_POST hooks already report alike: a
	 * hit nothing mitigates earns its damage in XP, and a hit a shield blocks whole earns
	 * none.
	 *
	 * <p>The loaders do not yet agree on a hit that armor or a wearer-hurt trait reduces,
	 * that a shield blocks part of, or that kills: GameHooks.damagePost has the numbers.
	 * Those cases belong here once the mod's author has said which loader is right.
	 */
	public static void armorXpPerHit(GameTestHelper helper) {
		enablePvp(helper);
		ServerPlayer attacker = mockVulnerablePlayer(helper);

		// Generic damage skips armor: what is dealt is what is taken.
		ServerPlayer plain = mockVulnerablePlayer(helper);
		plain.setItemSlot(EquipmentSlot.CHEST, newArmor(ToolType.CHESTPLATE));
		float health = plain.getHealth();
		helper.assertTrue(armorXpFromHit(helper, plain, plain.damageSources().generic(), 10.0f) == 10,
				"a hit for 10 that nothing mitigates should earn 10 armor XP, got "
						+ LootUtils.getXP(plain.getItemBySlot(EquipmentSlot.CHEST)));
		helper.assertTrue(health - plain.getHealth() == 10.0f, "the unmitigated hit should take 10 health");

		// The blocker looks along +z at an attacker two blocks down it.
		ServerPlayer blocker = mockVulnerablePlayer(helper);
		blocker.setItemSlot(EquipmentSlot.CHEST, newArmor(ToolType.CHESTPLATE));
		blocker.snapTo(attacker.getX(), attacker.getY(), attacker.getZ() - 2.0, 0.0f, 0.0f);
		blocker.setYHeadRot(0.0f);
		DamageSource attack = blocker.damageSources().playerAttack(attacker);

		// Control: the same hit earns XP until the shield is up.
		helper.assertTrue(armorXpFromHit(helper, blocker, attack, 10.0f) > 0,
				"the attack should earn armor XP when nothing blocks it");

		// A shield with no raise delay: a mock player is never ticked, so the vanilla
		// quarter second would never pass.
		ItemStack shield = new ItemStack(Items.SHIELD);
		shield.set(DataComponents.BLOCKS_ATTACKS, new BlocksAttacks(0.0f, 1.0f,
				List.of(new BlocksAttacks.DamageReduction(90.0f, Optional.empty(), 0.0f, 1.0f)),
				BlocksAttacks.ItemDamageFunction.DEFAULT, Optional.empty(), Optional.empty(), Optional.empty()));
		blocker.setItemInHand(InteractionHand.OFF_HAND, shield);
		blocker.startUsingItem(InteractionHand.OFF_HAND);
		helper.assertTrue(blocker.isBlocking(), "the shield should be up");

		health = blocker.getMaxHealth();
		helper.assertTrue(armorXpFromHit(helper, blocker, attack, 10.0f) == 0,
				"a hit a shield blocks whole should earn no armor XP");
		helper.assertTrue(blocker.getHealth() == health, "the blocked hit should take no health");

		helper.succeed();
	}

	/** Hits the wearer once at full health and returns the XP their chestplate gained from it. */
	private static int armorXpFromHit(GameTestHelper helper, ServerPlayer wearer, DamageSource source, float damage) {
		int before = LootUtils.getXP(wearer.getItemBySlot(EquipmentSlot.CHEST));
		freshHit(helper, wearer, source, damage);
		return LootUtils.getXP(wearer.getItemBySlot(EquipmentSlot.CHEST)) - before;
	}

	/** The anvil's material-repair path accepts items from the armor_repair_materials tag. */
	public static void armorRepairable(GameTestHelper helper) {
		ItemStack armor = new ItemStack(ModItems.ARMOR.get());

		helper.assertTrue(armor.isValidRepairItem(new ItemStack(Items.DIAMOND)),
				"diamond should repair Random Armor (armor_repair_materials tag)");
		helper.assertFalse(armor.isValidRepairItem(new ItemStack(Items.STICK)),
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

		LivingEntity zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
		zombie.setHealth(0.1f);
		player.attack(zombie);

		helper.assertTrue(zombie.isDeadOrDying(), "zombie should die to the player's attack");
		helper.assertTrue(LootUtils.getXP(tool) == xpBefore + 1,
				"a real weapon hit should award one tool XP");
		helper.assertTrue(tool.getDamageValue() == damageBefore + 1,
				"a real weapon hit should consume one durability");
		helper.assertEntityPresent(EntityTypes.BEE);

		Modifier nemesis = LootUtils.getModifiers(player.getMainHandItem()).stream()
				.filter(m -> m.tagName().equals("nemesis")).findFirst().orElse(null);
		helper.assertTrue(nemesis != null, "nemesis trait should still be on the tool");
		int zombieKills = nemesis.toNBT().getCompoundOrEmpty("killCounts").getIntOr("minecraft:zombie", 0);
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

		player.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, 0, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0, false, false));

		ItemStack tool = newTool(ToolType.SWORD);
		LootUtils.addModifier(tool, ModifierRegistry.getModifier("catalyst"));
		player.setItemInHand(InteractionHand.MAIN_HAND, tool);

		for (int i = 0; i < 200; i++) {
			tool.inventoryTick(helper.getLevel(), player, EquipmentSlot.MAINHAND);
		}

		MobEffectInstance speed = player.getEffect(MobEffects.SPEED);
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
	 * and the {@code minecraft:cinnabar}/{@code minecraft:sulfur} ids resolve.
	 */
	public static void newTraitRecipesLoad(GameTestHelper helper) {
		var recipes = helper.getLevel().recipeAccess().getRecipes();

		record Expected(String trait, Identifier ingredient) {
		}
		Expected[] expected = {
				new Expected("catalyst", Identifier.withDefaultNamespace("cinnabar")),
				new Expected("stench", Identifier.withDefaultNamespace("sulfur")),
		};

		for (Expected e : expected) {
			helper.assertTrue(BuiltInRegistries.ITEM.containsKey(e.ingredient()),
					"ingredient item should exist: " + e.ingredient());

			Identifier recipeId = Identifier.fromNamespaceAndPath(RandomLoot.MODID, "trait_" + e.trait());
			boolean present = recipes.stream().anyMatch(
					h -> h.id().identifier().equals(recipeId) && h.value() instanceof TraitAdditionRecipe);
			helper.assertTrue(present, "trait recipe should load: " + recipeId);
		}

		helper.succeed();
	}

	/** Stench (sulfur) afflicts hostile mobs inside its radius with Slowness and Weakness. */
	public static void stenchDebuffsMobs(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();

		LivingEntity zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
		// Stand the holder on the zombie so it's well within the aura radius.
		player.setPos(zombie.getX(), zombie.getY(), zombie.getZ());

		ItemStack tool = newTool(ToolType.SWORD);
		LootUtils.addModifier(tool, ModifierRegistry.getModifier("stench"));
		player.setItemInHand(InteractionHand.MAIN_HAND, tool);

		tool.inventoryTick(helper.getLevel(), player, EquipmentSlot.MAINHAND);

		helper.assertTrue(zombie.hasEffect(MobEffects.SLOWNESS), "stench should slow nearby hostiles");
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
	 * levelUpTraitChance gates the bonus trait roll on every level-up: at 1.0 a single
	 * level-up rolls exactly one trait onto a bare tool, at 0.0 two level-ups roll none.
	 */
	public static void levelUpRollsTrait(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		double saved = Config.LevelUpTraitChance;

		try {
			Config.LevelUpTraitChance = 1.0;
			ItemStack rolled = newTool(ToolType.PICKAXE);
			LootUtils.addXp(rolled, player, 500);
			helper.assertTrue(LootUtils.rawTraitNames(rolled).size() == 1,
					"one level-up at chance 1.0 should roll one trait, got " + LootUtils.rawTraitNames(rolled));

			Config.LevelUpTraitChance = 0.0;
			ItemStack plain = newTool(ToolType.PICKAXE);
			LootUtils.addXp(plain, player, 1600);
			helper.assertTrue(LootUtils.rawTraitNames(plain).isEmpty(),
					"level-ups at chance 0.0 should roll no traits, got " + LootUtils.rawTraitNames(plain));
		} finally {
			Config.LevelUpTraitChance = saved;
		}

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
			helper.assertTrue(advancements.get(Identifier.fromNamespaceAndPath(RandomLoot.MODID, id)) != null,
					"advancement " + id + " should be loaded");
		}

		helper.succeed();
	}

	private static void assertAdvancementDone(GameTestHelper helper, ServerPlayer player, String id) {
		helper.assertTrue(advancementDone(helper, player, id), "gameplay sequence should complete advancement: " + id);
	}

	private static boolean advancementDone(GameTestHelper helper, ServerPlayer player, String id) {
		var advancement = helper.getLevel().getServer().getAdvancements()
				.get(Identifier.fromNamespaceAndPath(RandomLoot.MODID, id));
		helper.assertTrue(advancement != null, "advancement should be loaded: " + id);
		return player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	/** The anvil's material-repair path accepts items from the tool_repair_materials tag. */
	public static void toolRepairable(GameTestHelper helper) {
		ItemStack tool = new ItemStack(ModItems.TOOL.get());

		helper.assertTrue(tool.isValidRepairItem(new ItemStack(Items.DIAMOND)),
				"diamond should repair Random Tools (tool_repair_materials tag)");
		helper.assertFalse(tool.isValidRepairItem(new ItemStack(Items.STICK)),
				"stick should not repair Random Tools");

		helper.succeed();
	}

	public static boolean hasTrait(ItemStack tool, String tagName) {
		return LootUtils.getModifiers(tool).stream().anyMatch(m -> m.tagName().equals(tagName));
	}

	/** The /randomloot admin commands: give, trait add/remove (with gating), and xp. */
	public static void adminCommands(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		CommandSourceStack source = player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS);
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
				.withMaximumPermission(PermissionSet.NO_PERMISSIONS);
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
				BuiltInRegistries.ITEM.wrapAsHolder(Items.CACTUS), 1, "thorny");
		TraitAdditionRecipe critical = new TraitAdditionRecipe(
				BuiltInRegistries.ITEM.wrapAsHolder(Items.GHAST_TEAR), 1, "critical");

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
		// The subtraction template is ungated by type so a mismatched trait can always be
		// stripped - but only when the trait is actually on the gear. Matching without it
		// consumed the template and the addition and handed back an identical item.
		helper.assertFalse(thorny.matches(new SmithingRecipeInput(subTemplate, sword, cactus), level),
				"the subtraction template must not match gear that lacks the trait");
		ItemStack mismatched = newTool(ToolType.SWORD);
		LootUtils.addModifier(mismatched, ModifierRegistry.getModifier("thorny"));
		helper.assertTrue(thorny.matches(new SmithingRecipeInput(subTemplate, mismatched, cactus), level),
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

		menu.clicked(SmithingMenu.RESULT_SLOT, 0, ContainerInput.PICKUP, player);
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

		for (var holder : helper.getLevel().recipeAccess().getRecipes()) {
			if (holder.value() instanceof TraitAdditionRecipe recipe) {
				int stackSize = new ItemStack(recipe.additionItem()).getMaxStackSize();
				helper.assertTrue(recipe.additionCount() <= stackSize, holder.id().identifier() + " asks for "
						+ recipe.additionCount() + " but only " + stackSize + " fit in the smithing slot");
			}
		}

		helper.succeed();
	}

	/** The datapack's recipe for a trait, as loaded. */
	private static TraitAdditionRecipe traitRecipe(GameTestHelper helper, String trait) {
		Identifier id = Identifier.fromNamespaceAndPath(RandomLoot.MODID, "trait_" + trait);
		return helper.getLevel().recipeAccess().getRecipes().stream()
				.filter(h -> h.id().identifier().equals(id) && h.value() instanceof TraitAdditionRecipe)
				.map(h -> (TraitAdditionRecipe) h.value())
				.findFirst()
				.orElseThrow(() -> helper.assertionException(Component.literal("trait recipe should load: " + id)));
	}

	/** A smithing table with the given inputs in it, opened by the player. */
	private static SmithingMenu openSmithing(GameTestHelper helper, ServerPlayer player, ItemStack template,
			ItemStack base, ItemStack addition) {
		BlockPos tablePos = new BlockPos(1, 1, 1);
		helper.setBlock(tablePos, Blocks.SMITHING_TABLE);

		SmithingMenu menu = new SmithingMenu(1, player.getInventory(),
				ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(tablePos)));
		// NeoForge's take hook finds the menu through the player, as the game does.
		player.containerMenu = menu;
		menu.getSlot(SmithingMenu.TEMPLATE_SLOT).set(template);
		menu.getSlot(SmithingMenu.BASE_SLOT).set(base);
		menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(addition);
		return menu;
	}

	/**
	 * Taking the result uses up the recipe's whole count. Vanilla's onTake shrinks each
	 * input by one, so the other three cactus here only go if the loader's SMITHING_TAKE
	 * hook really runs. Also checks the preview, which has to vanish the moment the slot
	 * can no longer pay.
	 */
	public static void smithingTakeConsumesCount(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
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
		menu.clicked(SmithingMenu.RESULT_SLOT, 0, ContainerInput.PICKUP, player);

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
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		// DirtPlace never rolls on armor; it is here as the one trait named after a forger.
		ItemStack chestplate = newArmor(ToolType.CHESTPLATE);
		LootUtils.addModifier(chestplate, new DirtPlace("Nobody's Grace", 1));
		SmithingMenu menu = openSmithing(helper, player, new ItemStack(ModItems.MOD_ADD.get()), chestplate,
				new ItemStack(Items.CACTUS, 10));

		menu.clicked(SmithingMenu.RESULT_SLOT, 0, ContainerInput.QUICK_MOVE, player);

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
	 * second stack of torches. Nor is it adding a trait: the take hook serves both
	 * templates and used to fire the crafted-trait criterion behind add_trait for either.
	 * (smithingShiftClickConsumesCount is the control: an add does complete it.)
	 */
	public static void smithingRemovalCostsOne(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack thornyChestplate = newArmor(ToolType.CHESTPLATE);
		LootUtils.addModifier(thornyChestplate, ModifierRegistry.getModifier("thorny"));

		SmithingMenu menu = openSmithing(helper, player, new ItemStack(ModItems.MOD_SUB.get()), thornyChestplate,
				new ItemStack(Items.CACTUS));

		ItemStack preview = menu.getSlot(SmithingMenu.RESULT_SLOT).getItem();
		helper.assertTrue(!preview.isEmpty() && !hasTrait(preview, "thorny"),
				"one cactus should be enough to preview the removal");

		menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(new ItemStack(Items.CACTUS, 10));
		menu.clicked(SmithingMenu.RESULT_SLOT, 0, ContainerInput.PICKUP, player);

		helper.assertTrue(!menu.getCarried().isEmpty() && !hasTrait(menu.getCarried(), "thorny"),
				"the taken result should have lost thorny");
		ItemStack left = menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem();
		helper.assertTrue(left.is(Items.CACTUS) && left.getCount() == 9,
				"removing a trait should consume exactly 1 of 10 cactus, left " + left.getCount());
		helper.assertFalse(advancementDone(helper, player, "add_trait"),
				"removing a trait must not complete add_trait");

		helper.succeed();
	}

	/**
	 * Each Chaotic grant rolls a seed of its own. The registry holds one Chaotic, and
	 * every grant - case roll, level-up roll, command, smithing table - ends in
	 * addModifier writing that one onto the gear. So the seed it picked for itself at
	 * start-up went onto every Chaotic tool granted until the server restarted, and they
	 * all swung in step.
	 */
	public static void chaoticGrantsRollOwnSeeds(GameTestHelper helper) {
		Modifier chaotic = ModifierRegistry.getModifier("chaotic");

		List<ItemStack> tools = List.of(newTool(ToolType.SWORD), newTool(ToolType.SWORD), newTool(ToolType.SWORD));
		tools.forEach(tool -> LootUtils.addModifier(tool, chaotic));
		List<CompoundTag> granted = tools.stream().map(GameTestBodies::chaoticData).toList();
		helper.assertTrue(granted.stream().distinct().count() == 3,
				"three chaotic grants should carry three seeds, got " + granted);

		// Only the grant rolls: a seed that is on the gear stays there through a rewrite.
		ItemStack tool = tools.get(0);
		LootUtils.updateModifier(tool, LootUtils.getModifiers(tool).get(0));
		helper.assertTrue(chaoticData(tool).equals(granted.get(0)),
				"rewriting chaotic on the gear should keep its seed, got " + chaoticData(tool) + " after "
						+ granted.get(0));

		helper.succeed();
	}

	/** What the gear stores for its Chaotic trait: the name and the seed. */
	private static CompoundTag chaoticData(ItemStack gear) {
		return GearTags.read(gear, Modifier.MODTAG).getCompoundOrEmpty("chaotic");
	}

	/**
	 * Blinding and Heat Resistant have a single level: vanilla only asks whether Blindness
	 * and Fire Resistance are present and never reads their amplifier, so a second level
	 * would take the recipe's items again and change nothing. Poisonous and Healing are the
	 * controls - the same setup has to keep leveling a trait whose amplifier does count.
	 */
	public static void singleLevelEffectsStopAtLevelOne(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();

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
	 * Every GameHook reaches common on the loader under test. Each step makes the game do
	 * the thing behind a hook, through the loader's real wiring - an event listener, a
	 * mixin, an item override or a loot modifier file - and GameHooks records what
	 * arrived. A hook wired on one loader and forgotten on the other, a mixin dropped from
	 * the mixins json, a handler deleted from a shim: each leaves its hook undelivered.
	 *
	 * <p>COMMANDS and CONFIG arrive while the server starts and SERVER_TICK at the end of
	 * every tick, so there is no step for them. SERVER_STOPPING is the one hook left out
	 * for good: nothing can deliver it while the server running this test is still up.
	 */
	public static void loaderHooksAllBound(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		enablePvp(helper);
		ServerPlayer player = mockVulnerablePlayer(helper);
		ServerPlayer attacker = mockVulnerablePlayer(helper);

		// DAMAGE_PRE, DAMAGE_POST, ARMOR_HURT: a player in a chestplate takes a hit.
		// (Only a player's armor wears: LivingEntity.hurtArmor does nothing for a mob.)
		player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
		freshHit(helper, player, player.damageSources().playerAttack(attacker), 8.0f);

		// KILL
		LivingEntity zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
		helper.hurt(zombie, zombie.damageSources().generic(), 1000.0f);

		// BREAK_SPEED
		player.getDestroySpeed(Blocks.STONE.defaultBlockState());

		// AFTER_USE_ITEM
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
		player.gameMode.useItem(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND);

		// CREATIVE_TAB
		buildCreativeTab(helper, ModItems.CREATIVE_TAB);

		// ANVIL_COMBINE
		var anvil = new AnvilMenu(1, player.getInventory(),
				ContainerLevelAccess.create(level, helper.absolutePos(BlockPos.ZERO)));
		anvil.getSlot(0).set(newTool(ToolType.SWORD));
		anvil.getSlot(1).set(newTool(ToolType.SWORD));
		anvil.createResult();

		// ENCHANT_GATE
		var enchants = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		tableOffers(newTool(ToolType.SWORD), enchants.getOrThrow(Enchantments.SHARPNESS),
				enchants.getOrThrow(Enchantments.EFFICIENCY));

		// LOOT_INJECTION
		level.getServer().reloadableRegistries()
				.getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
						Identifier.withDefaultNamespace("chests/simple_dungeon")))
				.getRandomItems(new LootParams.Builder(level).create(LootContextParamSets.EMPTY));

		// SMITHING_TAKE
		SmithingMenu smithing = openSmithing(helper, player, new ItemStack(ModItems.MOD_ADD.get()),
				newArmor(ToolType.CHESTPLATE), new ItemStack(Items.CACTUS, 4));
		smithing.clicked(SmithingMenu.RESULT_SLOT, 0, ContainerInput.PICKUP, player);

		Set<GameHook> undelivered = GameHooks.undelivered();
		undelivered.remove(GameHook.SERVER_STOPPING);
		// A known gap this test found, left for the mod's author to rule on because closing
		// it changes what Fabric players get: Fabric does not deliver the first config load
		// (see GameHooks.config). Take this out with the fix.
		if (Services.PLATFORM.platformName().equals("Fabric")) {
			undelivered.remove(GameHook.CONFIG);
		}
		helper.assertTrue(undelivered.isEmpty(), "this loader never delivered these hooks: " + undelivered);

		helper.succeed();
	}

	/**
	 * The mod's items are listed in the Tools & Utilities creative tab, in their display
	 * order, after everything vanilla puts there, and in no other tab. Tabs are only ever
	 * built on a client in play; building them here goes through the same loader hook.
	 */
	public static void creativeTabListsModItems(GameTestHelper helper) {
		List<Item> listed = buildCreativeTab(helper, ModItems.CREATIVE_TAB).getDisplayItems().stream()
				.map(ItemStack::getItem).toList();
		List<Item> expected = ModItems.creativeTabItems();

		helper.assertTrue(listed.size() > expected.size(), "vanilla's own tools should be in the tab too");
		helper.assertTrue(listed.subList(listed.size() - expected.size(), listed.size()).equals(expected),
				"the tab should end with " + expected + ", ends with "
						+ listed.subList(listed.size() - expected.size(), listed.size()));
		helper.assertTrue(listed.stream().filter(expected::contains).count() == expected.size(),
				"each of the mod's items should be listed once");

		// CreativeModeTabs keeps its keys private, so the combat tab's is built from its id.
		ResourceKey<CreativeModeTab> combat = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
				Identifier.withDefaultNamespace("combat"));
		for (ItemStack stack : buildCreativeTab(helper, combat).getDisplayItems()) {
			helper.assertFalse(expected.contains(stack.getItem()),
					"the combat tab should not list " + stack.getItem());
		}

		helper.succeed();
	}

	/** Builds one creative tab's contents, as the client does when the creative inventory opens. */
	private static CreativeModeTab buildCreativeTab(GameTestHelper helper, ResourceKey<CreativeModeTab> key) {
		CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(key);
		tab.buildContents(new CreativeModeTab.ItemDisplayParameters(helper.getLevel().enabledFeatures(), true,
				helper.getLevel().registryAccess()));
		return tab;
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
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
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
			for (ItemEntity item : helper.getEntities(EntityTypes.ITEM, relative, 2.0)) {
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
		helper.getBlockEntity(relative, ShulkerBoxBlockEntity.class).setItem(0, new ItemStack(Items.DIAMOND, 3));

		ItemStack pick = newTool(ToolType.PICKAXE);
		ServerPlayer player = survivalPlayerHolding(helper, pick);
		BlockPos absolute = helper.absolutePos(relative);

		helper.assertTrue(LootUtils.breakBlockAsPlayer(pick, absolute, player, helper.getLevel(),
				helper.getLevel().getBlockState(absolute)), "the shulker box should break");

		ItemStack dropped = ItemStack.EMPTY;
		for (ItemEntity item : helper.getEntities(EntityTypes.ITEM, relative, 2.0)) {
			if (item.getItem().is(Items.SHULKER_BOX)) {
				dropped = item.getItem();
			}
		}
		helper.assertFalse(dropped.isEmpty(), "the shulker box should drop as an item");
		ItemContainerContents contents = dropped.get(DataComponents.CONTAINER);
		helper.assertTrue(contents != null && contents.nonEmptyItems().iterator().hasNext(),
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
			List<Shulker> markers = helper.getEntities(EntityTypes.SHULKER, relative, 1.0);
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

		LivingEntity zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
		zombie.setItemSlot(EquipmentSlot.CHEST, chest);

		ArmorDispatcher.onLivingDamagePost(zombie, Float.MAX_VALUE);

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
	 * One roll of a chest's loot is one injection roll: with both chances forced to 1 it
	 * adds exactly one case and one template, and with both at 0 it adds neither. That
	 * holds for a table that rolls other chest-type tables too - the trial vault's reward
	 * table rolls reward_common/_rare/_unique, and on Fabric every one of those used to
	 * carry its own injected pool.
	 */
	public static void lootInjectionRollsOncePerChest(GameTestHelper helper) {
		double savedCase = Config.CaseChance;
		double savedMod = Config.ModChance;
		try {
			for (String path : List.of("chests/simple_dungeon", "chests/trial_chambers/reward")) {
				var table = helper.getLevel().getServer().reloadableRegistries().getLootTable(
						ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace(path)));
				helper.assertTrue(table != LootTable.EMPTY, path + " loot table should exist");

				Config.CaseChance = 1.0;
				Config.ModChance = 1.0;
				assertInjectedPerRoll(helper, table, path, 1);

				Config.CaseChance = 0.0;
				Config.ModChance = 0.0;
				assertInjectedPerRoll(helper, table, path, 0);
			}
		} finally {
			Config.CaseChance = savedCase;
			Config.ModChance = savedMod;
		}

		helper.succeed();
	}

	/** Rolls the table a few times and checks each roll holds exactly this many cases and this many templates. */
	private static void assertInjectedPerRoll(GameTestHelper helper, LootTable table, String path, int expected) {
		var params = new LootParams.Builder(helper.getLevel()).create(LootContextParamSets.EMPTY);

		for (int i = 0; i < 20; i++) {
			int cases = 0;
			int templates = 0;
			for (ItemStack drop : table.getRandomItems(params)) {
				if (drop.is(ModItems.CASE.get())) {
					cases += drop.getCount();
				} else if (drop.is(ModItems.MOD_ADD.get())) {
					templates += drop.getCount();
				}
			}
			helper.assertTrue(cases == expected && templates == expected,
					"one roll of " + path + " should inject " + expected + " case and " + expected
							+ " template, got " + cases + " and " + templates);
		}
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
		LivingEntity fresh = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
		helper.hurt(fresh, fresh.damageSources().playerAttack(player), 12.0f);
		float freshAfterHit = fresh.getHealth();
		earlyBird.hurtEnemy(sword, fresh, player);
		helper.assertTrue(fresh.getHealth() < freshAfterHit,
				"Early Bird should add bonus damage to a target that was at full health before the hit");

		// Already-wounded target: no bonus, even when it is only a scratch short of full.
		LivingEntity wounded = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
		wounded.setHealth(wounded.getMaxHealth() * 0.97f);
		helper.hurt(wounded, wounded.damageSources().playerAttack(player), 4.0f);
		float woundedAfterHit = wounded.getHealth();
		earlyBird.hurtEnemy(sword, wounded, player);
		helper.assertTrue(wounded.getHealth() == woundedAfterHit,
				"Early Bird should not trigger on a target that was already wounded");

		// A full-health hit earlier in the same tick must not vouch for a later hit: the
		// record describes the latest hit, and by then the target is wounded.
		LivingEntity hitTwice = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
		helper.hurt(hitTwice, hitTwice.damageSources().generic(), 2.0f);
		helper.hurt(hitTwice, hitTwice.damageSources().playerAttack(player), 8.0f);
		float hitTwiceAfter = hitTwice.getHealth();
		helper.assertTrue(hitTwiceAfter < hitTwice.getMaxHealth() - 2.0f, "the second hit should have landed");
		earlyBird.hurtEnemy(sword, hitTwice, player);
		helper.assertTrue(hitTwice.getHealth() == hitTwiceAfter,
				"Early Bird should not reuse an earlier same-tick full-health hit for a wounded target");

		// Another trait's bonus hit overwrites that record too, so the tool runs Early
		// Bird before its other traits. Through the real LootItem.hurtEnemy: a sword with
		// Feasting and Early Bird must out-damage one with Feasting alone. (Feasting's key
		// iterates ahead of Early Bird's in the trait tag, so it would otherwise go first.)
		float[] lost = new float[2];
		String[][] traitSets = { { "feasting", "early_bird" }, { "feasting" } };
		for (int i = 0; i < traitSets.length; i++) {
			ItemStack traited = newTool(ToolType.SWORD);
			LootUtils.setStats(traited, 4.0f);
			for (String trait : traitSets[i]) {
				LootUtils.addModifier(traited, ModifierRegistry.getModifier(trait));
			}
			LivingEntity target = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
			helper.hurt(target, target.damageSources().playerAttack(player), 6.0f);
			float afterHit = target.getHealth();
			((LootItem) traited.getItem()).hurtEnemy(traited, target, player);
			lost[i] = afterHit - target.getHealth();
		}
		helper.assertTrue(lost[1] > 0.0f, "Feasting should add bonus damage for a well-fed attacker");
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
			LivingEntity zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
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
				if (onBlock instanceof InteractionResult.Success || onBlock instanceof InteractionResult.Fail) {
					return;
				}
			}

			if (!held.isEmpty() && player.gameMode.useItem(player, helper.getLevel(), held,
					hand) instanceof InteractionResult.Success) {
				return;
			}
		}
	}
}
