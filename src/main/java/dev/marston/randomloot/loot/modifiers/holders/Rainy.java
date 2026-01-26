package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;

public class Rainy implements HoldModifier {

	private String name;
	private float power;
	private final static String POWER = "power";

	public Rainy(String name, float power) {
		this.name = name;
		this.power = power;
	}

	public Rainy() {
		this.name = "Rainy";
		this.power = 4.0f;
	}

	public Modifier clone() {
		return new Rainy();
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
		return new Rainy(
			tag.hasKey(NAME) ? tag.getString(NAME) : "Rainy",
			tag.hasKey(POWER) ? tag.getFloat(POWER) : 4.0f);
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "rainy";
	}

	@Override
	public String color() {
		return TextFormatting.BLUE.getFriendlyName();
	}

	@Override
	public String description() {
		return "While holding the tool in the rain, mine faster!";
	}

	@Override
	public void writeToLore(List<String> list, boolean shift) {
		String comp = Modifier.formatText(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.PICKAXE) || type.equals(ToolType.AXE) || type.equals(ToolType.SHOVEL);
	}

	@Override
	public void hold(ItemStack stack, World world, Entity holder) {
		if (world.isRainingAt(holder.getPosition())) {
			PotionEffect haste = new PotionEffect(MobEffects.HASTE, 3, 2, false, false);

			if (holder instanceof EntityLivingBase) {
				EntityLivingBase le = (EntityLivingBase) holder;
				le.addPotionEffect(haste);
			}
			if (holder instanceof EntityPlayer) {
				EntityPlayer p = (EntityPlayer) holder;
				p.addPotionEffect(haste);
			}
		}

	}
}
