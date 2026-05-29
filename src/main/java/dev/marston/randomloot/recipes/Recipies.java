package dev.marston.randomloot.recipes;

import dev.marston.randomloot.RandomLoot;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class Recipies {
	public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, RandomLoot.MODID);
	public static final Supplier<RecipeSerializer<TextureChangeRecipe>> TEXTURE_CHANGE_SHAPELESS = RECIPE_SERIALIZERS.register("texture_change_recipe", () -> new RecipeSerializer<>(TextureChangeRecipe.CODEC, TextureChangeRecipe.STREAM_CODEC));

	public static final Supplier<RecipeSerializer<TraitAdditionRecipe>> TRAIT_ADDITION_RECIPE = RECIPE_SERIALIZERS.register("trait_change", () -> new RecipeSerializer<>(TraitAdditionRecipe.CODEC, TraitAdditionRecipe.STREAM_CODEC));

	public static void register(IEventBus eventBus) {
		RandomLoot.LOGGER.info("REGISTERING RECIPES!");
		RECIPE_SERIALIZERS.register(eventBus);
	}

}
