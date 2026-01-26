package dev.marston.randomloot;

import dev.marston.randomloot.loot.LootTableHandler;
import dev.marston.randomloot.recipes.AnvilRecipeHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = RandomLoot.MODID, name = RandomLoot.NAME, version = RandomLoot.VERSION)
public class RandomLoot {
    public static final String MODID = "randomloot";
    public static final String NAME = "Random Loot 2";
    public static final String VERSION = "1.0.0";

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @SidedProxy(clientSide = "dev.marston.randomloot.ClientProxy", serverSide = "dev.marston.randomloot.CommonProxy")
    public static CommonProxy proxy;

    @Mod.Instance(MODID)
    public static RandomLoot instance;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Random Loot 2 Pre-Initialization");
        Config.init(event.getSuggestedConfigurationFile());
        proxy.preInit(event);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("Random Loot 2 Initialization");
        proxy.init(event);
        
        // Register event handlers
        MinecraftForge.EVENT_BUS.register(new AnvilRecipeHandler());
        MinecraftForge.EVENT_BUS.register(new LootTableHandler());
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        LOGGER.info("Random Loot 2 Post-Initialization");
        proxy.postInit(event);
    }
}
