package dev.marston.randomloot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.marston.randomloot.Config.Group;
import dev.marston.randomloot.Config.Setting;
import dev.marston.randomloot.loot.LootInjection;
import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.TraitEligibility;
import dev.marston.randomloot.loot.TraitEligibility.GearContext;
import dev.marston.randomloot.loot.TraitEligibility.Verdict;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import dev.marston.randomloot.loot.modifiers.wearers.Thorny;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

/**
 * The config settings, through what the rest of the mod uses: the spec the loaders
 * register, the listing CONFIG.md is rendered from, and the value a reader gets.
 */
class ConfigSettingTest {

	/** Every setting, hand-declared and per-trait alike. */
	private static List<Setting<?>> everySetting() {
		List<Setting<?>> all = new ArrayList<>();
		for (Group group : Group.values()) {
			all.addAll(Config.settings(group));
		}
		return all;
	}

	private static ModConfigSpec.ValueSpec specOf(Setting<?> setting) {
		Object entry = Config.SPEC.getSpec().get(List.of(setting.group().section(), setting.key()));
		return assertInstanceOf(ModConfigSpec.ValueSpec.class, entry,
				setting.key() + " should be defined in the spec under [" + setting.group().section() + "]");
	}

	@Test
	void everySettingConstantIsListed() throws IllegalAccessException {
		// Found by reflection, so a setting added to Config is covered without an edit here.
		Set<Setting<?>> listed = new HashSet<>(everySetting());
		int constants = 0;
		for (Field field : Config.class.getDeclaredFields()) {
			if (field.getType() != Setting.class || !Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			constants++;
			assertTrue(Modifier.isPublic(field.getModifiers()), field.getName() + " should be public so the mod can read it");
			assertTrue(listed.contains(field.get(null)), field.getName() + " is not in Config.settings(its group)");
		}
		assertTrue(constants >= 8, "expected the eight hand-declared settings, found " + constants);
	}

	@Test
	void everySettingIsInTheSpecAsDeclared() {
		for (Setting<?> setting : everySetting()) {
			ModConfigSpec.ValueSpec spec = specOf(setting);

			assertEquals(setting.defaultValue(), spec.getDefault(), setting.key() + " default");
			assertTrue(spec.getComment().startsWith(setting.comment()),
					setting.key() + " comment, got: " + spec.getComment());
			if (setting.min() == null) {
				assertNull(spec.getRange(), setting.key() + " declares no range");
			} else {
				assertEquals(setting.min(), spec.getRange().getMin(), setting.key() + " minimum");
				assertEquals(setting.max(), spec.getRange().getMax(), setting.key() + " maximum");
			}
		}
	}

	@Test
	void everySettingHasADocsEntry() {
		Set<String> keys = new HashSet<>();
		for (Setting<?> setting : everySetting()) {
			assertTrue(keys.add(setting.key()), "two settings share the key " + setting.key());
			assertNotNull(setting.defaultValue(), setting.key() + " default");
			assertFalse(setting.description().isBlank(), setting.key() + " description");
			assertFalse(setting.description().endsWith("."), setting.key() + " description is a table cell, no full stop");
		}
	}

	@Test
	void everyRegisteredTraitHasAToggle() {
		assertEquals(ModifierRegistry.getModifiers().size(), Config.settings(Group.TRAIT_TOGGLES).size());
		for (String tagName : ModifierRegistry.getModifiers().keySet()) {
			Setting<Boolean> toggle = Config.traitToggle(tagName);
			assertEquals(tagName + "_enabled", toggle.key());
			assertEquals(Group.TRAIT_TOGGLES, toggle.group());
			assertTrue(toggle.defaultValue(), tagName + " should default to enabled");
		}
		assertThrows(IllegalArgumentException.class, () -> Config.traitToggle("no_such_trait"));
		assertTrue(Config.traitEnabled("no_such_trait"), "an unknown tag name counts as enabled");
	}

	@Test
	void aSettingReadsAsItsDefaultBeforeTheFileLoads() {
		// No config file is loaded in a unit test, which is the state early world gen can see.
		for (Setting<?> setting : everySetting()) {
			assertEquals(setting.defaultValue(), setting.get(), setting.key());
		}
	}

	@Test
	void aSubstitutedValueIsSeenByAReader() {
		assertTrue(LootInjection.matchesTable("chests/simple_dungeon"));
		assertFalse(LootInjection.matchesTable("archaeology/desert_pyramid"));

		try (Config.Substitution digSites = Config.LOOT_TABLE_MATCHES.substitute(List.of("archaeology"))) {
			assertFalse(LootInjection.matchesTable("chests/simple_dungeon"));
			assertTrue(LootInjection.matchesTable("archaeology/desert_pyramid"));
		}

		assertTrue(LootInjection.matchesTable("chests/simple_dungeon"), "closing the substitution restores the setting");
		assertFalse(LootInjection.matchesTable("archaeology/desert_pyramid"));
	}

	@Test
	void closingASubstitutionPutsThePreviousValueBack() {
		try (Config.Substitution outer = Config.CASE_CHANCE.substitute(1.0)) {
			try (Config.Substitution inner = Config.CASE_CHANCE.substitute(0.0)) {
				assertEquals(0.0, Config.CASE_CHANCE.get());
			}
			assertEquals(1.0, Config.CASE_CHANCE.get());
		}
		assertEquals(Config.CASE_CHANCE.defaultValue(), Config.CASE_CHANCE.get());
	}

	/**
	 * The gate for adding a trait. That a disabled trait already on gear stops doing
	 * anything needs an item stack, so it is the {@code disabled_trait_is_filtered} gametest.
	 */
	@Test
	void aDisabledTraitIsRefused() {
		GearContext chestplate = GearContext.of(ToolType.CHESTPLATE, List.of());
		assertTrue(TraitEligibility.check(chestplate, new Thorny()).allowed());

		try (Config.Substitution off = Config.traitToggle("thorny").substitute(false)) {
			assertFalse(Config.traitEnabled("thorny"));
			assertEquals(Verdict.DISABLED, TraitEligibility.check(chestplate, new Thorny()).verdict());
			assertTrue(Config.traitEnabled("bulwark"), "only the substituted trait is off");
		}

		assertTrue(TraitEligibility.check(chestplate, new Thorny()).allowed());
	}
}
