package dev.marston.randomloot.recipes;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootNBT;
import dev.marston.randomloot.loot.modifiers.BiomeRestrictedModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles anvil recipes for adding traits to tools.
 * All traits cost 30 XP levels.
 */
@Mod.EventBusSubscriber(modid = RandomLoot.MODID)
public class AnvilRecipeHandler {

    private static final int XP_COST = 30;
    private static final Map<Item, String> TRAIT_RECIPES = new HashMap<>();

    static {
        // Initialize trait recipes - tagNames must match ModifierRegistry exactly
        TRAIT_RECIPES.put(Items.ENDER_PEARL, "void_touched");
        TRAIT_RECIPES.put(Items.BLAZE_POWDER, "scorched");
        TRAIT_RECIPES.put(Items.PRISMARINE_SHARD, "aquatic");
        TRAIT_RECIPES.put(Item.getItemFromBlock(Blocks.PACKED_ICE), "frozen");
        TRAIT_RECIPES.put(Item.getItemFromBlock(Blocks.VINE), "overgrown");
        TRAIT_RECIPES.put(Item.getItemFromBlock(Blocks.TNT), "explode");
        TRAIT_RECIPES.put(Items.MAGMA_CREAM, "flaming");
        TRAIT_RECIPES.put(Items.GOLDEN_APPLE, "necrotic");  // Draining
        TRAIT_RECIPES.put(Items.EXPERIENCE_BOTTLE, "learning");
        TRAIT_RECIPES.put(Item.getItemFromBlock(Blocks.IRON_BLOCK), "attracting");
        TRAIT_RECIPES.put(Items.LAVA_BUCKET, "melting");
        TRAIT_RECIPES.put(Items.DIAMOND_PICKAXE, "excavator");
        TRAIT_RECIPES.put(Items.DIAMOND, "prospector");
        TRAIT_RECIPES.put(Item.getItemFromBlock(Blocks.REDSTONE_BLOCK), "veiny");
        TRAIT_RECIPES.put(Items.COAL, "torch_place");
        TRAIT_RECIPES.put(Item.getItemFromBlock(Blocks.DIRT), "dirt_place");
        TRAIT_RECIPES.put(Items.FLINT_AND_STEEL, "fire_place");
        TRAIT_RECIPES.put(Items.FIRE_CHARGE, "flame_thrower");
        TRAIT_RECIPES.put(Items.DIAMOND_SWORD, "critical");
        TRAIT_RECIPES.put(Items.FERMENTED_SPIDER_EYE, "poison");
        TRAIT_RECIPES.put(Items.SKULL, "wither"); // Wither skeleton skull (meta 1)
        TRAIT_RECIPES.put(Items.DYE, "blinding"); // Ink sac (meta 0)
        TRAIT_RECIPES.put(Items.NETHER_STAR, "bezerk");
        TRAIT_RECIPES.put(Items.NAME_TAG, "nemesis");
        TRAIT_RECIPES.put(Item.getItemFromBlock(Blocks.PISTON), "charged");
        TRAIT_RECIPES.put(Item.getItemFromBlock(Blocks.HOPPER), "combo");
        TRAIT_RECIPES.put(Items.SUGAR, "hasty");
        TRAIT_RECIPES.put(Items.GOLDEN_CARROT, "filling");
        TRAIT_RECIPES.put(Items.GHAST_TEAR, "regeneration");
        TRAIT_RECIPES.put(Item.getItemFromBlock(Blocks.OBSIDIAN), "resistance");
        TRAIT_RECIPES.put(Item.getItemFromBlock(Blocks.MAGMA), "fire_resistance");
        TRAIT_RECIPES.put(Items.BUCKET, "rainy");
        TRAIT_RECIPES.put(Items.EMERALD, "detecting");  // OreFinder
        TRAIT_RECIPES.put(Items.ENDER_EYE, "spawner");  // TreasureFinder
        TRAIT_RECIPES.put(Items.PRISMARINE_CRYSTALS, "living");
        TRAIT_RECIPES.put(Item.getItemFromBlock(Blocks.DIAMOND_BLOCK), "unbreaking");
        TRAIT_RECIPES.put(Item.getItemFromBlock(Blocks.SLIME_BLOCK), "busted");
        TRAIT_RECIPES.put(Items.TOTEM_OF_UNDYING, "absorption");  // Appley effect
        TRAIT_RECIPES.put(Items.BONE, "soulbound");
    }

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack tool = event.getLeft();
        ItemStack ingredient = event.getRight();

        // Check if left item is a random loot tool
        if (!(tool.getItem() instanceof LootItem)) {
            return;
        }

        // Check if we have a valid trait recipe
        String traitName = getTraitForItem(ingredient);
        if (traitName == null) {
            return;
        }

        // Check if tool already has this trait
        if (LootNBT.hasModifier(tool, traitName)) {
            return;
        }

        // Get the modifier
        Modifier modifier = ModifierRegistry.getModifier(traitName);
        if (modifier == null) {
            return;
        }

        // Check tool type compatibility
        int toolTypeOrdinal = LootNBT.getToolTypeOrdinal(tool);
        LootItem.ToolType toolType = LootItem.ToolType.values()[toolTypeOrdinal];
        if (!modifier.forTool(toolType)) {
            return;
        }

        // Check biome restrictions
        if (modifier instanceof BiomeRestrictedModifier) {
            BiomeRestrictedModifier restricted = (BiomeRestrictedModifier) modifier;
            String biomeKey = LootNBT.getBiomeKey(tool);
            float biomeTemp = LootNBT.getBiomeTemp(tool);
            String dimension = LootNBT.getDimension(tool);

            if (!restricted.canSpawnInBiome(biomeKey, biomeTemp, dimension)) {
                return; // Biome restriction not met
            }
        }

        // Create output with added trait
        ItemStack output = tool.copy();
        LootNBT.addModifier(output, modifier);

        event.setOutput(output);
        event.setCost(XP_COST);
        event.setMaterialCost(1);
    }

    private static String getTraitForItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        Item item = stack.getItem();
        String trait = TRAIT_RECIPES.get(item);

        // Handle special cases with metadata
        if (trait != null) {
            // Ink sac specifically (dye meta 0)
            if (item == Items.DYE && stack.getMetadata() != 0) {
                return null;
            }
            // Wither skeleton skull (meta 1)
            if (item == Items.SKULL && stack.getMetadata() != 1) {
                return null;
            }
        }

        return trait;
    }
}
