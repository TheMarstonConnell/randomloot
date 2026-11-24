package dev.marston.randomloot.loot.modifiers.breakers;

import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class Excavator extends AbstractModifier implements BlockBreakModifier {
	private List<BlockPos> get3x3Positions(BlockPos center, ServerPlayer player) {
		// The plane perpendicular to the look direction: horizontal when looking up or
		// down, else the vertical plane across the facing.
		boolean flat = Math.abs(player.getXRot()) > 45;
		Direction.Axis outer = !flat && player.getDirection().getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
		Direction.Axis inner = flat ? Direction.Axis.Z : Direction.Axis.Y;

		List<BlockPos> positions = new ArrayList<>();
		for (int a = -1; a <= 1; a++) {
			for (int b = -1; b <= 1; b++) {
				if (a != 0 || b != 0) {
					positions.add(center.relative(outer, a).relative(inner, b));
				}
			}
		}
		return positions;
	}

	@Override
	public void startBreak(ItemStack itemstack, BlockState broken, BlockPos pos, LivingEntity p) {
		if (!(p instanceof ServerPlayer player) || !player.isCrouching()) {
			return;
		}

		BlockBreakModifier.breakAlso(itemstack, get3x3Positions(pos, player), player, itemstack::isCorrectToolForDrops);
	}

	@Override
	public String description() {
		return "Breaking blocks while crouching mines a 3x3 area perpendicular to the surface.";
	}
}
