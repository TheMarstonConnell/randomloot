package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class Soulbound extends AbstractModifier implements EntityHurtModifier {
	@Override
	public String description() {
		return "Grants 15% bonus damage and mining speed when wielded by the original owner.";
	}

	@Override
	public void hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		// Only apply bonus damage for swords and axes
		ToolType type = LootUtils.getToolType(itemstack);
		if (!type.equals(ToolType.SWORD) && !type.equals(ToolType.AXE)) {
			return;
		}

		// Check if hurter is the original owner
		if (!isOwner(itemstack, hurter)) {
			return;
		}

		// Apply 15% bonus damage
		dealBonusDamage(hurtee, hurter, attackDamage(itemstack) * 0.15f);
	}

	/**
	 * Check if the given entity is the original owner of the tool.
	 */
	public static boolean isOwner(ItemStack stack, LivingEntity entity) {
		return LootUtils.getOwnerUUID(stack).equals(entity.getStringUUID());
	}

	/**
	 * Mining speed bonus when the player is the original owner. Called from each
	 * loader's break-speed hook; returns the adjusted speed.
	 */
	public static float modifyBreakSpeed(Player player, float currentSpeed) {
		ItemStack stack = player.getMainHandItem();

		// This runs on every destroy-speed query while mining, so use the cheap
		// key-presence check instead of deserializing the whole modifier list.
		if (!stack.is(ModItems.TOOL.get()) || !LootUtils.hasEnabledModifier(stack, "soulbound")) {
			return currentSpeed;
		}

		// Check if player is the original owner
		if (!isOwner(stack, player)) {
			return currentSpeed;
		}

		// Apply 15% mining speed bonus
		return currentSpeed * 1.15f;
	}
}
