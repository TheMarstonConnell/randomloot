package dev.marston.randomloot.loot.modifiers;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.ToolType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;

/**
 * Whether an off-hand item would do something with a right-click, for
 * {@link UseModifier#yieldsToOffhand}.
 *
 * <p>There is no vanilla query for "is this item usable", so this mirrors how a click is
 * routed: {@code Item.use} / {@code Item.useOn} act on a handful of data components, and
 * every other usable item (ender pearl, snowball, bucket, fishing rod, flint and steel,
 * modded items) is a class that overrides one of those two methods. Checking for the
 * override covers them all without a list to keep in step with the game. It errs toward
 * "usable": an override that happens to pass still makes the trait stand down, which is
 * the cheap direction to be wrong in.
 */
final class OffhandUse {

	private OffhandUse() {
	}

	/** Item classes that override {@code Item.use}: they act on a plain right-click. */
	private static final ClassValue<Boolean> OVERRIDES_USE = overrideCheck("use", Level.class, Player.class,
			InteractionHand.class);

	/** Item classes that override {@code Item.useOn}: they act on a targeted block. */
	private static final ClassValue<Boolean> OVERRIDES_USE_ON = overrideCheck("useOn", UseOnContext.class);

	/** Cached per item class; the game ships with official names, so the lookup is by name. */
	private static ClassValue<Boolean> overrideCheck(String method, Class<?>... parameters) {
		return new ClassValue<>() {
			@Override
			protected Boolean computeValue(Class<?> type) {
				try {
					return type.getMethod(method, parameters).getDeclaringClass() != Item.class;
				} catch (NoSuchMethodException e) {
					return false;
				}
			}
		};
	}

	/** Whether right-clicking at nothing in particular would use the stack. */
	static boolean actsInAir(ItemStack stack, Player player) {
		// Our own tools override use() for every stack; only right-click-anywhere traits act.
		if (stack.getItem() instanceof LootItem) {
			for (Modifier mod : LootUtils.getModifiers(stack)) {
				if (mod instanceof UseModifier use && use.useAnywhere()) {
					return true;
				}
			}
			return false;
		}

		// Rockets are the one item players park in the off-hand that only acts mid-flight.
		if (stack.is(Items.FIREWORK_ROCKET)) {
			return player.isFallFlying();
		}

		if (stack.getUseAnimation() != ItemUseAnimation.NONE) {
			return true;
		}

		// The component-driven uses the base Item.use handles itself.
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		if (stack.has(DataComponents.CONSUMABLE) || (equippable != null && equippable.swappable())
				|| stack.has(DataComponents.BLOCKS_ATTACKS) || stack.has(DataComponents.KINETIC_WEAPON)) {
			return true;
		}

		return OVERRIDES_USE.get(stack.getItem().getClass());
	}

	/** Whether right-clicking a block would use the stack on it. */
	static boolean actsOnBlock(ItemStack stack) {
		// Our own tools: only axes and shovels have a block action, plus any right-click trait.
		if (stack.getItem() instanceof LootItem) {
			ToolType type = LootUtils.getToolType(stack);
			if (type == ToolType.AXE || type == ToolType.SHOVEL) {
				return true;
			}
			for (Modifier mod : LootUtils.getModifiers(stack)) {
				if (mod instanceof UseModifier) {
					return true;
				}
			}
			return false;
		}

		// BLOCK_TRANSFORMER is the component the base Item.useOn handles (strip, flatten).
		return stack.has(DataComponents.BLOCK_TRANSFORMER) || OVERRIDES_USE_ON.get(stack.getItem().getClass());
	}
}
