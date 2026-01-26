package dev.marston.randomloot.loot.modifiers.breakers;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;

public class Learning implements BlockBreakModifier {

	private String name;
	private int count;
	private final static String COUNT = "count";
	private static final int max = 10;
	private int points;
	private final static String POINTS = "points";

	public Learning(String name, int count, int points) {
		this.name = name;
		this.count = count;
		this.points = points;
	}

	public Learning() {
		this.name = "Learning";
		this.count = 0;
		this.points = 3;
	}

	public Modifier clone() {
		return new Learning();
	}

	@Override
	public boolean startBreak(ItemStack itemstack, BlockPos pos, EntityLivingBase entity) {

		if (!(entity instanceof EntityPlayer)) {
			return false;
		}

		EntityPlayer player = (EntityPlayer) entity;

		this.count++;

		while (count >= max) {
			count = count - max;
			player.addExperience(this.points);
		}

		LootUtils.updateModifier(itemstack, this);
		return false;
	}

	@Override
	public NBTTagCompound toNBT() {

		NBTTagCompound tag = new NBTTagCompound();

		tag.setInteger(COUNT, count);
		tag.setInteger(POINTS, points);
		tag.setString(NAME, name);

		return tag;
	}

	@Override
	public Modifier fromNBT(NBTTagCompound tag) {
		return new Learning(
			tag.hasKey(NAME) ? tag.getString(NAME) : "Learning",
			tag.hasKey(COUNT) ? tag.getInteger(COUNT) : 0,
			tag.hasKey(POINTS) ? tag.getInteger(POINTS) : 3);
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "learning";
	}

	@Override
	public String color() {
		return "green";
	}

	@Override
	public String description() {
		return "After breaking " + max + " blocks as allowed by this tool, gain " + points + " experience points.";
	}

	@Override
	public void writeToLore(List<String> list, boolean shift) {
		String comp = Modifier.formatText(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public String writeDetailsToLore(World world) {
		float amt = ((float) count) / ((float) max) * 100;
		String perc = String.format("%.0f%% Learned", amt);
		return Modifier.formatText(perc, TextFormatting.GRAY);
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.PICKAXE) || type.equals(ToolType.AXE) || type.equals(ToolType.SHOVEL);
	}
}
