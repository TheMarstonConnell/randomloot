package dev.marston.randomloot.forge;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.platform.GameHooks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

/**
 * The mod-bus half of the Forge shim for the loader hook contract; 1.20.1 needs the
 * explicit bus (26.x auto-routes by event type, so its handlers sit in NeoForgeEvents).
 */
@Mod.EventBusSubscriber(modid = RandomLoot.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ForgeModEvents {

    private ForgeModEvents() {
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
