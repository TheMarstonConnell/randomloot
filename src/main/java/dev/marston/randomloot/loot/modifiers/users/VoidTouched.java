package dev.marston.randomloot.loot.modifiers.users;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.BiomeRestrictedModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import dev.marston.randomloot.loot.modifiers.UseModifier;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.List;

public class VoidTouched implements UseModifier, BiomeRestrictedModifier {

	private String name;
	private int level = 0;
	private final static String LEVEL = "trait_level";

	public VoidTouched(String name, int level) {
		this.name = name;
		this.level = level;
	}

	public VoidTouched() {
		this.name = "Void-Touched";
		this.level = 0;
	}

	public Modifier clone() {
		return new VoidTouched();
	}

	@Override
	public NBTTagCompound toNBT() {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setString(NAME, name);
		tag.setInteger(LEVEL, level);
		return tag;
	}

	@Override
	public Modifier fromNBT(NBTTagCompound tag) {
		return new VoidTouched(
			tag.hasKey(NAME) ? tag.getString(NAME) : "Void-Touched",
			tag.hasKey(LEVEL) ? tag.getInteger(LEVEL) : 0);
	}

	@Override
	public String name() {
		if (this.level == 0) {
			return this.name;
		}
		return this.name + " " + LootUtils.roman(this.level + 1);
	}

	@Override
	public String tagName() {
		return "void_touched";
	}

	@Override
	public String color() {
		return TextFormatting.DARK_PURPLE.getFriendlyName();
	}

	@Override
	public String description() {
		float distance = 8.0f + (this.level * 4.0f);
		return "Right-click to teleport up to " + distance + " blocks. Costs 10 durability.";
	}

	@Override
	public void writeToLore(List<String> list, boolean shift) {
		String comp = Modifier.formatText(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public boolean compatible(Modifier mod) {
		// Allow leveling up by being compatible with same modifier
		if (mod.tagName().equals(this.tagName())) {
			return true;
		}
		// Incompatible with other USERS modifiers
		return !ModifierRegistry.USERS.contains(mod);
	}

	@Override
	public boolean forTool(ToolType type) {
		return true; // All tools
	}

	@Override
	public EnumActionResult use(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		return EnumActionResult.PASS;
	}

	@Override
	public boolean use(World world, EntityPlayer player, EnumHand hand) {
		if (world.isRemote) return false;

		float distance = 8.0f + (this.level * 4.0f);
		Vec3d lookVec = player.getLookVec();
		Vec3d destination = new Vec3d(
			player.posX + lookVec.x * distance,
			player.posY + lookVec.y * distance,
			player.posZ + lookVec.z * distance
		);

		BlockPos targetPos = new BlockPos(destination);
		BlockPos safePos = findSafeTeleportLocation(world, targetPos);

		if (safePos != null) {
			player.setPositionAndUpdate(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);

			if (world instanceof WorldServer) {
				WorldServer serverWorld = (WorldServer) world;
				serverWorld.spawnParticle(EnumParticleTypes.PORTAL,
					player.posX, player.posY + 1, player.posZ,
					32, 0.5, 0.5, 0.5, 0.1);
			}

			ItemStack stack = player.getHeldItem(hand);
			stack.damageItem(10, player);
			player.playSound(SoundEvents.ENTITY_ENDERMEN_TELEPORT, 1.0f, 1.0f);
		}

		return true;
	}

	private BlockPos findSafeTeleportLocation(World world, BlockPos target) {
		for (int yOffset = 0; yOffset >= -5; yOffset--) {
			BlockPos checkPos = target.add(0, yOffset, 0);
			IBlockState below = world.getBlockState(checkPos);
			IBlockState at = world.getBlockState(checkPos.up());
			IBlockState above = world.getBlockState(checkPos.up(2));

			if (below.isFullBlock() && !at.isFullBlock() && !above.isFullBlock()) {
				return checkPos.up();
			}
		}
		return null;
	}

	@Override
	public boolean useAnywhere() {
		return true;
	}

	@Override
	public boolean canLevel() {
		return level < 2; // Max level 3 (0, 1, 2)
	}

	@Override
	public void levelUp() {
		this.level++;
	}

	@Override
	public boolean canSpawnInBiome(String biomeKey, float temperature, String dimension) {
		return dimension != null && dimension.equals("minecraft:the_end");
	}
}
