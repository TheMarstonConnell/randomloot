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
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class TraitAdditionRecipe implements SmithingRecipe {
	final Holder<Item> additionItem;
	/** How many of {@link #additionItem} the add template needs and consumes. */
	final int additionCount;
	final Optional<Ingredient> template;
	final Optional<Ingredient> base;
	final String trait;
	@Nullable
	private PlacementInfo placementInfo;

	/** The recipe JSON's {@code "item"} object: the addition and how many of it adding costs. */
	private record Addition(Holder<Item> item, int count) {
	}

	// Reads {"id": <item>, "count": <int>} via Item.CODEC, which (unlike ItemStack.CODEC) does
	// NOT require item data-components to be bound. Recipes are parsed during datapack load,
	// before components are bound, so ItemStack.CODEC made every trait recipe fail to load with
	// "Item ... does not have components yet" -- which left the tool unplaceable in the smithing
	// table (no recipe contributed it to the SMITHING_BASE set). The same ordering is why the
	// count is only range-checked here and not against the item's max stack size (a component);
	// the trait_recipe_counts_load gametest covers that instead.
	private static final Codec<Addition> ADDITION_CODEC = RecordCodecBuilder.create(
			i -> i.group(
							Item.CODEC.fieldOf("id").forGetter(Addition::item),
							ExtraCodecs.intRange(1, Item.ABSOLUTE_MAX_STACK_SIZE).optionalFieldOf("count", 1)
									.forGetter(Addition::count)
					)
					.apply(i, Addition::new)
	);

	public static final MapCodec<TraitAdditionRecipe> CODEC = RecordCodecBuilder.mapCodec(
			builder -> builder.group(
							ADDITION_CODEC.fieldOf("item").forGetter(g -> new Addition(g.additionItem, g.additionCount)),
							Codec.STRING.fieldOf("trait").forGetter(g -> g.trait)
					)
					.apply(builder, (addition, trait) -> new TraitAdditionRecipe(addition.item(), addition.count(), trait))
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, TraitAdditionRecipe> STREAM_CODEC = StreamCodec.composite(
			Item.STREAM_CODEC,
			c -> c.additionItem,
			ByteBufCodecs.VAR_INT,
			c -> c.additionCount,
			ByteBufCodecs.STRING_UTF8,
			c -> c.trait,
			TraitAdditionRecipe::new
	);

	public TraitAdditionRecipe(Holder<Item> additionItem, int additionCount, String traitIn) {
		this.additionItem = additionItem;
		this.additionCount = additionCount;
		this.trait = traitIn;
		this.base = Optional.of(Ingredient.of(ModItems.TOOL.get(), ModItems.ARMOR.get()));
		this.template= Optional.of(Ingredient.of(ModItems.MOD_SUB.get(), ModItems.MOD_ADD.get()));
	}

	public Holder<Item> additionItem() {
		return this.additionItem;
	}

	/** The count from the recipe JSON: what adding the trait costs. */
	public int additionCount() {
		return this.additionCount;
	}

	/**
	 * How many of the addition item this template needs in the slot and uses up. Adding
	 * pays the recipe's full count; stripping a trait back off costs a single item, so
	 * undoing Spelunking is not another 64 torches.
	 */
	public int additionCost(ItemStack template) {
		return template.is(ModItems.MOD_ADD.get()) ? this.additionCount : 1;
	}

	/**
	 * Charges the part of {@link #additionCost} vanilla does not. {@code SmithingMenu.onTake}
	 * shrinks every input slot by exactly one, so the other {@code cost - 1} come out of
	 * the addition slot here, just before that happens. Both loaders route their
	 * take-the-result hook in ({@link dev.marston.randomloot.platform.GameHook#SMITHING_TAKE})
	 * and both fire it once per take, for a plain click and for a shift-click alike.
	 *
	 * <p>It has to be a menu hook rather than {@code Item.onCraftedBy} on the result: a
	 * shift-click has already moved the result into the inventory by the time
	 * {@code onTake} runs, so vanilla hands it an empty stack and no item callback fires.
	 *
	 * <p>The recipe is read back from the result container instead of being looked up
	 * again, so the charge always belongs to the recipe that produced the result being
	 * taken. Only the server records one, which makes this a no-op on the client: it
	 * predicts a one-item shrink and is corrected by the slot sync that follows the click.
	 */
	public static void chargeAddition(SmithingMenu menu) {
		if (!(menu.getSlot(SmithingMenu.RESULT_SLOT).container instanceof RecipeCraftingHolder results)) {
			return;
		}
		RecipeHolder<?> used = results.getRecipeUsed();
		if (used == null || !(used.value() instanceof TraitAdditionRecipe recipe)) {
			return;
		}
		int extra = recipe.additionCost(menu.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem()) - 1;
		if (extra > 0) {
			// No setChanged: vanilla's own shrink of this slot follows immediately and
			// re-runs createResult against the final contents.
			menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem().shrink(extra);
		}
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

		// The count in the recipe JSON is the price. It used to be parsed and dropped, so
		// a single torch bought Spelunking; the preview now stays empty until the slot
		// holds enough to pay.
		if (input.addition().getCount() < additionCost(input.template())) {
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
		// subtraction template stays ungated so mismatched traits can always be stripped -
		// but the trait does have to be on the gear, or it has the same problem: the
		// materials are consumed and an identical item comes back.
		if (input.template().is(ModItems.MOD_ADD.get())) {
			TraitEligibility.Result verdict = TraitEligibility.check(input.base(), modToAdd);
			if (!verdict.allowed()) {
				RandomLoot.LOGGER.debug("Recipe blocked: {} cannot be added to this gear ({})", this.trait,
						verdict.verdict());
				return false;
			}
		} else if (input.template().is(ModItems.MOD_SUB.get())
				&& !LootUtils.hasRawTrait(input.base(), this.trait)) {
			return false;
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

	/**
	 * One entry per template, because they cost different amounts: adding shows the
	 * recipe's count on the addition, removing shows a single item.
	 */
	@Override
	public List<RecipeDisplay> display() {
		return List.of(displayFor(ModItems.MOD_ADD.get(), this.additionCount), displayFor(ModItems.MOD_SUB.get(), 1));
	}

	private RecipeDisplay displayFor(Item template, int cost) {
		return new SmithingRecipeDisplay(
				new SlotDisplay.ItemSlotDisplay(template),
				baseIngredient().display(),
				new SlotDisplay.ItemStackSlotDisplay(
						new ItemStackTemplate(this.additionItem, cost, DataComponentPatch.EMPTY)),
				baseIngredient().display(),
				new SlotDisplay.ItemSlotDisplay(Items.SMITHING_TABLE)
		);
	}
}
