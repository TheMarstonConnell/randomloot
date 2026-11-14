package dev.marston.randomloot.loot.modifiers.breakers;

import dev.marston.randomloot.loot.modifiers.Stored;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.TraitDispatch;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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

	public void checkAndBreak(ItemStack itemstack, BlockPos pos, Player player, Level level, int index, Block blockType,
			Set<BlockPos> tobreak, Set<BlockPos> visited) {

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

		checkAndBreak(itemstack, pos.above(), player, level, dex, blockType, tobreak, visited);
		checkAndBreak(itemstack, pos.below(), player, level, dex, blockType, tobreak, visited);
		checkAndBreak(itemstack, pos.east(), player, level, dex, blockType, tobreak, visited);
		checkAndBreak(itemstack, pos.west(), player, level, dex, blockType, tobreak, visited);
		checkAndBreak(itemstack, pos.north(), player, level, dex, blockType, tobreak, visited);
		checkAndBreak(itemstack, pos.south(), player, level, dex, blockType, tobreak, visited);

	}

	@Override
	public void startBreak(ItemStack itemstack, BlockState broken, BlockPos pos, LivingEntity p) {

		if (!(p instanceof ServerPlayer)) {
			return;
		}

		ServerPlayer player = (ServerPlayer) p;

		if (!player.isCrouching()) {
			return;
		}

		Level l = player.level();

		LootItem li = (LootItem) itemstack.getItem();

		if (!li.isCorrectToolForDrops(itemstack, broken)) {
			return;
		}

		Block b = broken.getBlock();

		Set<BlockPos> toBreak = new HashSet<BlockPos>();

		checkAndBreak(itemstack, pos, player, l, 0, b, toBreak, new HashSet<BlockPos>());

		for (BlockPos blockPos : toBreak) {
			boolean destroyed = LootUtils.breakBlockAsPlayer(itemstack, blockPos, player, l, l.getBlockState(blockPos));

			if (destroyed) {
				TraitDispatch.spendDurability(itemstack, 1, player, EquipmentSlot.MAINHAND);
				if (itemstack.isEmpty()) {
					break;
				}
			}
		}
	}

	@Override
	public String description() {
		return "Breaking any block while crouching will cause all blocks of the same type adjacent to it to break up to "
				+ power.get().intValue() + " in each direction.";
	}

	@Override
	public boolean compatible(Modifier mod) {
		// Incompatible with Excavator
		return !(mod instanceof Excavator);
	}

}
