package dev.marston.randomloot.loot.modifiers.wearers;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import dev.marston.randomloot.loot.modifiers.WearerHurtModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Boots trait: softens fall damage, removing it entirely when maxed.
 */
public class Featherweight extends AbstractModifier implements WearerHurtModifier {

	/** Fall damage reduction: 25% at level 0, +25% per level, 100% when maxed. */
	private static final float REDUCTION_PER_LEVEL = 0.25f;
	private static final int MAX_LEVEL = 3;

	private int level;

	public Featherweight(String name, int level) {
		this.name = name;
		this.level = level;
	}

	public Featherweight() {
		this("Featherweight", 0);
	}

	@Override
	public String tagName() {
		return "featherweight";
	}

	@Override
	public String description() {
		return "Reduces fall damage by " + String.format("%.0f", reduction() * 100) + "%.";
	}

	@Override
	public String name() {
		if (this.level == 0) {
			return this.name;
		}

		if (!this.canLevel()) {
			return "Weightless";
		}

		return this.name + " " + LootUtils.roman(this.level + 1);
	}

	@Override
	public String color() {
		return ChatFormatting.WHITE.getName();
	}

	@Override
	public Modifier clone() {
		return new Featherweight(this.name, this.level);
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();

		tag.putString(NAME, name);
		tag.putInt(ModifierConstants.LEVEL, level);

		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Featherweight(tag.getStringOr(NAME, "Featherweight"), ModifierConstants.getLevel(tag, 0));
	}

	@Override
	public boolean forTool(ToolType type) {
		return type == ToolType.BOOTS;
	}

	@Override
	public boolean canLevel() {
		return this.level < MAX_LEVEL;
	}

	@Override
	public void levelUp() {
		this.level++;
	}

	private float reduction() {
		return REDUCTION_PER_LEVEL * (float) (this.level + 1);
	}

	@Override
	public float onWearerHurt(ItemStack stack, LivingEntity wearer, DamageSource source, float damage) {

		if (!source.is(DamageTypeTags.IS_FALL)) {
			return damage;
		}

		return damage * (1.0f - reduction());
	}
}
