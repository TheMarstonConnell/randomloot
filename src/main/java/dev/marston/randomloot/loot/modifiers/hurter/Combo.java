package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.ChargeTracker;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;

public class Combo implements EntityHurtModifier {
	private String name;
	private int points;
	private long charged;

	public Combo(String name, int points, long charged) {
		this.name = name;
		this.points = points;
		this.charged = charged;
	}

	public Combo() {
		this.name = "Dexterous";
		this.points = 2;
		this.charged = 0;
	}

	public Modifier clone() {
		return new Combo();
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
		return new Combo(
			tag.hasKey(ModifierConstants.NAME) ? tag.getString(ModifierConstants.NAME) : "Dexterous",
			tag.hasKey(ModifierConstants.POINTS) ? tag.getInteger(ModifierConstants.POINTS) : 2,
			tag.hasKey(ModifierConstants.CHARGED) ? tag.getLong(ModifierConstants.CHARGED) : 0L);
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "combo";
	}

	@Override
	public String color() {
		return TextFormatting.YELLOW.getFriendlyName();
	}

	@Override
	public String description() {
		return "Hitting enemies within " + this.points + " seconds after hitting them deals an extra 25% damage.";
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

			String s = "Not Ready";
			if (charge < 1.0f) {
				s = "Ready";
			}

			return Modifier.formatText(s, TextFormatting.RED);
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

		if (ChargeTracker.getCharge(world, charged, points) < 1.0f) {

			float damage = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));

			hurtee.attackEntityFrom(DamageSource.causeMobDamage(hurtee), damage * 0.25f);

			Modifier.TrackEntityParticle(world, hurtee, EnumParticleTypes.CRIT);

		}

		this.charged = time;
		LootUtils.updateModifier(itemstack, this);

		return false;
	}
}
