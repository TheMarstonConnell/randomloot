package dev.marston.randomloot.loot.modifiers.breakers;

import java.util.HashSet;
import java.util.Set;

import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class Lumbering extends AbstractModifier implements BlockBreakModifier {

	private static final int MAX_BLOCKS = 64;

	public Lumbering() {
		this.name = "Lumbering";
	}

	public Lumbering(String name) {
		this.name = name;
	}

	@Override
	public String tagName() {
		return "lumbering";
	}

	@Override
	public ChatFormatting color() {
		return ChatFormatting.DARK_GREEN;
	}

	@Override
	public String description() {
		return "Breaking a log fells all connected logs";
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Lumbering(tag.getStringOr(NAME, "Lumbering"));
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.AXE);
	}

	private boolean isLog(BlockState state) {
		return state.is(BlockTags.LOGS);
	}

	private void findConnectedLogs(Level level, BlockPos pos, Set<BlockPos> found) {
		if (found.size() >= MAX_BLOCKS) {
			return;
		}

		if (found.contains(pos)) {
			return;
		}

		BlockState state = level.getBlockState(pos);
		if (!isLog(state)) {
			return;
		}

		found.add(pos);

		for (BlockPos neighbor : treeNeighbors(pos)) {
			findConnectedLogs(level, neighbor, found);
		}
	}

	/** Upward and sideways neighbours, to follow a tree's structure without digging down. */
	private static BlockPos[] treeNeighbors(BlockPos pos) {
		return new BlockPos[] {
			pos.above(),
			pos.above().north(),
			pos.above().south(),
			pos.above().east(),
			pos.above().west(),
			pos.north(),
			pos.south(),
			pos.east(),
			pos.west(),
		};
	}

	@Override
	public boolean startBreak(ItemStack itemstack, BlockState broken, BlockPos pos, LivingEntity p) {
		if (!(p instanceof ServerPlayer player)) {
			return false;
		}

		Level level = player.level();
		if (level.isClientSide()) {
			return false;
		}

		if (!isLog(broken)) {
			return false;
		}

		// Seed the search with the block the player broke instead of re-reading it:
		// it is still in the world on NeoForge but already gone on Fabric. It counts
		// toward MAX_BLOCKS either way and is broken by the normal path, not here.
		Set<BlockPos> logsToBreak = new HashSet<>();
		logsToBreak.add(pos);
		for (BlockPos neighbor : treeNeighbors(pos)) {
			findConnectedLogs(level, neighbor, logsToBreak);
		}
		logsToBreak.remove(pos);

		for (BlockPos logPos : logsToBreak) {
			BlockState logState = level.getBlockState(logPos);
			if (isLog(logState)) {
				boolean destroyed = LootUtils.breakBlockAsPlayer(itemstack, logPos, player, level, logState);
				if (destroyed) {
					LootUtils.damageGear(itemstack, 1, player, EquipmentSlot.MAINHAND);

					if (itemstack.isEmpty()) {
						break;
					}
				}
			}
		}

		return false;
	}

	@Override
	public boolean compatible(Modifier mod) {
		return !(mod instanceof Veiny);
	}
}
