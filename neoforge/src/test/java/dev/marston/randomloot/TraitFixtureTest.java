package dev.marston.randomloot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.NameGenerator;
import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.TraitEligibility;
import dev.marston.randomloot.loot.TraitEligibility.GearContext;
import dev.marston.randomloot.loot.TraitGrant;
import dev.marston.randomloot.loot.modifiers.BiomeRestrictedModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

/**
 * Saved gear is frozen. These fixtures are what the trait classes stored, loaded and
 * rendered before trait declarations existed, for every one of the registry's entries,
 * and each test holds today's code to them line for line:
 *
 * <ul>
 * <li>{@code grants.txt} - the tag a fresh grant stores, at every level a trait reaches,
 * with the name and description gear shows for it;
 * <li>{@code loads.txt} - saved tags read back and written out again: empty, renamed,
 * with a stray key, under the legacy {@code level} key, above the cap, and with real
 * state and tuning values that differ from today's defaults;
 * <li>{@code fits.txt} - which gear types and places each trait goes on;
 * <li>{@code registry.txt} - the wiki groups as the registry's role sets hold them;
 * <li>{@code gate.txt} - the gate's verdict for every ordered pair of traits.
 * </ul>
 *
 * <p>Two values are random by design and compared by shape: Chaotic's seed on a fresh
 * grant, and the forger in Grace's name when no world or stored name supplies one.
 *
 * <p>To regenerate after a deliberate change, run the tests with the environment
 * variable {@code RL_WRITE_TRAIT_FIXTURES} set to the fixture directory
 * ({@code neoforge/src/test/resources/trait-fixtures}) and read the diff.
 */
class TraitFixtureTest {

	private static final String WRITE_TO = System.getenv("RL_WRITE_TRAIT_FIXTURES");
	private static final long WORLD = 20261114L;

	/** A place of every kind a biome trait asks for, plus two it does not. */
	private static final Map<String, String[]> PLACES = places();

	private static Map<String, String[]> places() {
		Map<String, String[]> places = new LinkedHashMap<>();
		// name -> biome key, temperature, dimension
		places.put("plains", new String[] { "minecraft:plains", "0.8", "minecraft:overworld" });
		places.put("ocean", new String[] { "minecraft:deep_ocean", "0.5", "minecraft:overworld" });
		places.put("frozen", new String[] { "minecraft:snowy_plains", "0.0", "minecraft:overworld" });
		places.put("desert", new String[] { "minecraft:desert", "2.0", "minecraft:overworld" });
		places.put("jungle", new String[] { "minecraft:bamboo_jungle", "0.95", "minecraft:overworld" });
		places.put("nether", new String[] { "minecraft:nether_wastes", "2.0", "minecraft:the_nether" });
		places.put("end", new String[] { "minecraft:the_end", "0.5", "minecraft:the_end" });
		places.put("unknown", new String[] { "", "0.7", "minecraft:overworld" });
		return places;
	}

	private static GearContext gear(ToolType type, String place, List<Modifier> carrying) {
		String[] p = PLACES.get(place);
		return new GearContext(type, p[0], Float.parseFloat(p[1]), p[2], carrying, false, true);
	}

	private static Map<String, Modifier> registry() {
		return new TreeMap<>(ModifierRegistry.getModifiers());
	}

	// ---- grants.txt ----

