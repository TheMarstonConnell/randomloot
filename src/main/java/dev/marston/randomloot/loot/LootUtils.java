package dev.marston.randomloot.loot;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.BiomeRestrictedModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class LootUtils {

    private static int PICKAXE_COUNT = 18;
    private static int AXE_COUNT = 14;
    private static int SHOVEL_COUNT = 9;
    private static int SWORD_COUNT = 50;

    public static int getMaxXP(int level) {
        int starting = 500;
        return (int) (starting * Math.pow(2, level));
    }

    public static ItemStack levelUp(ItemStack item, EntityLivingBase holder) {
        float stats = getStats(item);
        stats = stats * 1.1f;
        setStats(item, stats);

        holder.world.playSound(null, holder.posX, holder.posY, holder.posZ, 
                SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.0f);

        return item;
    }

    public static void addXp(ItemStack item, EntityLivingBase holder, int amount) {
        int level = LootNBT.getLevel(item);
        int xp = LootNBT.getXP(item);

        xp += amount;

        int max = getMaxXP(level);

        while (xp >= max) {
            xp = xp - max;
            level++;
            levelUp(item, holder);
        }

        LootNBT.setLevel(item, level);
        LootNBT.setXP(item, xp);
    }

    public static float getStats(ItemStack stack) {
        return LootNBT.getGoodness(stack);
    }

    public static void setStats(ItemStack stack, float goodness) {
        LootNBT.setGoodness(stack, goodness);
    }

    public static ToolType getToolType(ItemStack item) {
        int typeOrdinal = LootNBT.getToolTypeOrdinal(item);
        if (typeOrdinal < 0 || typeOrdinal >= ToolType.values().length) {
            return ToolType.NULL;
        }
        return ToolType.values()[typeOrdinal];
    }

    public static ItemStack setToolType(ItemStack item, ToolType type) {
        LootNBT.setToolType(item, type.ordinal());
        return item;
    }

    public static void addModifier(ItemStack item, Modifier mod) {
        // Check if modifier already exists and can level
        List<Modifier> existing = LootNBT.getModifiers(item);
        for (Modifier m : existing) {
            if (m.tagName().equals(mod.tagName())) {
                if (m.canLevel()) {
                    m.levelUp();
                    LootNBT.updateModifier(item, m);
                }
                return;
            }
        }
        
        // Add new modifier
        LootNBT.addModifier(item, mod.clone());
    }

    public static void updateModifier(ItemStack item, Modifier mod) {
        LootNBT.updateModifier(item, mod);
    }

    public static ItemStack removeModifier(ItemStack item, Modifier mod) {
        LootNBT.removeModifier(item, mod.tagName());
        return item;
    }

    private static void storeBiomeData(ItemStack lootItem, World world, EntityPlayer player) {
        float temp = 0.7f;
        String biomeKey = "unknown";
        String dimension = "minecraft:overworld";

        if (player != null) {
            Biome biome = world.getBiome(player.getPosition());
            temp = biome.getDefaultTemperature();
            
            ResourceLocation biomeRL = biome.getRegistryName();
            if (biomeRL != null) {
                biomeKey = biomeRL.toString();
            }

            // Get dimension
            dimension = "minecraft:" + world.provider.getDimensionType().getName();
        }

        LootNBT.setBiomeData(lootItem, biomeKey, temp, dimension);
        RandomLoot.LOGGER.info("Tool generated in biome: {} with temp: {} in dimension: {}", biomeKey, temp, dimension);
    }

    private static String biomeKeyToReadableName(String biomeKey) {
        if (biomeKey == null || biomeKey.isEmpty() || biomeKey.equals("unknown")) {
            return "an Unknown Biome";
        }

        // Remove namespace (minecraft:desert -> desert)
        String biomeName = biomeKey.contains(":") ? biomeKey.substring(biomeKey.indexOf(":") + 1) : biomeKey;

        // Replace underscores with spaces and capitalize each word
        String[] words = biomeName.split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (result.length() > 0) result.append(" ");
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }

        return "a " + result.toString();
    }

    private static void generateLore(ItemStack lootItem, World world, EntityPlayer player) {
        float temp = LootNBT.getBiomeTemp(lootItem);

        String name = "a machine";
        if (player != null) {
            name = player.getName();
        }

        String forger = NameGenerator.generateForger(temp);
        String biomeKey = LootNBT.getBiomeKey(lootItem);
        String biomeName = biomeKeyToReadableName(biomeKey);

        String loreText = "Discovered by " + name + " in " + biomeName + ", forged by " + forger + ".";
        LootNBT.setLore(lootItem, loreText);

        // Generate and set the tool name
        String toolName = NameGenerator.generateNameWPrefix(temp, world.isRaining());
        LootNBT.setToolName(lootItem, toolName);
    }

    public static void generateNewTrait(ItemStack stack, ToolType type) {
        List<Modifier> mods = LootNBT.getModifiers(stack);
        ArrayList<Modifier> allowedMods = new ArrayList<>();

        RandomLoot.LOGGER.info("=== Generating trait for {} ===", type);

        for (Entry<String, Modifier> entry : ModifierRegistry.getModifiers().entrySet()) {
            Modifier newMod = entry.getValue();

            if (!Config.traitEnabled(newMod.tagName())) {
                continue;
            }

            // Filter by biome restrictions
            if (newMod instanceof BiomeRestrictedModifier) {
                BiomeRestrictedModifier biomeRestricted = (BiomeRestrictedModifier) newMod;
                String biomeKey = LootNBT.getBiomeKey(stack);
                float temp = LootNBT.getBiomeTemp(stack);
                String dimension = LootNBT.getDimension(stack);

                if (!biomeRestricted.canSpawnInBiome(biomeKey, temp, dimension)) {
                    continue;
                }
            }

            if (!newMod.forTool(type)) {
                continue;
            }

            boolean compatible = true;
            for (Modifier modifier : mods) {
                if (modifier.tagName().equals(newMod.tagName())) {
                    if (!modifier.canLevel()) {
                        compatible = false;
                        break;
                    }
                }

                if (!modifier.compatible(newMod)) {
                    compatible = false;
                    break;
                }
            }

            if (compatible) {
                allowedMods.add(newMod);
            }
        }

        int size = allowedMods.size();
        if (size == 0) {
            return;
        }

        int choice = (int) (Math.random() * size);
        Modifier m = allowedMods.get(choice);

        RandomLoot.LOGGER.info("SELECTED: {}", m.tagName());
        addModifier(stack, m);
    }

    public static void generateInitialTraits(ItemStack stack, ToolType type, int count) {
        for (int i = 0; i < count; i++) {
            generateNewTrait(stack, type);
        }
    }

    public static int getToolMaxTextures(ItemStack stack) {
        ToolType m = getToolType(stack);

        switch (m) {
            case PICKAXE: return PICKAXE_COUNT;
            case AXE: return AXE_COUNT;
            case SHOVEL: return SHOVEL_COUNT;
            case SWORD: return SWORD_COUNT;
            default: return 0;
        }
    }

    public static ItemStack genTool(EntityPlayer player, World world) {
        ItemStack lootItem = new ItemStack(ModItems.TOOL);

        if (world.isRemote) {
            return ItemStack.EMPTY;
        }

        // Calculate goodness based on cases opened
        int count = 0;
        if (player != null && player instanceof EntityPlayerMP) {
            EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
            count = serverPlayer.getStatFile().readStat(StatList.getObjectUseStats(ModItems.CASE));
        }

        float goodness = (float) (Math.sqrt(count + 1) * Config.Goodness);
        int traits = (int) (Math.floor(goodness / 2.0f));
        
        RandomLoot.LOGGER.info("Generating tool: count={}, goodness={}, traits={}", count, goodness, traits);

        LootNBT.setGoodness(lootItem, goodness);

        // Random tool type
        int toolType = (int) (Math.random() * 4);
        ToolType m;
        switch (toolType) {
            case 0: m = ToolType.PICKAXE; break;
            case 1: m = ToolType.AXE; break;
            case 2: m = ToolType.SHOVEL; break;
            case 3: m = ToolType.SWORD; break;
            default: m = ToolType.PICKAXE;
        }

        int textureCount;
        switch (m) {
            case PICKAXE: textureCount = PICKAXE_COUNT; break;
            case AXE: textureCount = AXE_COUNT; break;
            case SHOVEL: textureCount = SHOVEL_COUNT; break;
            case SWORD: textureCount = SWORD_COUNT; break;
            default: textureCount = 0;
        }

        setToolType(lootItem, m);

        // Store biome data BEFORE generating traits
        storeBiomeData(lootItem, world, player);

        // Store owner data for Soulbound modifier
        if (player != null) {
            LootNBT.setOwnerUUID(lootItem, player.getUniqueID().toString());
        }

        generateInitialTraits(lootItem, m, traits);
        generateLore(lootItem, world, player);

        LootNBT.setTexture(lootItem, (int) (Math.random() * textureCount));

        return lootItem;
    }

    public static boolean generateTool(EntityPlayerMP player, World world) {
        ItemStack lootItem = genTool(player, world);

        boolean added = player.inventory.addItemStackToInventory(lootItem);
        if (!added) {
            EntityItem dropItem = new EntityItem(world, player.posX, player.posY, player.posZ, lootItem);
            world.spawnEntity(dropItem);
        }
        return true;
    }

    public static String roman(int input) {
        if (input < 1 || input > 3999)
            return "Invalid Roman Number Value";
        StringBuilder s = new StringBuilder();
        while (input >= 1000) { s.append("M"); input -= 1000; }
        while (input >= 900) { s.append("CM"); input -= 900; }
        while (input >= 500) { s.append("D"); input -= 500; }
        while (input >= 400) { s.append("CD"); input -= 400; }
        while (input >= 100) { s.append("C"); input -= 100; }
        while (input >= 90) { s.append("XC"); input -= 90; }
        while (input >= 50) { s.append("L"); input -= 50; }
        while (input >= 40) { s.append("XL"); input -= 40; }
        while (input >= 10) { s.append("X"); input -= 10; }
        while (input >= 9) { s.append("IX"); input -= 9; }
        while (input >= 5) { s.append("V"); input -= 5; }
        while (input >= 4) { s.append("IV"); input -= 4; }
        while (input >= 1) { s.append("I"); input -= 1; }
        return s.toString();
    }
}
