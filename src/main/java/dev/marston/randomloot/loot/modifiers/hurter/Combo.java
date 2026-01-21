package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.ChargeTracker;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

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
	public CompoundTag toNBT() {

		CompoundTag tag = new CompoundTag();

		tag.putInt(ModifierConstants.POINTS, points);
		tag.putString(ModifierConstants.NAME, name);
		tag.putLong(ModifierConstants.CHARGED, charged);
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Combo(tag.getStringOr(ModifierConstants.NAME, "Dexterous"), tag.getIntOr(ModifierConstants.POINTS, 2), tag.getLongOr(ModifierConstants.CHARGED, 0L));
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
		return ChatFormatting.YELLOW.getName();
	}

	@Override
	public String description() {
		return "Hitting enemies within " + this.points + " seconds after hitting them deals an extra 25% damage.";
	}

	@Override
	public void writeToLore(List<Component> list, boolean shift) {

		MutableComponent comp = Modifier.makeComp(this.name(), this.color());
		list.add(comp);

	}

	@Override
	public Component writeDetailsToLore(Level level) {
		if (level != null) {
			float charge = ChargeTracker.getCharge(level, charged, points);

			String s = "Not Ready";
			if (charge < 1.0f) {
				s = "Ready";
			}

			return Modifier.makeComp(s, ChatFormatting.RED);
		}

		return null;
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.SWORD) || type.equals(ToolType.AXE);
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {

		Level level = hurtee.level();

		long time = level.getGameTime();

		if (ChargeTracker.getCharge(level, charged, points) < 1.0f) {

			float damage = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));

			hurtee.hurt(hurter.damageSources().mobAttack(hurtee), damage * 0.25f);

			Modifier.TrackEntityParticle(level, hurtee, ParticleTypes.CRIT);

		}

		this.charged = time;
		LootUtils.updateModifier(itemstack, this);

		return false;
	}
}
