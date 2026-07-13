package dev.marston.randomloot.neoforge;

import dev.marston.randomloot.loot.LootArmorItem;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.platform.ToolAction;
import dev.marston.randomloot.platform.services.IPlatformHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

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

    static ItemAbility toItemAbility(ToolAction action) {
        return switch (action) {
            case AXE_STRIP -> ItemAbilities.AXE_STRIP;
            case AXE_SCRAPE -> ItemAbilities.AXE_SCRAPE;
            case AXE_WAX_OFF -> ItemAbilities.AXE_WAX_OFF;
            case SHOVEL_FLATTEN -> ItemAbilities.SHOVEL_FLATTEN;
        };
    }

    private static final Map<ItemAbility, ToolAction> BY_ABILITY = new IdentityHashMap<>();
    static {
        for (ToolAction action : ToolAction.values()) {
            BY_ABILITY.put(toItemAbility(action), action);
        }
    }

    /** The ToolAction behind a NeoForge ItemAbility, or null for abilities the mod doesn't model. */
    @Nullable
    static ToolAction fromItemAbility(ItemAbility ability) {
        return BY_ABILITY.get(ability);
    }

    @Override
    public BlockState getToolModifiedState(UseOnContext ctx, ToolAction action) {
        return ctx.getLevel().getBlockState(ctx.getClickedPos()).getToolModifiedState(ctx, toItemAbility(action), false);
    }
}
