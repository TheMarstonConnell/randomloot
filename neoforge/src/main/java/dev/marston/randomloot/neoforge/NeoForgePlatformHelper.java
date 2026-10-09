package dev.marston.randomloot.neoforge;

import dev.marston.randomloot.loot.LootArmorItem;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.platform.ToolAction;
import dev.marston.randomloot.platform.ToolTransforms;
import dev.marston.randomloot.platform.services.IPlatformHelper;
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
        // 26.3 moved strip/scrape/wax-off/flatten out of ItemAbilities and into the vanilla
        // BlockTransformer datapack registry; DataMapHooks.getAllTransformers folds NeoForge's
        // modded datamap entries into the vanilla transforms so modded blocks still resolve.
        Level level = ctx.getLevel();
        ResourceKey<BlockTransformer> key = action == ToolAction.SHOVEL_FLATTEN ? BlockTransformers.SHOVEL : BlockTransformers.AXE;
        Holder<BlockTransformer> holder = level.registryAccess().lookupOrThrow(Registries.BLOCK_TRANSFORMER).getOrThrow(key);
        Holder<SoundEvent> sound = switch (action) {
            case AXE_STRIP -> SoundEvents.AXE_STRIP;
            case AXE_SCRAPE -> SoundEvents.AXE_SCRAPE;
            case AXE_WAX_OFF -> SoundEvents.AXE_WAX_OFF;
            case SHOVEL_FLATTEN -> SoundEvents.SHOVEL_FLATTEN;
        };
        return ToolTransforms.firstTransform(DataMapHooks.getAllTransformers(holder), level, ctx.getClickedPos(), sound);
    }
}
