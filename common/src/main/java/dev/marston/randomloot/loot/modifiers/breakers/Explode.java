package dev.marston.randomloot.loot.modifiers.breakers;

import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.Stored;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.state.BlockState;

public class Explode extends AbstractModifier implements BlockBreakModifier {
	private final static String POWER = "power";

	/** Blast size. Stored, so gear keeps the size it was made with. */
	private final Stored<Float> power = stored(POWER, 4.0f);

	@Override
	public void startBreak(ItemStack itemstack, BlockState broken, BlockPos pos, LivingEntity player) {

		Level l = player.level();

		l.explode(player, Explosion.getDefaultDamageSource(l, player), null, pos.getX(), pos.getY() + 0.5, pos.getZ(), power.get(), false, ExplosionInteraction.TNT);
	}

	@Override
	public String description() {
		return "Upon breaking a block (allowed by tool type), the current block position will explode causing damage to surrounding blocks.";
	}

}
