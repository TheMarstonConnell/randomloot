package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.BiomeRestrictedModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
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

public class Scorched implements EntityHurtModifier, HoldModifier, BiomeRestrictedModifier {

	private String name;
	private int level = 0;
	private final static String LEVEL = "trait_level";

	public Scorched(String name, int level) {
		this.name = name;
		this.level = level;
	}

	public Scorched() {
		this.name = "Scorched";
		this.level = 0;
	}

	public Modifier clone() {
		return new Scorched();
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
		return new Scorched(
			tag.hasKey(NAME) ? tag.getString(NAME) : "Scorched",
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
		return "scorched";
	}

	@Override
	public String color() {
		return TextFormatting.GOLD.getFriendlyName();
	}

	@Override
	public String description() {
		int duration = 4 + (this.level * 2);
		return "Sets enemies on fire for " + duration + " seconds. Grants fire resistance while held.";
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
		int fireDuration = 4 + (this.level * 2);
		hurtee.setFire(fireDuration);
		return false;
	}

	@Override
	public void hold(ItemStack stack, World world, Entity holder) {
		if (!(holder instanceof EntityLivingBase)) return;
		EntityLivingBase living = (EntityLivingBase) holder;
		living.addPotionEffect(new PotionEffect(MobEffects.FIRE_RESISTANCE, 60, 0, false, false));
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
		return temperature >= 1.0f || (dimension != null && dimension.equals("minecraft:the_nether"));
	}
}
