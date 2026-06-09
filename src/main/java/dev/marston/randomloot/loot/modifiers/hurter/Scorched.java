package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import dev.marston.randomloot.loot.modifiers.BiomeRestrictedModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;


public class Scorched extends AbstractModifier implements EntityHurtModifier, HoldModifier, BiomeRestrictedModifier {

	private int level = 0;

	public Scorched(String name, int level) {
		this.name = name;
		this.level = level;
	}

	public Scorched() {
		this.name = "Scorched";
		this.level = 0;
	}

	public Modifier clone() {
		return new Scorched();
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
		return new Scorched(tag.getStringOr(NAME, "Scorched"), ModifierConstants.getLevel(tag, 0));
	}

	@Override
	public String name() {
		if (this.level == 0) {
			return this.name;
		}
		return this.name + " " + LootUtils.roman(this.level + 1);
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
	public boolean canLevel() {
		return level < 2; // Max level 3 (0, 1, 2)
	}

	@Override
	public void levelUp() {
		this.level++;
	}

	@Override
	public boolean canSpawnInBiome(String biomeKey, float temperature, String dimension) {
		return temperature >= 1.0f || (dimension != null && dimension.equals("minecraft:the_nether"));
	}
}
