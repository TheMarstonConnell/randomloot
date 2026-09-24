package dev.marston.randomloot.platform.services;

import dev.marston.randomloot.loot.LootArmorItem;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.platform.ToolAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
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
     * does not apply. Since 26.3 both loaders read the same vanilla source:
     * strip/flatten resolve through the {@code BLOCK_TRANSFORMER} registry
     * (so datapack- and mod-registered transformers participate), while
     * scrape/wax-off keep using the vanilla weathering/honeycomb maps.
     */
    @Nullable
    default BlockState getToolModifiedState(UseOnContext ctx, ToolAction action) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        return switch (action) {
            case AXE_SCRAPE -> WeatheringCopper.getPrevious(state).orElse(null);
            case AXE_WAX_OFF -> {
                Block unwaxed = HoneycombItem.WAX_OFF_BY_BLOCK.get().get(state.getBlock());
                yield unwaxed == null ? null : unwaxed.withPropertiesOf(state);
            }
            case AXE_STRIP -> transformedState(level, pos, BlockTransformers.AXE, SoundEvents.AXE_STRIP.value());
            case SHOVEL_FLATTEN -> transformedState(level, pos, BlockTransformers.SHOVEL, null);
        };
    }

    /**
     * Evaluates a vanilla block transformer against the block at {@code pos},
     * returning the state it would become (without applying it). When
     * {@code onlySound} is non-null only the transform entries carrying that
     * sound are considered, which isolates axe stripping from the scrape/wax
     * entries bundled into the same {@code AXE} transformer.
     */
    @Nullable
    private static BlockState transformedState(Level level, BlockPos pos,
            ResourceKey<BlockTransformer> key, @Nullable SoundEvent onlySound) {
        BlockTransformer transformer = level.registryAccess()
                .lookupOrThrow(Registries.BLOCK_TRANSFORMER).getValueOrThrow(key);
        for (BlockTransformer.BlockTransformData data : transformer.transforms()) {
            if (onlySound != null && data.sound().value() != onlySound) {
                continue;
            }
            BlockState result = data.blockStateProvider().value().getOptionalState(level, level.getRandom(), pos);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
