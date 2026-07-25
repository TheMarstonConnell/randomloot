package dev.marston.randomloot.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.TraitEligibility;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import com.mojang.brigadier.CommandDispatcher;

import java.util.Locale;
import java.util.stream.Stream;

/**
 * Admin/debug commands for working with Random Loot gear:
 *
 * <pre>
 * /randomloot give &lt;type&gt;          - spawn a base (0 goodness) tool or armor piece
 * /randomloot trait add &lt;trait&gt;    - add a trait to the held gear (levels it if already present)
 * /randomloot trait remove &lt;trait&gt; - remove a trait from the held gear
 * /randomloot xp &lt;amount&gt;          - add XP to the held gear
 * </pre>
 */
public class ModCommands {

	private static final SuggestionProvider<CommandSourceStack> ADDABLE_TRAITS = (ctx, builder) -> {
		ServerPlayer player = ctx.getSource().getPlayer();
		if (player == null) {
			return builder.buildFuture();
		}
		ItemStack held = heldGear(player);
		if (held.isEmpty()) {
			return builder.buildFuture();
		}
		return SharedSuggestionProvider.suggest(ModifierRegistry.getModifiers().values().stream()
				.filter(mod -> addBlocker(held, mod) == null).map(Modifier::tagName), builder);
	};

	private static final SuggestionProvider<CommandSourceStack> HELD_TRAITS = (ctx, builder) -> {
		ServerPlayer player = ctx.getSource().getPlayer();
		if (player == null) {
			return builder.buildFuture();
		}
		// Raw tag keys, not getModifiers(): config-disabled traits should still be removable.
		return SharedSuggestionProvider
				.suggest(LootUtils.getOrCreateTagElement(player.getMainHandItem(), Modifier.MODTAG).keySet(), builder);
	};

	private static final SuggestionProvider<CommandSourceStack> GEAR_TYPES = (ctx, builder) -> SharedSuggestionProvider
			.suggest(Stream.of(ToolType.values()).filter(t -> t != ToolType.NULL)
					.map(t -> t.name().toLowerCase(Locale.ROOT)), builder);

