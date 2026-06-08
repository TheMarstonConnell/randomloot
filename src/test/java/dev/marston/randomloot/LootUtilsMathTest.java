package dev.marston.randomloot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.marston.randomloot.loot.LootUtils;
import org.junit.jupiter.api.Test;

/** Pure-math helpers that back tool naming and leveling. */
class LootUtilsMathTest {

	@Test
	void romanNumeralsCoverEachBreakpoint() {
		assertEquals("I", LootUtils.roman(1));
		assertEquals("IV", LootUtils.roman(4));
		assertEquals("V", LootUtils.roman(5));
		assertEquals("IX", LootUtils.roman(9));
		assertEquals("XL", LootUtils.roman(40));
		assertEquals("XC", LootUtils.roman(90));
		assertEquals("CD", LootUtils.roman(400));
		assertEquals("CM", LootUtils.roman(900));
		assertEquals("MMXXIV", LootUtils.roman(2024));
		assertEquals("MMMCMXCIX", LootUtils.roman(3999));
	}

	@Test
	void romanRejectsOutOfRange() {
		assertEquals("Invalid Roman Number Value", LootUtils.roman(0));
		assertEquals("Invalid Roman Number Value", LootUtils.roman(4000));
	}

	@Test
	void maxXpFollowsDoublingCurve() {
		// getMaxXP(level) = 500 * 2^level
		assertEquals(500, LootUtils.getMaxXP(0));
		assertEquals(1000, LootUtils.getMaxXP(1));
		assertEquals(2000, LootUtils.getMaxXP(2));
		assertEquals(8000, LootUtils.getMaxXP(4));
	}
}
