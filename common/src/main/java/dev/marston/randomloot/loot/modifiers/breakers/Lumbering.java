package dev.marston.randomloot.loot.modifiers.breakers;

import java.util.HashSet;
import java.util.Set;

import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class Lumbering extends AbstractModifier implements BlockBreakModifier {
	private static final int MAX_BLOCKS = 64;

	@Override
	public String description() {
		return "Breaking a log fells all connected logs";
	}

	private void findConnectedLogs(Level level, BlockPos pos, Set<BlockPos> found) {
		if (found.size() >= MAX_BLOCKS || found.contains(pos)) {
			return;
		}

		BlockState state = level.getBlockState(pos);
		if (!state.is(BlockTags.LOGS)) {
			return;
		}

		found.add(pos);

		for (BlockPos neighbor : treeNeighbors(pos)) {
			findConnectedLogs(level, neighbor, found);
		}
	}

	/** Upward and sideways neighbours, to follow a tree's structure without digging down. */
	private static BlockPos[] treeNeighbors(BlockPos pos) {
		return new BlockPos[] {
			pos.above(),
			pos.above().north(),
			pos.above().south(),
			pos.above().east(),
			pos.above().west(),
			pos.north(),
			pos.south(),
			pos.east(),
			pos.west(),
		};
	}

	@Override
	public void startBreak(ItemStack itemstack, BlockState broken, BlockPos pos, LivingEntity p) {
		if (!(p instanceof ServerPlayer player)) {
			return;
		}

		Level level = player.level();

		if (!broken.is(BlockTags.LOGS)) {
			return;
		}

		// Seed the search with the block the player broke instead of re-reading it:
		// it is still in the world on NeoForge but already gone on Fabric. It counts
		// toward MAX_BLOCKS either way and is broken by the normal path, not here.
		Set<BlockPos> logsToBreak = new HashSet<>();
		logsToBreak.add(pos);
		for (BlockPos neighbor : treeNeighbors(pos)) {
			findConnectedLogs(level, neighbor, logsToBreak);
		}
		logsToBreak.remove(pos);

		BlockBreakModifier.breakAlso(itemstack, logsToBreak, player, s -> s.is(BlockTags.LOGS));
	}
}
