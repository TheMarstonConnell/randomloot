package dev.marston.randomloot.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.TraitEligibility;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class TraitAdditionRecipe implements SmithingRecipe {
	final Holder<Item> additionItem;
	final Optional<Ingredient> template;
	final Optional<Ingredient> base;
	final String trait;

	// Reads {"id": <item>, "count": <int>} via Item.CODEC, which (unlike ItemStack.CODEC) does
	// NOT require item data-components to be bound. Recipes are parsed during datapack load,
	// before components are bound, so ItemStack.CODEC made every trait recipe fail to load with
	// "Item ... does not have components yet" -- which left the tool unplaceable in the smithing
	// table (no recipe contributed it to the SMITHING_BASE set). Only the item type is needed.
	private static final Codec<Holder<Item>> ADDITION_ITEM_CODEC = RecordCodecBuilder.create(
			i -> i.group(
							BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("id").forGetter(holder -> holder),
							Codec.INT.optionalFieldOf("count", 1).forGetter(holder -> 1)
					)
					.apply(i, (item, count) -> item)
	);

	public static final MapCodec<TraitAdditionRecipe> CODEC = RecordCodecBuilder.mapCodec(
			builder -> builder.group(
							ADDITION_ITEM_CODEC.fieldOf("item").forGetter(g -> g.additionItem),
							Codec.STRING.fieldOf("trait").forGetter(g -> g.trait)
					)
					.apply(builder, TraitAdditionRecipe::new)
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, TraitAdditionRecipe> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.holderRegistry(Registries.ITEM),
			c -> c.additionItem,
			ByteBufCodecs.STRING_UTF8,
			c -> c.trait,
			TraitAdditionRecipe::new
	);

	public TraitAdditionRecipe(Holder<Item> additionItem, String traitIn) {
		this.additionItem = additionItem;
		this.trait = traitIn;
		this.base = Optional.of(Ingredient.of(ModItems.TOOL.get(), ModItems.ARMOR.get()));
		this.template= Optional.of(Ingredient.of(ModItems.MOD_SUB.get(), ModItems.MOD_ADD.get()));
	}

	@Override
	public boolean matches(SmithingRecipeInput input, Level level) {

		if (!input.base().is(ModItems.TOOL.get()) && !input.base().is(ModItems.ARMOR.get())) {
			return false;
		}

		// Gear that is still rolling has no settled identity to smith against.
		if (LootUtils.isRolling(input.base())) {
			return false;
		}

		if (!input.addition().is(this.additionItem.value())) {
			return false;
		}

		// An unknown/typo'd trait id (e.g. from a stale data pack) must not match, or the
		// recipe would consume the template + addition while producing an unchanged tool.
		Modifier modToAdd = ModifierRegistry.getModifier(this.trait);
		if (modToAdd == null) {
			return false;
		}

		// Adding runs the same gate as the case roll and /randomloot trait add: type,
		// biome, config toggle, compatibility and the level cap. Skipping any of them
		// here used to eat the template and the addition for an unchanged tool. The
		// subtraction template stays ungated so mismatched traits can always be stripped.
		if (input.template().is(ModItems.MOD_ADD.get())) {
			TraitEligibility.Result verdict = TraitEligibility.check(input.base(), modToAdd);
			if (!verdict.allowed()) {
				RandomLoot.LOGGER.debug("Recipe blocked: {} cannot be added to this gear ({})", this.trait,
						verdict.verdict());
				return false;
			}
		}

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

			if (template.is(ModItems.MOD_ADD.get())) {
				LootUtils.addModifier(stack, modToAdd);
			} else if (template.is(ModItems.MOD_SUB.get())) {
				LootUtils.removeModifier(stack, modToAdd);
			}


			return stack;
	}




	@Override
	public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider provider) {
		return this.getResult(input);
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider provider) {
		// Recipe-book preview only; the real result is assembled per input.
		return new ItemStack(ModItems.TOOL.get());
	}

	@Override
	public boolean isTemplateIngredient(ItemStack stack) {
		return this.template.get().test(stack);
	}

	@Override
	public boolean isBaseIngredient(ItemStack stack) {
		return this.base.get().test(stack);
	}

	@Override
	public boolean isAdditionIngredient(ItemStack stack) {
		return stack.is(this.additionItem.value());
	}

	@Override
	public @NotNull RecipeSerializer<TraitAdditionRecipe> getSerializer() {
		return Recipies.TRAIT_ADDITION_RECIPE.get();
	}
}

