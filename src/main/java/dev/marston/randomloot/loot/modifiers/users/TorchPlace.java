package dev.marston.randomloot.loot.modifiers.users;

import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;


public class TorchPlace extends PlaceOnUseModifier {

	public TorchPlace(String name, int damage) {
		this.name = name;
		this.damage = damage;
	}

	public TorchPlace() {
		this("Spelunking", 10);
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new TorchPlace(
			tag.getStringOr(NAME, "Spelunking"),
			tag.getIntOr(DAMAGE, 10)
		);
	}

	@Override
	public String tagName() {
		return "torch_place";
	}

	@Override
	public ChatFormatting color() {
		return ChatFormatting.YELLOW;
	}

	private boolean canPlace(LevelReader level, BlockState state, BlockPos pos) {
		return state.canSurvive(level, pos);
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

			if (torchState != null && canPlace(level, torchState, blockpos)) {
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
		BlockPlaceContext placeCtx = new BlockPlaceContext(ctx);

		if (!placeCtx.canPlace()) {
			return InteractionResult.FAIL;
		}

		BlockState blockstate = getPlacementState(placeCtx);
		if (blockstate == null) {
			return InteractionResult.FAIL;
		}

		Level level = placeCtx.getLevel();
		BlockPos blockpos = placeCtx.getClickedPos();

		if (!level.setBlock(blockpos, blockstate, 11)) {
			return InteractionResult.FAIL;
		}

		Player player = placeCtx.getPlayer();
		ItemStack itemstack = placeCtx.getItemInHand();
		BlockState placedState = level.getBlockState(blockpos);

		if (placedState.is(blockstate.getBlock())) {
			placedState.getBlock().setPlacedBy(level, blockpos, placedState, player, itemstack);
			if (player instanceof ServerPlayer serverPlayer) {
				CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, blockpos, itemstack);
			}
		}

		SoundType soundtype = placedState.getSoundType(level, blockpos, player);
		level.playSound(player, blockpos, soundtype.getPlaceSound(), SoundSource.BLOCKS,
				(soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
		level.gameEvent(GameEvent.BLOCK_PLACE, blockpos, GameEvent.Context.of(player, placedState));

		return InteractionResult.SUCCESS;
	}

	@Override
	public String description() {
		return "Right clicking on a block while crouching with the tool in hand will place a torch and use " + this.damage
				+ " durability points.";
	}
}
