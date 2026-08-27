package dev.marston.randomloot.loot;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.items.ModItems;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * The loot-injection policy - which tables receive cases/templates and at what
 * chance. Owned by common so the loaders only differ in delivery mechanism:
 * NeoForge applies it per-roll via the case_item global loot modifier, Fabric
 * bakes pools into the tables via LootTableEvents.MODIFY.
 */
public final class LootInjection {

	public record Entry(Item item, double chance) {
	}

	private LootInjection() {
	}

	/**
	 * Loot tables a block drops when broken. Never an injection target however the
	 * config is written: vanilla names the chest block's drop table
	 * {@code blocks/chest} (likewise {@code blocks/trapped_chest} and
	 * {@code blocks/ender_chest}), so the default "chest" substring matched it and
	 * every chest broken - empty, player-placed, any of them - rolled a case and a
	 * template on top of the chest item itself. Injection targets container loot.
	 */
	private static final String BLOCK_DROPS = "blocks/";

	/**
	 * Which loot tables get injected into; substring match, configurable (default:
	 * "chest"), minus {@linkplain #BLOCK_DROPS block drops}. The exclusion is not
	 * configurable because it isn't a preference - a table under {@code blocks/} is
	 * what a block drops when broken, never chest loot.
	 */
	public static boolean matchesTable(String tablePath) {
		if (tablePath.startsWith(BLOCK_DROPS)) {
			return false;
		}

		for (String match : Config.LootTableMatches) {
			if (tablePath.contains(match)) {
				return true;
			}
		}
		return false;
	}

	/** Every injected item with its current configured chance. */
	public static List<Entry> entries() {
		return List.of(
				new Entry(ModItems.CASE.get(), Config.CaseChance),
				new Entry(ModItems.MOD_ADD.get(), Config.ModChance));
	}

	/** The configured chance for an injectable item, or -1 when the item isn't injected. */
	public static double chanceFor(Item item) {
		for (Entry entry : entries()) {
			if (entry.item() == item) {
				return entry.chance();
			}
		}
		return -1.0;
	}
}
