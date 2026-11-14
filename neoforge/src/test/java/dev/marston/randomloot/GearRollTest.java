package dev.marston.randomloot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.marston.randomloot.loot.GearRoll;
import dev.marston.randomloot.loot.GearStats;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.NameGenerator;
import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.TraitEligibility;
import dev.marston.randomloot.loot.TraitEligibility.GearContext;
import dev.marston.randomloot.loot.TraitGrant;
import dev.marston.randomloot.loot.modifiers.BiomeRestrictedModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

/**
 * The gear roll: what a case produces, decided from plain facts and a random source.
 * It builds no ItemStack, so the things generation could never be asked about from a
 * gametest - the armor share, the trait picks, the texture range - are checked here with
 * a seed. Each test draws its rolls from one source, the way a level's random is used.
 */
class GearRollTest {

	private static final long WORLD = 4242L;
	private static final GearRoll.Place PLAINS = new GearRoll.Place("minecraft:plains", 0.8f, "minecraft:overworld");
	private static final GearRoll.Place THE_END = new GearRoll.Place("minecraft:the_end", 0.5f, "minecraft:the_end");
	private static final GearRoll.Owner TESTER = new GearRoll.Owner("11111111-2222-3333-4444-555555555555", "Tester");

	private static GearRoll.Scene scene(GearRoll.Place place) {
		return new GearRoll.Scene(place, WORLD, false, TESTER);
	}

	@Test
	void armorShareIsTheArmorChance() {
		RandomSource random = RandomSource.create(1L);
		int rolls = 4000;
		int armor = 0;
		for (int i = 0; i < rolls; i++) {
			if (GearRoll.roll(1.0f, scene(PLAINS), 0.15, random).type().isArmor()) {
				armor++;
			}
		}
		assertEquals(0.15, armor / (double) rolls, 0.03, "share of armor at an armor chance of 0.15");

		for (int i = 0; i < 200; i++) {
			assertFalse(GearRoll.roll(1.0f, scene(PLAINS), 0.0, random).type().isArmor(), "armor at chance 0");
			assertTrue(GearRoll.roll(1.0f, scene(PLAINS), 1.0, random).type().isArmor(), "a tool at chance 1");
		}
	}

	@Test
	void everyTypeIsReachedAndTheFourOfAKindAreEquallyLikely() {
		RandomSource random = RandomSource.create(2L);
		int rolls = 8000;
		Map<ToolType, Integer> seen = new EnumMap<>(ToolType.class);
		for (int i = 0; i < rolls; i++) {
			seen.merge(GearRoll.roll(1.0f, scene(PLAINS), 0.5, random).type(), 1, Integer::sum);
		}

		assertFalse(seen.containsKey(ToolType.NULL), "a roll never decides on no type");
		assertEquals(8, seen.size(), "all four tools and all four armor pieces should turn up: " + seen);
		// Half the rolls are armor and half tools, so each type is an eighth.
		for (Map.Entry<ToolType, Integer> type : seen.entrySet()) {
			assertEquals(0.125, type.getValue() / (double) rolls, 0.02, "share of " + type.getKey());
		}
	}

	@Test
	void goodnessEarnsExactlyItsStartingTraitPicks() {
		RandomSource random = RandomSource.create(3L);
		// 30 goodness is 15 picks, far more than armor has traits: picks repeat and level.
		for (float goodness : new float[] { 0.0f, 1.9f, 2.0f, 7.5f, 12.0f, 30.0f }) {
			for (ToolType type : new ToolType[] { ToolType.PICKAXE, ToolType.SWORD, ToolType.BOOTS }) {
				GearRoll.Rolled rolled = GearRoll.ofType(type, goodness, scene(PLAINS), random);

				int grants = 0;
				for (String tag : rolled.traits().keySet()) {
					grants += grantsBehind(rolled, tag);
				}
				assertEquals(GearStats.startingTraits(goodness), grants,
						type + " at goodness " + goodness + " rolled " + rolled.traits());
				assertEquals(goodness, rolled.goodness());
				assertEquals(type, rolled.type());
			}
		}
	}

