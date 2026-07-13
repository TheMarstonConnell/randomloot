package dev.marston.randomloot.neoforge;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;

@EventBusSubscriber(modid = RandomLoot.MODID, value = Dist.CLIENT)
public final class NeoForgeClientEvents {

    private NeoForgeClientEvents() {
    }

    @SubscribeEvent
    public static void registerRangeProperties(RegisterRangeSelectItemModelPropertyEvent event) {
        event.register(
                Identifier.fromNamespaceAndPath(RandomLoot.MODID, "cosmetic"),
                LootUtils.TextureProperty.MAP_CODEC
        );
    }
}
