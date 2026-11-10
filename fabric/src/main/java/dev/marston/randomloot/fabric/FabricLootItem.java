package dev.marston.randomloot.fabric;

import dev.marston.randomloot.loot.LootGearItem;
import dev.marston.randomloot.loot.LootItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** LootItem with Fabric API's item extension hooks (held-item update animation). */
public class FabricLootItem extends LootItem {

    public FabricLootItem(Properties p) {
        super(p);
    }

    /** tool_mod changes on nearly every use (XP); it alone should not replay the re-equip animation. */
    @Override
    public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack,
            ItemStack newStack) {
        return !LootGearItem.sameApartFromToolMod(oldStack, newStack);
    }
}
