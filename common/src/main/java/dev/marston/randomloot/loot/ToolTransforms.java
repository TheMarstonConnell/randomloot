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

/**
 * Computes the block state a vanilla axe/shovel action produces, without mutating the
 * world (the caller applies it). 26.3 replaced the hard-coded {@code AxeItem.STRIPPABLES}
 * / {@code ShovelItem.FLATTENABLES} maps — and the loader hooks built on them (NeoForge's
 * AXE_STRIP/SHOVEL_FLATTEN {@code ItemAbility}s, Fabric API's StrippableBlockRegistry) —
 * with the data-driven {@code BLOCK_TRANSFORMER} registry, so both loaders now share this
 * pure vanilla lookup.
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

    // Looks the transform up in the registry and runs its BlockStateProvider. This also
    // covers blocks registered the standard datapack way. Filtering by sound isolates the
    // stripping sub-transform from the scrape/wax entries bundled into the same AXE
    // transformer, which keep their dedicated WeatheringCopper/HoneycombItem paths above.
    private static BlockState transformedState(UseOnContext ctx, ResourceKey<BlockTransformer> key,
            Holder<SoundEvent> sound) {
        Level level = ctx.getLevel();
        BlockTransformer transformer = level.registryAccess()
                .lookupOrThrow(Registries.BLOCK_TRANSFORMER).getValue(key);
        if (transformer == null) {
            return null;
        }
        BlockPos pos = ctx.getClickedPos();
        for (BlockTransformer.BlockTransformData transform : transformer.transforms()) {
            if (transform.sound().value() != sound.value()) {
                continue;
            }
            if (transform.disallowedFaces().contains(ctx.getClickedFace())) {
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
