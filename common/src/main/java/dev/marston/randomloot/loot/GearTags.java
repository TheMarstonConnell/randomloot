package dev.marston.randomloot.loot;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * The per-stack tag store behind {@link LootUtils}' named accessors.
 *
 * <p>1.20.1 has no data components, so gear state lives in the stack's NBT under the
 * {@link #ROOT} compound as a map of namespace to raw {@code CompoundTag} (26.x and 1.21.1
 * keep the same map in the {@code TOOL_MODIFIER} data component). The namespaces are
 * named here rather than repeated as bare string literals at the call sites.
 *
 * <p>{@link #mutate} is the only way to change a namespace. It reads, applies the edit,
 * writes back, and refreshes the derived stats (the vanilla attribute NBT and the cached max damage)
 * on the way out. That last step used to be a convention each mutator had to remember -
 * {@code setLevelAndXP}, {@code setTexture} and {@code CloneItem} all forgot, and the
 * inventoryTick safety net cannot recover a stack that was stamped once and then mutated,
 * because it bails as soon as it sees an existing {@link #DERIVED} entry. Going
 * through {@code mutate} makes forgetting impossible.
 */
public final class GearTags {

	private GearTags() {
	}

	/** The stack-NBT compound every namespace lives under. */
	public static final String ROOT = "randomloot";

	/**
	 * Values {@link LootUtils#refreshDerivedComponents} caches on the stack (max damage).
	 * Its presence is also the "this stack has been stamped" marker.
	 */
	public static final String DERIVED = "derived";

	/** Gear identity: type, biome, dimension, owner. */
	public static final String INFO = "info";
	/** Goodness, the input to every derived stat. */
	public static final String STATS = "itemStats";
	/** Texture index. */
	public static final String COSMETICS = "cosmetics";
	/** Level and XP progress. */
	public static final String XP = "XP";
	/** The generated "discovered by ... forged by ..." line. */
	public static final String LORE = "itemLore";
	/** Case-opening reveal state; see {@link LootUtils#startRoll}. */
	public static final String ROLLING = "rolling";

	/**
	 * The stack's {@code namespace} element, or an empty tag when absent. Treat the
	 * result as read-only: when the namespace exists this is the very tag stored inside
	 * the data component, which every {@code ItemStack.copy()} of the stack shares. Go
	 * through {@link #mutate} to change it.
	 */
	public static CompoundTag read(ItemStack stack, String namespace) {
		@Nullable CompoundTag root = stack.getTagElement(ROOT);
		if (root == null || !root.contains(namespace, Tag.TAG_COMPOUND)) {
			return new CompoundTag();
		}
		return root.getCompound(namespace);
	}

	/**
	 * Allocation-free presence check for a non-empty element; prefer this over
	 * {@code !read(...).isEmpty()} on per-tick paths.
	 */
	public static boolean has(ItemStack stack, String namespace) {
		@Nullable CompoundTag root = stack.getTagElement(ROOT);
		return root != null && root.contains(namespace, Tag.TAG_COMPOUND) && !root.getCompound(namespace).isEmpty();
	}

	/** Applies {@code edit} to the stack's {@code namespace} element and refreshes derived stats. */
	public static void mutate(ItemStack stack, String namespace, Consumer<CompoundTag> edit) {
		CompoundTag tag = read(stack, namespace).copy();
		edit.accept(tag);
		write(stack, namespace, tag);
		LootUtils.refreshDerivedComponents(stack);
	}

	/** Drops a whole namespace and refreshes derived stats. */
	public static void clear(ItemStack stack, String namespace) {
		@Nullable CompoundTag root = stack.getTagElement(ROOT);
		if (root != null) {
			root.remove(namespace);
		}
		LootUtils.refreshDerivedComponents(stack);
	}

	/**
	 * Writes a namespace <em>without</em> refreshing derived stats. This is the raw
	 * layer: use {@link #mutate} unless you are deliberately building a stack that has
	 * not been stamped (the migration GameTest does exactly that).
	 */
	public static void write(ItemStack stack, String namespace, CompoundTag tag) {
		stack.getOrCreateTagElement(ROOT).put(namespace, tag);
	}
}
