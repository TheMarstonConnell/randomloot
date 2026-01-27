package dev.marston.randomloot.loot.modifiers.breakers;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class Cascading implements BlockBreakModifier {

	private String name;
	private float cascadeChance;
	private int maxChainLength;
	private final static String CASCADE_CHANCE = "cascade_chance";
	private final static String MAX_CHAIN = "max_chain_length";

	public Cascading(String name, float cascadeChance, int maxChainLength) {
		this.name = name;
		this.cascadeChance = cascadeChance;
		this.maxChainLength = maxChainLength;
	}

	public Cascading() {
		this.name = "Cascading";
		this.cascadeChance = 0.3f; // 30% chance
		this.maxChainLength = 8;
	}

	public Modifier clone() {
		return new Cascading();
	}

	private boolean isOreBlock(BlockState state) {
		// Check if the block is an ore
		return state.is(BlockTags.COAL_ORES) || 
		       state.is(BlockTags.IRON_ORES) ||
		       state.is(BlockTags.COPPER_ORES) ||
		       state.is(BlockTags.GOLD_ORES) ||
		       state.is(BlockTags.LAPIS_ORES) ||
		       state.is(BlockTags.DIAMOND_ORES) ||
		       state.is(BlockTags.REDSTONE_ORES) ||
		       state.is(BlockTags.EMERALD_ORES) ||
		       state.getBlock().getName().getString().toLowerCase().contains("ore");
	}

	private void cascadeBreak(ItemStack itemstack, BlockPos startPos, ServerPlayer player, Level level, 
			Block targetType, Set<BlockPos> processed, int chainLength) {
		
		if (chainLength >= maxChainLength || processed.size() >= 50) { // Safety limit
			return;
		}

		// Get all adjacent positions (6 directions)
		BlockPos[] adjacent = {
			startPos.above(), startPos.below(),
			startPos.north(), startPos.south(), 
			startPos.east(), startPos.west()
		};

		List<BlockPos> candidates = new ArrayList<>();
		
		for (BlockPos pos : adjacent) {
			if (processed.contains(pos)) continue;
			
			BlockState state = level.getBlockState(pos);
			if (state.is(targetType) && isOreBlock(state)) {
				candidates.add(pos);
			}
		}

		// Randomly select from candidates based on cascade chance
		for (BlockPos pos : candidates) {
			if (level.getRandom().nextFloat() < cascadeChance) {
				processed.add(pos);
				
				BlockState state = level.getBlockState(pos);
				
				// Check if tool can harvest this block
				if (state.canHarvestBlock(level, pos, player)) {
					// Break the block
					state.getBlock().playerDestroy(level, player, pos, state, null, itemstack);
					level.removeBlock(pos, false);
					itemstack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
					
					// Continue the cascade from this position
					cascadeBreak(itemstack, pos, player, level, targetType, processed, chainLength + 1);
				}
			}
		}
	}

	@Override
	public boolean startBreak(ItemStack itemstack, BlockPos pos, LivingEntity entity) {
		if (!(entity instanceof ServerPlayer)) {
			return false;
		}

		ServerPlayer player = (ServerPlayer) entity;
		Level level = player.level();

		if (level.isClientSide()) {
			return false;
		}

		BlockState state = level.getBlockState(pos);
		
		// Only trigger on ore blocks
		if (!isOreBlock(state)) {
			return false;
		}

		LootItem li = (LootItem) itemstack.getItem();
		if (!li.isCorrectToolForDrops(itemstack, state)) {
			return false;
		}

		// Start cascade with small delay to allow original break to complete
		if (level instanceof ServerLevel serverLevel) {
			serverLevel.getServer().execute(() -> {
				Set<BlockPos> processed = new HashSet<>();
				processed.add(pos); // Don't re-break the original block
				cascadeBreak(itemstack, pos, player, level, state.getBlock(), processed, 0);
			});
		}

		return false;
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putFloat(CASCADE_CHANCE, cascadeChance);
		tag.putInt(MAX_CHAIN, maxChainLength);
		tag.putString(NAME, name);
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Cascading(
			tag.getStringOr(NAME, "Cascading"), 
			tag.getFloatOr(CASCADE_CHANCE, 0.3f),
			tag.getIntOr(MAX_CHAIN, 8)
		);
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "cascading";
	}

	@Override
	public String color() {
		return ChatFormatting.YELLOW.name();
	}

	@Override
	public String description() {
		return "Breaking ore blocks has a " + (int)(cascadeChance * 100) + 
		       "% chance to chain-break connected ore veins up to " + maxChainLength + " blocks.";
	}

	@Override
	public void writeToLore(List<Component> list, boolean shift) {
		MutableComponent comp = Modifier.makeComp(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public boolean compatible(Modifier mod) {
		// Compatible with most other modifiers
		return true;
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.PICKAXE);
	}
}