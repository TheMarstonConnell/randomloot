package dev.marston.randomloot.neoforge;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.platform.GameHooks;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.SmithingMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.ArmorHurtEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Map;

/**
 * The NeoForge shim for the loader hook contract: each handler unpacks one NeoForge
 * event and makes one call to {@link GameHooks}, whose javadoc says what that hook has
 * to be handed. No handler decides anything.
 *
 * <p>Four hooks are not events and are wired elsewhere: AFTER_USE_ITEM by
 * {@code mixin/ServerPlayerGameModeMixin}, ENCHANT_GATE by the {@code supportsEnchantment}
 * overrides on {@link NeoForgeLootItem} and {@link NeoForgeLootArmorItem}, and
 * LOOT_INJECTION by {@code CaseLootModifier} with its two files in
 * {@code data/randomloot/loot_modifiers/}.
 */
@EventBusSubscriber(modid = RandomLoot.MODID)
public final class NeoForgeEvents {

    private NeoForgeEvents() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        GameHooks.kill(event.getEntity(), event.getSource());
    }

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        event.setNewDamage(GameHooks.damagePre(event.getEntity(), event.getSource(), event.getNewDamage()));
    }

    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        GameHooks.damagePost(event.getEntity(), event.getInflictedDamage());
    }

    @SubscribeEvent
    public static void onArmorHurt(ArmorHurtEvent event) {
        for (Map.Entry<EquipmentSlot, ArmorHurtEvent.ArmorEntry> entry : event.getArmorMap().entrySet()) {
            ArmorHurtEvent.ArmorEntry armorEntry = entry.getValue();
            event.setNewDamage(entry.getKey(), GameHooks.armorHurt(event.getEntity(),
                    armorEntry.armorItemStack, entry.getKey(), armorEntry.newDamage));
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        event.setNewSpeed(GameHooks.breakSpeed(event.getEntity(), event.getNewSpeed()));
    }

    /**
     * Cancelling the event empties the result. isCombineRepairable=false on the two item
     * classes is not enough: it only stops the durability merge, and the enchantment
     * transfer would still produce a result. Fabric's shim is AnvilMenuMixin.
     */
    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        if (GameHooks.blocksAnvilCombine(event.getLeft(), event.getRight())) {
            event.setCanceled(true);
        }
    }

    /**
     * Fired once per take, before the inputs shrink, for a click (from onTake) and for a
     * shift-click (from onQuickCraft) alike. The event's javadoc warns against touching
     * the inputs because vanilla is about to decrement them; shrinking the addition
     * further is exactly the point here, and vanilla re-reads the slot before its own
     * shrink. The event carries the player and not the menu, and getResult() is no use:
     * on a shift-click it is a copy of gear that is already in the inventory. Fabric's
     * shim is SmithingMenuMixin.
     */
    @SubscribeEvent
    public static void onItemSmithed(PlayerEvent.ItemSmithedEvent event) {
        if (event.getEntity().containerMenu instanceof SmithingMenu menu) {
            GameHooks.smithingTake(menu, event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        GameHooks.serverTick();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        GameHooks.serverStopping();
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        GameHooks.commands(event.getDispatcher());
    }

    // Mod-bus events below: @EventBusSubscriber auto-routes by event type in 26.x.

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent event) {
        GameHooks.config();
    }

    @SubscribeEvent
    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        GameHooks.creativeTab(event.getTabKey(), event::accept);
    }
}
