package dev.marston.randomloot.loot.modifiers.wearers;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.LeveledModifier;
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
public class Featherweight extends LeveledModifier implements WearerHurtModifier {

	/** Fall damage reduction: 25% at level 0, +25% per level, 100% when maxed. */
	private static final float REDUCTION_PER_LEVEL = 0.25f;

	public Featherweight(String name, int level) {
		this.name = name;
		this.level = level;
	}

	public Featherweight() {
		this("Featherweight", 0);
	}

	@Override
	protected int minLevel() {
		return 0;
	}

	@Override
	protected int maxLevel() {
		return 3;
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
		if (this.level > 0 && !this.canLevel()) {
			return "Weightless";
		}
		return super.name();
	}

	@Override
	public ChatFormatting color() {
		return ChatFormatting.WHITE;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Featherweight(tag.getStringOr(NAME, "Featherweight"), ModifierConstants.getLevel(tag, 0));
	}

	@Override
	public boolean forTool(ToolType type) {
		return type == ToolType.BOOTS;
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
