package dev.marston.randomloot.neoforge;

import dev.marston.randomloot.loot.LootArmorItem;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.platform.ToolAction;
import dev.marston.randomloot.platform.services.IPlatformHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.core.registries.Registries;
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
        // 26.3 dropped the strip/scrape/flatten/wax-off ItemAbilities (and BlockState's
        // getToolModifiedState) in favour of the data-driven BlockTransformer registry.
        // DataMapHooks appends NeoForge's data-map transformers so modded strippables and
        // flattenables registered the NeoForge way keep working.
        Level level = ctx.getLevel();
        Holder<BlockTransformer> transformer = level.registryAccess()
                .lookupOrThrow(Registries.BLOCK_TRANSFORMER)
                .getOrThrow(LootUtils.transformerKey(action));
        return LootUtils.resolveBlockTransform(DataMapHooks.getAllTransformers(transformer), level,
                ctx.getClickedPos(), ctx.getClickedFace(), LootUtils.transformerSoundFilter(action));
    }
}
