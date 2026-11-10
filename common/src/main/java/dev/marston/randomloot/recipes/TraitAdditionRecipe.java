package dev.marston.randomloot.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.advancements.ModCriteria;
import dev.marston.randomloot.advancements.TraitObtainedTrigger;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.TraitEligibility;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.SmithingMenu;
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
	/** How many of {@link #additionItem} the add template needs and consumes. */
	final int additionCount;
	final Optional<Ingredient> template;
	final Optional<Ingredient> base;
	final String trait;
	/**
	 * The seed of the server world this recipe last matched in, which is the world its
	 * next {@link #assemble} is for. One server has one seed, so it never changes once set.
	 */
	@Nullable
	private Long worldSeed;

	/** The recipe JSON's {@code "item"} object: the addition and how many of it adding costs. */
	private record Addition(Holder<Item> item, int count) {
	}

	// Reads {"id": <item>, "count": <int>} by item id, which (unlike ItemStack.CODEC) does
	// NOT require item data-components to be bound. Recipes are parsed during datapack load,
	// before components are bound, so ItemStack.CODEC made every trait recipe fail to load with
	// "Item ... does not have components yet" -- which left the tool unplaceable in the smithing
	// table (no recipe contributed it to the SMITHING_BASE set). The same ordering is why the
	// count is only range-checked here and not against the item's max stack size (a component);
	// the trait_recipe_counts_load gametest covers that instead.
	private static final Codec<Addition> ADDITION_CODEC = RecordCodecBuilder.create(
			i -> i.group(
							BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("id").forGetter(Addition::item),
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
			ByteBufCodecs.holderRegistry(Registries.ITEM),
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
	 * What a trait recipe does when its result leaves the table, by a click or a
	 * shift-click ({@link dev.marston.randomloot.platform.GameHook#SMITHING_TAKE}). Call
	 * it once per take, before the inputs shrink. On 1.21.1 the client has the recipes
	 * too and runs this for its own copy of the menu, so it predicts the same shrink;
	 * the advancement criteria are server-only.
	 */
	public static void onTake(SmithingMenu menu, Player player) {
		if (!(menu.getSlot(SmithingMenu.RESULT_SLOT).container instanceof RecipeCraftingHolder results)) {
			return;
		}
		RecipeHolder<?> used = results.getRecipeUsed();
		if (used == null || !(used.value() instanceof TraitAdditionRecipe recipe)) {
			return;
		}
		SmithingRecipeInput input = new SmithingRecipeInput(menu.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem(),
				menu.getSlot(SmithingMenu.BASE_SLOT).getItem(), menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem());

		// A shift-click has already moved the taken stack into the inventory, out of
		// reach. The inputs are still whole and assemble to the same gear.
		if (player instanceof ServerPlayer serverPlayer) {
			ModCriteria.traitsObtained(serverPlayer, recipe.assemble(input, serverPlayer.registryAccess()), TraitObtainedTrigger.SOURCE_CRAFTED);
		}

		// Vanilla's onTake shrinks every input by one; the rest of additionCost leaves the
		// addition slot here.
		int extra = recipe.additionCost(input.template()) - 1;
		if (extra > 0) {
			// No setChanged: vanilla's own shrink of this slot follows immediately and
			// re-runs createResult against the final contents.
			input.addition().shrink(extra);
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

		if (!this.template.get().test(input.template())) {
			return false;
		}

		// assemble() is handed no level, so the match leaves it the world to forge for.
		if (level instanceof ServerLevel serverLevel) {
			this.worldSeed = serverLevel.getSeed();
		}
		return true;
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

			// Stamped as the result is built, not as it is taken: nothing can get at the
			// stack a shift-click takes (see onTake), and the preview now shows the name
			// the player will actually get.
			if (this.worldSeed != null) {
				LootUtils.applyWorldForgers(stack, this.worldSeed);
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
