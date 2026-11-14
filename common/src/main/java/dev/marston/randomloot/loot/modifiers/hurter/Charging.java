package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.advancements.ModCriteria;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.ChargeTracker;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.Stored;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class Charging extends AbstractModifier implements EntityHurtModifier {
	private final Stored<Integer> points = stored(ModifierConstants.POINTS, 10);
	private final Stored<Long> charged = stored(ModifierConstants.CHARGED, 0L);

	@Override
	public String description() {
		return "After " + this.points.get()
				+ " seconds, hitting and enemy will summon a lightning bolt and empty the charge meter.";
	}

	@Override
	public Component writeDetailsToLore(Level level) {
		if (level != null) {
			float charge = ChargeTracker.getCharge(level, charged.get(), points.get());

			String perc = String.format("%.0f%% Charged", charge * 100.0f);

			return Modifier.makeComp(perc, ChatFormatting.GREEN);
		}

		return null;
	}

	@Override
	public void hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {

		Level level = hurtee.level();

		long time = level.getGameTime();

		if (level instanceof ServerLevel serverLevel && ChargeTracker.getCharge(level, charged.get(), points.get()) >= 1.0f) {
			LightningBolt lb = new LightningBolt(EntityTypes.LIGHTNING_BOLT, level);
			lb.setPos(hurtee.position());
			if (hurter instanceof ServerPlayer) {
				lb.setCause((ServerPlayer) hurter);
			}

			// A real bolt strikes everything within 3 blocks - which, in melee, always
			// includes the wielder. Show the bolt but apply the strike to the target
			// alone. The primary hit has just started the target's damage cooldown, so
			// clear it or the lightning damage would be swallowed.
			lb.setVisualOnly(true);
			level.addFreshEntity(lb);
			hurtee.damageCooldownTime = 0;
			hurtee.thunderHit(serverLevel, lb);

			ModCriteria.traitUsed(hurter, this);

			this.charged.set(time);
		}
	}
}
