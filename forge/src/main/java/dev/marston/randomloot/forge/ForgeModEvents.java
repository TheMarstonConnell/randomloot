package dev.marston.randomloot.forge;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.items.ModItems;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

/** Mod-bus handlers; 1.20.1 needs the explicit bus (26.x auto-routes by event type). */
@Mod.EventBusSubscriber(modid = RandomLoot.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ForgeModEvents {

    private ForgeModEvents() {
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
