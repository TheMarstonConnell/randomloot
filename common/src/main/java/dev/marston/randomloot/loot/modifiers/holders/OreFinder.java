package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.RandomLoot;
import net.minecraft.core.registries.Registries;
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

	@Override
	public String description() {
		return "While held or worn, ores around you will glow.";
	}

	@Override
	protected boolean isTarget(BlockState state) {
		return state.is(DETECTABLE) && !state.is(UNDETECTABLE);
	}
}
