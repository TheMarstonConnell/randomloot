package dev.marston.randomloot.loot.modifiers.stats;

import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.StatsModifier;
import net.minecraft.world.item.ItemStack;

public class Busted extends AbstractModifier implements StatsModifier {
	@Override
	public String description() {
		return "Dig speed is increased as tool durability drops.";
	}

	@Override
	public float getStats(ItemStack itemstack) {
		float maxDamage = itemstack.getMaxDamage();
		float currentDamage = itemstack.getDamageValue();

		float ratio = currentDamage / maxDamage;

		float maxGoodness = 1.0f;

		return ratio * maxGoodness + 1.0f;

	}
}
