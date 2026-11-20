package dev.marston.randomloot.loot;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.items.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * Loot injection: which loot tables hand out cases and templates, at what chance, and
 * the roll itself. A loader shim only says which table it is and where the stacks go.
 *
 * <p>The two loaders deliver it differently, and each keeps the scope it has:
 * <ul>
 * <li><b>NeoForge</b> runs a global loot modifier on the finished roll, once per
 * injected item, with the id of the table that was asked for. A table rolled from
 * inside that one is never seen. {@link #rollItem}.</li>
 * <li><b>Fabric</b> adds a pool per injected item to every matching table as it loads
 * (Fabric API for 1.21.1 has no event for a table's finished drops, which 26.x uses).
 * Tables rolled from inside another share the outer table's {@link LootContext}, and
 * the shim's condition on each pool keeps an item to one roll per context, asked for
 * here. {@link #rollsItem}.</li>
 * </ul>
 * They differ only when a datapack or another mod nests a matching table inside one
 * that does not match: Fabric injects there and NeoForge does not.
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

		for (String match : Config.LOOT_TABLE_MATCHES.get()) {
			if (tablePath.contains(match)) {
				return true;
			}
		}
		return false;
	}

	/** Every injected item with its current configured chance. */
	public static List<Entry> entries() {
		return List.of(
				new Entry(ModItems.CASE.get(), Config.CASE_CHANCE.get()),
				new Entry(ModItems.MOD_ADD.get(), Config.MOD_CHANCE.get()));
	}

	/**
	 * Gives {@code drops} the stacks of one injected item to add to a finished roll of
	 * {@code queriedTable}: none when the table is not an injection target or
	 * {@code item} is not injected. For a loader that calls once per item and top-level
	 * roll.
	 */
	public static void rollItem(ResourceLocation queriedTable, LootContext context, Item item, Consumer<ItemStack> drops) {
		if (!matchesTable(queriedTable.getPath())) {
			return;
		}

		for (Entry entry : entries()) {
			if (entry.item() == item) {
				roll(entry, context, drops);
			}
		}
	}

	/**
	 * Whether {@code item} drops for this roll: one draw from the context's random against
	 * the item's chance as the config has it now. False for an item that is not injected.
	 * For a loader that injects through pools on the tables and only asks, per pool,
	 * whether it pays out.
	 */
	public static boolean rollsItem(Item item, LootContext context) {
		for (Entry entry : entries()) {
			if (entry.item() == item) {
				return context.getRandom().nextDouble() < entry.chance();
			}
		}
		return false;
	}

	/** One draw from the context's random per entry, whatever the outcome. */
	private static void roll(Entry entry, LootContext context, Consumer<ItemStack> drops) {
		if (context.getRandom().nextDouble() < entry.chance()) {
			drops.accept(new ItemStack(entry.item()));
		}
	}
}
