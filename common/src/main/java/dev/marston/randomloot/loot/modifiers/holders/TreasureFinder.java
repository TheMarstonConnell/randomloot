package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class TreasureFinder extends BlockHighlighter {

	public TreasureFinder(String name) {
		this.name = name;
	}

	public TreasureFinder() {
		this("Tomb Raider");
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new TreasureFinder(tag.getStringOr(NAME, "Tomb Raider"));
	}

	@Override
	public String tagName() {
		return "spawner";
	}

	@Override
	public ChatFormatting color() {
		return ChatFormatting.DARK_AQUA;
	}

	@Override
	public String description() {
		return "While held or worn, spawners around you will glow.";
	}

	@Override
	public boolean forTool(ToolType type) {
		return true;
	}

	@Override
	protected boolean isTarget(Block block) {
		return block == Blocks.SPAWNER;
	}
}
