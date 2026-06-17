package dev.marston.randomloot.loot.modifiers.wearers;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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

	public Magnetized(String name) {
		this.name = name;
	}

	public Magnetized() {
		this("Magnetized");
	}

	@Override
	public String tagName() {
		return "magnetized";
	}

	@Override
	public String description() {
		return "While worn, nearby items are pulled toward you.";
	}

	@Override
	public ChatFormatting color() {
		return ChatFormatting.LIGHT_PURPLE;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Magnetized(tag.getStringOr(NAME, "Magnetized"));
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.isArmor();
	}

	@Override
	public void hold(ItemStack stack, Level level, Entity holder) {
		if (level.isClientSide()) {
			return;
		}

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