	@Test
	void aFreshGrantStoresWhatItAlwaysDidAtEveryLevel() throws Exception {
		List<String> lines = new ArrayList<>();
		for (Map.Entry<String, Modifier> entry : registry().entrySet()) {
			String tag = entry.getKey();
			GearContext home = homeOf(entry.getValue());
			CompoundTag traits = new CompoundTag();

			for (int grant = 1; grant <= 64; grant++) {
				GearContext now = new GearContext(home.type(), home.biomeKey(), home.temperature(),
						home.dimension(), LootUtils.getModifiers(traits), false, true);
				TraitGrant.Kind kind = TraitGrant.grant(now, traits, entry.getValue(), WORLD, RandomSource.create(grant));
				if (kind == TraitGrant.Kind.REFUSED) {
					lines.add(tag + " #" + grant + " REFUSED");
					break;
				}

				assertEquals(1, traits.keySet().size(), tag + " should be the only trait stored");
				CompoundTag stored = traits.getCompoundOrEmpty(tag);
				Modifier onGear = LootUtils.getModifiers(traits).get(0);
				// Granted with a world, so Grace's forger is that world's and not a roll.
				lines.add(tag + " #" + grant + " " + kind + " " + shape(tag, stored, tag.equals("chaotic")));
				lines.add("    " + render(onGear));
				assertEquals(stored, onGear.toNBT(), tag + " read back off gear should write the same tag");
				// Trait dispatch saves a trait when what it would store differs before and
				// after a hook. One that stored something different each time it was asked
				// would be written to the gear on every tick it is held.
				assertEquals(onGear.toNBT(), onGear.toNBT(), tag + " should store the same thing every time it is asked");
			}
		}
		check("grants.txt", lines);
	}

	/** Gear a trait may go on: the first type and place the gate allows it for. */
	private static GearContext homeOf(Modifier trait) {
		for (ToolType type : ToolType.values()) {
			for (String place : PLACES.keySet()) {
				GearContext gear = gear(type, place, List.of());
				if (TraitEligibility.check(gear, trait).allowed()) {
					return gear;
				}
			}
		}
		throw new AssertionError(trait.tagName() + " goes on no gear anywhere");
	}

	// ---- loads.txt ----

	@Test
	void savedGearLoadsAndWritesBackAsItAlwaysDid() throws Exception {
		List<String> lines = new ArrayList<>();
		for (Map.Entry<String, Modifier> entry : registry().entrySet()) {
			String tag = entry.getKey();
			Modifier registered = entry.getValue();
			CompoundTag fresh = registered.forWorld(WORLD).toNBT();
			if (fresh.contains("chaoticSeed")) {
				fresh.putLong("chaoticSeed", 42L); // a set seed, so the cases below read the same every run
			}

			// Every trait: nothing stored, another name, a key nobody reads.
			load(lines, tag, "empty", new CompoundTag());
			CompoundTag renamed = fresh.copy();
			renamed.putString("name", "Renamed");
			load(lines, tag, "renamed", renamed);
			CompoundTag stray = fresh.copy();
			stray.putByte("stray", (byte) 1);
			load(lines, tag, "stray key", stray);

			// Every trait that levels, under each key it has ever used.
			for (String levelKey : List.of("trait_level", "power", "points")) {
				if (!fresh.contains(levelKey) || !(fresh.get(levelKey) instanceof net.minecraft.nbt.NumericTag)) {
					continue;
				}
				for (int level : new int[] { -1, 0, 1, 2, 3, 9, 40 }) {
					CompoundTag at = fresh.copy();
					if (fresh.get(levelKey) instanceof net.minecraft.nbt.FloatTag) {
						at.putFloat(levelKey, level);
					} else {
						at.putInt(levelKey, level);
					}
					load(lines, tag, levelKey + "=" + level, at);

					if (levelKey.equals("trait_level")) {
						CompoundTag legacy = at.copy();
						legacy.remove("trait_level");
						legacy.putInt("level", level);
						load(lines, tag, "legacy level=" + level, legacy);
					}
				}
			}
		}

		// Real state, and tuning values other than the ones a fresh grant gets today.
		String[][] saved = {
				{ "nemesis", "{name:\"Nemesis\",trait_level:2,killCounts:{\"minecraft:zombie\":3,\"minecraft:spider\":1}}" },
				{ "nemesis", "{name:\"Nemesis\",level:3,killCounts:{\"minecraft:creeper\":40}}" },
				{ "combo", "{points:5,name:\"Dexterous\",charged:123456L}" },
				{ "charged", "{points:7,name:\"Charged\",charged:999L}" },
				{ "learning", "{count:7,points:9,name:\"Learning\"}" },
				{ "prospector", "{name:\"Prospector\",trait_level:4,totalFinds:12}" },
				{ "chaotic", "{name:\"Chaotic\",chaoticSeed:987654321L}" },
				{ "dirt_place", "{name:\"Nobody's Grace\",DAMAGE:3}" },
				{ "dirt_place", "{DAMAGE:1}" },
				{ "torch_place", "{name:\"Spelunking\",DAMAGE:4}" },
				{ "fire_place", "{name:\"Fire Starter\",DAMAGE:5}" },
				{ "flame_thrower", "{name:\"Flame Thrower\",DAMAGE:6}" },
				{ "explode", "{power:6.5f,name:\"Explosive\"}" },
				{ "veiny", "{power:3.0f,name:\"Veiny\"}" },
				{ "hasty", "{name:\"Hasty\",power:1}" },
				{ "hasty", "{name:\"Hasty\",level:1}" },
				{ "hasty", "{name:\"Hasty\",trait_level:1,power:0}" },
				{ "hasty", "{name:\"Hasty\",trait_level:0,power:1}" },
				{ "blinding", "{name:\"Blinding\",power:3}" },
				{ "fire_resistance", "{name:\"Heat Resistant\",power:2}" },
				{ "poison", "{name:\"Poisonous\",power:4}" },
				{ "flaming", "{points:4,name:\"Flaming\"}" },
				{ "necrotic", "{points:7,name:\"Necrotic\"}" },
				{ "unbreaking", "{name:\"Unbreaking\",trait_level:4}" },
				{ "featherweight", "{name:\"Featherweight\",trait_level:3}" },
		};
		for (String[] one : saved) {
			load(lines, one[0], "saved", TagParser.parseCompoundFully(one[1]));
		}

		check("loads.txt", lines);
	}

