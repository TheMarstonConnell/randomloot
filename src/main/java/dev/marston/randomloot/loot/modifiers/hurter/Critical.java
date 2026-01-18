package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class Critical implements EntityHurtModifier {
	private String name;

	public Critical(String name) {
		this.name = name;
	}

	public Critical() {
		this.name = "Critical";
	}

	public Modifier clone() {
		return new Critical();
	}

	@Override
	public CompoundTag toNBT() {

		CompoundTag tag = new CompoundTag();

		tag.putString(NAME, name);

		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Critical(tag.getStringOr(NAME, "Critical"));
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "critical";
	}

	@Override
	public String color() {
		return ChatFormatting.GOLD.getName();
	}

	@Override
	public String description() {
		return "Always critically strikes enemy.";
	}

	@Override
	public void writeToLore(List<Component> list, boolean shift) {

		MutableComponent comp = Modifier.makeComp(this.name(), this.color());
		list.add(comp);

	}

	@Override
	public Component writeDetailsToLore(Level level) {

		return null;
	}

	@Override
	public boolean compatible(Modifier mod) {
		return true;
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.SWORD) || type.equals(ToolType.AXE);
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		float dmg = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));

		float amt = dmg * 0.5f;

		Modifier.TrackEntityParticle(hurtee.level(), hurtee, ParticleTypes.CRIT);

		if (hurter instanceof Player) {
			Player p = (Player) hurter;
			hurtee.hurt(hurter.damageSources().playerAttack(p), amt);
			return false;
		}

		hurtee.hurt(hurter.damageSources().mobAttack(hurter), amt);

		return false;
	}

	public boolean canLevel() {
		return false;
	}

	public void levelUp() {
		return;
	}
}
