package dev.marston.randomloot.neoforge;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.platform.ToolAction;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ItemAbility;

/** LootItem with NeoForge's item-ability hook, so getToolModifiedState and other mods' ability checks work. */
public class NeoForgeLootItem extends LootItem {

    public NeoForgeLootItem(Properties p) {
        super(p);
    }

    @Override
    public boolean canPerformAction(ItemInstance stack, ItemAbility itemAbility) {
        if (!(stack instanceof ItemStack itemStack)) {
            return false;
        }

        for (ToolAction action : ToolAction.values()) {
            if (NeoForgePlatformHelper.toItemAbility(action) == itemAbility) {
                return canPerform(itemStack, action);
            }
        }
        return false;
    }
}
