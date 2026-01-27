package dev.marston.randomloot.loot.modifiers.hurter;

import java.util.List;
import java.util.Random;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.StatsModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class Munchies implements EntityHurtModifier, BlockBreakModifier, StatsModifier {

	private static final float STAT_BOOST = 0.15f; // 15% boost
	private static final float HUNGER_CHANCE = 0.10f; // 10% chance

	private String name;
	private Random random = new Random();

	public Munchies() {
		this.name = "Munchies";
	}

	public Munchies(String name) {
		this.name = name;
	}

	@Override
	public Modifier clone() {
		return new Munchies(this.name);
	}

	@Override
	public String tagName() {
		return "munchies";
	}

	@Override
	public String name() {
		return this.name;
	}

	@Override
	public String color() {
		return ChatFormatting.YELLOW.getName();
	}

	@Override
	public String description() {
		return "15% stat boost, but 10% chance to consume hunger on use";
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putString(NAME, name);
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Munchies(tag.getStringOr(NAME, "Munchies"));
	}

	@Override
	public boolean forTool(ToolType type) {
		return true; // Works with all tool types
	}

	@Override
	public void writeToLore(List<Component> list, boolean shift) {
		MutableComponent comp = Modifier.makeComp(this.name(), this.color());
		list.add(comp);
	}

	private void tryConsumeHunger(LivingEntity entity) {
		if (entity instanceof Player player) {
			if (random.nextFloat() < HUNGER_CHANCE) {
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