	private static void load(List<String> lines, String tag, String what, CompoundTag saved) {
		Modifier loaded = ModifierRegistry.loadModifier(tag, saved);
		assertNotNull(loaded, tag + " should load");
		assertEquals(tag, loaded.tagName(), tag + " should load as itself");

		CompoundTag written = loaded.toNBT();
		// A name that is missing from Grace's tag is rolled afresh on every load.
		boolean rolledName = tag.equals("dirt_place") && !saved.contains("name");
		if (!rolledName) {
			assertEquals(written, ModifierRegistry.loadModifier(tag, written).toNBT(),
					tag + " (" + what + ") should write the same tag after a second load");
		}

		lines.add(tag + " [" + what + "] " + saved + " -> " + shape(tag, written, rolledName));
		lines.add("    " + (rolledName ? forgerShape(renderBriefly(loaded)) : renderBriefly(loaded)));
	}

	// ---- fits.txt ----

	@Test
	void everyTraitFitsTheGearAndPlacesItAlwaysDid() throws Exception {
		List<String> lines = new ArrayList<>();
		lines.add("# one letter per ToolType in declaration order " + List.of(ToolType.values())
				+ ", then one per place " + PLACES.keySet() + " (- for a trait with no biome rule)");
		for (Map.Entry<String, Modifier> entry : registry().entrySet()) {
			Modifier trait = entry.getValue();
			StringBuilder line = new StringBuilder(entry.getKey()).append(' ');
			for (ToolType type : ToolType.values()) {
				line.append(trait.forTool(type) ? 'Y' : '.');
			}
			line.append(' ');
			for (String[] place : PLACES.values()) {
				if (trait instanceof BiomeRestrictedModifier restricted) {
					line.append(restricted.canSpawnInBiome(place[0], Float.parseFloat(place[1]), place[2]) ? 'Y' : '.');
				} else {
					line.append('-');
				}
			}
			if (trait instanceof BiomeRestrictedModifier restricted) {
				line.append(' ').append(restricted.describeRestriction());
			}
			lines.add(line.toString());
		}
		check("fits.txt", lines);
	}

	// ---- registry.txt ----

