package dev.marston.randomloot.loot;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import dev.marston.randomloot.component.ModDataComponents;
import dev.marston.randomloot.component.ToolModifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * The per-stack tag store behind {@link LootUtils}' named accessors.
 *
 * <p>Gear state lives in the {@code TOOL_MODIFIER} data component as a map of namespace
 * to raw {@code CompoundTag}. The namespaces are named here rather than repeated as bare
 * string literals at the call sites.
 *
 * <p>{@link #mutate} is the only way to change a namespace. It reads, applies the edit,
 * writes back, and refreshes the derived vanilla components (attributes and max damage)
 * on the way out. That last step used to be a convention each mutator had to remember -
 * {@code setLevelAndXP}, {@code setTexture} and {@code CloneItem} all forgot, and the
 * inventoryTick safety net cannot recover a stack that was stamped once and then mutated,
 * because it bails as soon as it sees an existing ATTRIBUTE_MODIFIERS entry. Going
 * through {@code mutate} makes forgetting impossible.
 */
public final class GearTags {

	private GearTags() {
	}

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
	 * The stack's {@code namespace} element, or an empty tag when absent. The returned
	 * tag is detached - editing it does nothing unless it is written back, which is why
	 * callers should use {@link #mutate} rather than pairing this with {@link #write}.
	 */
	public static CompoundTag read(ItemStack stack, String namespace) {
		@Nullable ToolModifier mods = stack.get(ModDataComponents.TOOL_MODIFIER.get());
		if (mods == null) {
			return new CompoundTag();
		}

		CompoundTag tag = mods.getTags().get(namespace);
		return tag == null ? new CompoundTag() : tag;
	}

	/**
	 * Allocation-free presence check for a non-empty element; prefer this over
	 * {@code !read(...).isEmpty()} on per-tick paths.
	 */
	public static boolean has(ItemStack stack, String namespace) {
		@Nullable ToolModifier mods = stack.get(ModDataComponents.TOOL_MODIFIER.get());
		if (mods == null) {
			return false;
		}

		CompoundTag tag = mods.getTags().get(namespace);
		return tag != null && !tag.isEmpty();
	}

	/** Applies {@code edit} to the stack's {@code namespace} element and refreshes derived components. */
	public static void mutate(ItemStack stack, String namespace, Consumer<CompoundTag> edit) {
		CompoundTag tag = read(stack, namespace);
		edit.accept(tag);
		write(stack, namespace, tag);
		LootUtils.refreshDerivedComponents(stack);
	}

	/** Drops a whole namespace and refreshes derived components. */
	public static void clear(ItemStack stack, String namespace) {
		replaceTags(stack, tags -> tags.remove(namespace));
		LootUtils.refreshDerivedComponents(stack);
	}

	/**
	 * Writes a namespace <em>without</em> refreshing derived components. This is the raw
	 * layer: use {@link #mutate} unless you are deliberately building a stack that has
	 * not had its components stamped (the migration GameTest does exactly that).
	 */
	public static void write(ItemStack stack, String namespace, CompoundTag tag) {
		replaceTags(stack, tags -> tags.put(namespace, tag));
	}

	private static void replaceTags(ItemStack stack, Consumer<Map<String, CompoundTag>> edit) {
		@Nullable ToolModifier mods = stack.get(ModDataComponents.TOOL_MODIFIER.get());
		Map<String, CompoundTag> tags = mods == null ? new HashMap<>() : new HashMap<>(mods.getTags());

		edit.accept(tags);

		stack.set(ModDataComponents.TOOL_MODIFIER.get(), new ToolModifier(tags));
	}
}
