package dev.marston.randomloot.forge;

import dev.marston.randomloot.RandomLoot;
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

/**
 * The Forge shim for the loader hook contract: each handler unpacks one Forge event and
 * makes one call to {@link GameHooks}, whose javadoc says what that hook has to be
 * handed. No handler decides anything.
 *
 * <p>The mod-bus events (CONFIG, CREATIVE_TAB) are in {@link ForgeModEvents}: a
 * subscriber class listens on one bus. Four more hooks are not events and are wired
 * elsewhere: AFTER_USE_ITEM by {@code mixin/ServerPlayerGameModeMixin}, SMITHING_TAKE by
 * {@code mixin/SmithingMenuMixin} (Forge 1.20.1 has no ItemSmithedEvent), ENCHANT_GATE by
 * the {@code canApplyAtEnchantingTable} overrides on {@link ForgeLootItem} and
 * {@link ForgeLootArmorItem} (the table) and {@code mixin/AnvilMenuMixin} (books), and
 * LOOT_INJECTION by {@code CaseLootModifier} with its two files in
 * {@code data/randomloot/loot_modifiers/}.
 */
@Mod.EventBusSubscriber(modid = RandomLoot.MODID)
public final class ForgeEvents {

    private ForgeEvents() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        GameHooks.kill(event.getEntity(), event.getSource());
    }

    /**
     * Forge 1.20.1 has one LivingDamageEvent, fired after armor and absorption and
     * right before health is subtracted (NeoForge 21.1 split it into Pre and Post).
     * The pre hook runs at normal priority and may change the amount; the post hook
     * runs last and reads whatever amount survived every other listener.
     */
    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent event) {
        event.setAmount(GameHooks.damagePre(event.getEntity(), event.getSource(), event.getAmount()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamagePost(LivingDamageEvent event) {
        GameHooks.damagePost(event.getEntity(), event.getSource(), event.getAmount());
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        event.setNewSpeed(GameHooks.breakSpeed(event.getEntity(), event.getNewSpeed()));
    }

    /**
     * Cancelling the event empties the result. Fabric's shim is AnvilMenuMixin.
     */
    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        if (GameHooks.blocksAnvilCombine(event.getLeft(), event.getRight())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            GameHooks.serverTick();
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        GameHooks.serverStopping();
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        GameHooks.commands(event.getDispatcher());
    }

}
