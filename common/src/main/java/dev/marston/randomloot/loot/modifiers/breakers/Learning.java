package dev.marston.randomloot.loot.modifiers.breakers;

import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.Stored;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class Learning extends AbstractModifier implements BlockBreakModifier {
	private final static String COUNT = "count";
	private final static String POINTS = "points";
	private static final int max = 10;

	/** Blocks broken since the last payout. */
	private final Stored<Integer> count = stored(COUNT, 0);
	/** Experience a payout gives. Stored, so gear keeps the amount it was made with. */
	private final Stored<Integer> points = stored(POINTS, 3);

	@Override
	public void startBreak(ItemStack itemstack, BlockState broken, BlockPos pos, LivingEntity entity) {

		if (!(entity instanceof Player)) {
			return;
		}

		Player player = (Player) entity;

		int counted = count.get() + 1;

		while (counted >= max) {
			counted = counted - max;
			player.giveExperiencePoints(points.get());
		}

		count.set(counted);
	}

	@Override
	public String description() {
		return "After breaking " + max + " blocks as allowed by this tool, gain " + points.get() + " experience points.";
	}

	@Override
	public Component writeDetailsToLore(Level level) {

		float amt = ((float) count.get()) / ((float) max) * 100;
		String perc = String.format("%.0f%% Learned", amt);

		return Modifier.makeComp(perc, ChatFormatting.GRAY);
	}

}
