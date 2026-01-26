package dev.marston.randomloot.loot.modifiers.breakers;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.LootNBT;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

@Mod.EventBusSubscriber(modid = RandomLoot.MODID)
public class MeltingHandler {

    @SubscribeEvent
    public static void onHarvestDrops(BlockEvent.HarvestDropsEvent event) {
        EntityPlayer player = event.getHarvester();
        if (player == null) {
            return;
        }

        ItemStack heldItem = player.getHeldItemMainhand();
        if (heldItem.isEmpty() || heldItem.getItem() != ModItems.TOOL) {
            return;
        }

        // Check if tool has Melting modifier
        List<Modifier> mods = LootNBT.getModifiers(heldItem);
        boolean hasMelting = false;
        for (Modifier mod : mods) {
            if (mod instanceof Melting && Config.traitEnabled(mod.tagName())) {
                hasMelting = true;
                break;
            }
        }

        if (hasMelting) {
            Melting.processDrops(event.getDrops());
        }
    }
}
