package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Base for "finder" traits that highlight target blocks by spawning short-lived
 * invisible glowing shulkers inside them (OreFinder, TreasureFinder). Owns the block
 * scan, the marker spawn/dedup and the shared marker lifecycle; subclasses only say
 * which blocks to highlight via {@link #isTarget(Block)}.
 */
@EventBusSubscriber(modid = RandomLoot.MODID)
public abstract class BlockHighlighter extends AbstractModifier implements HoldModifier {

	/** Half-extent of the cube scanned around the holder. */
	private static final int SCAN_RADIUS = 10;
	/** Ticks between scans; the 20^3 block sweep is too expensive to run every tick. */
	private static final int SCAN_INTERVAL = 20;
	/** Ticks between marker lifecycle sweeps. */
	private static final int CLEANUP_INTERVAL = 10;
	/** Lifecycle sweeps a marker survives before being despawned. */
	private static final int MAX_MARKER_LIFE = 10;

	private static int time = 0;

	// Thread-safe lists for concurrent access between tick events and hold()
	private static final List<Shulker> shulkers = new CopyOnWriteArrayList<>();
	private static final List<Integer> timings = new CopyOnWriteArrayList<>();

	/** True for blocks this trait should highlight. */
	protected abstract boolean isTarget(Block block);

	@SubscribeEvent
	public static void serverStop(ServerStoppingEvent event) {
		for (Shulker shulker : shulkers) {
			kill(shulker);
		}
		shulkers.clear();
		timings.clear();
	}

	@SubscribeEvent
	public static void tickEvent(ServerTickEvent.Post event) {
		time = (time + 1) % CLEANUP_INTERVAL;
		if (time != 0) {
			return;
		}

		for (int i = shulkers.size() - 1; i >= 0; i--) {
			if (i >= shulkers.size() || i >= timings.size()) {
				continue; // List may have been modified
			}

			int tick = timings.get(i) + 1;
			timings.set(i, tick);
			Shulker sh = shulkers.get(i);

			if (tick > MAX_MARKER_LIFE
					|| sh.level().getBlockState(sh.blockPosition()).getBlock().equals(Blocks.AIR)) {
				kill(sh);
				shulkers.remove(i);
				timings.remove(i);
			}
		}
	}

	/** Drops the marker below the world floor so it despawns without visible death. */
	private static void kill(Shulker shulker) {
		shulker.setPos(0, -256, 0);
		shulker.setHealth(0);
	}

	@Override
	public void hold(ItemStack stack, Level level, Entity holder) {
		if (level.getGameTime() % SCAN_INTERVAL != 0) {
			return;
		}

		for (int i = -SCAN_RADIUS; i < SCAN_RADIUS; i++) {
			for (int j = -SCAN_RADIUS; j < SCAN_RADIUS; j++) {
				for (int k = -SCAN_RADIUS; k < SCAN_RADIUS; k++) {
					BlockPos p = new BlockPos((int) (holder.getX() + i), (int) (holder.getY() + j),
							(int) (holder.getZ() + k));
					if (!isTarget(level.getBlockState(p).getBlock())) {
						continue;
					}
					if (!hasMarker(level, p)) {
						spawnMarker(level, p);
					}
				}
			}
		}
	}

	private static boolean hasMarker(Level level, BlockPos p) {
		for (Entity entity : level.getEntities(null, new AABB(p))) {
			if (entity.getType() == EntityType.SHULKER) {
				return true;
			}
		}
		return false;
	}

	private static void spawnMarker(Level level, BlockPos p) {
		Shulker se = new Shulker(EntityType.SHULKER, level);
		se.setGlowingTag(true);
		se.setInvulnerable(true);
		se.setInvisible(true);
		se.setPos(p.getX(), p.getY(), p.getZ());
		se.setNoAi(true);

		level.addFreshEntity(se);
		se.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 1200, 0, false, false));

		shulkers.add(se);
		timings.add(-1);
	}
}
