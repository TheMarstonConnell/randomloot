package dev.marston.randomloot.loot;

import dev.marston.randomloot.loot.LootItem.ToolType;

/**
 * Every number derived from a piece of gear's goodness and type: dig speed, attack,
 * defense, durability, and the XP/progression curves.
 *
 * <p>Pure by design - it takes primitives, not an {@code ItemStack}. Reading goodness off
 * a stack means resolving the {@code TOOL_MODIFIER} data component, which needs a bound
 * item registry, which needs a running loader; that dependency is why the stat maths
 * could only ever be checked from an in-world GameTest. The item classes read the stack
 * and hand the numbers here, so the formulas themselves are reachable from a plain JUnit
 * test.
 *
 * <p>{@code statMultiplier} is the product of the gear's {@code StatsModifier} traits -
 * see {@link LootUtils#statMultiplier(net.minecraft.world.item.ItemStack)}.
 */
public final class GearStats {

	private GearStats() {
	}

	/** XP needed to go from {@code level} to the next one. */
	public static int maxXp(int level) {
		return (int) (500 * Math.pow(2, level));
	}

	/** Goodness gained per level-up: a flat 10% on the existing stats. */
	public static float leveledGoodness(float goodness) {
		return goodness * 1.1f;
	}

	/**
	 * The goodness of gear generated for a player who has opened {@code casesOpened}
	 * cases. A square-root curve: gear keeps improving without running away, and stays
	 * comparable to the levelled tool the player is already carrying.
	 */
	public static float goodnessForCases(int casesOpened, double rate) {
		return (float) (Math.sqrt(casesOpened + 1) * rate);
	}

	/** How many traits freshly generated gear starts with. */
	public static int startingTraits(float goodness) {
		return (int) Math.floor(goodness / 2.0f);
	}

	/** Mining speed against a block this tool type is effective on. */
	public static float digSpeed(float goodness, ToolType type, float statMultiplier) {
		if (type == ToolType.SWORD) {
			return 1.0f;
		}
		return ((goodness / 2.0f) + 6.0f) * statMultiplier;
	}

	/** Melee damage bonus, scaled per tool type. */
	public static float attackDamage(float goodness, ToolType type) {
		float damage = goodness + 1.0f;

		return switch (type) {
		case PICKAXE -> damage * 0.5f;
		case AXE -> damage * 1.2f;
		case SHOVEL -> damage * 0.6f;
		default -> damage;
		};
	}

	/** Attack-speed modifier, a per-type constant that ignores goodness. */
	public static float attackSpeed(ToolType type) {
		return switch (type) {
		case PICKAXE -> -2.8f;
		case AXE, SHOVEL -> -3.0f;
		case SWORD -> -2.4f;
		default -> 0.0f;
		};
	}

	/** Armor points for a worn piece; 0 for hand tools. */
	public static float defense(float goodness, ToolType type, float statMultiplier) {
		float scale = switch (type) {
		case CHESTPLATE -> 1.4f;
		case LEGGINGS -> 1.1f;
		case HELMET, BOOTS -> 0.6f;
		default -> 0.0f;
		};

		return (goodness + 1.0f) * scale * statMultiplier;
	}

	/** Armor toughness for a worn piece; 0 for hand tools. */
	public static float toughness(float goodness, ToolType type) {
		if (!type.isArmor()) {
			return 0.0f;
		}
		return goodness * 0.25f;
	}

	/**
	 * Durability. Armor pieces weight it the way vanilla weights its ArmorType unit
	 * durabilities; hand tools use a flat unit.
	 */
	public static int maxDamage(float goodness, ToolType type) {
		int unit = switch (type) {
		case HELMET -> 11;
		case CHESTPLATE -> 16;
		case LEGGINGS -> 15;
		case BOOTS -> 13;
		default -> 10;
		};

		// Hand tools scale by 80 per point; armor by unit * 3.
		float perPoint = type.isArmor() ? unit * 3.0f : 80.0f;

		return (int) ((goodness + 10.0f) * perPoint);
	}
}
