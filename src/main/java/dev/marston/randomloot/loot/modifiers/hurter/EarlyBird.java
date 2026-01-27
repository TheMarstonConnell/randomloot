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

import java.util.List;

public class EarlyBird implements EntityHurtModifier {
	private String name;
	private static final float BONUS_DAMAGE = 0.15f;

	public EarlyBird(String name) {
		this.name = name;
	}

	public EarlyBird() {
		this.name = "Early Bird";
	}

	public Modifier clone() {
		return new EarlyBird();
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putString(NAME, name);
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new EarlyBird(tag.getStringOr(NAME, "Early Bird"));
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "early_bird";
	}

	@Override
	public String color() {
		return ChatFormatting.YELLOW.getName();
	}

	@Override
	public String description() {
		return "Deals 15% extra damage to full-health targets";
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
		float currentHealth = hurtee.getHealth();
		float maxHealth = hurtee.getMaxHealth();

		// Check if target was at full health before this attack
		// Since damage is already applied, check if current health + base damage >= max health
		if (currentHealth + dmg >= maxHealth * 0.95f) {
			float bonusDamage = dmg * BONUS_DAMAGE;

			if (hurter instanceof Player p) {
				hurtee.hurt(hurter.damageSources().playerAttack(p), bonusDamage);
			} else {
				hurtee.hurt(hurter.damageSources().mobAttack(hurter), bonusDamage);
			}
		}

		return false;
	}
}
