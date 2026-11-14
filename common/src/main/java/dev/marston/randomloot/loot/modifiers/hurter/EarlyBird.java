package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.PreHitModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class EarlyBird extends AbstractModifier implements PreHitModifier {
	/**
	 * Entities whose latest hit found them at full health. {@code hurtEnemy} runs after
	 * the hit has landed, so the health it sees is already reduced; this is recorded
	 * from the pre-damage hook instead. Server thread only; weak keys let dead and
	 * unloaded entities go.
	 */
	private static final Set<LivingEntity> FULL_HEALTH_HITS = Collections.newSetFromMap(new WeakHashMap<>());

	/**
	 * Every hit either sets or clears the victim's entry, so it always describes the
	 * latest one: a full-health hit earlier in the same tick must not vouch for a later
	 * hit on the now-wounded target. Other traits' bonus hits count as hits here too,
	 * which is why a {@link PreHitModifier} runs before the tool's other traits.
	 */
	@Override
	public void beforeDamage(LivingEntity victim) {
		// Exactly full. The old 95% allowance only existed to absorb the error in
		// estimating pre-hit health after the fact; this is the real value.
		if (victim.getHealth() >= victim.getMaxHealth()) {
			FULL_HEALTH_HITS.add(victim);
		} else {
			FULL_HEALTH_HITS.remove(victim);
		}
	}

	/** Reads and clears the mark: one full-health hit vouches for one swing. */
	private static boolean wasAtFullHealth(LivingEntity victim) {
		return FULL_HEALTH_HITS.remove(victim);
	}

	private float getBonusDamage() {
		// Level 1: 15%, Level 2: 25%, Level 3: 40%
		switch (traitLevel()) {
			case 1: return 0.15f;
			case 2: return 0.25f;
			case 3: return 0.40f;
			default: return 0.15f;
		}
	}

	@Override
	public String description() {
		return "Deals " + String.format("%.0f", getBonusDamage() * 100) + "% extra damage to full-health targets";
	}

	@Override
	public void hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		// The hit has already landed, so ask what the pre-damage hook saw. Adding the
		// tool's base damage back onto the current health used to stand in for this, but
		// that undercounts any hit that crits or carries Sharpness/Strength, and those
		// then failed the check against a genuinely untouched target.
		if (wasAtFullHealth(hurtee)) {
			float dmg = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));
			dealBonusDamage(hurtee, hurter, dmg * getBonusDamage());
		}
	}
}
