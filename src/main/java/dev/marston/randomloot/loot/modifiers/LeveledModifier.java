package dev.marston.randomloot.loot.modifiers;

import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.nbt.CompoundTag;

/**
 * Base for traits that level up via the smithing table. Holds the {@code level} field,
 * the roman-numeral display suffix, the {@code canLevel()}/{@code levelUp()} rules and
 * the {@link ModifierConstants#LEVEL} serialization every leveled trait used to copy.
 *
 * <p>House style for the suffix: the freshly rolled trait ({@link #minLevel()}) shows no
 * numeral, and the first upgrade shows "II" — the bare name reads as level I. Subclasses
 * read the level back in {@code fromNBT} via {@link ModifierConstants#getLevel}.
 */
public abstract class LeveledModifier extends AbstractModifier {

	protected int level;

	/** Level a freshly rolled trait starts at (0 or 1, varies by trait). */
	protected abstract int minLevel();

	/** Level at which the trait stops accepting upgrades. */
	protected abstract int maxLevel();

	@Override
	public String name() {
		if (level == minLevel()) {
			return name;
		}
		return name + " " + LootUtils.roman(level - minLevel() + 1);
	}

	@Override
	public boolean canLevel() {
		return level < maxLevel();
	}

	@Override
	public void levelUp() {
		this.level++;
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = super.toNBT();
		tag.putInt(ModifierConstants.LEVEL, level);
		return tag;
	}
}
