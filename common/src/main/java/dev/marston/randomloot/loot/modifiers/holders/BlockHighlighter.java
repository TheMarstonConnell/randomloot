package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

/**
 * Base for "finder" traits that highlight target blocks by spawning short-lived
 * invisible glowing shulkers inside them (OreFinder, TreasureFinder). Owns the block
 * scan, the marker spawn/dedup and the shared marker lifecycle; subclasses only say
 * which blocks to highlight via {@link #isTarget(BlockState)}. The marker lifecycle
 * hooks ({@link #onServerStopping()}, {@link #onServerTick()}) are called from each
 * loader's event shim.
 */
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

	/**
	 * Live markers and the lifecycle sweeps each has survived (from -1). Only touched on
	 * the server thread. An expired marker is discarded outright, with no death animation
	 * or drops. This used to teleport it to (0, -256, 0) with zero health and wait for it
	 * to die, but an entity moved into an unloaded chunk stops ticking and is saved there
	 * instead - so every expired marker piled up in chunk (0, 0)'s entity storage.
	 */
	private static final Map<Shulker, Integer> markers = new HashMap<>();

	/** True for blocks this trait should highlight. */
	protected abstract boolean isTarget(BlockState state);

	public static void onServerStopping() {
		markers.keySet().forEach(Shulker::discard);
		markers.clear();
	}

	/** Post-server-tick marker lifecycle sweep. */
	public static void onServerTick() {
		time = (time + 1) % CLEANUP_INTERVAL;
		if (time != 0) {
			return;
		}

		markers.replaceAll((sh, n) -> n + 1);
		markers.entrySet().removeIf(e -> {
			Shulker sh = e.getKey();
			// Check loaded-ness first: getBlockState on an unloaded position would
			// force the chunk back in just to ask about a marker nobody can see.
			boolean gone = e.getValue() > MAX_MARKER_LIFE
					|| sh.isRemoved()
					|| !sh.level().isLoaded(sh.blockPosition())
					|| sh.level().getBlockState(sh.blockPosition()).isAir();
			if (gone) {
				sh.discard();
			}
			return gone;
		});
	}

	@Override
	public void hold(ItemStack stack, ServerLevel level, LivingEntity holder) {
		if (level.getGameTime() % SCAN_INTERVAL != 0) {
			return;
		}

		for (int i = -SCAN_RADIUS; i < SCAN_RADIUS; i++) {
			for (int j = -SCAN_RADIUS; j < SCAN_RADIUS; j++) {
				for (int k = -SCAN_RADIUS; k < SCAN_RADIUS; k++) {
					BlockPos p = new BlockPos((int) (holder.getX() + i), (int) (holder.getY() + j),
							(int) (holder.getZ() + k));
					if (isTarget(level.getBlockState(p))
							&& level.getEntities(EntityTypes.SHULKER, new AABB(p), e -> true).isEmpty()) {
						spawnMarker(level, p);
					}
				}
			}
		}
	}

	private static void spawnMarker(Level level, BlockPos p) {
		// Never written to disk: a marker only means something while this class is
		// tracking it, and a saved copy would come back as an untracked, invulnerable,
		// AI-less shulker stuck inside the block forever. Unsaved entities are also left
		// in memory when their chunk unloads, so the lifecycle sweep can still discard them.
		Shulker se = new Shulker(EntityTypes.SHULKER, level) {
			@Override
			public boolean shouldBeSaved() {
				return false;
			}
		};
		se.setGlowingTag(true);
		se.setPermanentlyInvulnerable(true);
		se.setInvisible(true);
		se.setPos(p.getX(), p.getY(), p.getZ());
		se.setNoAi(true);

		level.addFreshEntity(se);
		se.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 1200, 0, false, false));

		markers.put(se, -1);
	}
}
