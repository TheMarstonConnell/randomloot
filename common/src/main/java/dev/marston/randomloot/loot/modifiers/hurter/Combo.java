package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.ChargeTracker;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import dev.marston.randomloot.loot.modifiers.Stored;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class Combo extends AbstractModifier implements EntityHurtModifier {
	private final Stored<Integer> points = stored(ModifierConstants.POINTS, 2);
	private final Stored<Long> charged = stored(ModifierConstants.CHARGED, 0L);

	@Override
	public String description() {
		return "Hitting enemies within " + this.points.get() + " seconds after hitting them deals an extra 25% damage.";
	}

	@Override
	public Component writeDetailsToLore(Level level) {
		if (level != null) {
			float charge = ChargeTracker.getCharge(level, charged.get(), points.get());

			String s = "Not Ready";
			if (charge < 1.0f) {
				s = "Ready";
			}

			return Modifier.makeComp(s, ChatFormatting.RED);
		}

		return null;
	}

	@Override
	public void hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {

		Level level = hurtee.level();

		long time = level.getGameTime();

		if (ChargeTracker.getCharge(level, charged.get(), points.get()) < 1.0f) {

			float damage = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));

			dealBonusDamage(hurtee, hurter, damage * 0.25f);

			Modifier.TrackEntityParticle(level, hurtee, ParticleTypes.CRIT);

		}

		this.charged.set(time);
	}
}
