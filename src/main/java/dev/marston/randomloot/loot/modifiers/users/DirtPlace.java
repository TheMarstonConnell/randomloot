package dev.marston.randomloot.loot.modifiers.users;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.NameGenerator;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import dev.marston.randomloot.loot.modifiers.UseModifier;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;

public class DirtPlace implements UseModifier {
	private String name;
	private int damage;
	private static final String DAMAGE = "DAMAGE";

	public DirtPlace(String name, int damage) {
		this.name = name;
		this.damage = damage;
	}

	public DirtPlace() {
		this.name = NameGenerator.generateForger(0.5f) + "'s Grace";
		this.damage = 1;
	}

	public Modifier clone() {
		return new DirtPlace();
	}

	@Override
	public NBTTagCompound toNBT() {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setString(NAME, name);
		tag.setInteger(DAMAGE, damage);
		return tag;
	}

	@Override
	public Modifier fromNBT(NBTTagCompound tag) {
		return new DirtPlace(
			tag.hasKey(NAME) ? tag.getString(NAME) : NameGenerator.generateForger(0.5f) + "'s Grace",
			tag.hasKey(DAMAGE) ? tag.getInteger(DAMAGE) : 1);
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "dirt_place";
	}

	@Override
	public String color() {
		return TextFormatting.DARK_GREEN.getFriendlyName();
	}

	private EnumActionResult place(World world, EntityPlayer player, BlockPos pos, EnumFacing facing, EnumHand hand) {
		BlockPos placePos = pos.offset(facing);
		IBlockState targetState = world.getBlockState(placePos);

		// Check if position is air or replaceable
		if (!targetState.getBlock().isReplaceable(world, placePos)) {
			return EnumActionResult.FAIL;
		}

		IBlockState dirtState = Blocks.DIRT.getDefaultState();
		world.setBlockState(placePos, dirtState, 11);

		SoundType soundtype = dirtState.getBlock().getSoundType(dirtState, world, placePos, player);
		world.playSound(player, placePos, soundtype.getPlaceSound(), SoundCategory.BLOCKS,
				(soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);

		return EnumActionResult.SUCCESS;
	}

	@Override
	public EnumActionResult use(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (!player.isSneaking()) {
			return EnumActionResult.PASS;  // Allow normal tool behaviors when not crouching
		}

		EnumActionResult result = place(world, player, pos, facing, hand);

		if (result == EnumActionResult.SUCCESS) {
			ItemStack stack = player.getHeldItem(hand);
			stack.damageItem(this.damage, player);
		}

		return result;
	}

	@Override
	public String description() {
		return "Right clicking on a block while crouching with the tool in hand will place a dirt block and use "
				+ this.damage + " durability points.";
	}

	@Override
	public void writeToLore(List<String> list, boolean shift) {
		String comp = Modifier.formatText(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public boolean compatible(Modifier mod) {
		return !ModifierRegistry.USERS.contains(mod);
	}

	@Override
	public boolean forTool(ToolType type) {
		return true;
	}

	@Override
	public boolean use(World world, EntityPlayer player, EnumHand hand) {
		return true;
	}

	@Override
	public boolean useAnywhere() {
		return false;
	}
}
