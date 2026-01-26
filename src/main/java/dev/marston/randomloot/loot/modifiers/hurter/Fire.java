package dev.marston.randomloot.loot.modifiers.hurter;

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

public class Fire implements EntityHurtModifier {
	private String name;
	private int points;
	private final static String POINTS = "points";

	public Fire(String name, int points) {
		this.name = name;
		this.points = points;
	}

	public Fire() {
		this.name = "Flaming";
		this.points = 2;
	}

	public Modifier clone() {
		return new Fire();
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
		return new Fire(
			tag.hasKey(NAME) ? tag.getString(NAME) : "Flaming",
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
		return "flaming";
	}

	@Override
	public String color() {
		return TextFormatting.RED.getFriendlyName();
	}

	@Override
	public String description() {
		return "Sets enemy on fire for " + this.points + " seconds.";
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
		hurtee.setFire(points);
		return false;
	}

	public boolean canLevel() {
		return this.points < 5;
	}

	public void levelUp() {
		this.points++;
	}
}
