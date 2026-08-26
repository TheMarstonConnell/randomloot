package dev.marston.randomloot.neoforge;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.commands.ModCommands;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.ArmorDispatcher;
import dev.marston.randomloot.loot.modifiers.KillDispatcher;
import dev.marston.randomloot.loot.modifiers.holders.BlockHighlighter;
import dev.marston.randomloot.loot.modifiers.hurter.Soulbound;
import dev.marston.randomloot.platform.GameHook;
import dev.marston.randomloot.platform.GameHooks;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.ArmorHurtEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Map;

/** Routes NeoForge game-bus events to the loader-neutral dispatchers. */
@EventBusSubscriber(modid = RandomLoot.MODID)
public final class NeoForgeEvents {

    private NeoForgeEvents() {
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
        GameHooks.bind(GameHook.ARMOR_HURT);
        GameHooks.bind(GameHook.BREAK_SPEED);
        GameHooks.bind(GameHook.SERVER_TICK);
        GameHooks.bind(GameHook.SERVER_STOPPING);
        GameHooks.bind(GameHook.COMMANDS);
        GameHooks.bind(GameHook.CONFIG);
        GameHooks.bind(GameHook.CREATIVE_TAB);
        GameHooks.bind(GameHook.ANVIL_COMBINE);
        // Item-extension overrides on NeoForgeLootItem / NeoForgeLootArmorItem.
        GameHooks.bind(GameHook.ENCHANT_GATE);
        // The case_item global loot modifier; see data/randomloot/loot_modifiers/.
        GameHooks.bind(GameHook.LOOT_INJECTION);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        KillDispatcher.onLivingDeath(event.getEntity(), event.getSource());
    }

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        event.setNewDamage(ArmorDispatcher.onLivingDamagePre(event.getEntity(), event.getSource(), event.getNewDamage()));
    }

    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        ArmorDispatcher.onLivingDamagePost(event.getEntity(), event.getSource(), event.getNewDamage());
    }

    @SubscribeEvent
    public static void onArmorHurt(ArmorHurtEvent event) {
        for (Map.Entry<EquipmentSlot, ArmorHurtEvent.ArmorEntry> entry : event.getArmorMap().entrySet()) {
            ArmorHurtEvent.ArmorEntry armorEntry = entry.getValue();
            event.setNewDamage(entry.getKey(), ArmorDispatcher.onArmorHurt(event.getEntity(),
                    armorEntry.armorItemStack, entry.getKey(), armorEntry.newDamage));
        }
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
    public static void onServerTick(ServerTickEvent.Post event) {
        BlockHighlighter.onServerTick();
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
