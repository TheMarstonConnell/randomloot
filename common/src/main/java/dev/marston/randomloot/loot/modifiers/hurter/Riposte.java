package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.ChargeTracker;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import dev.marston.randomloot.loot.modifiers.WearerHurtModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Riposte: taking a hit while wielding the weapon "primes" a counter-attack.
 * The next melee strike within {@link #WINDOW_SECONDS} seconds deals bonus
 * damage, then the charge is spent. Rides both the wielder-hurt path — to prime,
 * via {@link WearerHurtModifier} (see {@code ArmorDispatcher} scanning held loot
 * tools, not just worn armor) — and the attack path, to spend, via
 * {@link EntityHurtModifier}, on the same weapon stack.
 */
public class Riposte extends AbstractModifier implements WearerHurtModifier, EntityHurtModifier {

	/** Seconds after taking a hit that the counter stays primed. */
	private static final int WINDOW_SECONDS = 5;
	/** Bonus damage as a fraction of the weapon's base attack damage. */
	private static final float BONUS_MULTIPLIER = 1.5f;

	/** Game time the counter was primed; {@code 0} (or long past) means unprimed. */
	private long primed;

	public Riposte(long primed) {
		this.name = "Riposte";
		this.primed = primed;
	}

	public Riposte() {
		this(0L);
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = super.toNBT();
		tag.putLong(ModifierConstants.CHARGED, primed);
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Riposte(tag.getLongOr(ModifierConstants.CHARGED, 0L));
	}

	@Override
	public String tagName() {
		return "riposte";
	}

	@Override
	public ChatFormatting color() {
		return ChatFormatting.AQUA;
	}

	@Override
	public String description() {
		return "After taking a hit, your next strike within " + WINDOW_SECONDS + " seconds deals bonus damage.";
	}

	@Override
	public boolean forTool(ToolType type) {
		return isWeapon(type);
	}

	/** True while the counter is still within its window after being primed. */
	private boolean isPrimed(Level level) {
		return primed != 0L && ChargeTracker.getCharge(level, primed, WINDOW_SECONDS) < 1.0f;
	}

	@Override
	public float onWearerHurt(ItemStack stack, LivingEntity wearer, DamageSource source, float damage) {
		Level level = wearer.level();
		if (!level.isClientSide()) {
			this.primed = level.getGameTime();
			LootUtils.updateModifier(stack, this);
		}
		// Purely reactive: Riposte counters, it does not soften the incoming hit.
		return damage;
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		Level level = hurtee.level();
		if (level.isClientSide()) {
			return false;
		}

		if (isPrimed(level)) {
			float base = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));
			dealBonusDamage(hurtee, hurter, base * BONUS_MULTIPLIER);
			Modifier.TrackEntityParticle(level, hurtee, ParticleTypes.ENCHANTED_HIT);

			this.primed = 0L;
			LootUtils.updateModifier(itemstack, this);
		}

		return false;
	}

	@Override
	public Component writeDetailsToLore(Level level) {
		if (level == null) {
			return null;
		}
		if (isPrimed(level)) {
			return Modifier.makeComp("Primed!", ChatFormatting.AQUA);
		}
		return Modifier.makeComp("Ready", ChatFormatting.GRAY);
	}
}
