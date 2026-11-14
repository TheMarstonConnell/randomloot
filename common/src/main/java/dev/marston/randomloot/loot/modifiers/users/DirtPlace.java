package dev.marston.randomloot.loot.modifiers.users;

import dev.marston.randomloot.loot.NameGenerator;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;

public class DirtPlace extends PlaceOnUseModifier {

	public DirtPlace() {
		super(1);
	}

	/** Named after a forger rolled on the spot, when neither a world nor the gear says which. */
	@Override
	protected String defaultName() {
		return NameGenerator.generateForger(RandomSource.create(), 0.5f) + "'s Grace";
	}

	@Override
	public Modifier forWorld(long worldSeed) {
		DirtPlace forged = (DirtPlace) fromNBT(toNBT());
		// 0.5f lands in the temperate band, matching the random roll this replaces.
		forged.name = NameGenerator.forgerForWorld(worldSeed, 0.5f) + "'s Grace";
		return forged;
	}

	@Override
	public boolean hasDynamicName() {
		// Each instance is named after a randomly generated forger ("<Forger>'s Grace"),
		// so there is no stable name to translate.
		return true;
	}

	private boolean canPlace(BlockPlaceContext ctx, BlockState state) {
		Player player = ctx.getPlayer();
		CollisionContext collisionContext = player == null ? CollisionContext.empty() : CollisionContext.of(player);
		return state.canSurvive(ctx.getLevel(), ctx.getClickedPos())
				&& ctx.getLevel().isUnobstructed(state, ctx.getClickedPos(), collisionContext);
	}

	private BlockState getPlacementState(BlockPlaceContext ctx) {
		BlockState blockstate = Blocks.DIRT.getStateForPlacement(ctx);
		return blockstate != null && canPlace(ctx, blockstate) ? blockstate : null;
	}

	@Override
	protected InteractionResult place(UseOnContext useCtx) {
		BlockPlaceContext ctx = new BlockPlaceContext(useCtx);
		if (!ctx.canPlace()) {
			return InteractionResult.FAIL;
		}

		BlockState blockstate = getPlacementState(ctx);
		if (blockstate == null) {
			return InteractionResult.FAIL;
		}

		if (!ctx.getLevel().setBlock(ctx.getClickedPos(), blockstate, 11)) {
			return InteractionResult.FAIL;
		}

		BlockPos blockpos = ctx.getClickedPos();
		Level level = ctx.getLevel();
		Player player = ctx.getPlayer();
		ItemStack itemstack = ctx.getItemInHand();
		BlockState placedState = level.getBlockState(blockpos);

		if (placedState.is(blockstate.getBlock())) {
			placedState.getBlock().setPlacedBy(level, blockpos, placedState, player, itemstack);
			if (player instanceof ServerPlayer serverPlayer) {
				CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, blockpos, itemstack);
			}
		}

		SoundType soundtype = placedState.getSoundType();
		level.playSound(player, blockpos, soundtype.getPlaceSound(), SoundSource.BLOCKS,
				(soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
		level.gameEvent(GameEvent.BLOCK_PLACE, blockpos, GameEvent.Context.of(player, placedState));

		return InteractionResult.SUCCESS;
	}

	@Override
	public String description() {
		return "Right clicking on a block while crouching with the tool in hand will place a dirt block and use "
				+ this.damage.get() + " durability points.";
	}
}
