package dev.marston.randomloot.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

public class CaseLootModifier extends LootModifier {
    private final Item item;

    /**
     * Items covered by a loaded case_item modifier. Fabric enumerates
     * LootInjection.entries() directly, so it picks up a new injected item for free;
     * Forge needs a hand-written JSON in data/randomloot/loot_modifiers/, and adding
     * one without the other silently ships the item on Fabric only. Recorded here so
     * loot_modifiers_load can check the two agree. Refilled on every datapack load.
     */
    public static final java.util.Set<Item> LOADED_ITEMS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    // See below for how the codec works.
    public static final Codec<CaseLootModifier> CODEC = RecordCodecBuilder.create(inst ->
            LootModifier.codecStart(inst).and(
                    BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(e -> e.item)
            ).apply(inst, CaseLootModifier::new)
    );

    // First constructor parameter is the list of conditions. The rest is our extra properties.
    public CaseLootModifier(LootItemCondition[] conditions, Item itemIn) {
        super(conditions);
        this.item = itemIn;
        LOADED_ITEMS.add(itemIn);
    }

    // Return our codec here.
    @Override
    public @NotNull Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }


    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot,
                                                          LootContext context) {

        // Table matching and per-item chances are the common LootInjection policy;
        // this modifier is just Forge's delivery mechanism for it.
        if (!LootInjection.matchesTable(context.getQueriedLootTableId().getPath())) {
            return generatedLoot;
        }

        double chance = LootInjection.chanceFor(item);
        if (chance < 0) {
            return generatedLoot;
        }

        if (context.getRandom().nextDouble() < chance) {
            generatedLoot.add(new ItemStack(item));
        }

        return generatedLoot;
    }
}