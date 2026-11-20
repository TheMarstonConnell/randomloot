package dev.marston.randomloot.loot.modifiers.holders;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class TreasureFinder extends BlockHighlighter {
	@Override
	public String description() {
		return "While held or worn, spawners around you will glow.";
	}

	@Override
	protected boolean isTarget(BlockState state) {
		return state.is(Blocks.SPAWNER);
	}
}
