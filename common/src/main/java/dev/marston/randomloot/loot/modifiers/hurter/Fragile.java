package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.StatsModifier;
import dev.marston.randomloot.loot.modifiers.TraitDispatch;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class Fragile extends AbstractModifier implements EntityHurtModifier, BlockBreakModifier, StatsModifier {
	private static final float STAT_BOOST = 0.25f; // 25% boost

	@Override
	public String description() {
		return "25% more damage and speed, but loses durability " + String.format("%.1fx", getDurabilityMultiplier()) + " as fast";
	}

	private float getDurabilityMultiplier() {
		// Level 1: 2.0x, Level 2: 1.6x, Level 3: 1.25x
		switch (traitLevel()) {
			case 1: return 2.0f;
			case 2: return 1.6f;
			case 3: return 1.25f;
			default: return 2.0f;
		}
	}

	/** Chance-based extra durability cost: 2.0x -> always, 1.6x -> 60%, 1.25x -> 25%. */
	private void tryExtraDamage(ItemStack itemstack, ServerPlayer player) {
		if (player.level().getRandom().nextFloat() < (getDurabilityMultiplier() - 1.0f)) {
			TraitDispatch.spendDurability(itemstack, 1, player, EquipmentSlot.MAINHAND);
		}
	}

	@Override
	public void hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		// The "25% more damage" half of the trait. getStats only reaches mining speed,
		// so without this a Fragile sword paid the durability cost for nothing.
		float baseDamage = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));
		dealBonusDamage(hurtee, hurter, baseDamage * STAT_BOOST);

		if (hurter instanceof ServerPlayer player) {
			tryExtraDamage(itemstack, player);
		}
	}

	@Override
	public void startBreak(ItemStack itemstack, BlockState broken, BlockPos pos, LivingEntity player) {
		if (player instanceof ServerPlayer serverPlayer) {
			tryExtraDamage(itemstack, serverPlayer);
		}
	}

	@Override
	public float getStats(ItemStack itemstack) {
		// 25% faster mining speed
		return 1.0f + STAT_BOOST;
	}
}
