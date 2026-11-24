package dev.marston.randomloot.recipes;

import com.mojang.serialization.MapCodec;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.LootArmorItem;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TextureChangeRecipe extends CustomRecipe {
	/** Tools and armor both support cosmetic texture cycling - once they've settled. */
	private static boolean isLootGear(ItemStack stack) {
		return LootUtils.isLootGear(stack) && !LootUtils.isRolling(stack);
	}

	// Resolved per-call rather than in a static Ingredient so this class never
	// touches the item registry during class load.
	private static boolean isEssence(ItemStack stack) {
		return stack.is(ModItems.ESSENCE.get());
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
		// assemble() would have no gear to retexture.
		return hasTool;
	}

	@Override
	public @NotNull ItemStack assemble(@NotNull CraftingInput craftingInput) {
		// matches admits exactly one settled gear stack, and essence for every other stack.
		ItemStack result = LootUtils.CloneItem(craftingInput.items().stream()
				.filter(TextureChangeRecipe::isLootGear).findFirst().orElseThrow());
		LootUtils.addTexture(result, craftingInput.ingredientCount() - 1);
		return result;
	}

	@Override
	public RecipeSerializer<TextureChangeRecipe> getSerializer() {
		return Recipies.TEXTURE_CHANGE_SHAPELESS.get();
	}
}
