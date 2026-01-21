package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

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
	public CompoundTag toNBT() {

		CompoundTag tag = new CompoundTag();

		tag.putString(NAME, name);

		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Bezerk(tag.getStringOr(NAME, "Bezerk"));
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
		return ChatFormatting.GOLD.getName();
	}

	@Override
	public String description() {
		return "Deals more damage at lower player health.";
	}

	@Override
	public void writeToLore(List<Component> list, boolean shift) {

		MutableComponent comp = Modifier.makeComp(this.name(), this.color());
		list.add(comp);

	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.SWORD) || type.equals(ToolType.AXE);
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		float dmg = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));

		float maxHealth = hurter.getMaxHealth();
		float currentHealth = hurter.getHealth();

		float ratio = maxHealth / currentHealth;

		float amt = dmg * 0.05f * (ratio - 1);

		if (hurter instanceof Player) {
			Player p = (Player) hurter;
			hurtee.hurt(hurter.damageSources().playerAttack(p), amt);
			return false;
		}

		hurtee.hurt(hurter.damageSources().mobAttack(hurter), amt);

		return false;
	}
}
