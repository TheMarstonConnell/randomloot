package dev.marston.randomloot.loot;

import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;

/**
 * NBT storage utility for Random Loot tools.
 * Replaces the DataComponent system from 1.21+
 * 
 * NBT Structure:
 * randomloot (compound)
 *   ├── modifiers (compound) - each modifier's serialized data
 *   ├── XP (compound) - {level, xp}
 *   ├── info (compound) - {type, biomeTemp, biomeKey, dimension, ownerUUID}
 *   ├── itemStats (compound) - {goodness}
 *   ├── cosmetics (compound) - {texture}
 *   └── itemLore (string)
 */
public class LootNBT {
    
    public static final String ROOT_TAG = "randomloot";
    public static final String MODIFIERS_TAG = "modifiers";
    public static final String XP_TAG = "XP";
    public static final String INFO_TAG = "info";
    public static final String STATS_TAG = "itemStats";
    public static final String COSMETICS_TAG = "cosmetics";
    public static final String LORE_TAG = "itemLore";

    // ========== Root Tag Management ==========
    
    public static NBTTagCompound getOrCreateRootTag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound rootTag = stack.getTagCompound();
        if (!rootTag.hasKey(ROOT_TAG)) {
            rootTag.setTag(ROOT_TAG, new NBTTagCompound());
        }
        return rootTag.getCompoundTag(ROOT_TAG);
    }

    public static NBTTagCompound getRootTag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            return new NBTTagCompound();
        }
        return stack.getTagCompound().getCompoundTag(ROOT_TAG);
    }

    public static boolean hasRootTag(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().hasKey(ROOT_TAG);
    }

    // ========== Modifiers ==========

    public static NBTTagCompound getModifiersTag(ItemStack stack) {
        return getRootTag(stack).getCompoundTag(MODIFIERS_TAG);
    }

    public static void setModifiersTag(ItemStack stack, NBTTagCompound modifiers) {
        getOrCreateRootTag(stack).setTag(MODIFIERS_TAG, modifiers);
    }

    public static List<Modifier> getModifiers(ItemStack stack) {
        List<Modifier> modifiers = new ArrayList<>();
        NBTTagCompound modsTag = getModifiersTag(stack);
        
        for (String key : modsTag.getKeySet()) {
            Modifier mod = ModifierRegistry.loadModifier(key, modsTag.getCompoundTag(key));
            if (mod != null) {
                modifiers.add(mod);
            }
        }
        return modifiers;
    }

    public static void addModifier(ItemStack stack, Modifier modifier) {
        NBTTagCompound modsTag = getModifiersTag(stack);
        modsTag.setTag(modifier.tagName(), modifier.toNBT());
        setModifiersTag(stack, modsTag);
    }

    public static void removeModifier(ItemStack stack, String tagName) {
        NBTTagCompound modsTag = getModifiersTag(stack);
        modsTag.removeTag(tagName);
        setModifiersTag(stack, modsTag);
    }

    public static boolean hasModifier(ItemStack stack, String tagName) {
        return getModifiersTag(stack).hasKey(tagName);
    }

    public static void updateModifier(ItemStack stack, Modifier modifier) {
        NBTTagCompound modsTag = getModifiersTag(stack);
        modsTag.setTag(modifier.tagName(), modifier.toNBT());
        setModifiersTag(stack, modsTag);
    }

    // ========== XP/Level ==========

    public static int getLevel(ItemStack stack) {
        return getRootTag(stack).getCompoundTag(XP_TAG).getInteger("level");
    }

    public static void setLevel(ItemStack stack, int level) {
        NBTTagCompound xpTag = getOrCreateRootTag(stack).getCompoundTag(XP_TAG);
        xpTag.setInteger("level", level);
        getOrCreateRootTag(stack).setTag(XP_TAG, xpTag);
    }

    public static int getXP(ItemStack stack) {
        return getRootTag(stack).getCompoundTag(XP_TAG).getInteger("xp");
    }

    public static void setXP(ItemStack stack, int xp) {
        NBTTagCompound xpTag = getOrCreateRootTag(stack).getCompoundTag(XP_TAG);
        xpTag.setInteger("xp", xp);
        getOrCreateRootTag(stack).setTag(XP_TAG, xpTag);
    }

    // ========== Tool Info ==========

    public static NBTTagCompound getInfoTag(ItemStack stack) {
        return getRootTag(stack).getCompoundTag(INFO_TAG);
    }

    public static void setInfoTag(ItemStack stack, NBTTagCompound info) {
        getOrCreateRootTag(stack).setTag(INFO_TAG, info);
    }

    public static int getToolTypeOrdinal(ItemStack stack) {
        return getInfoTag(stack).getInteger("type");
    }

    public static void setToolType(ItemStack stack, int typeOrdinal) {
        NBTTagCompound info = getInfoTag(stack);
        info.setInteger("type", typeOrdinal);
        setInfoTag(stack, info);
    }

    public static String getBiomeKey(ItemStack stack) {
        NBTTagCompound info = getInfoTag(stack);
        return info.hasKey("biomeKey") ? info.getString("biomeKey") : null;
    }

    public static float getBiomeTemp(ItemStack stack) {
        return getInfoTag(stack).getFloat("biomeTemp");
    }

    public static String getDimension(ItemStack stack) {
        NBTTagCompound info = getInfoTag(stack);
        return info.hasKey("dimension") ? info.getString("dimension") : "minecraft:overworld";
    }

    public static void setBiomeData(ItemStack stack, String biomeKey, float temp, String dimension) {
        NBTTagCompound info = getInfoTag(stack);
        info.setString("biomeKey", biomeKey);
        info.setFloat("biomeTemp", temp);
        info.setString("dimension", dimension);
        setInfoTag(stack, info);
    }

    public static String getOwnerUUID(ItemStack stack) {
        NBTTagCompound info = getInfoTag(stack);
        return info.hasKey("ownerUUID") ? info.getString("ownerUUID") : null;
    }

    public static void setOwnerUUID(ItemStack stack, String uuid) {
        NBTTagCompound info = getInfoTag(stack);
        info.setString("ownerUUID", uuid);
        setInfoTag(stack, info);
    }

    // ========== Stats ==========

    public static float getGoodness(ItemStack stack) {
        NBTTagCompound stats = getRootTag(stack).getCompoundTag(STATS_TAG);
        return stats.hasKey("goodness") ? stats.getFloat("goodness") : 0.0f;
    }

    public static void setGoodness(ItemStack stack, float goodness) {
        NBTTagCompound stats = getRootTag(stack).getCompoundTag(STATS_TAG);
        stats.setFloat("goodness", goodness);
        getOrCreateRootTag(stack).setTag(STATS_TAG, stats);
    }

    // ========== Cosmetics ==========

    public static int getTexture(ItemStack stack) {
        return getRootTag(stack).getCompoundTag(COSMETICS_TAG).getInteger("texture");
    }

    public static void setTexture(ItemStack stack, int texture) {
        NBTTagCompound cosmetics = getRootTag(stack).getCompoundTag(COSMETICS_TAG);
        cosmetics.setInteger("texture", texture);
        getOrCreateRootTag(stack).setTag(COSMETICS_TAG, cosmetics);
    }

    // ========== Lore ==========

    public static String getLore(ItemStack stack) {
        NBTTagCompound root = getRootTag(stack);
        return root.hasKey(LORE_TAG) ? root.getString(LORE_TAG) : "";
    }

    public static void setLore(ItemStack stack, String lore) {
        getOrCreateRootTag(stack).setString(LORE_TAG, lore);
    }

    // ========== Display Name ==========

    public static String getToolName(ItemStack stack) {
        NBTTagCompound info = getInfoTag(stack);
        return info.hasKey("name") ? info.getString("name") : "Random Tool";
    }

    public static void setToolName(ItemStack stack, String name) {
        NBTTagCompound info = getInfoTag(stack);
        info.setString("name", name);
        setInfoTag(stack, info);
        // Also set the actual display name
        stack.setStackDisplayName(name);
    }
}
