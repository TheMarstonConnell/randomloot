package dev.marston.randomloot.loot.modifiers.users;

import dev.marston.randomloot.advancements.ModCriteria;
import dev.marston.randomloot.loot.modifiers.BiomeRestrictedModifier;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import dev.marston.randomloot.loot.modifiers.UseModifier;
import dev.marston.randomloot.loot.modifiers.TraitDispatch;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class VoidTouched extends AbstractModifier implements UseModifier, BiomeRestrictedModifier {
	@Override
	public String description() {
		float distance = 8.0f + (traitLevel() * 4.0f);
		return "Right-click to teleport up to " + distance + " blocks. Costs 10 durability.";
	}

	@Override
	public InteractionResult use(UseOnContext ctx) {
		return InteractionResult.PASS;
	}

	@Override
	public boolean use(Level level, Player player, InteractionHand hand) {
		float distance = 8.0f + (traitLevel() * 4.0f);
		Vec3 lookVec = player.getLookAngle();
		Vec3 destination = player.position().add(
			lookVec.x * distance,
			lookVec.y * distance,
			lookVec.z * distance
		);

		BlockPos targetPos = BlockPos.containing(destination);
		BlockPos safePos = findSafeTeleportLocation(level, targetPos);

		// No safe landing spot: don't consume the use or award the use stat.
		if (safePos == null) {
			return false;
		}

		player.teleportTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);

		if (level instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.PORTAL,
				player.getX(), player.getY() + 1, player.getZ(),
				32, 0.5, 0.5, 0.5, 0.1);
		}

		TraitDispatch.spendDurability(player.getItemInHand(hand), 10, player, EquipmentSlot.MAINHAND);
		player.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.0f);

		ModCriteria.traitUsed(player, this);

		return true;
	}

	private BlockPos findSafeTeleportLocation(Level level, BlockPos target) {
		for (int yOffset = 0; yOffset >= -5; yOffset--) {
			BlockPos checkPos = target.offset(0, yOffset, 0);
			BlockState below = level.getBlockState(checkPos);
			BlockState at = level.getBlockState(checkPos.above());
			BlockState above = level.getBlockState(checkPos.above(2));

			if (below.isSolid() && at.isAir() && above.isAir()) {
				return checkPos.above();
			}
		}
		return null;
	}

	@Override
	public boolean useAnywhere() {
		return true;
	}

	@Override
	public String describeRestriction() {
		return "The End dimension only";
	}

	@Override
	public boolean canSpawnInBiome(String biomeKey, float temperature, String dimension) {
		return dimension != null && dimension.equals("minecraft:the_end");
	}
}
