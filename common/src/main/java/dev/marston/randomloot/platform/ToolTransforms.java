package dev.marston.randomloot.platform;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Resolves a tool's vanilla block conversion (strip / scrape / wax / flatten) for the clicked
 * position without applying it, so {@code LootItem}'s own useOn can play the sound and set the block.
 *
 * <p>Since 26.3 vanilla drives strip and flatten through the data-driven {@link BlockTransformer}
 * registry (the old {@code AxeItem.STRIPPABLES} / {@code ShovelItem.FLATTENABLES} maps were removed),
 * while scrape and wax still expose direct block maps. This is all pure vanilla API shared by both
 * loaders; the only platform seam is whether a loader augments the registry transforms with its own
 * datamap-registered entries (NeoForge does via {@code DataMapHooks}; Fabric has no equivalent),
 * supplied as {@code augmenter}.
 */
public final class ToolTransforms {

	/** Lets a loader add its own (e.g. datamap-registered) transforms to the vanilla registry list. */
	@FunctionalInterface
	public interface Augmenter {
		Iterable<BlockTransformer.BlockTransformData> augment(ItemStack stack, List<BlockTransformer.BlockTransformData> registryTransforms);
	}

	private ToolTransforms() {
	}

	public static BlockState modifiedState(UseOnContext ctx, ToolAction action, Augmenter augmenter) {
		BlockState state = ctx.getLevel().getBlockState(ctx.getClickedPos());
		Block block = state.getBlock();

		return switch (action) {
			case AXE_STRIP -> transformed(ctx, BlockTransformers.AXE, SoundEvents.AXE_STRIP, augmenter);
			case AXE_SCRAPE -> WeatheringCopper.getPrevious(state).orElse(null);
			case AXE_WAX_OFF -> {
				Block unwaxed = HoneycombItem.WAX_OFF_BY_BLOCK.get().get(block);
				yield unwaxed == null ? null : unwaxed.withPropertiesOf(state);
			}
			case SHOVEL_FLATTEN -> transformed(ctx, BlockTransformers.SHOVEL, SoundEvents.SHOVEL_FLATTEN, augmenter);
		};
	}

	/**
	 * The result state of a vanilla {@link BlockTransformer}, restricted to the sub-transform that
	 * plays {@code sound} so the axe transformer (strip + scrape + wax bundled) yields only the
	 * requested action. Returns null when no rule matches, mirroring the old map lookups.
	 */
	private static BlockState transformed(UseOnContext ctx, ResourceKey<BlockTransformer> key, Holder<SoundEvent> sound, Augmenter augmenter) {
		Level level = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		BlockTransformer transformer = level.registryAccess().lookupOrThrow(Registries.BLOCK_TRANSFORMER).getValueOrThrow(key);
		for (BlockTransformer.BlockTransformData data : augmenter.augment(ctx.getItemInHand(), transformer.transforms())) {
			if (!data.sound().equals(sound) || data.disallowedFaces().contains(ctx.getClickedFace())) {
				continue;
			}
			BlockState result = data.blockStateProvider().value().getOptionalState(level, level.getRandom(), pos);
			if (result != null) {
				return data.updateFromNeighbors() ? Block.updateFromNeighbourShapes(result, level, pos) : result;
			}
		}
		return null;
	}
}
