package dev.marston.randomloot.loot;

import com.google.common.collect.Multimap;
import dev.marston.randomloot.Config;
import dev.marston.randomloot.loot.modifiers.*;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

public class LootItem extends Item {

    public enum ToolType {
        PICKAXE, SHOVEL, AXE, SWORD, NULL;

        @Override
        public String toString() {
            switch (this) {
                case PICKAXE: return "Pickaxe";
                case SHOVEL: return "Shovel";
                case AXE: return "Axe";
                case SWORD: return "Sword";
                default: return "Null";
            }
        }
    }

    public LootItem() {
        super();
        this.setMaxStackSize(1);
        this.setMaxDamage(100); // Will be overridden by getMaxDamage()
    }

    public static float getDigSpeed(ItemStack stack, ToolType type) {
        float statMod = 1.0f;

        List<Modifier> mods = LootNBT.getModifiers(stack);
        for (Modifier mod : mods) {
            if (mod instanceof StatsModifier) {
                if (!Config.traitEnabled(mod.tagName())) {
                    continue;
                }
                StatsModifier sm = (StatsModifier) mod;
                statMod *= sm.getStats(stack);
            }
        }

        if (type.equals(ToolType.SWORD)) {
            return 1.0f;
        }

        float speed = (LootUtils.getStats(stack) / 2.0f) + 6.0f;
        return speed * statMod;
    }

    public static float getAttackSpeed(ItemStack stack, ToolType type) {
        float speed = 0.0f;

        switch (type) {
            case PICKAXE:
                speed = -2.8F;
                break;
            case AXE:
                speed = -3.0F;
                break;
            case SHOVEL:
                speed = -3.0F;
                break;
            case SWORD:
                speed = -2.4F;
                break;
            default:
                break;
        }

        return speed;
    }

    public static float getAttackDamage(ItemStack stack, ToolType type) {
        float damage = (LootUtils.getStats(stack)) + 1.0f;

        switch (type) {
            case PICKAXE:
                damage = damage * 0.5f;
                break;
            case AXE:
                damage = damage * 1.2f;
                break;
            case SHOVEL:
                damage = damage * 0.6f;
                break;
            default:
                break;
        }

        return damage;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, IBlockState state) {
        ToolType type = LootUtils.getToolType(stack);
        Material material = state.getMaterial();
        Block block = state.getBlock();

        if (type == ToolType.PICKAXE) {
            if (material == Material.ROCK || material == Material.IRON || material == Material.ANVIL) {
                return getDigSpeed(stack, type);
            }
        } else if (type == ToolType.AXE) {
            if (material == Material.WOOD || material == Material.PLANTS || material == Material.VINE) {
                return getDigSpeed(stack, type);
            }
        } else if (type == ToolType.SHOVEL) {
            if (material == Material.GROUND || material == Material.GRASS || 
                material == Material.SAND || material == Material.SNOW || 
                material == Material.CRAFTED_SNOW || material == Material.CLAY) {
                return getDigSpeed(stack, type);
            }
        } else if (type == ToolType.SWORD) {
            // Cobwebs - instant break
            if (block == Blocks.WEB) {
                return 15.0f;
            }
            // Leaves - faster
            if (material == Material.LEAVES) {
                return 1.5f;
            }
            return 1.0f;
        }

        return 1.0f;
    }

    @Override
    public boolean canHarvestBlock(IBlockState state) {
        // Swords can harvest cobwebs
        Block block = state.getBlock();
        return block == Blocks.WEB;
    }

    @Override
    public boolean canHarvestBlock(IBlockState state, ItemStack stack) {
        ToolType type = LootUtils.getToolType(stack);
        Material material = state.getMaterial();
        Block block = state.getBlock();

        if (type == ToolType.PICKAXE) {
            return material == Material.ROCK || material == Material.IRON || material == Material.ANVIL;
        } else if (type == ToolType.SHOVEL) {
            return material == Material.GROUND || material == Material.GRASS || 
                   material == Material.SAND || material == Material.SNOW || 
                   material == Material.CRAFTED_SNOW || material == Material.CLAY;
        } else if (type == ToolType.AXE) {
            return material == Material.WOOD || material == Material.PLANTS || material == Material.VINE;
        } else if (type == ToolType.SWORD) {
            return block == Blocks.WEB;
        }

        return false;
    }

