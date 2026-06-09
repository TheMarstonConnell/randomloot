package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;


public class Bezerk extends AbstractModifier implements EntityHurtModifier {

	/**
	 * Bonus damage per "missing health ratio": bonus = attack * SCALE * (max/current - 1).
	 * At half health that's a 5% bonus; near death it approaches +95% (20 max HP).
	 */
	private static final float BONUS_DAMAGE_SCALE = 0.05f;

	public Bezerk(String name) {
		this.name = name;
	}

	public Bezerk() {
		this.name = "Bezerk";
	}

	public Modifier clone() {
		return new Bezerk();
	}

	@Override
	public CompoundTag toNBT() {

		CompoundTag tag = new CompoundTag();

		tag.putString(NAME, name);

		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Bezerk(tag.getStringOr(NAME, "Bezerk"));
	}

	@Override
	public String tagName() {
		return "bezerk";
	}

	@Override
	public String color() {
		return ChatFormatting.GOLD.getName();
	}

	@Override
	public String description() {
		return "Deals more damage at lower player health.";
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

		float maxHealth = hurter.getMaxHealth();
		float currentHealth = Math.max(hurter.getHealth(), 1.0f);

		float ratio = maxHealth / currentHealth;

		dealBonusDamage(hurtee, hurter, dmg * BONUS_DAMAGE_SCALE * (ratio - 1));

		return false;
	}
}
