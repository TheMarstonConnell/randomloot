package dev.marston.randomloot.recipes;

import dev.marston.randomloot.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.*;

import java.util.function.Supplier;

public final class Recipies {

	@SuppressWarnings("unchecked")
	public static final Supplier<RecipeSerializer<TextureChangeRecipe>> TEXTURE_CHANGE_SHAPELESS =
			(Supplier<RecipeSerializer<TextureChangeRecipe>>) (Supplier<?>) Services.REG.register(Registries.RECIPE_SERIALIZER,
					"texture_change_recipe", () -> new RecipeSerializer<>(TextureChangeRecipe.CODEC, TextureChangeRecipe.STREAM_CODEC));

	@SuppressWarnings("unchecked")
	public static final Supplier<RecipeSerializer<TraitAdditionRecipe>> TRAIT_ADDITION_RECIPE =
			(Supplier<RecipeSerializer<TraitAdditionRecipe>>) (Supplier<?>) Services.REG.register(Registries.RECIPE_SERIALIZER,
					"trait_change", () -> new RecipeSerializer<>(TraitAdditionRecipe.CODEC, TraitAdditionRecipe.STREAM_CODEC));

	/** Classloads the class so the serializer registrations above run. */
	public static void init() {
	}
}
