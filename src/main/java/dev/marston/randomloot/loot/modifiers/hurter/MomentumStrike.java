package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class MomentumStrike implements EntityHurtModifier {
	private String name;
	private float damagePerSpeed;
	private final static String DAMAGE_PER_SPEED = "damage_per_speed";

	public MomentumStrike(String name, float damagePerSpeed) {
		this.name = name;
		this.damagePerSpeed = damagePerSpeed;
	}

	public MomentumStrike() {
		this.name = "Momentum Strike";
		this.damagePerSpeed = 0.05f; // 5% damage per block/second of speed
	}

	public Modifier clone() {
		return new MomentumStrike();
	}

	private float calculateMovementSpeed(LivingEntity entity) {
		Vec3 deltaMovement = entity.getDeltaMovement();
		// Calculate horizontal movement speed (blocks per second)
		double horizontalSpeed = Math.sqrt(deltaMovement.x * deltaMovement.x + deltaMovement.z * deltaMovement.z);
		// Convert from blocks per tick to blocks per second (20 ticks per second)
		return (float) (horizontalSpeed * 20.0);
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		float speed = calculateMovementSpeed(hurter);
		
		// Only apply bonus if moving at least 2 blocks/second (walking speed)
		if (speed < 2.0f) {
			return false;
		}

		// Cap the speed bonus at 20 blocks/second (sprinting with speed effects)
		speed = Mth.clamp(speed, 0, 20.0f);
		
		// Calculate base damage
		float baseDamage = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));
		
		// Calculate bonus damage based on movement speed
		float bonusDamage = baseDamage * (speed * damagePerSpeed);

		// Apply the bonus damage
		if (hurter instanceof Player) {
			Player p = (Player) hurter;
			hurtee.hurt(hurter.damageSources().playerAttack(p), bonusDamage);
		} else {
			hurtee.hurt(hurter.damageSources().mobAttack(hurter), bonusDamage);
		}

		// Add some visual feedback - particle effect based on speed
		if (!hurter.level().isClientSide && speed > 5.0f) {
			// Create a small "impact" effect at the target location
			// This could be expanded with custom particles
			hurter.level().broadcastEntityEvent(hurtee, (byte) 2); // Hurt animation
		}

		return false;
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putFloat(DAMAGE_PER_SPEED, damagePerSpeed);
		tag.putString(NAME, name);
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new MomentumStrike(
			tag.getStringOr(NAME, "Momentum Strike"),
			tag.getFloatOr(DAMAGE_PER_SPEED, 0.05f)
		);
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "momentum_strike";
	}

	@Override
	public String color() {
		return ChatFormatting.RED.getName();
	}

	@Override
	public String description() {
		return "Deals " + (int)(damagePerSpeed * 100) + 
		       "% bonus damage per block/second of movement speed. Faster attacks hit harder!";
	}

	@Override
	public void writeToLore(List<Component> list, boolean shift) {
		MutableComponent comp = Modifier.makeComp(this.name(), this.color());
		list.add(comp);

		if (shift) {
			list.add(Component.literal("  ").append(
				Component.literal("Movement speed: ").withStyle(ChatFormatting.GRAY)
			));
			list.add(Component.literal("  ").append(
				Component.literal("Walking: ~10% bonus").withStyle(ChatFormatting.GREEN)
			));
			list.add(Component.literal("  ").append(
				Component.literal("Sprinting: ~30% bonus").withStyle(ChatFormatting.GOLD)
			));
			list.add(Component.literal("  ").append(
				Component.literal("With Speed II: ~100% bonus").withStyle(ChatFormatting.AQUA)
			));
		}
	}

	@Override
	public boolean compatible(Modifier mod) {
		// Compatible with most other combat modifiers
		return true;
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.SWORD) || type.equals(ToolType.AXE);
	}
}