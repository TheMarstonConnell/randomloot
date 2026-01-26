package dev.marston.randomloot.loot.modifiers.breakers;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.List;

public class Melting implements BlockBreakModifier {

	private String name;
	private float power;
	private final static String POWER = "power";

	public Melting(String name, float power) {
		this.name = name;
		this.power = power;
	}

	public Melting() {
		this.name = "Melting";
		this.power = 1.0f;
	}

	public Modifier clone() {
		return new Melting();
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

		// Schedule execution after a short delay to allow block drops to spawn
		serverWorld.addScheduledTask(() -> {
			// Schedule for next tick to allow drops to spawn
			serverWorld.addScheduledTask(() -> {
				List<Entity> items = world.getEntitiesWithinAABB(Entity.class, box);

				for (Entity entity : items) {
					if (!(entity instanceof EntityItem)) {
						continue;
					}

					EntityItem i = (EntityItem) entity;
					if (i.getAge() > 10) {
						continue;
					}

					ItemStack stack = i.getItem();
					ItemStack result = FurnaceRecipes.instance().getSmeltingResult(stack);

					if (result.isEmpty()) {
						continue;
					}

					// Copy the result with correct count
					ItemStack newResult = result.copy();
					newResult.setCount(stack.getCount());

					// Create new item entity
					EntityItem k = new EntityItem(world, i.posX, i.posY, i.posZ, newResult);
					k.motionX = i.motionX;
					k.motionY = i.motionY;
					k.motionZ = i.motionZ;

					// Remove old item
					i.setDead();

					// Spawn new item
					world.spawnEntity(k);
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
		return new Melting(
			tag.hasKey(NAME) ? tag.getString(NAME) : "Melting",
			tag.hasKey(POWER) ? tag.getFloat(POWER) : 1.0f);
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "melting";
	}

	@Override
	public String color() {
		return "red";
	}

	@Override
	public String description() {
		return "Items dropped by blocks broken with this tool will be smelted.";
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