	/**
	 * How many grants lead to what the roll stores for a trait, by granting it afresh
	 * until the two match. This is also the check that a roll stores exactly what
	 * {@link TraitGrant} would: a level-up stored any other way never matches.
	 */
	private static int grantsBehind(GearRoll.Rolled rolled, String tag) {
		CompoundTag wanted = withoutChaoticSeed(rolled.traits().getCompoundOrEmpty(tag));
		Modifier registered = ModifierRegistry.getModifier(tag);
		GearRoll.Place place = rolled.place();

		CompoundTag replay = new CompoundTag();
		for (int grants = 1; grants <= 64; grants++) {
			GearContext gear = new GearContext(rolled.type(), place.biomeKey(), place.temperature(),
					place.dimension(), LootUtils.getModifiers(replay), false, true);
			TraitGrant.grant(gear, replay, registered, WORLD, RandomSource.create(grants));
			if (withoutChaoticSeed(replay.getCompoundOrEmpty(tag)).equals(wanted)) {
				return grants;
			}
		}
		throw new AssertionError("no number of grants of " + tag + " stores " + wanted);
	}

	/**
	 * Chaotic's seed comes from the random source its grant was given, and the replay
	 * above has a source of its own, so the seed is left out of that comparison.
	 */
	private static CompoundTag withoutChaoticSeed(CompoundTag trait) {
		CompoundTag copy = trait.copy();
		copy.remove("chaoticSeed");
		return copy;
	}

	@Test
	void biomeTraitsOnlyRollWhereTheyBelong() {
		RandomSource random = RandomSource.create(4L);
		Map<GearRoll.Place, Integer> biomeTraitsSeen = new HashMap<>();

		for (GearRoll.Place place : List.of(PLAINS, THE_END)) {
			for (int i = 0; i < 600; i++) {
				GearRoll.Rolled rolled = GearRoll.roll(12.0f, scene(place), 0.15, random);
				for (Modifier trait : LootUtils.getModifiers(rolled.traits())) {
					if (trait instanceof BiomeRestrictedModifier restricted) {
						assertTrue(restricted.canSpawnInBiome(place.biomeKey(), place.temperature(),
								place.dimension()), trait.tagName() + " rolled in " + place);
						biomeTraitsSeen.merge(place, 1, Integer::sum);
					}
				}
			}
		}

		assertNull(biomeTraitsSeen.get(PLAINS), "plains is no biome trait's home");
		assertNotNull(biomeTraitsSeen.get(THE_END), "the End's own trait should turn up in the End");
	}

	@Test
	void theLookIsOneOfTheTypesOwn() {
		RandomSource random = RandomSource.create(5L);
		Map<ToolType, Integer> highest = new EnumMap<>(ToolType.class);
		for (int i = 0; i < 4000; i++) {
			GearRoll.Rolled rolled = GearRoll.roll(1.0f, scene(PLAINS), 0.5, random);
			int looks = LootUtils.textureCount(rolled.type());
			assertTrue(rolled.texture() >= 0 && rolled.texture() < looks,
					rolled.type() + " has " + looks + " looks but rolled " + rolled.texture());
			highest.merge(rolled.type(), rolled.texture(), Math::max);
		}
		// The whole range is in play, not a leading slice of it.
		for (Map.Entry<ToolType, Integer> type : highest.entrySet()) {
			assertEquals(LootUtils.textureCount(type.getKey()) - 1, type.getValue(), "last look of " + type.getKey());
		}
	}

	@Test
	void theSameSeedDecidesTheSameGear() {
		for (long seed = 0; seed < 50; seed++) {
			GearRoll.Rolled first = GearRoll.roll(12.0f, scene(PLAINS), 0.15, RandomSource.create(seed));
			GearRoll.Rolled again = GearRoll.roll(12.0f, scene(PLAINS), 0.15, RandomSource.create(seed));

			assertEquals(first.type(), again.type());
			assertEquals(first.name(), again.name());
			assertEquals(first.lore(), again.lore());
			assertEquals(first.texture(), again.texture());
			// Down to Chaotic's seed: a grant draws it from the roll's own random source.
			assertEquals(first.traits(), again.traits());
		}

		assertFalse(GearRoll.roll(12.0f, scene(PLAINS), 0.15, RandomSource.create(1L)).name()
				.equals(GearRoll.roll(12.0f, scene(PLAINS), 0.15, RandomSource.create(2L)).name()),
				"two seeds should not name their gear alike");
	}

