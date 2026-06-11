package dev.marston.randomloot.loot.modifiers.breakers;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class Excavator extends AbstractModifier implements BlockBreakModifier {

	public Excavator(String name) {
		this.name = name;
	}

	public Excavator() {
		this.name = "Excavator";
	}

	private List<BlockPos> get3x3Positions(BlockPos center, ServerPlayer player) {
		List<BlockPos> positions = new ArrayList<>();

		Direction facing = player.getDirection();
		float pitch = player.getXRot();

		if (Math.abs(pitch) > 45) {
			// Looking up or down -> horizontal plane (X-Z)
			for (int x = -1; x <= 1; x++) {
				for (int z = -1; z <= 1; z++) {
					if (x == 0 && z == 0) continue; // Skip center
					positions.add(center.offset(x, 0, z));
				}
			}
		} else {
			// Looking horizontally -> vertical plane perpendicular to facing
			if (facing == Direction.NORTH || facing == Direction.SOUTH) {
				// Mine in X-Y plane
				for (int x = -1; x <= 1; x++) {
					for (int y = -1; y <= 1; y++) {
						if (x == 0 && y == 0) continue; // Skip center
						positions.add(center.offset(x, y, 0));
					}
				}
			} else {
				// EAST or WEST - mine in Z-Y plane
				for (int z = -1; z <= 1; z++) {
					for (int y = -1; y <= 1; y++) {
						if (z == 0 && y == 0) continue; // Skip center
						positions.add(center.offset(0, y, z));
					}
				}
			}
		}

		return positions;
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

		Level level = player.level();

		if (level.isClientSide()) {
			return false;
		}

		LootItem li = (LootItem) itemstack.getItem();
		List<BlockPos> positions = get3x3Positions(pos, player);

		for (BlockPos blockPos : positions) {
			BlockState state = level.getBlockState(blockPos);

			// Skip air and unharvestable blocks
			if (state.isAir()) {
				continue;
			}

			if (!state.canHarvestBlock(level, blockPos, player)) {
				continue;
			}

			if (!li.isCorrectToolForDrops(itemstack, state)) {
				continue;
			}

			// Break the block with drops
			state.getBlock().playerDestroy(level, player, blockPos, state, null, itemstack);
			level.removeBlock(blockPos, false);

			// Apply durability damage for each block
			itemstack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
		}

		return false;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Excavator(tag.getStringOr(NAME, "Excavator"));
	}

	@Override
	public String tagName() {
		return "excavator";
	}

	@Override
	public String color() {
		return ChatFormatting.GREEN.getName();
	}

	@Override
	public String description() {
		return "Breaking blocks while crouching mines a 3x3 area perpendicular to the surface.";
	}

	@Override
	public boolean compatible(Modifier mod) {
		// Incompatible with Veiny
		return !(mod instanceof Veiny);
	}

	@Override
	public boolean forTool(ToolType type) {
		return isMiningTool(type);
	}
}
