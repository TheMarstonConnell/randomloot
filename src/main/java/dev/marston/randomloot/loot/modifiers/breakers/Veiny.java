package dev.marston.randomloot.loot.modifiers.breakers;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Veiny implements BlockBreakModifier {

	private String name;
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

	public Modifier clone() {
		return new Veiny();
	}

	private void removeBlock(ItemStack itemstack, BlockPos pos, EntityPlayerMP player, World world, IBlockState state) {
		if (!state.getBlock().canHarvestBlock(world, pos, player)) {
			return;
		}

		state.getBlock().harvestBlock(world, player, pos, state, null, itemstack);
		world.setBlockToAir(pos);
	}

	public void checkAndBreak(ItemStack itemstack, BlockPos pos, EntityPlayer player, World world, int index, Block blockType,
			Set<BlockPos> tobreak) {

		if (index > power) {
			return;
		}

		IBlockState startingState = world.getBlockState(pos);

		if (startingState.getBlock() != blockType) {
			return;
		}

		if (index > 0) {
			tobreak.add(pos);
		}

		int dex = index + 1;

		checkAndBreak(itemstack, pos.up(), player, world, dex, blockType, tobreak);
		checkAndBreak(itemstack, pos.down(), player, world, dex, blockType, tobreak);
		checkAndBreak(itemstack, pos.east(), player, world, dex, blockType, tobreak);
		checkAndBreak(itemstack, pos.west(), player, world, dex, blockType, tobreak);
		checkAndBreak(itemstack, pos.north(), player, world, dex, blockType, tobreak);
		checkAndBreak(itemstack, pos.south(), player, world, dex, blockType, tobreak);

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

		IBlockState state = world.getBlockState(pos);

		LootItem li = (LootItem) itemstack.getItem();

		if (!li.canHarvestBlock(state, itemstack)) {
			return false;
		}

		Block b = state.getBlock();

		Set<BlockPos> toBreak = new HashSet<BlockPos>();

		checkAndBreak(itemstack, pos, player, world, 0, b, toBreak);

		for (Iterator<BlockPos> iterator = toBreak.iterator(); iterator.hasNext();) {
			BlockPos blockPos = iterator.next();

			removeBlock(itemstack, blockPos, player, world, world.getBlockState(blockPos));

			itemstack.damageItem(1, player);

		}
		return false;
	}

	@Override
	public NBTTagCompound toNBT() {

		NBTTagCompound tag = new NBTTagCompound();

		tag.setFloat(POWER, power);

		tag.setString(NAME, name);

		return tag;
	}

	@Override
	public Modifier fromNBT(NBTTagCompound tag) {
		return new Veiny(
			tag.hasKey(NAME) ? tag.getString(NAME) : "Veiny",
			tag.hasKey(POWER) ? tag.getFloat(POWER) : 5.0f);
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "veiny";
	}

	@Override
	public String color() {
		return TextFormatting.DARK_GREEN.getFriendlyName();
	}

	@Override
	public String description() {
		return "Breaking any block while crouching will cause all blocks of the same type adjacent to it to break up to "
				+ ((int) power) + " in each direction.";
	}

	@Override
	public void writeToLore(List<String> list, boolean shift) {
		String comp = Modifier.formatText(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public boolean compatible(Modifier mod) {
		// Incompatible with Excavator
		return !(mod instanceof Excavator);
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.PICKAXE) || type.equals(ToolType.AXE) || type.equals(ToolType.SHOVEL);
	}
}
