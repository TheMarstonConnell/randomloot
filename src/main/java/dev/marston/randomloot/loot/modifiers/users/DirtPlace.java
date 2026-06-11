package dev.marston.randomloot.loot.modifiers.users;

import dev.marston.randomloot.loot.NameGenerator;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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

	public DirtPlace(String name, int damage) {
		this.name = name;
		this.damage = damage;
	}

	public DirtPlace() {
		this(NameGenerator.generateForger(RandomSource.create(), 0.5f) + "'s Grace", 1);
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new DirtPlace(
			tag.getStringOr(NAME, NameGenerator.generateForger(RandomSource.create(), 0.5f) + "'s Grace"),
			tag.getIntOr(DAMAGE, 1)
		);
	}

	@Override
	public Modifier forWorld(long worldSeed) {
		// 0.5f lands in the temperate band, matching the random roll this replaces.
		return new DirtPlace(NameGenerator.forgerForWorld(worldSeed, 0.5f) + "'s Grace", this.damage);
	}

	@Override
	public String tagName() {
		return "dirt_place";
	}

	@Override
	public boolean hasDynamicName() {
		// Each instance is named after a randomly generated forger ("<Forger>'s Grace"),
		// so there is no stable name to translate.
		return true;
	}

	@Override
	public String color() {
		return ChatFormatting.DARK_GREEN.getName();
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

		SoundType soundtype = placedState.getSoundType(level, blockpos, player);
		level.playSound(player, blockpos, soundtype.getPlaceSound(), SoundSource.BLOCKS,
				(soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
		level.gameEvent(GameEvent.BLOCK_PLACE, blockpos, GameEvent.Context.of(player, placedState));

		return InteractionResult.SUCCESS;
	}

	@Override
	public String description() {
		return "Right clicking on a block while crouching with the tool in hand will place a dirt block and use "
				+ this.damage + " durability points.";
	}
}
