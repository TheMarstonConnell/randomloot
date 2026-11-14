package dev.marston.randomloot.fabric.mixin;

import dev.marston.randomloot.platform.GameHooks;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Fabric's shim for ARMOR_HURT, where NeoForge has ArmorHurtEvent. It sees every
 * hurtAndBreak that has an owner and a slot, which is more than the hook asks for;
 * common keeps the armor slots.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @ModifyVariable(method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V",
            at = @At("HEAD"), argsOnly = true)
    private int randomloot$armorUnbreaking(int amount, int amountArg, LivingEntity owner, EquipmentSlot slot) {
        ItemStack self = (ItemStack) (Object) this;
        return (int) GameHooks.armorHurt(owner, self, slot, amount);
    }
}
