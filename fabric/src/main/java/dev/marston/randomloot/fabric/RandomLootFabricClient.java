package dev.marston.randomloot.fabric;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.client.LootArmorRendering;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.LootUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

public class RandomLootFabricClient implements ClientModInitializer {

    private HumanoidModel<LivingEntity> innerModel;
    private HumanoidModel<LivingEntity> outerModel;

    @Override
    public void onInitializeClient() {
        // The randomloot:cosmetic model property drives per-stack texture selection
        // via classic model overrides (26.x uses range_dispatch + a codec'd property).
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(RandomLoot.MODID, "cosmetic");
        ItemProperties.register(ModItems.TOOL.get(), id,
                (stack, level, entity, seed) -> LootUtils.modelTexture(stack, level, seed));
        ItemProperties.register(ModItems.ARMOR.get(), id,
                (stack, level, entity, seed) -> LootUtils.modelTexture(stack, level, seed));

        // Worn Random Armor rendering (NeoForge uses a custom render layer instead).
        ArmorRenderer.register((matrices, buffers, stack, entity, slot, light, contextModel) -> {
            if (LootUtils.wearableSlot(stack) != slot) {
                return;
            }
            bakeModels();
            HumanoidModel<LivingEntity> model = LootArmorRendering.usesInnerModel(slot) ? innerModel : outerModel;
            LootArmorRendering.renderPiece(matrices, buffers, light, stack, slot, contextModel, model);
        }, ModItems.ARMOR.get());
    }

    /** Baked lazily: entity models aren't ready during client mod init. */
    private void bakeModels() {
        if (innerModel == null) {
            var models = Minecraft.getInstance().getEntityModels();
            innerModel = new HumanoidModel<>(models.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
            outerModel = new HumanoidModel<>(models.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
        }
    }
}
