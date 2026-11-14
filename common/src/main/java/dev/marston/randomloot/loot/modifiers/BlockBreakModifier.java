package dev.marston.randomloot.loot.modifiers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public interface BlockBreakModifier extends Modifier {
	/**
	 * Called by {@link TraitDispatch#blockBroken} when the tool breaks a block, on the
	 * server only. The block's durability is the dispatch's to spend, and so is saving
	 * this trait if the hook changed what it stores.
	 *
	 * @param broken the state of the block being broken. Use this rather than reading
	 *               {@code pos} from the world: NeoForge calls {@code mineBlock} before
	 *               the block is removed, but vanilla/Fabric call it afterwards, so on
	 *               Fabric the world already holds air there.
	 */
	public void startBreak(ItemStack itemstack, BlockState broken, BlockPos pos, LivingEntity player);

}
