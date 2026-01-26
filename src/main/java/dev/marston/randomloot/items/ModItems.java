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
    }
}
