package dev.marston.randomloot.loot.modifiers.users;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import dev.marston.randomloot.loot.modifiers.UseModifier;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
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

public class FirePlace implements UseModifier {
	private String name;
	private int damage;
	private static final String DAMAGE = "DAMAGE";

	public FirePlace(String name, int damage) {
		this.name = name;
		this.damage = damage;
	}

	public FirePlace() {
		this.name = "Fire Starter";
		this.damage = 2;
	}

	public Modifier clone() {
		return new FirePlace();
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
		return new FirePlace(
			tag.hasKey(NAME) ? tag.getString(NAME) : "Fire Starter",
			tag.hasKey(DAMAGE) ? tag.getInteger(DAMAGE) : 2);
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "fire_place";
	}

	@Override
	public String color() {
		return TextFormatting.RED.getFriendlyName();
	}

	private EnumActionResult flintNSteel(World world, EntityPlayer player, BlockPos pos, EnumFacing facing, EnumHand hand) {
		BlockPos blockpos1 = pos.offset(facing);
		IBlockState state = world.getBlockState(blockpos1);

		if (state.getBlock().isAir(state, world, blockpos1)) {
			world.playSound(player, blockpos1, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1.0F,
					world.rand.nextFloat() * 0.4F + 0.8F);
			world.setBlockState(blockpos1, Blocks.FIRE.getDefaultState(), 11);

			ItemStack stack = player.getHeldItem(hand);
			stack.damageItem(this.damage, player);

			return EnumActionResult.SUCCESS;
		}
		return EnumActionResult.FAIL;
	}

	@Override
	public EnumActionResult use(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {

		if (!player.isSneaking()) {
			return EnumActionResult.PASS;  // Allow axe stripping when not crouching
		}

		return flintNSteel(world, player, pos, facing, hand);

	}

	@Override
	public String description() {
		return "Right clicking on the top of a block while crouching with the tool in hand will start a fire and use "
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
