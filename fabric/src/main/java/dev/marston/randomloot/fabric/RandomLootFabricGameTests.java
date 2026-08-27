package dev.marston.randomloot.fabric;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.gametest.GameTestBodies;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;

import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.gametest.framework.GameTestHelper;


/**
 * Fabric registration for the shared gametest bodies in {@link GameTestBodies}.
 * Run headless with {@code ./gradlew :fabric:runGametest}.
 *
 * <p>The two enchant-filtering tests are NeoForge-only (they exercise the
 * supportsEnchantment item extension; Fabric wires the same common logic through
 * EnchantmentEvents.ALLOW_ENCHANTING), as is loot_modifiers_load (GLMs are a
 * NeoForge system - Fabric injects loot through LootTableEvents.MODIFY instead).
 */
public class RandomLootFabricGameTests implements FabricGameTest {

    private static final int MAX_TICKS = 200;

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void modifierRoundtrip(GameTestHelper helper) {
        GameTestBodies.modifierRoundTrip(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void genTool(GameTestHelper helper) {
        GameTestBodies.genTool(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void genToolDispenser(GameTestHelper helper) {
        GameTestBodies.genToolDispenser(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void dispenserOpensCase(GameTestHelper helper) {
        GameTestBodies.dispenserOpensCase(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void caseOpensIntoHand(GameTestHelper helper) {
        GameTestBodies.caseOpensIntoHand(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void caseRollReveal(GameTestHelper helper) {
        GameTestBodies.caseRollReveal(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void deterministicRollLifecycles(GameTestHelper helper) {
        GameTestBodies.deterministicRollLifecycles(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void breakBlock(GameTestHelper helper) {
        GameTestBodies.breakBlock(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void advancementsLoad(GameTestHelper helper) {
        GameTestBodies.advancementsLoad(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void xpLevelCurve(GameTestHelper helper) {
        GameTestBodies.xpLevelCurve(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void killTraitHooks(GameTestHelper helper) {
        GameTestBodies.killTraitHooks(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void catalystExtendsEffects(GameTestHelper helper) {
        GameTestBodies.catalystExtendsEffects(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void stenchDebuffsMobs(GameTestHelper helper) {
        GameTestBodies.stenchDebuffsMobs(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void newTraitRecipesLoad(GameTestHelper helper) {
        GameTestBodies.newTraitRecipesLoad(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void toolRepairable(GameTestHelper helper) {
        GameTestBodies.toolRepairable(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void armorComponents(GameTestHelper helper) {
        GameTestBodies.armorComponents(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void forgerWorldConstant(GameTestHelper helper) {
        GameTestBodies.forgerWorldConstant(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void dirtPlaceWorldForger(GameTestHelper helper) {
        GameTestBodies.dirtPlaceWorldForger(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void armorXpOnDamage(GameTestHelper helper) {
        GameTestBodies.armorXpOnDamage(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void thornyReflectsForPlayer(GameTestHelper helper) {
        GameTestBodies.thornyReflectsForPlayer(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void armorRepairable(GameTestHelper helper) {
        GameTestBodies.armorRepairable(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void adminCommands(GameTestHelper helper) {
        GameTestBodies.adminCommands(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void smithingTraitGating(GameTestHelper helper) {
        GameTestBodies.smithingTraitGating(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void smithingCraftSequence(GameTestHelper helper) {
        GameTestBodies.smithingCraftSequence(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void clonePreservesEnchantments(GameTestHelper helper) {
        GameTestBodies.clonePreservesEnchantments(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void loaderHooksAllBound(GameTestHelper helper) {
        GameTestBodies.loaderHooksAllBound(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void lootInjectionAddsCases(GameTestHelper helper) {
        GameTestBodies.lootInjectionAddsCases(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void lootInjectionSkipsBlockDrops(GameTestHelper helper) {
        GameTestBodies.lootInjectionSkipsBlockDrops(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void migrationRestoresDerivedComponents(GameTestHelper helper) {
        GameTestBodies.migrationRestoresDerivedComponents(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void featherweightSoftensFallDamage(GameTestHelper helper) {
        GameTestBodies.featherweightSoftensFallDamage(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void adrenalineGrantsSpeed(GameTestHelper helper) {
        GameTestBodies.adrenalineGrantsSpeed(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void bulwarkBlocksSomeHits(GameTestHelper helper) {
        GameTestBodies.bulwarkBlocksSomeHits(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void unbreakingSkipsArmorDurability(GameTestHelper helper) {
        GameTestBodies.unbreakingSkipsArmorDurability(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void soulboundOwnerMinesFaster(GameTestHelper helper) {
        GameTestBodies.soulboundOwnerMinesFaster(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void enchantingTableFiltersByType(GameTestHelper helper) {
        GameTestBodies.enchantingTableFiltersByType(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void axeToolActions(GameTestHelper helper) {
        GameTestBodies.axeToolActions(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void shovelFlattens(GameTestHelper helper) {
        GameTestBodies.shovelFlattens(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void anvilCannotCombineLootGear(GameTestHelper helper) {
        GameTestBodies.anvilCannotCombineLootGear(helper);
    }

    /**
     * Fabric-only: Forge Config API Port generated the same randomloot-common.toml
     * NeoForge writes, and its values reached the Config fields.
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MAX_TICKS)
    public void configLoads(GameTestHelper helper) {
        Path config = FabricLoader.getInstance().getConfigDir()
                .resolve("randomloot-common.toml");
        helper.assertTrue(Files.exists(config),
                "FCAP should generate randomloot-common.toml, looked at " + config);

        helper.assertTrue(Config.CaseChance > 0.0 && Config.CaseChance <= 1.0,
                "caseChance should be loaded and sane, got " + Config.CaseChance);
        helper.assertTrue(Config.traitEnabled("thorny"),
                "traits should default to enabled");

        helper.succeed();
    }
}
