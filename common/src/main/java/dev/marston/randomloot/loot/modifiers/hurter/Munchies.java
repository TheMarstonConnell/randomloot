package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.StatsModifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class Munchies extends AbstractModifier implements EntityHurtModifier, BlockBreakModifier, StatsModifier {
	private static final float STAT_BOOST = 0.15f; // 15% boost

	private float getHungerChance() {
		// Level 1: 10%, Level 2: 8%, Level 3: 6%, Level 4: 4%, Level 5: 2%
		return 0.12f - (traitLevel() * 0.02f);
	}

	@Override
	public String description() {
		return "15% stat boost, but " + String.format("%.0f", getHungerChance() * 100) + "% chance to consume hunger on use";
	}

	private void tryConsumeHunger(LivingEntity entity) {
		if (entity instanceof Player player) {
			if (player.level().getRandom().nextFloat() < getHungerChance()) {
				int currentFood = player.getFoodData().getFoodLevel();
				if (currentFood > 0) {
					player.getFoodData().setFoodLevel(currentFood - 1);
				}
			}
		}
	}

	@Override
	public void hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		// The melee half of the "15% stat boost". getStats only reaches mining speed,
		// so without this a Munchies sword drained hunger for nothing.
		float baseDamage = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));
		dealBonusDamage(hurtee, hurter, baseDamage * STAT_BOOST);

		// Chance to consume hunger
		tryConsumeHunger(hurter);
	}

	@Override
	public void startBreak(ItemStack itemstack, BlockState broken, BlockPos pos, LivingEntity player) {
		// Chance to consume hunger on block break
		tryConsumeHunger(player);
	}

	@Override
	public float getStats(ItemStack itemstack) {
		// 15% faster mining/better stats
		return 1.0f + STAT_BOOST;
	}
}
