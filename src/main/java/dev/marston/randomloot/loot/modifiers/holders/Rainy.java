package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
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


public class Rainy extends AbstractModifier implements HoldModifier {

	public Rainy(String name) {
		this.name = name;
	}

	public Rainy() {
		this("Rainy");
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Rainy(tag.getStringOr(NAME, "Rainy"));
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
		return "While holding or wearing this item in the rain, mine faster!";
	}

	@Override
	public boolean forTool(ToolType type) {
		return isMiningTool(type) || type.isArmor();
	}

	@Override
	public void hold(ItemStack stack, Level level, Entity holder) {
		if (level.isRainingAt(holder.blockPosition())) {
			// hold() runs every tick; a too-short duration makes the effect timer flicker.
			MobEffectInstance haste = new MobEffectInstance(MobEffects.HASTE, 40, 2, false, false);

			if (holder instanceof LivingEntity le) {
				le.addEffect(haste);
			}
		}

	}
}