	/** Registers /randomloot; called from each loader's command-registration hook. */
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("randomloot")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("give")
						.then(Commands.argument("type", StringArgumentType.word())
								.suggests(GEAR_TYPES)
								.executes(ctx -> give(ctx.getSource(), StringArgumentType.getString(ctx, "type")))))
				.then(Commands.literal("trait")
						.then(Commands.literal("add")
								.then(Commands.argument("trait", StringArgumentType.word())
										.suggests(ADDABLE_TRAITS)
										.executes(ctx -> addTrait(ctx.getSource(),
												StringArgumentType.getString(ctx, "trait")))))
						.then(Commands.literal("remove")
								.then(Commands.argument("trait", StringArgumentType.word())
										.suggests(HELD_TRAITS)
										.executes(ctx -> removeTrait(ctx.getSource(),
												StringArgumentType.getString(ctx, "trait"))))))
				.then(Commands.literal("xp")
						.then(Commands.argument("amount", IntegerArgumentType.integer(1))
								.executes(ctx -> addXp(ctx.getSource(),
										IntegerArgumentType.getInteger(ctx, "amount"))))));
	}

	private static int give(CommandSourceStack source, String typeName) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();

		ToolType parsed;
		try {
			parsed = ToolType.valueOf(typeName.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			parsed = ToolType.NULL;
		}
		if (parsed == ToolType.NULL) {
			source.sendFailure(Component.literal("Unknown gear type: " + typeName));
			return 0;
		}
		ToolType type = parsed;

		ItemStack stack = LootUtils.buildTool(player, player.level(), player.blockPosition(), type, 0.0f);

		// Inventory.add drains the inserted stack, so grab the name first.
		Component name = stack.getDisplayName();

		if (!player.getInventory().add(stack)) {
			ItemEntity dropItem = new ItemEntity(EntityTypes.ITEM, player.level());
			dropItem.setItem(stack);
			dropItem.setPos(player.position());
			player.level().addFreshEntity(dropItem);
		}
		source.sendSuccess(() -> Component.literal("Spawned base ").append(type.displayName())
				.append(" ").append(name), true);
		return 1;
	}

	/** The held Random Loot tool/armor piece, or EMPTY if the main hand holds anything else. */
	private static ItemStack heldGear(ServerPlayer player) {
		ItemStack held = player.getMainHandItem();
		if (!held.is(ModItems.TOOL.get()) && !held.is(ModItems.ARMOR.get())) {
			return ItemStack.EMPTY;
		}
		return held;
	}

	private static boolean hasTrait(ItemStack held, String tagName) {
		return LootUtils.getModifiers(held).stream().anyMatch(m -> m.tagName().equals(tagName));
	}

	/**
	 * Why {@code mod} can't be added to {@code held}, or {@code null} when it can. Shared
	 * by the add executor and its tab-completion so the suggestions never offer a trait
	 * the command would refuse - and with the case roll and the smithing table, via
	 * {@link TraitEligibility}.
	 */
	private static Component addBlocker(ItemStack held, Modifier mod) {
		return TraitEligibility.check(held, mod).reason(mod);
	}

	private static int addTrait(CommandSourceStack source, String traitName) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();

		ItemStack held = heldGear(player);
		if (held.isEmpty()) {
			source.sendFailure(Component.literal("Hold a Random Loot tool or armor piece in your main hand."));
			return 0;
		}

		Modifier mod = ModifierRegistry.getModifier(traitName);
		if (mod == null) {
			source.sendFailure(Component.literal("Unknown trait: " + traitName));
			return 0;
		}

		Component blocker = addBlocker(held, mod);
		if (blocker != null) {
			source.sendFailure(blocker);
			return 0;
		}

		boolean leveling = hasTrait(held, mod.tagName());

		LootUtils.addModifier(held, mod.forWorld(source.getLevel().getSeed()));

		boolean leveled = leveling;
		source.sendSuccess(() -> Component.literal(leveled ? "Leveled up " : "Added ").append(mod.displayName())
				.append(leveled ? " on " : " to ").append(held.getDisplayName()), true);
		return 1;
	}

	private static int removeTrait(CommandSourceStack source, String traitName) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();

		ItemStack held = heldGear(player);
		if (held.isEmpty()) {
			source.sendFailure(Component.literal("Hold a Random Loot tool or armor piece in your main hand."));
			return 0;
		}

		Modifier mod = ModifierRegistry.getModifier(traitName);
		if (mod == null) {
			source.sendFailure(Component.literal("Unknown trait: " + traitName));
			return 0;
		}

		// Check the raw tag, not getModifiers(): config-disabled traits are still on the
		// item and should still be removable.
		if (!LootUtils.getOrCreateTagElement(held, Modifier.MODTAG).contains(mod.tagName())) {
			source.sendFailure(Component.empty().append("This gear does not have ").append(mod.displayName())
					.append("."));
			return 0;
		}

		LootUtils.removeModifier(held, mod);

		source.sendSuccess(() -> Component.literal("Removed ").append(mod.displayName()).append(" from ")
				.append(held.getDisplayName()), true);
		return 1;
	}

	private static int addXp(CommandSourceStack source, int amount) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();

		ItemStack held = heldGear(player);
		if (held.isEmpty()) {
			source.sendFailure(Component.literal("Hold a Random Loot tool or armor piece in your main hand."));
			return 0;
		}

		LootUtils.addXp(held, player, amount);

		int level = LootUtils.getLevel(held);
		int xp = LootUtils.getXP(held);
		int max = LootUtils.getMaxXP(level);
		source.sendSuccess(() -> Component.literal("Added " + amount + " XP to ").append(held.getDisplayName())
				.append(" - now level " + level + " (" + xp + "/" + max + " XP)"), true);
		return 1;
	}
}
