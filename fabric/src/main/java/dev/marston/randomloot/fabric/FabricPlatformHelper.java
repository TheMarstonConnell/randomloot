package dev.marston.randomloot.fabric;

import dev.marston.randomloot.loot.LootArmorItem;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.platform.ToolAction;
import dev.marston.randomloot.platform.ToolTransforms;
import dev.marston.randomloot.platform.services.IPlatformHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String platformName() {
        return "Fabric";
    }

    @Override
    public LootItem createLootItem(Item.Properties props) {
        // Fabric has no item-ability hook; LootItem's own useOn handles strip/scrape/flatten.
        return new LootItem(props);
    }

    @Override
    public LootArmorItem createLootArmorItem(Item.Properties props) {
        return new LootArmorItem(props);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return !state.requiresCorrectToolForDrops() || player.hasCorrectToolForDrops(state);
    }

    @Override
    public BlockState getToolModifiedState(UseOnContext ctx, ToolAction action) {
        BlockState state = ctx.getLevel().getBlockState(ctx.getClickedPos());
        Block block = state.getBlock();

        // 26.3 replaced the static AxeItem.STRIPPABLES / ShovelItem.FLATTENABLES maps (and
        // Fabric API's StrippableBlockRegistry) with the data-driven BlockTransformer datapack
        // registry, so we resolve the registered AXE/SHOVEL transformers and ask them for the
        // resulting state (modded blocks register their transforms there too). Scrape and
        // wax-off still expose their own public maps, so we keep reading those directly to
        // keep each action on its own branch/sound.
        return switch (action) {
            // The AXE transformer bundles strip + scrape + wax-off under one key; restrict to
            // the strip sub-transforms (by sound) so scrape/wax keep their own branches.
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
     * Resolves the registered vanilla {@link BlockTransformer} for {@code key} and returns the
     * first non-null target state it produces for the clicked block (see {@link ToolTransforms}).
     * Fabric API's content registries feed the vanilla transforms, so modded blocks registered
     * the standard Fabric way resolve here too.
     */
    private static BlockState transformedState(UseOnContext ctx, ResourceKey<BlockTransformer> key, Holder<SoundEvent> soundFilter) {
        Level level = ctx.getLevel();
        BlockTransformer transformer = level.registryAccess().lookupOrThrow(Registries.BLOCK_TRANSFORMER).getValueOrThrow(key);
        return ToolTransforms.firstTransform(transformer.transforms(), level, ctx.getClickedPos(), soundFilter);
    }

    @Override
    public Level tooltipLevel(Item.TooltipContext ctx) {
        // Tooltips are only meaningfully built on the client; the indirection keeps
        // client classes out of dedicated-server classloading.
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return FabricClientLevelGetter.get();
        }
        return null;
    }
}
