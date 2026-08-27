package dev.marston.randomloot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.marston.randomloot.loot.LootInjection;
import org.junit.jupiter.api.Test;

/**
 * Which loot tables the case/template injection targets. The policy is a pure
 * function of the table path, so it unit-tests without a server.
 */
class LootInjectionTableTest {

	@Test
	void injectsIntoChestLoot() {
		assertTrue(LootInjection.matchesTable("chests/simple_dungeon"));
		assertTrue(LootInjection.matchesTable("chests/abandoned_mineshaft"));
		assertTrue(LootInjection.matchesTable("chests/village/village_weaponsmith"));
	}

	/**
	 * The chest BLOCK's drop table is "blocks/chest", which contains the default
	 * "chest" match - so every chest broken used to roll a case and a template on
	 * top of the chest item, regardless of its contents or who placed it.
	 */
	@Test
	void skipsBlockDropTables() {
		assertFalse(LootInjection.matchesTable("blocks/chest"));
		assertFalse(LootInjection.matchesTable("blocks/trapped_chest"));
		assertFalse(LootInjection.matchesTable("blocks/ender_chest"));
	}

	@Test
	void skipsUnrelatedTables() {
		assertFalse(LootInjection.matchesTable("entities/zombie"));
		assertFalse(LootInjection.matchesTable("gameplay/fishing/treasure"));
	}
}
