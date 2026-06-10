package dev.marston.randomloot.loot.modifiers;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;


public class Unbreaking extends AbstractModifier {

	/** Chance to skip durability loss: 20% at level 0, +20% per level, 100% when maxed. */
	private static final float CHANCE_PER_LEVEL = 0.2f;
	private static final int MAX_LEVEL = 4;

	int level;


	public Unbreaking(String name, int level) {
		this.name = name;
		this.level = level;
	}

	public Unbreaking() {
		this("Unbreaking", 0);
	}

	public String tagName() {
		return "unbreaking";
	}

	public String description() {
		return "This tool has a " + String.format("%.0f", chance() * 100) + "% chance of not taking damage.";
	}

	public String name() {
		if (this.level == 0) {
			return this.name;
		}

		if (!this.canLevel()) {
			return "Unbreakable";
		}

		return this.name + " " + LootUtils.roman(this.level + 1);
	}

	public String color() {
		return ChatFormatting.AQUA.getName();
	}

	public Modifier clone() {
		return new Unbreaking(this.name, this.level);
	}

	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();

		tag.putString(NAME, name);
		tag.putInt(ModifierConstants.LEVEL, level);

		return tag;
	}

	public Modifier fromNBT(CompoundTag tag) {
		return new Unbreaking(tag.getStringOr(NAME, "Unbreaking"), ModifierConstants.getLevel(tag, 0));
	}

	public boolean forTool(ToolType type) {
		return true;
	}

	public boolean canLevel() {
		return this.level < MAX_LEVEL;
	}

	public void levelUp() {
		this.level++;
	}

	private float chance() {
		return CHANCE_PER_LEVEL * (float) (this.level + 1);
	}

	public boolean test(Level level) {
		float f = level.getRandom().nextFloat();

		return f <= chance();
	}
}
