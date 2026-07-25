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


public class EarlyBird extends LeveledModifier implements EntityHurtModifier {

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

		float dmg = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));
		float currentHealth = hurtee.getHealth();
		float maxHealth = hurtee.getMaxHealth();

		// Check if target was at full health before this attack
		// Since damage is already applied, check if current health + base damage >= max health
		if (currentHealth + dmg >= maxHealth * 0.95f) {
			dealBonusDamage(hurtee, hurter, dmg * getBonusDamage());
		}

		return false;
	}
}
