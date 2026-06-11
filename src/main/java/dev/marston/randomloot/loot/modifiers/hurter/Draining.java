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


public class Draining extends AbstractModifier implements EntityHurtModifier {
	private int points;
	private final static String POINTS = "points";

	public Draining(String name, int points) {
		this.name = name;
		this.points = points;
	}

	public Draining() {
		this.name = "Necrotic";
		this.points = 2;
	}

	@Override
	public CompoundTag toNBT() {

		CompoundTag tag = new CompoundTag();

		tag.putInt(POINTS, points);
		tag.putString(NAME, name);

		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Draining(tag.getStringOr(NAME, "Necrotic"), tag.getIntOr(POINTS, 2));
	}

	@Override
	public String name() {
		if (points == 2) {
			return name;
		}
		return name + " " + LootUtils.roman(points - 1);
	}

	@Override
	public String tagName() {
		return "necrotic";
	}

	@Override
	public String color() {
		return ChatFormatting.RED.getName();
	}

	@Override
	public String description() {
		return "Heals " + String.format("%.0f", drain() * 100) + "% of damage dealt to target.";
	}

	public float drain() {
		return ((float) this.points) * 0.05f;
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
		float damage = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));

		hurter.heal(damage * drain());
		return false;
	}

	public boolean canLevel() {
		return this.points < 10;
	}

	public void levelUp() {
		this.points++;
	}
}
