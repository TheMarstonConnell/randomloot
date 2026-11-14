package dev.marston.randomloot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.marston.randomloot.loot.NameGenerator;
import dev.marston.randomloot.loot.GearStats;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.modifiers.BiomeRestrictedModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import dev.marston.randomloot.recipes.TraitAdditionRecipe;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;

/**
 * Wiki generation: the repo's player docs (MODIFIERS.md, CONFIG.md, PROGRESSION.md,
 * NAMES.md, LOOT.md, BIOMES.md) and the trait entries of lang/en_us.json, rendered from
 * what the game itself holds - the trait registry, the config settings, {@link GearStats}
 * and the recipes a server has loaded.
 *
 * <p>It runs as the {@code wiki_generates} row of the gametest table, on a server whose
 * datapacks have loaded, so it sees recipes the way the game does instead of parsing the
 * JSON files a second time. Every run renders the docs, and a trait whose recipe did not
 * load fails that run. Only a run started with {@code RL_PROD=false} writes them, to the
 * repo root named by {@code RL_WIKI_DIR}; that is how the wiki workflow regenerates them.
 */
public final class GenWiki {

    /** The trait recipes the server loaded, by trait tag name; sorted, which is the order LOOT.md lists them in. */
    private final SortedMap<String, RecipeCost> recipes;

    /** Traits MODIFIERS.md has an entry for but no recipe loaded for. Any at all fails the render. */
    private final Set<String> withoutRecipe = new TreeSet<>();

    private GenWiki(SortedMap<String, RecipeCost> recipes) {
        this.recipes = recipes;
    }

    private static void write(String s, StringBuilder f) {
        f.append(s).append('\n');
    }

    private static String stripItemName(String name) {
        Identifier loc = Identifier.tryParse(name);
        return loc.getPath();
    }

    // DirtPlace rolls a random forger name each launch ("Heartha's Grace",
    // "Ivern's Grace", ...), so use a stable label in the docs to keep wiki
    // regeneration deterministic.
    private static String docName(Modifier m) {
        if (m.tagName().equals("dirt_place")) {
            return "Forger's Grace";
        }
        return m.name();
    }

    /**
     * The lang file this regenerates, relative to the repo root. It is the one source
     * file wiki generation still opens: it is rewritten in place, and its hand-written
     * entries have to be read back to be kept. Recipes are not read from here any more;
     * a stale path used to make every recipe in the docs read "n/a" without failing.
     */
    private static final String LANG_FILE = "common/src/main/resources/assets/randomloot/lang/en_us.json";

    // Resolves a path relative to the repo root. RL_WIKI_DIR overrides the
    // default of "..", which only holds when the game's working directory is a
    // direct child of the repo root (e.g. run/) — CI run configs live deeper.
    private static File repoFile(String relativePath) {
        String root = System.getenv("RL_WIKI_DIR");
        if (root == null || root.isBlank()) {
            root = "..";
        }
        return new File(root.strip(), relativePath);
    }

    private void writeMod(Modifier m, StringBuilder f) {
        String tag = m.tagName();
        String recipe = "n/a";
        // The count is what the Addition Template charges; one item is implied.
        String cost = "";
        RecipeCost loaded = recipes.get(tag);
        if (loaded == null) {
            // Never reaches a file: render() throws once every page has been through.
            withoutRecipe.add(tag);
        } else {
            recipe = loaded.itemId();
            if (loaded.count() > 1) {
                cost = " \u00d7" + loaded.count();
            }
        }
        write("### " + docName(m), f);

        write("**id:** `" + tag + "` | **crafting:** `" + recipe + "`" + cost + " ![" + stripItemName(recipe)
                + "](https://raw.githubusercontent.com/anish-shanbhag/minecraft-api/master/public/images/items/"
                + stripItemName(recipe) + ".png)", f);
        write("", f);
        write("**Description:** " + m.description(), f);
    }

