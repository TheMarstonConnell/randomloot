package dev.marston.randomloot.platform;

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
 * Vanilla tool-action results (the block a strip/scrape/wax-off/flatten turns into),
 * queried without applying them so {@code LootItem} keeps its "compute, then apply" flow.
 *
 * <p>26.3 unified strip/flatten into the data-driven {@code BLOCK_TRANSFORMER} registry
 * (replacing {@code AxeItem.STRIPPABLES}/{@code ShovelItem.FLATTENABLES}, Fabric API's
 * strippable/flattenable registries, and NeoForge's {@code ItemAbilities} for these), so
 * both loaders now share this pure-vanilla path and modded blocks registered into the
 * datamap participate on either loader. Scrape/wax still key off the vanilla copper/wax maps.
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

    // Compute the result of a vanilla BlockTransformer without applying it. A transformer
    // (e.g. AXE) can bundle several actions, so we keep only the transforms whose sound
    // matches the requested action, mirroring the per-action sounds LootItem plays.
    private static BlockState transformedState(UseOnContext ctx, ResourceKey<BlockTransformer> key, Holder<SoundEvent> sound) {
        Level level = ctx.getLevel();
        BlockTransformer transformer = level.registryAccess().lookupOrThrow(Registries.BLOCK_TRANSFORMER).getValue(key);
        if (transformer == null) {
            return null;
        }
        Direction face = ctx.getClickedFace();
        for (BlockTransformer.BlockTransformData data : transformer.transforms()) {
            if (data.sound().value() != sound.value() || data.disallowedFaces().contains(face)) {
                continue;
            }
            BlockState result = data.blockStateProvider().value().getOptionalState(level, level.getRandom(), ctx.getClickedPos());
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
