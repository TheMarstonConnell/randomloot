package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.BiomeRestrictedModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class Aquatic extends AbstractModifier implements HoldModifier, BiomeRestrictedModifier {
	@Override
	public String description() {
		return "Grants water breathing and Haste " + LootUtils.roman(traitLevel() + 2) + " when underwater.";
	}

	@Override
	public void hold(ItemStack stack, ServerLevel level, LivingEntity holder) {
		// Water breathing
		holder.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 40, 0, false, false));

		// Extra haste when underwater
		if (holder.isUnderWater()) {
			holder.addEffect(new MobEffectInstance(MobEffects.HASTE, 40, traitLevel() + 1, true, false));
		}
	}

	@Override
	public String describeRestriction() {
		return "Ocean and river biomes";
	}

	@Override
	public boolean canSpawnInBiome(String biomeKey, float temperature, String dimension) {
		return biomeKey != null && (
			biomeKey.contains("ocean") ||
			biomeKey.contains("river")
		);
	}
}
