package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.LeveledModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.StatsModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class Munchies extends LeveledModifier implements EntityHurtModifier, BlockBreakModifier, StatsModifier {

	private static final float STAT_BOOST = 0.15f; // 15% boost

	public Munchies() {
		this("Munchies", 1);
	}

	public Munchies(String name, int level) {
		this.name = name;
		this.level = level;
	}

	@Override
	protected int minLevel() {
		return 1;
	}

	@Override
	protected int maxLevel() {
		return 5;
	}

	private float getHungerChance() {
		// Level 1: 10%, Level 2: 8%, Level 3: 6%, Level 4: 4%, Level 5: 2%
		return 0.12f - (level * 0.02f);
	}

	@Override
	public String tagName() {
		return "munchies";
	}

	@Override
	public ChatFormatting color() {
		return ChatFormatting.YELLOW;
	}

	@Override
	public String description() {
		return "15% stat boost, but " + String.format("%.0f", getHungerChance() * 100) + "% chance to consume hunger on use";
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Munchies(tag.getStringOr(NAME, "Munchies"), ModifierConstants.getLevel(tag, 1));
	}

	@Override
	public boolean forTool(ToolType type) {
		// Its payoff lives in tool-only hooks, so on armor it would be a confusing no-op.
		return !type.isArmor();
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
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		if (hurtee.level().isClientSide()) {
			return false;
		}

		// Chance to consume hunger
		tryConsumeHunger(hurter);

		return false;
	}

	@Override
	public boolean startBreak(ItemStack itemstack, BlockPos pos, LivingEntity player) {
		if (player.level().isClientSide()) {
			return false;
		}

		// Chance to consume hunger on block break
		tryConsumeHunger(player);

		return false;
	}

	@Override
	public float getStats(ItemStack itemstack) {
		// 15% faster mining/better stats
		return 1.0f + STAT_BOOST;
	}
}
