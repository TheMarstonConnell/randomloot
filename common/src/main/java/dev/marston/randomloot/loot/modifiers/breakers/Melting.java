package dev.marston.randomloot.loot.modifiers.breakers;

import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Melting extends AbstractModifier implements BlockBreakModifier {
	@Override
	public void startBreak(ItemStack itemstack, BlockState broken, BlockPos pos, LivingEntity player) {

		Level level = player.level();

		ServerLevel serverLevel = (ServerLevel) level;
		AABB box = new AABB(Vec3.atCenterOf(pos.east().south().below()), Vec3.atCenterOf(pos.west().north().above()));

		// Schedule execution after a short delay to allow block drops to spawn
		// Then submit to server thread for thread-safe execution
		CompletableFuture.delayedExecutor(50, TimeUnit.MILLISECONDS).execute(() -> {
			serverLevel.getServer().execute(() -> {
			List<Entity> items = level.getEntities(null, box);

			for (Entity entity : items) {
				if (entity.getType() != EntityType.ITEM) {
					continue;
				}

				ItemEntity i = (ItemEntity) entity;
				if (i.getAge() > 10) {
					continue;
				}

				ItemStack stack = i.getItem();

				RecipeManager manager = serverLevel.getRecipeManager();

				List<SmeltingRecipe> smeltingRecipes = manager.getAllRecipesFor(RecipeType.SMELTING);
				net.minecraft.world.Container smeltInput = new net.minecraft.world.SimpleContainer(stack);

				for (SmeltingRecipe recipe : smeltingRecipes) {
					if (!recipe.matches(smeltInput, level)) {
						continue;
					}

					ItemStack result = recipe.assemble(smeltInput, serverLevel.registryAccess());

					if (result.isEmpty()) {
						continue;
					}

					// The recipe result is for ONE input item, but a drop is often a
					// stack (2-5 raw copper, Fortune), so smelt the whole stack rather
					// than replacing it with a single result.
					int remaining = result.getCount() * stack.getCount();

					// Capture position and motion before removing the original item
					var motion = i.getDeltaMovement();
					double x = i.getX();
					double y = i.getY();
					double z = i.getZ();
					i.discard();

					while (remaining > 0) {
						int count = Math.min(remaining, result.getMaxStackSize());
						ItemEntity k = new ItemEntity(level, x, y, z, result.copyWithCount(count));
						level.addFreshEntity(k);

						// Set motion after spawn to prevent it being reset
						k.setDeltaMovement(motion);
						remaining -= count;
					}

					break;
				}
			}
			});
		});
	}

	@Override
	public String description() {
		return "Items dropped by blocks broken with this tool will be smelted.";
	}

}
