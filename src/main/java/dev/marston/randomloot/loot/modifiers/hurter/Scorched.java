package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import dev.marston.randomloot.loot.modifiers.BiomeRestrictedModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
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


public class Scorched extends LeveledModifier implements EntityHurtModifier, HoldModifier, BiomeRestrictedModifier {

	public Scorched(String name, int level) {
		this.name = name;
		this.level = level;
	}

	public Scorched() {
		this("Scorched", 0);
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
		return new Scorched(tag.getStringOr(NAME, "Scorched"), ModifierConstants.getLevel(tag, 0));
	}

	@Override
	public String tagName() {
		return "scorched";
	}

	@Override
	public String color() {
		return ChatFormatting.GOLD.getName();
	}

	@Override
	public String description() {
		int duration = 4 + (this.level * 2);
		return "Sets enemies on fire for " + duration + " seconds. Grants fire resistance while held.";
	}

	@Override
	public boolean forTool(ToolType type) {
		return isWeapon(type);
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		int fireDuration = (4 + (this.level * 2)) * 20;
		hurtee.setRemainingFireTicks(fireDuration);
		return false;
	}

	@Override
	public void hold(ItemStack stack, Level level, Entity holder) {
		if (!(holder instanceof LivingEntity living)) return;
		living.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, false, false));
	}

	@Override
	public boolean canSpawnInBiome(String biomeKey, float temperature, String dimension) {
		return temperature >= 1.0f || (dimension != null && dimension.equals("minecraft:the_nether"));
	}
}
