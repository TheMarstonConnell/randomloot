package dev.marston.randomloot.loot;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

/**
 * Gear roll: decides what a case produces, before any item exists.
 *
 * <p>Pure in the way {@link GearStats} is: it takes plain facts and a random source and
 * returns a description, with no {@code ItemStack} and no {@code Level}. The item is made
 * afterwards, in one step, by {@link LootUtils#stamp}. That split is what lets the armor
 * share, the trait picks and the texture range be checked from a JUnit test with a seed,
 * where the old procedure could only be asked whether it had produced something.
 *
 * <p>The config's numbers (goodness, the armor chance, the level-up chance) come in as
 * arguments; the callers in {@link LootUtils} read them. One thing is read from the config
 * on the way: which traits are switched on. That question is the gate's
 * ({@link TraitEligibility}), which answers it the same way for every caller, so with the
 * same toggles the same arguments decide the same gear.
 */
public final class GearRoll {

	private GearRoll() {
	}

	private static final ToolType[] TOOLS = { ToolType.PICKAXE, ToolType.AXE, ToolType.SHOVEL, ToolType.SWORD };
	private static final ToolType[] ARMOR = { ToolType.HELMET, ToolType.CHESTPLATE, ToolType.LEGGINGS,
			ToolType.BOOTS };

	/** Where the gear is made: the biome's registry key and base temperature, and the dimension's key. */
	public record Place(String biomeKey, float temperature, String dimension) {
	}

	/** Who opened the case. */
	public record Owner(String uuid, String name) {
	}

	/**
	 * Everything a roll reads that is not chance.
	 *
	 * @param owner null when a machine opened the case
	 */
	public record Scene(Place place, long worldSeed, boolean raining, @Nullable Owner owner) {
	}

	/**
	 * The gear a roll decided on: everything {@link LootUtils#stamp} writes.
	 *
	 * @param traits  what the gear stores for its traits, keyed by tag name, exactly as
	 *                {@link TraitGrant} leaves it on a stack
	 * @param texture the look's index within the type's range
	 */
	public record Rolled(ToolType type, float goodness, Place place, @Nullable Owner owner, CompoundTag traits,
			String name, String nameColor, String lore, int texture) {
	}

	/** Rolls the gear a case produces: armor with probability {@code armorChance}, else a tool. */
	public static Rolled roll(float goodness, Scene scene, double armorChance, RandomSource random) {
		ToolType[] kinds = random.nextFloat() < armorChance ? ARMOR : TOOLS;
		return ofType(kinds[random.nextInt(kinds.length)], goodness, scene, random);
	}

	/**
	 * Rolls gear of a set type, one of the eight: {@link GearStats#startingTraits} picks, a
	 * name, lore and a look. A pick of a trait the gear already has levels it, so the gear
	 * can end up with fewer traits than picks.
	 */
	public static Rolled ofType(ToolType type, float goodness, Scene scene, RandomSource random) {
		Place place = scene.place();

		CompoundTag traits = new CompoundTag();
		int picks = GearStats.startingTraits(goodness);
		for (int i = 0; i < picks; i++) {
			// The gear as it stands, so each pick is gated against the ones before it.
			TraitEligibility.GearContext gear = new TraitEligibility.GearContext(type, place.biomeKey(),
					place.temperature(), place.dimension(), LootUtils.getModifiers(traits), false, true);
			Modifier pick = pickTrait(gear, random);
			if (pick != null) {
				TraitGrant.grant(gear, traits, pick, scene.worldSeed(), random);
			}
		}

		String name = NameGenerator.generateNameWPrefix(random, place.temperature(), scene.raining());

		// Forgers are world-constant per temperature band, not rolled per item.
		String forger = NameGenerator.forgerForWorld(scene.worldSeed(), place.temperature());
		String opener = scene.owner() == null ? "a machine" : scene.owner().name();
		String lore = "Discovered by " + opener + " in " + readableBiome(place.biomeKey()) + ", forged by " + forger
				+ ".";

		int texture = random.nextInt(LootUtils.textureCount(type));

		return new Rolled(type, goodness, place, scene.owner(), traits, name, nameColor(place.dimension()), lore,
				texture);
	}

	/**
	 * Picks one trait the gear may take, each eligible one equally likely, or null when
	 * there is none. {@link TraitEligibility} is the only judge of eligible.
	 */
	@Nullable
	public static Modifier pickTrait(TraitEligibility.GearContext gear, RandomSource random) {
		List<Modifier> eligible = new ArrayList<>();
		for (Modifier trait : ModifierRegistry.getModifiers().values()) {
			if (TraitEligibility.check(gear, trait).allowed()) {
				eligible.add(trait);
			}
		}
		return eligible.isEmpty() ? null : eligible.get(random.nextInt(eligible.size()));
	}

	/**
	 * The trait a level-up brings: with probability {@code chance} one picked like any
	 * other, otherwise null. Also null when the gear can take no trait at all.
	 */
	@Nullable
	public static Modifier bonusTrait(TraitEligibility.GearContext gear, double chance, RandomSource random) {
		return random.nextDouble() < chance ? pickTrait(gear, random) : null;
	}

	private static String nameColor(String dimension) {
		return switch (dimension) {
		case "minecraft:the_nether" -> "#FF8C19";
		case "minecraft:the_end" -> "#C419FF";
		default -> "#50ab4b";
		};
	}

	private static String readableBiome(String biomeKey) {
		if (biomeKey == null || biomeKey.isEmpty() || biomeKey.equals("unknown")) {
			return "an Unknown Biome";
		}

		return "a " + LootUtils.titleCase(biomeKey);
	}
}
