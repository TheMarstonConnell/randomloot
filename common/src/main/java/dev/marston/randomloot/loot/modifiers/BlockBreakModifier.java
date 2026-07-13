package dev.marston.randomloot.loot.modifiers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface BlockBreakModifier extends Modifier {
	public boolean startBreak(ItemStack itemstack, BlockPos pos, LivingEntity player);

}
