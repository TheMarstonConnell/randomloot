package dev.marston.randomloot.loot.modifiers;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface HoldModifier extends Modifier {
	public void hold(ItemStack stack, Level level, Entity holder);
}