    @Override
    public int getHarvestLevel(ItemStack stack, String toolClass, @Nullable EntityPlayer player, @Nullable IBlockState blockState) {
        ToolType type = LootUtils.getToolType(stack);
        
        // Return diamond-level harvest level for matching tool types
        if (type == ToolType.PICKAXE && toolClass.equals("pickaxe")) {
            return 3; // Diamond level
        }
        if (type == ToolType.AXE && toolClass.equals("axe")) {
            return 3;
        }
        if (type == ToolType.SHOVEL && toolClass.equals("shovel")) {
            return 3;
        }
        
        return -1;
    }

    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {
        Multimap<String, AttributeModifier> multimap = super.getAttributeModifiers(slot, stack);

        if (slot == EntityEquipmentSlot.MAINHAND) {
            ToolType tt = LootUtils.getToolType(stack);
            float attack = getAttackDamage(stack, tt);
            float speed = getAttackSpeed(stack, tt);

            multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                    new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", attack, 0));
            multimap.put(SharedMonsterAttributes.ATTACK_SPEED.getName(),
                    new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", speed, 0));
        }

        return multimap;
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        ToolType type = LootUtils.getToolType(stack);

        if (type == ToolType.AXE || type == ToolType.SWORD) {
            LootUtils.addXp(stack, attacker, 1);
        }

        List<Modifier> mods = LootNBT.getModifiers(stack);

        boolean shouldSkipBreak = false;
        for (Modifier mod : mods) {
            if (mod instanceof EntityHurtModifier) {
                if (!Config.traitEnabled(mod.tagName())) {
                    continue;
                }
                EntityHurtModifier ehm = (EntityHurtModifier) mod;
                if (ehm.hurtEnemy(stack, target, attacker)) {
                    shouldSkipBreak = true;
                }
            }

            if (mod instanceof Unbreaking) {
                if (!Config.traitEnabled(mod.tagName())) {
                    continue;
                }
                Unbreaking unbreaking = (Unbreaking) mod;
                if (unbreaking.test(target.world)) {
                    shouldSkipBreak = true;
                }
            }
        }

        if (!shouldSkipBreak) {
            stack.damageItem(1, attacker);
        }

        return true;
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World world, IBlockState state, BlockPos pos, EntityLivingBase player) {
        if (!world.isRemote && state.getBlockHardness(world, pos) != 0.0F) {
            List<Modifier> mods = LootNBT.getModifiers(stack);

            boolean shouldSkipBreak = false;
            for (Modifier mod : mods) {
                if (mod instanceof BlockBreakModifier) {
                    if (!Config.traitEnabled(mod.tagName())) {
                        continue;
                    }
                    BlockBreakModifier bbm = (BlockBreakModifier) mod;
                    if (bbm.startBreak(stack, pos, player)) {
                        shouldSkipBreak = true;
                    }
                }

                if (mod instanceof Unbreaking) {
                    if (!Config.traitEnabled(mod.tagName())) {
                        continue;
                    }
                    Unbreaking unbreaking = (Unbreaking) mod;
                    if (unbreaking.test(world)) {
                        shouldSkipBreak = true;
                    }
                }
            }

            if (!shouldSkipBreak) {
                stack.damageItem(1, player);
            }

            LootUtils.addXp(stack, player, 1);
        }

        return true;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        float stats = (LootUtils.getStats(stack) + 10.0f) * 80.0f;
        return (int) stats;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, 
                                       EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        List<Modifier> mods = LootNBT.getModifiers(stack);

        // Try UseModifier abilities
        for (Modifier mod : mods) {
            if (mod instanceof UseModifier) {
                if (!Config.traitEnabled(mod.tagName())) {
                    continue;
                }
                UseModifier um = (UseModifier) mod;
                EnumActionResult result = um.use(player, world, pos, hand, facing, hitX, hitY, hitZ);
                if (result != EnumActionResult.PASS) {
                    return result;
                }
            }
        }

        // TODO: Add vanilla axe stripping and shovel path making for 1.12.2
        // These features were added in later versions

        return EnumActionResult.PASS;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        List<Modifier> mods = LootNBT.getModifiers(stack);

        boolean used = false;
        for (Modifier mod : mods) {
            if (mod instanceof UseModifier) {
                if (!Config.traitEnabled(mod.tagName())) {
                    continue;
                }
                UseModifier um = (UseModifier) mod;
                if (um.useAnywhere()) {
                    used = used || um.use(world, player, hand);
                }
            }
        }

        if (used) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        return new ActionResult<>(EnumActionResult.PASS, stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        boolean shift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        boolean ctrl = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);

        ToolType tt = LootUtils.getToolType(stack);

        if (shift) {
            tooltip.add(TextFormatting.BLUE + tt.toString());
        }

        String lore = LootNBT.getLore(stack);
        if (!lore.isEmpty()) {
            tooltip.add(TextFormatting.GRAY + lore);
        }

        if (shift) {
            tooltip.add("");
            int itemLevel = LootNBT.getLevel(stack);
            tooltip.add(TextFormatting.GRAY + "Level: " + itemLevel);
            tooltip.add(TextFormatting.GRAY + "XP: " + LootNBT.getXP(stack) + " / " + LootUtils.getMaxXP(itemLevel));
        }

        tooltip.add("");

        List<Modifier> mods = LootNBT.getModifiers(stack);
        mods.sort(Comparator.comparing(Modifier::tagName));

        for (Modifier modifier : mods) {
            if (!Config.traitEnabled(modifier.tagName())) {
                continue;
            }
            modifier.writeToLore(tooltip, shift);
            if (shift) {
                String details = modifier.writeDetailsToLore(world);
                if (details != null) {
                    tooltip.add(TextFormatting.GRAY + " - " + details);
                }
            }
            if (ctrl) {
                tooltip.add(TextFormatting.GRAY + modifier.description());
            }
        }

        if (shift) {
            tooltip.add("");
            float digSpeed = LootItem.getDigSpeed(stack, tt);
            tooltip.add(TextFormatting.GRAY + String.format("Speed: %.2f", digSpeed));
            float attackDamage = LootItem.getAttackDamage(stack, tt);
            tooltip.add(TextFormatting.GRAY + String.format("Damage: %.2f", attackDamage));
        }

        if (!shift && !ctrl) {
            tooltip.add("");
            tooltip.add(TextFormatting.GRAY + "[Shift for more]");
            tooltip.add(TextFormatting.GRAY + "[Ctrl for trait info]");
        }
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean isSelected) {
        if (isSelected && !world.isRemote) {
            List<Modifier> mods = LootNBT.getModifiers(stack);

            for (Modifier mod : mods) {
                if (mod instanceof HoldModifier) {
                    if (!Config.traitEnabled(mod.tagName())) {
                        continue;
                    }
                    HoldModifier holdMod = (HoldModifier) mod;
                    holdMod.hold(stack, world, entity);
                }
            }
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String name = LootNBT.getToolName(stack);
        if (name != null && !name.isEmpty() && !name.equals("Random Tool")) {
            return name;
        }
        return super.getItemStackDisplayName(stack);
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return false;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getItemEnchantability(ItemStack stack) {
        return 15;
    }
}