	@Test
	void nameLoreAndOwnerComeFromTheScene() {
		RandomSource random = RandomSource.create(6L);
		String forger = NameGenerator.forgerForWorld(WORLD, PLAINS.temperature());

		GearRoll.Rolled opened = GearRoll.ofType(ToolType.AXE, 1.0f, scene(PLAINS), random);
		assertEquals("Discovered by Tester in a Plains, forged by " + forger + ".", opened.lore());
		assertEquals(TESTER, opened.owner());
		assertEquals(PLAINS, opened.place());
		assertEquals("#50ab4b", opened.nameColor());
		assertTrue(opened.name().contains(" "), "a name is an adjective and a made-up word: " + opened.name());

		GearRoll.Rolled dispensed = GearRoll.ofType(ToolType.AXE, 1.0f,
				new GearRoll.Scene(PLAINS, WORLD, false, null), random);
		assertEquals("Discovered by a machine in a Plains, forged by " + forger + ".", dispensed.lore());
		assertNull(dispensed.owner());

		GearRoll.Place nether = new GearRoll.Place("minecraft:nether_wastes", 2.0f, "minecraft:the_nether");
		assertEquals("#FF8C19", GearRoll.ofType(ToolType.AXE, 1.0f, scene(nether), random).nameColor());
		assertEquals("#C419FF", GearRoll.ofType(ToolType.AXE, 1.0f, scene(THE_END), random).nameColor());

		GearRoll.Place nowhere = new GearRoll.Place("unknown", 0.7f, "minecraft:overworld");
		assertTrue(GearRoll.ofType(ToolType.AXE, 1.0f, scene(nowhere), random).lore()
				.contains(" in an Unknown Biome, "));
	}

	@Test
	void graceGoesOnAsTheWorldsForger() {
		RandomSource random = RandomSource.create(7L);
		String expected = NameGenerator.forgerForWorld(WORLD, 0.5f) + "'s Grace";

		int found = 0;
		for (int i = 0; i < 400; i++) {
			GearRoll.Rolled rolled = GearRoll.ofType(ToolType.SHOVEL, 12.0f, scene(PLAINS), random);
			for (Modifier trait : LootUtils.getModifiers(rolled.traits())) {
				if (trait.tagName().equals("dirt_place")) {
					assertEquals(expected, trait.name());
					found++;
				}
			}
		}
		assertTrue(found > 0, "400 shovels with six picks each should include a Grace");
	}

	@Test
	void baseGearHasNoTraits() {
		GearRoll.Rolled base = GearRoll.ofType(ToolType.SWORD, 0.0f, scene(PLAINS), RandomSource.create(8L));

		assertEquals(ToolType.SWORD, base.type());
		assertEquals(0.0f, base.goodness());
		assertTrue(base.traits().isEmpty(), "goodness 0 earns no picks: " + base.traits());
	}

	@Test
	void aPickIsAlwaysEligibleAndEachEligibleTraitIsAsLikely() {
		RandomSource random = RandomSource.create(9L);
		GearContext sword = GearContext.of(ToolType.SWORD, List.of());
		long eligible = ModifierRegistry.getModifiers().values().stream()
				.filter(trait -> TraitEligibility.check(sword, trait).allowed()).count();

		int picks = 40000;
		Map<String, Integer> picked = new HashMap<>();
		for (int i = 0; i < picks; i++) {
			Modifier pick = GearRoll.pickTrait(sword, random);
			assertTrue(TraitEligibility.check(sword, pick).allowed(), pick.tagName() + " is not for a sword");
			picked.merge(pick.tagName(), 1, Integer::sum);
		}

		assertEquals(eligible, picked.size(), "every eligible trait should be picked at some point");
		for (Map.Entry<String, Integer> trait : picked.entrySet()) {
			assertEquals(1.0 / eligible, trait.getValue() / (double) picks, 0.006, "share of " + trait.getKey());
		}

		GearContext rolling = new GearContext(ToolType.SWORD, "", 0.7f, "minecraft:overworld", List.of(), true, true);
		assertNull(GearRoll.pickTrait(rolling, random), "gear that can take no trait gets no pick");
	}

	@Test
	void aLevelUpBringsABonusTraitAboutAsOftenAsItsChance() {
		RandomSource random = RandomSource.create(10L);
		GearContext sword = GearContext.of(ToolType.SWORD, List.of());

		int levelUps = 4000;
		int bonuses = 0;
		for (int i = 0; i < levelUps; i++) {
			if (GearRoll.bonusTrait(sword, 0.2, random) != null) {
				bonuses++;
			}
		}
		assertEquals(0.2, bonuses / (double) levelUps, 0.03, "share of level-ups with a bonus trait at 0.2");

		for (int i = 0; i < 200; i++) {
			assertNull(GearRoll.bonusTrait(sword, 0.0, random), "a bonus trait at chance 0");
			assertNotNull(GearRoll.bonusTrait(sword, 1.0, random), "no bonus trait at chance 1");
		}
	}
}
