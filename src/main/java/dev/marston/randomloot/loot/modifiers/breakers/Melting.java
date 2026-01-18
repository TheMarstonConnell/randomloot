package dev.marston.randomloot.loot.modifiers.breakers;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.DummyContainer;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Collection;
import java.util.List;

public class Melting implements BlockBreakModifier {

	private String name;
	private float power;
	private final static String POWER = "power";

	public Melting(String name, float power) {
		this.name = name;
		this.power = power;
	}

	public Melting() {
		this.name = "Melting";
		this.power = 1.0f;
	}

	public Modifier clone() {
		return new Melting();
	}

	@Override
	public boolean startBreak(ItemStack itemstack, BlockPos pos, LivingEntity player) {

		Level l = player.level();

		AABB box = new AABB(pos.east().south().below().getCenter(), pos.west().north().above().getCenter());

		RegistryAccess access = l.registryAccess();

		Thread thread = new Thread() {
			public void run() {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				List<Entity> items = l.getEntities(null, box);

				for (Entity entity : items) {

					if (entity.getType() == EntityType.ITEM) {
						ItemEntity i = (ItemEntity) entity;
						if (i.getAge() > 10) {
							continue;
						}

						RandomLoot.LOGGER.info(i.getItem().getDisplayName().getString());

						ItemStack stack = i.getItem();

						DummyContainer mc = new DummyContainer(stack);

						Level level = player.level();

						if (level.isClientSide()) {
							return;
						}

						ServerLevel serverLevel = (ServerLevel) level;

						RecipeManager manager = serverLevel.recipeAccess();

						Collection<RecipeHolder<?>> recipes = manager.getRecipes();
						List<SingleItemRecipe> smeltingRecipes = recipes.stream().map(RecipeHolder::value).filter(r -> r.getType() == RecipeType.SMELTING).map(r->(SingleItemRecipe)r).toList();

						for (SingleItemRecipe recipe : smeltingRecipes) {
							if (!recipe.matches(new SingleRecipeInput(stack), level)) {
								continue;
							}

							ItemStack result = recipe.assemble(new SingleRecipeInput(stack), null);

							if (result.isEmpty()) {
								continue;
							}

							ItemEntity k = new ItemEntity(l, i.getX(), i.getY(), i.getZ(), result);
							k.setDeltaMovement(i.getDeltaMovement());

							i.setPos(i.position().x, -1, i.position().z);
							i.kill(serverLevel);

							l.addFreshEntity(k);

							break;
						}

					}
				}

			}
		};

		thread.start();
		return false;

	}

	@Override
	public CompoundTag toNBT() {

		CompoundTag tag = new CompoundTag();

		tag.putFloat(POWER, power);

		tag.putString(NAME, name);

		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Melting(tag.getStringOr(NAME, "Melting"), tag.getFloatOr(POWER, 1.0f));
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "melting";
	}

	@Override
	public String color() {
		return "red";
	}

	@Override
	public String description() {
		return "Items dropped by blocks broken with this tool will be smelted.";
	}

	@Override
	public void writeToLore(List<Component> list, boolean shift) {

		MutableComponent comp = Modifier.makeComp(this.name(), this.color());

		list.add(comp);
	}

	@Override
	public Component writeDetailsToLore(Level level) {

		return null;
	}

	@Override
	public boolean compatible(Modifier mod) {
		return true;
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.PICKAXE) || type.equals(ToolType.AXE) || type.equals(ToolType.SHOVEL);
	}

	public boolean canLevel() {
		return false;
	}

	public void levelUp() {
		return;
	}
}
