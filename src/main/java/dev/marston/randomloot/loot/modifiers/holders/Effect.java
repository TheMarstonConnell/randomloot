package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.EffectModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;


public class Effect extends EffectModifier implements HoldModifier {

	public Effect(String name, String tagname, int power, int duration, Holder<MobEffect> effect) {
		super(name, tagname, power, duration, effect);
	}

	public Effect(String name, String tagname, int duration, Holder<MobEffect> effect) {
		this(name, tagname, 0, duration, effect);
	}

	public Modifier clone() {
		return new Effect(this.name, this.tagname, this.power, this.duration, this.effect);
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Effect(tag.getStringOr(NAME, this.name), this.tagname, tag.getIntOr(POWER, 0), this.duration, this.effect);
	}

	@Override
	public String color() {
		int color = effect.value().getColor();
		ChatFormatting format = ChatFormatting.getById(color);
		if (format == null) {
			return ChatFormatting.LIGHT_PURPLE.getName();
		}
		return format.getName();
	}

	@Override
	public String description() {
		return "While holding the tool, get the " + I18n.get(effect.value().getDisplayName().getString()).toLowerCase() + " "
				+ LootUtils.roman(this.power + 1) + " effect.";
	}

	@Override
	public boolean forTool(ToolType type) {
		return isMiningTool(type);
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
