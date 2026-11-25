package dev.marston.randomloot.loot.modifiers.users;

import java.util.function.Function;

import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.Stored;
import dev.marston.randomloot.loot.modifiers.UseModifier;
import dev.marston.randomloot.loot.modifiers.TraitDispatch;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Base for right-click traits that place or ignite a block while crouching and charge
 * durability on success (DirtPlace, TorchPlace, FirePlace). Owns the crouch gate, the
 * durability cost and its {@code DAMAGE} NBT field; subclasses implement
 * {@link #place(UseOnContext)}.
 */
public abstract class PlaceOnUseModifier extends AbstractModifier implements UseModifier {
	protected static final String DAMAGE = "DAMAGE";

	/** Durability a placement costs. Stored, so gear keeps the cost it was made with. */
	protected final Stored<Integer> damage;

	/** @param durabilityCost what a placement costs on freshly granted gear */
	protected PlaceOnUseModifier(int durabilityCost) {
		this.damage = stored(DAMAGE, durabilityCost);
	}

	/** Attempts the placement; durability is only charged on SUCCESS. */
	protected abstract InteractionResult place(UseOnContext ctx);

	/**
	 * Places a block the way a block item does: the state {@code stateFor} picks for the
	 * placement (null when it cannot go there), then the placed-by call, the
	 * {@code placed_block} criterion, the sound and the game event.
	 */
	protected static InteractionResult placeBlock(UseOnContext useCtx, Function<BlockPlaceContext, BlockState> stateFor) {
		BlockPlaceContext ctx = new BlockPlaceContext(useCtx);
		BlockPos blockpos = ctx.getClickedPos();
		Level level = ctx.getLevel();
		BlockState blockstate = ctx.canPlace() ? stateFor.apply(ctx) : null;
		if (blockstate == null || !level.setBlock(blockpos, blockstate, 11)) {
			return InteractionResult.FAIL;
		}

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
	public InteractionResult use(UseOnContext ctx) {
		if (!ctx.getPlayer().isCrouching()) {
			return InteractionResult.PASS; // Allow normal tool behaviors when not crouching
		}

		InteractionResult result = place(ctx);

		if (result == InteractionResult.SUCCESS) {
			TraitDispatch.spendDurability(ctx.getItemInHand(), this.damage.get(), ctx.getPlayer(), EquipmentSlot.MAINHAND);
		}

		return result;
	}
}
