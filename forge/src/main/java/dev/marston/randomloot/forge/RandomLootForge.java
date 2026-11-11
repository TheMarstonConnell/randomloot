package dev.marston.randomloot.forge;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.ModLootModifiers;
import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.gametest.RandomLootGameTests;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(RandomLoot.MODID)
public class RandomLootForge {

    public RandomLootForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Populate the DeferredRegisters (common registration), then attach them to the bus.
        RandomLoot.init();
        ForgeRegHelper.registerBuses(modEventBus);

        ModLootModifiers.register(modEventBus);

        ForgeEvents.bindHooks();

        // In-world GameTests (only run when the gametest system is enabled, e.g. runGameTestServer).
        RandomLootGameTests.init(modEventBus);

        modEventBus.addListener(this::commonSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Dispenser behavior registration is not thread-safe; defer off the parallel mod-loading pool.
        event.enqueueWork(RandomLoot::commonSetup);
    }
}
