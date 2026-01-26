package dev.marston.randomloot;

import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.LootNBT;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = RandomLoot.MODID, value = Side.CLIENT)
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        registerItemPropertyOverrides();
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        registerModel(ModItems.TOOL, 0, "tool");
        registerModel(ModItems.CASE, 0, "case");
    }

    private static void registerModel(Item item, int meta, String name) {
        ModelLoader.setCustomModelResourceLocation(item, meta,
                new ModelResourceLocation(new ResourceLocation(RandomLoot.MODID, name), "inventory"));
    }

    private void registerItemPropertyOverrides() {
        // Register custom property for dynamic texture selection
        // Encoding matches tool.json overrides:
        // 0.1xxx = pickaxe, 0.2xxx = shovel, 0.3xxx = axe, 0.4xxx = sword
        ModItems.TOOL.addPropertyOverride(
                new ResourceLocation(RandomLoot.MODID, "cosmetic"),
                (stack, world, entity) -> {
                    if (stack.isEmpty() || !stack.hasTagCompound()) {
                        return 0.0f;
                    }
                    int toolType = LootNBT.getToolTypeOrdinal(stack);
                    int texture = LootNBT.getTexture(stack);
                    
                    // Map tool type ordinal to JSON encoding
                    // PICKAXE=0->0.1, SHOVEL=1->0.2, AXE=2->0.3, SWORD=3->0.4
                    float base;
                    switch (toolType) {
                        case 0: base = 0.1f; break;  // PICKAXE
                        case 1: base = 0.2f; break;  // SHOVEL
                        case 2: base = 0.3f; break;  // AXE
                        case 3: base = 0.4f; break;  // SWORD
                        default: base = 0.0f; break;
                    }
                    
                    return base + texture * 0.0001f;
                }
        );
    }
}
