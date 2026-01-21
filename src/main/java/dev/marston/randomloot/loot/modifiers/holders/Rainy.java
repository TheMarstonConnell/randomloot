package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.LootItem.ToolType;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class Rainy implements HoldModifier {

	private String name;
	private float power;
	private final static String POWER = "power";

	public Rainy(String name, float power) {
		this.name = name;
		this.power = power;
	}

	public Rainy() {
		this.name = "Rainy";
		this.power = 4.0f;
	}

	public Modifier clone() {
		return new Rainy();
	}

	@Override
	public CompoundTag toNBT() {

		CompoundTag tag = new CompoundTag();

		tag.putFloat(POWER, power);

		tag.putString(NAME, name);

		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Rainy(tag.getStringOr(NAME, "Rainy"), tag.getFloatOr(POWER, 4.0f));
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "rainy";
	}

	@Override
	public String color() {
		return ChatFormatting.BLUE.getName();
	}

	@Override
	public String description() {
		return "While holding the tool in the rain, mine faster!";
	}

	@Override
	public void writeToLore(List<Component> list, boolean shift) {

		MutableComponent comp = Modifier.makeComp(this.name(), this.color());

		list.add(comp);
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.PICKAXE) || type.equals(ToolType.AXE) || type.equals(ToolType.SHOVEL);
	}

	@Override
	public void hold(ItemStack stack, Level level, Entity holder) {
		if (level.isRainingAt(holder.blockPosition())) {
			MobEffectInstance haste = new MobEffectInstance(MobEffects.HASTE, 3, 2, false, false);

			if (holder instanceof LivingEntity) {
				LivingEntity le = (LivingEntity) holder;
				le.addEffect(haste);
			}
			if (holder instanceof Player) {
				Player p = (Player) holder;
				p.addEffect(haste);
			}
		}

	}
}
