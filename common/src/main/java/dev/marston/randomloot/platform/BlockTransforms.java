package dev.marston.randomloot.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Shared helper for the data-driven block-transform system introduced in MC 26.3.
 *
 * <p>Vanilla axe/shovel conversions used to live in the {@code AxeItem.STRIPPABLES} /
 * {@code ShovelItem.FLATTENABLES} maps; those (and the {@code AxeItem}/{@code ShovelItem}
 * classes) were removed in favour of the {@link BlockTransformer} datapack registry. This
 * resolves the {@link BlockState} a {@link ToolAction} would turn the clicked block into,
 * <em>without</em> applying it, so callers keep doing their own sound/durability handling.
 */
public final class BlockTransforms {

	private BlockTransforms() {
	}

	/** The vanilla transformer registry key backing the given action. */
	public static ResourceKey<BlockTransformer> transformerKey(ToolAction action) {
		return action == ToolAction.SHOVEL_FLATTEN ? BlockTransformers.SHOVEL : BlockTransformers.AXE;
	}

	/** The sound the matching sub-transform plays; used to pick it out of the transformer. */
	public static Holder<SoundEvent> sound(ToolAction action) {
		return switch (action) {
			case AXE_STRIP -> SoundEvents.AXE_STRIP;
			case AXE_SCRAPE -> SoundEvents.AXE_SCRAPE;
			case AXE_WAX_OFF -> SoundEvents.AXE_WAX_OFF;
			case SHOVEL_FLATTEN -> SoundEvents.SHOVEL_FLATTEN;
		};
	}

	/** The vanilla transformer holder for the action, looked up from the level's registries. */
	public static Holder<BlockTransformer> transformer(Level level, ToolAction action) {
		return level.registryAccess().lookupOrThrow(Registries.BLOCK_TRANSFORMER).getOrThrow(transformerKey(action));
	}

	/**
	 * Resolves the state {@code action} would produce using the vanilla transformer only.
	 * Loaders that augment the vanilla transforms (e.g. modded entries) should instead call
	 * {@link #resolve} with their augmented list.
	 */
	@Nullable
	public static BlockState resolve(UseOnContext ctx, ToolAction action) {
		return resolve(ctx, action, transformer(ctx.getLevel(), action).value().transforms());
	}

	/**
	 * Resolves the state {@code action} would turn the clicked block into, scanning the given
	 * transform list for the sub-transform matching the action's sound. Returns {@code null}
	 * when the action doesn't apply to the clicked block. Side-effect free.
	 */
	@Nullable
	public static BlockState resolve(UseOnContext ctx, ToolAction action,
			Iterable<BlockTransformer.BlockTransformData> transforms) {
		Level level = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		Holder<SoundEvent> sound = sound(action);
		for (BlockTransformer.BlockTransformData data : transforms) {
			if (!sound.equals(data.sound())) {
				continue;
			}
			if (data.disallowedFaces().contains(ctx.getClickedFace())) {
				continue;
			}
			BlockState result = data.blockStateProvider().value().getOptionalState(level, level.getRandom(), pos);
			if (result != null) {
				return result;
			}
		}
		return null;
	}
}
