package dev.marston.randomloot.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.BiomeRestrictedModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class TraitAdditionRecipe implements SmithingRecipe {
	final Holder<Item> additionItem;
	final Optional<Ingredient> template;
	final Optional<Ingredient> base;
	final String trait;
	@Nullable
	private PlacementInfo placementInfo;

	// Reads {"id": <item>, "count": <int>} via Item.CODEC, which (unlike ItemStack.CODEC) does
	// NOT require item data-components to be bound. Recipes are parsed during datapack load,
	// before components are bound, so ItemStack.CODEC made every trait recipe fail to load with
	// "Item ... does not have components yet" -- which left the tool unplaceable in the smithing
	// table (no recipe contributed it to the SMITHING_BASE set). Only the item type is needed.
	private static final Codec<Holder<Item>> ADDITION_ITEM_CODEC = RecordCodecBuilder.create(
			i -> i.group(
							Item.CODEC.fieldOf("id").forGetter(holder -> holder),
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
			Item.STREAM_CODEC,
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

		// Gear only accepts traits that actually work on its type (no Veiny helmets, no
		// Thorny swords), matching the case-roll and /randomloot trait add paths. The
		// subtraction template stays ungated so mismatched traits can always be stripped.
		LootItem.ToolType baseType = LootUtils.getToolType(input.base());
		if (input.template().is(ModItems.MOD_ADD.get()) && !modToAdd.forTool(baseType)) {
			return false;
		}

		// Check if we're adding a modifier (not removing)
		if (input.template().is(ModItems.MOD_ADD.get())) {
			// If it's a biome-restricted modifier, check if the tool's biome matches
			if (modToAdd instanceof BiomeRestrictedModifier biomeRestricted) {
				ItemStack tool = input.base();
				String biomeKey = LootUtils.getBiomeKey(tool);
				float temp = LootUtils.getBiomeTemperature(tool);
				String dimension = LootUtils.getDimension(tool);

				boolean canAdd = biomeRestricted.canSpawnInBiome(biomeKey, temp, dimension);

				if (!canAdd) {
					RandomLoot.LOGGER.info("Recipe blocked: {} cannot be added to tool from biome: {}, temp: {}, dim: {}",
							this.trait, biomeKey, temp, dimension);
					return false;
				}
			}
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

			if (template.is(ModItems.MOD_ADD.get())) {
				LootUtils.addModifier(stack, modToAdd);
			} else if (template.is(ModItems.MOD_SUB.get())) {
				LootUtils.removeModifier(stack, modToAdd);
			}


			return stack;
	}




	@Override
	public ItemStack assemble(SmithingRecipeInput input) {
		return this.getResult(input);
	}

	@Override
	public String group() {
		return "";
	}

	@Override
	public boolean showNotification() {
		return true;
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
		return Optional.of(Ingredient.of(this.additionItem.value()));
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
}

