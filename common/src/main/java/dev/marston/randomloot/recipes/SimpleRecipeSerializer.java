package dev.marston.randomloot.recipes;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * 1.21.1 backport shim: RecipeSerializer became a record in 26.x, so the 26.x
 * branch constructs it directly. Here it is still an interface; this record is
 * the equivalent constructor.
 */
public record SimpleRecipeSerializer<T extends Recipe<?>>(MapCodec<T> codec,
		StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) implements RecipeSerializer<T> {
}
