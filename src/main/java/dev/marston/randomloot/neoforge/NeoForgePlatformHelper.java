package dev.marston.randomloot.neoforge;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.platform.ToolAction;
import dev.marston.randomloot.platform.services.IPlatformHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String platformName() {
        return "NeoForge";
    }

    @Override
    public LootItem createLootItem(Item.Properties props) {
        return new NeoForgeLootItem(props);
    }

    static ItemAbility toItemAbility(ToolAction action) {
        return switch (action) {
            case AXE_STRIP -> ItemAbilities.AXE_STRIP;
            case AXE_SCRAPE -> ItemAbilities.AXE_SCRAPE;
            case AXE_WAX_OFF -> ItemAbilities.AXE_WAX_OFF;
            case SHOVEL_FLATTEN -> ItemAbilities.SHOVEL_FLATTEN;
        };
    }

    @Override
    public BlockState getToolModifiedState(UseOnContext ctx, ToolAction action) {
        return ctx.getLevel().getBlockState(ctx.getClickedPos()).getToolModifiedState(ctx, toItemAbility(action), false);
    }
}
