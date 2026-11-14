package dev.marston.randomloot.loot.modifiers;

import dev.marston.randomloot.loot.modifiers.breakers.*;
import dev.marston.randomloot.loot.modifiers.holders.*;
import dev.marston.randomloot.loot.modifiers.hurter.*;
import dev.marston.randomloot.loot.modifiers.hurter.Pummeling;
import dev.marston.randomloot.loot.modifiers.hurter.Soulbound;
import dev.marston.randomloot.loot.modifiers.stats.Busted;
import dev.marston.randomloot.loot.modifiers.hurter.Munchies;
import dev.marston.randomloot.loot.modifiers.wearers.Adrenaline;
import dev.marston.randomloot.loot.modifiers.wearers.Bulwark;
import dev.marston.randomloot.loot.modifiers.wearers.Featherweight;
import dev.marston.randomloot.loot.modifiers.wearers.Magnetized;
import dev.marston.randomloot.loot.modifiers.wearers.Thorny;
import dev.marston.randomloot.loot.modifiers.users.DirtPlace;
import dev.marston.randomloot.loot.modifiers.users.FireBall;
import dev.marston.randomloot.loot.modifiers.users.FirePlace;
import dev.marston.randomloot.loot.modifiers.users.VoidTouched;
import dev.marston.randomloot.loot.modifiers.users.TorchPlace;
import dev.marston.randomloot.loot.ToolType;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import static dev.marston.randomloot.loot.modifiers.TraitDeclaration.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Trait registry: the table of every trait's {@link TraitDeclaration}, and the one place
 * a trait instance is made.
 *
 * <p>It keeps one instance of each trait for itself - what {@link #getModifiers} and the
 * static fields hold - for anything that only asks about a trait: the gate, the wiki,
 * the config toggles. Gear never carries that instance. {@link #loadModifier}
 * builds a new one from the declaration for every trait read off gear, and a grant
 * stores a new one too ({@link Modifier#granted}).
 */
public class ModifierRegistry {

	private static final HashMap<String, Modifier> Modifiers = new HashMap<>();
	private static final HashMap<String, TraitDeclaration> Declarations = new HashMap<>();
	private static final HashMap<String, Boolean> ModifierEnabled = new HashMap<>();

	public static Map<String, Modifier> getModifiers() {
		return Collections.unmodifiableMap(Modifiers);
	}

	public static Map<String, Boolean> getModifierEnabled() {
		return Collections.unmodifiableMap(ModifierEnabled);
	}

	public static Modifier getModifier(String name) {
		return Modifiers.get(name);
	}

	public static Modifier EXPLODE = trait("explode", "Explosive", ChatFormatting.RED, MINING_TOOLS, Group.BREAKERS)
			.make(Explode::new);
	public static Modifier LEARNING = trait("learning", "Learning", ChatFormatting.GREEN, MINING_TOOLS, Group.BREAKERS)
			.make(Learning::new);
	public static Modifier ATTRACTING = trait("attracting", "Magnetic", ChatFormatting.RED, MINING_TOOLS, Group.BREAKERS)
			.make(Attracting::new);
	public static Modifier VEINY = trait("veiny", "Veiny", ChatFormatting.DARK_GREEN, MINING_TOOLS, Group.BREAKERS)
			.make(Veiny::new);
	public static Modifier MELTING = trait("melting", "Melting", ChatFormatting.RED, MINING_TOOLS, Group.BREAKERS)
			.make(Melting::new);
	public static Modifier EXCAVATOR = trait("excavator", "Excavator", ChatFormatting.GREEN, MINING_TOOLS, Group.BREAKERS)
			.make(Excavator::new);
	public static Modifier PROSPECTOR = trait("prospector", "Prospector", ChatFormatting.GOLD, only(ToolType.PICKAXE), Group.BREAKERS)
			.levels(1, 10).make(Prospector::new);
	public static Modifier LUMBERING = trait("lumbering", "Lumbering", ChatFormatting.DARK_GREEN, only(ToolType.AXE), Group.BREAKERS)
			.make(Lumbering::new);

	public static Modifier TORCH_PLACE = trait("torch_place", "Spelunking", ChatFormatting.YELLOW, HAND_HELD, Group.USERS)
			.make(TorchPlace::new);
	public static Modifier DIRT_PLACE = trait("dirt_place", "Grace", ChatFormatting.DARK_GREEN, HAND_HELD, Group.USERS)
			.make(DirtPlace::new);
	public static Modifier FIRE_PLACE = trait("fire_place", "Fire Starter", ChatFormatting.RED, HAND_HELD, Group.USERS)
			.make(FirePlace::new);
	public static Modifier FIRE_BALL = trait("flame_thrower", "Flame Thrower", ChatFormatting.DARK_RED, only(ToolType.SWORD), Group.USERS)
			.make(FireBall::new);

	public static Modifier FLAMING = trait("flaming", "Flaming", ChatFormatting.RED, WEAPONS, Group.HURTERS)
			.levels(ModifierConstants.POINTS, 2, 5).make(Fire::new);
	public static Modifier CRITICAL = trait("critical", "Critical", ChatFormatting.GOLD, WEAPONS, Group.HURTERS)
			.make(Critical::new);
	public static Modifier CHARGING = trait("charged", "Charged", ChatFormatting.YELLOW, WEAPONS, Group.HURTERS)
			.make(Charging::new);
	public static Modifier COMBO = trait("combo", "Dexterous", ChatFormatting.YELLOW, WEAPONS, Group.HURTERS)
			.make(Combo::new);
	public static Modifier DRAINING = trait("necrotic", "Necrotic", ChatFormatting.RED, WEAPONS, Group.HURTERS)
			.levels(ModifierConstants.POINTS, 2, 10).make(Draining::new);
	public static Modifier POISONOUS = hitEffect("poison", "Poisonous", ChatFormatting.DARK_GREEN, MobEffects.POISON, 5);
	public static Modifier WITHERING = hitEffect("wither", "Withering", ChatFormatting.DARK_GRAY, MobEffects.WITHER, 3);
	public static Modifier BLINDING = hitEffect("blinding", "Blinding", ChatFormatting.BLUE, MobEffects.BLINDNESS, 4);
	public static Modifier BEZERK = trait("bezerk", "Bezerk", ChatFormatting.GOLD, WEAPONS, Group.HURTERS)
			.make(Bezerk::new);
	public static Modifier NEMESIS = trait("nemesis", "Nemesis", ChatFormatting.DARK_RED, WEAPONS, Group.HURTERS)
			.levels(1, 3).make(Nemesis::new);
	public static Modifier SOULBOUND = trait("soulbound", "Soulbound", ChatFormatting.DARK_PURPLE, HAND_HELD, Group.HURTERS)
			.make(Soulbound::new);
	public static Modifier EXECUTIONER = trait("executioner", "Executioner", ChatFormatting.DARK_RED, WEAPONS, Group.HURTERS)
			.levels(1, 5).make(Executioner::new);
	public static Modifier CROWD_PLEASER = trait("crowd_pleaser", "Crowd Pleaser", ChatFormatting.LIGHT_PURPLE, WEAPONS, Group.HURTERS)
			.make(CrowdPleaser::new);
	public static Modifier PUMMELING = trait("pummeling", "Pummeling", ChatFormatting.DARK_RED, WEAPONS, Group.HURTERS)
			.make(Pummeling::new);
	public static Modifier HAILEYS_WRATH = trait("haileys_wrath", "Hailey's Wrath", ChatFormatting.GOLD, WEAPONS, Group.HURTERS)
			.make(HaileysWrath::new);
	public static Modifier EARLY_BIRD = trait("early_bird", "Early Bird", ChatFormatting.YELLOW, WEAPONS, Group.HURTERS)
			.levels(1, 3).make(EarlyBird::new);

	// Biome-restricted modifiers
	public static Modifier AQUATIC = trait("aquatic", "Aquatic", ChatFormatting.AQUA, MINING_TOOLS_AND_ARMOR, Group.HOLDERS)
			.levels(0, 2).make(Aquatic::new);
	public static Modifier SCORCHED = trait("scorched", "Scorched", ChatFormatting.GOLD, WEAPONS, Group.HURTERS)
			.levels(0, 2).make(Scorched::new);
	public static Modifier FROZEN = trait("frozen", "Frozen", ChatFormatting.AQUA, WEAPONS, Group.HURTERS).levels(0, 2)
			.make(Frozen::new);
	public static Modifier OVERGROWN = trait("overgrown", "Overgrown", ChatFormatting.GREEN, only(ToolType.SHOVEL, ToolType.AXE, ToolType.SWORD), Group.HURTERS)
			.levels(0, 2).make(Overgrown::new);
	public static Modifier VOID_TOUCHED = trait("void_touched", "Void-Touched", ChatFormatting.DARK_PURPLE, HAND_HELD, Group.USERS)
			.levels(0, 2).make(VoidTouched::new);

	public static Modifier HASTY = trait("hasty", "Hasty", ChatFormatting.BLUE, MINING_TOOLS_AND_ARMOR, Group.HOLDERS)
			.levels(0, 1).mirrorsLevelAs(ModifierConstants.POWER).make(Hasty::new);
	public static Modifier FILLING = heldEffect("filling", "Filling", ChatFormatting.GOLD, MobEffects.SATURATION, 2);
	public static Modifier ABSORBTION = heldEffect("absorption", "Appley", ChatFormatting.YELLOW,
			MobEffects.ABSORPTION, 10);
	public static Modifier REGENERATING = heldEffect("regeneration", "Healing", ChatFormatting.LIGHT_PURPLE,
			MobEffects.REGENERATION, 3);
	public static Modifier RESISTANT = heldEffect("resistance", "Resistant", ChatFormatting.GRAY,
			MobEffects.RESISTANCE, 1);
	public static Modifier FIRE_RESISTANT = heldEffect("fire_resistance", "Heat Resistant", ChatFormatting.GOLD,
			MobEffects.FIRE_RESISTANCE, 1);
	public static Modifier RAINY = trait("rainy", "Rainy", ChatFormatting.BLUE, MINING_TOOLS_AND_ARMOR, Group.HOLDERS)
			.make(Rainy::new);
	public static Modifier NATURALIST = trait("naturalist", "Naturalist", ChatFormatting.GREEN, ANY_GEAR, Group.HOLDERS)
			.levels(1, 3).make(Naturalist::new);
	public static Modifier ORE_FINDER = trait("detecting", "Detecting", ChatFormatting.WHITE, MINING_TOOLS_AND_ARMOR, Group.HOLDERS)
			.make(OreFinder::new);
	public static Modifier SPAWNER_FINDER = trait("spawner", "Tomb Raider", ChatFormatting.DARK_AQUA, ANY_GEAR, Group.HOLDERS)
			.make(TreasureFinder::new);
	public static Modifier LIVING = trait("living", "Living", ChatFormatting.GREEN, ANY_GEAR, Group.HOLDERS)
			.levels(1, 3).make(Healing::new);
	public static Modifier HUNTER = trait("hunter", "Hunter", ChatFormatting.DARK_RED, union(only(ToolType.SWORD), ARMOR), Group.HOLDERS)
			.make(Hunter::new);
	public static Modifier CHAOTIC = trait("chaotic", "Chaotic", ChatFormatting.DARK_PURPLE, HAND_HELD, Group.HURTERS, Group.STATS)
			.make(Chaotic::new);
	public static Modifier CATALYST = trait("catalyst", "Catalyst", ChatFormatting.RED, ANY_GEAR, Group.HOLDERS)
			.levels(1, 3).make(Catalyst::new);
	public static Modifier STENCH = trait("stench", "Stench", ChatFormatting.YELLOW, ANY_GEAR, Group.HOLDERS)
			.levels(1, 3).make(Stench::new);

	public static Modifier BUSTED = trait("busted", "Busted", ChatFormatting.LIGHT_PURPLE, MINING_TOOLS, Group.STATS)
			.make(Busted::new);
	public static Modifier FIERCE = trait("fierce", "Fierce", ChatFormatting.RED, HAND_HELD, Group.HURTERS, Group.STATS)
			.make(Fierce::new);
	public static Modifier MUNCHIES = trait("munchies", "Munchies", ChatFormatting.YELLOW, HAND_HELD, Group.BREAKERS, Group.HURTERS, Group.STATS)
			.levels(1, 5).make(Munchies::new);

	public static Modifier UNBREAKING = trait("unbreaking", "Unbreaking", ChatFormatting.AQUA, ANY_GEAR, Group.MISC)
			.levels(0, 4).make(Unbreaking::new);
	public static Modifier FEASTING = trait("feasting", "Feasting", ChatFormatting.GOLD, HAND_HELD, Group.HURTERS, Group.HOLDERS)
			.make(Feasting::new);

	public static Modifier FRAGILE = trait("fragile", "Fragile", ChatFormatting.WHITE, HAND_HELD, Group.BREAKERS,
			Group.HURTERS, Group.STATS).levels(1, 3).make(Fragile::new);
	public static Modifier CLUNKY = trait("clunky", "Clunky", ChatFormatting.DARK_GRAY, WEAPONS, Group.HURTERS, Group.HOLDERS)
			.make(Clunky::new);

	// Armor-only traits
	public static Modifier THORNY = trait("thorny", "Thorny", ChatFormatting.DARK_GREEN, ARMOR, Group.WEARERS)
			.levels(0, 4).make(Thorny::new);
	public static Modifier FEATHERWEIGHT = trait("featherweight", "Featherweight", ChatFormatting.WHITE, only(ToolType.BOOTS), Group.WEARERS)
			.levels(0, 3).make(Featherweight::new);
	public static Modifier BULWARK = trait("bulwark", "Bulwark", ChatFormatting.DARK_AQUA, ARMOR, Group.WEARERS)
			.levels(0, 4).make(Bulwark::new);
	public static Modifier ADRENALINE = trait("adrenaline", "Adrenaline", ChatFormatting.RED, ARMOR, Group.WEARERS)
			.levels(0, 4).make(Adrenaline::new);
	public static Modifier MAGNETIZED = trait("magnetized", "Magnetized", ChatFormatting.LIGHT_PURPLE, ARMOR, Group.WEARERS)
			.make(Magnetized::new);

	public static final Set<Modifier> BREAKERS = Set.of(EXPLODE, LEARNING, ATTRACTING, VEINY, MELTING, EXCAVATOR, PROSPECTOR, MUNCHIES, FRAGILE, LUMBERING);
	public static final Set<Modifier> USERS = Set.of(TORCH_PLACE, DIRT_PLACE, FIRE_PLACE, FIRE_BALL, VOID_TOUCHED);
	public static final Set<Modifier> HURTERS = Set.of(CRITICAL, CHARGING, FLAMING, COMBO, DRAINING, POISONOUS,
			WITHERING, BLINDING, BEZERK, NEMESIS, SOULBOUND, SCORCHED, FROZEN, OVERGROWN, FIERCE, FEASTING, EXECUTIONER, CROWD_PLEASER, PUMMELING, HAILEYS_WRATH, MUNCHIES, CHAOTIC, FRAGILE, CLUNKY, EARLY_BIRD);
	public static final Set<Modifier> HOLDERS = Set.of(HASTY, ABSORBTION, FILLING, RAINY, ORE_FINDER, SPAWNER_FINDER,
			LIVING, REGENERATING, RESISTANT, FIRE_RESISTANT, AQUATIC, HUNTER, FEASTING, NATURALIST, CLUNKY, CATALYST,
			STENCH);

	public static final Set<Modifier> STATS = Set.of(BUSTED, FIERCE, MUNCHIES, CHAOTIC, FRAGILE);

	public static final Set<Modifier> WEARERS = Set.of(THORNY, FEATHERWEIGHT, BULWARK, ADRENALINE, MAGNETIZED);

	public static final Set<Modifier> MISC = Set.of(UNBREAKING);

	public static Modifier register(Modifier modifier) {

		String tagName = modifier.tagName();

		if (Modifiers.containsKey(tagName)) {
			throw new IllegalStateException("Cannot register modifier twice: " + tagName);
		}

		Modifiers.put(tagName, modifier);
		ModifierEnabled.put(tagName, true);

		return modifier;
	}

	/** A trait that puts {@code effect} on whatever its weapon hits, for {@code seconds}. */
	private static Modifier hitEffect(String tag, String name, ChatFormatting color, Holder<MobEffect> effect,
			int seconds) {
		return effectTrait(tag, name, color, WEAPONS, Group.HURTERS, effect)
				.make(() -> new HurtEffect(effect, seconds));
	}

	/** A trait that keeps {@code effect} on whoever holds or wears the gear. */
	private static Modifier heldEffect(String tag, String name, ChatFormatting color, Holder<MobEffect> effect,
			int seconds) {
		return effectTrait(tag, name, color, MINING_TOOLS_AND_ARMOR, Group.HOLDERS, effect)
				.make(() -> new Effect(effect, seconds));
	}

	/**
	 * Effect traits store their level as "power", the effect's amplifier, and read back at
	 * the cap when old gear holds more than the effect now allows.
	 */
	private static TraitDeclaration effectTrait(String tag, String name, ChatFormatting color, Set<ToolType> fits,
			Group group, Holder<MobEffect> effect) {
		return trait(tag, name, color, fits, group).levels(ModifierConstants.POWER, 0, EffectModifier.maxPower(effect))
				.capsOnLoad();
	}

	/** Starts the declaration of a trait; see {@link TraitDeclaration}. */
	static TraitDeclaration trait(String tag, String name, ChatFormatting color, Set<ToolType> fits,
			TraitDeclaration.Group... groups) {
		return new TraitDeclaration(tag, name, color, fits, groups);
	}

	/** Registers a finished declaration and returns the registry's own instance of the trait. */
	static Modifier register(TraitDeclaration declaration) {
		Declarations.put(declaration.tag, declaration);
		return register(declaration.create());
	}

	/** A new instance of the named trait holding what {@code tag} stores, or null for a name nobody registered. */
	public static Modifier loadModifier(String name, CompoundTag tag) {
		TraitDeclaration declaration = Declarations.get(name);
		if (declaration != null) {
			AbstractModifier trait = declaration.create();
			trait.load(tag);
			return trait;
		}

		Modifier m = Modifiers.get(name);
		if (m == null) {
			return null;
		}

		return m.fromNBT(tag);
	}

}
