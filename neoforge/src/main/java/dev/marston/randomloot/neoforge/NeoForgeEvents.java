package dev.marston.randomloot.neoforge;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.commands.ModCommands;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.modifiers.ArmorDispatcher;
import dev.marston.randomloot.loot.modifiers.KillDispatcher;
import dev.marston.randomloot.loot.modifiers.holders.BlockHighlighter;
import dev.marston.randomloot.loot.modifiers.hurter.Soulbound;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
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
        ArmorDispatcher.onLivingDamagePost(event.getEntity(), event.getInflictedDamage());
    }

    @SubscribeEvent
    public static void onArmorHurt(ArmorHurtEvent event) {
        for (Map.Entry<EquipmentSlot, ArmorHurtEvent.ArmorEntry> entry : event.getArmorMap().entrySet()) {
            ArmorHurtEvent.ArmorEntry armorEntry = entry.getValue();
            event.setNewDamage(entry.getKey(),
                    ArmorDispatcher.onArmorHurt(event.getEntity(), armorEntry.armorItemStack, armorEntry.newDamage));
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        event.setNewSpeed(Soulbound.modifyBreakSpeed(event.getEntity(), event.getNewSpeed()));
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

    // Mod-bus events below: @EventBusSubscriber auto-routes by event type in 26.x.

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent event) {
        Config.onLoad();
    }

    @SubscribeEvent
    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            for (Item item : ModItems.creativeTabItems()) {
                event.accept(item);
            }
        }
    }
}
