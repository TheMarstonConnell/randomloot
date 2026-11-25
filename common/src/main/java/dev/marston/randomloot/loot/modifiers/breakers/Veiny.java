package dev.marston.randomloot.loot.modifiers.breakers;

import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.Stored;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

public class Veiny extends AbstractModifier implements BlockBreakModifier {
	private final static String POWER = "power";

	/** How far the vein is followed. Stored, so gear keeps the reach it was made with. */
	private final Stored<Float> power = stored(POWER, 5.0f);

	private static final Direction[] SPREAD = {Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST,
			Direction.NORTH, Direction.SOUTH};

	private void checkAndBreak(BlockPos pos, Level level, int index, Block blockType, Set<BlockPos> tobreak,
			Set<BlockPos> visited) {

		if (index > power.get()) {
			return;
		}

		// Guard against revisiting positions: without it the 6-way recursion
		// re-expands the same cells exponentially. The origin is tracked here even
		// though it is deliberately never added to tobreak.
		if (!visited.add(pos.immutable())) {
			return;
		}

		// The origin (index 0) is the block the player broke: it is still in the world
		// on NeoForge but already gone on Fabric, so only the spread is checked.
		if (index > 0) {
			if (!level.getBlockState(pos).is(blockType)) {
				return;
			}

			tobreak.add(pos.immutable());
		}

		int dex = index + 1;

		for (Direction d : SPREAD) {
			checkAndBreak(pos.relative(d), level, dex, blockType, tobreak, visited);
		}
	}

	@Override
	public void startBreak(ItemStack itemstack, BlockState broken, BlockPos pos, LivingEntity p) {
		if (!(p instanceof ServerPlayer player) || !player.isCrouching()) {
			return;
		}

		if (!itemstack.isCorrectToolForDrops(broken)) {
			return;
		}

		Set<BlockPos> toBreak = new HashSet<BlockPos>();
		checkAndBreak(pos, player.level(), 0, broken.getBlock(), toBreak, new HashSet<BlockPos>());

		BlockBreakModifier.breakAlso(itemstack, toBreak, player, s -> true);
	}

	@Override
	public String description() {
		return "Breaking any block while crouching will cause all blocks of the same type adjacent to it to break up to "
				+ power.get().intValue() + " in each direction.";
	}
}
