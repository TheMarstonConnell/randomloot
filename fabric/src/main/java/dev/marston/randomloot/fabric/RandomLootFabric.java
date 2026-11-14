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
import dev.marston.randomloot.platform.GameHook;
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
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.neoforged.fml.config.ModConfig;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class RandomLootFabric implements ModInitializer {

    /**
     * Loot contexts that have already had their injection roll. A table that rolls other
     * tables hands them its own context, so this is what keeps one chest (or vault) to
     * one roll. Weak keys: a context is garbage once its generation finishes.
     */
    private static final Set<LootContext> INJECTED_CONTEXTS =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    @Override
    public void onInitialize() {
        // Common registration (items, components, criteria, recipe serializers).
        RandomLoot.init();

        // Dispenser behavior; mod init is single-threaded on Fabric so no deferral needed.
        RandomLoot.commonSetup();

        // NeoForge-style TOML config via Forge Config API Port; same file name on both loaders.
        ConfigRegistry.INSTANCE.register(RandomLoot.MODID, ModConfig.Type.COMMON, Config.SPEC);
        ModConfigEvents.loading(RandomLoot.MODID).register(config -> Config.onLoad());
        // Both registrations matter: dropping .reloading silently breaks /reload-time
        // config refresh, which NeoForge gets from the single ModConfigEvent.
        ModConfigEvents.reloading(RandomLoot.MODID).register(config -> Config.onLoad());
        GameHooks.bind(GameHook.CONFIG);

        registerEvents();
    }

    private static void registerEvents() {
        ServerLivingEntityEvents.AFTER_DEATH.register(KillDispatcher::onLivingDeath);
        GameHooks.bind(GameHook.KILL);

        // Wired by mixin rather than a Fabric API callback - see randomloot.fabric.mixins.json.
        // LivingEntityMixin and PlayerMixin (@WrapOperation on getDamageAfterMagicAbsorb
        // inside each class's actuallyHurt), ItemStackMixin (hurtAndBreak), PlayerMixin
        // (getDestroySpeed), AnvilMenuMixin, SmithingMenuMixin (onTake).
        GameHooks.bind(GameHook.DAMAGE_PRE);
        GameHooks.bind(GameHook.ARMOR_HURT);
        GameHooks.bind(GameHook.BREAK_SPEED);
        GameHooks.bind(GameHook.ANVIL_COMBINE);
        GameHooks.bind(GameHook.SMITHING_TAKE);

        // Armor XP from damage soaked; the pre-damage trait hook lives in LivingEntityMixin and PlayerMixin.
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (!blocked) {
                ArmorDispatcher.onLivingDamagePost(entity, damageTaken);
            }
        });
        GameHooks.bind(GameHook.DAMAGE_POST);
        // ServerPlayerGameModeMixin (useItem RETURN): off-hand arbitration for right-click traits.
        GameHooks.bind(GameHook.AFTER_USE_ITEM);

        ServerTickEvents.END_SERVER_TICK.register(server -> BlockHighlighter.onServerTick());
        GameHooks.bind(GameHook.SERVER_TICK);

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> BlockHighlighter.onServerStopping());
        GameHooks.bind(GameHook.SERVER_STOPPING);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                ModCommands.register(dispatcher));
        GameHooks.bind(GameHook.COMMANDS);

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
            for (Item item : ModItems.creativeTabItems()) {
                output.accept(item);
            }
        });
        GameHooks.bind(GameHook.CREATIVE_TAB);

        // Per-type/per-piece enchantment gating (NeoForge does this via supportsEnchantment overrides).
        EnchantmentEvents.ALLOW_ENCHANTING.register((enchantment, target, context) -> {
            Boolean allowed = LootUtils.gearEnchantGate(target, enchantment);
            return allowed == null ? TriState.DEFAULT : TriState.of(allowed);
        });
        GameHooks.bind(GameHook.ENCHANT_GATE);

        // Loot injection: the LootInjection policy is common; NeoForge delivers it with
        // the case_item global loot modifier, Fabric by appending to a table's finished
        // drops. This used to add a pool to every matching table at load time, but then
        // a table that rolls other matching tables injected once per nested roll - the
        // trial vault's "reward" rolls reward_common/_rare/_unique, so vaults paid out
        // several times NeoForge's rate. MODIFY_DROPS fires for nested tables too, and
        // they share the parent's LootContext, hence one roll per context.
        LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
            var key = table.unwrapKey();
            if (key.isEmpty() || !LootInjection.matchesTable(key.get().identifier().getPath())) {
                return;
            }

            if (!INJECTED_CONTEXTS.add(context)) {
                return;
            }

            for (LootInjection.Entry entry : LootInjection.entries()) {
                if (context.getRandom().nextDouble() < entry.chance()) {
                    drops.add(new ItemStack(entry.item()));
                }
            }
        });
        GameHooks.bind(GameHook.LOOT_INJECTION);
    }
}
