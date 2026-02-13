package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Random;

public class Riftwalk implements EntityHurtModifier {

	private String name;
	private static final float TELEPORT_CHANCE = 0.15f; // 15% chance to teleport behind enemy
	private static final double TELEPORT_RANGE = 3.0; // Teleport up to 3 blocks behind
	private Random random = new Random();

	public Riftwalk() {
		this("Riftwalk");
	}

	public Riftwalk(String name) {
		this.name = name;
	}

	public Modifier clone() {
		return new Riftwalk();
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putString(NAME, name);
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Riftwalk(tag.getStringOr(NAME, "Riftwalk"));
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "riftwalk";
	}

	@Override
	public String color() {
		return ChatFormatting.DARK_BLUE.getName();
	}

	@Override
	public String description() {
		return "15% chance to teleport behind your target after hitting them, dealing bonus damage on the next hit.";
	}

	@Override
	public void writeToLore(List<Component> list, boolean shift) {
		MutableComponent comp = Modifier.makeComp(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.SWORD) || type.equals(ToolType.AXE);
	}

	private void teleportBehindTarget(LivingEntity attacker, LivingEntity target, Level level) {
		if (level.isClientSide()) {
			return;
		}

		// Calculate position behind the target
		double angle = Math.atan2(target.getZ() - attacker.getZ(), target.getX() - attacker.getX());
		
		// Teleport distance (3 blocks behind)
		double teleportX = target.getX() - Math.cos(angle) * TELEPORT_RANGE;
		double teleportZ = target.getZ() - Math.sin(angle) * TELEPORT_RANGE;
		double teleportY = target.getY();

		// Store old position
		double oldX = attacker.getX();
		double oldY = attacker.getY();
		double oldZ = attacker.getZ();

		// Teleport attacker
		attacker.teleportTo(teleportX, teleportY, teleportZ);

		// Play sound at old position
		if (level instanceof ServerLevel serverLevel) {
			serverLevel.playSound(
				null,
				oldX, oldY, oldZ,
				SoundEvents.ENDERMAN_TELEPORT,
				SoundSource.PLAYERS,
				1.0f, 1.0f
			);

			// Particles at old position
			for (int i = 0; i < 16; i++) {
				serverLevel.sendParticles(
					ParticleTypes.PORTAL,
					oldX, oldY + 1.0, oldZ,
					1,
					random.nextGaussian() * 0.5,
					random.nextGaussian() * 0.5,
					random.nextGaussian() * 0.5,
					0.5
				);
			}

			// Sound at new position
			serverLevel.playSound(
				null,
				teleportX, teleportY, teleportZ,
				SoundEvents.ENDERMAN_TELEPORT,
				SoundSource.PLAYERS,
				1.0f, 1.2f
			);

			// Particles at new position
			for (int i = 0; i < 16; i++) {
				serverLevel.sendParticles(
					ParticleTypes.PORTAL,
					teleportX, teleportY + 1.0, teleportZ,
					1,
					random.nextGaussian() * 0.5,
					random.nextGaussian() * 0.5,
					random.nextGaussian() * 0.5,
					0.5
				);
			}
		}
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		Level level = hurtee.level();

		// 15% chance to trigger riftwalk
		if (random.nextFloat() < TELEPORT_CHANCE) {
			teleportBehindTarget(hurter, hurtee, level);
		}

		return false;
	}

	@Override
	public boolean compatible(Modifier mod) {
		// Incompatible with Void-Touched (another teleport modifier)
		return !mod.tagName().equals("void_touched");
	}
}
