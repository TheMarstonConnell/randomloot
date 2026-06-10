package dev.marston.randomloot.items;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.component.ModDataComponents;
import dev.marston.randomloot.component.ToolModifier;
import dev.marston.randomloot.loot.LootArmorItem;
import dev.marston.randomloot.loot.LootCase;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.ModTemplate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RandomLoot.MODID);

    // Datapack-editable anvil repair materials for Random Tools; see
    // data/randomloot/tags/item/tool_repair_materials.json (default: diamond).
    public static final TagKey<Item> TOOL_REPAIR_MATERIALS = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(RandomLoot.MODID, "tool_repair_materials"));

    // Datapack-editable anvil repair materials for Random Armor; see
    // data/randomloot/tags/item/armor_repair_materials.json (default: diamond).
    public static final TagKey<Item> ARMOR_REPAIR_MATERIALS = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(RandomLoot.MODID, "armor_repair_materials"));

    public static DeferredItem<Item> TOOL = ITEMS.registerItem("tool", p -> new LootItem(p.component(ModDataComponents.TOOL_MODIFIER.get(), new ToolModifier(new HashMap<>())).enchantable(15).repairable(TOOL_REPAIR_MATERIALS)));
    public static DeferredItem<Item> ARMOR = ITEMS.registerItem("armor", p -> new LootArmorItem(p.component(ModDataComponents.TOOL_MODIFIER.get(), new ToolModifier(new HashMap<>())).enchantable(15).repairable(ARMOR_REPAIR_MATERIALS)));
    public static DeferredItem<Item> CASE = ITEMS.registerItem("case", LootCase::new);
    public static DeferredItem<Item> MOD_ADD = ITEMS.registerItem("mod_add", p -> new ModTemplate(p, true));
    public static DeferredItem<Item> MOD_SUB = ITEMS.registerItem("mod_sub", p -> new ModTemplate(p, false));
    // Salvage material smelted from Random Tools and Random Armor; also the
    // crafting-grid ingredient for cycling loot gear textures.
    public static DeferredItem<Item> ESSENCE = ITEMS.registerItem("essence", Item::new);


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(TOOL);
            event.accept(ARMOR);
            event.accept(CASE);
            event.accept(MOD_ADD);
            event.accept(MOD_SUB);
            event.accept(ESSENCE);
        }
    }
}


