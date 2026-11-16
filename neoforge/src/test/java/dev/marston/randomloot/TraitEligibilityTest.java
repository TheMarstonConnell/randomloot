package dev.marston.randomloot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.TraitEligibility;
import dev.marston.randomloot.loot.TraitEligibility.GearContext;
import dev.marston.randomloot.loot.TraitEligibility.Verdict;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import org.junit.jupiter.api.Test;

/**
 * The trait gate used by the case roll, {@code /randomloot trait add} and the smithing
 * table. It works off a {@link GearContext} rather than an {@code ItemStack}, so unlike
 * the rest of the gear logic it can be exercised without a running loader.
 */
class TraitEligibilityTest {

	/** A trait as gear carries it: a fresh instance, never the registry's own. */
	private static Modifier trait(String tag) {
		Modifier registered = ModifierRegistry.getModifier(tag);
		return registered.fromNBT(registered.toNBT());
	}

	private static GearContext gear(ToolType type, Modifier... existing) {
		return GearContext.of(type, List.of(existing));
	}

	@Test
	void allowsATraitThatFitsTheGear() {
		assertTrue(TraitEligibility.check(gear(ToolType.SWORD), trait("fierce")).allowed());
		assertTrue(TraitEligibility.check(gear(ToolType.CHESTPLATE), trait("thorny")).allowed());
	}

	@Test
	void rejectsTraitsThatDoNotFitTheGearType() {
		TraitEligibility.Result result = TraitEligibility.check(gear(ToolType.SWORD), trait("thorny"));

		assertFalse(result.allowed());
		assertEquals(Verdict.WRONG_TYPE, result.verdict());
	}

	@Test
	void rejectsBiomeRestrictedTraitsOutsideTheirBiome() {
		// Void-Touched is End-only; the default context is an overworld biome.
		TraitEligibility.Result overworld = TraitEligibility.check(gear(ToolType.PICKAXE), trait("void_touched"));
		assertEquals(Verdict.WRONG_BIOME, overworld.verdict());

		GearContext end = new GearContext(ToolType.PICKAXE, "minecraft:the_end", 0.5f, "minecraft:the_end",
				List.of(), false, true);
		assertTrue(TraitEligibility.check(end, trait("void_touched")).allowed());
	}

	@Test
	void rejectsIncompatibleTraitsAndNamesTheConflict() {
		Modifier veiny = trait("veiny");
		TraitEligibility.Result result = TraitEligibility.check(gear(ToolType.PICKAXE, veiny), trait("excavator"));

		assertEquals(Verdict.INCOMPATIBLE, result.verdict());
		assertNotNull(result.conflict());
		assertEquals(veiny.tagName(), result.conflict().tagName());
	}

	@Test
	void rejectsAnIncompatiblePairInEitherOrder() {
		// Lumbering names Veiny as a conflict; Veiny's own list only names Excavator.
		Modifier veiny = trait("veiny");
		Modifier lumbering = trait("lumbering");

		assertEquals(Verdict.INCOMPATIBLE,
				TraitEligibility.check(gear(ToolType.AXE, lumbering), veiny).verdict(),
				"a Lumbering axe should refuse Veiny");

		TraitEligibility.Result reverse = TraitEligibility.check(gear(ToolType.AXE, veiny), lumbering);
		assertEquals(Verdict.INCOMPATIBLE, reverse.verdict(), "a Veiny axe should refuse Lumbering");
		assertNotNull(reverse.conflict());
		assertEquals(veiny.tagName(), reverse.conflict().tagName());
	}

	@Test
	void rejectsARepeatOfATraitThatCannotLevel() {
		Modifier veiny = trait("veiny");
		assertFalse(veiny.canLevel(), "this test assumes Veiny does not level");

		TraitEligibility.Result result = TraitEligibility.check(gear(ToolType.PICKAXE, veiny), veiny);

		assertEquals(Verdict.ALREADY_MAXED, result.verdict());
	}

	@Test
	void allowsARepeatOfATraitThatStillLevels() {
		Modifier thorny = trait("thorny");
		assertTrue(thorny.canLevel(), "this test assumes a fresh Thorny can still level");

		assertTrue(TraitEligibility.check(gear(ToolType.CHESTPLATE, thorny), thorny).allowed());
	}

	@Test
	void rejectsGearThatIsStillRolling() {
		GearContext rolling = new GearContext(ToolType.SWORD, "", 0.7f, "minecraft:overworld", List.of(), true, true);

		assertEquals(Verdict.ROLLING, TraitEligibility.check(rolling, trait("fierce")).verdict());
	}

	@Test
	void everyRefusalExplainsItself() {
		Modifier thorny = trait("thorny");

		assertNull(TraitEligibility.check(gear(ToolType.CHESTPLATE), thorny).reason(thorny),
				"an allowed trait has no refusal to explain");
		assertNotNull(TraitEligibility.check(gear(ToolType.SWORD), thorny).reason(thorny));
	}
}
