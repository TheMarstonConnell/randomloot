package dev.marston.randomloot.loot.modifiers;

import net.minecraft.item.ItemStack;

public interface StatsModifier extends Modifier {
    /**
     * Get the stat modifier value for this modifier.
     * Positive values increase stats, negative values decrease them.
     * 
     * @param itemstack The tool
     * @return The stat modifier value
     */
    public float getStats(ItemStack itemstack);
}
