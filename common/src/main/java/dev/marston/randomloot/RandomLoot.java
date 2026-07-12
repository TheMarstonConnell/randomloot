package dev.marston.randomloot;

import com.mojang.logging.LogUtils;
import dev.marston.randomloot.advancements.ModCriteria;
import dev.marston.randomloot.component.ModDataComponents;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.LootCase;
import dev.marston.randomloot.recipes.Recipies;
import org.slf4j.Logger;

/**
 * Loader-neutral mod core: id, logger, and the shared bootstrap. Each loader's
 * entrypoint (RandomLootNeoForge / RandomLootFabric) calls {@link #init()}
 * during mod construction, then wires loader events to the common dispatchers.
 */
public class RandomLoot {

    public static final String MODID = "randomloot";
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Runs all common registration (through {@link dev.marston.randomloot.platform.services.RegHelper})
     * by classloading the registration classes, in dependency order.
     */
    public static void init() {
        ModDataComponents.init();
        ModCriteria.init();
        ModItems.init();
        Recipies.init();

        GenWiki.genWiki();
    }

    /**
     * Registration that must run after registries are frozen but before the
     * game runs; loaders call this from their common-setup phase (thread-safe
     * context).
     */
    public static void commonSetup() {
        LootCase.initDispenser();
    }
}
