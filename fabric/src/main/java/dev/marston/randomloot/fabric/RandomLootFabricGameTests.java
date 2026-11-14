package dev.marston.randomloot.fabric;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.gametest.GameTestTable;
import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;
import net.fabricmc.fabric.api.event.registry.DynamicRegistryView;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.gametest.v1.GameTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;


/**
 * Fabric adapter for the gametest table ({@link GameTestTable}), plus the
 * Fabric-only tests. Run headless with {@code ./gradlew :fabric:runGametest}.
 *
 * <p>Shared tests are not declared here: {@link #registerShared()} registers every
 * row of the table at runtime, under the same {@code randomloot:<name>} ids NeoForge
 * uses. Only a test that makes sense on Fabric alone is written as a
 * {@code @GameTest} method below (this class is the {@code fabric-gametest}
 * entrypoint); such a test keeps Fabric's generated id and default structure. One of
 * them, {@link #gametestTableRegistered}, checks that the rows really were registered.
 *
 * <p>The two enchant-filtering tests are NeoForge-only (they exercise the
 * supportsEnchantment item extension; Fabric wires the same common logic through
 * EnchantmentEvents.ALLOW_ENCHANTING), as is loot_modifiers_load (GLMs are a
 * NeoForge system - Fabric injects loot through LootTableEvents.MODIFY_DROPS instead).
 */
public class RandomLootFabricGameTests {

    private static final int MAX_TICKS = 200;

    /**
     * Registers every row of the gametest table. Called from mod init.
     *
     * <p>Fabric API's {@code @GameTest} support does two things for each annotated
     * method, and this does the same two for each row, so a row ends up as an
     * ordinary vanilla function test:
     * <ol>
     * <li>The body goes into the built-in {@code TEST_FUNCTION} registry during mod
     * init, while that registry is still open.</li>
     * <li>A {@link FunctionGameTestInstance} pointing at it goes into the dynamic
     * {@code TEST_INSTANCE} registry. Fabric API adds its own just before that
     * registry freezes, from a mixin with no public hook, so the rows go in earlier
     * through {@link DynamicRegistrySetupCallback}: it fires on the server side only,
     * once the empty registries exist and before any datapack entry loads.</li>
     * </ol>
     *
     * <p>Gated the way Fabric API gates its own registration (a gametest run, or a
     * development environment so {@code /test} works in a dev world), so nothing is
     * registered in a normal game.
     */
    public static void registerShared() {
        if (System.getProperty("fabric-api.gametest") == null
                && !FabricLoader.getInstance().isDevelopmentEnvironment()) {
            return;
        }

        for (GameTestTable.Row row : GameTestTable.rows()) {
            Registry.register(BuiltInRegistries.TEST_FUNCTION, row.id(), row.body());
        }

        DynamicRegistrySetupCallback.EVENT.register(RandomLootFabricGameTests::registerInstances);
    }

    private static void registerInstances(DynamicRegistryView registries) {
        // Fired once per group of dynamic registries; only one group holds the test registries.
        Optional<Registry<GameTestInstance>> instances = registries.getOptional(Registries.TEST_INSTANCE);
        Optional<Registry<TestEnvironmentDefinition<?>>> environments =
                registries.getOptional(Registries.TEST_ENVIRONMENT);
        if (instances.isEmpty() || environments.isEmpty()) {
            return;
        }

        // An empty environment, under the id NeoForge registers through RegisterGameTestsEvent.
        Holder<TestEnvironmentDefinition<?>> environment = Registry.registerForHolder(environments.get(),
                ResourceKey.create(Registries.TEST_ENVIRONMENT, GameTestTable.ENVIRONMENT),
                new TestEnvironmentDefinition.AllOf(List.of()));

        for (GameTestTable.Row row : GameTestTable.rows()) {
            Registry.register(instances.get(), row.id(), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, row.id()), row.data(environment)));
        }
    }

    /**
     * Fabric-only: every row of the gametest table reached the test registry.
     *
     * <p>The rows go in through a registry-sync event, not Fabric API's own
     * {@code @GameTest} route. If a Fabric API update stopped that event firing, the
     * shared tests would vanish and the run would still pass. This test is registered
     * by the annotation route, so it is still there to fail.
     */
    @GameTest(maxTicks = MAX_TICKS)
    public void gametestTableRegistered(GameTestHelper helper) {
        Registry<GameTestInstance> instances =
                helper.getLevel().registryAccess().lookupOrThrow(Registries.TEST_INSTANCE);
        for (GameTestTable.Row row : GameTestTable.rows()) {
            helper.assertTrue(instances.containsKey(row.id()),
                    "gametest table row was not registered on Fabric: " + row.id());
        }

        helper.succeed();
    }

    /**
     * Fabric-only: the values in randomloot-common.toml are the values the mod runs with.
     *
     * <p>The gametest server's config file is seeded with one value off its default (the
     * seedGametestConfig task in fabric/build.gradle), because a file of defaults reads
     * the same whether or not anything was loaded: every setting starts out holding
     * its default. That is how this test used to pass while Fabric ignored the file at
     * startup - Forge Config API Port loads it inside ConfigRegistry.register, and the
     * mod was adding its loading listener after that call.
     */
    @GameTest(maxTicks = MAX_TICKS)
    public void configLoads(GameTestHelper helper) throws IOException {
        Path config = FabricLoader.getInstance().getConfigDir()
                .resolve("randomloot-common.toml");
        helper.assertTrue(Files.exists(config),
                "FCAP should generate randomloot-common.toml, looked at " + config);

        Matcher inFile = Pattern.compile("(?m)^\\s*livingWhispersPerMinute\\s*=\\s*(\\d+)")
                .matcher(Files.readString(config));
        helper.assertTrue(inFile.find(), "livingWhispersPerMinute should be in " + config);
        int whispers = Integer.parseInt(inFile.group(1));
        helper.assertTrue(whispers != Config.LIVING_WHISPERS_PER_MINUTE.defaultValue(),
                "the gametest config should set livingWhispersPerMinute off its default of "
                        + Config.LIVING_WHISPERS_PER_MINUTE.defaultValue() + " (seedGametestConfig in fabric/build.gradle);"
                        + " run this through ./gradlew :fabric:runGametest");
        int running = Config.LIVING_WHISPERS_PER_MINUTE.get();
        helper.assertTrue(running == whispers,
                "the mod should run with the file's livingWhispersPerMinute of " + whispers + ", runs with "
                        + running);

        double caseChance = Config.CASE_CHANCE.get();
        helper.assertTrue(caseChance > 0.0 && caseChance <= 1.0,
                "caseChance should be loaded and sane, got " + caseChance);
        helper.assertTrue(Config.traitEnabled("thorny"),
                "traits should default to enabled");

        helper.succeed();
    }
}
