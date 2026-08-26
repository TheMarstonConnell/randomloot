package dev.marston.randomloot.fabric;

import dev.marston.randomloot.loot.LootArmorItem;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.platform.ToolAction;
import dev.marston.randomloot.platform.services.IPlatformHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String platformName() {
        return "Fabric";
    }

    @Override
    public LootItem createLootItem(Item.Properties props) {
        // Fabric has no item-ability hook; LootItem's own useOn handles strip/scrape/flatten.
        return new LootItem(props);
    }

    @Override
    public LootArmorItem createLootArmorItem(Item.Properties props) {
        // Per-stack equipment slot (NeoForge overrides getEquipmentSlot instead);
        // fabric-item-api-v1 injects equipmentSlot() into Item.Properties.
        props.equipmentSlot((entity, stack) -> {
            net.minecraft.world.entity.EquipmentSlot slot = dev.marston.randomloot.loot.LootUtils.wearableSlot(stack);
            return slot != null ? slot : net.minecraft.world.entity.EquipmentSlot.MAINHAND;
        });
        return new LootArmorItem(props);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return !state.requiresCorrectToolForDrops() || player.hasCorrectToolForDrops(state);
    }

    @Override
    public BlockState getToolModifiedState(UseOnContext ctx, ToolAction action) {
        BlockState state = ctx.getLevel().getBlockState(ctx.getClickedPos());
        Block block = state.getBlock();

        // Vanilla conversion maps (AxeItem/ShovelItem expose them to us via access
        // widener). Fabric API's content registries feed the same maps
        // (StrippableBlockRegistry/FlattenableBlockRegistry/OxidizableBlocksRegistry),
        // so modded blocks registered the standard Fabric way work here too.
        return switch (action) {
            case AXE_STRIP -> {
                // Fabric API's StrippableBlockRegistry feeds this same vanilla map.
                Block stripped = AxeItem.STRIPPABLES.get(block);
                yield stripped == null ? null : stripped.withPropertiesOf(state);
            }
            case AXE_SCRAPE -> WeatheringCopper.getPrevious(state).orElse(null);
            case AXE_WAX_OFF -> {
                Block unwaxed = HoneycombItem.WAX_OFF_BY_BLOCK.get().get(block);
                yield unwaxed == null ? null : unwaxed.withPropertiesOf(state);
            }
            case SHOVEL_FLATTEN -> ShovelItem.FLATTENABLES.get(block);
        };
    }

    @Override
    public Level tooltipLevel(Item.TooltipContext ctx) {
        // Tooltips are only meaningfully built on the client; the indirection keeps
        // client classes out of dedicated-server classloading.
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return FabricClientLevelGetter.get();
        }
        return null;
    }
}
