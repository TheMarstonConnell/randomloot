package dev.marston.randomloot.loot.modifiers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface UseModifier extends Modifier {
    /**
     * Called when the player right-clicks on a block with this tool.
     * 
     * @param player The player using the tool
     * @param world The world
     * @param pos The position of the block clicked
     * @param hand The hand holding the tool
     * @param facing The face of the block clicked
     * @param hitX Hit X position on block face
     * @param hitY Hit Y position on block face
     * @param hitZ Hit Z position on block face
     * @return The action result
     */
    public EnumActionResult use(EntityPlayer player, World world, BlockPos pos, EnumHand hand, 
                                 EnumFacing facing, float hitX, float hitY, float hitZ);

    /**
     * Called when the player right-clicks in the air with this tool.
     * 
     * @param world The world
     * @param player The player using the tool
     * @param hand The hand holding the tool
     * @return true if the action was handled, false otherwise
     */
    public boolean use(World world, EntityPlayer player, EnumHand hand);

    /**
     * Whether this modifier's use action works anywhere (in air), not just on blocks.
     */
    public boolean useAnywhere();
}
