package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.modifiers.BiomeRestrictedModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class Scorched extends AbstractModifier implements EntityHurtModifier, HoldModifier, BiomeRestrictedModifier {
	@Override
	public String description() {
		int duration = 4 + (traitLevel() * 2);
		return "Sets enemies on fire for " + duration + " seconds. Grants fire resistance while held.";
	}

	@Override
	public void hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		int fireDuration = (4 + (traitLevel() * 2)) * 20;
		hurtee.setRemainingFireTicks(fireDuration);
	}

	@Override
	public void hold(ItemStack stack, ServerLevel level, LivingEntity holder) {
		holder.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, false, false));
	}

	@Override
	public String describeRestriction() {
		return "Hot biomes (temperature >= 1.0) or the Nether";
	}

	@Override
	public boolean canSpawnInBiome(String biomeKey, float temperature, String dimension) {
		return temperature >= 1.0f || (dimension != null && dimension.equals("minecraft:the_nether"));
	}
}
