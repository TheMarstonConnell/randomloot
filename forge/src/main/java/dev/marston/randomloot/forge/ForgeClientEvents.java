package dev.marston.randomloot.forge;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;

@Mod.EventBusSubscriber(modid = RandomLoot.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ForgeClientEvents {

    private ForgeClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // The randomloot:cosmetic model property drives per-stack texture selection
            // via classic model overrides (26.x uses range_dispatch + a codec'd property).
            ResourceLocation id = new ResourceLocation(RandomLoot.MODID, "cosmetic");
            ItemProperties.register(ModItems.TOOL.get(), id,
                    (stack, level, entity, seed) -> LootUtils.modelTexture(stack, level, seed));
            ItemProperties.register(ModItems.ARMOR.get(), id,
                    (stack, level, entity, seed) -> LootUtils.modelTexture(stack, level, seed));
        });
    }

    /**
     * Worn Random Armor rendering: 1.20.1's HumanoidArmorLayer only handles ArmorItem,
     * so player renderers get a dedicated layer (Fabric uses ArmorRenderer instead).
     */
    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer renderer) {
                renderer.addLayer(new ForgeLootArmorLayer<>(renderer, event.getEntityModels()));
            }
        }
    }
}
