package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;

public class HurtEffect implements EntityHurtModifier {

	private String name;
	private int power;
	private String tagname;
	private final static String POWER = "power";
	private Potion effect;
	private int duration;

	public HurtEffect(String name, String tagname, int power, int duration, Potion effect) {
		this.name = name;
		this.effect = effect;
		this.power = power;
		this.tagname = tagname;
		this.duration = duration;
	}

	public HurtEffect(String name, String tagname, int duration, Potion effect) {
		this(name, tagname, 0, duration, effect);
	}

	public Modifier clone() {
		return new HurtEffect(this.name, this.tagname, this.duration, this.effect);
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
		return new HurtEffect(
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
		return TextFormatting.LIGHT_PURPLE.getFriendlyName();
	}

	@Override
	public String description() {
		return "When attacking with tool, apply the " + effect.getName().toLowerCase()
				+ " " + LootUtils.roman(this.power + 1) + " effect to the target for " + this.duration + " seconds.";
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
		PotionEffect eff = new PotionEffect(effect, duration * 20, power, false, false);

		hurtee.addPotionEffect(eff);
		return false;
	}

	public boolean canLevel() {
		return this.power < 4;
	}

	public void levelUp() {
		this.power++;
	}
}
