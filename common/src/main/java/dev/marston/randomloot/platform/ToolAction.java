package dev.marston.randomloot.platform;

import net.minecraft.core.Holder;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.component.BlockTransformers;

/**
 * Loader-neutral names for the vanilla tool right-click behaviors LootItem emulates. Each
 * action carries the 26.3 {@code BlockTransformer} datapack-registry key whose sub-transforms
 * model it and the sound that sub-transform uses, so both loader platform helpers resolve
 * through one shared mapping instead of duplicating it.
 */
public enum ToolAction {
    AXE_STRIP(BlockTransformers.AXE, SoundEvents.AXE_STRIP),
    AXE_SCRAPE(BlockTransformers.AXE, SoundEvents.AXE_SCRAPE),
    AXE_WAX_OFF(BlockTransformers.AXE, SoundEvents.AXE_WAX_OFF),
    SHOVEL_FLATTEN(BlockTransformers.SHOVEL, SoundEvents.SHOVEL_FLATTEN);

    private final ResourceKey<BlockTransformer> transformerKey;
    private final Holder<SoundEvent> sound;

    ToolAction(ResourceKey<BlockTransformer> transformerKey, Holder<SoundEvent> sound) {
        this.transformerKey = transformerKey;
        this.sound = sound;
    }

    /** The vanilla BlockTransformer registry key whose sub-transforms model this action. */
    public ResourceKey<BlockTransformer> transformerKey() {
        return transformerKey;
    }

    /** The sound the matching sub-transform carries, used to pick this action out of the merged transformer. */
    public Holder<SoundEvent> sound() {
        return sound;
    }
}
