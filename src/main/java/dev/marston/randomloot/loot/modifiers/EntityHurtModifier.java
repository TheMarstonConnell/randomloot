package dev.marston.randomloot.loot.modifiers;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

public interface EntityHurtModifier extends Modifier {
    /**
     * Called when an entity is hit with this tool.
     * 
     * @param itemstack The tool being used
     * @param hurtee The entity being hit
     * @param hurter The entity doing the hitting
     * @return true to skip durability damage, false to continue normally
     */
    boolean hurtEnemy(ItemStack itemstack, EntityLivingBase hurtee, EntityLivingBase hurter);
}
