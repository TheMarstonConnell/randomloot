package dev.marston.randomloot.platform.services;

import dev.marston.randomloot.loot.LootArmorItem;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.platform.ToolAction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface IPlatformHelper {

    /** The loader's name ("NeoForge"/"Fabric"), for logging. */
    String platformName();

    /**
     * Creates the main tool item. Loaders may return a subclass that hooks
     * their item extension points (NeoForge's canPerformAction,
     * supportsEnchantment, isCombineRepairable) into the loader-neutral
     * methods on {@link LootItem}.
     */
    LootItem createLootItem(Item.Properties props);

    /** Creates the armor item; same subclassing contract as {@link #createLootItem}. */
    LootArmorItem createLootArmorItem(Item.Properties props);

    /**
     * Whether the player harvests drops from this block with their current
     * tool. NeoForge routes through its event-aware canHarvestBlock
     * extension; Fabric uses the vanilla correct-tool check.
     */
    boolean canHarvestBlock(BlockState state, Level level, BlockPos pos, Player player);

    /**
     * The level behind a tooltip context, if the loader exposes one (NeoForge
     * patches TooltipContext with level(); on Fabric only the client can
     * supply it). Tooltip code must tolerate null.
     */
    @Nullable
    Level tooltipLevel(Item.TooltipContext ctx);

    /**
     * The state a block turns into when the given tool action is applied
     * (axe strip/scrape/wax-off, shovel flatten), or null when the action
     * does not apply. NeoForge routes through getToolModifiedState so other
     * mods' blocks participate; Fabric falls back to the vanilla conversion
     * maps.
     */
    @Nullable
    BlockState getToolModifiedState(UseOnContext ctx, ToolAction action);
}
