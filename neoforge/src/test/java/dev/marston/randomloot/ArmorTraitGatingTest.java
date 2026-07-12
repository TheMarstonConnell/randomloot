package dev.marston.randomloot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.Unbreaking;
import dev.marston.randomloot.loot.modifiers.hurter.Feasting;
import dev.marston.randomloot.loot.modifiers.hurter.Fierce;
import dev.marston.randomloot.loot.modifiers.hurter.Fragile;
import dev.marston.randomloot.loot.modifiers.hurter.Munchies;
import dev.marston.randomloot.loot.modifiers.wearers.Adrenaline;
import dev.marston.randomloot.loot.modifiers.wearers.Bulwark;
import dev.marston.randomloot.loot.modifiers.wearers.Featherweight;
import dev.marston.randomloot.loot.modifiers.wearers.Magnetized;
import dev.marston.randomloot.loot.modifiers.wearers.Thorny;
import net.minecraft.world.entity.EquipmentSlot;
import org.junit.jupiter.api.Test;

/**
 * Guards the armor/tool boundary of the trait system: armor-only traits must never
 * land on hand tools (and vice versa), since both natural generation and the smithing
 * recipe rely on {@link Modifier#forTool}.
 */
class ArmorTraitGatingTest {

	@Test
	void armorTraitsNeverApplyToHandTools() {
		Modifier[] armorTraits = { new Thorny(), new Bulwark(), new Adrenaline(), new Magnetized(), new Featherweight() };

		for (Modifier trait : armorTraits) {
			for (ToolType tool : new ToolType[] { ToolType.PICKAXE, ToolType.AXE, ToolType.SHOVEL, ToolType.SWORD }) {
				assertFalse(trait.forTool(tool), trait.tagName() + " must not apply to " + tool);
			}
		}
	}

	@Test
	void generalArmorTraitsApplyToEveryPiece() {
		Modifier[] generalArmorTraits = { new Thorny(), new Bulwark(), new Adrenaline(), new Magnetized() };

		for (Modifier trait : generalArmorTraits) {
			for (ToolType piece : new ToolType[] { ToolType.HELMET, ToolType.CHESTPLATE, ToolType.LEGGINGS, ToolType.BOOTS }) {
				assertTrue(trait.forTool(piece), trait.tagName() + " should apply to " + piece);
			}
		}
	}

	@Test
	void featherweightIsBootsOnly() {
		Modifier featherweight = new Featherweight();

		assertTrue(featherweight.forTool(ToolType.BOOTS));
		assertFalse(featherweight.forTool(ToolType.HELMET));
		assertFalse(featherweight.forTool(ToolType.CHESTPLATE));
		assertFalse(featherweight.forTool(ToolType.LEGGINGS));
	}

	@Test
	void unbreakingAppliesToArmor() {
		assertTrue(new Unbreaking().forTool(ToolType.CHESTPLATE));
	}

	/**
	 * These traits' payoffs (melee damage scaling, hunger conversion, durability
	 * trade-offs) live in tool-only hooks, so on armor they'd be confusing no-ops
	 * whose descriptions talk about dealing damage.
	 */
	@Test
	void toolCombatTraitsStayOffArmor() {
		Modifier[] toolTraits = { new Fierce(), new Fragile(), new Munchies(), new Feasting() };

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
