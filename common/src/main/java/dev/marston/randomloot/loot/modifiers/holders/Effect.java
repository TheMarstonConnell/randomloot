package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.EffectModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class Effect extends EffectModifier implements HoldModifier {

	public Effect(Holder<MobEffect> effect, int duration) {
		super(effect, duration);
	}

	@Override
	public String description() {
		return "While holding or wearing this item, get the "
				+ effect.value().getDisplayName().getString().toLowerCase() + " "
				+ LootUtils.roman(power() + 1) + " effect.";
	}

	@Override
	public void hold(ItemStack stack, ServerLevel level, LivingEntity holder) {
		if (!holder.hasEffect(effect)) {
			holder.addEffect(makeInstance());
		}
	}
}
