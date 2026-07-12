package dev.marston.randomloot.items;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.component.ModDataComponents;
import dev.marston.randomloot.component.ToolModifier;
import dev.marston.randomloot.loot.LootArmorItem;
import dev.marston.randomloot.loot.LootCase;
import dev.marston.randomloot.loot.ModTemplate;
import dev.marston.randomloot.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

public class ModItems {

    // Datapack-editable anvil repair materials for Random Tools; see
    // data/randomloot/tags/item/tool_repair_materials.json (default: diamond).
    public static final TagKey<Item> TOOL_REPAIR_MATERIALS = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(RandomLoot.MODID, "tool_repair_materials"));

    // Datapack-editable anvil repair materials for Random Armor; see
    // data/randomloot/tags/item/armor_repair_materials.json (default: diamond).
    public static final TagKey<Item> ARMOR_REPAIR_MATERIALS = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(RandomLoot.MODID, "armor_repair_materials"));

    public static Supplier<Item> TOOL = Services.REG.registerItem("tool", p -> Services.PLATFORM.createLootItem(p.component(ModDataComponents.TOOL_MODIFIER.get(), new ToolModifier(new HashMap<>())).enchantable(15).repairable(TOOL_REPAIR_MATERIALS)));
    public static Supplier<Item> ARMOR = Services.REG.registerItem("armor", p -> new LootArmorItem(p.component(ModDataComponents.TOOL_MODIFIER.get(), new ToolModifier(new HashMap<>())).enchantable(15).repairable(ARMOR_REPAIR_MATERIALS)));
    public static Supplier<Item> CASE = Services.REG.registerItem("case", LootCase::new);
    public static Supplier<Item> MOD_ADD = Services.REG.registerItem("mod_add", p -> new ModTemplate(p, true));
    public static Supplier<Item> MOD_SUB = Services.REG.registerItem("mod_sub", p -> new ModTemplate(p, false));
    // Salvage material smelted from Random Tools and Random Armor; also the
    // crafting-grid ingredient for cycling loot gear textures.
    public static Supplier<Item> ESSENCE = Services.REG.registerItem("essence", Item::new);

    /** Classloads the class so the item registrations above run. */
    public static void init() {
    }

    /** Items shown in the Tools & Utilities creative tab, in display order. */
    public static List<Item> creativeTabItems() {
        return List.of(TOOL.get(), ARMOR.get(), CASE.get(), MOD_ADD.get(), MOD_SUB.get(), ESSENCE.get());
    }
}
