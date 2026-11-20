package dev.marston.randomloot.loot.modifiers;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import dev.marston.randomloot.loot.GearTags;
import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Trait dispatch: carries a game event to the traits on a piece of gear.
 *
 * <p>A caller names the event and hands over its arguments. What the events have in
 * common is decided here, once, so a trait class states only its own rule:
 * <ul>
 * <li>the gear's traits are loaded once for an event's hooks and the durability the event
 * itself costs, without the config-disabled ones;
 * <li>gear still in its reveal roll is inert, worn or held;
 * <li>a hit trait that reads pre-hit state ({@link PreHitModifier}) runs before the rest;
 * <li>hooks that change the world run on the server only;
 * <li>a trait whose stored state changed in its hook is written back to the gear, and a
 * trait that changed nothing writes nothing;
 * <li>the durability an event costs is spent here, with Unbreaking's roll.
 * </ul>
 */
public final class TraitDispatch {

	private TraitDispatch() {
	}

	/** A melee hit with the weapon has landed. Costs the weapon one durability. */
	public static void hit(ItemStack weapon, LivingEntity target, LivingEntity attacker) {
		Level level = target.level();
		if (level.isClientSide() || LootUtils.isRolling(weapon)) {
			return;
		}

		List<Modifier> traits = traitsOf(weapon);
		if (traits.size() > 1) {
			// Another trait's bonus damage is a hit too and would overwrite the note a
			// pre-hit trait is about to read. The sort is stable: the rest keep their order.
			traits.sort(Comparator.comparing(trait -> !(trait instanceof PreHitModifier)));
		}

		for (Modifier trait : traits) {
			if (trait instanceof EntityHurtModifier onHit) {
				run(weapon, trait, level, () -> onHit.hurtEnemy(weapon, target, attacker));
			}
		}

		spend(weapon, traits, 1, attacker, EquipmentSlot.MAINHAND);
	}

	/**
	 * The tool has broken a block. Costs the tool one durability.
	 *
	 * @param broken the state that was broken; see {@link BlockBreakModifier#startBreak}
	 */
	public static void blockBroken(ItemStack tool, BlockState broken, BlockPos pos, LivingEntity breaker) {
		Level level = breaker.level();
		if (level.isClientSide() || LootUtils.isRolling(tool)) {
			return;
		}

		List<Modifier> traits = traitsOf(tool);
		for (Modifier trait : traits) {
			if (trait instanceof BlockBreakModifier onBreak) {
				run(tool, trait, level, () -> onBreak.startBreak(tool, broken, pos, breaker));
			}
		}

		spend(tool, traits, 1, breaker, EquipmentSlot.MAINHAND);
	}

	/**
	 * The tool was right-clicked on a block. Returns the first trait result that consumes
	 * the click, or PASS. This one runs on the client too: a placing trait predicts its
	 * block there, like any block item.
	 */
	public static InteractionResult useOn(UseOnContext ctx) {
		ItemStack tool = ctx.getItemInHand();
		if (LootUtils.isRolling(tool)) {
			return InteractionResult.PASS;
		}

		for (Modifier trait : traitsOf(tool)) {
			if (trait instanceof UseModifier onUse) {
				InteractionResult result = call(tool, trait, ctx.getLevel(), () -> onUse.use(ctx));
				if (result.consumesAction()) {
					return result;
				}
			}
		}
		return InteractionResult.PASS;
	}

	/** The tool was right-clicked at nothing in particular. True when a trait acted on it. */
	public static boolean use(ItemStack tool, Level level, Player player, InteractionHand hand) {
		if (level.isClientSide() || LootUtils.isRolling(tool)) {
			return false;
		}

		for (Modifier trait : traitsOf(tool)) {
			if (trait instanceof UseModifier onUse && onUse.useAnywhere()
					&& call(tool, trait, level, () -> onUse.use(level, player, hand))) {
				return true;
			}
		}
		return false;
	}

	/** Whether a right-click anywhere would reach a trait on this tool. */
	public static boolean usesAnywhere(ItemStack tool) {
		if (LootUtils.isRolling(tool)) {
			return false;
		}

		for (Modifier trait : traitsOf(tool)) {
			if (trait instanceof UseModifier onUse && onUse.useAnywhere()) {
				return true;
			}
		}
		return false;
	}

	/** One server tick of the gear being held, or worn in its own slot. */
	public static void tick(ItemStack gear, ServerLevel level, Entity holder) {
		if (LootUtils.isRolling(gear)) {
			return;
		}

		for (Modifier trait : traitsOf(gear)) {
			if (trait instanceof HoldModifier onHold) {
				run(gear, trait, level, () -> onHold.hold(gear, level, holder));
			}
		}
	}

	/** The killer's weapon is credited with a kill. */
	public static void kill(ItemStack weapon, LivingEntity victim, LivingEntity killer) {
		Level level = victim.level();
		if (level.isClientSide() || LootUtils.isRolling(weapon)) {
			return;
		}

		for (Modifier trait : traitsOf(weapon)) {
			if (trait instanceof EntityKillModifier onKill) {
				run(weapon, trait, level, () -> onKill.onKill(weapon, victim, killer));
			}
		}
	}

	/**
	 * A living entity is about to take damage. Lets every {@link PreHitModifier} take
	 * its note, whoever dealt the damage and whatever they hold: each hit has to set or
	 * clear the note, so it always describes the latest one.
	 */
	public static void beforeDamage(LivingEntity victim) {
		for (PreHitModifier trait : PreHit.TRAITS) {
			trait.beforeDamage(victim);
		}
	}

