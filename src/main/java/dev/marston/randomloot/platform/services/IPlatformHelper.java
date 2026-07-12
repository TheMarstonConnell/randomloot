package dev.marston.randomloot.platform.services;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.platform.ToolAction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface IPlatformHelper {

    /** The loader's name ("NeoForge"/"Fabric"), for logging. */
    String platformName();

    /**
     * Creates the main tool item. Loaders may return a subclass that hooks
     * their tool-action API (NeoForge's canPerformAction) into
     * {@link LootItem#canPerform}.
     */
    LootItem createLootItem(Item.Properties props);

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
