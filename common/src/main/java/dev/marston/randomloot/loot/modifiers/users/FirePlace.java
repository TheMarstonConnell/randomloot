package dev.marston.randomloot.loot.modifiers.users;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;

public class FirePlace extends PlaceOnUseModifier {

	public FirePlace() {
		super(2);
	}

	@Override
	protected InteractionResult place(UseOnContext ctx) {
		Player player = ctx.getPlayer();
		Level level = ctx.getLevel();
		BlockPos blockpos = ctx.getClickedPos();
		BlockState blockstate = level.getBlockState(blockpos);

		if (CampfireBlock.canLight(blockstate) || CandleBlock.canLight(blockstate)
				|| CandleCakeBlock.canLight(blockstate)) {
			level.playSound(player, blockpos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F,
					level.getRandom().nextFloat() * 0.4F + 0.8F);
			level.setBlock(blockpos, blockstate.setValue(BlockStateProperties.LIT, Boolean.valueOf(true)), 11);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, blockpos);
			return InteractionResult.SUCCESS;
		}

		BlockPos firePos = blockpos.relative(ctx.getClickedFace());
		if (!BaseFireBlock.canBePlacedAt(level, firePos, ctx.getHorizontalDirection())) {
			return InteractionResult.FAIL;
		}

		level.playSound(player, firePos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F,
				level.getRandom().nextFloat() * 0.4F + 0.8F);
		level.setBlock(firePos, BaseFireBlock.getState(level, firePos), 11);
		level.gameEvent(player, GameEvent.BLOCK_PLACE, blockpos);
		if (player instanceof ServerPlayer serverPlayer) {
			CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, firePos, ctx.getItemInHand());
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public String description() {
		return "Right clicking on the top of a block while crouching with the tool in hand will start a fire and use "
				+ this.damage.get() + " durability points.";
	}
}
