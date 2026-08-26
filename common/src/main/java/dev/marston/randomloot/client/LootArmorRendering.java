package dev.marston.randomloot.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Worn-armor rendering for Random Armor on 1.21.1. 26.x renders worn looks through
 * the vanilla EQUIPPABLE component's equipment assets; 1.21.1's HumanoidArmorLayer
 * only renders ArmorItem, so each loader wires this helper in itself (NeoForge via a
 * custom render layer, Fabric via fabric-rendering-v1's ArmorRenderer). The texture
 * files are the same equipment textures the 26.x branch ships.
 */
public final class LootArmorRendering {

	private LootArmorRendering() {
	}

	/** The worn texture for the piece: the 26.x equipment asset pngs, addressed directly. */
	public static ResourceLocation texture(ItemStack stack, EquipmentSlot slot) {
		int set = LootUtils.wornSetIndex(stack);
		String folder = slot == EquipmentSlot.LEGS ? "humanoid_leggings" : "humanoid";
		return ResourceLocation.fromNamespaceAndPath(RandomLoot.MODID,
				"textures/entity/equipment/" + folder + "/set" + set + ".png");
	}

	/** Mirrors HumanoidArmorLayer.setPartVisibility. */
	public static void setPartVisibility(HumanoidModel<?> model, EquipmentSlot slot) {
		model.setAllVisible(false);
		switch (slot) {
		case HEAD -> {
			model.head.visible = true;
			model.hat.visible = true;
		}
		case CHEST -> {
			model.body.visible = true;
			model.rightArm.visible = true;
			model.leftArm.visible = true;
		}
		case LEGS -> {
			model.body.visible = true;
			model.rightLeg.visible = true;
			model.leftLeg.visible = true;
		}
		case FEET -> {
			model.rightLeg.visible = true;
			model.leftLeg.visible = true;
		}
		default -> {
		}
		}
	}

	/** Whether the piece uses the inner (thin) armor model, like vanilla leggings. */
	public static boolean usesInnerModel(EquipmentSlot slot) {
		return slot == EquipmentSlot.LEGS;
	}

	/**
	 * Renders one worn piece: copies the wearer's pose onto the armor model, masks it
	 * to the piece's parts and draws it with the stack's texture set (+ foil glint).
	 */
	public static <T extends LivingEntity> void renderPiece(PoseStack pose, MultiBufferSource buffers, int light,
			ItemStack stack, EquipmentSlot slot, HumanoidModel<T> parent, HumanoidModel<T> armorModel) {
		parent.copyPropertiesTo(armorModel);
		setPartVisibility(armorModel, slot);

		VertexConsumer vc = ItemRenderer.getArmorFoilBuffer(buffers,
				RenderType.armorCutoutNoCull(texture(stack, slot)), stack.hasFoil());
		armorModel.renderToBuffer(pose, vc, light, OverlayTexture.NO_OVERLAY, -1);
	}
}
