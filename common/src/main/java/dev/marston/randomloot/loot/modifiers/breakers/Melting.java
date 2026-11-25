package dev.marston.randomloot.loot.modifiers.breakers;

import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;

public class Melting extends AbstractModifier implements BlockBreakModifier {
	@Override
	public void startBreak(ItemStack itemstack, BlockState broken, BlockPos pos, LivingEntity player) {
		ServerLevel level = (ServerLevel) player.level();

		BlockBreakModifier.forDropsAfter(50, level, pos, i -> {
			if (i.getAge() > 10) {
				return;
			}

			ItemStack stack = i.getItem();
			SingleRecipeInput input = new SingleRecipeInput(stack);
			level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level).ifPresent(holder -> {
				ItemStack result = holder.value().assemble(input);
				if (result.isEmpty()) {
					return;
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
			});
		});
	}

	@Override
	public String description() {
		return "Items dropped by blocks broken with this tool will be smelted.";
	}

}
