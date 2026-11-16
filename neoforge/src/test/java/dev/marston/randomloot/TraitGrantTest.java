package dev.marston.randomloot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.NameGenerator;
import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.TraitEligibility.GearContext;
import dev.marston.randomloot.loot.TraitGrant;
import dev.marston.randomloot.loot.TraitGrant.Kind;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

/**
 * The stack-free form of a trait grant: the same decision and the same stored result,
 * on a description of gear and the tag of traits it will carry. The stack form is
 * covered by the trait_grant_* gametests, which also check the two store the same thing.
 */
class TraitGrantTest {

	private static final RandomSource RANDOM = RandomSource.create(1L);

	/** Gear of the given type carrying what {@code traits} holds. */
	private static GearContext gear(ToolType type, CompoundTag traits) {
		return GearContext.of(type, LootUtils.getModifiers(traits));
	}

	private static String nameOf(CompoundTag traits, String tag) {
		List<Modifier> stored = LootUtils.getModifiers(traits);
		return stored.stream().filter(trait -> trait.tagName().equals(tag)).map(Modifier::name).findFirst()
				.orElse("nothing");
	}

	@Test
	void addsThenLevelsThenRefusesAtTheCap() {
		Modifier thorny = ModifierRegistry.getModifier("thorny");
		CompoundTag traits = new CompoundTag();

		assertEquals(Kind.ADDED, TraitGrant.grant(gear(ToolType.CHESTPLATE, traits), traits, thorny, 1L, RANDOM));
		assertEquals("Thorny", nameOf(traits, "thorny"));

		assertEquals(Kind.LEVELED, TraitGrant.grant(gear(ToolType.CHESTPLATE, traits), traits, thorny, 1L, RANDOM));
		assertEquals("Thorny II", nameOf(traits, "thorny"));

		Kind last = Kind.LEVELED;
		CompoundTag before = traits.copy();
		for (int grants = 2; last == Kind.LEVELED && grants < 64; grants++) {
			before = traits.copy();
			last = TraitGrant.grant(gear(ToolType.CHESTPLATE, traits), traits, thorny, 1L, RANDOM);
		}
		assertEquals(Kind.REFUSED, last, "a capped trait is refused");
		assertEquals(before, traits, "a refused grant leaves the stored traits alone");
		assertEquals(1, traits.keySet().size(), "leveling never adds a second entry");
	}

	@Test
	void refusesWhatTheGateRefusesAndStoresNothing() {
		CompoundTag traits = new CompoundTag();

		assertEquals(Kind.REFUSED,
				TraitGrant.grant(gear(ToolType.SWORD, traits), traits, ModifierRegistry.getModifier("thorny"), 1L, RANDOM));
		assertTrue(traits.isEmpty(), "an armor trait must not be stored for a sword: " + traits);
	}

	@Test
	void aWorldDependentTraitGoesOnAsThatWorldsVariant() {
		Modifier grace = ModifierRegistry.getModifier("dirt_place");
		long seed = 7L;

		CompoundTag stamped = new CompoundTag();
		assertEquals(Kind.ADDED, TraitGrant.grant(gear(ToolType.SHOVEL, stamped), stamped, grace, seed, RANDOM));
		assertEquals(NameGenerator.forgerForWorld(seed, 0.5f) + "'s Grace", nameOf(stamped, "dirt_place"));

		CompoundTag unstamped = new CompoundTag();
		TraitGrant.grant(gear(ToolType.SHOVEL, unstamped), unstamped, grace, null, RANDOM);
		assertEquals("Grace", nameOf(unstamped, "dirt_place"),
				"with no world, the trait goes on as the registry holds it: naming no forger");
	}
}
