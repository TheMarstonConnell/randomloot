package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.StatsModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;


public class Fierce extends AbstractModifier implements EntityHurtModifier, StatsModifier {

	public Fierce() {
		this("Fierce");
	}

	public Fierce(String name) {
		this.name = name;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Fierce(tag.getStringOr(NAME, "Fierce"));
	}

	@Override
	public String tagName() {
		return "fierce";
	}

	@Override
	public ChatFormatting color() {
		return ChatFormatting.RED;
	}

	@Override
	public String description() {
		return "Deals more damage as durability decreases";
	}

	@Override
	public boolean forTool(ToolType type) {
		// The damage scaling only fires from melee hooks; on armor only the hidden
		// stats multiplier would apply, which contradicts the description.
		return !type.isArmor();
	}

	private float getDamageMultiplier(ItemStack itemstack) {
		float maxDamage = itemstack.getMaxDamage();
		if (maxDamage <= 0) return 1.0f;

		float currentDamage = itemstack.getDamageValue();
		float ratio = currentDamage / maxDamage;

		return 1.0f + (ratio * 0.5f);
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		if (hurtee.level().isClientSide()) {
			return false;
		}

		float multiplier = getDamageMultiplier(itemstack);
		if (multiplier <= 1.0f) {
			return false;
		}

		float baseDamage = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));
		dealBonusDamage(hurtee, hurter, baseDamage * (multiplier - 1.0f));

		return false;
	}

	@Override
	public float getStats(ItemStack itemstack) {
		float maxDamage = itemstack.getMaxDamage();
		if (maxDamage <= 0) return 1.0f;

		float currentDamage = itemstack.getDamageValue();
		float ratio = currentDamage / maxDamage;

		return 1.0f + (ratio * 0.25f);
	}
}
