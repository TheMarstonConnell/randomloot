package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;


public class Hasty extends AbstractModifier implements HoldModifier {

	private int power;
	private final static String POWER = "power";

	private int level = 0;

	public Hasty(String name, int power, int level) {
		this.name = name;
		this.power = power;
		this.level = level;
	}

	public Hasty() {
		this.name = "Hasty";
		this.power = 0;
		this.level = 0;
	}

	public Modifier clone() {
		return new Hasty();
	}

	@Override
	public CompoundTag toNBT() {

		CompoundTag tag = new CompoundTag();

		tag.putString(NAME, name);

		tag.putInt(POWER, power);

		tag.putInt(ModifierConstants.LEVEL, level);

		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Hasty(tag.getStringOr(NAME, "Hasty"), tag.getIntOr(POWER, 0), ModifierConstants.getLevel(tag, 0));
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
		return "hasty";
	}

	@Override
	public String color() {
		return ChatFormatting.BLUE.getName();
	}

	@Override
	public String description() {
		return "While holding the tool, get the Haste " + LootUtils.roman(this.level + 1) + " effect.";
	}

	@Override
	public boolean forTool(ToolType type) {
		return isMiningTool(type);
	}

	@Override
	public void hold(ItemStack stack, Level level, Entity holder) {
		MobEffectInstance haste = new MobEffectInstance(MobEffects.HASTE, 2, power, true, false);

		if (holder instanceof LivingEntity le) {
			le.addEffect(haste);
		}

	}

	public boolean canLevel() {
		return level == 0;
	}

	public void levelUp() {
		this.level++;
		this.power++;
		return;
	}
}
