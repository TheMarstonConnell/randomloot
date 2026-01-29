package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class Lightweight implements HoldModifier {

	private String name;
	private int power;
	private final static String POWER = "power";
	private final static String LEVEL = "trait_level";

	private int level = 0;

	public Lightweight(String name, int power, int level) {
		this.name = name;
		this.power = power;
		this.level = level;
	}

	public Lightweight() {
		this.name = "Lightweight";
		this.power = 0;
		this.level = 0;
	}

	public Modifier clone() {
		return new Lightweight();
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putString(NAME, name);
		tag.putInt(POWER, power);
		tag.putInt(LEVEL, level);
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Lightweight(tag.getStringOr(NAME, "Lightweight"), tag.getIntOr(POWER, 0), tag.getIntOr(LEVEL, 0));
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
		return "lightweight";
	}

	@Override
	public String color() {
		return ChatFormatting.AQUA.getName();
	}

	@Override
	public String description() {
		return "While holding the tool, gain Speed " + LootUtils.roman(this.level + 1) + " effect.";
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

	@Override
	public void hold(ItemStack stack, Level level, Entity holder) {
		MobEffectInstance speed = new MobEffectInstance(MobEffects.SPEED, 2, power, true, false);

		if (holder instanceof LivingEntity le) {
			le.addEffect(speed);
		}
	}

	public boolean canLevel() {
		return level < 2;
	}

	public void levelUp() {
		this.level++;
		this.power++;
	}
}
