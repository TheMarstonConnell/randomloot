package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.EffectModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;


public class HurtEffect extends EffectModifier implements EntityHurtModifier {

	private final ChatFormatting format;

	public HurtEffect(String name, String tagname, int power, int duration, Holder<MobEffect> effect, ChatFormatting format) {
		super(name, tagname, power, duration, effect);
		this.format = format;
	}

	public HurtEffect(String name, String tagname, int duration, Holder<MobEffect> effect, ChatFormatting format) {
		this(name, tagname, 0, duration, effect, format);
	}

	public Modifier clone() {
		return new HurtEffect(this.name, this.tagname, this.power, this.duration, this.effect, this.format);
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new HurtEffect(tag.getStringOr(NAME, this.name), this.tagname, tag.getIntOr(POWER, 0), this.duration, this.effect, this.format);
	}

	@Override
	public String color() {
		return format.getName();
	}

	@Override
	public String description() {
		return "When attacking with tool, apply the " + effect.value().getDisplayName().getString().toLowerCase()
				+ " " + LootUtils.roman(this.power + 1) + " effect to the target for " + this.duration + " seconds.";
	}

	@Override
	public boolean forTool(ToolType type) {
		return isWeapon(type);
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		hurtee.addEffect(makeInstance());
		return false;
	}
}
