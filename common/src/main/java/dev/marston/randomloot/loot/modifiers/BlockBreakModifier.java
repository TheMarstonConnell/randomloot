package dev.marston.randomloot.loot.modifiers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public interface BlockBreakModifier extends Modifier {
	/**
	 * Called by {@link TraitDispatch#blockBroken} when the tool breaks a block, on the
	 * server only. The block's durability is the dispatch's to spend, and so is saving
	 * this trait if the hook changed what it stores.
	 *
	 * @param broken the state of the block being broken. Use this rather than reading
	 *               {@code pos} from the world: NeoForge calls {@code mineBlock} before
	 *               the block is removed, but vanilla/Fabric call it afterwards, so on
	 *               Fabric the world already holds air there.
	 */
	public void startBreak(ItemStack itemstack, BlockState broken, BlockPos pos, LivingEntity player);

	/**
	 * Breaks the blocks at {@code positions} that {@code breakable} accepts, as the player,
	 * one durability for each block actually broken, and stops once the tool is gone.
	 * breakBlockAsPlayer checks harvestability and carries the block entity along (so a
	 * shulker box keeps its contents); only real breaks cost durability.
	 */
	static void breakAlso(ItemStack tool, Iterable<BlockPos> positions, ServerPlayer player,
			Predicate<BlockState> breakable) {
		Level level = player.level();
		for (BlockPos p : positions) {
			BlockState s = level.getBlockState(p);
			if (breakable.test(s) && LootUtils.breakBlockAsPlayer(tool, p, player, level, s)) {
				TraitDispatch.spendDurability(tool, 1, player, EquipmentSlot.MAINHAND);
				if (tool.isEmpty()) {
					return;
				}
			}
		}
	}

	/**
	 * Runs {@code action} on every item entity in the 3x3x3 box around {@code pos} once
	 * {@code delayMs} has passed. The delay lets the block's drops spawn; the action is
	 * then submitted to the server thread for thread-safe execution.
	 */
	static void forDropsAfter(long delayMs, ServerLevel level, BlockPos pos, Consumer<ItemEntity> action) {
		AABB box = new AABB(Vec3.atCenterOf(pos.east().south().below()), Vec3.atCenterOf(pos.west().north().above()));
		CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS).execute(() -> {
			level.getServer().execute(() -> level.getEntities(EntityTypes.ITEM, box, e -> true).forEach(action));
		});
	}

}
