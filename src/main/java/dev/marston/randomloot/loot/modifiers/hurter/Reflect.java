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

import java.util.List;
import java.util.Random;

public class Reflect implements EntityHurtModifier {
	private static final String LEVEL = "level";
	private static final int MAX_LEVEL = 3;
	private static final float REFLECT_CHANCE_BASE = 0.3f;

	private String name;
	private int level;
	private final Random random = new Random();

	public Reflect(String name, int level) {
		this.name = name;
		this.level = level;
	}

	public Reflect() {
		this.name = "Reflect";
		this.level = 1;
	}

	public Modifier clone() {
		return new Reflect();
	}

	@Override
	public boolean canLevel() {
		return level < MAX_LEVEL;
	}

	@Override
	public void levelUp() {
		this.level++;
	}

	private float getReflectChance() {
		// Level 1: 30%, Level 2: 45%, Level 3: 60%
		switch (level) {
			case 1: return 0.30f;
			case 2: return 0.45f;
			case 3: return 0.60f;
			default: return 0.30f;
		}
	}

	private float getReflectDamageRatio() {
		// Level 1: 50%, Level 2: 75%, Level 3: 100%
		switch (level) {
			case 1: return 0.50f;
			case 2: return 0.75f;
			case 3: return 1.00f;
			default: return 0.50f;
		}
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putString(NAME, name);
		tag.putInt(LEVEL, level);
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Reflect(tag.getStringOr(NAME, "Reflect"), tag.getIntOr(LEVEL, 1));
	}

	@Override
	public String name() {
		if (level == 1) {
			return name;
		}
		return name + " " + LootUtils.roman(level);
	}

	@Override
	public String tagName() {
		return "reflect";
	}

	@Override
	public String color() {
		return ChatFormatting.LIGHT_PURPLE.getName();
	}

	@Override
	public String description() {
		return String.format("%.0f", getReflectChance() * 100) + "% chance to deal " +
				String.format("%.0f", getReflectDamageRatio() * 100) + "% weapon damage when striking enemies";
	}

	@Override
	public void writeToLore(List<Component> list, boolean shift) {
		MutableComponent comp = Modifier.makeComp(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.SWORD) || type.equals(ToolType.AXE) || type.equals(ToolType.PICKAXE);
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		if (random.nextFloat() < getReflectChance()) {
			float dmg = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));
			float bonusDamage = dmg * getReflectDamageRatio();

			Modifier.TrackEntityParticle(hurtee.level(), hurtee, ParticleTypes.INSTANT_EFFECT);

			if (hurter instanceof Player p) {
				hurtee.hurt(hurter.damageSources().playerAttack(p), bonusDamage);
			} else {
				hurtee.hurt(hurter.damageSources().mobAttack(hurter), bonusDamage);
			}
		}
		return false;
	}
}
