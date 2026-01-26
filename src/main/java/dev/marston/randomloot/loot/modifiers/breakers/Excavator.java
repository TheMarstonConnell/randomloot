package dev.marston.randomloot.loot.modifiers.breakers;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class Excavator implements BlockBreakModifier {

	private String name;

	public Excavator(String name) {
		this.name = name;
	}

	public Excavator() {
		this.name = "Excavator";
	}

	public Modifier clone() {
		return new Excavator();
	}

	private List<BlockPos> get3x3Positions(BlockPos center, EntityPlayerMP player) {
		List<BlockPos> positions = new ArrayList<>();

		EnumFacing facing = player.getHorizontalFacing();
		float pitch = player.rotationPitch;

		if (Math.abs(pitch) > 45) {
			// Looking up or down -> horizontal plane (X-Z)
			for (int x = -1; x <= 1; x++) {
				for (int z = -1; z <= 1; z++) {
					if (x == 0 && z == 0) continue; // Skip center
					positions.add(center.add(x, 0, z));
				}
			}
		} else {
			// Looking horizontally -> vertical plane perpendicular to facing
			if (facing == EnumFacing.NORTH || facing == EnumFacing.SOUTH) {
				// Mine in X-Y plane
				for (int x = -1; x <= 1; x++) {
					for (int y = -1; y <= 1; y++) {
						if (x == 0 && y == 0) continue; // Skip center
						positions.add(center.add(x, y, 0));
					}
				}
			} else {
				// EAST or WEST - mine in Z-Y plane
				for (int z = -1; z <= 1; z++) {
					for (int y = -1; y <= 1; y++) {
						if (z == 0 && y == 0) continue; // Skip center
						positions.add(center.add(0, y, z));
					}
				}
			}
		}

		return positions;
	}

	@Override
	public boolean startBreak(ItemStack itemstack, BlockPos pos, EntityLivingBase p) {

		if (!(p instanceof EntityPlayerMP)) {
			return false;
		}

		EntityPlayerMP player = (EntityPlayerMP) p;

		if (!player.isSneaking()) {
			return false;
		}

		World world = player.world;

		if (world.isRemote) {
			return false;
		}

		LootItem li = (LootItem) itemstack.getItem();
		List<BlockPos> positions = get3x3Positions(pos, player);

		for (BlockPos blockPos : positions) {
			IBlockState state = world.getBlockState(blockPos);

			// Skip air and unharvestable blocks
			if (state.getBlock().isAir(state, world, blockPos)) {
				continue;
			}

			if (!state.getBlock().canHarvestBlock(world, blockPos, player)) {
				continue;
			}

			if (!li.canHarvestBlock(state, itemstack)) {
				continue;
			}

			// Break the block with drops
			state.getBlock().harvestBlock(world, player, blockPos, state, null, itemstack);
			world.setBlockToAir(blockPos);

			// Apply durability damage for each block
			itemstack.damageItem(1, player);
		}

		return false;
	}

	@Override
	public NBTTagCompound toNBT() {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setString(NAME, name);
		return tag;
	}

	@Override
	public Modifier fromNBT(NBTTagCompound tag) {
		return new Excavator(tag.hasKey(NAME) ? tag.getString(NAME) : "Excavator");
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "excavator";
	}

	@Override
	public String color() {
		return TextFormatting.GREEN.getFriendlyName();
	}

	@Override
	public String description() {
		return "Breaking blocks while crouching mines a 3x3 area perpendicular to the surface.";
	}

	@Override
	public void writeToLore(List<String> list, boolean shift) {
		String comp = Modifier.formatText(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public boolean compatible(Modifier mod) {
		// Incompatible with Veiny
		return !(mod instanceof Veiny);
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.PICKAXE) || type.equals(ToolType.AXE) || type.equals(ToolType.SHOVEL);
	}
}
