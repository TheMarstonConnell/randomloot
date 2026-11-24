package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.modifiers.BiomeRestrictedModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class Overgrown extends AbstractModifier implements EntityHurtModifier, HoldModifier, BiomeRestrictedModifier {
	@Override
	public String description() {
		float bonusDamage = 2.5f + (traitLevel() * 2.5f);
		return "Grants poison immunity. Deals " + bonusDamage + " bonus damage to arthropods.";
	}

	@Override
	public void hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		if (hurtee.is(EntityTypeTags.ARTHROPOD)) {
			float bonusDamage = 2.5f + (traitLevel() * 2.5f);
			dealBonusDamage(hurtee, hurter, bonusDamage);
			hurtee.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, traitLevel()));
		}
	}

	@Override
	public void hold(ItemStack stack, ServerLevel level, LivingEntity holder) {
		if (holder.hasEffect(MobEffects.POISON)) {
			holder.removeEffect(MobEffects.POISON);
		}
	}

	@Override
	public String describeRestriction() {
		return "Jungle, swamp and bamboo biomes";
	}

	@Override
	public boolean canSpawnInBiome(String biomeKey, float temperature, String dimension) {
		return biomeKey != null && (
			biomeKey.contains("jungle") ||
			biomeKey.contains("swamp") ||
			biomeKey.contains("bamboo")
		);
	}
}
