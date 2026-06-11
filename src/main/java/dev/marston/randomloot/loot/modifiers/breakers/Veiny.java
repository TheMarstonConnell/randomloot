package dev.marston.randomloot.loot.modifiers.breakers;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

public class Veiny extends AbstractModifier implements BlockBreakModifier {

	private float power;
	private final static String POWER = "power";

	public Veiny(String name, float power) {
		this.name = name;
		this.power = power;
	}

	public Veiny() {
		this.name = "Veiny";
		this.power = 5.0f;
	}

	public void checkAndBreak(ItemStack itemstack, BlockPos pos, Player player, Level level, int index, Block blockType,
			Set<BlockPos> tobreak, Set<BlockPos> visited) {

		if (index > power) {
			return;
		}

		// Guard against revisiting positions: without it the 6-way recursion
		// re-expands the same cells exponentially. The origin is tracked here even
		// though it is deliberately never added to tobreak.
		if (!visited.add(pos.immutable())) {
			return;
		}

		BlockState startingState = level.getBlockState(pos);

		if (!startingState.is(blockType)) {
			return;
		}

		if (index > 0) {
			tobreak.add(pos.immutable());
		}

		int dex = index + 1;

		checkAndBreak(itemstack, pos.above(), player, level, dex, blockType, tobreak, visited);
		checkAndBreak(itemstack, pos.below(), player, level, dex, blockType, tobreak, visited);
		checkAndBreak(itemstack, pos.east(), player, level, dex, blockType, tobreak, visited);
		checkAndBreak(itemstack, pos.west(), player, level, dex, blockType, tobreak, visited);
		checkAndBreak(itemstack, pos.north(), player, level, dex, blockType, tobreak, visited);
		checkAndBreak(itemstack, pos.south(), player, level, dex, blockType, tobreak, visited);

	}

	@Override
	public boolean startBreak(ItemStack itemstack, BlockPos pos, LivingEntity p) {

		if (!(p instanceof ServerPlayer)) {
			return false;
		}

		ServerPlayer player = (ServerPlayer) p;

		if (!player.isCrouching()) {
			return false;
		}

		Level l = player.level();

		if (l.isClientSide()) {
			return false;
		}

		BlockState state = l.getBlockState(pos);

		LootItem li = (LootItem) itemstack.getItem();

		if (!li.isCorrectToolForDrops(itemstack, state)) {
			return false;
		}

		Block b = state.getBlock();

		Set<BlockPos> toBreak = new HashSet<BlockPos>();

		checkAndBreak(itemstack, pos, player, l, 0, b, toBreak, new HashSet<BlockPos>());

		for (BlockPos blockPos : toBreak) {
			boolean destroyed = LootUtils.breakBlockAsPlayer(itemstack, blockPos, player, l, l.getBlockState(blockPos));

			if (destroyed) {
				itemstack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
				if (itemstack.isEmpty()) {
					break;
				}
			}
		}
		return false;
	}

	@Override
	public CompoundTag toNBT() {

		CompoundTag tag = new CompoundTag();

		tag.putFloat(POWER, power);

		tag.putString(NAME, name);

		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Veiny(tag.getStringOr(NAME, "Veiny"), tag.getFloatOr(POWER, 5.0f));
	}

	@Override
	public String tagName() {
		return "veiny";
	}

	@Override
	public String color() {
		return ChatFormatting.DARK_GREEN.getName();
	}

	@Override
	public String description() {
		return "Breaking any block while crouching will cause all blocks of the same type adjacent to it to break up to "
				+ ((int) power) + " in each direction.";
	}

	@Override
	public boolean compatible(Modifier mod) {
		// Incompatible with Excavator
		return !(mod instanceof Excavator);
	}

	@Override
	public boolean forTool(ToolType type) {
		return isMiningTool(type);
	}
}
