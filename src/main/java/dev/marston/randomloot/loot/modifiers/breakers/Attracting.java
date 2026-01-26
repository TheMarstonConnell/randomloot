package dev.marston.randomloot.loot.modifiers.breakers;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.List;

public class Attracting implements BlockBreakModifier {

	private String name;
	private float power;
	private final static String POWER = "power";

	public Attracting(String name, float power) {
		this.name = name;
		this.power = power;
	}

	public Attracting() {
		this.name = "Magnetic";
		this.power = 2.0f;
	}

	public Modifier clone() {
		return new Attracting();
	}

	@Override
	public boolean startBreak(ItemStack itemstack, BlockPos pos, EntityLivingBase player) {

		World world = player.world;

		if (world.isRemote) {
			return false;
		}

		final WorldServer serverWorld = (WorldServer) world;
		AxisAlignedBB box = new AxisAlignedBB(
			pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,
			pos.getX() + 2, pos.getY() + 2, pos.getZ() + 2);

		final double playerX = player.posX;
		final double playerY = player.posY;
		final double playerZ = player.posZ;

		// Schedule execution after a short delay to allow block drops to spawn
		serverWorld.addScheduledTask(() -> {
			// Schedule for next tick to allow drops to spawn
			serverWorld.addScheduledTask(() -> {
				List<Entity> items = world.getEntitiesWithinAABB(Entity.class, box);

				for (Entity entity : items) {
					if (entity instanceof EntityItem) {
						entity.setPosition(playerX, playerY, playerZ);
					}
				}
			});
		});

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
		return new Attracting(
			tag.hasKey(NAME) ? tag.getString(NAME) : "Magnetic",
			tag.hasKey(POWER) ? tag.getFloat(POWER) : 2.0f);
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "attracting";
	}

	@Override
	public String color() {
		return "red";
	}

	@Override
	public String description() {
		return "Upon breaking a block (allowed by tool type), all items at that block's position will teleport to you.";
	}

	@Override
	public void writeToLore(List<String> list, boolean shift) {
		String comp = Modifier.formatText(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.PICKAXE) || type.equals(ToolType.AXE) || type.equals(ToolType.SHOVEL);
	}
}
