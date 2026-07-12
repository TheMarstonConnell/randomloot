package dev.marston.randomloot.fabric;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.loot.LootUtils;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.resources.Identifier;

public class RandomLootFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // The randomloot:cosmetic range property drives per-stack texture selection
        // in items/tool.json + items/armor.json. ID_MAPPER is opened by our access
        // widener; NeoForge wraps the same map in an event.
        RangeSelectItemModelProperties.ID_MAPPER.put(
                Identifier.fromNamespaceAndPath(RandomLoot.MODID, "cosmetic"),
                LootUtils.TextureProperty.MAP_CODEC);
    }
}
