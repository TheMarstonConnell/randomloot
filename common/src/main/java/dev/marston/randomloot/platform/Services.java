package dev.marston.randomloot.platform;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.platform.services.IPlatformHelper;
import dev.marston.randomloot.platform.services.RegHelper;

import java.util.ServiceLoader;

/**
 * Loader-provided implementations of the platform seam, resolved via
 * {@link ServiceLoader}. Each loader project ships a META-INF/services file
 * binding these interfaces to its implementation.
 */
public class Services {

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);
    public static final RegHelper REG = load(RegHelper.class);

    public static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        RandomLoot.LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
