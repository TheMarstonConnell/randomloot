package dev.marston.randomloot.fabric;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.platform.services.RegHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import java.util.function.Function;
import java.util.function.Supplier;

/** Fabric registration is immediate: registries are open during mod init. */
public class FabricRegHelper implements RegHelper {

    @Override
    public Supplier<Item> registerItem(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(RandomLoot.MODID, name));
        Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
        return () -> item;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, R extends T> Supplier<R> register(ResourceKey<? extends Registry<T>> registry, String name, Supplier<R> value) {
        Registry<T> target = (Registry<T>) BuiltInRegistries.REGISTRY.getValue(registry.identifier());
        if (target == null) {
            throw new IllegalArgumentException("Unknown registry " + registry);
        }
        R registered = Registry.register(target, Identifier.fromNamespaceAndPath(RandomLoot.MODID, name), value.get());
        return () -> registered;
    }
}
