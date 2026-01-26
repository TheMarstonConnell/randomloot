package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.ChargeTracker;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;

public class Charging implements EntityHurtModifier {
	private String name;
	private int points;
	private long charged;

	public Charging(String name, int points, long charged) {
		this.name = name;
		this.points = points;
		this.charged = charged;
	}

	public Charging() {
		this.name = "Charged";
		this.points = 10;
		this.charged = 0;
	}

	public Modifier clone() {
		return new Charging();
	}

	@Override
	public NBTTagCompound toNBT() {

		NBTTagCompound tag = new NBTTagCompound();

		tag.setInteger(ModifierConstants.POINTS, points);
		tag.setString(ModifierConstants.NAME, name);
		tag.setLong(ModifierConstants.CHARGED, charged);
		return tag;
	}

	@Override
	public Modifier fromNBT(NBTTagCompound tag) {
		return new Charging(
			tag.hasKey(ModifierConstants.NAME) ? tag.getString(ModifierConstants.NAME) : "Charged",
			tag.hasKey(ModifierConstants.POINTS) ? tag.getInteger(ModifierConstants.POINTS) : 10,
			tag.hasKey(ModifierConstants.CHARGED) ? tag.getLong(ModifierConstants.CHARGED) : 0L);
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "charged";
	}

	@Override
	public String color() {
		return TextFormatting.YELLOW.getFriendlyName();
	}

	@Override
	public String description() {
		return "After " + this.points
				+ " seconds, hitting and enemy will summon a lightning bolt and empty the charge meter.";
	}

	@Override
	public void writeToLore(List<String> list, boolean shift) {
		String comp = Modifier.formatText(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public String writeDetailsToLore(World world) {
		if (world != null) {
			float charge = ChargeTracker.getCharge(world, charged, points);

			String perc = String.format("%.0f%% Charged", charge * 100.0f);

			return Modifier.formatText(perc, TextFormatting.GREEN);
		}

		return null;
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.SWORD) || type.equals(ToolType.AXE);
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, EntityLivingBase hurtee, EntityLivingBase hurter) {

		World world = hurtee.world;

		long time = world.getTotalWorldTime();

		if (ChargeTracker.getCharge(world, charged, points) >= 1.0f) {
			EntityLightningBolt lb = new EntityLightningBolt(world, hurtee.posX, hurtee.posY, hurtee.posZ, false);

			world.spawnEntity(lb);

			this.charged = time;
			LootUtils.updateModifier(itemstack, this);
		}

		return false;

	}
}
