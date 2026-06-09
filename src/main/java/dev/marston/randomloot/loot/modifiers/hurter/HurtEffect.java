package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;


public class HurtEffect extends AbstractModifier implements EntityHurtModifier {

	private int power;
	private String tagname;
	private final static String POWER = "power";
	private Holder<MobEffect> effect;
	private int duration;
	private ChatFormatting format;

	public HurtEffect(String name, String tagname, int power, int duration, Holder<MobEffect> effect, ChatFormatting format) {
		this.name = name;
		this.effect = effect;
		this.power = power;
		this.tagname = tagname;
		this.duration = duration;
		this.format = format;
	}

	public HurtEffect(String name, String tagname, int duration, Holder<MobEffect> effect, ChatFormatting format) {
		this(name, tagname, 0, duration, effect, format);
	}

	public Modifier clone() {
		return new HurtEffect(this.name, this.tagname, this.duration, this.effect, this.format);
	}

	@Override
	public CompoundTag toNBT() {

		CompoundTag tag = new CompoundTag();

		tag.putString(NAME, name);
		tag.putInt(POWER, power);

		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new HurtEffect(tag.getStringOr(NAME, this.name), this.tagname, tag.getIntOr(POWER, 0), this.duration, this.effect, this.format);
	}

	@Override
	public String name() {
		if (this.power == 0) {
			return name;
		}
		return name + " " + LootUtils.roman(this.power + 1);
	}

	@Override
	public String tagName() {
		return tagname;
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
		MobEffectInstance eff = new MobEffectInstance(effect, duration * 20, power, false, false);

		hurtee.addEffect(eff);
		return false;
	}

	public boolean canLevel() {
		return this.power < 4;
	}

	public void levelUp() {
		this.power++;
	}
}
