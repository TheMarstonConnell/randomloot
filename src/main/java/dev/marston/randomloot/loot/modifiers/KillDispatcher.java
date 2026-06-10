package dev.marston.randomloot.loot.modifiers;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Routes kills to {@link EntityKillModifier} traits on the killer's main-hand
 * tool. Listening to the death event (rather than checking victim health in
 * the hurt hook) credits kills no matter how the trait's tool finished the
 * job.
 */
@EventBusSubscriber(modid = RandomLoot.MODID)
public final class KillDispatcher {

	private KillDispatcher() {
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		LivingEntity victim = event.getEntity();
		if (victim.level().isClientSide()) {
			return;
		}

		if (!(event.getSource().getEntity() instanceof LivingEntity killer)) {
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
