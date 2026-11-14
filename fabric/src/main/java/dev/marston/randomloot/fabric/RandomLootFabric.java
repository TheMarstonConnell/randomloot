package dev.marston.randomloot.fabric;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.platform.GameHooks;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.v5.ModConfigEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.EnchantmentEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.util.TriState;
import net.neoforged.fml.config.ModConfig;

/**
 * The Fabric mod entrypoint and, in {@link #registerHooks}, most of the Fabric shim for
 * the loader hook contract: each listener unpacks one Fabric API event and makes one call
 * to {@link GameHooks}, whose javadoc says what that hook has to be handed.
 *
 * <p>Fabric API has no event for six hooks; those are mixins, listed in
 * {@code randomloot.fabric.mixins.json}: DAMAGE_PRE ({@code LivingEntityMixin} and
 * {@code PlayerMixin}), ARMOR_HURT ({@code ItemStackMixin}), BREAK_SPEED
 * ({@code PlayerMixin}), AFTER_USE_ITEM ({@code ServerPlayerGameModeMixin}),
 * ANVIL_COMBINE ({@code AnvilMenuMixin}) and SMITHING_TAKE ({@code SmithingMenuMixin}).
 */
public class RandomLootFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        // Common registration (items, components, criteria, recipe serializers).
        RandomLoot.init();

        // Dispenser behavior; mod init is single-threaded on Fabric so no deferral needed.
        RandomLoot.commonSetup();

        // NeoForge-style TOML config via Forge Config API Port; same file name on both loaders.
        ConfigRegistry.INSTANCE.register(RandomLoot.MODID, ModConfig.Type.COMMON, Config.SPEC);

        registerHooks();

        // Shared gametests from the common table; does nothing outside a gametest run or dev environment.
        RandomLootFabricGameTests.registerShared();
    }

    private static void registerHooks() {
        // Both registrations matter: dropping .reloading silently breaks /reload-time
        // config refresh, which NeoForge gets from the single ModConfigEvent.
        ModConfigEvents.loading(RandomLoot.MODID).register(config -> GameHooks.config());
        ModConfigEvents.reloading(RandomLoot.MODID).register(config -> GameHooks.config());

        ServerLivingEntityEvents.AFTER_DEATH.register(GameHooks::kill);

        // Not yet a plain translation: this drops any hit a shield blocked part of, and
        // AFTER_DAMAGE itself hands over the damage before armor and skips killing hits.
        // NeoForge's shim does none of that. Which is meant is open; see GameHooks.damagePost.
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (!blocked) {
                GameHooks.damagePost(entity, damageTaken);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> GameHooks.serverTick());

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> GameHooks.serverStopping());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                GameHooks.commands(dispatcher));

        CreativeModeTabEvents.modifyOutputEvent(ModItems.CREATIVE_TAB).register(output ->
                GameHooks.creativeTab(ModItems.CREATIVE_TAB, output::accept));

        // NeoForge's shim is the supportsEnchantment override on its two item classes.
        EnchantmentEvents.ALLOW_ENCHANTING.register((enchantment, target, context) ->
                GameHooks.enchantGate(target, enchantment, TriState.TRUE, TriState.FALSE, () -> TriState.DEFAULT));

        // Fired for every table as it rolls, nested ones included; a table defined inline
        // has no id and is skipped. NeoForge's shim is the case_item global loot modifier.
        LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> table.unwrapKey().ifPresent(key ->
                drops.addAll(GameHooks.lootInjection(key.identifier(), context))));
    }
}
