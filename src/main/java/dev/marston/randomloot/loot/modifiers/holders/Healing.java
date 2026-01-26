package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;

public class Healing implements HoldModifier {

	private String name;
	private float power;
	private final static String POWER = "power";

	public Healing(String name, float power) {
		this.name = name;
		this.power = power;
	}

	public Healing() {
		this.name = "Living";
		this.power = 0.005f;
	}

	public Modifier clone() {
		return new Healing();
	}

	@Override
	public NBTTagCompound toNBT() {

		NBTTagCompound tag = new NBTTagCompound();

		tag.setFloat(POWER, power);

		tag.setString(NAME, name);

		return tag;
	}

	@Override
	public Modifier fromNBT(NBTTagCompound tag) {
		return new Healing(
			tag.hasKey(NAME) ? tag.getString(NAME) : "Living",
			tag.hasKey(POWER) ? tag.getFloat(POWER) : 0.005f);
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "living";
	}

	@Override
	public String color() {
		return TextFormatting.GREEN.getFriendlyName();
	}

	@Override
	public String description() {
		return "While holding the tool, it will randomly heal itself";
	}

	@Override
	public void writeToLore(List<String> list, boolean shift) {
		String comp = Modifier.formatText(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public boolean forTool(ToolType type) {
		return true;
	}

	@Override
	public void hold(ItemStack stack, World world, Entity holder) {

		float f = world.rand.nextFloat();
		if (f < power) {

			if (stack.getItemDamage() == 0) {
				return;
			}

			stack.setItemDamage(Math.max(stack.getItemDamage() - 1, 0));

			if (f < power / 5) {
				String text = TextFormatting.GRAY + "" + TextFormatting.ITALIC + "pssst...";

				if (holder instanceof EntityPlayer) {
					EntityPlayer p = (EntityPlayer) holder;
					p.sendMessage(new TextComponentString(text));
				}
			}

		}

	}
}
