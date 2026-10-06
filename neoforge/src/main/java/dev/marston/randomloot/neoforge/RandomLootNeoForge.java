package dev.marston.randomloot.neoforge;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.ModLootModifiers;
import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.gametest.RandomLootGameTests;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(RandomLoot.MODID)
public class RandomLootNeoForge {

    public RandomLootNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        // Populate the DeferredRegisters (common registration), then attach them to the bus.
        RandomLoot.init();
        NeoForgeRegHelper.registerBuses(modEventBus);

        ModLootModifiers.register(modEventBus);

        NeoForgeEvents.bindHooks();

        // In-world GameTests (only run when the gametest system is enabled, e.g. runGameTestServer).
        RandomLootGameTests.init(modEventBus);

        modEventBus.addListener(this::commonSetup);

        // Type.COMMON was renamed to LOCAL in MC 26.3; pin the file name so the config stays
        // randomloot-common.toml (matching Fabric/FCAP, which keeps the COMMON name).
        modContainer.registerConfig(ModConfig.Type.LOCAL, Config.SPEC, "randomloot-common.toml");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Dispenser behavior registration is not thread-safe; defer off the parallel mod-loading pool.
        event.enqueueWork(RandomLoot::commonSetup);
    }
}
