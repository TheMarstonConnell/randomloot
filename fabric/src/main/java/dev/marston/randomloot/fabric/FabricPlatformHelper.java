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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
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
        // 26.3 replaced the static AxeItem.STRIPPABLES / ShovelItem.FLATTENABLES maps (and
        // Fabric API's StrippableBlockRegistry) with the data-driven BlockTransformer datapack
        // registry. Every action resolves through the registered AXE/SHOVEL transformer so
        // vanilla, Fabric-API and datapack entries all apply. The AXE transformer merges
        // strip + scrape + wax-off under one key, so we filter its sub-transforms by sound.
        ResourceKey<BlockTransformer> key = action == ToolAction.SHOVEL_FLATTEN ? BlockTransformers.SHOVEL : BlockTransformers.AXE;
        Holder<SoundEvent> sound = switch (action) {
            case AXE_STRIP -> SoundEvents.AXE_STRIP;
            case AXE_SCRAPE -> SoundEvents.AXE_SCRAPE;
            case AXE_WAX_OFF -> SoundEvents.AXE_WAX_OFF;
            case SHOVEL_FLATTEN -> SoundEvents.SHOVEL_FLATTEN;
        };
        Level level = ctx.getLevel();
        BlockTransformer transformer = level.registryAccess().lookupOrThrow(Registries.BLOCK_TRANSFORMER).getValueOrThrow(key);
        return ToolTransforms.firstTransform(transformer.transforms(), level, ctx.getClickedPos(), ctx.getClickedFace(), sound);
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
