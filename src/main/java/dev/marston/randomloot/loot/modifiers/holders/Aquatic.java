package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import dev.marston.randomloot.loot.modifiers.BiomeRestrictedModifier;
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


public class Aquatic extends AbstractModifier implements HoldModifier, BiomeRestrictedModifier {

	private int level = 0;

	public Aquatic(String name, int level) {
		this.name = name;
		this.level = level;
	}

	public Aquatic() {
		this.name = "Aquatic";
		this.level = 0;
	}

	public Modifier clone() {
		return new Aquatic();
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
		return new Aquatic(tag.getStringOr(NAME, "Aquatic"), ModifierConstants.getLevel(tag, 0));
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
		return "aquatic";
	}

	@Override
	public String color() {
		return ChatFormatting.AQUA.getName();
	}

	@Override
	public String description() {
		return "Grants water breathing and Haste " + LootUtils.roman(this.level + 2) + " when underwater.";
	}

	@Override
	public boolean forTool(ToolType type) {
		return isMiningTool(type);
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
			biomeKey.contains("ocean") ||
			biomeKey.contains("river")
		);
	}
}
