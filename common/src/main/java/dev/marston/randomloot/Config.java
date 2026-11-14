package dev.marston.randomloot;

import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * The mod's config settings. A setting is declared once, as a {@link Setting} constant
 * below: its key, default, range, comment and group. Everything else is derived from
 * that declaration - the entry in the spec both loaders register ({@link #SPEC}), the
 * row in CONFIG.md ({@link #settings(Group)}) and the value the mod reads
 * ({@link Setting#get()}).
 *
 * <p>Built on NeoForge's ModConfigSpec. On Fabric the same spec API is provided by
 * Forge Config API Port, so this class stays in common; each loader registers
 * {@link #SPEC} and wires its config-loaded event to {@link #onLoad()}.
 *
 * <p>To add a setting, add one constant above {@link #SPEC}. Nothing else needs an edit.
 */
public final class Config
{
    /** Every setting in declaration order, trait toggles included. Stays above the first declaration. */
    private static final List<Setting<?>> DECLARED = new ArrayList<>();

    /** Set once {@link #SPEC} exists, so a setting declared below it fails loudly instead of being left out. */
    private static boolean specBuilt;

    /**
     * Where a setting sits. One group gives both groupings: the section of
     * randomloot-common.toml it is written under, and the table of CONFIG.md it is listed
     * in. Constants are in the order the file writes its sections.
     */
    public enum Group {
        LOOT_CHANCES("Loot Chances"),
        /** One toggle per registered trait, declared from {@link ModifierRegistry} rather than by hand. */
        TRAIT_TOGGLES("Modifiers Enabled"),
        MODIFIER_SETTINGS("Modifier Settings"),
        /** The file has always called this section "Misc"; existing files depend on that name. */
        PROGRESSION("Misc");

        private final String section;

        Group(String section) {
            this.section = section;
        }

        /** The section name in randomloot-common.toml. */
        public String section() {
            return section;
        }
    }

    public static final Setting<Double> CASE_CHANCE = decimal(Group.LOOT_CHANCES, "caseChance", 0.25, 0.0, 1.0,
            "chance to find a case in a chest.");
    public static final Setting<Double> MOD_CHANCE = decimal(Group.LOOT_CHANCES, "modChance", 0.15, 0.0, 1.0,
            "chance to find a modifier template in a chest.");
    // The file's comment and the CONFIG.md row have always worded this one differently,
    // so it carries both; every other setting's row is its comment.
    public static final Setting<List<? extends String>> LOOT_TABLE_MATCHES = strings(Group.LOOT_CHANCES,
            "lootTableMatches", List.of("chest"),
            "loot table id substrings that cases and templates can be injected into. An empty list disables injection entirely.")
            .describedAs("Loot table id substrings that cases/templates can be injected into. Empty list disables injection");

    public static final Setting<Integer> LIVING_WHISPERS_PER_MINUTE = whole(Group.MODIFIER_SETTINGS,
            "livingWhispersPerMinute", 1, 0, 1200,
            "max whispers (\"pssst...\", \"hi...\", ...) per minute from the Living trait, shared across every Living item a player holds or wears. 0 disables them.");

    public static final Setting<Double> GOODNESS = decimal(Group.PROGRESSION, "goodness_rate", 1.0, 0.01, 10.0,
            "rate of tool improvement per player");
    public static final Setting<Double> ARMOR_CHANCE = decimal(Group.PROGRESSION, "armorChance", 0.15, 0.0, 1.0,
            "chance that a loot case contains an armor piece instead of a tool.");
    public static final Setting<Double> DISPENSER_GOODNESS = decimal(Group.PROGRESSION, "dispenserGoodness", 0.75, 0.0, 1.0,
            "goodness of dispenser-opened cases, as a fraction of the highest goodness of any online player.");
    public static final Setting<Double> LEVEL_UP_TRAIT_CHANCE = decimal(Group.PROGRESSION, "levelUpTraitChance", 0.2, 0.0, 1.0,
            "chance that each gear level-up also rolls a new trait (or levels up one it already has). 0 disables it, 1 rolls on every level-up.");

    /** The per-trait toggles by trait tag name, in the registry's own iteration order (the order the file lists them). */
    private static final Map<String, Setting<Boolean>> TRAIT_TOGGLES = declareTraitToggles();

    /** The spec both loaders register. Built from every setting declared above it. */
    public static final ModConfigSpec SPEC = buildSpec();

    private Config() {
    }

    private static Map<String, Setting<Boolean>> declareTraitToggles() {
        Map<String, Setting<Boolean>> toggles = new LinkedHashMap<>();
        for (Map.Entry<String, Modifier> entry : ModifierRegistry.getModifiers().entrySet()) {
            String tagName = entry.getKey();
            toggles.put(tagName, flag(Group.TRAIT_TOGGLES, tagName + "_enabled", true,
                    "should the " + entry.getValue().name() + " trait be enabled"));
        }
        return toggles;
    }

    private static ModConfigSpec buildSpec() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        for (Group group : Group.values()) {
            builder.push(group.section());
            for (Setting<?> setting : settings(group)) {
                setting.define(builder);
            }
            builder.pop();
        }
        specBuilt = true;
        return builder.build();
    }

    /** A group's settings in declaration order, which is the order the file lists them in (and CONFIG.md, which only re-sorts the trait toggles). */
    public static List<Setting<?>> settings(Group group) {
        List<Setting<?>> inGroup = new ArrayList<>();
        for (Setting<?> setting : DECLARED) {
            if (setting.group() == group) {
                inGroup.add(setting);
            }
        }
        return inGroup;
    }

    /** Whether a trait is switched on. A tag name with no trait behind it counts as on. */
    public static boolean traitEnabled(String tagName) {
        Setting<Boolean> toggle = TRAIT_TOGGLES.get(tagName);
        return toggle == null || toggle.get();
    }

    /** The on/off setting of a registered trait, e.g. to {@linkplain Setting#substitute substitute} it in a test. */
    public static Setting<Boolean> traitToggle(String tagName) {
        Setting<Boolean> toggle = TRAIT_TOGGLES.get(tagName);
        if (toggle == null) {
            throw new IllegalArgumentException("no trait is registered as '" + tagName + "'");
        }
        return toggle;
    }

    /** Takes every setting's value from the loaded file; each loader calls this from its config loaded and reloaded events. */
    public static void onLoad() {
        for (Setting<?> setting : DECLARED) {
            setting.load();
        }
    }

    private static Setting<Double> decimal(Group group, String key, double defaultValue, double min, double max,
            String comment) {
        return new Setting<>(group, key, defaultValue, min, max, comment,
                builder -> builder.defineInRange(key, defaultValue, min, max));
    }

    /** A whole-number setting, so the file and CONFIG.md don't show it as a decimal. */
    private static Setting<Integer> whole(Group group, String key, int defaultValue, int min, int max,
            String comment) {
        return new Setting<>(group, key, defaultValue, min, max, comment,
                builder -> builder.defineInRange(key, defaultValue, min, max));
    }

    /** A list of strings, which may be empty. A config screen offers the first default as the entry to add. */
    private static Setting<List<? extends String>> strings(Group group, String key, List<String> defaultValue,
            String comment) {
        return new Setting<>(group, key, defaultValue, null, null, comment,
                builder -> builder.defineListAllowEmpty(key, defaultValue,
                        () -> defaultValue.isEmpty() ? "" : defaultValue.get(0), o -> o instanceof String));
    }

    private static Setting<Boolean> flag(Group group, String key, boolean defaultValue, String comment) {
        return new Setting<>(group, key, defaultValue, null, null, comment,
                builder -> builder.define(key, defaultValue));
    }

    /**
     * One config setting: what it is called, what it defaults to, and how to read it.
     *
     * <p>{@link #get()} is how the mod reads the value. Until the config file has loaded
     * it returns the default, so a read that lands early (world gen before the first
     * load event) sees a sane value; after that it returns what the file said at the
     * last load or reload.
     */
    public static final class Setting<T> {
        private final Group group;
        private final String key;
        private final T defaultValue;
        private final T min;
        private final T max;
        private final String comment;
        private final Function<ModConfigSpec.Builder, ModConfigSpec.ConfigValue<T>> definer;
        private String description;

        private ModConfigSpec.ConfigValue<T> specValue;
        private volatile T loaded;
        private volatile T substituted;

        private Setting(Group group, String key, T defaultValue, T min, T max, String comment,
                Function<ModConfigSpec.Builder, ModConfigSpec.ConfigValue<T>> definer) {
            if (specBuilt) {
                throw new IllegalStateException("config setting '" + key + "' is declared below Config.SPEC,"
                        + " so the spec was built without it. Move the declaration above SPEC.");
            }
            this.group = group;
            this.key = key;
            this.defaultValue = defaultValue;
            this.min = min;
            this.max = max;
            this.comment = comment;
            this.definer = definer;
            this.loaded = defaultValue;
            DECLARED.add(this);
        }

        /** Gives the setting a CONFIG.md description that is not its comment. */
        private Setting<T> describedAs(String description) {
            this.description = description;
            return this;
        }

        private void define(ModConfigSpec.Builder builder) {
            specValue = definer.apply(builder.comment(comment));
        }

        private void load() {
            loaded = specValue.get();
        }

        /** The setting's current value. */
        public T get() {
            T substitute = substituted;
            return substitute != null ? substitute : loaded;
        }

        /**
         * Makes {@link #get()} return {@code value} until the returned substitution is
         * closed, whatever the config file says. For tests:
         *
         * <pre>{@code
         * try (Config.Substitution chance = Config.LEVEL_UP_TRAIT_CHANCE.substitute(1.0)) {
         *     // every reader sees 1.0 here
         * }
         * }</pre>
         *
         * Every reader on every thread sees it, so a gametest must open and close it
         * within one tick; left open across ticks it would leak into the tests running
         * beside it. Nothing in the mod itself calls this.
         */
        public Substitution substitute(T value) {
            T previous = substituted;
            substituted = Objects.requireNonNull(value, "value");
            return () -> substituted = previous;
        }

        /** The key in randomloot-common.toml, e.g. {@code caseChance}. */
        public String key() {
            return key;
        }

        public Group group() {
            return group;
        }

        public T defaultValue() {
            return defaultValue;
        }

        /** The lowest allowed value, or null for a setting with no range (a list, a toggle). */
        public T min() {
            return min;
        }

        /** The highest allowed value, or null for a setting with no range. */
        public T max() {
            return max;
        }

        /** The comment written above the setting in randomloot-common.toml. */
        public String comment() {
            return comment;
        }

        /** The setting described for CONFIG.md: its comment, capitalized and without the trailing period, unless it was given its own wording. */
        public String description() {
            if (description != null) {
                return description;
            }
            return Character.toUpperCase(comment.charAt(0)) + comment.substring(1).replaceAll("\\.$", "");
        }
    }

    /** A value put in place of a setting's own by {@link Setting#substitute}; closing it puts the previous one back. */
    @FunctionalInterface
    public interface Substitution extends AutoCloseable {
        @Override
        void close();
    }
}
