package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.NbtCompat;

import dev.marston.randomloot.loot.ToolType;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;


public class Overgrown extends LeveledModifier implements EntityHurtModifier, HoldModifier, BiomeRestrictedModifier {

	public Overgrown(String name, int level) {
		this.name = name;
		this.level = level;
	}

	public Overgrown() {
		this("Overgrown", 0);
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
		return new Overgrown(NbtCompat.getStringOr(tag, NAME, "Overgrown"), ModifierConstants.getLevel(tag, 0));
	}

	@Override
	public String tagName() {
		return "overgrown";
	}

	@Override
	public ChatFormatting color() {
		return ChatFormatting.GREEN;
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
			if (hurtee.level().isClientSide()) {
				return false;
			}
			float bonusDamage = 2.5f + (this.level * 2.5f);
			dealBonusDamage(hurtee, hurter, bonusDamage);
			hurtee.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, this.level));
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
	public String describeRestriction() {
		return "Jungle, swamp and bamboo biomes";
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
