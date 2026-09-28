package dev.marston.randomloot.neoforge;

import dev.marston.randomloot.loot.LootArmorItem;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.platform.ToolAction;
import dev.marston.randomloot.platform.ToolTransforms;
import dev.marston.randomloot.platform.services.IPlatformHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.DataMapHooks;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String platformName() {
        return "NeoForge";
    }

    @Override
    public LootItem createLootItem(Item.Properties props) {
        return new NeoForgeLootItem(props);
    }

    @Override
    public LootArmorItem createLootArmorItem(Item.Properties props) {
        return new NeoForgeLootArmorItem(props);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return state.canHarvestBlock(level, pos, player);
    }

    @Override
    public Level tooltipLevel(Item.TooltipContext ctx) {
        return ctx.level();
    }

    @Override
    public BlockState getToolModifiedState(UseOnContext ctx, ToolAction action) {
        // Same pure-vanilla resolution as Fabric, but fold in NeoForge's datamap-registered
        // transformers so modded strippables/flattenables keep working.
        return ToolTransforms.modifiedState(ctx, action, DataMapHooks::appendDatamapTransformers);
    }
}
