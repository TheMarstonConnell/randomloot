package dev.marston.randomloot.loot.modifiers;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface HoldModifier extends Modifier {
	/**
	 * Called by {@link TraitDispatch#tick} every server tick the gear is held, or worn in
	 * its own slot. Pace anything slower off the game clock, not off state stored on the
	 * gear: a write to a held stack resets the block its holder is mining.
	 */
	public void hold(ItemStack stack, Level level, Entity holder);
}
