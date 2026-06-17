package dev.marston.randomloot.loot.modifiers.stats;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.StatsModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;


public class Busted extends AbstractModifier implements StatsModifier {

	public Busted() {
		this("Busted");
	}

	public Busted(String name) {
		this.name = name;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Busted(tag.getStringOr(NAME, "Busted"));
	}

	@Override
	public String tagName() {
		return "busted";
	}

	@Override
	public ChatFormatting color() {

		return ChatFormatting.LIGHT_PURPLE;

	}

	@Override
	public String description() {
		return "Dig speed is increased as tool durability drops.";
	}

	@Override
	public boolean forTool(ToolType type) {
		return type == ToolType.AXE || type == ToolType.PICKAXE || type == ToolType.SHOVEL;
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
