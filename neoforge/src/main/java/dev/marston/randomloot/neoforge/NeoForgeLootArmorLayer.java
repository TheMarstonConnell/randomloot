package dev.marston.randomloot.neoforge;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.marston.randomloot.client.LootArmorRendering;
import dev.marston.randomloot.loot.LootArmorItem;
import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Renders worn Random Armor (see {@link LootArmorRendering}). Vanilla's
 * HumanoidArmorLayer skips it because LootArmorItem is not an ArmorItem.
 */
public class NeoForgeLootArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {

    private final HumanoidModel<T> innerModel;
    private final HumanoidModel<T> outerModel;

    public NeoForgeLootArmorLayer(RenderLayerParent<T, M> parent, EntityModelSet models) {
        super(parent);
        this.innerModel = new HumanoidModel<>(models.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
        this.outerModel = new HumanoidModel<>(models.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing,
            float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                EquipmentSlot.FEET }) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!(stack.getItem() instanceof LootArmorItem)) {
                continue;
            }
            if (LootUtils.wearableSlot(stack) != slot) {
                continue;
            }
            HumanoidModel<T> model = LootArmorRendering.usesInnerModel(slot) ? innerModel : outerModel;
            LootArmorRendering.renderPiece(pose, buffers, light, stack, slot, getParentModel(), model);
        }
    }
}
