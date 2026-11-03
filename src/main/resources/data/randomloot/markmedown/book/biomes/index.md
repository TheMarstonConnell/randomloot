---
title: Biome Traits
icon: "minecraft:prismarine_shard"
index: 3
---
Some traits in Random Loot are tied to specific biomes. These special traits can only appear on tools generated in matching biomes, and can only be added via smithing table to tools that originated from compatible biomes.

## How It Works

When you open a Loot Case, the tool remembers:
- The **biome** you were standing in
- The **temperature** of that biome
- The **dimension** you were in (Overworld, Nether, or End)

This information determines which biome-specific traits can appear on the tool, both at creation and when crafting.

## Biome-Restricted Traits

| Trait | Biome Requirement | Details |
|-------|-------------------|---------|
| [Aquatic](modifiers/holders) | Ocean or River biomes | Water breathing + Haste underwater |
| [Frozen](modifiers/hurters) | Cold biomes (temp <= 0.15) | Slowness on hit, frost walker |
| [Scorched](modifiers/hurters) | Hot biomes (temp >= 1.0) or Nether | Fire damage, fire resistance |
| [Overgrown](modifiers/hurters) | Jungle, Swamp, or Bamboo biomes | Arthropod damage, poison immunity |
| [Void-Touched](modifiers/users) | The End dimension only | Teleport on right-click |

See [Modifiers](modifiers) for full effect descriptions and crafting recipes.

## Crafting Restrictions

When using the Smithing Table to add a biome-specific trait, the recipe will only work if the tool was originally created in a compatible biome. A tool created in the desert cannot have the Frozen trait added to it, even with a Smithing Table.

## Tips

- **Explore different biomes** to collect tools with different trait possibilities
- **Nether tools** can have Scorched trait naturally
- **End tools** are the only way to get Void-Touched
- **Biome data is permanent** - you cannot change a tool's origin biome

## Biome Temperature Reference

| Temperature | Biome Examples |
|-------------|----------------|
| <= 0.15 (Cold) | Snowy Plains, Ice Spikes, Frozen Ocean, Grove |
| 0.15 - 1.0 (Temperate) | Plains, Forest, Taiga, Ocean, Mountains |
| >= 1.0 (Hot) | Desert, Badlands, Savanna, Jungle |

*Note: The Nether counts as "hot" for Scorched regardless of specific biome temperature.*
