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

        // 26.3 renamed ModConfig.Type.COMMON to LOCAL; pass the file name explicitly so the
        // config stays randomloot-common.toml (matching the Fabric/FCAP side) instead of
        // defaulting to randomloot-local.toml.
        modContainer.registerConfig(ModConfig.Type.LOCAL, Config.SPEC, "randomloot-common.toml");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Dispenser behavior registration is not thread-safe; defer off the parallel mod-loading pool.
        event.enqueueWork(RandomLoot::commonSetup);
    }
}
