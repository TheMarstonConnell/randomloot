package dev.marston.randomloot.fabric.mixin;

import dev.marston.randomloot.loot.modifiers.ArmorDispatcher;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Fabric stand-in for NeoForge's ArmorHurtEvent (Unbreaking's chance to skip armor durability). */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @ModifyVariable(method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V",
            at = @At("HEAD"), argsOnly = true)
    private int randomloot$armorUnbreaking(int amount, int amountArg, LivingEntity owner, EquipmentSlot slot) {
        ItemStack self = (ItemStack) (Object) this;
        return (int) ArmorDispatcher.onArmorHurt(owner, self, amount);
    }
}
