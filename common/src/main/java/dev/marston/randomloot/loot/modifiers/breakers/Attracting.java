package dev.marston.randomloot.loot.modifiers.breakers;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Attracting extends AbstractModifier implements BlockBreakModifier {

	public Attracting(String name) {
		this.name = name;
	}

	public Attracting() {
		this.name = "Magnetic";
	}

	@Override
	public boolean startBreak(ItemStack itemstack, BlockPos pos, LivingEntity player) {

		Level level = player.level();

		if (level.isClientSide()) {
			return false;
		}

		ServerLevel serverLevel = (ServerLevel) level;
		AABB box = new AABB(Vec3.atCenterOf(pos.east().south().below()), Vec3.atCenterOf(pos.west().north().above()));

		// Schedule execution after a short delay to allow block drops to spawn
		// Then submit to server thread for thread-safe execution
		CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS).execute(() -> {
			serverLevel.getServer().execute(() -> {
				List<Entity> items = level.getEntities(null, box);

				for (Entity entity : items) {
					if (entity.getType() == EntityTypes.ITEM) {
						entity.setPos(player.position());
					}
				}
			});
		});

		return false;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Attracting(tag.getStringOr(NAME, "Magnetic"));
	}

	@Override
	public String tagName() {
		return "attracting";
	}

	@Override
	public ChatFormatting color() {
		return ChatFormatting.RED;
	}

	@Override
	public String description() {
		return "Upon breaking a block (allowed by tool type), all items at that block's position will teleport to you.";
	}

	@Override
	public boolean forTool(ToolType type) {
		return isMiningTool(type);
	}
}
