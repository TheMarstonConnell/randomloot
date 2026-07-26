package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;

public class OreFinder extends BlockHighlighter {

	public OreFinder(String name) {
		this.name = name;
	}

	public OreFinder() {
		this("Detecting");
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new OreFinder(tag.getStringOr(NAME, "Detecting"));
	}

	@Override
	public String tagName() {
		return "detecting";
	}

	@Override
	public ChatFormatting color() {
		return ChatFormatting.WHITE;
	}

	@Override
	public String description() {
		return "While held or worn, ores around you will glow.";
	}

	@Override
	public boolean forTool(ToolType type) {
		return isMiningTool(type) || type.isArmor();
	}

	@Override
	protected boolean isTarget(Block block) {
		return block.getName().getString().toLowerCase().contains("ore");
	}
}
