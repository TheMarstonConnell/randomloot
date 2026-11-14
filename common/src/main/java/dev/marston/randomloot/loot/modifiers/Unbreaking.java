package dev.marston.randomloot.loot.modifiers;

import dev.marston.randomloot.loot.ToolType;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;


public class Unbreaking extends LeveledModifier {

	/** Chance to skip durability loss: 20% at level 0, +20% per level, 100% when maxed. */
	private static final float CHANCE_PER_LEVEL = 0.2f;

	public Unbreaking(String name, int level) {
		this.name = name;
		this.level = level;
	}

	public Unbreaking() {
		this("Unbreaking", 0);
	}

	@Override
	protected int minLevel() {
		return 0;
	}

	@Override
	protected int maxLevel() {
		return 4;
	}

	public String tagName() {
		return "unbreaking";
	}

	public String description() {
		return "This tool has a " + String.format("%.0f", chance() * 100) + "% chance of not taking damage.";
	}

	@Override
	public String name() {
		if (this.level > 0 && !this.canLevel()) {
			return "Unbreakable";
		}
		return super.name();
	}

	public ChatFormatting color() {
		return ChatFormatting.AQUA;
	}

	public Modifier fromNBT(CompoundTag tag) {
		return new Unbreaking(tag.getStringOr(NAME, "Unbreaking"), ModifierConstants.getLevel(tag, 0));
	}

	public boolean forTool(ToolType type) {
		return true;
	}

	private float chance() {
		return CHANCE_PER_LEVEL * (float) (this.level + 1);
	}

	/**
	 * How many of {@code points} of durability the gear still has to pay: each point gets
	 * its own roll to be let off. The one rule for tools and worn armor alike.
	 */
	public int remaining(int points, Level level) {
		int owed = 0;
		for (int i = 0; i < points; i++) {
			if (level.getRandom().nextFloat() > chance()) {
				owed++;
			}
		}
		return owed;
	}
}
