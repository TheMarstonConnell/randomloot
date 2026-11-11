package dev.marston.randomloot.recipes;

import dev.marston.randomloot.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.*;

import java.util.function.Supplier;

public final class Recipies {

	public static final Supplier<RecipeSerializer<TextureChangeRecipe>> TEXTURE_CHANGE_SHAPELESS =
			Services.REG.register(Registries.RECIPE_SERIALIZER, "texture_change_recipe",
					() -> new SimpleCraftingRecipeSerializer<>(TextureChangeRecipe::new));

	public static final Supplier<RecipeSerializer<TraitAdditionRecipe>> TRAIT_ADDITION_RECIPE =
			Services.REG.register(Registries.RECIPE_SERIALIZER, "trait_change",
					TraitAdditionRecipe.Serializer::new);

	/** Classloads the class so the serializer registrations above run. */
	public static void init() {
	}
}
