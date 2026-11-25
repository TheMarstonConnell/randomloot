package dev.marston.randomloot.loot.modifiers.users;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

public class TorchPlace extends PlaceOnUseModifier {

	public TorchPlace() {
		super(10);
	}

	private BlockState getPlacementState(BlockPlaceContext ctx) {
		// Try standing torch first, then wall torch based on placement direction
		LevelReader level = ctx.getLevel();
		BlockPos blockpos = ctx.getClickedPos();

		// Check each direction the player is looking at
		for (Direction direction : ctx.getNearestLookingDirections()) {
			BlockState torchState;
			if (direction == Direction.DOWN) {
				// Can't place torch pointing down
				continue;
			} else if (direction == Direction.UP) {
				// Standing torch
				torchState = Blocks.TORCH.getStateForPlacement(ctx);
			} else {
				// Wall torch - placed on the opposite face
				torchState = Blocks.WALL_TORCH.getStateForPlacement(ctx);
			}

			if (torchState != null && torchState.canSurvive(level, blockpos)) {
				// Check if the position is unobstructed
				if (level.isUnobstructed(torchState, blockpos, CollisionContext.empty())) {
					return torchState;
				}
			}
		}

		return null;
	}

	@Override
	protected InteractionResult place(UseOnContext ctx) {
		return placeBlock(ctx, this::getPlacementState);
	}

	@Override
	public String description() {
		return "Right clicking on a block while crouching with the tool in hand will place a torch and use " + this.damage.get()
				+ " durability points.";
	}
}
