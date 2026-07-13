package dev.marston.randomloot.neoforge;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.platform.services.RegHelper;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Backs common registration with DeferredRegisters. Common registration
 * classes are classloaded (populating the registers) before the mod
 * constructor calls {@link #registerBuses}.
 */
public class NeoForgeRegHelper implements RegHelper {

    // LinkedHashMap so bus registration order is deterministic.
    private static final Map<ResourceKey<? extends Registry<?>>, DeferredRegister<?>> REGISTERS = new LinkedHashMap<>();

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RandomLoot.MODID);

    @SuppressWarnings("unchecked")
    private static <T> DeferredRegister<T> registerFor(ResourceKey<? extends Registry<T>> key) {
        return (DeferredRegister<T>) REGISTERS.computeIfAbsent(key, k -> DeferredRegister.create(key, RandomLoot.MODID));
    }

    @Override
    public Supplier<Item> registerItem(String name, Function<Item.Properties, Item> factory) {
        return ITEMS.registerItem(name, factory);
    }

    @Override
    public <T, R extends T> Supplier<R> register(ResourceKey<? extends Registry<T>> registry, String name, Supplier<R> value) {
        return registerFor(registry).register(name, value);
    }

    static void registerBuses(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        for (DeferredRegister<?> register : REGISTERS.values()) {
            register.register(modEventBus);
        }
    }
}
