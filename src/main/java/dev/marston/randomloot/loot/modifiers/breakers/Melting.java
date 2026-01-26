package dev.marston.randomloot.loot.modifiers.breakers;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

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
		// Melting is now handled via MeltingHandler event
		// This method is kept for interface compliance but does nothing
		return false;
	}

	/**
	 * Process drops for smelting - called from MeltingHandler event.
	 * Modifies the drop list in place, replacing items with their smelted versions.
	 */
	public static void processDrops(List<ItemStack> drops) {
		for (int i = 0; i < drops.size(); i++) {
			ItemStack stack = drops.get(i);
			ItemStack result = FurnaceRecipes.instance().getSmeltingResult(stack);

			if (!result.isEmpty()) {
				ItemStack newResult = result.copy();
				newResult.setCount(stack.getCount());
				drops.set(i, newResult);
			}
		}
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
