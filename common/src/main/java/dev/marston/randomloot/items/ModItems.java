package dev.marston.randomloot.items;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.loot.LootArmorItem;
import dev.marston.randomloot.loot.LootCase;
import dev.marston.randomloot.loot.ModTemplate;
import dev.marston.randomloot.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.Supplier;

public class ModItems {

    // Datapack-editable anvil repair materials for Random Tools; see
    // data/randomloot/tags/items/tool_repair_materials.json (default: diamond).
    public static final TagKey<Item> TOOL_REPAIR_MATERIALS = TagKey.create(Registries.ITEM,
            new ResourceLocation(RandomLoot.MODID, "tool_repair_materials"));

    // Datapack-editable anvil repair materials for Random Armor; see
    // data/randomloot/tags/items/armor_repair_materials.json (default: diamond).
    public static final TagKey<Item> ARMOR_REPAIR_MATERIALS = TagKey.create(Registries.ITEM,
            new ResourceLocation(RandomLoot.MODID, "armor_repair_materials"));

    public static Supplier<Item> TOOL = Services.REG.registerItem("tool",
            p -> Services.PLATFORM.createLootItem(gearProps(p, TOOL_REPAIR_MATERIALS)));
    public static Supplier<Item> ARMOR = Services.REG.registerItem("armor",
            p -> Services.PLATFORM.createLootArmorItem(gearProps(p, ARMOR_REPAIR_MATERIALS)));
    public static Supplier<Item> CASE = Services.REG.registerItem("case", LootCase::new);
    public static Supplier<Item> MOD_ADD = Services.REG.registerItem("mod_add", p -> new ModTemplate(p, true));
    public static Supplier<Item> MOD_SUB = Services.REG.registerItem("mod_sub", p -> new ModTemplate(p, false));
    // Salvage material smelted from Random Tools and Random Armor; also the
    // crafting-grid ingredient for cycling loot gear textures.
    public static Supplier<Item> ESSENCE = Services.REG.registerItem("essence", Item::new);

    /**
     * Shared properties for both gear items. Nothing to add on 1.20.1: trait state is
     * plain stack NBT, and enchantability and tag-based repair live on LootGearItem (no
     * Properties.enchantable / repairable; the item methods are the API).
     */
    private static Item.Properties gearProps(Item.Properties p, TagKey<Item> repairMaterials) {
        return p;
    }

    /** Classloads the class so the item registrations above run. */
    public static void init() {
    }

    /** Items shown in the Tools & Utilities creative tab, in display order. */
    public static List<Item> creativeTabItems() {
        return List.of(TOOL.get(), ARMOR.get(), CASE.get(), MOD_ADD.get(), MOD_SUB.get(), ESSENCE.get());
    }
}
