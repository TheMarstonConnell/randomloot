package dev.marston.randomloot;

import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mod configuration, built on NeoForge's ModConfigSpec. On Fabric the same
 * spec API is provided by Forge Config API Port, so this class stays in
 * common; each loader registers {@link #SPEC} and wires its config-loaded
 * event to {@link #onLoad()}.
 */
public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static ModConfigSpec.DoubleValue CASE_CHANCE;

    private static ModConfigSpec.DoubleValue MOD_CHANCE;

    private static ModConfigSpec.DoubleValue GOODNESS;

    private static ModConfigSpec.DoubleValue ARMOR_CHANCE;

    private static ModConfigSpec.DoubleValue DISPENSER_GOODNESS;

    private static ModConfigSpec.ConfigValue<List<? extends String>> LOOT_TABLE_MATCHES;

    /**
     * A documented numeric option: one source for the spec definition, for the plain
     * static field's pre-load default, and for the CONFIG.md table {@link GenWiki}
     * renders. Defaults and ranges used to be written twice - once here, once as literal
     * markdown - so the docs could drift from the spec with nothing to notice.
     *
     * <p>These must be declared above {@link #SPEC}: static initializers run in source
     * order, and {@code SPEC = build()} reads them.
     */
    public record Option(String name, double defaultValue, double min, double max, String comment) {
    }

    public static final Option CASE_CHANCE_OPTION = new Option("caseChance", 0.25, 0.0, 1.0,
            "chance to find a case in a chest.");
    public static final Option MOD_CHANCE_OPTION = new Option("modChance", 0.15, 0.0, 1.0,
            "chance to find a modifier template in a chest.");
    public static final Option GOODNESS_OPTION = new Option("goodness_rate", 1.0, 0.01, 10.0,
            "rate of tool improvement per player");
    public static final Option ARMOR_CHANCE_OPTION = new Option("armorChance", 0.15, 0.0, 1.0,
            "chance that a loot case contains an armor piece instead of a tool.");
    public static final Option DISPENSER_GOODNESS_OPTION = new Option("dispenserGoodness", 0.75, 0.0, 1.0,
            "goodness of dispenser-opened cases, as a fraction of the highest goodness of any online player.");

    /** The loot-chance options, in the order CONFIG.md lists them. */
    public static List<Option> lootChanceOptions() {
        return List.of(CASE_CHANCE_OPTION, MOD_CHANCE_OPTION);
    }

    /** The progression options, in the order CONFIG.md lists them. */
    public static List<Option> progressionOptions() {
        return List.of(GOODNESS_OPTION, ARMOR_CHANCE_OPTION, DISPENSER_GOODNESS_OPTION);
    }

    // These mirror the spec so reads that land before the first ModConfigEvent.Loading
    // (e.g. early world gen) see sane values, not 0.0.
    public static double CaseChance = CASE_CHANCE_OPTION.defaultValue();
    public static double ModChance = MOD_CHANCE_OPTION.defaultValue();
    public static double Goodness = GOODNESS_OPTION.defaultValue();
    public static double ArmorChance = ARMOR_CHANCE_OPTION.defaultValue();
    public static double DispenserGoodness = DISPENSER_GOODNESS_OPTION.defaultValue();
    public static List<? extends String> LootTableMatches = List.of("chest");

    private static Map<String, ModConfigSpec.BooleanValue> MODIFIERS_ENABLED;
    private static Map<String, Boolean> ModsEnabled = new HashMap<>();

    public static final ModConfigSpec SPEC = build();

    public static ModConfigSpec build() {
        init();
        return BUILDER.build();
    }

    private static ModConfigSpec.DoubleValue define(Option option) {
        return BUILDER.comment(option.comment())
                .defineInRange(option.name(), option.defaultValue(), option.min(), option.max());
    }

    public static void init() {

        BUILDER.push("Loot Chances");
        CASE_CHANCE = define(CASE_CHANCE_OPTION);
        MOD_CHANCE = define(MOD_CHANCE_OPTION);
        LOOT_TABLE_MATCHES = BUILDER
                .comment("loot table id substrings that cases and templates can be injected into. An empty list disables injection entirely.")
                .defineListAllowEmpty("lootTableMatches", List.of("chest"), () -> "chest", o -> o instanceof String);
        BUILDER.pop();

        BUILDER.push("Modifiers Enabled");
        MODIFIERS_ENABLED = new HashMap<String, ModConfigSpec.BooleanValue>();

        for (Map.Entry<String, Modifier> entry : ModifierRegistry.getModifiers().entrySet()) {
            String key = entry.getKey();
            Modifier mod = entry.getValue();

            MODIFIERS_ENABLED.put(key,
                    BUILDER.comment("should the " + mod.name() + " trait be enabled").define(key + "_enabled", true));
        }
        BUILDER.pop();

        BUILDER.push("Misc");
        GOODNESS = define(GOODNESS_OPTION);
        ARMOR_CHANCE = define(ARMOR_CHANCE_OPTION);
        DISPENSER_GOODNESS = define(DISPENSER_GOODNESS_OPTION);
        BUILDER.pop();
    }

    public static boolean traitEnabled(String tagName) {
        return ModsEnabled.getOrDefault(tagName, true);
    }

    /** Pulls spec values into the plain static fields; called from each loader's config-loaded event. */
    public static void onLoad() {
        CaseChance = CASE_CHANCE.get();
        ModChance = MOD_CHANCE.get();
        Goodness = GOODNESS.get();
        ArmorChance = ARMOR_CHANCE.get();
        DispenserGoodness = DISPENSER_GOODNESS.get();
        LootTableMatches = LOOT_TABLE_MATCHES.get();

        ModsEnabled = new HashMap<String, Boolean>();
        for (Map.Entry<String, ModConfigSpec.BooleanValue> entry : MODIFIERS_ENABLED.entrySet()) {
            String key = entry.getKey();
            ModConfigSpec.BooleanValue val = entry.getValue();

            ModsEnabled.put(key, val.get());
        }

    }
}
