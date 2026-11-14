package dev.marston.randomloot.loot;

import org.jetbrains.annotations.Nullable;

import dev.marston.randomloot.advancements.ModCriteria;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Trait grant: puts a trait on a piece of gear, or takes one off, and says what happened.
 *
 * <p>{@link TraitEligibility} answers whether a trait may go on. This module does the
 * rest in one step - asks that gate, resolves the trait for the world, adds it or levels
 * the one already there - and hands back an {@link Outcome}. The case roll, the level-up
 * roll, the smithing table and {@code /randomloot trait add} all grant through here, so
 * each of them only renders the outcome: a preview stack, a chat line, a command reply.
 */
public final class TraitGrant {

	private TraitGrant() {
	}

	/** What a grant or a removal did to the gear. */
	public enum Kind {
		/** The trait is new on the gear. */
		ADDED,
		/** The gear already carried the trait, which went up a level. */
		LEVELED,
		/** The trait was taken off the gear. */
		REMOVED,
		/** The trait could not go on. The gear is unchanged and {@link Outcome#gate()} says why. */
		REFUSED,
		/** The trait to take off was not on the gear. The gear is unchanged. */
		NOT_PRESENT
	}

	/**
	 * The result of one {@link #grant} or {@link #remove}.
	 *
	 * @param trait the trait that was asked for, as its world's variant when a seed was
	 *              given. It names the trait; the level lives on the gear's own copy.
	 * @param gear  the stack that was asked to change
	 * @param gate  the gate's refusal when {@code kind} is {@link Kind#REFUSED}, else null
	 */
	public record Outcome(Kind kind, Modifier trait, ItemStack gear, @Nullable TraitEligibility.Result gate) {

		/** The gear gained the trait, or a level of it. */
		public boolean granted() {
			return this.kind == Kind.ADDED || this.kind == Kind.LEVELED;
		}

		/** The gear is not what it was: something was granted or removed. */
		public boolean changed() {
			return granted() || this.kind == Kind.REMOVED;
		}

		/** Why nothing changed, worded for a player, or null when something did. */
		@Nullable
		public Component refusal() {
			return switch (this.kind) {
			case REFUSED -> this.gate.reason(this.trait);
			case NOT_PRESENT -> Component.empty().append("This gear does not have ").append(this.trait.displayName())
					.append(".");
			case ADDED, LEVELED, REMOVED -> null;
			};
		}

		/**
		 * Fires {@code trait_obtained} for the player, with the gear's traits as they are
		 * now, if this outcome granted something. A removal or a refusal obtains nothing,
		 * so a caller names its source and does not have to check.
		 */
		public void obtainedBy(ServerPlayer player, String source) {
			if (granted()) {
				ModCriteria.traitsObtained(player, this.gear, source);
			}
		}
	}

	/**
	 * Grants {@code trait} to {@code gear} if the gate allows it: adds it, or levels the
	 * one already there. A trait that varies by world ({@link Modifier#forWorld}, such as
	 * the forger in DirtPlace's name) goes on as that world's variant. A null seed means
	 * no world is known, and the trait goes on as the registry holds it.
	 */
	public static Outcome grant(ItemStack gear, Modifier trait, @Nullable Long worldSeed) {
		TraitEligibility.Result gate = TraitEligibility.check(gear, trait);
		if (!gate.allowed()) {
			return new Outcome(Kind.REFUSED, trait, gear, gate);
		}
		return write(gear, worldSeed == null ? trait : trait.forWorld(worldSeed));
	}

	/**
	 * {@link #grant} with no gate and no world, for building gear a player could not have
	 * made: a Thorny sword, a forger that does not exist. Test setup only.
	 */
	public static Outcome force(ItemStack gear, Modifier trait) {
		return write(gear, trait);
	}

	/**
	 * Takes {@code trait} off {@code gear}. There is no gate on the way out: a trait that
	 * is switched off in the config, or that the gear should never have had, still comes off.
	 */
	public static Outcome remove(ItemStack gear, Modifier trait) {
		String tag = trait.tagName();
		if (!LootUtils.hasRawTrait(gear, tag)) {
			return new Outcome(Kind.NOT_PRESENT, trait, gear, null);
		}
		GearTags.mutate(gear, Modifier.MODTAG, traits -> traits.remove(tag));
		return new Outcome(Kind.REMOVED, trait, gear, null);
	}

	/**
	 * Re-resolves every world-dependent trait the gear carries against this world.
	 * {@link #grant} stamps only the trait it adds; the smithing table also calls this, so
	 * gear leaving a table credits this world's forger whatever it carried going in.
	 */
	public static void restamp(ItemStack gear, long worldSeed) {
		for (Modifier held : LootUtils.getModifiers(gear)) {
			Modifier resolved = held.forWorld(worldSeed);
			if (resolved != held) {
				LootUtils.updateModifier(gear, resolved);
			}
		}
	}

	private static Outcome write(ItemStack gear, Modifier trait) {
		String tag = trait.tagName();
		CompoundTag stored = GearTags.read(gear, Modifier.MODTAG);

		if (!stored.contains(tag)) {
			GearTags.mutate(gear, Modifier.MODTAG, traits -> traits.put(tag, trait.toNBT()));
			return new Outcome(Kind.ADDED, trait, gear, null);
		}

		// Already on the gear: its own copy levels, keeping whatever else it stores.
		Modifier held = trait.fromNBT(stored.getCompoundOrEmpty(tag));
		if (!held.canLevel()) {
			// The gate refuses a capped trait before grant gets here; only force can.
			return new Outcome(Kind.REFUSED, trait, gear, new TraitEligibility.Result(
					TraitEligibility.Verdict.ALREADY_MAXED, held, LootUtils.getToolType(gear)));
		}

		held.levelUp();
		GearTags.mutate(gear, Modifier.MODTAG, traits -> traits.put(held.tagName(), held.toNBT()));
		return new Outcome(Kind.LEVELED, trait, gear, null);
	}
}
