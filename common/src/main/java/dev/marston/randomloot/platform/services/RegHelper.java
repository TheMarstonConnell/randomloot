package dev.marston.randomloot.platform.services;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Loader-neutral registration. On NeoForge entries land in DeferredRegisters
 * that the mod constructor attaches to the mod event bus; on Fabric they are
 * registered immediately. All registration classes must therefore be
 * classloaded during mod construction/init on both loaders.
 */
public interface RegHelper {

    /** Registers an item, letting the loader wire up the item id on the Properties. */
    Supplier<Item> registerItem(String name, Function<Item.Properties, Item> factory);

    /**
     * Registers into any registry. R is the concrete element type so callers of
     * wildcarded registries (RecipeSerializer&lt;?&gt;, DataComponentType&lt;?&gt;, ...)
     * get back a typed supplier without casts.
     */
    <T, R extends T> Supplier<R> register(ResourceKey<? extends Registry<T>> registry, String name, Supplier<R> value);
}