	@Test
	void theRegistryListsAndGroupsTraitsAsItAlwaysDid() throws Exception {
		List<String> lines = new ArrayList<>();
		lines.add("traits " + registry().keySet());

		Map<String, Set<Modifier>> groups = new LinkedHashMap<>();
		groups.put("BREAKERS", ModifierRegistry.BREAKERS);
		groups.put("USERS", ModifierRegistry.USERS);
		groups.put("HURTERS", ModifierRegistry.HURTERS);
		groups.put("HOLDERS", ModifierRegistry.HOLDERS);
		groups.put("STATS", ModifierRegistry.STATS);
		groups.put("WEARERS", ModifierRegistry.WEARERS);
		groups.put("MISC", ModifierRegistry.MISC);
		for (Map.Entry<String, Set<Modifier>> group : groups.entrySet()) {
			Set<String> tags = new TreeSet<>();
			for (Modifier member : group.getValue()) {
				tags.add(member.tagName());
				// The wiki walks these sets and the registry side by side: same instances.
				assertEquals(ModifierRegistry.getModifier(member.tagName()), member,
						member.tagName() + " in " + group.getKey() + " should be the registry's own instance");
			}
			lines.add(group.getKey() + " " + tags);
		}

		lines.add("# what the registry's own instance of each trait shows: the config comment, the wiki and the lang file read these");
		for (Map.Entry<String, Modifier> entry : registry().entrySet()) {
			Modifier trait = entry.getValue();
			lines.add(entry.getKey() + " " + (entry.getKey().equals("dirt_place") ? renderShape(trait) : render(trait)));
		}
		check("registry.txt", lines);
	}

	// ---- gate.txt ----

	@Test
	void theGateGivesEveryPairOfTraitsTheVerdictItAlwaysDid() throws Exception {
		List<String> lines = new ArrayList<>();
		Map<String, Modifier> registry = registry();
		List<String> tags = new ArrayList<>(registry.keySet());

		// What gear carries is read back from a stored tag, never the registry's own
		// instance. Once as first granted, once as far up as it levels.
		Map<String, Modifier> fresh = new LinkedHashMap<>();
		Map<String, Modifier> maxed = new LinkedHashMap<>();
		for (String tag : tags) {
			Modifier registered = registry.get(tag);
			fresh.put(tag, registered.fromNBT(registered.forWorld(WORLD).toNBT()));
			Modifier top = registered.fromNBT(registered.forWorld(WORLD).toNBT());
			for (int i = 0; i < 64 && top.canLevel(); i++) {
				top.levelUp();
			}
			maxed.put(tag, registered.fromNBT(top.toNBT()));
		}

		// Readable: each new trait on gear it fits, against every trait that gear could
		// already carry. One row per carried trait, one letter per new trait.
		lines.add("# letters: O ok, D disabled, T wrong type, B wrong biome, M already maxed, I incompatible, G not gear, R rolling");
		lines.add("# columns, in order: " + tags);
		for (Map<String, Modifier> carried : List.of(fresh, maxed)) {
			lines.add(carried == fresh ? "carried as first granted:" : "carried at its cap:");
			for (String a : tags) {
				StringBuilder row = new StringBuilder();
				for (String b : tags) {
					GearContext home = homeOf(registry.get(b));
					GearContext gear = new GearContext(home.type(), home.biomeKey(), home.temperature(),
							home.dimension(), List.of(carried.get(a)), false, true);
					row.append(letter(TraitEligibility.check(gear, registry.get(b)).verdict()));
				}
				lines.add(row + " " + a);
			}
		}

		// Exhaustive: every gear type, every place, both ways of carrying, every ordered
		// pair. Far too many verdicts to read, so each grid is kept as a digest.
		lines.add("# digests of the full grid per gear type and place (carried as first granted / at its cap)");
		for (ToolType type : ToolType.values()) {
			for (String place : PLACES.keySet()) {
				StringBuilder line = new StringBuilder(type + " " + place);
				for (Map<String, Modifier> carried : List.of(fresh, maxed)) {
					StringBuilder grid = new StringBuilder();
					for (String a : tags) {
						for (String b : tags) {
							grid.append(letter(TraitEligibility
									.check(gear(type, place, List.of(carried.get(a))), registry.get(b)).verdict()));
						}
					}
					line.append(' ').append(digest(grid.toString()));
				}
				lines.add(line.toString());
			}
		}
		check("gate.txt", lines);
	}

