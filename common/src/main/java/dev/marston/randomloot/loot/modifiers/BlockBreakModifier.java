package dev.marston.randomloot.loot.modifiers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public interface BlockBreakModifier extends Modifier {
	/**
	 * Called from {@code LootItem.mineBlock} when the tool breaks a block.
	 *
	 * @param broken the state of the block being broken. Use this rather than reading
	 *               {@code pos} from the world: NeoForge calls {@code mineBlock} before
	 *               the block is removed, but vanilla/Fabric call it afterwards, so on
	 *               Fabric the world already holds air there.
	 * @return {@code true} to skip the tool's durability loss for this block
	 */
	public boolean startBreak(ItemStack itemstack, BlockState broken, BlockPos pos, LivingEntity player);

}
