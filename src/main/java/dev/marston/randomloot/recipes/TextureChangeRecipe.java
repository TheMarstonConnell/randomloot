package dev.marston.randomloot.recipes;

import com.mojang.serialization.MapCodec;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Predicate;

public class TextureChangeRecipe extends CustomRecipe {
	private static final Item ingredient = Items.AMETHYST_SHARD;
	private static final List<Predicate<ItemStack>> ITEM_PREDICATES = List.of(
			stack -> stack.getItem() instanceof LootItem,
			stack -> stack.getItem().equals(ingredient)
	);
	private static final Ingredient CHANGE_TEXTURE_INGREDIENT = Ingredient.of(ingredient);



	public TextureChangeRecipe() {
	}

	public static final TextureChangeRecipe INSTANCE = new TextureChangeRecipe();
	public static final MapCodec<TextureChangeRecipe> CODEC = MapCodec.unit(INSTANCE);
	public static final StreamCodec<RegistryFriendlyByteBuf, TextureChangeRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public boolean matches(CraftingInput container, Level level) {
		if (container.ingredientCount() < 2) {
			return false;
		}

		boolean hasTool = false;
		List<ItemStack> items = container.items();

		for (ItemStack item: items) {
			if (item.isEmpty()) {
				continue;
			}

			if (item.getItem() instanceof LootItem) {
				if (hasTool) {
					return false;
				}
				hasTool = true;
				continue;
			}

			if (!CHANGE_TEXTURE_INGREDIENT.test(item)) {
				return false;
			}
		}

		// Require an actual tool: matches() must not pass on shards alone, or it would
		// shadow vanilla amethyst recipes and assemble() would return EMPTY.
		return hasTool;
	}

	@Override
	public @NotNull ItemStack assemble(@NotNull CraftingInput craftingInput) {
		int modCount = 0;
		List<ItemStack> stacks = craftingInput.items();

		ItemStack result = ItemStack.EMPTY;
        for (ItemStack item : stacks) {
			if (item.isEmpty()) {
				continue;
			}

            if (item.getItem() instanceof LootItem) {
                result = LootUtils.CloneItem(item);
				continue;
            }


			if (CHANGE_TEXTURE_INGREDIENT.test(item)) {
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