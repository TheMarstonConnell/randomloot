package dev.marston.randomloot.loot.modifiers.hurter;


import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.LeveledModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.StatsModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class Fragile extends LeveledModifier implements EntityHurtModifier, BlockBreakModifier, StatsModifier {

	private static final float STAT_BOOST = 0.25f; // 25% boost

	public Fragile() {
		this("Fragile", 1);
	}

	public Fragile(String name, int level) {
		this.name = name;
		this.level = level;
	}

	@Override
	protected int minLevel() {
		return 1;
	}

	@Override
	protected int maxLevel() {
		return 3;
	}

	@Override
	public String tagName() {
		return "fragile";
	}

	@Override
	public ChatFormatting color() {
		return ChatFormatting.WHITE;
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

	/** Chance-based extra durability cost: 2.0x -> always, 1.6x -> 60%, 1.25x -> 25%. */
	private void tryExtraDamage(ItemStack itemstack, ServerPlayer player) {
		if (player.level().getRandom().nextFloat() < (getDurabilityMultiplier() - 1.0f)) {
			itemstack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
		}
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Fragile(tag.getStringOr(NAME, "Fragile"), ModifierConstants.getLevel(tag, 1));
	}

	@Override
	public boolean forTool(ToolType type) {
		// Its payoff lives in tool-only hooks, so on armor it would be a confusing no-op.
		return !type.isArmor();
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		if (hurtee.level().isClientSide()) {
			return false;
		}

		if (hurter instanceof ServerPlayer player) {
			tryExtraDamage(itemstack, player);
		}

		return false;
	}

	@Override
	public boolean startBreak(ItemStack itemstack, BlockPos pos, LivingEntity player) {
		if (player.level().isClientSide()) {
			return false;
		}

		if (player instanceof ServerPlayer serverPlayer) {
			tryExtraDamage(itemstack, serverPlayer);
		}

		return false;
	}

	@Override
	public float getStats(ItemStack itemstack) {
		// 25% faster mining speed
		return 1.0f + STAT_BOOST;
	}
}
