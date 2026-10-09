package dev.marston.randomloot.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
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
     * Returns the first non-null target state produced for the block at {@code pos},
     * mirroring {@code BlockTransformer#transformBlock}'s resolution without the side
     * effects. When {@code soundFilter} is non-null only transforms carrying that sound
     * are considered, which separates strip / scrape / wax-off (all merged under the one
     * AXE transformer) back into the mod's individual tool actions.
     */
    public static BlockState firstTransform(Iterable<BlockTransformer.BlockTransformData> transforms,
            Level level, BlockPos pos, Holder<SoundEvent> soundFilter) {
        for (BlockTransformer.BlockTransformData transform : transforms) {
            if (soundFilter != null && transform.sound().value() != soundFilter.value()) {
                continue;
            }
            BlockState newState = transform.blockStateProvider().value().getOptionalState(level, level.getRandom(), pos);
            if (newState != null) {
                return newState;
            }
        }
        return null;
    }
}
