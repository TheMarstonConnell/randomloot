package dev.marston.randomloot.neoforge;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.platform.GameHooks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * The mod-bus half of the NeoForge shim for the loader hook contract; 1.21.1 needs the
 * explicit bus (26.x auto-routes by event type, so its handlers sit in NeoForgeEvents).
 */
@EventBusSubscriber(modid = RandomLoot.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class NeoForgeModEvents {

    private NeoForgeModEvents() {
    }

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent event) {
        GameHooks.config();
    }

    @SubscribeEvent
    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        GameHooks.creativeTab(event.getTabKey(), event::accept);
    }
}
