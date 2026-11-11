package dev.marston.randomloot.recipes;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.jetbrains.annotations.Nullable;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.advancements.ModCriteria;
import dev.marston.randomloot.advancements.TraitObtainedTrigger;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.TraitEligibility;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.RecipeHolder;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class TraitAdditionRecipe implements SmithingRecipe {
	private final ResourceLocation id;
	final Item additionItem;
	/** How many of {@link #additionItem} the add template needs and consumes. */
	final int additionCount;
	final Ingredient template;
	final Ingredient base;
	final String trait;
	/**
	 * The seed of the server world this recipe last matched in, which is the world its
	 * next {@link #assemble} is for. One server has one seed, so it never changes once set.
	 */
	@Nullable
	private Long worldSeed;

	/**
	 * The three smithing slots by name. 1.21.1+ hand recipes a SmithingRecipeInput; on
	 * 1.20.1 a recipe gets the menu's raw input Container, so this wraps slots 0-2.
	 */
	public record Input(ItemStack template, ItemStack base, ItemStack addition) {
		public static Input of(Container container) {
			return new Input(container.getItem(SmithingMenu.TEMPLATE_SLOT), container.getItem(SmithingMenu.BASE_SLOT),
					container.getItem(SmithingMenu.ADDITIONAL_SLOT));
		}

		/** A throwaway container holding these stacks in smithing-slot order. */
		public Container toContainer() {
			return new SimpleContainer(template, base, addition);
		}
	}

	/**
	 * 1.20.1 recipe serializers read JSON and the network buffer by hand (the codec form
	 * arrived in 1.20.2). The JSON shape is the same as on the newer branches:
	 * {@code {"item": {"id": <item>, "count": <int>}, "trait": <trait>}}.
	 */
	public static final class Serializer implements RecipeSerializer<TraitAdditionRecipe> {
		@Override
		public TraitAdditionRecipe fromJson(ResourceLocation id, JsonObject json) {
			JsonObject addition = GsonHelper.getAsJsonObject(json, "item");
			ResourceLocation itemId = new ResourceLocation(GsonHelper.getAsString(addition, "id"));
			Item item = BuiltInRegistries.ITEM.getOptional(itemId)
					.orElseThrow(() -> new JsonSyntaxException("Unknown item '" + itemId + "'"));
			int count = GsonHelper.getAsInt(addition, "count", 1);
			if (count < 1 || count > 64) {
				throw new JsonSyntaxException("Trait recipe count must be 1-64, got " + count);
			}
			return new TraitAdditionRecipe(id, item, count, GsonHelper.getAsString(json, "trait"));
		}

		@Override
		public TraitAdditionRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
			Item item = buf.readById(BuiltInRegistries.ITEM);
			int count = buf.readVarInt();
			return new TraitAdditionRecipe(id, item, count, buf.readUtf());
		}

		@Override
		public void toNetwork(FriendlyByteBuf buf, TraitAdditionRecipe recipe) {
			buf.writeId(BuiltInRegistries.ITEM, recipe.additionItem);
			buf.writeVarInt(recipe.additionCount);
			buf.writeUtf(recipe.trait);
		}
	}

	public TraitAdditionRecipe(ResourceLocation id, Item additionItem, int additionCount, String traitIn) {
		this.id = id;
		this.additionItem = additionItem;
		this.additionCount = additionCount;
		this.trait = traitIn;
		this.base = Ingredient.of(ModItems.TOOL.get(), ModItems.ARMOR.get());
		this.template = Ingredient.of(ModItems.MOD_SUB.get(), ModItems.MOD_ADD.get());
	}

	@Override
	public ResourceLocation getId() {
		return this.id;
	}

	public Item additionItem() {
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
		if (!(menu.getSlot(SmithingMenu.RESULT_SLOT).container instanceof RecipeHolder results)) {
			return;
		}
		Recipe<?> used = results.getRecipeUsed();
		if (!(used instanceof TraitAdditionRecipe recipe)) {
			return;
		}
		Input input = new Input(menu.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem(),
				menu.getSlot(SmithingMenu.BASE_SLOT).getItem(), menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem());

		// A shift-click has already moved the taken stack into the inventory, out of
		// reach. The inputs are still whole and assemble to the same gear.
		if (player instanceof ServerPlayer serverPlayer) {
			ModCriteria.traitsObtained(serverPlayer, recipe.getResult(input), TraitObtainedTrigger.SOURCE_CRAFTED);
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
	public boolean matches(Container container, Level level) {
		Input input = Input.of(container);

		if (!input.base().is(ModItems.TOOL.get()) && !input.base().is(ModItems.ARMOR.get())) {
			return false;
		}

		// Gear that is still rolling has no settled identity to smith against.
		if (LootUtils.isRolling(input.base())) {
			return false;
		}

		if (!input.addition().is(this.additionItem)) {
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

		if (!this.template.test(input.template())) {
			return false;
		}

		// assemble() is handed no level, so the match leaves it the world to forge for.
		if (level instanceof ServerLevel serverLevel) {
			this.worldSeed = serverLevel.getSeed();
		}
		return true;
	}

	/** The gear this recipe turns {@code input} into; {@link #assemble} by slot name. */
	public ItemStack getResult(Input input) {

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
	public ItemStack assemble(Container container, RegistryAccess registryAccess) {
		return this.getResult(Input.of(container));
	}

	@Override
	public ItemStack getResultItem(RegistryAccess registryAccess) {
		// Recipe-book preview only; the real result is assembled per input.
		return new ItemStack(ModItems.TOOL.get());
	}

	@Override
	public boolean isTemplateIngredient(ItemStack stack) {
		return this.template.test(stack);
	}

	@Override
	public boolean isBaseIngredient(ItemStack stack) {
		return this.base.test(stack);
	}

	@Override
	public boolean isAdditionIngredient(ItemStack stack) {
		return stack.is(this.additionItem);
	}

	@Override
	public @NotNull RecipeSerializer<TraitAdditionRecipe> getSerializer() {
		return Recipies.TRAIT_ADDITION_RECIPE.get();
	}
}
