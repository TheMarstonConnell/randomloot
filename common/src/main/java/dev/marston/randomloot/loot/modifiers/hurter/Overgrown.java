package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.modifiers.BiomeRestrictedModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class Overgrown extends AbstractModifier implements EntityHurtModifier, HoldModifier, BiomeRestrictedModifier {
	@Override
	public String description() {
		float bonusDamage = 2.5f + (traitLevel() * 2.5f);
		return "Grants poison immunity. Deals " + bonusDamage + " bonus damage to arthropods.";
	}

	@Override
	public void hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		EntityType<?> type = hurtee.getType();
		boolean isArthropod = type == EntityTypes.SPIDER ||
							  type == EntityTypes.CAVE_SPIDER ||
							  type == EntityTypes.SILVERFISH ||
							  type == EntityTypes.ENDERMITE ||
							  type == EntityTypes.BEE;

		if (isArthropod) {
			float bonusDamage = 2.5f + (traitLevel() * 2.5f);
			dealBonusDamage(hurtee, hurter, bonusDamage);
			hurtee.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, traitLevel()));
		}
	}

	@Override
	public void hold(ItemStack stack, Level level, Entity holder) {
		if (!(holder instanceof LivingEntity living)) return;
		if (living.hasEffect(MobEffects.POISON)) {
			living.removeEffect(MobEffects.POISON);
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
