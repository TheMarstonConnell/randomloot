package dev.marston.randomloot.forge;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.commands.ModCommands;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.ArmorDispatcher;
import dev.marston.randomloot.loot.modifiers.KillDispatcher;
import dev.marston.randomloot.loot.modifiers.holders.BlockHighlighter;
import dev.marston.randomloot.loot.modifiers.hurter.Soulbound;
import dev.marston.randomloot.platform.GameHook;
import dev.marston.randomloot.platform.GameHooks;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.TickEvent;

/** Routes Forge game-bus events to the loader-neutral dispatchers. */
@Mod.EventBusSubscriber(modid = RandomLoot.MODID)
public final class ForgeEvents {

    private ForgeEvents() {
    }

    /**
     * Declares the hooks routed below. @EventBusSubscriber wires them by annotation, so
     * there is nothing to call at registration time - this is the checked inventory.
     * Keep it in step with the handlers; loader_hooks_all_bound fails if a GameHook is
     * never bound.
     */
    public static void bindHooks() {
        GameHooks.bind(GameHook.KILL);
        GameHooks.bind(GameHook.DAMAGE_PRE);
        GameHooks.bind(GameHook.DAMAGE_POST);
        // No event: vanilla only damages ArmorItem instances, so
        // ArmorDispatcher.onLivingDamagePost spends armor durability (and rolls
        // Unbreaking through onArmorHurt) itself.
        GameHooks.bind(GameHook.ARMOR_HURT);
        GameHooks.bind(GameHook.BREAK_SPEED);
        // Wired by mixin, not an event: mixin/ServerPlayerGameModeMixin (useItem and
        // useItemOn RETURN). No Forge event sees the result of a use.
        GameHooks.bind(GameHook.AFTER_USE_ITEM);
        GameHooks.bind(GameHook.SERVER_TICK);
        GameHooks.bind(GameHook.SERVER_STOPPING);
        GameHooks.bind(GameHook.COMMANDS);
        GameHooks.bind(GameHook.CONFIG);
        GameHooks.bind(GameHook.CREATIVE_TAB);
        GameHooks.bind(GameHook.ANVIL_COMBINE);
        // Table: canApplyAtEnchantingTable on ForgeLootItem / ForgeLootArmorItem.
        // Anvil: mixin/AnvilMenuMixin (vanilla asks Enchantment.canEnchant there).
        GameHooks.bind(GameHook.ENCHANT_GATE);
        // The case_item global loot modifier; see data/randomloot/loot_modifiers/.
        GameHooks.bind(GameHook.LOOT_INJECTION);
        // Wired by mixin: mixin/SmithingMenuMixin (onTake HEAD). Forge 1.20.1 has no
        // ItemSmithedEvent (26.x routes this through it), so both loaders carry the mixin.
        GameHooks.bind(GameHook.SMITHING_TAKE);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        KillDispatcher.onLivingDeath(event.getEntity(), event.getSource());
    }

    /**
     * Forge 1.20.1 has one LivingDamageEvent, fired after armor and absorption and
     * right before health is subtracted (NeoForge 21.1 split it into Pre and Post).
     * The pre hook runs at normal priority and may change the amount; the post hook
     * runs last and reads whatever amount survived every other listener.
     */
    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent event) {
        event.setAmount(ArmorDispatcher.onLivingDamagePre(event.getEntity(), event.getSource(), event.getAmount()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamagePost(LivingDamageEvent event) {
        ArmorDispatcher.onLivingDamagePost(event.getEntity(), event.getSource(), event.getAmount());
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        event.setNewSpeed(Soulbound.modifyBreakSpeed(event.getEntity(), event.getNewSpeed()));
    }

    /**
     * Blocks anvil-combining two Random Loot items entirely. isCombineRepairable=false
     * only stops the durability-merge branch; the enchant-transfer branch would still
     * produce a result and destroy the right item's identity. Fabric mirrors this in
     * AnvilMenuMixin.
     */
    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        if (LootUtils.isLootGear(event.getLeft()) && LootUtils.isLootGear(event.getRight())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            BlockHighlighter.onServerTick();
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        BlockHighlighter.onServerStopping();
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }

}
