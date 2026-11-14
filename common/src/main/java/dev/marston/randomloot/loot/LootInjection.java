package dev.marston.randomloot.loot;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.items.ModItems;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Loot injection: which loot tables hand out cases and templates, at what chance, and
 * the roll itself. A loader shim only says which table rolled and adds the stacks it
 * gets back.
 *
 * <p>The two loaders see a roll differently, and each keeps the scope it has:
 * <ul>
 * <li><b>NeoForge</b> runs a global loot modifier on the finished roll, once per
 * injected item, with the id of the table that was asked for. A table rolled from
 * inside that one is never seen. {@link #rollItem}.</li>
 * <li><b>Fabric</b> is told about every table with an id as it rolls, the tables rolled
 * from inside another included; they share the outer table's {@link LootContext}, so
 * the roll happens once per context. {@link #rollOncePerContext}.</li>
 * </ul>
 * For every roll vanilla 26.3 can make the two inject the same: checked against all
 * 1,447 vanilla loot tables. They differ only when a datapack or another mod nests a
 * matching table inside one that does not match: Fabric injects there and NeoForge does
 * not.
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

	/**
	 * Loot contexts that have had their roll. A table that rolls other tables hands them
	 * its own context, so this keeps one chest (or vault) to one roll. Weak keys: a
	 * context is garbage once its generation finishes.
	 */
	private static final Set<LootContext> ROLLED = Collections
			.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

	/**
	 * The stacks of one injected item to add to a finished roll of {@code queriedTable}:
	 * none when the table is not an injection target or {@code item} is not injected.
	 * For a loader that calls once per item and top-level roll.
	 */
	public static List<ItemStack> rollItem(Identifier queriedTable, LootContext context, Item item) {
		if (!matchesTable(queriedTable.getPath())) {
			return List.of();
		}

		List<ItemStack> stacks = new ArrayList<>(1);
		for (Entry entry : entries()) {
			if (entry.item() == item) {
				roll(entry, context, stacks);
			}
		}
		return stacks;
	}

	/**
	 * The stacks to add to the drops of {@code table}: every injected item's roll the
	 * first time a matching table is seen for {@code context}, and none after that. For
	 * a loader that calls for every table in a roll, nested ones included - without the
	 * once, a table that rolls other matching tables paid out once per table (the trial
	 * vault's {@code reward} rolls {@code reward_common}, {@code _rare} and
	 * {@code _unique}).
	 */
	public static List<ItemStack> rollOncePerContext(Identifier table, LootContext context) {
		if (!matchesTable(table.getPath()) || !ROLLED.add(context)) {
			return List.of();
		}

		List<ItemStack> stacks = new ArrayList<>(2);
		for (Entry entry : entries()) {
			roll(entry, context, stacks);
		}
		return stacks;
	}

	/** One draw from the context's random per entry, whatever the outcome. */
	private static void roll(Entry entry, LootContext context, List<ItemStack> stacks) {
		if (context.getRandom().nextDouble() < entry.chance()) {
			stacks.add(new ItemStack(entry.item()));
		}
	}
}
