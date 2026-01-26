package dev.marston.randomloot;

import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import net.minecraftforge.common.config.Configuration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Config {
    private static Configuration config;

    // Loot chances
    public static double CaseChance = 0.25;

    // Misc
    public static double Goodness = 1.0;

    // Modifier toggles
    private static Map<String, Boolean> ModsEnabled = new HashMap<>();

    public static void init(File configFile) {
        if (config == null) {
            config = new Configuration(configFile);
            loadConfig();
        }
    }

    private static void loadConfig() {
        try {
            config.load();

            // Loot Chances category
            CaseChance = config.getFloat("caseChance", "loot_chances", 0.25f, 0.0f, 1.0f,
                    "Chance to find a case in a chest");

            // Misc category
            Goodness = config.getFloat("goodness_rate", "misc", 1.0f, 0.01f, 10.0f,
                    "Rate of tool improvement per player");

            // Modifiers Enabled category
            for (Map.Entry<String, Modifier> entry : ModifierRegistry.getModifiers().entrySet()) {
                String key = entry.getKey();
                Modifier mod = entry.getValue();
                boolean enabled = config.getBoolean(key + "_enabled", "modifiers_enabled", true,
                        "Should the " + mod.name() + " trait be enabled");
                ModsEnabled.put(key, enabled);
            }

        } catch (Exception e) {
            RandomLoot.LOGGER.error("Error loading config", e);
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }

    public static boolean traitEnabled(String tagName) {
        return ModsEnabled.getOrDefault(tagName, true);
    }
}
