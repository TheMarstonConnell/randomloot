package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.StatsModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class Fierce implements EntityHurtModifier, StatsModifier {

	private String name;

	public Fierce() {
		this("Fierce");
	}

	public Fierce(String name) {
		this.name = name;
	}

	@Override
	public Modifier clone() {
		return new Fierce(this.name);
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putString(NAME, name);
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Fierce(tag.getStringOr(NAME, "Fierce"));
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "fierce";
	}

	@Override
	public String color() {
		return ChatFormatting.RED.getName();
	}

	@Override
	public String description() {
		return "Deals more damage as durability decreases";
	}

	@Override
	public void writeToLore(List<Component> list, boolean shift) {
		MutableComponent comp = Modifier.makeComp(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public boolean forTool(ToolType type) {
		return true;
	}

	private float getDamageMultiplier(ItemStack itemstack) {
		float maxDamage = itemstack.getMaxDamage();
		if (maxDamage <= 0) return 1.0f;

		float currentDamage = itemstack.getDamageValue();
		float ratio = currentDamage / maxDamage;

		return 1.0f + (ratio * 0.5f);
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		float multiplier = getDamageMultiplier(itemstack);
		if (multiplier <= 1.0f) {
			return false;
		}

		float baseDamage = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));
		float bonusDamage = baseDamage * (multiplier - 1.0f);

		if (hurter instanceof Player p) {
			hurtee.hurt(hurter.damageSources().playerAttack(p), bonusDamage);
		} else {
			hurtee.hurt(hurter.damageSources().mobAttack(hurter), bonusDamage);
		}

		return false;
	}

	@Override
	public float getStats(ItemStack itemstack) {
		float maxDamage = itemstack.getMaxDamage();
		if (maxDamage <= 0) return 1.0f;

		float currentDamage = itemstack.getDamageValue();
		float ratio = currentDamage / maxDamage;

		return 1.0f + (ratio * 0.25f);
	}
}
