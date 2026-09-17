package dev.marston.randomloot.loot;

import dev.marston.randomloot.platform.ToolAction;
import net.minecraft.core.BlockPos;
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
import org.jetbrains.annotations.Nullable;

/**
 * Loader-neutral computation of the block state a vanilla tool right-click produces.
 * 26.3 unified strip / scrape / wax-off / flatten under the data-driven
 * {@code BlockTransformer} registry, dropping the old per-loader seams (the vanilla
 * {@code AxeItem.STRIPPABLES} / {@code ShovelItem.FLATTENABLES} maps, NeoForge's
 * {@code ItemAbilities.AXE_STRIP} and friends, and Fabric API's
 * {@code StrippableBlockRegistry}). Both loaders now share this vanilla path: strip and
 * flatten read the resulting state straight from the AXE / SHOVEL transformers (covering
 * vanilla and modded blocks), while scrape and wax-off key off the vanilla BiMaps those
 * transformers are themselves built from.
 */
public final class ToolTransforms {

    private ToolTransforms() {
    }

    @Nullable
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
            case SHOVEL_FLATTEN -> transformedState(ctx, BlockTransformers.SHOVEL, null);
        };
    }

    /**
     * The block state the given transformer would produce at the clicked position, or
     * null if none of its rules match. When {@code sound} is non-null only transforms
     * playing that sound are considered, so a single behaviour (strip) can be isolated
     * from the others bundled into the same transformer (scrape / wax-off).
     */
    @Nullable
    private static BlockState transformedState(UseOnContext ctx, ResourceKey<BlockTransformer> key, @Nullable Holder<SoundEvent> sound) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockTransformer transformer = level.registryAccess().lookupOrThrow(Registries.BLOCK_TRANSFORMER).getOrThrow(key).value();
        for (BlockTransformer.BlockTransformData transform : transformer.transforms()) {
            if (sound != null && transform.sound().value() != sound.value()) {
                continue;
            }
            BlockState result = transform.blockStateProvider().value().getOptionalState(level, level.getRandom(), pos);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