	private static char letter(TraitEligibility.Verdict verdict) {
		return switch (verdict) {
		case OK -> 'O';
		case DISABLED -> 'D';
		case WRONG_TYPE -> 'T';
		case WRONG_BIOME -> 'B';
		case ALREADY_MAXED -> 'M';
		case INCOMPATIBLE -> 'I';
		case NOT_GEAR -> 'G';
		case ROLLING -> 'R';
		};
	}

	private static String digest(String text) throws Exception {
		byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
		return HexFormat.of().formatHex(hash, 0, 8);
	}

	// ---- shared ----

	/** A stored tag as text, with Chaotic's seed shown by shape when it was just rolled. */
	private static String shape(String tag, CompoundTag stored, boolean rolled) {
		CompoundTag shown = stored.copy();
		String text;
		if (rolled && tag.equals("chaotic") && shown.contains("chaoticSeed")) {
			shown.remove("chaoticSeed");
			shown.putString("chaoticSeed", "<a long>");
			text = shown.toString();
		} else if (rolled && tag.equals("dirt_place")) {
			text = forgerShape(shown.toString());
		} else {
			text = shown.toString();
		}
		return text;
	}

	/** What gear shows for a trait: the name with its level, the description, and how both are rendered. */
	private static String render(Modifier trait) {
		List<Component> lore = new ArrayList<>();
		trait.writeToLore(lore, false);
		List<Component> shiftLore = new ArrayList<>();
		trait.writeToLore(shiftLore, true);
		String details;
		try {
			Component component = trait.writeDetailsToLore(null);
			details = component == null ? "none" : component.toString();
		} catch (RuntimeException e) {
			details = "throws " + e.getClass().getSimpleName();
		}

		return "name=" + trait.name() + " | canLevel=" + trait.canLevel() + " | color=" + trait.color().name()
				+ " | description=" + trait.description() + " | displayName=" + trait.displayName()
				+ " | displayDescription=" + trait.displayDescription() + " | lore=" + lore + " | shiftLore="
				+ shiftLore + " | details=" + details + " | dynamicName=" + trait.hasDynamicName();
	}

	/** The parts of {@link #render} that depend on what was stored; the rest follows from them. */
	private static String renderBriefly(Modifier trait) {
		String details;
		try {
			Component component = trait.writeDetailsToLore(null);
			details = component == null ? "none" : component.toString();
		} catch (RuntimeException e) {
			details = "throws " + e.getClass().getSimpleName();
		}
		return "name=" + trait.name() + " | canLevel=" + trait.canLevel() + " | description=" + trait.description()
				+ " | details=" + details;
	}

	private static String renderShape(Modifier trait) {
		return forgerShape(render(trait));
	}

	/** Replaces whichever temperate forger was rolled, so a rolled name compares by shape. */
	private static String forgerShape(String text) {
		for (String forger : NameGenerator.TemperateNames) {
			text = text.replace(forger + "'s Grace", "<a forger>'s Grace");
		}
		return text;
	}

	private static void check(String file, List<String> lines) throws IOException {
		if (WRITE_TO != null && !WRITE_TO.isBlank()) {
			Files.createDirectories(Path.of(WRITE_TO));
			Files.write(Path.of(WRITE_TO, file), lines, StandardCharsets.UTF_8);
			return;
		}

		List<String> expected;
		try (InputStream in = TraitFixtureTest.class.getResourceAsStream("/trait-fixtures/" + file)) {
			assertNotNull(in, "fixture trait-fixtures/" + file + " is missing");
			expected = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
		}

		for (int i = 0; i < Math.min(expected.size(), lines.size()); i++) {
			if (!expected.get(i).equals(lines.get(i))) {
				fail(file + " line " + (i + 1) + " changed.\n  fixture: " + expected.get(i) + "\n  now:     "
						+ lines.get(i));
			}
		}
		assertEquals(expected.size(), lines.size(), file + " has a different number of lines");
	}
}
