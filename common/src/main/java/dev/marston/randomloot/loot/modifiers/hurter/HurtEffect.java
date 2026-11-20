package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.EffectModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class HurtEffect extends EffectModifier implements EntityHurtModifier {

	public HurtEffect(Holder<MobEffect> effect, int duration) {
		super(effect, duration);
	}

	@Override
	public String description() {
		return "When attacking with tool, apply the " + effect.value().getDisplayName().getString().toLowerCase()
				+ " " + LootUtils.roman(power() + 1) + " effect to the target for " + this.duration + " seconds.";
	}

	@Override
	public void hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		hurtee.addEffect(makeInstance());
	}
}