    private void writeMods(Set<Modifier> mods, StringBuilder f) {
        List<Modifier> sortedList = new ArrayList<>(mods);
        sortedList.sort(Comparator.comparing(GenWiki::docName));

        for (Modifier modifier : sortedList) {
            writeMod(modifier, f);
        }
    }

    private void writeModifiers(StringBuilder f) {

        write("# Modifiers", f);
        write("This is a full list of modifiers in the game and a description of what they do.", f);

        write("## Breakers", f);
        write("These effects are applied when breaking blocks.", f);
        writeMods(ModifierRegistry.BREAKERS, f);

        write("## Holders", f);
        write("These effects are applied when holding the tool.", f);
        writeMods(ModifierRegistry.HOLDERS, f);

        write("## Users", f);
        write("These effects are applied when right clicking.", f);
        writeMods(ModifierRegistry.USERS, f);

        write("## Hurters", f);
        write("These effects are applied when hurting enemies.", f);
        writeMods(ModifierRegistry.HURTERS, f);

        write("## Wearers", f);
        write("These effects are exclusive to armor and trigger while worn.", f);
        writeMods(ModifierRegistry.WEARERS, f);

        write("## Stats", f);
        write("These effects are used to calculate stats for tools.", f);
        writeMods(ModifierRegistry.STATS, f);

        write("## Misc.", f);
        write("These effects are general and don't fit into any other categories.", f);
        writeMods(ModifierRegistry.MISC, f);
    }

    /** One CONFIG.md table: a row per setting of the group, rendered from its declaration rather than restated. */
    private static void writeSettingRows(Config.Group group, StringBuilder f) {
        for (Config.Setting<?> setting : Config.settings(group)) {
            write("| `" + setting.key() + "` | " + defaultCell(setting) + " | " + rangeCell(setting) + " | "
                    + setting.description() + " |", f);
        }
    }

    /** A number as Java prints it (0.25, and 1.0 for a decimal but 1 for a whole number); a list as inline code. */
    private static String defaultCell(Config.Setting<?> setting) {
        if (setting.defaultValue() instanceof List<?> list) {
            StringJoiner entries = new StringJoiner(", ", "`[", "]`");
            for (Object entry : list) {
                entries.add("\"" + entry + "\"");
            }
            return entries.toString();
        }
        return String.valueOf(setting.defaultValue());
    }

    private static String rangeCell(Config.Setting<?> setting) {
        if (setting.defaultValue() instanceof List<?>) {
            return "list of strings";
        }
        return setting.min() + "-" + setting.max();
    }

    private static void writeConfig(StringBuilder f) {
        write("# Configuration Guide", f);
        write("", f);
        write("This document describes all configuration options available for Random Loot.", f);
        write("", f);

        write("## Loot Chances", f);
        write("", f);
        write("These settings control how often Random Loot items appear in structure chests.", f);
        write("", f);
        write("| Option | Default | Range | Description |", f);
        write("|--------|---------|-------|-------------|", f);
        writeSettingRows(Config.Group.LOOT_CHANCES, f);
        write("", f);

        write("## Progression", f);
        write("", f);
        write("These settings affect how tools improve over time.", f);
        write("", f);
        write("| Option | Default | Range | Description |", f);
        write("|--------|---------|-------|-------------|", f);
        writeSettingRows(Config.Group.PROGRESSION, f);
        write("", f);

        write("## Modifier Settings", f);
        write("", f);
        write("These settings tune how individual modifiers behave.", f);
        write("", f);
        write("| Option | Default | Range | Description |", f);
        write("|--------|---------|-------|-------------|", f);
        writeSettingRows(Config.Group.MODIFIER_SETTINGS, f);
        write("", f);

        write("## Modifier Toggles", f);
        write("", f);
        write("Each modifier can be individually enabled or disabled. Set to `false` to disable a modifier.", f);
        write("", f);
        write("| Config Option | Modifier | Description |", f);
        write("|---------------|----------|-------------|", f);

        List<Map.Entry<String, Modifier>> sortedMods = new ArrayList<>(ModifierRegistry.getModifiers().entrySet());
        sortedMods.sort(Comparator.comparing(Map.Entry::getKey));

        for (Map.Entry<String, Modifier> entry : sortedMods) {
            String key = entry.getKey();
            Modifier mod = entry.getValue();
            write("| `" + Config.traitToggle(key).key() + "` | [" + docName(mod) + "](MODIFIERS.md#" + docName(mod).toLowerCase().replace(" ", "-") + ") | " + mod.description() + " |", f);
        }
        write("", f);
    }

