package dev.marston.randomloot.loot.modifiers.hurter;


import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.StatsModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class Fragile extends AbstractModifier implements EntityHurtModifier, BlockBreakModifier, StatsModifier {

	private static final float STAT_BOOST = 0.25f; // 25% boost
	private static final int MAX_LEVEL = 3;

	private int level;

	public Fragile() {
		this.name = "Fragile";
		this.level = 1;
	}

	public Fragile(String name, int level) {
		this.name = name;
		this.level = level;
	}

	@Override
	public String tagName() {
		return "fragile";
	}

	@Override
	public String name() {
		if (level == 1) {
			return this.name;
		}
		return this.name + " " + LootUtils.roman(level);
	}

	@Override
	public String color() {
		return ChatFormatting.WHITE.getName();
	}

	@Override
	public String description() {
		return "25% more damage and speed, but loses durability " + String.format("%.1fx", getDurabilityMultiplier()) + " as fast";
	}

	private float getDurabilityMultiplier() {
		// Level 1: 2.0x, Level 2: 1.6x, Level 3: 1.25x
		switch (level) {
			case 1: return 2.0f;
			case 2: return 1.6f;
			case 3: return 1.25f;
			default: return 2.0f;
		}
	}

	private boolean shouldTakeExtraDamage(java.util.Random random) {
		float mult = getDurabilityMultiplier();
		// mult - 1.0 = chance of taking extra 1 durability
		// e.g., 2.0 -> always take 1 extra, 1.6 -> 60% chance, 1.25 -> 25% chance
		return random.nextFloat() < (mult - 1.0f);
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putString(NAME, name);
		tag.putInt(ModifierConstants.LEVEL, level);
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Fragile(tag.getStringOr(NAME, "Fragile"), ModifierConstants.getLevel(tag, 1));
	}

	@Override
	public Modifier clone() {
		return new Fragile();
	}

	@Override
	public boolean canLevel() {
		return level < MAX_LEVEL;
	}

	@Override
	public void levelUp() {
		this.level++;
	}

	@Override
	public boolean forTool(ToolType type) {
		// Its payoff lives in tool-only hooks, so on armor it would be a confusing no-op.
		return !type.isArmor();
	}

	private java.util.Random random = new java.util.Random();

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		if (hurtee.level().isClientSide()) {
			return false;
		}

		// Extra durability loss based on level (probability-based for fractional multipliers)
		if (hurter instanceof ServerPlayer player && shouldTakeExtraDamage(random)) {
			itemstack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
		}

		return false;
	}

	@Override
	public boolean startBreak(ItemStack itemstack, BlockPos pos, LivingEntity player) {
		if (player.level().isClientSide()) {
			return false;
		}

		// Extra durability loss on block break based on level
		if (player instanceof ServerPlayer serverPlayer && shouldTakeExtraDamage(random)) {
			itemstack.hurtAndBreak(1, serverPlayer, EquipmentSlot.MAINHAND);
		}

		return false;
	}

	@Override
	public float getStats(ItemStack itemstack) {
		// 25% faster mining speed
		return 1.0f + STAT_BOOST;
	}
}
