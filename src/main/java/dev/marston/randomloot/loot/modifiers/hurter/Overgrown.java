package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.BiomeRestrictedModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityCaveSpider;
import net.minecraft.entity.monster.EntityEndermite;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;

public class Overgrown implements EntityHurtModifier, HoldModifier, BiomeRestrictedModifier {

	private String name;
	private int level = 0;
	private final static String LEVEL = "trait_level";

	public Overgrown(String name, int level) {
		this.name = name;
		this.level = level;
	}

	public Overgrown() {
		this.name = "Overgrown";
		this.level = 0;
	}

	public Modifier clone() {
		return new Overgrown();
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
		return new Overgrown(
			tag.hasKey(NAME) ? tag.getString(NAME) : "Overgrown",
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
		return "overgrown";
	}

	@Override
	public String color() {
		return TextFormatting.GREEN.getFriendlyName();
	}

	@Override
	public String description() {
		float bonusDamage = 2.5f + (this.level * 2.5f);
		return "Grants poison immunity. Deals " + bonusDamage + " bonus damage to arthropods.";
	}

	@Override
	public void writeToLore(List<String> list, boolean shift) {
		String comp = Modifier.formatText(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.SWORD) || type.equals(ToolType.AXE) || type.equals(ToolType.SHOVEL);
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, EntityLivingBase hurtee, EntityLivingBase hurter) {
		boolean isArthropod = hurtee instanceof EntitySpider ||
							  hurtee instanceof EntityCaveSpider ||
							  hurtee instanceof EntitySilverfish ||
							  hurtee instanceof EntityEndermite;

		if (isArthropod) {
			float bonusDamage = 2.5f + (this.level * 2.5f);
			hurtee.attackEntityFrom(DamageSource.causeMobDamage(hurter), bonusDamage);
			hurtee.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, this.level));
		}

		return false;
	}

	@Override
	public void hold(ItemStack stack, World world, Entity holder) {
		if (!(holder instanceof EntityLivingBase)) return;
		EntityLivingBase living = (EntityLivingBase) holder;
		if (living.isPotionActive(MobEffects.POISON)) {
			living.removePotionEffect(MobEffects.POISON);
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
			biomeKey.contains("jungle") ||
			biomeKey.contains("swamp") ||
			biomeKey.contains("bamboo")
		);
	}
}