    private static void writeProgression(StringBuilder f) {
        write("# Tool Progression", f);
        write("", f);
        write("This document explains how Random Loot tools improve over time.", f);
        write("", f);

        write("## Tool Stats (Goodness)", f);
        write("", f);
        write("Each tool has a \"goodness\" value that determines its base stats. Higher goodness means:", f);
        write("- Higher mining/digging speed", f);
        write("- More attack damage", f);
        write("- More durability", f);
        write("", f);
        write("**Speed Formula:** `(goodness / 2) + 6`", f);
        write("", f);
        write("| Goodness | Speed | Damage (Sword) | Durability |", f);
        write("|----------|-------|----------------|------------|", f);
        for (int goodness : new int[] { 0, 2, 5, 10, 20 }) {
            write(String.format("| %d | %.2f | %.2f | %,d |", goodness,
                    GearStats.digSpeed(goodness, ToolType.PICKAXE, 1.0f),
                    GearStats.attackDamage(goodness, ToolType.SWORD),
                    GearStats.maxDamage(goodness, ToolType.SWORD)), f);
        }
        write("", f);
        write("**Damage Formula:** `goodness + 1` (modified by tool type)", f);
        write("- Pickaxe: 50% damage", f);
        write("- Axe: 120% damage", f);
        write("- Shovel: 60% damage", f);
        write("- Sword: 100% damage", f);
        write("", f);
        write("**Durability Formula:** `(goodness + 10) × 80`", f);
        write("", f);

        write("## Leveling System", f);
        write("", f);
        write("Tools gain XP from mining blocks and attacking enemies. Each level grants a **10% stat boost**.", f);
        write("", f);
        write(String.format("Each level-up also has a **%d%%** chance to roll a bonus trait, or to level up a trait the gear already has"
                + " (`%s` in [CONFIG.md](CONFIG.md)).",
                Math.round(Config.LEVEL_UP_TRAIT_CHANCE.defaultValue() * 100), Config.LEVEL_UP_TRAIT_CHANCE.key()), f);
        write("", f);
        write("**XP Required per Level:** `500 × 2^level`", f);
        write("", f);
        write("| Level | XP Required | Total XP | Stat Multiplier |", f);
        write("|-------|-------------|----------|-----------------|", f);
        for (int i = 0; i <= 10; i++) {
            int totalXP = 0;
            for (int j = 0; j < i; j++) {
                totalXP += GearStats.maxXp(j);
            }
            // Walk the same 10%-per-level step the game applies, rather than restating it.
            float multiplier = 1.0f;
            for (int j = 0; j < i; j++) {
                multiplier = GearStats.leveledGoodness(multiplier);
            }
            write(String.format("| %d | %,d | %,d | %.2fx |", i, GearStats.maxXp(i), totalXP, multiplier), f);
        }
        write("", f);

        write("## Progression Curve", f);
        write("", f);
        write("The more cases you open, the better your new tools become. Goodness is calculated as:", f);
        write("", f);
        write("**Goodness Formula:** `√(cases_opened + 1) × goodness_rate`", f);
        write("", f);
        write("**Starting Traits:** `floor(goodness / 2)`", f);
        write("", f);
        write("| Cases Opened | Goodness | Starting Traits |", f);
        write("|--------------|----------|-----------------|", f);
        int[] caseCounts = {0, 1, 5, 10, 25, 50, 100, 200, 500, 1000};
        for (int count : caseCounts) {
            // Rate 1.0: the table documents the base curve, not a configured one.
            float goodness = GearStats.goodnessForCases(count, 1.0);
            write(String.format("| %d | %.2f | %d |", count, goodness, GearStats.startingTraits(goodness)), f);
        }
        write("", f);

        write("## Tool Types", f);
        write("", f);
        write("Random Loot generates four types of tools, each with multiple texture variants:", f);
        write("", f);
        write("| Tool Type | Texture Variants | Primary Use |", f);
        write("|-----------|------------------|-------------|", f);
        write("| Pickaxe | " + LootUtils.textureCount(ToolType.PICKAXE) + " | Mining stone and ores |", f);
        write("| Axe | " + LootUtils.textureCount(ToolType.AXE) + " | Chopping wood |", f);
        write("| Shovel | " + LootUtils.textureCount(ToolType.SHOVEL) + " | Digging dirt and sand |", f);
        write("| Sword | " + LootUtils.textureCount(ToolType.SWORD) + " | Combat |", f);
        write("", f);
        write("Armor comes in " + LootUtils.ARMOR_SET_COUNT + " sets, each covering all four pieces.", f);
        write("", f);
    }

