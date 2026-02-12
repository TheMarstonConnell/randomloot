package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class Vampiric implements EntityHurtModifier, HoldModifier {

	private String name;
	private long lastHit;
	private static final float HEAL_PERCENT = 0.05f; // 5% of damage dealt
	private static final int DRAIN_INTERVAL = 600; // 30 seconds (600 ticks)
	private static final float DRAIN_DAMAGE = 1.0f; // 0.5 hearts

	public Vampiric() {
		this("Vampiric", 0L);
	}

	public Vampiric(String name, long lastHit) {
		this.name = name;
		this.lastHit = lastHit;
	}

	public Modifier clone() {
		return new Vampiric();
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putString(NAME, name);
		tag.putLong("lastHit", lastHit);
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Vampiric(
			tag.getStringOr(NAME, "Vampiric"),
			tag.getLongOr("lastHit", 0L)
		);
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "vampiric";
	}

	@Override
	public String color() {
		return ChatFormatting.DARK_RED.getName();
	}

	@Override
	public String description() {
		return "Heals player for 5% of damage dealt to enemies, but prevents natural health regeneration and causes 0.5 hearts damage per 30 seconds when not hitting enemies.";
	}

	@Override
	public void writeToLore(List<Component> list, boolean shift) {
		MutableComponent comp = Modifier.makeComp(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public Component writeDetailsToLore(Level level) {
		if (level != null && lastHit > 0) {
			long timeSinceHit = level.getGameTime() - lastHit;
			long secondsSinceHit = timeSinceHit / 20;
			
			if (secondsSinceHit < 30) {
				return Modifier.makeComp("Fed (" + (30 - secondsSinceHit) + "s)", ChatFormatting.GREEN);
			} else {
				return Modifier.makeComp("Hungry!", ChatFormatting.DARK_RED);
			}
		}
		return null;
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.SWORD) || type.equals(ToolType.AXE);
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		Level level = hurtee.level();
		float damage = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));
		
		// Heal the player for 5% of damage dealt
		float healAmount = damage * HEAL_PERCENT;
		hurter.heal(healAmount);
		
		// Update last hit time
		this.lastHit = level.getGameTime();
		LootUtils.updateModifier(itemstack, this);
		
		// Visual feedback - red particles for lifesteal
		if (!level.isClientSide()) {
			Modifier.TrackEntityParticle(level, hurter, ParticleTypes.DAMAGE_INDICATOR);
		}
		
		return false;
	}

	@Override
	public void hold(ItemStack stack, Level level, Entity holder) {
		if (!(holder instanceof LivingEntity livingEntity)) {
			return;
		}

		// Prevent natural regeneration
		MobEffectInstance hungerEffect = new MobEffectInstance(MobEffects.HUNGER, 3, 0, false, false);
		livingEntity.addEffect(hungerEffect);
		
		// If we haven't hit anything recently, drain health
		if (lastHit > 0) {
			long timeSinceHit = level.getGameTime() - lastHit;
			
			// Every DRAIN_INTERVAL ticks after the last hit, apply damage
			if (timeSinceHit > 0 && timeSinceHit % DRAIN_INTERVAL == 0) {
				if (holder instanceof Player player) {
					// Create a custom damage source that bypasses armor
					DamageSource vampiricDrain = level.damageSources().magic();
					player.hurt(vampiricDrain, DRAIN_DAMAGE);
					
					// Visual feedback - dark red particles
					if (!level.isClientSide()) {
						Modifier.TrackEntityParticle(level, player, ParticleTypes.SOUL);
					}
				}
			}
		}
	}

	@Override
	public boolean compatible(Modifier mod) {
		// Not compatible with Necrotic (Draining) to avoid lifesteal stacking
		if (mod.tagName().equals("necrotic")) {
			return false;
		}
		return true;
	}
}
