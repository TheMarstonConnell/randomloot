package dev.marston.randomloot.loot;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.items.ModItems;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.conditions.RandomChance;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Handles injection of loot cases into vanilla loot tables.
 */
public class LootTableHandler {

    @SubscribeEvent
    public void onLootTableLoad(LootTableLoadEvent event) {
        String name = event.getName().toString();

        // Add to dungeon, temple, and other chest loot tables
        if (shouldInjectLoot(name)) {
            LootPool casePool = createCasePool();
            event.getTable().addPool(casePool);
        }
    }

    private boolean shouldInjectLoot(String tableName) {
        // Inject into most chest-based loot tables
        return tableName.contains("chests/") ||
               tableName.contains("chest") ||
               tableName.equals("minecraft:gameplay/fishing/treasure");
    }

    private LootPool createCasePool() {
        // Create condition for random chance based on config
        LootCondition[] conditions = new LootCondition[] {
                new RandomChance((float) Config.CaseChance)
        };

        // Create entry for the case item
        LootEntry caseEntry = new LootEntryItem(
                ModItems.CASE,
                1,                      // weight
                0,                      // quality
                new LootFunction[0],    // functions
                new LootCondition[0],   // conditions
                RandomLoot.MODID + ":case_entry"
        );

        return new LootPool(
                new LootEntry[] { caseEntry },
                conditions,
                new RandomValueRange(0, 1),    // rolls
                new RandomValueRange(0, 0),    // bonus rolls
                RandomLoot.MODID + "_case_pool"
        );
    }
}
