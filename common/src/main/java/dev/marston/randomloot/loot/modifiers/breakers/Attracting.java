package dev.marston.randomloot.loot.modifiers.breakers;

import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Attracting extends AbstractModifier implements BlockBreakModifier {
	@Override
	public void startBreak(ItemStack itemstack, BlockState broken, BlockPos pos, LivingEntity player) {

		Level level = player.level();

		ServerLevel serverLevel = (ServerLevel) level;
		AABB box = new AABB(Vec3.atCenterOf(pos.east().south().below()), Vec3.atCenterOf(pos.west().north().above()));

		// Schedule execution after a short delay to allow block drops to spawn
		// Then submit to server thread for thread-safe execution
		CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS).execute(() -> {
			serverLevel.getServer().execute(() -> {
				List<Entity> items = level.getEntities(null, box);

				for (Entity entity : items) {
					if (entity.getType() == EntityTypes.ITEM) {
						entity.setPos(player.position());
					}
				}
			});
		});
	}

	@Override
	public String description() {
		return "Upon breaking a block (allowed by tool type), all items at that block's position will teleport to you.";
	}

}
