package dev.marston.randomloot.loot.modifiers.users;

import dev.marston.randomloot.advancements.ModCriteria;
import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import dev.marston.randomloot.loot.modifiers.BiomeRestrictedModifier;
import dev.marston.randomloot.loot.modifiers.LeveledModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import dev.marston.randomloot.loot.modifiers.UseModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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


public class VoidTouched extends LeveledModifier implements UseModifier, BiomeRestrictedModifier {

	public VoidTouched(String name, int level) {
		this.name = name;
		this.level = level;
	}

	public VoidTouched() {
		this("Void-Touched", 0);
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
		return new VoidTouched(tag.getStringOr(NAME, "Void-Touched"), ModifierConstants.getLevel(tag, 0));
	}

	@Override
	public String tagName() {
		return "void_touched";
	}

	@Override
	public ChatFormatting color() {
		return ChatFormatting.DARK_PURPLE;
	}

	@Override
	public String description() {
		float distance = 8.0f + (this.level * 4.0f);
		return "Right-click to teleport up to " + distance + " blocks. Costs 10 durability.";
	}

	@Override
	public boolean compatible(Modifier mod) {
		// Allow leveling up by being compatible with same modifier
		if (mod.tagName().equals(this.tagName())) {
			return true;
		}
		// Incompatible with other USERS modifiers
		return !ModifierRegistry.USERS.contains(mod);
	}

	@Override
	public boolean forTool(ToolType type) {
		// Right-click traits never fire from worn armor.
		return !type.isArmor();
	}

	@Override
	public InteractionResult use(UseOnContext ctx) {
		return InteractionResult.PASS;
	}

	@Override
	public boolean use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) return false;

		float distance = 8.0f + (this.level * 4.0f);
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

		player.getItemInHand(hand).hurtAndBreak(10, player, EquipmentSlot.MAINHAND);
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
	public boolean canSpawnInBiome(String biomeKey, float temperature, String dimension) {
		return dimension != null && dimension.equals("minecraft:the_end");
	}
}
