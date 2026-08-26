package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.NbtCompat;

import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.EffectModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;


public class Effect extends EffectModifier implements HoldModifier {

	public Effect(String name, String tagname, int power, int duration, Holder<MobEffect> effect,
			ChatFormatting format) {
		super(name, tagname, power, duration, effect, format);
	}

	public Effect(String name, String tagname, int duration, Holder<MobEffect> effect, ChatFormatting format) {
		this(name, tagname, 0, duration, effect, format);
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Effect(NbtCompat.getStringOr(tag, NAME, this.name), this.tagname, NbtCompat.getIntOr(tag, POWER, 0), this.duration,
				this.effect, this.format);
	}

	@Override
	public String description() {
		return "While holding or wearing this item, get the "
				+ effect.value().getDisplayName().getString().toLowerCase() + " "
				+ LootUtils.roman(this.power + 1) + " effect.";
	}

	@Override
	public boolean forTool(ToolType type) {
		return isMiningTool(type) || type.isArmor();
	}

	@Override
	public void hold(ItemStack stack, Level level, Entity holder) {
		if (!(holder instanceof LivingEntity livingHolder)) {
			return;
		}

		if (!livingHolder.hasEffect(effect)) {
			livingHolder.addEffect(makeInstance());
		}
	}
}
