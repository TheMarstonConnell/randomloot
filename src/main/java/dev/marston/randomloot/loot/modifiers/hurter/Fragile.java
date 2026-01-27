package dev.marston.randomloot.loot.modifiers.hurter;

import java.util.List;

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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class Fragile implements EntityHurtModifier, BlockBreakModifier, StatsModifier {

	private static final float STAT_BOOST = 0.25f; // 25% boost

	private String name;

	public Fragile() {
		this.name = "Fragile";
	}

	public Fragile(String name) {
		this.name = name;
	}

	@Override
	public String tagName() {
		return "fragile";
	}

	@Override
	public String name() {
		return this.name;
	}

	@Override
	public String color() {
		return ChatFormatting.WHITE.getName();
	}

	@Override
	public String description() {
		return "25% more damage and speed, but loses durability twice as fast";
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putString(NAME, name);
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Fragile(tag.getStringOr(NAME, "Fragile"));
	}

	@Override
	public Modifier clone() {
		return new Fragile(this.name);
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

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		if (hurtee.level().isClientSide()) {
			return false;
		}

		// Extra durability loss (1 extra = 2 total with normal loss)
		if (hurter instanceof ServerPlayer player) {
			itemstack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
		}

		return false;
	}

	@Override
	public boolean startBreak(ItemStack itemstack, BlockPos pos, LivingEntity player) {
		if (player.level().isClientSide()) {
			return false;
		}

		// Extra durability loss on block break (1 extra = 2 total with normal loss)
		if (player instanceof ServerPlayer serverPlayer) {
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
