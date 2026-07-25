package dev.marston.randomloot.loot.modifiers.breakers;

import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Melting extends AbstractModifier implements BlockBreakModifier {

	public Melting(String name) {
		this.name = name;
	}

	public Melting() {
		this.name = "Melting";
	}

	@Override
	public boolean startBreak(ItemStack itemstack, BlockPos pos, LivingEntity player) {

		Level level = player.level();

		if (level.isClientSide()) {
			return false;
		}

		ServerLevel serverLevel = (ServerLevel) level;
		AABB box = new AABB(Vec3.atCenterOf(pos.east().south().below()), Vec3.atCenterOf(pos.west().north().above()));

		// Schedule execution after a short delay to allow block drops to spawn
		// Then submit to server thread for thread-safe execution
		CompletableFuture.delayedExecutor(50, TimeUnit.MILLISECONDS).execute(() -> {
			serverLevel.getServer().execute(() -> {
			List<Entity> items = level.getEntities(null, box);

			for (Entity entity : items) {
				if (entity.getType() != EntityTypes.ITEM) {
					continue;
				}

				ItemEntity i = (ItemEntity) entity;
				if (i.getAge() > 10) {
					continue;
				}

				ItemStack stack = i.getItem();

				RecipeManager manager = serverLevel.recipeAccess();

				Collection<RecipeHolder<?>> recipes = manager.getRecipes();
				List<SingleItemRecipe> smeltingRecipes = recipes.stream()
						.map(RecipeHolder::value)
						.filter(r -> r.getType() == RecipeType.SMELTING)
						.filter(r -> r instanceof SingleItemRecipe)
						.map(r -> (SingleItemRecipe) r)
						.toList();

				for (SingleItemRecipe recipe : smeltingRecipes) {
					if (!recipe.matches(new SingleRecipeInput(stack), level)) {
						continue;
					}

					ItemStack result = recipe.assemble(new SingleRecipeInput(stack));

					if (result.isEmpty()) {
						continue;
					}

					// Capture motion before modifying original item
					var motion = i.getDeltaMovement();

					ItemEntity k = new ItemEntity(level, i.getX(), i.getY(), i.getZ(), result);

					i.setPos(i.position().x, -1, i.position().z);
					i.kill(serverLevel);

					level.addFreshEntity(k);

					// Set motion after spawn to prevent it being reset
					k.setDeltaMovement(motion);

					break;
				}
			}
			});
		});

		return false;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Melting(tag.getStringOr(NAME, "Melting"));
	}

	@Override
	public String tagName() {
		return "melting";
	}

	@Override
	public ChatFormatting color() {
		return ChatFormatting.RED;
	}

	@Override
	public String description() {
		return "Items dropped by blocks broken with this tool will be smelted.";
	}

	@Override
	public boolean forTool(ToolType type) {
		return isMiningTool(type);
	}
}
