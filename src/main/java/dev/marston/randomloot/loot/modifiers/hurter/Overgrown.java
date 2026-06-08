package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.BiomeRestrictedModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;


public class Overgrown extends AbstractModifier implements EntityHurtModifier, HoldModifier, BiomeRestrictedModifier {

	private int level = 0;
	private final static String LEVEL = "trait_level";

	public Overgrown(String name, int level) {
		this.name = name;
		this.level = level;
	}

	public Overgrown() {
		this.name = "Overgrown";
		this.level = 0;
	}

	public Modifier clone() {
		return new Overgrown();
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putString(NAME, name);
		tag.putInt(LEVEL, level);
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Overgrown(tag.getStringOr(NAME, "Overgrown"), tag.getIntOr(LEVEL, 0));
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
		return "overgrown";
	}

	@Override
	public String color() {
		return ChatFormatting.GREEN.getName();
	}

	@Override
	public String description() {
		float bonusDamage = 2.5f + (this.level * 2.5f);
		return "Grants poison immunity. Deals " + bonusDamage + " bonus damage to arthropods.";
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.SWORD) || type.equals(ToolType.AXE) || type.equals(ToolType.SHOVEL);
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		EntityType<?> type = hurtee.getType();
		boolean isArthropod = type == EntityType.SPIDER ||
							  type == EntityType.CAVE_SPIDER ||
							  type == EntityType.SILVERFISH ||
							  type == EntityType.ENDERMITE ||
							  type == EntityType.BEE;

		if (isArthropod) {
			float bonusDamage = 2.5f + (this.level * 2.5f);
			hurtee.hurt(hurter.damageSources().mobAttack(hurter), bonusDamage);
			hurtee.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, this.level));
		}

		return false;
	}

	@Override
	public void hold(ItemStack stack, Level level, Entity holder) {
		if (!(holder instanceof LivingEntity living)) return;
		if (living.hasEffect(MobEffects.POISON)) {
			living.removeEffect(MobEffects.POISON);
		}
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
		return biomeKey != null && (
			biomeKey.contains("jungle") ||
			biomeKey.contains("swamp") ||
			biomeKey.contains("bamboo")
		);
	}
}
