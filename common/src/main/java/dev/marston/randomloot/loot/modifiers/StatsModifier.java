package dev.marston.randomloot.loot.modifiers;

import net.minecraft.world.item.ItemStack;

public interface StatsModifier extends Modifier {
	public float getStats(ItemStack itemstack);

}
