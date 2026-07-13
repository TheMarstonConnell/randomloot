# Loot & Crafting Guide

This document explains where to find Random Loot items and how to craft with them.

## Finding Loot Cases

Loot Cases spawn in structure chests throughout your world:
- Dungeons
- Mineshafts
- Desert Temples
- Jungle Temples
- Strongholds
- Villages
- Woodland Mansions
- End Cities
- And any other structure with loot chests!

Default spawn chance: **25%** (configurable in [CONFIG.md](CONFIG.md))

## Using Loot Cases

**Right-click** with a Loot Case in hand to open it and receive a random tool.

The tool's quality depends on how many cases you've opened - see [PROGRESSION.md](PROGRESSION.md) for details.

### Dispenser Automation

Loot Cases can be placed in **Dispensers** for automated opening:
- The dispenser will open the case and eject the generated item
- Dispensed gear rolls at a fraction of the goodness of the most progressed online player (default 75%, configurable via `dispenserGoodness` in [CONFIG.md](CONFIG.md))
- The dispenser's own biome and dimension are recorded on the item, so biome-restricted traits can roll
- **Note:** Opening cases with a dispenser does not advance player progression

## Repairing Tools

Random Tools can be repaired in an **Anvil** with a repair material (default: **Diamond**). Each material restores 25% of the tool's maximum durability, just like vanilla tool repair. Repairing keeps the tool's name, traits, level and XP.

Modpacks can change which items count as repair materials by editing the `randomloot:tool_repair_materials` item tag.

## Finding Trait Templates

Trait Templates also spawn in structure chests.

Default spawn chance: **15%** (configurable in [CONFIG.md](CONFIG.md))

## Trait Templates

There are two types of Trait Templates:

| Template | Function |
|----------|----------|
| Addition Template | Add a new trait to a tool |
| Subtraction Template | Remove a trait from a tool |

**Right-click** with a template to toggle between Addition and Subtraction modes.

### Using the Smithing Table

To add or remove traits:

1. Open a **Smithing Table**
2. Place a **Trait Template** in the template slot
3. Place your **Random Tool** in the base slot
4. Place the **required item** for the trait in the addition slot
5. Preview and take your modified tool

## Trait Recipes

Each trait requires a specific item to add or remove. See [MODIFIERS.md](MODIFIERS.md) for the full list.

| Trait | Required Item | Count |
|-------|---------------|-------|

