package dev.marston.randomloot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static dev.marston.randomloot.loot.modifiers.ModifierRegistry.getModifier;

import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.world.entity.EquipmentSlot;
import org.junit.jupiter.api.Test;

/**
 * Guards the armor/tool boundary of the trait system: armor-only traits must never
 * land on hand tools (and vice versa), since both natural generation and the smithing
 * recipe rely on {@link Modifier#forTool}.
 */
class ArmorTraitGatingTest {

	@Test
	void generalArmorTraitsFitEveryPieceAndNoTool() {
		for (String tag : new String[] { "thorny", "bulwark", "adrenaline", "magnetized" }) {
			for (ToolType type : ToolType.values()) {
				if (type != ToolType.NULL) {
					assertEquals(type.isArmor(), getModifier(tag).forTool(type), tag + " on " + type);
				}
			}
		}
	}

	@Test
	void featherweightIsBootsOnly() {
		for (ToolType type : ToolType.values()) {
			if (type != ToolType.NULL) {
				assertEquals(type == ToolType.BOOTS, getModifier("featherweight").forTool(type), "featherweight on " + type);
			}
		}
	}

	@Test
	void unbreakingAppliesToArmor() {
		assertTrue(getModifier("unbreaking").forTool(ToolType.CHESTPLATE));
	}

	/**
	 * These traits' payoffs (melee damage scaling, hunger conversion, durability
	 * trade-offs) live in tool-only hooks, so on armor they'd be confusing no-ops
	 * whose descriptions talk about dealing damage.
	 */
	@Test
	void toolCombatTraitsStayOffArmor() {
		Modifier[] toolTraits = { getModifier("fierce"), getModifier("fragile"), getModifier("munchies"), getModifier("feasting"), getModifier("chaotic") };

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
