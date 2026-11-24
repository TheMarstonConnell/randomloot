package dev.marston.randomloot.loot.modifiers;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface HoldModifier extends Modifier {
	/**
	 * Called by {@link TraitDispatch#tick} every server tick the gear is held, or worn in
	 * its own slot. Pace anything slower off the game clock, not off state stored on the
	 * gear: a write to a held stack resets the block its holder is mining.
	 */
	public void hold(ItemStack stack, ServerLevel level, LivingEntity holder);
}
