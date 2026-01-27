package dev.marston.randomloot.migration;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.items.LegacyItem;
import dev.marston.randomloot.items.ModItems;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Handles migration of legacy items from the old 1.12 Random Loot mod.
 * When a player holds a legacy item in their hand, it is converted to a case.
 */
@Mod.EventBusSubscriber(modid = RandomLoot.MODID)
public class LegacyMigrationHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        EntityPlayer player = event.player;
        if (player.world.isRemote) {
            return;
        }

        // Check main hand
        ItemStack mainHand = player.getHeldItemMainhand();
        if (mainHand.getItem() instanceof LegacyItem) {
            LegacyItem legacy = (LegacyItem) mainHand.getItem();
            convertLegacyItem(player, EnumHand.MAIN_HAND, legacy.getLegacyType());
        }

        // Check off hand
        ItemStack offHand = player.getHeldItemOffhand();
        if (offHand.getItem() instanceof LegacyItem) {
            LegacyItem legacy = (LegacyItem) offHand.getItem();
            convertLegacyItem(player, EnumHand.OFF_HAND, legacy.getLegacyType());
        }
    }

    private static void convertLegacyItem(EntityPlayer player, EnumHand hand, String type) {
        ItemStack newCase = new ItemStack(ModItems.CASE, 1);
        player.setHeldItem(hand, newCase);

        String itemName = formatLegacyName(type);
        String message = TextFormatting.GOLD + "Your legacy " + itemName + 
            " from an older version of Random Loot has been converted to a case. Thank you for playing!";
        player.sendMessage(new TextComponentString(message));

        RandomLoot.LOGGER.info("Converted legacy {} for player {}", type, player.getName());
    }

    private static String formatLegacyName(String type) {
        // Convert "heavy_boots" -> "Heavy Boots"
        String[] words = type.split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(Character.toUpperCase(word.charAt(0)));
            result.append(word.substring(1));
        }
        return result.toString();
    }
}
