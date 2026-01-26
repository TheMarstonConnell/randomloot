package dev.marston.randomloot.loot.modifiers.stats;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.StatsModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;

public class Busted implements StatsModifier {

	private String name;

	public Busted() {
		this("Busted");
	}

	public Busted(String name) {
		this.name = name;
	}

	public Modifier clone() {
		return new Busted(this.name);
	}

	@Override
	public NBTTagCompound toNBT() {

		NBTTagCompound tag = new NBTTagCompound();

		tag.setString(NAME, name);

		return tag;
	}

	@Override
	public Modifier fromNBT(NBTTagCompound tag) {
		return new Busted(tag.hasKey(NAME) ? tag.getString(NAME) : "Busted");
	}

	@Override
	public String name() {

		return name;
	}

	@Override
	public String tagName() {
		return "busted";
	}

	@Override
	public String color() {

		return TextFormatting.LIGHT_PURPLE.getFriendlyName();

	}

	@Override
	public String description() {
		return "Dig speed is increased as tool durability drops.";
	}

	@Override
	public void writeToLore(List<String> list, boolean shift) {
		String comp = Modifier.formatText(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public boolean forTool(ToolType type) {
		return type == ToolType.AXE || type == ToolType.PICKAXE || type == ToolType.SHOVEL;
	}

	@Override
	public float getStats(ItemStack itemstack) {
		float maxDamage = itemstack.getMaxDamage();
		float currentDamage = itemstack.getItemDamage();

		float ratio = currentDamage / maxDamage;

		float maxGoodness = 1.0f;

		return ratio * maxGoodness + 1.0f;

	}
}
