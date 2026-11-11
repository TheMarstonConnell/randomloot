package dev.marston.randomloot.forge;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.platform.services.RegHelper;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Backs common registration with DeferredRegisters. Common registration
 * classes are classloaded (populating the registers) before the mod
 * constructor calls {@link #registerBuses}.
 */
public class ForgeRegHelper implements RegHelper {

    // LinkedHashMap so bus registration order is deterministic.
    private static final Map<ResourceKey<? extends Registry<?>>, DeferredRegister<?>> REGISTERS = new LinkedHashMap<>();

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RandomLoot.MODID);

    @SuppressWarnings("unchecked")
    private static <T> DeferredRegister<T> registerFor(ResourceKey<? extends Registry<T>> key) {
        return (DeferredRegister<T>) REGISTERS.computeIfAbsent(key, k -> DeferredRegister.create(key, RandomLoot.MODID));
    }

    @Override
    public Supplier<Item> registerItem(String name, Function<Item.Properties, Item> factory) {
        return ITEMS.register(name, () -> factory.apply(new Item.Properties()));
    }

    @Override
    public <T, R extends T> Supplier<R> register(ResourceKey<? extends Registry<T>> registry, String name, Supplier<R> value) {
        return registerFor(registry).register(name, value);
    }

    /** Forge's access transformer makes vanilla's private CriteriaTriggers.register public. */
    @Override
    public <T extends CriterionTrigger<?>> T registerCriterion(T trigger) {
        return CriteriaTriggers.register(trigger);
    }

    static void registerBuses(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        for (DeferredRegister<?> register : REGISTERS.values()) {
            register.register(modEventBus);
        }
    }
}
