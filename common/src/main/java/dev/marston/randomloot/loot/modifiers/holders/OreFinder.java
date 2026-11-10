package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.NbtCompat;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class OreFinder extends BlockHighlighter {

	// Datapack-editable; see data/randomloot/tags/block/. Ores are matched by tag, not by
	// display name: the name is translated with the integrated server's language, so
	// "contains ore" found nothing outside English and lit up Spore Blossoms in it.
	private static final TagKey<Block> DETECTABLE = TagKey.create(Registries.BLOCK,
			ResourceLocation.fromNamespaceAndPath(RandomLoot.MODID, "detectable_ores"));
	private static final TagKey<Block> UNDETECTABLE = TagKey.create(Registries.BLOCK,
			ResourceLocation.fromNamespaceAndPath(RandomLoot.MODID, "undetectable_ores"));

	public OreFinder(String name) {
		this.name = name;
	}

	public OreFinder() {
		this("Detecting");
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new OreFinder(NbtCompat.getStringOr(tag, NAME, "Detecting"));
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
	protected boolean isTarget(BlockState state) {
		return state.is(DETECTABLE) && !state.is(UNDETECTABLE);
	}
}
