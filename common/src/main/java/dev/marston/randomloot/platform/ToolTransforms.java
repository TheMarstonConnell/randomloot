package dev.marston.randomloot.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Vanilla-only resolution of a tool's block conversion (strip / scrape / wax / flatten).
 * 26.3 replaced the {@code AxeItem.STRIPPABLES} / {@code ShovelItem.FLATTENABLES} maps and
 * NeoForge's {@code ItemAbility}-based tool state with the data-driven {@code BLOCK_TRANSFORMER}
 * datamap, so both loaders now resolve this the same way — it lives in common instead of the
 * platform seam. Callers apply the result themselves (set block, play sound, damage tool).
 */
public final class ToolTransforms {

    private ToolTransforms() {
    }

    public static BlockState modifiedState(UseOnContext ctx, ToolAction action) {
        BlockState state = ctx.getLevel().getBlockState(ctx.getClickedPos());
        Block block = state.getBlock();

        return switch (action) {
            case AXE_STRIP -> transformedState(ctx, BlockTransformers.AXE, SoundEvents.AXE_STRIP);
            case AXE_SCRAPE -> WeatheringCopper.getPrevious(state).orElse(null);
            case AXE_WAX_OFF -> {
                Block unwaxed = HoneycombItem.WAX_OFF_BY_BLOCK.get().get(block);
                yield unwaxed == null ? null : unwaxed.withPropertiesOf(state);
            }
            case SHOVEL_FLATTEN -> transformedState(ctx, BlockTransformers.SHOVEL, SoundEvents.SHOVEL_FLATTEN);
        };
    }

    /**
     * Computes the block-transformer result the same way vanilla's {@link BlockTransformer} does
     * (matching state provider + optional neighbour-shape update) but without applying it. A tool's
     * transformer bundles every action (strip, scrape, wax all sit under {@code AXE}), so we filter
     * by the action's sound to isolate the one requested.
     */
    private static BlockState transformedState(UseOnContext ctx, ResourceKey<BlockTransformer> key, Holder<SoundEvent> sound) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Direction face = ctx.getClickedFace();
        BlockTransformer transformer = level.registryAccess()
                .lookupOrThrow(Registries.BLOCK_TRANSFORMER)
                .getValueOrThrow(key);
        for (BlockTransformer.BlockTransformData data : transformer.transforms()) {
            if (data.sound().value() != sound.value() || data.disallowedFaces().contains(face)) {
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
