package dev.marston.randomloot.fabric;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.platform.GameHooks;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import fuzs.forgeconfigapiport.api.config.v2.ModConfigEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.fml.config.ModConfig;

import static net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem;

/**
 * The Fabric mod entrypoint and, in {@link #registerHooks}, most of the Fabric shim for
 * the loader hook contract: each listener unpacks one Fabric API event and makes one call
 * to {@link GameHooks}, whose javadoc says what that hook has to be handed.
 *
 * <p>Fabric API has no event, or none at the right stage, for seven hooks; those are
 * mixins, listed in {@code randomloot.fabric.mixins.json}: DAMAGE_PRE and DAMAGE_POST
 * ({@code LivingEntityMixin} and {@code PlayerMixin}), BREAK_SPEED ({@code PlayerMixin}),
 * AFTER_USE_ITEM ({@code ServerPlayerGameModeMixin}), ANVIL_COMBINE
 * ({@code AnvilMenuMixin}), ENCHANT_GATE ({@code EnchantmentHelperMixin} for the table,
 * {@code AnvilMenuMixin} for books) and SMITHING_TAKE ({@code SmithingMenuMixin}).
 */
public class RandomLootFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        // Common registration (items, components, criteria, recipe serializers).
        RandomLoot.init();

        // Dispenser behavior; mod init is single-threaded on Fabric so no deferral needed.
        RandomLoot.commonSetup();

        // The condition the injected loot pools carry; see the loot injection below.
        Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE,
                new ResourceLocation(RandomLoot.MODID, "once_per_loot_roll"), OncePerLootRoll.TYPE);

        // Forge-style TOML config via Forge Config API Port; same file name on both loaders.
        // The hooks go first: Forge Config API Port loads the file inside register(), so a
        // loading listener added after it never hears the first load, and the mod then runs
        // on its defaults whatever the file says.
        registerHooks();
        ForgeConfigRegistry.INSTANCE.register(RandomLoot.MODID, ModConfig.Type.COMMON, Config.SPEC);
    }

    private static void registerHooks() {
        // Both registrations matter: dropping .reloading silently breaks /reload-time
        // config refresh, which Forge gets from the single ModConfigEvent.
        ModConfigEvents.loading(RandomLoot.MODID).register(config -> GameHooks.config());
        ModConfigEvents.reloading(RandomLoot.MODID).register(config -> GameHooks.config());

        ServerLivingEntityEvents.AFTER_DEATH.register(GameHooks::kill);

        ServerTickEvents.END_SERVER_TICK.register(server -> GameHooks.serverTick());

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> GameHooks.serverStopping());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                GameHooks.commands(dispatcher));

        ItemGroupEvents.modifyEntriesEvent(ModItems.CREATIVE_TAB).register(output ->
                GameHooks.creativeTab(ModItems.CREATIVE_TAB, output::accept));

        // Fabric API for 1.20.1 has no LootTableEvents.MODIFY_DROPS (26.x appends to the
        // finished drops through it), so the injected items are pools added to each table
        // as it loads. Every matching table gets them, including tables only ever rolled
        // from inside another matching table (a datapack chest table that rolls other
        // chest tables), so each pool is gated on OncePerLootRoll: nested tables share
        // the parent's LootContext, and only the first pool to reach it rolls. The chance
        // is asked for at that roll, not baked into the pool, so the config as it stands
        // then decides, as on Forge. Forge's shim is the case_item global loot modifier.
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            for (Item item : GameHooks.lootInjectionPools(id)) {
                tableBuilder.withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0f))
                        .when(() -> new OncePerLootRoll(item))
                        .add(lootTableItem(item)));
            }
        });
    }
}
