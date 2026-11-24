package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class Hasty extends AbstractModifier implements HoldModifier {
	@Override
	public String description() {
		return "While holding or wearing this item, get the Haste " + LootUtils.roman(traitLevel() + 1) + " effect.";
	}

	@Override
	public void hold(ItemStack stack, ServerLevel level, LivingEntity holder) {
		// hold() runs every tick; a too-short duration makes the effect timer flicker.
		holder.addEffect(new MobEffectInstance(MobEffects.HASTE, 40, traitLevel(), true, false));
	}
}
