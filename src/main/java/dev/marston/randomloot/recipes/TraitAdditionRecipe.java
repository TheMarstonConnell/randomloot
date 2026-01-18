package dev.marston.randomloot.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class TraitAdditionRecipe implements SmithingRecipe {
	final ItemStack addition;
	final Optional<Ingredient> template;
	final Optional<Ingredient> base;
	final String trait;
	@Nullable
	private PlacementInfo placementInfo;

	public TraitAdditionRecipe(ItemStack addition, String traitIn) {
		this.addition = addition;
		this.trait = traitIn;
		this.base = Optional.of(Ingredient.of(ModItems.TOOL.asItem()));
		this.template= Optional.of(Ingredient.of(ModItems.MOD_SUB.asItem(), ModItems.MOD_ADD.asItem()));
	}

	@Override
	public boolean matches(SmithingRecipeInput input, Level level) {

		if (!input.base().is(ModItems.TOOL.asItem())) {
			return false;
		}

		if (!this.addition.is(input.addition().getItem())) {

			return false;
		}

//		if (this.addition.getCount() > input.addition().getCount()) {
//			return false;
//		}

        return this.template.get().test(input.template());
    }

	private ItemStack getResult(SmithingRecipeInput input) {

			ItemStack tool = input.base();

			ItemStack template = input.template();

			ItemStack stack = LootUtils.CloneItem(tool);

			Modifier modToAdd = ModifierRegistry.getModifier(this.trait);

			if (modToAdd == null) {
				RandomLoot.LOGGER.warn("Unknown modifier trait in recipe: {}", this.trait);
				return stack;
			}

			if (template.is(ModItems.MOD_ADD.asItem())) {
				LootUtils.addModifier(stack, modToAdd);
			} else if (template.is(ModItems.MOD_SUB.asItem())) {
				LootUtils.removeModifier(stack, modToAdd);
			}


			return stack;
	}




	public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider provider) {
		return this.getResult(input);
	}

	@Override
	public Optional<Ingredient> templateIngredient() {
		return this.template;
	}

	@Override
	public Ingredient baseIngredient() {
		return this.base.get();
	}

	@Override
	public Optional<Ingredient> additionIngredient() {
		return Optional.of(Ingredient.of(this.addition.getItem()));
	}

	@Override
	public @NotNull RecipeSerializer<TraitAdditionRecipe> getSerializer() {
		return Recipies.TRAIT_ADDITION_RECIPE.get();
	}

	@Override
	public PlacementInfo placementInfo() {
		if (this.placementInfo == null) {
			this.placementInfo = PlacementInfo.createFromOptionals(List.of(templateIngredient(), Optional.of(baseIngredient()), additionIngredient()));
		}

		return this.placementInfo;
	}

	@Override
	public List<RecipeDisplay> display() {
		return List.of(
				new SmithingRecipeDisplay(
						Ingredient.optionalIngredientToDisplay(templateIngredient()),
						baseIngredient().display(),
						Ingredient.optionalIngredientToDisplay(additionIngredient()),
						baseIngredient().display(),
						new SlotDisplay.ItemSlotDisplay(Items.SMITHING_TABLE)
				)
		);
	}

	public static class Serializer implements RecipeSerializer<TraitAdditionRecipe> {
		private static final MapCodec<TraitAdditionRecipe> CODEC = RecordCodecBuilder.mapCodec(
				builder -> builder.group(
								ItemStack.CODEC.fieldOf("item").forGetter(g -> g.addition),
								Codec.STRING.fieldOf("trait").forGetter(g -> g.trait)
						)
						.apply(builder, TraitAdditionRecipe::new)
		);
		public static final StreamCodec<RegistryFriendlyByteBuf, TraitAdditionRecipe> STREAM_CODEC = StreamCodec.composite(
				ItemStack.STREAM_CODEC,
				c -> c.addition,
				ByteBufCodecs.STRING_UTF8,
				c -> c.trait,
				TraitAdditionRecipe::new
		);

		@Override
		public MapCodec<TraitAdditionRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, TraitAdditionRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}

