package dev.marston.randomloot.loot.modifiers;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Routes kills to {@link EntityKillModifier} traits on the killer's main-hand
 * tool. Called from each loader's death event (rather than checking victim
 * health in the hurt hook) so kills are credited no matter how the trait's
 * tool finished the job.
 */
public final class KillDispatcher {

	private KillDispatcher() {
	}

	public static void onLivingDeath(LivingEntity victim, DamageSource source) {
		if (victim.level().isClientSide()) {
			return;
		}

		if (!(source.getEntity() instanceof LivingEntity killer)) {
			return;
		}

		ItemStack weapon = killer.getMainHandItem();
		if (!(weapon.getItem() instanceof LootItem)) {
			return;
		}

		for (Modifier mod : LootUtils.getModifiers(weapon)) {
			if (mod instanceof EntityKillModifier killMod && Config.traitEnabled(mod.tagName())) {
				killMod.onKill(weapon, victim, killer);
			}
		}
	}
}
