package dev.marston.randomloot.forge;

import dev.marston.randomloot.loot.LootArmorItem;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.platform.services.IPlatformHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

public class ForgePlatformHelper implements IPlatformHelper {

    @Override
    public String platformName() {
        return "Forge";
    }

    @Override
    public LootItem createLootItem(Item.Properties props) {
        return new ForgeLootItem(props);
    }

    @Override
    public LootArmorItem createLootArmorItem(Item.Properties props) {
        return new ForgeLootArmorItem(props);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return state.canHarvestBlock(level, pos, player);
    }

    // The mod's loader-neutral ToolAction shares its simple name with Forge's, so the
    // common one is spelled out in full here.
    static ToolAction toForgeAction(dev.marston.randomloot.platform.ToolAction action) {
        return switch (action) {
            case AXE_STRIP -> ToolActions.AXE_STRIP;
            case AXE_SCRAPE -> ToolActions.AXE_SCRAPE;
            case AXE_WAX_OFF -> ToolActions.AXE_WAX_OFF;
            case SHOVEL_FLATTEN -> ToolActions.SHOVEL_FLATTEN;
        };
    }

    private static final Map<ToolAction, dev.marston.randomloot.platform.ToolAction> BY_FORGE_ACTION = new IdentityHashMap<>();
    static {
        for (dev.marston.randomloot.platform.ToolAction action : dev.marston.randomloot.platform.ToolAction.values()) {
            BY_FORGE_ACTION.put(toForgeAction(action), action);
        }
    }

    /** The mod's ToolAction behind a Forge ToolAction, or null for actions the mod doesn't model. */
    @Nullable
    static dev.marston.randomloot.platform.ToolAction fromForgeAction(ToolAction action) {
        return BY_FORGE_ACTION.get(action);
    }

    @Override
    public BlockState getToolModifiedState(UseOnContext ctx, dev.marston.randomloot.platform.ToolAction action) {
        return ctx.getLevel().getBlockState(ctx.getClickedPos()).getToolModifiedState(ctx, toForgeAction(action), false);
    }
}
