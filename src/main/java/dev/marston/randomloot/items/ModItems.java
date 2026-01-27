package dev.marston.randomloot.items;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.component.ModDataComponents;
import dev.marston.randomloot.component.ToolModifier;
import dev.marston.randomloot.loot.LootCase;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.ModTemplate;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RandomLoot.MODID);


    public static DeferredItem<Item> TOOL = ITEMS.registerItem("tool", p -> new LootItem(p.component(ModDataComponents.TOOL_MODIFIER.get(), new ToolModifier(new HashMap<>())).enchantable(15)));
    public static DeferredItem<Item> CASE = ITEMS.registerItem("case", LootCase::new);
    public static DeferredItem<Item> MOD_ADD = ITEMS.registerItem("mod_add", p -> new ModTemplate(p, true));
    public static DeferredItem<Item> MOD_SUB = ITEMS.registerItem("mod_sub", p -> new ModTemplate(p, false));

    // === LEGACY ITEMS (for 1.12 migration) ===
    // These items exist to catch old items when players upgrade from 1.12.
    // When held in hand, they are converted to cases via LegacyMigrationHandler.

    // Legacy tools
    public static DeferredItem<Item> LEGACY_SWORD = ITEMS.registerItem("sword",
        p -> new LegacyItem(p, "sword"));
    public static DeferredItem<Item> LEGACY_PICKAXE = ITEMS.registerItem("pickaxe",
        p -> new LegacyItem(p, "pickaxe"));
    public static DeferredItem<Item> LEGACY_AXE = ITEMS.registerItem("axe",
        p -> new LegacyItem(p, "axe"));
    public static DeferredItem<Item> LEGACY_SHOVEL = ITEMS.registerItem("shovel",
        p -> new LegacyItem(p, "shovel"));
    public static DeferredItem<Item> LEGACY_BOW = ITEMS.registerItem("rl_bow",
        p -> new LegacyItem(p, "bow"));

    // Legacy cases
    public static DeferredItem<Item> LEGACY_BASIC_CASE = ITEMS.registerItem("basic_case",
        p -> new LegacyItem(p, "basic_case"));
    public static DeferredItem<Item> LEGACY_GOLDEN_CASE = ITEMS.registerItem("golden_case",
        p -> new LegacyItem(p, "golden_case"));
    public static DeferredItem<Item> LEGACY_TITAN_CASE = ITEMS.registerItem("titan_case",
        p -> new LegacyItem(p, "titan_case"));

    // Legacy armor - Heavy set
    public static DeferredItem<Item> LEGACY_HEAVY_BOOTS = ITEMS.registerItem("heavy_boots",
        p -> new LegacyItem(p, "heavy_boots"));
    public static DeferredItem<Item> LEGACY_HEAVY_LEGS = ITEMS.registerItem("heavy_legs",
        p -> new LegacyItem(p, "heavy_legs"));
    public static DeferredItem<Item> LEGACY_HEAVY_CHEST = ITEMS.registerItem("heavy_chest",
        p -> new LegacyItem(p, "heavy_chest"));
    public static DeferredItem<Item> LEGACY_HEAVY_HELMET = ITEMS.registerItem("heavy_helmet",
        p -> new LegacyItem(p, "heavy_helmet"));

    // Legacy armor - Titanium set
    public static DeferredItem<Item> LEGACY_TITANIUM_BOOTS = ITEMS.registerItem("titanium_boots",
        p -> new LegacyItem(p, "titanium_boots"));
    public static DeferredItem<Item> LEGACY_TITANIUM_LEGS = ITEMS.registerItem("titanium_legs",
        p -> new LegacyItem(p, "titanium_legs"));
    public static DeferredItem<Item> LEGACY_TITANIUM_CHEST = ITEMS.registerItem("titanium_chest",
        p -> new LegacyItem(p, "titanium_chest"));
    public static DeferredItem<Item> LEGACY_TITANIUM_HELMET = ITEMS.registerItem("titanium_helmet",
        p -> new LegacyItem(p, "titanium_helmet"));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(TOOL);
            event.accept(CASE);
        }
    }
}


