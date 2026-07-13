package dev.marston.randomloot.fabric;

import dev.marston.randomloot.gametest.GameTestBodies;
import dev.marston.randomloot.items.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;

/**
 * Fabric registration for the shared gametest bodies in {@link GameTestBodies}.
 * Run headless with {@code ./gradlew :fabric:runGametest}.
 *
 * <p>The two enchant-filtering tests are NeoForge-only (they exercise the
 * supportsEnchantment item extension; Fabric wires the same common logic through
 * EnchantmentEvents.ALLOW_ENCHANTING), as is loot_modifiers_load (GLMs are a
 * NeoForge system - Fabric injects loot through LootTableEvents.MODIFY instead).
 */
public class RandomLootFabricGameTests {

    private static final int MAX_TICKS = 200;

    @GameTest(maxTicks = MAX_TICKS)
    public void modifierRoundtrip(GameTestHelper helper) {
        GameTestBodies.modifierRoundTrip(helper);
    }

    @GameTest(maxTicks = MAX_TICKS)
    public void genTool(GameTestHelper helper) {
        GameTestBodies.genTool(helper);
    }

    @GameTest(maxTicks = MAX_TICKS)
    public void genToolDispenser(GameTestHelper helper) {
        GameTestBodies.genToolDispenser(helper);
    }

    @GameTest(maxTicks = MAX_TICKS)
    public void dispenserOpensCase(GameTestHelper helper) {
        GameTestBodies.dispenserOpensCase(helper);
    }

    @GameTest(maxTicks = MAX_TICKS)
    public void caseOpensIntoHand(GameTestHelper helper) {
        GameTestBodies.caseOpensIntoHand(helper);
    }

    @GameTest(maxTicks = MAX_TICKS)
    public void caseRollReveal(GameTestHelper helper) {
        GameTestBodies.caseRollReveal(helper);
    }

    @GameTest(maxTicks = MAX_TICKS)
    public void breakBlock(GameTestHelper helper) {
        GameTestBodies.breakBlock(helper);
    }

    @GameTest(maxTicks = MAX_TICKS)
    public void advancementsLoad(GameTestHelper helper) {
        GameTestBodies.advancementsLoad(helper);
    }

    @GameTest(maxTicks = MAX_TICKS)
    public void xpLevelCurve(GameTestHelper helper) {
        GameTestBodies.xpLevelCurve(helper);
    }

    @GameTest(maxTicks = MAX_TICKS)
    public void killTraitHooks(GameTestHelper helper) {
        GameTestBodies.killTraitHooks(helper);
    }

    @GameTest(maxTicks = MAX_TICKS)
    public void catalystExtendsEffects(GameTestHelper helper) {
        GameTestBodies.catalystExtendsEffects(helper);
    }

    @GameTest(maxTicks = MAX_TICKS)
    public void stenchDebuffsMobs(GameTestHelper helper) {
        GameTestBodies.stenchDebuffsMobs(helper);
    }

    @GameTest(maxTicks = MAX_TICKS)
    public void newTraitRecipesLoad(GameTestHelper helper) {
        GameTestBodies.newTraitRecipesLoad(helper);
    }

    @GameTest(maxTicks = MAX_TICKS)
    public void toolRepairable(GameTestHelper helper) {
        GameTestBodies.toolRepairable(helper);
    }

    @GameTest(maxTicks = MAX_TICKS)
    public void armorComponents(GameTestHelper helper) {
        GameTestBodies.armorComponents(helper);
    }

    @GameTest(maxTicks = MAX_TICKS)
    public void forgerWorldConstant(GameTestHelper helper) {
        GameTestBodies.forgerWorldConstant(helper);
    }

    @GameTest(maxTicks = MAX_TICKS)
    public void dirtPlaceWorldForger(GameTestHelper helper) {
        GameTestBodies.dirtPlaceWorldForger(helper);
    }

    @GameTest(maxTicks = MAX_TICKS)
    public void armorXpOnDamage(GameTestHelper helper) {
        GameTestBodies.armorXpOnDamage(helper);
    }

    @GameTest(maxTicks = MAX_TICKS)
    public void thornyReflectsForPlayer(GameTestHelper helper) {
        GameTestBodies.thornyReflectsForPlayer(helper);
    }

    @GameTest(maxTicks = MAX_TICKS)
    public void armorRepairable(GameTestHelper helper) {
        GameTestBodies.armorRepairable(helper);
    }

    @GameTest(maxTicks = MAX_TICKS)
    public void adminCommands(GameTestHelper helper) {
        GameTestBodies.adminCommands(helper);
    }

    @GameTest(maxTicks = MAX_TICKS)
    public void smithingTraitGating(GameTestHelper helper) {
        GameTestBodies.smithingTraitGating(helper);
    }

    @GameTest(maxTicks = MAX_TICKS)
    public void clonePreservesEnchantments(GameTestHelper helper) {
        GameTestBodies.clonePreservesEnchantments(helper);
    }

    /**
     * Fabric-only: LootTableEvents.MODIFY injected the case/template pools into
     * chest tables (the Fabric replacement for the NeoForge global loot modifier).
     * With the default 25% case chance, 200 rolls without a single case has
     * probability ~1e-25, so this is deterministic in practice.
     */
    @GameTest(maxTicks = MAX_TICKS)
    public void lootInjectionLoads(GameTestHelper helper) {
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE,
                Identifier.withDefaultNamespace("chests/simple_dungeon"));
        LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(key);
        helper.assertTrue(table != LootTable.EMPTY, "simple_dungeon loot table should exist");

        LootParams params = new LootParams.Builder(helper.getLevel())
                .create(LootContextParamSets.EMPTY);

        boolean caseFound = false;
        for (int i = 0; i < 200 && !caseFound; i++) {
            List<ItemStack> loot = table.getRandomItems(params);
            caseFound = loot.stream()
                    .anyMatch(s -> s.is(ModItems.CASE.get()) || s.is(ModItems.MOD_ADD.get()));
        }
        helper.assertTrue(caseFound, "chest loot should contain injected cases/templates");

        helper.succeed();
    }
}
