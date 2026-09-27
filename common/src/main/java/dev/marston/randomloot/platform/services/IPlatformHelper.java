package dev.marston.randomloot.platform.services;

import dev.marston.randomloot.loot.LootArmorItem;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.platform.ToolAction;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface IPlatformHelper {

    /** The loader's name ("NeoForge"/"Fabric"), for logging. */
    String platformName();

    /**
     * Creates the main tool item. Loaders may return a subclass that hooks
     * their item extension points (NeoForge's supportsEnchantment,
     * isCombineRepairable) into the loader-neutral methods on {@link LootItem}.
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
     * does not apply. 26.3 unified these behaviours into the vanilla
     * {@link BlockTransformer} registry (which vanilla tools carry as
     * {@code DataComponents.BLOCK_TRANSFORMER}), so this is now identical on
     * both loaders and lives here rather than in a platform seam.
     */
    @Nullable
    default BlockState getToolModifiedState(UseOnContext ctx, ToolAction action) {
        return switch (action) {
            case AXE_STRIP -> transformedState(ctx, BlockTransformers.AXE, SoundEvents.AXE_STRIP);
            case AXE_SCRAPE -> transformedState(ctx, BlockTransformers.AXE, SoundEvents.AXE_SCRAPE);
            case AXE_WAX_OFF -> transformedState(ctx, BlockTransformers.AXE, SoundEvents.AXE_WAX_OFF);
            case SHOVEL_FLATTEN -> transformedState(ctx, BlockTransformers.SHOVEL, SoundEvents.SHOVEL_FLATTEN);
        };
    }

    /**
     * Resolves the block-state a vanilla tool transform would produce at the clicked
     * position without applying it (the caller performs the placement/sound/durability).
     * A tool's transformer bundles every action it performs; each transform is tagged with
     * its sound, so matching on that preserves the per-action split the loot item drives.
     */
    @Nullable
    private static BlockState transformedState(UseOnContext ctx, ResourceKey<BlockTransformer> key, Holder<SoundEvent> sound) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockTransformer transformer = level.registryAccess().lookupOrThrow(Registries.BLOCK_TRANSFORMER).getValueOrThrow(key);

        for (BlockTransformer.BlockTransformData transform : transformer.transforms()) {
            if (!transform.sound().value().equals(sound.value())) {
                continue;
            }
            if (transform.disallowedFaces().contains(ctx.getClickedFace())) {
                continue;
            }
            BlockState result = transform.blockStateProvider().value().getOptionalState(level, level.getRandom(), pos);
            if (result != null) {
                return transform.updateFromNeighbors() ? Block.updateFromNeighbourShapes(result, level, pos) : result;
            }
        }
        return null;
    }
}
