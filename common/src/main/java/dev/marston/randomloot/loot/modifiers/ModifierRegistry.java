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

	public static Modifier EXPLODE = register(new Explode());
	public static Modifier LEARNING = register(new Learning());
	public static Modifier ATTRACTING = register(new Attracting());
	public static Modifier VEINY = register(new Veiny());
	public static Modifier MELTING = register(new Melting());
	public static Modifier EXCAVATOR = register(new Excavator());
	public static Modifier PROSPECTOR = register(new Prospector());
	public static Modifier LUMBERING = register(new Lumbering());

	public static Modifier TORCH_PLACE = register(new TorchPlace());
	public static Modifier DIRT_PLACE = register(new DirtPlace());
	public static Modifier FIRE_PLACE = register(new FirePlace());
	public static Modifier FIRE_BALL = register(new FireBall());

	public static Modifier FLAMING = register(new Fire());
	public static Modifier CRITICAL = trait("critical", "Critical", ChatFormatting.GOLD, WEAPONS, Group.HURTERS)
			.make(Critical::new);
	public static Modifier CHARGING = trait("charged", "Charged", ChatFormatting.YELLOW, WEAPONS, Group.HURTERS)
			.make(Charging::new);
	public static Modifier COMBO = register(new Combo());
	public static Modifier DRAINING = register(new Draining());
	public static Modifier POISONOUS = register(new HurtEffect("Poisonous", "poison", 5, MobEffects.POISON, ChatFormatting.DARK_GREEN));
	public static Modifier WITHERING = register(new HurtEffect("Withering", "wither", 3, MobEffects.WITHER, ChatFormatting.DARK_GRAY));
	public static Modifier BLINDING = register(new HurtEffect("Blinding", "blinding", 4, MobEffects.BLINDNESS, ChatFormatting.BLUE));
	public static Modifier BEZERK = register(new Bezerk());
	public static Modifier NEMESIS = trait("nemesis", "Nemesis", ChatFormatting.DARK_RED, WEAPONS, Group.HURTERS)
			.levels(1, 3).make(Nemesis::new);
	public static Modifier SOULBOUND = register(new Soulbound());
	public static Modifier EXECUTIONER = register(new Executioner());
	public static Modifier CROWD_PLEASER = register(new CrowdPleaser());
	public static Modifier PUMMELING = register(new Pummeling());
	public static Modifier HAILEYS_WRATH = register(new HaileysWrath());
	public static Modifier EARLY_BIRD = register(new EarlyBird());

	// Biome-restricted modifiers
	public static Modifier AQUATIC = register(new Aquatic());
	public static Modifier SCORCHED = register(new Scorched());
	public static Modifier FROZEN = register(new Frozen());
	public static Modifier OVERGROWN = register(new Overgrown());
	public static Modifier VOID_TOUCHED = register(new VoidTouched());

	public static Modifier HASTY = register(new Hasty());
	public static Modifier FILLING = register(new Effect("Filling", "filling", 2, MobEffects.SATURATION, ChatFormatting.GOLD));
	public static Modifier ABSORBTION = register(new Effect("Appley", "absorption", 10, MobEffects.ABSORPTION, ChatFormatting.YELLOW));
	public static Modifier REGENERATING = register(new Effect("Healing", "regeneration", 3, MobEffects.REGENERATION, ChatFormatting.LIGHT_PURPLE));
	public static Modifier RESISTANT = register(new Effect("Resistant", "resistance", 1, MobEffects.RESISTANCE, ChatFormatting.GRAY));
	public static Modifier FIRE_RESISTANT = register(
			new Effect("Heat Resistant", "fire_resistance", 1, MobEffects.FIRE_RESISTANCE, ChatFormatting.GOLD));
	public static Modifier RAINY = register(new Rainy());
	public static Modifier NATURALIST = register(new Naturalist());
	public static Modifier ORE_FINDER = register(new OreFinder());
	public static Modifier SPAWNER_FINDER = register(new TreasureFinder());
	public static Modifier LIVING = register(new Healing());
	public static Modifier HUNTER = register(new Hunter());
	public static Modifier CHAOTIC = register(new Chaotic());
	public static Modifier CATALYST = register(new Catalyst());
	public static Modifier STENCH = register(new Stench());

	public static Modifier BUSTED = register(new Busted());
	public static Modifier FIERCE = register(new Fierce());
	public static Modifier MUNCHIES = register(new Munchies());

	public static Modifier UNBREAKING = register(new Unbreaking());
	public static Modifier FEASTING = register(new Feasting());

	public static Modifier FRAGILE = trait("fragile", "Fragile", ChatFormatting.WHITE, HAND_HELD, Group.BREAKERS,
			Group.HURTERS, Group.STATS).levels(1, 3).make(Fragile::new);
	public static Modifier CLUNKY = register(new Clunky());

	// Armor-only traits
	public static Modifier THORNY = register(new Thorny());
	public static Modifier FEATHERWEIGHT = register(new Featherweight());
	public static Modifier BULWARK = register(new Bulwark());
	public static Modifier ADRENALINE = register(new Adrenaline());
	public static Modifier MAGNETIZED = register(new Magnetized());

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
