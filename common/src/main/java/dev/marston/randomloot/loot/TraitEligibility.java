package dev.marston.randomloot.loot;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.BiomeRestrictedModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Whether a trait may be added to a piece of loot gear, and why not when it can't.
 *
 * <p>The rule used to be written three times - once for the case roll
 * ({@link LootUtils#generateNewTrait}), once for {@code /randomloot trait add}, and once
 * for the smithing table - and the three copies disagreed: smithing skipped the config
 * toggle, the compatibility check and the level cap, so it would consume the template
 * and the addition item and hand back an unchanged tool. All three now ask this module.
 *
 * <p>{@link #check(GearContext, Modifier)} works off a {@link GearContext} rather than an
 * {@code ItemStack} so the rule can be exercised without a bound item registry; the
 * {@code ItemStack} overload just reads a context off the stack.
 */
public final class TraitEligibility {

	private TraitEligibility() {
	}

	/** Why a trait can or cannot go on a piece of gear. */
	public enum Verdict {
		OK,
		/** The stack isn't a Random Loot tool or armor piece. */
		NOT_GEAR,
		/** Freshly opened gear has no settled identity to smith against yet. */
		ROLLING,
		/** Turned off in the config. */
		DISABLED,
		/** The trait doesn't work on this gear type (no Veiny helmets, no Thorny swords). */
		WRONG_TYPE,
		/** A biome-restricted trait, on gear from the wrong biome/dimension. */
		WRONG_BIOME,
		/** Already present, and it doesn't level. */
		ALREADY_MAXED,
		/** Another trait on the gear conflicts with this one. */
		INCOMPATIBLE
	}

	/**
	 * The outcome of a check. Carries enough to render the refusal, so callers that talk
	 * to a player don't re-derive it.
	 */
	public record Result(Verdict verdict, @Nullable Modifier conflict, ToolType type) {

		public boolean allowed() {
			return verdict == Verdict.OK;
		}

		/** Player-facing explanation, or {@code null} when the trait is allowed. */
		@Nullable
		public Component reason(Modifier trait) {
			return switch (verdict) {
			case OK -> null;
			case NOT_GEAR -> Component.literal("Hold a Random Loot tool or armor piece in your main hand.");
			case ROLLING -> Component.literal("This gear is still being revealed.");
			case DISABLED -> Component.literal("Trait is disabled in the config: " + trait.tagName());
			case WRONG_TYPE -> Component.empty().append(trait.displayName()).append(" cannot be applied to a ")
					.append(type.displayName()).append(".");
			case WRONG_BIOME -> Component.empty().append(trait.displayName())
					.append(" cannot be applied to gear from this gear's biome.");
			case ALREADY_MAXED -> Component.empty().append("This gear already has ").append(trait.displayName())
					.append(" and it cannot level up.");
			case INCOMPATIBLE -> Component.empty().append(trait.displayName()).append(" is incompatible with ")
					.append(conflict == null ? Component.literal("another trait") : conflict.displayName())
					.append(".");
			};
		}
	}

	/**
	 * The gear facts the rule needs. Read one off a stack with {@link #contextOf}, or
	 * build one directly to exercise the rule without a live {@code ItemStack}.
	 */
	public record GearContext(ToolType type, String biomeKey, float temperature, String dimension,
			List<Modifier> existing, boolean rolling, boolean gear) {

		/** A context for gear that carries no biome data - used by tests and base gear. */
		public static GearContext of(ToolType type, List<Modifier> existing) {
			return new GearContext(type, "", 0.7f, "minecraft:overworld", existing, false, true);
		}
	}

	/** Reads the facts {@link #check} needs off a stack. */
	public static GearContext contextOf(ItemStack gear) {
		if (!LootUtils.isLootGear(gear)) {
			return new GearContext(ToolType.NULL, "", 0.7f, "minecraft:overworld", List.of(), false, false);
		}
		return new GearContext(LootUtils.getToolType(gear), LootUtils.getBiomeKey(gear),
				LootUtils.getBiomeTemperature(gear), LootUtils.getDimension(gear), LootUtils.getModifiers(gear),
				LootUtils.isRolling(gear), true);
	}

	/** Whether {@code trait} may be added to the gear described by {@code ctx}. */
	public static Result check(GearContext ctx, Modifier trait) {
		if (!ctx.gear()) {
			return new Result(Verdict.NOT_GEAR, null, ctx.type());
		}
		if (ctx.rolling()) {
			return new Result(Verdict.ROLLING, null, ctx.type());
		}
		if (!Config.traitEnabled(trait.tagName())) {
			return new Result(Verdict.DISABLED, null, ctx.type());
		}
		if (!trait.forTool(ctx.type())) {
			return new Result(Verdict.WRONG_TYPE, null, ctx.type());
		}
		if (trait instanceof BiomeRestrictedModifier restricted
				&& !restricted.canSpawnInBiome(ctx.biomeKey(), ctx.temperature(), ctx.dimension())) {
			return new Result(Verdict.WRONG_BIOME, null, ctx.type());
		}

		for (Modifier existing : ctx.existing()) {
			if (existing.tagName().equals(trait.tagName())) {
				if (!existing.canLevel()) {
					return new Result(Verdict.ALREADY_MAXED, existing, ctx.type());
				}
			} else if (!existing.compatible(trait)) {
				return new Result(Verdict.INCOMPATIBLE, existing, ctx.type());
			}
		}

		return new Result(Verdict.OK, null, ctx.type());
	}

	/** Whether {@code trait} may be added to {@code gear}. */
	public static Result check(ItemStack gear, Modifier trait) {
		return check(contextOf(gear), trait);
	}
}
