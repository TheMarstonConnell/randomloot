package dev.marston.randomloot.fabric;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.commands.ModCommands;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.LootInjection;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.ArmorDispatcher;
import dev.marston.randomloot.loot.modifiers.KillDispatcher;
import dev.marston.randomloot.loot.modifiers.holders.BlockHighlighter;
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
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.fml.config.ModConfig;

import static net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem;

public class RandomLootFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        // Common registration (items, components, criteria, recipe serializers).
        RandomLoot.init();

        // Dispenser behavior; mod init is single-threaded on Fabric so no deferral needed.
        RandomLoot.commonSetup();

        // NeoForge-style TOML config via Forge Config API Port; same file name on both loaders.
        ConfigRegistry.INSTANCE.register(RandomLoot.MODID, ModConfig.Type.COMMON, Config.SPEC);
        ModConfigEvents.loading(RandomLoot.MODID).register(config -> Config.onLoad());
        ModConfigEvents.reloading(RandomLoot.MODID).register(config -> Config.onLoad());

        registerEvents();
    }

    private static void registerEvents() {
        ServerLivingEntityEvents.AFTER_DEATH.register(KillDispatcher::onLivingDeath);

        // Armor XP from damage soaked; the pre-damage trait hook lives in LivingEntityMixin.
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (!blocked) {
                ArmorDispatcher.onLivingDamagePost(entity, damageTaken);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> BlockHighlighter.onServerTick());
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> BlockHighlighter.onServerStopping());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                ModCommands.register(dispatcher));

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
            for (Item item : ModItems.creativeTabItems()) {
                output.accept(item);
            }
        });

        // Per-type/per-piece enchantment gating (NeoForge does this via supportsEnchantment overrides).
        EnchantmentEvents.ALLOW_ENCHANTING.register((enchantment, target, context) -> {
            Boolean allowed = LootUtils.gearEnchantGate(target, enchantment);
            return allowed == null ? TriState.DEFAULT : TriState.of(allowed);
        });

        // Loot injection: the LootInjection policy is common; pools are Fabric's
        // delivery mechanism (NeoForge uses the case_item global loot modifier).
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (!LootInjection.matchesTable(key.identifier().getPath())) {
                return;
            }

            for (LootInjection.Entry entry : LootInjection.entries()) {
                tableBuilder.withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0f))
                        .when(LootItemRandomChanceCondition.randomChance((float) entry.chance()))
                        .add(lootTableItem(entry.item())));
            }
        });
    }
}
