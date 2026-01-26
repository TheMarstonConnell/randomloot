package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;

public class Draining implements EntityHurtModifier {
	private String name;
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

	public Modifier clone() {
		return new Draining();
	}

	@Override
	public NBTTagCompound toNBT() {

		NBTTagCompound tag = new NBTTagCompound();

		tag.setInteger(POINTS, points);
		tag.setString(NAME, name);

		return tag;
	}

	@Override
	public Modifier fromNBT(NBTTagCompound tag) {
		return new Draining(
			tag.hasKey(NAME) ? tag.getString(NAME) : "Necrotic",
			tag.hasKey(POINTS) ? tag.getInteger(POINTS) : 2);
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
		return TextFormatting.RED.getFriendlyName();
	}

	@Override
	public String description() {
		return "Heals " + String.format("%.0f", drain() * 100) + "% of damage dealt to target.";
	}

	public float drain() {
		return ((float) this.points) * 0.05f;
	}

	@Override
	public void writeToLore(List<String> list, boolean shift) {
		String comp = Modifier.formatText(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.SWORD) || type.equals(ToolType.AXE);
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, EntityLivingBase hurtee, EntityLivingBase hurter) {
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
