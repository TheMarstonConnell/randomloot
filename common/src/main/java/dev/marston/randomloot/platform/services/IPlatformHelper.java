package dev.marston.randomloot.platform.services;

import dev.marston.randomloot.loot.LootArmorItem;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.resources.ResourceKey;
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
     * Resolve the given vanilla block transformer ({@code BlockTransformers.AXE} /
     * {@code .SHOVEL}) at the clicked position: the state the block turns into plus the
     * sound to play, or null when nothing applies. NeoForge folds in its data-map
     * transformers so other mods' blocks participate; Fabric reads the registry entry
     * directly (Fabric mods add entries via datapack).
     */
    @Nullable
    LootUtils.ToolTransform resolveToolTransform(UseOnContext ctx, ResourceKey<BlockTransformer> transformer);
}
