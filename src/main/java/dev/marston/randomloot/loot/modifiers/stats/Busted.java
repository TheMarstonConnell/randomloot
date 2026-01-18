package dev.marston.randomloot.loot.modifiers.stats;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.StatsModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class Busted implements StatsModifier {

	private String name;

	public Busted() {
		this("Busted");
	}

	public Busted(String name) {
		this.name = name;
	}

	public Modifier clone() {
		return new Busted(this.name);
	}

	@Override
	public CompoundTag toNBT() {

		CompoundTag tag = new CompoundTag();

		tag.putString(NAME, name);

		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Busted(tag.getStringOr(NAME, "Busted"));
	}

	@Override
	public String name() {

		return name;
	}

	@Override
	public String tagName() {
		return "busted";
	}

	@Override
	public String color() {

		return ChatFormatting.LIGHT_PURPLE.getName();

	}

	@Override
	public String description() {
		return "Dig speed is increased as tool durability drops.";
	}

	@Override
	public void writeToLore(List<Component> list, boolean shift) {

		MutableComponent comp = Modifier.makeComp(this.name(), this.color());

		list.add(comp);
	}

	@Override
	public Component writeDetailsToLore(Level level) {

		return null;
	}

	@Override
	public boolean compatible(Modifier mod) {
		return true;
	}

	@Override
	public boolean forTool(ToolType type) {
		return type == ToolType.AXE || type == ToolType.PICKAXE || type == ToolType.SHOVEL;
	}

	public boolean canLevel() {
		return false;
	}

	public void levelUp() {
		return;
	}

	@Override
	public float getStats(ItemStack itemstack) {
		float maxDamage = itemstack.getMaxDamage();
		float currentDamage = itemstack.getDamageValue();

		float ratio = currentDamage / maxDamage;

		float maxGoodness = 1.0f;

		return ratio * maxGoodness + 1.0f;

	}
}
