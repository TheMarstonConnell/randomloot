package dev.marston.randomloot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import org.junit.jupiter.api.Test;

/**
 * Trait dispatch decides whether to save a trait by comparing what it would store before
 * and after its hook. That only works if a trait read back from gear stores the same
 * thing every time it is asked: one that did not would be written to the gear on every
 * tick it is held, which resets the block its holder is mining.
 */
class TraitStoredStateTest {

	@Test
	void aTraitReadBackFromGearStoresTheSameThingEveryTime() {
		for (Modifier registered : ModifierRegistry.getModifiers().values()) {
			Modifier onGear = registered.fromNBT(registered.toNBT());

			assertEquals(onGear.toNBT(), onGear.toNBT(),
					registered.tagName() + " stores something different each time it is asked");
			assertEquals(registered.tagName(), onGear.tagName(),
					registered.tagName() + " reads back as a different trait");
		}
	}
}
