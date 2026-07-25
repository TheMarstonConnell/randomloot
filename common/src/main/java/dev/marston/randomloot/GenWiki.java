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
import net.minecraft.resources.Identifier;

import java.io.*;
import java.util.*;

public class GenWiki {

    private static void write(String s, FileWriter f) throws IOException {
        f.write(s + "\n");
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

    // Resolves a path relative to the repo root. RL_WIKI_DIR overrides the
    // default of "..", which only holds when the game's working directory is a
    // direct child of the repo root (e.g. run/) — CI run configs live deeper.
    /**
     * Where the mod's assets and data live. This moved from src/ to common/src/ in the
     * multiloader restructure, and GenWiki was not updated: every recipe lookup and the
     * lang merge silently failed, which is why all 68 entries in MODIFIERS.md read
     * "crafting: n/a" and LOOT.md's recipe table shipped with no rows.
     */
    private static final String RESOURCES = "common/src/main/resources/";

    private static File repoFile(String relativePath) {
        String root = System.getenv("RL_WIKI_DIR");
        if (root == null || root.isBlank()) {
            root = "..";
        }
        return new File(root.strip(), relativePath);
    }

    private static void writeMod(Modifier m, FileWriter f) throws IOException {
        String tag = m.tagName();
        String recipe = "n/a";
        try {
            recipe = readRecipe(tag);
        } catch (Exception e) {
            RandomLoot.LOGGER.warn("failed to find recipe for " + tag + ".");
        }
        write("### " + docName(m), f);

        write("**id:** `" + tag + "` | **crafting:** `" + recipe + "` ![" + stripItemName(recipe)
                + "](https://raw.githubusercontent.com/anish-shanbhag/minecraft-api/master/public/images/items/"
                + stripItemName(recipe) + ".png)", f);
        write("", f);
        write("**Description:** " + m.description(), f);
    }

    private static void writeMods(Set<Modifier> mods, FileWriter f) throws IOException {
        List<Modifier> sortedList = new ArrayList<>(mods);
        sortedList.sort(Comparator.comparing(GenWiki::docName));

        for (Modifier modifier : sortedList) {
            writeMod(modifier, f);
        }
    }

    private static void writeModifiers(FileWriter f) throws IOException {

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

    /** One CONFIG.md row, rendered from the spec definition rather than restated. */
    private static void writeOptionRow(Config.Option option, FileWriter f) throws IOException {
        String description = Character.toUpperCase(option.comment().charAt(0))
                + option.comment().substring(1).replaceAll("\\.$", "");
        write("| `" + option.name() + "` | " + trimNumber(option.defaultValue()) + " | "
                + trimNumber(option.min()) + "-" + trimNumber(option.max()) + " | " + description + " |", f);
    }

    /** 0.25 not 0.25000000000000001; 1.0 stays 1.0 so ranges read as decimals. */
    private static String trimNumber(double value) {
        return String.valueOf(value);
    }

    private static void writeConfig(FileWriter f) throws IOException {
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
        for (Config.Option option : Config.lootChanceOptions()) {
            writeOptionRow(option, f);
        }
        write("| `lootTableMatches` | `[\"chest\"]` | list of strings | Loot table id substrings that cases/templates can be injected into. Empty list disables injection |", f);
        write("", f);

        write("## Progression", f);
        write("", f);
        write("These settings affect how tools improve over time.", f);
        write("", f);
        write("| Option | Default | Range | Description |", f);
        write("|--------|---------|-------|-------------|", f);
        for (Config.Option option : Config.progressionOptions()) {
            writeOptionRow(option, f);
        }
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
            write("| `" + key + "_enabled` | [" + docName(mod) + "](MODIFIERS.md#" + docName(mod).toLowerCase().replace(" ", "-") + ") | " + mod.description() + " |", f);
        }
        write("", f);
    }

    private static void writeProgression(FileWriter f) throws IOException {
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

    private static void writeNames(FileWriter f) throws IOException {
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

    private static void writeStringArray(String[] array, FileWriter f) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            sb.append("`").append(array[i]).append("`");
            if (i < array.length - 1) {
                sb.append(", ");
            }
        }
        write(sb.toString(), f);
    }

    private static void writeBiomes(FileWriter f) throws IOException {
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

    private static void writeLoot(FileWriter f) throws IOException {
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
        write("Default spawn chance: **25%** (configurable in [CONFIG.md](CONFIG.md))", f);
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
        write("- Dispensed gear rolls at a fraction of the goodness of the most progressed online player (default 75%, configurable via `dispenserGoodness` in [CONFIG.md](CONFIG.md))", f);
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
        write("Default spawn chance: **15%** (configurable in [CONFIG.md](CONFIG.md))", f);
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
        write("4. Place the **required item** for the trait in the addition slot", f);
        write("5. Preview and take your modified tool", f);
        write("", f);

        write("## Trait Recipes", f);
        write("", f);
        write("Each trait requires a specific item to add or remove. See [MODIFIERS.md](MODIFIERS.md) for the full list.", f);
        write("", f);
        write("| Trait | Required Item | Count |", f);
        write("|-------|---------------|-------|", f);

        File recipeDir = repoFile(RESOURCES + "data/randomloot/recipe/");
        File[] recipeFiles = recipeDir.listFiles((dir, name) -> name.startsWith("trait_") && name.endsWith(".json"));

        if (recipeFiles != null) {
            Arrays.sort(recipeFiles);
            for (File recipeFile : recipeFiles) {
                try (BufferedReader bufferedReader = new BufferedReader(new FileReader(recipeFile))) {
                    String traitName = recipeFile.getName().replace("trait_", "").replace(".json", "");
                    JsonObject obj = new Gson().fromJson(bufferedReader, JsonObject.class);
                    JsonObject item = obj.get("item").getAsJsonObject();
                    String itemId = item.get("id").getAsString();
                    int count = 1;
                    if (item.has("count")) {
                        count = item.get("count").getAsInt();
                    }
                    // The recipe's tag name and the trait's display name can differ
                    // (e.g. trait_necrotic.json is the "Draining" trait), so resolve
                    // through the registry and only fall back to the filename.
                    Modifier mod = ModifierRegistry.getModifier(traitName);
                    String displayName = mod != null ? docName(mod)
                            : traitName.substring(0, 1).toUpperCase() + traitName.substring(1).replace("_", " ");
                    write("| " + displayName + " | `" + itemId + "` | " + count + " |", f);
                } catch (Exception e) {
                    RandomLoot.LOGGER.warn("Failed to read recipe: " + recipeFile.getName());
                }
            }
        }
        write("", f);
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
        File langFile = repoFile(RESOURCES + "assets/randomloot/lang/en_us.json");

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

    public static void genWiki() {

        String isProdEnv = System.getenv("RL_PROD");
        if (isProdEnv == null) {
            return;
        }

        String isProd = isProdEnv.strip();

        if (isProd.contains("false")) {
            RandomLoot.LOGGER.info("Creating wiki...");

            try {
                FileWriter modifiersWriter = new FileWriter(repoFile("MODIFIERS.md"));
                writeModifiers(modifiersWriter);
                modifiersWriter.close();
                RandomLoot.LOGGER.info("Generated MODIFIERS.md");

                FileWriter configWriter = new FileWriter(repoFile("CONFIG.md"));
                writeConfig(configWriter);
                configWriter.close();
                RandomLoot.LOGGER.info("Generated CONFIG.md");

                FileWriter progressionWriter = new FileWriter(repoFile("PROGRESSION.md"));
                writeProgression(progressionWriter);
                progressionWriter.close();
                RandomLoot.LOGGER.info("Generated PROGRESSION.md");

                FileWriter namesWriter = new FileWriter(repoFile("NAMES.md"));
                writeNames(namesWriter);
                namesWriter.close();
                RandomLoot.LOGGER.info("Generated NAMES.md");

                FileWriter lootWriter = new FileWriter(repoFile("LOOT.md"));
                writeLoot(lootWriter);
                lootWriter.close();
                RandomLoot.LOGGER.info("Generated LOOT.md");

                FileWriter biomesWriter = new FileWriter(repoFile("BIOMES.md"));
                writeBiomes(biomesWriter);
                biomesWriter.close();
                RandomLoot.LOGGER.info("Generated BIOMES.md");

                writeLang();
                RandomLoot.LOGGER.info("Generated lang/en_us.json");

                RandomLoot.LOGGER.info("Wiki generation complete!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static String readRecipe(String trait) throws IOException {
        File recipeFile = repoFile(RESOURCES + "data/randomloot/recipe/trait_" + trait + ".json");
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(recipeFile))) {
            JsonObject obj = new Gson().fromJson(bufferedReader, JsonObject.class);
            return obj.get("item").getAsJsonObject().get("id").getAsString();
        }
    }

}