package dev.marston.randomloot.loot.modifiers;

import dev.marston.randomloot.loot.modifiers.breakers.*;
import dev.marston.randomloot.loot.modifiers.holders.*;
import dev.marston.randomloot.loot.modifiers.hurter.*;
import dev.marston.randomloot.loot.modifiers.hurter.Soulbound;
import dev.marston.randomloot.loot.modifiers.stats.Busted;
import dev.marston.randomloot.loot.modifiers.users.DirtPlace;
import dev.marston.randomloot.loot.modifiers.users.FireBall;
import dev.marston.randomloot.loot.modifiers.users.FirePlace;
import dev.marston.randomloot.loot.modifiers.users.VoidTouched;
import dev.marston.randomloot.loot.modifiers.users.TorchPlace;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffects;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ModifierRegistry {

	private static final HashMap<String, Modifier> Modifiers = new HashMap<>();
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

	public static Modifier TORCH_PLACE = register(new TorchPlace());
	public static Modifier DIRT_PLACE = register(new DirtPlace());
	public static Modifier FIRE_PLACE = register(new FirePlace());
	public static Modifier FIRE_BALL = register(new FireBall());

	public static Modifier FLAMING = register(new Fire());
	public static Modifier CRITICAL = register(new Critical());
	public static Modifier CHARGING = register(new Charging());
	public static Modifier COMBO = register(new Combo());
	public static Modifier DRAINING = register(new Draining());
	public static Modifier POISONOUS = register(new HurtEffect("Poisonous", "poison", 5, MobEffects.POISON));
	public static Modifier WITHERING = register(new HurtEffect("Withering", "wither", 3, MobEffects.WITHER));
	public static Modifier BLINDING = register(new HurtEffect("Blinding", "blinding", 4, MobEffects.BLINDNESS));
	public static Modifier BEZERK = register(new Bezerk());
	public static Modifier NEMESIS = register(new Nemesis());
	public static Modifier SOULBOUND = register(new Soulbound());

	// Biome-restricted modifiers
	public static Modifier AQUATIC = register(new Aquatic());
	public static Modifier SCORCHED = register(new Scorched());
	public static Modifier FROZEN = register(new Frozen());
	public static Modifier OVERGROWN = register(new Overgrown());
	public static Modifier VOID_TOUCHED = register(new VoidTouched());

	public static Modifier HASTY = register(new Hasty());
	public static Modifier FILLING = register(new Effect("Filling", "filling", 2, MobEffects.SATURATION));
	public static Modifier ABSORBTION = register(new Effect("Appley", "absorption", 10, MobEffects.ABSORPTION));
	public static Modifier REGENERATING = register(new Effect("Healing", "regeneration", 3, MobEffects.REGENERATION));
	public static Modifier RESISTANT = register(new Effect("Resistant", "resistance", 1, MobEffects.RESISTANCE));
	public static Modifier FIRE_RESISTANT = register(
			new Effect("Heat Resistant", "fire_resistance", 1, MobEffects.FIRE_RESISTANCE));
	public static Modifier RAINY = register(new Rainy());
	public static Modifier ORE_FINDER = register(new OreFinder());
	public static Modifier SPAWNER_FINDER = register(new TreasureFinder());
	public static Modifier LIVING = register(new Healing());

	public static Modifier BUSTED = register(new Busted());

	public static Modifier UNBREAKING = register(new Unbreaking());

	public static final Set<Modifier> BREAKERS = Set.of(EXPLODE, LEARNING, ATTRACTING, VEINY, MELTING);
	public static final Set<Modifier> USERS = Set.of(TORCH_PLACE, DIRT_PLACE, FIRE_PLACE, FIRE_BALL, VOID_TOUCHED);
	public static final Set<Modifier> HURTERS = Set.of(CRITICAL, CHARGING, FLAMING, COMBO, DRAINING, POISONOUS,
			WITHERING, BLINDING, BEZERK, NEMESIS, SOULBOUND, SCORCHED, FROZEN, OVERGROWN);
	public static final Set<Modifier> HOLDERS = Set.of(HASTY, ABSORBTION, FILLING, RAINY, ORE_FINDER, SPAWNER_FINDER,
			LIVING, REGENERATING, RESISTANT, FIRE_RESISTANT, AQUATIC);

	public static final Set<Modifier> STATS = Set.of(BUSTED);

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

	public static Modifier loadModifier(String name, CompoundTag tag) {
		Modifier m = Modifiers.get(name);
		if (m == null) {
			return null;
		}

		return m.fromNBT(tag);
	}

}
