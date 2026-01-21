package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class HurtEffect implements EntityHurtModifier {

	private String name;
	private int power;
	private String tagname;
	private final static String POWER = "power";
	private Holder<MobEffect> effect;
	private int duration;

	public HurtEffect(String name, String tagname, int power, int duration, Holder<MobEffect> effect) {
		this.name = name;
		this.effect = effect;
		this.power = power;
		this.tagname = tagname;
		this.duration = duration;
	}

	public HurtEffect(String name, String tagname, int duration, Holder<MobEffect> effect) {
		this(name, tagname, 0, duration, effect);
	}

	public Modifier clone() {
		return new HurtEffect(this.name, this.tagname, this.duration, this.effect);
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
		return new HurtEffect(tag.getStringOr(NAME, this.name), this.tagname, tag.getIntOr(POWER, 0), this.duration, this.effect);
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
		int color = effect.value().getColor();
		ChatFormatting format = ChatFormatting.getById(color);
		if (format == null) {
			return ChatFormatting.LIGHT_PURPLE.getName();
		}
		return format.getName();
	}

	@Override
	public String description() {
		return "When attacking with tool, apply the " + I18n.get(effect.value().getDisplayName().getString()).toLowerCase()
				+ " " + LootUtils.roman(this.power + 1) + " effect to the target for " + this.duration + " seconds.";
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