    private static void writeNames(StringBuilder f) {
        write("# Name Generation", f);
        write("", f);
        write("This document explains how Random Loot tool names are generated.", f);
        write("", f);

        write("## Name Structure", f);
        write("", f);
        write("Tool names follow this pattern:", f);
        write("```", f);
        write("[Adjective] [Prefix][Suffix]", f);
        write("```", f);
        write("", f);
        write("Example: \"Blazing Fytenblade\" or \"Frozen Halbedda\"", f);
        write("", f);

        write("## Base Names", f);
        write("", f);
        write("### Prefixes", f);
        write("", f);
        writeStringArray(NameGenerator.Prefixes, f);
        write("", f);

        write("### Suffixes", f);
        write("", f);
        writeStringArray(NameGenerator.Suffixes, f);
        write("", f);

        write("## Temperature-Based Adjectives", f);
        write("", f);
        write("The adjective is chosen based on the biome temperature where the tool is created.", f);
        write("", f);

        write("### Hot Biomes (temp ≥ 1.0)", f);
        write("", f);
        writeStringArray(NameGenerator.HotAdj, f);
        write("", f);

        write("### Cold Biomes (temp ≤ 0.1)", f);
        write("", f);
        writeStringArray(NameGenerator.ColdAdj, f);
        write("", f);

        write("### Temperate Biomes (0.1 < temp < 1.0)", f);
        write("", f);
        writeStringArray(NameGenerator.TemperateAdj, f);
        write("", f);

        write("### Rainy Weather", f);
        write("", f);
        write("If it's raining when the tool is created, these adjectives are used instead:", f);
        write("", f);
        writeStringArray(NameGenerator.RainingAdj, f);
        write("", f);

        write("## Forger Names", f);
        write("", f);
        write("Each tool's lore mentions a forger. The forger name is based on biome temperature.", f);
        write("", f);

        write("### Hot Biome Forgers", f);
        write("", f);
        writeStringArray(NameGenerator.HotNames, f);
        write("", f);

        write("### Cold Biome Forgers", f);
        write("", f);
        writeStringArray(NameGenerator.ColdNames, f);
        write("", f);

        write("### Temperate Biome Forgers", f);
        write("", f);
        writeStringArray(NameGenerator.TemperateNames, f);
        write("", f);

        write("## Example", f);
        write("", f);
        write("A tool created in a desert (hot biome) might be named:", f);
        write("- **\"Scorched Kitmer\"** - forged by Blazicus", f);
        write("", f);
        write("A tool created in a snowy tundra (cold biome) might be named:", f);
        write("- **\"Frigid Halwam\"** - forged by Boreas", f);
        write("", f);
    }