	/**
	 * The wearer of this armor piece is about to take {@code damage}. Returns what they
	 * should take after the piece's traits.
	 */
	public static float wearerHurt(ItemStack armor, LivingEntity wearer, DamageSource source, float damage) {
		Level level = wearer.level();
		if (level.isClientSide() || LootUtils.isRolling(armor)) {
			return damage;
		}

		for (Modifier trait : traitsOf(armor)) {
			if (trait instanceof WearerHurtModifier onHurt) {
				float incoming = damage;
				damage = call(armor, trait, level, () -> onHurt.onWearerHurt(armor, wearer, source, incoming));
			}
		}
		return damage;
	}

	/** The combined multiplier the gear's stat traits apply to dig speed and defense. Either side. */
	public static float statMultiplier(ItemStack gear) {
		if (LootUtils.isRolling(gear)) {
			return 1.0f;
		}

		float multiplier = 1.0f;
		for (Modifier trait : traitsOf(gear)) {
			if (trait instanceof StatsModifier stats) {
				multiplier *= stats.getStats(gear);
			}
		}
		return multiplier;
	}

	/**
	 * Spends durability on a tool, giving its Unbreaking trait a roll per point first.
	 * Every durability cost a tool pays outside {@link #hit} and {@link #blockBroken}
	 * goes through here - stripping and pathing, the right-click traits, the extra
	 * blocks area traits break - so a maxed "Unbreakable" tool really is. Never call
	 * {@code hurtAndBreak} on loot gear directly.
	 *
	 * <p>Each call loads the gear's traits to find Unbreaking, as the old
	 * {@code damageGear} did, also when a trait calls it from inside its own hook. Only
	 * the one point {@link #hit} and {@link #blockBroken} charge is spent with the traits
	 * the dispatch already holds.
	 */
	public static void spendDurability(ItemStack tool, int points, @Nullable LivingEntity holder,
			EquipmentSlot slot) {
		if (holder != null && points > 0 && !tool.isEmpty() && !LootUtils.isRolling(tool)) {
			spend(tool, traitsOf(tool), points, holder, slot);
		}
	}

	/**
	 * Worn armor is damaged inside vanilla, which asks here how much of
	 * {@code durabilityDamage} the piece should really take. Unbreaking gets a roll per
	 * point, as it does for a tool. Both loaders charge the whole points of what comes
	 * back, so those are the points that are rolled. A piece still in its reveal roll
	 * gets no roll: it is charged what vanilla asked for.
	 */
	public static float armorDurability(ItemStack armor, Level level, float durabilityDamage) {
		if (LootUtils.isRolling(armor)) {
			return durabilityDamage;
		}

		Unbreaking unbreaking = unbreakingOn(traitsOf(armor));
		return unbreaking == null ? durabilityDamage : unbreaking.remaining((int) durabilityDamage, level);
	}

	/** The gear's traits. {@link LootUtils#getModifiers} leaves out the config-disabled ones. */
	private static List<Modifier> traitsOf(ItemStack gear) {
		// The key check skips deserializing anything for gear with no traits, which is
		// most of what the tick sees.
		return GearTags.has(gear, Modifier.MODTAG) ? LootUtils.getModifiers(gear) : List.of();
	}

	private static void run(ItemStack gear, Modifier trait, Level level, Runnable hook) {
		call(gear, trait, level, () -> {
			hook.run();
			return null;
		});
	}

	/**
	 * Runs one trait's hook and, on the server, writes the trait back to the gear if the
	 * hook changed what it stores. Comparing what the trait would store before and after
	 * keeps an untouched trait from writing: a write to a held stack resets the block
	 * its holder is mining, and would rewrite legacy keys the trait still reads.
	 */
	private static <R> R call(ItemStack gear, Modifier trait, Level level, Supplier<R> hook) {
		if (level.isClientSide()) {
			return hook.get();
		}

		CompoundTag before = trait.toNBT();
		R result = hook.get();
		if (!before.equals(trait.toNBT())) {
			LootUtils.updateModifier(gear, trait);
		}
		return result;
	}

	private static void spend(ItemStack gear, List<Modifier> traits, int points, LivingEntity holder,
			EquipmentSlot slot) {
		// A hook may have used the gear up before its own event's point is spent.
		if (gear.isEmpty()) {
			return;
		}

		Unbreaking unbreaking = unbreakingOn(traits);
		if (unbreaking != null) {
			points = unbreaking.remaining(points, holder.level());
		}

		if (points > 0) {
			gear.hurtAndBreak(points, holder, broken -> broken.broadcastBreakEvent(slot));
		}
	}

	/** The gear's Unbreaking trait, or null. A trait appears at most once on a piece of gear. */
	@Nullable
	private static Unbreaking unbreakingOn(List<Modifier> traits) {
		for (Modifier trait : traits) {
			if (trait instanceof Unbreaking unbreaking) {
				return unbreaking;
			}
		}
		return null;
	}

	/** The registry's pre-hit traits, found once: {@link #beforeDamage} runs for every damage event on the server. */
	private static final class PreHit {
		static final List<PreHitModifier> TRAITS = ModifierRegistry.getModifiers().values().stream()
				.filter(PreHitModifier.class::isInstance).map(PreHitModifier.class::cast).toList();
	}
}
