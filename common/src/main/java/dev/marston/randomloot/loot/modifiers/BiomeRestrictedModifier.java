package dev.marston.randomloot.loot.modifiers;

public interface BiomeRestrictedModifier extends Modifier {
	/**
	 * Check if this modifier can spawn in the given biome context.
	 * Only used during tool generation to restrict natural spawning.
	 *
	 * @param biomeKey The biome registry key (e.g., "minecraft:ocean")
	 * @param temperature The biome temperature (0.0 to 2.0+)
	 * @param dimension The dimension key (e.g., "minecraft:the_end")
	 * @return true if modifier can spawn naturally in this biome
	 */
	boolean canSpawnInBiome(String biomeKey, float temperature, String dimension);
}
