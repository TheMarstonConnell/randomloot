package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class Hasty extends AbstractModifier implements HoldModifier {
	@Override
	public String description() {
		return "While holding or wearing this item, get the Haste " + LootUtils.roman(traitLevel() + 1) + " effect.";
	}

	@Override
	public void hold(ItemStack stack, Level level, Entity holder) {
		// hold() runs every tick; a too-short duration makes the effect timer flicker.
		MobEffectInstance haste = new MobEffectInstance(MobEffects.HASTE, 40, traitLevel(), true, false);

		if (holder instanceof LivingEntity le) {
			le.addEffect(haste);
		}
	}
}
