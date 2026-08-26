package dev.marston.randomloot.neoforge;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.items.ModItems;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/** Mod-bus handlers; 1.21.1 needs the explicit bus (26.x auto-routes by event type). */
@EventBusSubscriber(modid = RandomLoot.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class NeoForgeModEvents {

    private NeoForgeModEvents() {
    }

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent event) {
        Config.onLoad();
    }

    @SubscribeEvent
    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            for (Item item : ModItems.creativeTabItems()) {
                event.accept(item);
            }
        }
    }
}
