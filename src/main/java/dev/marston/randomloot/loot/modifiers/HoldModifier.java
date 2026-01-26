package dev.marston.randomloot.loot.modifiers;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public interface HoldModifier extends Modifier {
    /**
     * Called every tick when this tool is held in the player's hand.
     * 
     * @param stack The tool being held
     * @param world The world
     * @param holder The entity holding the tool
     */
    public void hold(ItemStack stack, World world, Entity holder);
}
