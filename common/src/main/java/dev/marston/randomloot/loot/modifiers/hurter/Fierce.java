package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.StatsModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class Fierce extends AbstractModifier implements EntityHurtModifier, StatsModifier {
	@Override
	public String description() {
		return "Deals more damage as durability decreases";
	}

	private float getDamageMultiplier(ItemStack itemstack) {
		float maxDamage = itemstack.getMaxDamage();
		if (maxDamage <= 0) return 1.0f;

		float currentDamage = itemstack.getDamageValue();
		float ratio = currentDamage / maxDamage;

		return 1.0f + (ratio * 0.5f);
	}

	@Override
	public void hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		float multiplier = getDamageMultiplier(itemstack);
		if (multiplier <= 1.0f) {
			return;
		}

		float baseDamage = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));
		dealBonusDamage(hurtee, hurter, baseDamage * (multiplier - 1.0f));
	}

	@Override
	public float getStats(ItemStack itemstack) {
		float maxDamage = itemstack.getMaxDamage();
		if (maxDamage <= 0) return 1.0f;

		float currentDamage = itemstack.getDamageValue();
		float ratio = currentDamage / maxDamage;

		return 1.0f + (ratio * 0.25f);
	}
}
