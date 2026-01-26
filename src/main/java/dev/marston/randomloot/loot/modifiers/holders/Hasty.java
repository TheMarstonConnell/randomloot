package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
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

public class Hasty implements HoldModifier {

	private String name;
	private int power;
	private final static String POWER = "power";
	private final static String LEVEL = "trait_level";

	private int level = 0;

	public Hasty(String name, int power, int level) {
		this.name = name;
		this.power = power;
		this.level = level;
	}

	public Hasty() {
		this.name = "Hasty";
		this.power = 0;
		this.level = 0;
	}

	public Modifier clone() {
		return new Hasty();
	}

	@Override
	public NBTTagCompound toNBT() {

		NBTTagCompound tag = new NBTTagCompound();

		tag.setString(NAME, name);

		tag.setInteger(POWER, power);

		tag.setInteger(LEVEL, level);

		return tag;
	}

	@Override
	public Modifier fromNBT(NBTTagCompound tag) {
		return new Hasty(
			tag.hasKey(NAME) ? tag.getString(NAME) : "Hasty",
			tag.hasKey(POWER) ? tag.getInteger(POWER) : 0,
			tag.hasKey(LEVEL) ? tag.getInteger(LEVEL) : 0);
	}

	@Override
	public String name() {

		if (this.level == 0) {
			return this.name;
		}

		return this.name + " " + LootUtils.roman(this.level + 1);

	}

	@Override
	public String tagName() {
		return "hasty";
	}

	@Override
	public String color() {
		return TextFormatting.BLUE.getFriendlyName();
	}

	@Override
	public String description() {
		return "While holding the tool, get the Haste " + LootUtils.roman(this.level + 1) + " effect.";
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
		PotionEffect haste = new PotionEffect(MobEffects.HASTE, 2, power, true, false);

		if (holder instanceof EntityLivingBase) {
			EntityLivingBase le = (EntityLivingBase) holder;
			le.addPotionEffect(haste);
		}
		if (holder instanceof EntityPlayer) {
			EntityPlayer p = (EntityPlayer) holder;
			p.addPotionEffect(haste);
		}

	}

	public boolean canLevel() {
		return level == 0;
	}

	public void levelUp() {
		this.level++;
		this.power++;
		return;
	}
}
