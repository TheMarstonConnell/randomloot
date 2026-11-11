package dev.marston.randomloot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.marston.randomloot.loot.GearStats;
import dev.marston.randomloot.loot.ToolType;
import org.junit.jupiter.api.Test;

/**
 * The gear stat formulas. These used to be reachable only from an in-world GameTest,
 * because reading goodness off an ItemStack needs a bound item registry; GearStats takes
 * the numbers directly, so the curves are checkable here.
 */
class GearStatsTest {

	private static final float EPSILON = 0.0001f;

	@Test
	void digSpeedRisesWithGoodnessButSwordsAlwaysDigLikeAHand() {
		assertEquals(6.0f, GearStats.digSpeed(0.0f, ToolType.PICKAXE, 1.0f), EPSILON);
		assertEquals(11.0f, GearStats.digSpeed(10.0f, ToolType.PICKAXE, 1.0f), EPSILON);

		assertEquals(1.0f, GearStats.digSpeed(0.0f, ToolType.SWORD, 1.0f), EPSILON);
		assertEquals(1.0f, GearStats.digSpeed(100.0f, ToolType.SWORD, 4.0f), EPSILON,
				"a sword's dig speed ignores both goodness and stat traits");
	}

	@Test
	void statTraitsMultiplyDigSpeedAndDefense() {
		assertEquals(12.0f, GearStats.digSpeed(0.0f, ToolType.PICKAXE, 2.0f), EPSILON);
		assertEquals(2.8f, GearStats.defense(0.0f, ToolType.CHESTPLATE, 2.0f), EPSILON);
	}

	@Test
	void attackDamageScalesPerToolType() {
		// Base is goodness + 1, then a per-type scale.
		assertEquals(11.0f, GearStats.attackDamage(10.0f, ToolType.SWORD), EPSILON);
		assertEquals(13.2f, GearStats.attackDamage(10.0f, ToolType.AXE), EPSILON);
		assertEquals(5.5f, GearStats.attackDamage(10.0f, ToolType.PICKAXE), EPSILON);
		assertEquals(6.6f, GearStats.attackDamage(10.0f, ToolType.SHOVEL), EPSILON);
	}

	@Test
	void attackSpeedIgnoresGoodness() {
		assertEquals(-2.4f, GearStats.attackSpeed(ToolType.SWORD), EPSILON);
		assertEquals(-3.0f, GearStats.attackSpeed(ToolType.AXE), EPSILON);
		assertEquals(0.0f, GearStats.attackSpeed(ToolType.HELMET), EPSILON);
	}

	@Test
	void defenseFollowsThePerPieceScale() {
		float goodness = 9.0f;

		assertEquals(14.0f, GearStats.defense(goodness, ToolType.CHESTPLATE, 1.0f), EPSILON);
		assertEquals(11.0f, GearStats.defense(goodness, ToolType.LEGGINGS, 1.0f), EPSILON);
		assertEquals(6.0f, GearStats.defense(goodness, ToolType.HELMET, 1.0f), EPSILON);
		assertEquals(6.0f, GearStats.defense(goodness, ToolType.BOOTS, 1.0f), EPSILON);

		assertEquals(0.0f, GearStats.defense(goodness, ToolType.SWORD, 1.0f), EPSILON,
				"hand tools have no armor value");
	}

	@Test
	void toughnessIsArmorOnly() {
		assertEquals(2.5f, GearStats.toughness(10.0f, ToolType.CHESTPLATE), EPSILON);
		assertEquals(0.0f, GearStats.toughness(10.0f, ToolType.PICKAXE), EPSILON);
	}

	@Test
	void durabilityWeightsArmorPiecesLikeVanilla() {
		assertEquals(800, GearStats.maxDamage(0.0f, ToolType.PICKAXE));
		assertEquals(1600, GearStats.maxDamage(10.0f, ToolType.SWORD));

		// (0 + 10) * unit * 3
		assertEquals(330, GearStats.maxDamage(0.0f, ToolType.HELMET));
		assertEquals(480, GearStats.maxDamage(0.0f, ToolType.CHESTPLATE));
		assertEquals(450, GearStats.maxDamage(0.0f, ToolType.LEGGINGS));
		assertEquals(390, GearStats.maxDamage(0.0f, ToolType.BOOTS));

		assertTrue(GearStats.maxDamage(5.0f, ToolType.CHESTPLATE) > GearStats.maxDamage(0.0f, ToolType.CHESTPLATE),
				"durability should rise with goodness");
	}

	@Test
	void xpCurveDoublesEachLevel() {
		assertEquals(500, GearStats.maxXp(0));
		assertEquals(1000, GearStats.maxXp(1));
		assertEquals(2000, GearStats.maxXp(2));
	}

	@Test
	void levellingAddsTenPercent() {
		assertEquals(11.0f, GearStats.leveledGoodness(10.0f), EPSILON);
	}

	@Test
	void goodnessFollowsASquareRootCurve() {
		assertEquals(1.0f, GearStats.goodnessForCases(0, 1.0), EPSILON);
		assertEquals(2.0f, GearStats.goodnessForCases(3, 1.0), EPSILON);
		assertEquals(4.0f, GearStats.goodnessForCases(15, 1.0), EPSILON);

		// The config rate scales the whole curve.
		assertEquals(4.0f, GearStats.goodnessForCases(3, 2.0), EPSILON);
	}

	@Test
	void startingTraitsIsHalfTheGoodnessRoundedDown() {
		assertEquals(0, GearStats.startingTraits(0.0f));
		assertEquals(0, GearStats.startingTraits(1.9f));
		assertEquals(1, GearStats.startingTraits(2.0f));
		assertEquals(3, GearStats.startingTraits(7.5f));
	}
}
