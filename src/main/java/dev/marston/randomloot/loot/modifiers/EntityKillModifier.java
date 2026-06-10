package dev.marston.randomloot.loot.modifiers;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Hook for traits that react to the holder killing something. Dispatched from
 * {@link KillDispatcher} off {@code LivingDeathEvent}, so it fires for every
 * kill the tool gets credit for: direct melee blows, follow-up bonus damage
 * from other traits (Executioner), and indirect kills like Charged lightning
 * or Flaming burn-down. Checking {@code hurtee.getHealth() <= 0} inside
 * {@link EntityHurtModifier#hurtEnemy} can't see any of the indirect paths and
 * depends on modifier iteration order for the rest.
 */
public interface EntityKillModifier extends Modifier {
	void onKill(ItemStack itemstack, LivingEntity victim, LivingEntity killer);
}
