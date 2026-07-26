package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import dev.marston.randomloot.loot.modifiers.BiomeRestrictedModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.LeveledModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;


public class Aquatic extends LeveledModifier implements HoldModifier, BiomeRestrictedModifier {

	public Aquatic(String name, int level) {
		this.name = name;
		this.level = level;
	}

	public Aquatic() {
		this("Aquatic", 0);
	}

	@Override
	protected int minLevel() {
		return 0;
	}

	@Override
	protected int maxLevel() {
		return 2; // Max level 3 (0, 1, 2)
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Aquatic(tag.getStringOr(NAME, "Aquatic"), ModifierConstants.getLevel(tag, 0));
	}

	@Override
	public String tagName() {
		return "aquatic";
	}

	@Override
	public ChatFormatting color() {
		return ChatFormatting.AQUA;
	}

	@Override
	public String description() {
		return "Grants water breathing and Haste " + LootUtils.roman(this.level + 2) + " when underwater.";
	}

	@Override
	public boolean forTool(ToolType type) {
		return isMiningTool(type) || type.isArmor();
	}

	@Override
	public void hold(ItemStack stack, Level level, Entity holder) {
		if (!(holder instanceof LivingEntity living)) return;

		// Water breathing
		living.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 40, 0, false, false));

		// Extra haste when underwater
		if (living.isUnderWater()) {
			living.addEffect(new MobEffectInstance(MobEffects.HASTE, 40, this.level + 1, true, false));
		}
	}

	@Override
	public String describeRestriction() {
		return "Ocean and river biomes";
	}

	@Override
	public boolean canSpawnInBiome(String biomeKey, float temperature, String dimension) {
		return biomeKey != null && (
			biomeKey.contains("ocean") ||
			biomeKey.contains("river")
		);
	}
}
