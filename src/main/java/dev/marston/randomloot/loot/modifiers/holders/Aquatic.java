package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.BiomeRestrictedModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;

public class Aquatic implements HoldModifier, BiomeRestrictedModifier {

	private String name;
	private int level = 0;
	private final static String LEVEL = "trait_level";

	public Aquatic(String name, int level) {
		this.name = name;
		this.level = level;
	}

	public Aquatic() {
		this.name = "Aquatic";
		this.level = 0;
	}

	public Modifier clone() {
		return new Aquatic();
	}

	@Override
	public NBTTagCompound toNBT() {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setString(NAME, name);
		tag.setInteger(LEVEL, level);
		return tag;
	}

	@Override
	public Modifier fromNBT(NBTTagCompound tag) {
		return new Aquatic(
			tag.hasKey(NAME) ? tag.getString(NAME) : "Aquatic",
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
		return "aquatic";
	}

	@Override
	public String color() {
		return TextFormatting.AQUA.getFriendlyName();
	}

	@Override
	public String description() {
		return "Grants water breathing and Haste " + LootUtils.roman(this.level + 2) + " when underwater.";
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
		if (!(holder instanceof EntityLivingBase)) return;
		EntityLivingBase living = (EntityLivingBase) holder;

		// Water breathing
		living.addPotionEffect(new PotionEffect(MobEffects.WATER_BREATHING, 40, 0, false, false));

		// Extra haste when underwater
		if (living.isInWater()) {
			living.addPotionEffect(new PotionEffect(MobEffects.HASTE, 40, this.level + 1, true, false));
		}
	}

	@Override
	public boolean canLevel() {
		return level < 2; // Max level 3 (0, 1, 2)
	}

	@Override
	public void levelUp() {
		this.level++;
	}

	@Override
	public boolean canSpawnInBiome(String biomeKey, float temperature, String dimension) {
		return biomeKey != null && (
			biomeKey.contains("ocean") ||
			biomeKey.contains("river")
		);
	}
}
