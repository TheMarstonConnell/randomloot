package dev.marston.randomloot.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.marston.randomloot.Config;
import dev.marston.randomloot.items.ModItems;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

public class CaseLootModifier extends LootModifier {
    private final Item item;

    // See below for how the codec works.
    public static final MapCodec<CaseLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst).and(
                    BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(e -> e.item)
            ).apply(inst, CaseLootModifier::new)
    );

    // First constructor parameter is the list of conditions. The rest is our extra properties.
    public CaseLootModifier(LootItemCondition[] conditions, int priority, Item itemIn) {
        super(conditions, priority);
        this.item = itemIn;
    }

    // Return our codec here.
    @Override
    public @NotNull MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }


    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot,
                                                          LootContext context) {

        String path = context.getQueriedLootTableId().getPath();

        if (!path.contains("chest")) {
            return generatedLoot;
        }

        double chance;
        if (item == ModItems.CASE.asItem()) {
            chance = Config.CaseChance;
        } else if (item == ModItems.MOD_ADD.asItem()) {
            chance = Config.ModChance;
        } else {
            return generatedLoot;
        }

        if (context.getRandom().nextDouble() < chance) {
            generatedLoot.add(new ItemStack(item));
        }

        return generatedLoot;
    }
}