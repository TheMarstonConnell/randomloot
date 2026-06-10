package dev.marston.randomloot.loot.modifiers.wearers;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import dev.marston.randomloot.loot.modifiers.WearerHurtModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Armor trait: chance to block half of any incoming hit, with a shield clang.
 */
public class Bulwark extends AbstractModifier implements WearerHurtModifier {

	/** Chance to halve a hit: 10% at level 0, +10% per level, 50% when maxed. */
	private static final float CHANCE_PER_LEVEL = 0.1f;
	private static final int MAX_LEVEL = 4;

	private int level;

	public Bulwark(String name, int level) {
		this.name = name;
		this.level = level;
	}

	public Bulwark() {
		this("Bulwark", 0);
	}

	@Override
	public String tagName() {
		return "bulwark";
	}

	@Override
	public String description() {
		return "Has a " + String.format("%.0f", chance() * 100) + "% chance of halving damage taken.";
	}

	@Override
	public String name() {
		if (this.level == 0) {
			return this.name;
		}

		return this.name + " " + LootUtils.roman(this.level + 1);
	}

	@Override
	public String color() {
		return ChatFormatting.DARK_AQUA.getName();
	}

	@Override
	public Modifier clone() {
		return new Bulwark(this.name, this.level);
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
		return new Bulwark(tag.getStringOr(NAME, "Bulwark"), ModifierConstants.getLevel(tag, 0));
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.isArmor();
	}

	@Override
	public boolean canLevel() {
		return this.level < MAX_LEVEL;
	}

	@Override
	public void levelUp() {
		this.level++;
	}

	private float chance() {
		return CHANCE_PER_LEVEL * (float) (this.level + 1);
	}

	@Override
	public float onWearerHurt(ItemStack stack, LivingEntity wearer, DamageSource source, float damage) {

		if (wearer.level().getRandom().nextFloat() > chance()) {
			return damage;
		}

		wearer.level().playSound(null, wearer.getX(), wearer.getY(), wearer.getZ(), SoundEvents.SHIELD_BLOCK.value(),
				wearer.getSoundSource(), 1.0f, 1.0f);

		return damage / 2.0f;
	}
}
