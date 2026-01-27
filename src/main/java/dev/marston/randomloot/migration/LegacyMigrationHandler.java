package dev.marston.randomloot.migration;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.items.LegacyItem;
import dev.marston.randomloot.items.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Handles migration of legacy items from the 1.12 version of Random Loot.
 * When a player holds a legacy item in their hand, it is converted to a case.
 */
@EventBusSubscriber(modid = RandomLoot.MODID)
public class LegacyMigrationHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        // Check main hand
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof LegacyItem legacy) {
            convertLegacyItem(player, InteractionHand.MAIN_HAND, legacy.getLegacyType());
        }

        // Check off hand
        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof LegacyItem legacy) {
            convertLegacyItem(player, InteractionHand.OFF_HAND, legacy.getLegacyType());
        }
    }

    private static void convertLegacyItem(Player player, InteractionHand hand, String type) {
        ItemStack newCase = new ItemStack(ModItems.CASE.get(), 1);
        player.setItemInHand(hand, newCase);

        String itemName = formatLegacyName(type);
        player.displayClientMessage(
            Component.literal("Your legacy " + itemName + " from an older version of Random Loot has been converted to a case. Thank you for playing!")
                .withStyle(ChatFormatting.GOLD),
            false
        );

        RandomLoot.LOGGER.info("Converted legacy {} for player {}", type, player.getName().getString());
    }

    private static String formatLegacyName(String type) {
        // Convert "heavy_boots" -> "Heavy Boots"
        return Arrays.stream(type.split("_"))
            .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
            .collect(Collectors.joining(" "));
    }
}
