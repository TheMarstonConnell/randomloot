package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.LeveledModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.WeakHashMap;


public class EarlyBird extends LeveledModifier implements EntityHurtModifier {

	/**
	 * Entities whose latest hit found them at full health, with the game time it
	 * happened. {@code hurtEnemy} runs after the hit has landed, so the health it sees
	 * is already reduced; this is recorded from the pre-damage hook instead. Server
	 * thread only; weak keys let dead and unloaded entities go.
	 */
	private static final Map<LivingEntity, Long> FULL_HEALTH_HITS = new WeakHashMap<>();

	/**
	 * Called from the pre-damage hook, before the incoming damage is applied. Every hit
	 * either sets or clears the victim's entry, so it always describes the latest one: a
	 * full-health hit earlier in the same tick must not vouch for a later hit on the
	 * now-wounded target. Other traits' bonus hits count as hits here too, which is why
	 * {@code LootItem.hurtEnemy} runs Early Bird before the tool's other traits.
	 */
	public static void notePreDamage(LivingEntity victim) {
		// Exactly full. The old 95% allowance only existed to absorb the error in
		// estimating pre-hit health after the fact; this is the real value.
		if (victim.getHealth() >= victim.getMaxHealth()) {
			FULL_HEALTH_HITS.put(victim, victim.level().getGameTime());
		} else {
			FULL_HEALTH_HITS.remove(victim);
		}
	}

	private static boolean wasAtFullHealth(LivingEntity victim) {
		Long time = FULL_HEALTH_HITS.get(victim);
		return time != null && time == victim.level().getGameTime();
	}

	public EarlyBird(String name, int level) {
		this.name = name;
		this.level = level;
	}

	public EarlyBird() {
		this("Early Bird", 1);
	}

	@Override
	protected int minLevel() {
		return 1;
	}

	@Override
	protected int maxLevel() {
		return 3;
	}

	private float getBonusDamage() {
		// Level 1: 15%, Level 2: 25%, Level 3: 40%
		switch (level) {
			case 1: return 0.15f;
			case 2: return 0.25f;
			case 3: return 0.40f;
			default: return 0.15f;
		}
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new EarlyBird(tag.getStringOr(NAME, "Early Bird"), ModifierConstants.getLevel(tag, 1));
	}

	@Override
	public String tagName() {
		return "early_bird";
	}

	@Override
	public ChatFormatting color() {
		return ChatFormatting.YELLOW;
	}

	@Override
	public String description() {
		return "Deals " + String.format("%.0f", getBonusDamage() * 100) + "% extra damage to full-health targets";
	}

	@Override
	public boolean forTool(ToolType type) {
		return isWeapon(type);
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		if (hurtee.level().isClientSide()) {
			return false;
		}

		// The hit has already landed, so ask what the pre-damage hook saw. Adding the
		// tool's base damage back onto the current health used to stand in for this, but
		// that undercounts any hit that crits or carries Sharpness/Strength, and those
		// then failed the check against a genuinely untouched target.
		if (wasAtFullHealth(hurtee)) {
			float dmg = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));
			dealBonusDamage(hurtee, hurter, dmg * getBonusDamage());
		}

		return false;
	}
}
