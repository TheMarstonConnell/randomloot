package dev.marston.randomloot.fabric;

import dev.marston.randomloot.loot.LootGearItem;
import dev.marston.randomloot.loot.LootItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** LootItem with Fabric API's item extension hooks (update animation, stack-aware correct-tool check). */
public class FabricLootItem extends LootItem {

    public FabricLootItem(Properties p) {
        super(p);
    }

    /** The mod's NBT changes on nearly every use (XP); it alone should not replay the re-equip animation. */
    @Override
    public boolean allowNbtUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack,
            ItemStack newStack) {
        return !LootGearItem.sameApartFromToolMod(oldStack, newStack);
    }

    /** Fabric API routes ItemStack.isCorrectToolForDrops through this stack-aware hook. */
    @Override
    public boolean isSuitableFor(ItemStack stack, BlockState state) {
        return isCorrectToolForDrops(stack, state);
    }
}
