package dev.marston.randomloot.loot.modifiers.breakers;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public class Explode implements BlockBreakModifier {

	private String name;
	private float power;
	private final static String POWER = "power";

	public Explode(String name, float power) {
		this.name = name;
		this.power = power;
	}

	public Explode() {
		this.name = "Explosive";
		this.power = 4.0f;
	}

	public Modifier clone() {
		return new Explode();
	}

	@Override
	public boolean startBreak(ItemStack itemstack, BlockPos pos, EntityLivingBase player) {

		World world = player.world;

		world.createExplosion(player, pos.getX(), pos.getY() + 0.5, pos.getZ(), power, true);

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
		return new Explode(
			tag.hasKey(NAME) ? tag.getString(NAME) : "Explosive",
			tag.hasKey(POWER) ? tag.getFloat(POWER) : 4.0f);
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "explode";
	}

	@Override
	public String color() {
		return "red";
	}

	@Override
	public String description() {
		return "Upon breaking a block (allowed by tool type), the current block position will explode causing damage to surrounding blocks.";
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
