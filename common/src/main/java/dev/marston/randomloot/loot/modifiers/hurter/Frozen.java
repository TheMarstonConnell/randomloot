package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.modifiers.BiomeRestrictedModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

public class Frozen extends AbstractModifier implements EntityHurtModifier, HoldModifier, BiomeRestrictedModifier {
	@Override
	public String description() {
		int radius = 3 + traitLevel();
		return "Slows enemies on hit. Creates " + radius + " block radius of frosted ice on water.";
	}

	@Override
	public void hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		int duration = 3 * 20;
		hurtee.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, traitLevel() + 1, false, true));
	}

	@Override
	public void hold(ItemStack stack, Level level, Entity holder) {
		if (!(holder instanceof Player player)) return;

		// Check every 2 ticks for smoother ice generation
		if (level.getGameTime() % 2 != 0) return;

		BlockPos centerPos = player.blockPosition().below();

		// Radius scales with level: 3.0, 4.0, 5.0
		double radius = 3.0 + traitLevel();

		// Create circular pattern of frosted ice (like Frost Walker but wider)
		int radiusInt = (int) Math.ceil(radius);
		for (int xOffset = -radiusInt; xOffset <= radiusInt; xOffset++) {
			for (int zOffset = -radiusInt; zOffset <= radiusInt; zOffset++) {
				// Use floating-point distance for smoother circle
				double distance = Math.sqrt(xOffset * xOffset + zOffset * zOffset);
				if (distance > radius) {
					continue; // Skip blocks outside the circle
				}

				BlockPos pos = centerPos.offset(xOffset, 0, zOffset);
				BlockState below = level.getBlockState(pos);

				// Create frosted ice on water surface (mimic Frost Walker)
				if (below.is(Blocks.WATER) && below.getValue(LiquidBlock.LEVEL) == 0) {
					level.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState());
				}
			}
		}
	}

	@Override
	public String describeRestriction() {
		return "Cold biomes (temperature <= 0.15)";
	}

	@Override
	public boolean canSpawnInBiome(String biomeKey, float temperature, String dimension) {
		return temperature <= 0.15f;
	}
}
