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

    <T> Supplier<T> register(ResourceKey<? extends Registry<T>> registry, String name, Supplier<T> value);
}
