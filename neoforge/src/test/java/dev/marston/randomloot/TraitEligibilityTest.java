package dev.marston.randomloot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.TraitEligibility;
import dev.marston.randomloot.loot.TraitEligibility.GearContext;
import dev.marston.randomloot.loot.TraitEligibility.Verdict;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.breakers.Excavator;
import dev.marston.randomloot.loot.modifiers.breakers.Veiny;
import dev.marston.randomloot.loot.modifiers.hurter.Fierce;
import dev.marston.randomloot.loot.modifiers.users.VoidTouched;
import dev.marston.randomloot.loot.modifiers.wearers.Thorny;
import org.junit.jupiter.api.Test;

/**
 * The trait gate used by the case roll, {@code /randomloot trait add} and the smithing
 * table. It works off a {@link GearContext} rather than an {@code ItemStack}, so unlike
 * the rest of the gear logic it can be exercised without a running loader.
 */
class TraitEligibilityTest {

	private static GearContext gear(ToolType type, Modifier... existing) {
		return GearContext.of(type, List.of(existing));
	}

	@Test
	void allowsATraitThatFitsTheGear() {
		assertTrue(TraitEligibility.check(gear(ToolType.SWORD), new Fierce()).allowed());
		assertTrue(TraitEligibility.check(gear(ToolType.CHESTPLATE), new Thorny()).allowed());
	}

	@Test
	void rejectsTraitsThatDoNotFitTheGearType() {
		TraitEligibility.Result result = TraitEligibility.check(gear(ToolType.SWORD), new Thorny());

		assertFalse(result.allowed());
		assertEquals(Verdict.WRONG_TYPE, result.verdict());
	}

	@Test
	void rejectsBiomeRestrictedTraitsOutsideTheirBiome() {
		// Void-Touched is End-only; the default context is an overworld biome.
		TraitEligibility.Result overworld = TraitEligibility.check(gear(ToolType.PICKAXE), new VoidTouched());
		assertEquals(Verdict.WRONG_BIOME, overworld.verdict());

		GearContext end = new GearContext(ToolType.PICKAXE, "minecraft:the_end", 0.5f, "minecraft:the_end",
				List.of(), false, true);
		assertTrue(TraitEligibility.check(end, new VoidTouched()).allowed());
	}

	@Test
	void rejectsIncompatibleTraitsAndNamesTheConflict() {
		Modifier veiny = new Veiny();
		TraitEligibility.Result result = TraitEligibility.check(gear(ToolType.PICKAXE, veiny), new Excavator());

		assertEquals(Verdict.INCOMPATIBLE, result.verdict());
		assertNotNull(result.conflict());
		assertEquals(veiny.tagName(), result.conflict().tagName());
	}

	@Test
	void rejectsARepeatOfATraitThatCannotLevel() {
		Modifier veiny = new Veiny();
		assertFalse(veiny.canLevel(), "this test assumes Veiny does not level");

		TraitEligibility.Result result = TraitEligibility.check(gear(ToolType.PICKAXE, veiny), veiny);

		assertEquals(Verdict.ALREADY_MAXED, result.verdict());
	}

	@Test
	void allowsARepeatOfATraitThatStillLevels() {
		Modifier thorny = new Thorny();
		assertTrue(thorny.canLevel(), "this test assumes a fresh Thorny can still level");

		assertTrue(TraitEligibility.check(gear(ToolType.CHESTPLATE, thorny), thorny).allowed());
	}

	@Test
	void rejectsGearThatIsStillRolling() {
		GearContext rolling = new GearContext(ToolType.SWORD, "", 0.7f, "minecraft:overworld", List.of(), true, true);

		assertEquals(Verdict.ROLLING, TraitEligibility.check(rolling, new Fierce()).verdict());
	}

	@Test
	void everyRefusalExplainsItself() {
		Modifier thorny = new Thorny();

		assertNull(TraitEligibility.check(gear(ToolType.CHESTPLATE), thorny).reason(thorny),
				"an allowed trait has no refusal to explain");
		assertNotNull(TraitEligibility.check(gear(ToolType.SWORD), thorny).reason(thorny));
	}
}