    private static void writeStringArray(String[] array, StringBuilder f) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            sb.append("`").append(array[i]).append("`");
            if (i < array.length - 1) {
                sb.append(", ");
            }
        }
        write(sb.toString(), f);
    }

    private static void writeBiomes(StringBuilder f) {
        write("# Biome-Specific Traits", f);
        write("", f);
        write("Some traits in Random Loot are tied to specific biomes. These special traits can only appear on tools generated in matching biomes, and can only be added via smithing table to tools that originated from compatible biomes.", f);
        write("", f);

        write("## How It Works", f);
        write("", f);
        write("When you open a Loot Case, the tool remembers:", f);
        write("- The **biome** you were standing in", f);
        write("- The **temperature** of that biome", f);
        write("- The **dimension** you were in (Overworld, Nether, or End)", f);
        write("", f);
        write("This information determines which biome-specific traits can appear on the tool, both at creation and when crafting.", f);
        write("", f);

        write("## Biome-Restricted Traits", f);
        write("", f);
        write("| Trait | Biome Requirement | Details |", f);
        write("|-------|-------------------|---------|", f);
        // Every biome-restricted trait in the registry describes its own restriction, so
        // a sixth one shows up here without an edit.
        List<Map.Entry<String, Modifier>> biomeMods = new ArrayList<>(ModifierRegistry.getModifiers().entrySet());
        biomeMods.sort(Comparator.comparing(Map.Entry::getKey));
        for (Map.Entry<String, Modifier> entry : biomeMods) {
            if (!(entry.getValue() instanceof BiomeRestrictedModifier restricted)) {
                continue;
            }
            Modifier mod = entry.getValue();
            write("| [" + docName(mod) + "](MODIFIERS.md#" + docName(mod).toLowerCase().replace(" ", "-") + ") | "
                    + restricted.describeRestriction() + " | " + mod.description() + " |", f);
        }
        write("", f);
        write("See [MODIFIERS.md](MODIFIERS.md) for full effect descriptions and crafting recipes.", f);
        write("", f);

        write("## Crafting Restrictions", f);
        write("", f);
        write("When using the Smithing Table to add a biome-specific trait, the recipe will only work if the tool was originally created in a compatible biome. A tool created in the desert cannot have the Frozen trait added to it, even with a Smithing Table.", f);
        write("", f);

        write("## Tips", f);
        write("", f);
        write("- **Explore different biomes** to collect tools with different trait possibilities", f);
        write("- **Nether tools** can have Scorched trait naturally", f);
        write("- **End tools** are the only way to get Void-Touched", f);
        write("- **Biome data is permanent** - you cannot change a tool's origin biome", f);
        write("", f);

        write("## Biome Temperature Reference", f);
        write("", f);
        write("| Temperature | Biome Examples |", f);
        write("|-------------|----------------|", f);
        write("| <= 0.15 (Cold) | Snowy Plains, Ice Spikes, Frozen Ocean, Grove |", f);
        write("| 0.15 - 1.0 (Temperate) | Plains, Forest, Taiga, Ocean, Mountains |", f);
        write("| >= 1.0 (Hot) | Desert, Badlands, Savanna, Jungle |", f);
        write("", f);
        write("*Note: The Nether counts as \"hot\" for Scorched regardless of specific biome temperature.*", f);
        write("", f);
    }

    private void writeLoot(StringBuilder f) {
        write("# Loot & Crafting Guide", f);
        write("", f);
        write("This document explains where to find Random Loot items and how to craft with them.", f);
        write("", f);

        write("## Finding Loot Cases", f);
        write("", f);
        write("Loot Cases spawn in structure chests throughout your world:", f);
        write("- Dungeons", f);
        write("- Mineshafts", f);
        write("- Desert Temples", f);
        write("- Jungle Temples", f);
        write("- Strongholds", f);
        write("- Villages", f);
        write("- Woodland Mansions", f);
        write("- End Cities", f);
        write("- And any other structure with loot chests!", f);
        write("", f);
        write("Default spawn chance: **" + percent(Config.CASE_CHANCE) + "** (configurable in [CONFIG.md](CONFIG.md))", f);
        write("", f);

        write("## Using Loot Cases", f);
        write("", f);
        write("**Right-click** with a Loot Case in hand to open it and receive a random tool.", f);
        write("", f);
        write("The tool's quality depends on how many cases you've opened - see [PROGRESSION.md](PROGRESSION.md) for details.", f);
        write("", f);

        write("### Dispenser Automation", f);
        write("", f);
        write("Loot Cases can be placed in **Dispensers** for automated opening:", f);
        write("- The dispenser will open the case and eject the generated item", f);
        write("- Dispensed gear rolls at a fraction of the goodness of the most progressed online player (default " + percent(Config.DISPENSER_GOODNESS)
                + ", configurable via `" + Config.DISPENSER_GOODNESS.key() + "` in [CONFIG.md](CONFIG.md))", f);
        write("- The dispenser's own biome and dimension are recorded on the item, so biome-restricted traits can roll", f);
        write("- **Note:** Opening cases with a dispenser does not advance player progression", f);
        write("", f);

        write("## Repairing Tools", f);
        write("", f);
        write("Random Tools can be repaired in an **Anvil** with a repair material (default: **Diamond**). Each material restores 25% of the tool's maximum durability, just like vanilla tool repair. Repairing keeps the tool's name, traits, level and XP.", f);
        write("", f);
        write("Modpacks can change which items count as repair materials by editing the `randomloot:tool_repair_materials` item tag.", f);
        write("", f);

        write("## Finding Trait Templates", f);
        write("", f);
        write("Trait Templates also spawn in structure chests.", f);
        write("", f);
        write("Default spawn chance: **" + percent(Config.MOD_CHANCE) + "** (configurable in [CONFIG.md](CONFIG.md))", f);
        write("", f);

        write("## Trait Templates", f);
        write("", f);
        write("There are two types of Trait Templates:", f);
        write("", f);
        write("| Template | Function |", f);
        write("|----------|----------|", f);
        write("| Addition Template | Add a new trait to a tool |", f);
        write("| Subtraction Template | Remove a trait from a tool |", f);
        write("", f);
        write("**Right-click** with a template to toggle between Addition and Subtraction modes.", f);
        write("", f);

        write("### Using the Smithing Table", f);
        write("", f);
        write("To add or remove traits:", f);
        write("", f);
        write("1. Open a **Smithing Table**", f);
        write("2. Place a **Trait Template** in the template slot", f);
        write("3. Place your **Random Tool** in the base slot", f);
        write("4. Place the **required item** for the trait in the addition slot - the full count from the table below when adding, a single one when removing", f);
        write("5. Preview and take your modified tool", f);
        write("", f);

        write("## Trait Recipes", f);
        write("", f);
        write("Each trait requires a specific item to add or remove. See [MODIFIERS.md](MODIFIERS.md) for the full list.", f);
        write("", f);
        write("**Count** is what the Addition Template needs in the slot and uses up, and a leveled trait pays it again for every level. The Subtraction Template only ever takes one.", f);
        write("", f);
        write("| Trait | Required Item | Count |", f);
        write("|-------|---------------|-------|", f);

        for (Map.Entry<String, RecipeCost> entry : recipes.entrySet()) {
            String traitName = entry.getKey();
            RecipeCost cost = entry.getValue();
            // The recipe's tag name and the trait's display name can differ
            // (e.g. trait_necrotic is the "Draining" trait), so resolve
            // through the registry and only fall back to the tag name.
            Modifier mod = ModifierRegistry.getModifier(traitName);
            String displayName = mod != null ? docName(mod)
                    : traitName.substring(0, 1).toUpperCase() + traitName.substring(1).replace("_", " ");
            write("| " + displayName + " | `" + cost.itemId() + "` | " + cost.count() + " |", f);
        }
        write("", f);
    }

    /** A chance setting's default as the docs print it: 0.25 is "25%". */
    private static String percent(Config.Setting<Double> chance) {
        return Math.round(chance.defaultValue() * 100) + "%";
    }

    /**
     * Regenerates assets/randomloot/lang/en_us.json from the modifier registry, so trait
     * translation keys never drift from the code the way the hand-maintained file did.
     *
     * <p>Existing entries the generator doesn't know about (advancements, future
     * hand-added keys) are preserved: the generated entries are merged OVER the current
     * file instead of replacing it. A from-scratch rewrite once silently deleted every
     * advancement title, which then displayed as raw translation keys in game.
     *
     * <p>Leveling traits deliberately get no description entry: their description() bakes
     * in live power/duration values, so the dynamic English fallback in
     * {@code Modifier#displayDescription()} is more accurate than a frozen level-0 string.
     */
    private static void writeLang() throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        File langFile = repoFile(LANG_FILE);

        Map<String, String> entries = new TreeMap<>();

        // Start from the current file so hand-maintained keys survive regeneration.
        if (langFile.isFile()) {
            try (FileReader r = new FileReader(langFile)) {
                JsonObject existing = gson.fromJson(r, JsonObject.class);
                for (Map.Entry<String, JsonElement> e : existing.entrySet()) {
                    entries.put(e.getKey(), e.getValue().getAsString());
                }
            }
        }

        entries.put("item.randomloot.tool", "Random Tool");
        entries.put("item.randomloot.armor", "Random Armor");
        entries.put("item.randomloot.case", "Loot Case");
        entries.put("item.randomloot.mod_add", "Trait Addition Template");
        entries.put("item.randomloot.mod_sub", "Trait Subtraction Template");

        entries.put("message.randomloot.level_up_trait", "%s learned %s!");
        entries.put("message.randomloot.level_up_trait_leveled", "%s's %s grew stronger!");

        entries.put("tooltip.randomloot.level", "Level: %s");
        entries.put("tooltip.randomloot.xp", "XP: %s / %s");
        entries.put("tooltip.randomloot.speed", "Speed: %s");
        entries.put("tooltip.randomloot.damage", "Damage: %s");
        entries.put("tooltip.randomloot.armor", "Armor: %s");
        entries.put("tooltip.randomloot.toughness", "Toughness: %s");
        entries.put("tooltip.randomloot.shift_hint", "[Shift for more]");
        entries.put("tooltip.randomloot.ctrl_hint", "[Ctrl for trait info]");
        entries.put("tooltip.randomloot.type.pickaxe", "Pickaxe");
        entries.put("tooltip.randomloot.type.shovel", "Shovel");
        entries.put("tooltip.randomloot.type.axe", "Axe");
        entries.put("tooltip.randomloot.type.sword", "Sword");
        entries.put("tooltip.randomloot.type.helmet", "Helmet");
        entries.put("tooltip.randomloot.type.chestplate", "Chestplate");
        entries.put("tooltip.randomloot.type.leggings", "Leggings");
        entries.put("tooltip.randomloot.type.boots", "Boots");

        for (Modifier mod : ModifierRegistry.getModifiers().values()) {
            if (!mod.hasDynamicName()) {
                entries.put("modifier.randomloot." + mod.tagName() + ".name", mod.name());
            }
            if (!mod.canLevel()) {
                entries.put("modifier.randomloot." + mod.tagName() + ".description", mod.description());
            }
        }

        try (FileWriter f = new FileWriter(langFile)) {
            gson.toJson(entries, f);
            f.write("\n");
        }
    }

    /**
     * Renders every doc from a server's loaded recipes and, when the run was started with
     * {@code RL_PROD=false}, writes them under the repo root named by {@code RL_WIKI_DIR}.
     * With {@code RL_PROD} unset nothing is written.
     *
     * @throws IllegalStateException when a trait in MODIFIERS.md has no loaded recipe, or
     *                               the docs were asked for and the root is not the repo
     * @throws UncheckedIOException  when a file cannot be written
     */
    public static void generate(Collection<RecipeHolder<?>> loadedRecipes) {
        Map<String, String> docs = render(loadedRecipes);

        String isProd = System.getenv("RL_PROD");
        if (isProd == null || !isProd.strip().contains("false")) {
            return;
        }

        // Checked before the first write: with the wrong root the docs would land in a
        // directory nobody commits from, and the lang file would lose its hand-written entries.
        if (!repoFile(LANG_FILE).isFile()) {
            throw new IllegalStateException("RL_PROD=false asks for the wiki, but "
                    + repoFile("").toPath().toAbsolutePath().normalize() + " is not the repo root (no " + LANG_FILE + "). Set RL_WIKI_DIR to the repo root.");
        }

        RandomLoot.LOGGER.info("Creating wiki...");
        try {
            for (Map.Entry<String, String> doc : docs.entrySet()) {
                Files.writeString(repoFile(doc.getKey()).toPath(), doc.getValue());
                RandomLoot.LOGGER.info("Generated " + doc.getKey());
            }
            writeLang();
            RandomLoot.LOGGER.info("Generated lang/en_us.json");
        } catch (IOException e) {
            throw new UncheckedIOException("wiki generation could not write its files", e);
        }
        RandomLoot.LOGGER.info("Wiki generation complete!");
    }

    /**
     * The six docs as text, by file name, in the order they are written. Touches no file.
     *
     * @throws IllegalStateException when a trait in MODIFIERS.md has no loaded recipe;
     *                               the message names every such trait
     */
    public static Map<String, String> render(Collection<RecipeHolder<?>> loadedRecipes) {
        GenWiki wiki = new GenWiki(traitRecipes(loadedRecipes));

        Map<String, String> docs = new LinkedHashMap<>();
        docs.put("MODIFIERS.md", page(wiki::writeModifiers));
        docs.put("CONFIG.md", page(GenWiki::writeConfig));
        docs.put("PROGRESSION.md", page(GenWiki::writeProgression));
        docs.put("NAMES.md", page(GenWiki::writeNames));
        docs.put("LOOT.md", page(wiki::writeLoot));
        docs.put("BIOMES.md", page(GenWiki::writeBiomes));

        if (!wiki.withoutRecipe.isEmpty()) {
            throw new IllegalStateException("no trait recipe is loaded for " + wiki.withoutRecipe
                    + ". Each trait needs data/" + RandomLoot.MODID + "/recipe/" + RECIPE_PREFIX + "<tag>.json.");
        }
        return docs;
    }

    private static String page(Consumer<StringBuilder> writer) {
        StringBuilder page = new StringBuilder();
        writer.accept(page);
        return page.toString();
    }

    /** A trait recipe's addition item and how many of it the Addition Template charges. */
    private record RecipeCost(String itemId, int count) {
    }

    /** A trait's recipe has the id {@code randomloot:trait_<tag>}. */
    private static final String RECIPE_PREFIX = "trait_";

    private static SortedMap<String, RecipeCost> traitRecipes(Collection<RecipeHolder<?>> loadedRecipes) {
        SortedMap<String, RecipeCost> byTrait = new TreeMap<>();
        for (RecipeHolder<?> holder : loadedRecipes) {
            Identifier id = holder.id().identifier();
            if (holder.value() instanceof TraitAdditionRecipe recipe && id.getNamespace().equals(RandomLoot.MODID)
                    && id.getPath().startsWith(RECIPE_PREFIX)) {
                byTrait.put(id.getPath().substring(RECIPE_PREFIX.length()),
                        new RecipeCost(recipe.additionItem().getRegisteredName(), recipe.additionCount()));
            }
        }
        return byTrait;
    }

}
