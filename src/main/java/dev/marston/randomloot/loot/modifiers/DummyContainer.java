package dev.marston.randomloot.loot.modifiers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

public class DummyContainer implements IInventory {

	ItemStack item;

	public DummyContainer(ItemStack stack) {
		item = stack;
	}

	public DummyContainer() {
		this(ItemStack.EMPTY);
	}

	@Override
	public void clear() {
		item = ItemStack.EMPTY;
	}

	@Override
	public int getSizeInventory() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return item.isEmpty();
	}

	@Override
	public ItemStack getStackInSlot(int index) {
		return item;
	}

	@Override
	public ItemStack decrStackSize(int index, int count) {
		if (item.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack result = item.splitStack(count);
		if (item.isEmpty()) {
			item = ItemStack.EMPTY;
		}
		return result;
	}

	@Override
	public ItemStack removeStackFromSlot(int index) {
		ItemStack result = item;
		item = ItemStack.EMPTY;
		return result;
	}

	@Override
	public void setInventorySlotContents(int index, ItemStack stack) {
		item = stack;
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public void markDirty() {
	}

	@Override
	public boolean isUsableByPlayer(EntityPlayer player) {
		return true;
	}

	@Override
	public void openInventory(EntityPlayer player) {
	}

	@Override
	public void closeInventory(EntityPlayer player) {
	}

	@Override
	public boolean isItemValidForSlot(int index, ItemStack stack) {
		return true;
	}

	@Override
	public int getField(int id) {
		return 0;
	}

	@Override
	public void setField(int id, int value) {
	}

	@Override
	public int getFieldCount() {
		return 0;
	}

	@Override
	public String getName() {
		return "DummyContainer";
	}

	@Override
	public boolean hasCustomName() {
		return false;
	}

	@Override
	public ITextComponent getDisplayName() {
		return new TextComponentString(getName());
	}
}
