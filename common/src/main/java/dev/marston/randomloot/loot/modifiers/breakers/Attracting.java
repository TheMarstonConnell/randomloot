package dev.marston.randomloot.loot.modifiers.breakers;

import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class Attracting extends AbstractModifier implements BlockBreakModifier {
	@Override
	public void startBreak(ItemStack itemstack, BlockState broken, BlockPos pos, LivingEntity player) {
		BlockBreakModifier.forDropsAfter(100, (ServerLevel) player.level(), pos, item -> item.setPos(player.position()));
	}

	@Override
	public String description() {
		return "Upon breaking a block (allowed by tool type), all items at that block's position will teleport to you.";
	}

}
