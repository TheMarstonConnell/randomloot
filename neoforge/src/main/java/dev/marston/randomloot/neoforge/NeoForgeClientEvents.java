package dev.marston.randomloot.neoforge;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = RandomLoot.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class NeoForgeClientEvents {

    private NeoForgeClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // The randomloot:cosmetic model property drives per-stack texture selection
            // via classic model overrides (26.x uses range_dispatch + a codec'd property).
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(RandomLoot.MODID, "cosmetic");
            ItemProperties.register(ModItems.TOOL.get(), id,
                    (stack, level, entity, seed) -> LootUtils.modelTexture(stack, level, seed));
            ItemProperties.register(ModItems.ARMOR.get(), id,
                    (stack, level, entity, seed) -> LootUtils.modelTexture(stack, level, seed));
        });
    }

    /**
     * Worn Random Armor rendering: 1.21.1's HumanoidArmorLayer only handles ArmorItem,
     * so player renderers get a dedicated layer (Fabric uses ArmorRenderer instead).
     */
    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (net.minecraft.client.resources.PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer renderer) {
                renderer.addLayer(new NeoForgeLootArmorLayer<>(renderer, event.getEntityModels()));
            }
        }
    }
}
