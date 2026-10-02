package dev.marston.randomloot.neoforge;

import dev.marston.randomloot.loot.LootArmorItem;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.platform.ToolAction;
import dev.marston.randomloot.platform.services.IPlatformHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
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

    // 26.3 removed the predefined ItemAbilities.AXE_*/SHOVEL_FLATTEN constants (vanilla
    // tool transforms moved to the BlockTransformer datamap), but abilities are still
    // interned by name, so canPerformAction can keep advertising these for mod compat.
    // The actual strip/scrape/wax/flatten behaviour runs through LootItem's own useOn.
    static ItemAbility toItemAbility(ToolAction action) {
        return switch (action) {
            case AXE_STRIP -> ItemAbility.get("axe_strip");
            case AXE_SCRAPE -> ItemAbility.get("axe_scrape");
            case AXE_WAX_OFF -> ItemAbility.get("axe_wax_off");
            case SHOVEL_FLATTEN -> ItemAbility.get("shovel_flatten");
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
}
