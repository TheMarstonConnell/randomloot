package dev.marston.randomloot.recipes;

import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.LootArmorItem;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Predicate;

public class TextureChangeRecipe extends CustomRecipe {
	private static final List<Predicate<ItemStack>> ITEM_PREDICATES = List.of(
			TextureChangeRecipe::isLootGear,
			TextureChangeRecipe::isEssence
	);

	/** Tools and armor both support cosmetic texture cycling - once they've settled. */
	private static boolean isLootGear(ItemStack stack) {
		return LootUtils.isLootGear(stack) && !LootUtils.isRolling(stack);
	}

	// Resolved per-call rather than in a static Ingredient so this class never
	// touches the item registry during class load.
	private static boolean isEssence(ItemStack stack) {
		return stack.is(ModItems.ESSENCE.get());
	}



	public TextureChangeRecipe(ResourceLocation id, CraftingBookCategory category) {
		super(id, category);
	}

	@Override
	public boolean matches(CraftingContainer container, Level level) {
		boolean hasTool = false;
		List<ItemStack> items = container.getItems();

		for (ItemStack item: items) {
			if (item.isEmpty()) {
				continue;
			}

			if (isLootGear(item)) {
				if (hasTool) {
					return false;
				}
				hasTool = true;
				continue;
			}

			if (!isEssence(item)) {
				return false;
			}
		}

		// Require an actual tool: matches() must not pass on essence alone, or
		// assemble() would return EMPTY.
		return hasTool;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height >= 2;
	}

	@Override
	public @NotNull ItemStack assemble(@NotNull CraftingContainer craftingInput, net.minecraft.core.RegistryAccess registryAccess) {
		int modCount = 0;
		List<ItemStack> stacks = craftingInput.getItems();

		ItemStack result = ItemStack.EMPTY;
        for (ItemStack item : stacks) {
			if (item.isEmpty()) {
				continue;
			}

            if (isLootGear(item)) {
                result = LootUtils.CloneItem(item);
				continue;
            }


			if (isEssence(item)) {
				modCount++;
			}

        }

		if (result.isEmpty()) {
			return ItemStack.EMPTY;
		}

		LootUtils.addTexture(result, modCount);

		return result;
	}

	@Override
	public RecipeSerializer<TextureChangeRecipe> getSerializer() {
		return Recipies.TEXTURE_CHANGE_SHAPELESS.get();
	}


}