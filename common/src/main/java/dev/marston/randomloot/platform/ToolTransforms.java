package dev.marston.randomloot.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared vanilla resolution for the data-driven tool block transforms introduced in
 * 26.3 ({@code net.minecraft.core.component.BlockTransformer}, keyed in the
 * {@code BLOCK_TRANSFORMER} datapack registry). It replaces the removed static
 * {@code AxeItem.STRIPPABLES} / {@code ShovelItem.FLATTENABLES} maps. Both loaders feed
 * in the transform list for the relevant key (each loader adding its own modded entries
 * first) and get back the resulting block state without running the full interaction.
 */
public final class ToolTransforms {

    private ToolTransforms() {
    }

    /**
     * Returns the resulting state for the block at {@code pos}, mirroring
     * {@code BlockTransformer#transformBlock}'s resolution (sub-transform selection,
     * disallowed-face skipping and neighbour-shape fixup) without the side effects, or
     * {@code null} when no sub-transform applies. Only transforms carrying {@code soundFilter}
     * are considered, which separates strip / scrape / wax-off (all merged under the one AXE
     * transformer) back into the mod's individual tool actions.
     */
    public static BlockState firstTransform(Iterable<BlockTransformer.BlockTransformData> transforms,
            Level level, BlockPos pos, Direction clickedFace, Holder<SoundEvent> soundFilter) {
        for (BlockTransformer.BlockTransformData transform : transforms) {
            if (transform.sound().value() != soundFilter.value()) {
                continue;
            }
            if (transform.disallowedFaces().contains(clickedFace)) {
                continue;
            }
            BlockState newState = transform.blockStateProvider().value().getOptionalState(level, level.getRandom(), pos);
            if (newState != null) {
                return transform.updateFromNeighbors() ? Block.updateFromNeighbourShapes(newState, level, pos) : newState;
            }
        }
        return null;
    }
}
