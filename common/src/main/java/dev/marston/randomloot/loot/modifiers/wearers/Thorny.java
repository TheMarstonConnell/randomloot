package dev.marston.randomloot.loot.modifiers.wearers;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.LeveledModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import dev.marston.randomloot.loot.modifiers.WearerHurtModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Armor trait: reflects a share of incoming damage back at the attacker.
 */
public class Thorny extends LeveledModifier implements WearerHurtModifier {

	/** Share of damage reflected: 15% at level 0, +15% per level, 75% when maxed. */
	private static final float REFLECT_PER_LEVEL = 0.15f;

	public Thorny(String name, int level) {
		this.name = name;
		this.level = level;
	}

	public Thorny() {
		this("Thorny", 0);
	}

	@Override
	protected int minLevel() {
		return 0;
	}

	@Override
	protected int maxLevel() {
		return 4;
	}

	@Override
	public String tagName() {
		return "thorny";
	}

	@Override
	public String description() {
		return "Reflects " + String.format("%.0f", reflectShare() * 100) + "% of damage taken back at the attacker.";
	}

	@Override
	public ChatFormatting color() {
		return ChatFormatting.DARK_GREEN;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Thorny(tag.getStringOr(NAME, "Thorny"), ModifierConstants.getLevel(tag, 0));
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.isArmor();
	}

	private float reflectShare() {
		return REFLECT_PER_LEVEL * (float) (this.level + 1);
	}

	@Override
	public float onWearerHurt(ItemStack stack, LivingEntity wearer, DamageSource source, float damage) {

		if (source.getEntity() instanceof LivingEntity attacker && attacker != wearer) {
			attacker.hurt(wearer.damageSources().thorns(wearer), damage * reflectShare());
		}

		return damage;
	}
}
