package dev.marston.randomloot.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Shared pure-query resolution of vanilla {@link BlockTransformer}s for both loaders. */
public final class ToolTransforms {

    private ToolTransforms() {
    }

    /**
     * Returns the state the first transform carrying {@code soundFilter} produces for the block
     * at {@code pos}, mirroring {@code BlockTransformer#transformBlock} (disallowed-face skip,
     * neighbour-shape fixup) without side effects, or {@code null} if none applies.
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
