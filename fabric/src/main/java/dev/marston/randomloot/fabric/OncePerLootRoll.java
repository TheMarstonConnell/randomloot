package dev.marston.randomloot.fabric;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Gate on the loot pools Fabric injects: passes for the first pool carrying
 * {@code item} that a {@link LootContext} reaches and fails for every later one.
 *
 * <p>A table that rolls other tables hands them its own context, and every matching
 * table carries the injected pools, so without this one chest (or vault) rolled once
 * per nested table. NeoForge needs no such gate: its global loot modifier only runs on
 * the outermost table.
 *
 * <p>Registered as a condition type so a table carrying it can still be encoded (mods
 * that dump or sync loot tables do that).
 */
public record OncePerLootRoll(Item item) implements LootItemCondition {

    public static final MapCodec<OncePerLootRoll> CODEC = BuiltInRegistries.ITEM.byNameCodec().fieldOf("item")
            .xmap(OncePerLootRoll::new, OncePerLootRoll::item);

    public static final LootItemConditionType TYPE = new LootItemConditionType(CODEC);

    /** Items already rolled for, by context. Weak keys: a context is garbage once its generation finishes. */
    private static final Map<LootContext, Set<Item>> ROLLED = new WeakHashMap<>();

    @Override
    public LootItemConditionType getType() {
        return TYPE;
    }

    @Override
    public boolean test(LootContext context) {
        // Loot can be generated off the server thread (worldgen), hence the lock.
        synchronized (ROLLED) {
            return ROLLED.computeIfAbsent(context, c -> new HashSet<>(2)).add(this.item);
        }
    }
}
