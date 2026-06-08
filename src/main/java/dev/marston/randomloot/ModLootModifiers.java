package dev.marston.randomloot;

import com.mojang.serialization.MapCodec;
import dev.marston.randomloot.loot.CaseLootModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModLootModifiers {
	public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLOBAL_LOOT_MODIFIER_SERIALIZERS = DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, RandomLoot.MODID);

    public static final Supplier<MapCodec<CaseLootModifier>> CASE_LOOT_MODIFIER =
            GLOBAL_LOOT_MODIFIER_SERIALIZERS.register("case_item", () -> CaseLootModifier.CODEC);

	public static void register(IEventBus bus) {
        GLOBAL_LOOT_MODIFIER_SERIALIZERS.register(bus);
	}
}
