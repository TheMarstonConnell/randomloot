package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;

public class Bezerk implements EntityHurtModifier {
	private String name;

	public Bezerk(String name) {
		this.name = name;
	}

	public Bezerk() {
		this.name = "Bezerk";
	}

	public Modifier clone() {
		return new Bezerk();
	}

	@Override
	public NBTTagCompound toNBT() {

		NBTTagCompound tag = new NBTTagCompound();

		tag.setString(NAME, name);

		return tag;
	}

	@Override
	public Modifier fromNBT(NBTTagCompound tag) {
		return new Bezerk(tag.hasKey(NAME) ? tag.getString(NAME) : "Bezerk");
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "bezerk";
	}

	@Override
	public String color() {
		return TextFormatting.GOLD.getFriendlyName();
	}

	@Override
	public String description() {
		return "Deals more damage at lower player health.";
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
		float dmg = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));

		float maxHealth = hurter.getMaxHealth();
		float currentHealth = hurter.getHealth();

		float ratio = maxHealth / currentHealth;

		float amt = dmg * 0.05f * (ratio - 1);

		if (hurter instanceof EntityPlayer) {
			EntityPlayer p = (EntityPlayer) hurter;
			hurtee.attackEntityFrom(DamageSource.causePlayerDamage(p), amt);
			return false;
		}

		hurtee.attackEntityFrom(DamageSource.causeMobDamage(hurter), amt);

		return false;
	}
}
