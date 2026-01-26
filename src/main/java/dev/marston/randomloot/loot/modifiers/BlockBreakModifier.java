package dev.marston.randomloot.loot.modifiers;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

public interface BlockBreakModifier extends Modifier {
    /**
     * Called when a block is about to be broken with this tool.
     * 
     * @param itemstack The tool being used
     * @param pos The position of the block being broken
     * @param player The entity breaking the block
     * @return true to skip normal block breaking, false to continue
     */
    public boolean startBreak(ItemStack itemstack, BlockPos pos, EntityLivingBase player);
}
