package dev.marston.randomloot.loot.modifiers.wearers;

import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Armor trait: while worn, nearby dropped items drift toward the wearer.
 */
public class Magnetized extends AbstractModifier implements HoldModifier {
	/** Pull radius in blocks. */
	private static final double RANGE = 6.0;
	/** Per-tick acceleration toward the wearer. */
	private static final double PULL = 0.15;

	@Override
	public String description() {
		return "While worn, nearby items are pulled toward you.";
	}

	@Override
	public void hold(ItemStack stack, ServerLevel level, LivingEntity holder) {
		List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, holder.getBoundingBox().inflate(RANGE));

		for (ItemEntity item : items) {
			Vec3 toHolder = holder.position().add(0, holder.getBbHeight() / 2.0, 0).subtract(item.position());
			double dist = toHolder.length();

			// Inside arm's reach vanilla pickup takes over; don't jitter the item.
			if (dist < 1.0) {
				continue;
			}

			item.setDeltaMovement(item.getDeltaMovement().scale(0.8).add(toHolder.normalize().scale(PULL)));
		}
	}
}
