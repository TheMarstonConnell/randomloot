package dev.marston.randomloot.fabric;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.commands.ModCommands;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.LootInjection;
import dev.marston.randomloot.loot.modifiers.KillDispatcher;
import dev.marston.randomloot.loot.modifiers.holders.BlockHighlighter;
import dev.marston.randomloot.platform.GameHook;
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
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.fml.config.ModConfig;

import static net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem;

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
        ForgeConfigRegistry.INSTANCE.register(RandomLoot.MODID, ModConfig.Type.COMMON, Config.SPEC);
        ModConfigEvents.loading(RandomLoot.MODID).register(config -> Config.onLoad());
        // Both registrations matter: dropping .reloading silently breaks /reload-time
        // config refresh, which Forge gets from the single ModConfigEvent.
        ModConfigEvents.reloading(RandomLoot.MODID).register(config -> Config.onLoad());
        GameHooks.bind(GameHook.CONFIG);

        registerEvents();
    }

    private static void registerEvents() {
        ServerLivingEntityEvents.AFTER_DEATH.register(KillDispatcher::onLivingDeath);
        GameHooks.bind(GameHook.KILL);

        // Wired by mixin rather than a Fabric API callback - see randomloot.fabric.mixins.json.
        // LivingEntityMixin (@WrapOperation on the actuallyHurt calls inside hurt: the
        // pre-damage trait hook before, the armor XP/durability hook after - Fabric API
        // for 1.20.1 has no AFTER_DAMAGE event), PlayerMixin (getDestroySpeed),
        // AnvilMenuMixin, SmithingMenuMixin (onTake).
        GameHooks.bind(GameHook.DAMAGE_PRE);
        GameHooks.bind(GameHook.DAMAGE_POST);
        // No hook to wire: vanilla only damages ArmorItem instances, so
        // ArmorDispatcher.onLivingDamagePost spends armor durability (and rolls
        // Unbreaking through onArmorHurt) itself.
        GameHooks.bind(GameHook.ARMOR_HURT);
        GameHooks.bind(GameHook.BREAK_SPEED);
        GameHooks.bind(GameHook.ANVIL_COMBINE);
        GameHooks.bind(GameHook.SMITHING_TAKE);

        // ServerPlayerGameModeMixin (useItem and useItemOn RETURN): off-hand arbitration
        // for right-click traits.
        GameHooks.bind(GameHook.AFTER_USE_ITEM);

        ServerTickEvents.END_SERVER_TICK.register(server -> BlockHighlighter.onServerTick());
        GameHooks.bind(GameHook.SERVER_TICK);

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> BlockHighlighter.onServerStopping());
        GameHooks.bind(GameHook.SERVER_STOPPING);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                ModCommands.register(dispatcher));
        GameHooks.bind(GameHook.COMMANDS);

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
            for (Item item : ModItems.creativeTabItems()) {
                output.accept(item);
            }
        });
        GameHooks.bind(GameHook.CREATIVE_TAB);

        // Per-type/per-piece enchantment gating, by mixin: EnchantmentHelperMixin (the
        // table's candidate list) and AnvilMenuMixin (books). Fabric API for 1.20.1 has
        // no enchanting event, and vanilla's EnchantmentCategory instanceof checks
        // refuse the single gear item outright.
        GameHooks.bind(GameHook.ENCHANT_GATE);

        // Loot injection: the LootInjection policy is common; pools are Fabric's
        // delivery mechanism (Forge uses the case_item global loot modifier). Every
        // matching table gets the pools, including tables that are only ever rolled
        // from inside another matching table (a datapack chest table that rolls other
        // chest tables), so each pool is gated on OncePerLootRoll: nested tables share
        // the parent's LootContext, and only the first pool to reach it rolls. Without
        // that gate such chests paid out several times Forge's rate.
        // (26.x appends to the finished drops through LootTableEvents.MODIFY_DROPS
        // instead; Fabric API for 1.20.1 has no such event.)
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            if (!LootInjection.matchesTable(id.getPath())) {
                return;
            }

            for (LootInjection.Entry entry : LootInjection.entries()) {
                tableBuilder.withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0f))
                        // Order matters: the gate goes first so a context's one roll is
                        // spent whether or not the chance then comes up.
                        .when(() -> new OncePerLootRoll(entry.item()))
                        .when(LootItemRandomChanceCondition.randomChance((float) entry.chance()))
                        .add(lootTableItem(entry.item())));
            }
        });
        GameHooks.bind(GameHook.LOOT_INJECTION);
    }
}
