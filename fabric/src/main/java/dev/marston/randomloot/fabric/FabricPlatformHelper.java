package dev.marston.randomloot.fabric;

import dev.marston.randomloot.loot.LootArmorItem;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.ToolTransforms;
import dev.marston.randomloot.platform.ToolAction;
import dev.marston.randomloot.platform.services.IPlatformHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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
        // 26.3 moved tool transforms into the vanilla BLOCK_TRANSFORMER registry; the
        // shared common helper computes the result state for both loaders.
        return ToolTransforms.modifiedState(ctx, action);
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
