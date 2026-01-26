package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;

public class Effect implements HoldModifier {

	private String name;
	private int power;
	private String tagname;
	private final static String POWER = "power";
	private Potion effect;
	private int duration;

	public Effect(String name, String tagname, int power, int duration, Potion effect) {
		this.name = name;
		this.effect = effect;
		this.power = 0;
		this.tagname = tagname;
		this.duration = duration;
	}

	public Effect(String name, String tagname, int duration, Potion effect) {
		this(name, tagname, 0, duration, effect);
	}

	public Modifier clone() {
		return new Effect(this.name, this.tagname, this.power, this.duration, this.effect);
	}

	@Override
	public NBTTagCompound toNBT() {

		NBTTagCompound tag = new NBTTagCompound();

		tag.setString(NAME, name);
		tag.setInteger(POWER, power);

		return tag;
	}

	@Override
	public Modifier fromNBT(NBTTagCompound tag) {
		return new Effect(
			tag.hasKey(NAME) ? tag.getString(NAME) : this.name,
			this.tagname,
			tag.hasKey(POWER) ? tag.getInteger(POWER) : 0,
			this.duration,
			this.effect);
	}

	@Override
	public String name() {
		if (this.power == 0) {
			return name;
		}
		return name + " " + LootUtils.roman(this.power + 1);
	}

	@Override
	public String tagName() {
		return tagname;
	}

	@Override
	public String color() {
		int color = effect.getLiquidColor();
		// Convert potion color to a reasonable TextFormatting
		// This is approximate - potion colors don't map directly to TextFormatting
		return TextFormatting.LIGHT_PURPLE.getFriendlyName();
	}

	@Override
	public String description() {
		return "While holding the tool, get the " + effect.getName().toLowerCase()
				+ " " + LootUtils.roman(this.power + 1) + " effect.";
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
		PotionEffect eff = new PotionEffect(effect, duration * 20, this.power, false, false);

		if (!(holder instanceof EntityLivingBase)) {
			return;
		}

		EntityLivingBase livingHolder = (EntityLivingBase) holder;
		boolean alreadyHasEffect = livingHolder.isPotionActive(effect);
		if (!alreadyHasEffect) {
			livingHolder.addPotionEffect(eff);
		}

	}

	public boolean canLevel() {
		return this.power < 4;
	}

	public void levelUp() {
		this.power++;
	}
}
