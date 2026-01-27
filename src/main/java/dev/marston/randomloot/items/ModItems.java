package dev.marston.randomloot.items;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.loot.LootCase;
import dev.marston.randomloot.loot.LootItem;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod.EventBusSubscriber(modid = RandomLoot.MODID)
public class ModItems {
    
    @GameRegistry.ObjectHolder(RandomLoot.MODID + ":tool")
    public static LootItem TOOL;
    
    @GameRegistry.ObjectHolder(RandomLoot.MODID + ":case")
    public static LootCase CASE;

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        LootItem tool = new LootItem();
        tool.setRegistryName(RandomLoot.MODID, "tool");
        tool.setTranslationKey(RandomLoot.MODID + ".tool");
        tool.setCreativeTab(CreativeTabs.TOOLS);
        
        LootCase lootCase = new LootCase();
        lootCase.setRegistryName(RandomLoot.MODID, "case");
        lootCase.setTranslationKey(RandomLoot.MODID + ".case");
        lootCase.setCreativeTab(CreativeTabs.TOOLS);
        
        event.getRegistry().registerAll(tool, lootCase);
        
        // === LEGACY ITEMS (for old 1.12 migration) ===
        // These items exist to catch old items when players upgrade.
        // When held in hand, they are converted to cases via LegacyMigrationHandler.
        
        // Legacy tools
        event.getRegistry().register(createLegacyItem("sword"));
        event.getRegistry().register(createLegacyItem("pickaxe"));
        event.getRegistry().register(createLegacyItem("axe"));
        event.getRegistry().register(createLegacyItem("shovel"));
        event.getRegistry().register(createLegacyItem("rl_bow"));
        
        // Legacy cases
        event.getRegistry().register(createLegacyItem("basic_case"));
        event.getRegistry().register(createLegacyItem("golden_case"));
        event.getRegistry().register(createLegacyItem("titan_case"));
        
        // Legacy armor - Heavy set
        event.getRegistry().register(createLegacyItem("heavy_boots"));
        event.getRegistry().register(createLegacyItem("heavy_legs"));
        event.getRegistry().register(createLegacyItem("heavy_chest"));
        event.getRegistry().register(createLegacyItem("heavy_helmet"));
        
        // Legacy armor - Titanium set
        event.getRegistry().register(createLegacyItem("titanium_boots"));
        event.getRegistry().register(createLegacyItem("titanium_legs"));
        event.getRegistry().register(createLegacyItem("titanium_chest"));
        event.getRegistry().register(createLegacyItem("titanium_helmet"));
    }
    
    private static LegacyItem createLegacyItem(String name) {
        LegacyItem item = new LegacyItem(name);
        item.setRegistryName(RandomLoot.MODID, name);
        return item;
    }
}
