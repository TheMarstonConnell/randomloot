package dev.marston.randomloot.fabric;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.platform.services.RegHelper;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.advancement.CriterionRegistry;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import java.util.function.Function;
import java.util.function.Supplier;

/** Fabric registration is immediate: registries are open during mod init. */
public class FabricRegHelper implements RegHelper {

    @Override
    public Supplier<Item> registerItem(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                new ResourceLocation(RandomLoot.MODID, name));
        // FabricItemSettings so loader item factories can reach Fabric-only settings
        // (equipmentSlot); 1.20.1 does not inject those into Item.Properties itself.
        Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new FabricItemSettings()));
        return () -> item;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, R extends T> Supplier<R> register(ResourceKey<? extends Registry<T>> registry, String name, Supplier<R> value) {
        Registry<T> target = (Registry<T>) BuiltInRegistries.REGISTRY.get(registry.location());
        if (target == null) {
            throw new IllegalArgumentException("Unknown registry " + registry);
        }
        R registered = Registry.register(target, new ResourceLocation(RandomLoot.MODID, name), value.get());
        return () -> registered;
    }

    /** Vanilla's CriteriaTriggers.register is private on 1.20.1; Fabric API fronts it. */
    @Override
    public <T extends CriterionTrigger<?>> T registerCriterion(T trigger) {
        return CriterionRegistry.register(trigger);
    }
}
