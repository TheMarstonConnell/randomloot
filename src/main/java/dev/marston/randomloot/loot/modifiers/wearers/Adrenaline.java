package dev.marston.randomloot.loot.modifiers.wearers;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.LeveledModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import dev.marston.randomloot.loot.modifiers.WearerHurtModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Armor trait: taking a hit triggers a burst of speed.
 */
public class Adrenaline extends LeveledModifier implements WearerHurtModifier {

	/** Speed buff duration in ticks (5 seconds). */
	private static final int DURATION = 100;

	public Adrenaline(String name, int level) {
		this.name = name;
		this.level = level;
	}

	public Adrenaline() {
		this("Adrenaline", 0);
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
		return "adrenaline";
	}

	@Override
	public String description() {
		return "Taking damage grants speed " + LootUtils.roman(this.level + 1) + " for 5 seconds.";
	}

	@Override
	public ChatFormatting color() {
		return ChatFormatting.RED;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Adrenaline(tag.getStringOr(NAME, "Adrenaline"), ModifierConstants.getLevel(tag, 0));
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.isArmor();
	}

	@Override
	public float onWearerHurt(ItemStack stack, LivingEntity wearer, DamageSource source, float damage) {

		wearer.addEffect(new MobEffectInstance(MobEffects.SPEED, DURATION, this.level, false, false));

		return damage;
	}
}
