# Biome-Specific Traits

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
| [Aquatic](MODIFIERS.md#aquatic) | Ocean and river biomes | Grants water breathing and Haste II when underwater. |
| [Frozen](MODIFIERS.md#frozen) | Cold biomes (temperature <= 0.15) | Slows enemies on hit. Creates 3 block radius of frosted ice on water. |
| [Overgrown](MODIFIERS.md#overgrown) | Jungle, swamp and bamboo biomes | Grants poison immunity. Deals 2.5 bonus damage to arthropods. |
| [Scorched](MODIFIERS.md#scorched) | Hot biomes (temperature >= 1.0) or the Nether | Sets enemies on fire for 4 seconds. Grants fire resistance while held. |
| [Void-Touched](MODIFIERS.md#void-touched) | The End dimension only | Right-click to teleport up to 8.0 blocks. Costs 10 durability. |

See [MODIFIERS.md](MODIFIERS.md) for full effect descriptions and crafting recipes.

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

