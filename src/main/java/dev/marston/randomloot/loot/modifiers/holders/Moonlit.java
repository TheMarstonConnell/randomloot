package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.StatsModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class Moonlit implements HoldModifier, StatsModifier, EntityHurtModifier {

	private String name;
	private static final long NIGHT_START = 12542L; // Night starts at tick 12542
	private static final long NIGHT_END = 23458L;   // Night ends at tick 23458
	private static final float NIGHT_BONUS = 0.15f; // 15% bonus
	private static final int EFFECT_DURATION = 3; // 3 ticks duration for effects

	public Moonlit() {
		this("Moonlit");
	}

	public Moonlit(String name) {
		this.name = name;
	}

	public Modifier clone() {
		return new Moonlit();
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putString(NAME, name);
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Moonlit(tag.getStringOr(NAME, "Moonlit"));
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "moonlit";
	}

	@Override
	public String color() {
		return ChatFormatting.DARK_PURPLE.getName();
	}

	@Override
	public String description() {
		return "Grants Luck I and +15% damage/mining speed during nighttime. Applies Weakness I during daytime.";
	}

	@Override
	public void writeToLore(List<Component> list, boolean shift) {
		MutableComponent comp = Modifier.makeComp(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public Component writeDetailsToLore(Level level) {
		if (level != null) {
			if (isNightTime(level)) {
				return Modifier.makeComp("Night ☾", ChatFormatting.DARK_PURPLE);
			} else {
				return Modifier.makeComp("Day ☀", ChatFormatting.YELLOW);
			}
		}
		return null;
	}

	@Override
	public boolean forTool(ToolType type) {
		return true; // Works on all tools
	}

	private boolean isNightTime(Level level) {
		long dayTime = level.getDayTime() % 24000L;
		return dayTime >= NIGHT_START && dayTime <= NIGHT_END;
	}

	@Override
	public float getStats(ItemStack itemstack) {
		// This won't have direct access to level, so we return base 1.0
		// The actual bonus is applied through the damage system in hurtEnemy
		return 1.0f;
	}

	@Override
	public void hold(ItemStack stack, Level level, Entity holder) {
		if (!(holder instanceof LivingEntity livingEntity)) {
			return;
		}

		if (isNightTime(level)) {
			// Night: Apply Luck I and Haste I for mining bonus
			MobEffectInstance luck = new MobEffectInstance(MobEffects.LUCK, EFFECT_DURATION, 0, false, false);
			MobEffectInstance haste = new MobEffectInstance(MobEffects.HASTE, EFFECT_DURATION, 0, false, false);
			livingEntity.addEffect(luck);
			livingEntity.addEffect(haste);
		} else {
			// Day: Apply Weakness I
			MobEffectInstance weakness = new MobEffectInstance(MobEffects.WEAKNESS, EFFECT_DURATION, 0, false, false);
			livingEntity.addEffect(weakness);
		}
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		Level level = hurtee.level();
		
		if (isNightTime(level)) {
			// Night bonus: +15% damage
			float baseDamage = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));
			float bonusDamage = baseDamage * NIGHT_BONUS;
			
			if (hurter instanceof Player p) {
				hurtee.hurt(hurter.damageSources().playerAttack(p), bonusDamage);
			} else {
				hurtee.hurt(hurter.damageSources().mobAttack(hurter), bonusDamage);
			}
		}
		// Note: Weakness effect already applied by hold() handles the day penalty
		
		return false;
	}
}
