package dev.marston.randomloot.fabric;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.gametest.GameTestTable;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.TestFunction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Fabric adapter for the gametest table ({@link GameTestTable}), plus the
 * Fabric-only tests. Run headless with {@code ./gradlew :fabric:runGametest}.
 *
 * <p>Shared tests are not declared here: {@link #shared()} hands the framework every
 * row of the table, under the same {@code randomloot.<name>} test names NeoForge
 * uses. Only a test that makes sense on Fabric alone is written as a
 * {@code @GameTest} method below (this class is the {@code fabric-gametest}
 * entrypoint). One of them, {@link #gametestTableRegistered}, checks that the rows
 * really were registered.
 *
 * <p>The two enchant-filtering tests are NeoForge-only (they exercise the
 * supportsEnchantment item extension; Fabric wires the same common logic through
 * EnchantmentEvents.ALLOW_ENCHANTING), as is loot_modifiers_load (GLMs are a
 * NeoForge system - Fabric injects loot through LootTableEvents.MODIFY instead).
 */
public class RandomLootFabricGameTests implements FabricGameTest {

    private static final int MAX_TICKS = 200;

    /** Shared tests: whatever the common table lists, so both loaders run the same set. */
    @GameTestGenerator
    public static Collection<TestFunction> shared() {
        return GameTestTable.testFunctions();
    }

    /**
     * Fabric-only: every row of the gametest table reached the test registry.
     *
     * <p>The rows go in through a generator method, not one annotated method each. If
     * that route stopped working, the shared tests would vanish and the run would
     * still pass. This test is registered by the annotation route, so it is still
     * there to fail.
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void gametestTableRegistered(GameTestHelper helper) {
        Set<String> registered = GameTestRegistry.getAllTestFunctions().stream()
                .map(TestFunction::testName).collect(Collectors.toSet());
        for (GameTestTable.Row row : GameTestTable.rows()) {
            helper.assertTrue(registered.contains(row.id()),
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
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
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
