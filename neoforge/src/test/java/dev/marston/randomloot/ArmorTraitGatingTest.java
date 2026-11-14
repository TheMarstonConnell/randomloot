package dev.marston.randomloot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import net.minecraft.world.entity.EquipmentSlot;
import org.junit.jupiter.api.Test;

/**
 * Guards the armor/tool boundary of the trait system: armor-only traits must never
 * land on hand tools (and vice versa), since both natural generation and the smithing
 * recipe rely on {@link Modifier#forTool}.
 */
class ArmorTraitGatingTest {

	/** A trait as gear carries it: a fresh instance, never the registry's own. */
	private static Modifier trait(String tag) {
		Modifier registered = ModifierRegistry.getModifier(tag);
		return registered.fromNBT(registered.toNBT());
	}

	@Test
	void armorTraitsNeverApplyToHandTools() {
		Modifier[] armorTraits = { trait("thorny"), trait("bulwark"), trait("adrenaline"), trait("magnetized"), trait("featherweight") };

		for (Modifier trait : armorTraits) {
			for (ToolType tool : new ToolType[] { ToolType.PICKAXE, ToolType.AXE, ToolType.SHOVEL, ToolType.SWORD }) {
				assertFalse(trait.forTool(tool), trait.tagName() + " must not apply to " + tool);
			}
		}
	}

	@Test
	void generalArmorTraitsApplyToEveryPiece() {
		Modifier[] generalArmorTraits = { trait("thorny"), trait("bulwark"), trait("adrenaline"), trait("magnetized") };

		for (Modifier trait : generalArmorTraits) {
			for (ToolType piece : new ToolType[] { ToolType.HELMET, ToolType.CHESTPLATE, ToolType.LEGGINGS, ToolType.BOOTS }) {
				assertTrue(trait.forTool(piece), trait.tagName() + " should apply to " + piece);
			}
		}
	}

	@Test
	void featherweightIsBootsOnly() {
		Modifier featherweight = trait("featherweight");

		assertTrue(featherweight.forTool(ToolType.BOOTS));
		assertFalse(featherweight.forTool(ToolType.HELMET));
		assertFalse(featherweight.forTool(ToolType.CHESTPLATE));
		assertFalse(featherweight.forTool(ToolType.LEGGINGS));
	}

	@Test
	void unbreakingAppliesToArmor() {
		assertTrue(trait("unbreaking").forTool(ToolType.CHESTPLATE));
	}

	/**
	 * These traits' payoffs (melee damage scaling, hunger conversion, durability
	 * trade-offs) live in tool-only hooks, so on armor they'd be confusing no-ops
	 * whose descriptions talk about dealing damage.
	 */
	@Test
	void toolCombatTraitsStayOffArmor() {
		Modifier[] toolTraits = { trait("fierce"), trait("fragile"), trait("munchies"), trait("feasting"), trait("chaotic") };

		for (Modifier trait : toolTraits) {
			for (ToolType piece : new ToolType[] { ToolType.HELMET, ToolType.CHESTPLATE, ToolType.LEGGINGS, ToolType.BOOTS }) {
				assertFalse(trait.forTool(piece), trait.tagName() + " must not apply to " + piece);
			}
			assertTrue(trait.forTool(ToolType.SWORD), trait.tagName() + " should still apply to swords");
		}
	}

	@Test
	void toolTypeArmorHelpers() {
		assertTrue(ToolType.HELMET.isArmor());
		assertFalse(ToolType.SWORD.isArmor());
		assertFalse(ToolType.NULL.isArmor());

		assertEquals(EquipmentSlot.HEAD, ToolType.HELMET.armorSlot());
		assertEquals(EquipmentSlot.CHEST, ToolType.CHESTPLATE.armorSlot());
		assertEquals(EquipmentSlot.LEGS, ToolType.LEGGINGS.armorSlot());
		assertEquals(EquipmentSlot.FEET, ToolType.BOOTS.armorSlot());
		assertNull(ToolType.PICKAXE.armorSlot());
	}
}
