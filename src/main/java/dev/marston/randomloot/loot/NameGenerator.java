package dev.marston.randomloot.loot;

import net.minecraft.util.RandomSource;

public class NameGenerator {
	public static final String[] Prefixes = new String[] { "Fyten", "Fetter", "Red", "Tita", "Ty", "A", "Demu", "Tra",
			"Yam", "Hal", "Wel", "Hel", "Min", "Ju", "Hwa", "Kit", "Kat", "Kib", "Lib", "Sla", "Sli", "Slu", "Effe",
			"Edi"

	};

	public static final String[] Suffixes = new String[] { "blade", "bedda", "ayer", "mit", "miter", "cer", "gomme",
			"mer", "ber", "wam", "lam", "lo", "no", "bo", "low", "bow", "plow", "cry", "dry", "wry", "wrought", "ought",
			"ut", "oot", "uit", "grey", "sher", "gy", "shire", "ord", "yer", };

	public static final String[] HotAdj = new String[] { "Flaming", "Scorched", "Schorching", "Red-Hot", "Humid",
			"Searing", "Seared", "Burning", "Burnt", "Molten", "Glowing", "Steamy", "Sizzling", "Fervent", "Torrid",
			"Roasted", "Roasting", "Blazing", "Scalding", "Ultrahot", "White-Hot", "Sweltering", "Warm", "Toasty" };

	public static final String[] ColdAdj = new String[] { "Freezing", "Chilly", "Frozen", "Artic", "Frigid", "Cool",
			"Icy", "Nippy", "Chilled", "Bone-Chilling", "Snappy", "Sub-Zero", "Cryogenic", "Brisk", "Glacial", "Polar",
			"Ice-Cold" };

	public static final String[] TemperateAdj = new String[] { "Temperate", "Agreeable", "Collected", "Fair", "Soft",
			"Gentle", "Mild", "Pleasent", "Mighty", "Sturdy", "Leafy", "Modest" };

	public static final String[] RainingAdj = new String[] { "Wet", "Damp", "Gloomy", "Sad", "Drizzly", "Stormy",
			"Gray", "Ghastly", "Somber", "Bleak", "Dim", "Murky" };

	public static final String[] ColdNames = new String[] { "Boreas", "Borias", "Frostis", "Wintor", "Icis", "Burr", };

	public static final String[] TemperateNames = new String[] { "Heartha", "Trunken", "Plenta", "Lushious", "Ivern",
			"Soummar" };

	public static final String[] HotNames = new String[] { "Blazicus", "Dante", "Fyrus", "Pyrok", "Spaisee",
			"Infernus" };

	public static String generateForger(RandomSource random, float temp) {
		String[] list = TemperateNames;

		if (temp <= 0.1f) {
			list = ColdNames;
		} else if (temp >= 1) {
			list = HotNames;
		}

		return list[random.nextInt(list.length)];
	}

	/**
	 * The world's resident forger for a temperature band. Forgers are world-constant:
	 * a given world always credits hot-biome gear to the same smith, while different
	 * worlds roll different smiths. Each band salts the seed differently so one world
	 * doesn't land on the same list index for all three bands.
	 */
	public static String forgerForWorld(long worldSeed, float temp) {
		String[] list = TemperateNames;
		long salt = 0x9E3779B97F4A7C15L;

		if (temp <= 0.1f) {
			list = ColdNames;
			salt = 0xC2B2AE3D27D4EB4FL;
		} else if (temp >= 1) {
			list = HotNames;
			salt = 0x165667B19E3779F9L;
		}

		// SplitMix64-style mix so neighboring seeds don't pick neighboring names.
		long h = worldSeed ^ salt;
		h ^= h >>> 30;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 27;
		h *= 0x94D049BB133111EBL;
		h ^= h >>> 31;

		return list[(int) Math.floorMod(h, list.length)];
	}

	public static final String getAdj(RandomSource random, float temp, boolean raining) {
		String[] list = TemperateAdj;

		if (raining) {
			list = RainingAdj;
		} else {
			if (temp <= 0.1f) {
				list = ColdAdj;
			} else if (temp >= 1) {
				list = HotAdj;
			}
		}

		return list[random.nextInt(list.length)];
	}

	public static String generateName(RandomSource random) {
		String prefix = Prefixes[random.nextInt(Prefixes.length)];
		String suffix = Suffixes[random.nextInt(Suffixes.length)];

		return prefix + suffix;

	}

	public static String generateNameWPrefix(RandomSource random, float temp, boolean raining) {
		String pref = getAdj(random, temp, raining);
		String suff = generateName(random);

		return pref + " " + suff;

	}
}
