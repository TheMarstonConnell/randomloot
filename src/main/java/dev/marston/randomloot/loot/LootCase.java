package dev.marston.randomloot.loot;

import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.client.util.ITooltipFlag;
import dev.marston.randomloot.items.ModItems;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class LootCase extends Item {

    public LootCase() {
        super();
        this.setMaxStackSize(1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return true; // Enchanted glow effect
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack lootCase = player.getHeldItem(hand);

        // Spawn particles around player
        Modifier.TrackEntityParticle(world, player, EnumParticleTypes.CLOUD);

        if (!world.isRemote && player instanceof EntityPlayerMP) {
            EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
            
            // Increment the case usage stat BEFORE generating (so first case = count 0)
            serverPlayer.addStat(StatList.getObjectUseStats(ModItems.CASE));
            
            LootUtils.generateTool(serverPlayer, world);
        }

        // Consume the case unless in creative mode
        if (!player.capabilities.isCreativeMode) {
            lootCase.shrink(1);
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, lootCase);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.GRAY + "Right-click for loot!");
    }
}
