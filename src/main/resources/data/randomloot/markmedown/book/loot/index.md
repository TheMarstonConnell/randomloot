---
title: Loot & Crafting
icon: "randomloot:case"
index: 1
---
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

Default spawn chance: **25%** (configurable in [Configuration](config))

## Using Loot Cases

**Right-click** with a Loot Case in hand to open it and receive a random tool.

The tool's quality depends on how many cases you've opened - see [Progression](progression) for details.

### Dispenser Automation

Loot Cases can be placed in **Dispensers** for automated opening:
- The dispenser will open the case and eject the generated item
- Dispensed gear rolls at a fraction of the goodness of the most progressed online player (default 75%, configurable via `dispenserGoodness` in [Configuration](config))
- The dispenser's own biome and dimension are recorded on the item, so biome-restricted traits can roll
- **Note:** Opening cases with a dispenser does not advance player progression

## Repairing Tools

Random Tools can be repaired in an **Anvil** with a repair material (default: **Diamond**). Each material restores 25% of the tool's maximum durability, just like vanilla tool repair. Repairing keeps the tool's name, traits, level and XP.

Modpacks can change which items count as repair materials by editing the `randomloot:tool_repair_materials` item tag.

## Finding Trait Templates

Trait Templates also spawn in structure chests.

Default spawn chance: **15%** (configurable in [Configuration](config))

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

Each trait requires a specific item to add or remove. See [Modifiers](modifiers) for the full list.

| Trait | Required Item | Count |
|-------|---------------|-------|
| Appley | `minecraft:golden_apple` | 1 |
| Adrenaline | `minecraft:sugar` | 16 |
| Aquatic | `minecraft:prismarine_shard` | 1 |
| Magnetic | `minecraft:iron_block` | 1 |
| Bezerk | `minecraft:beef` | 16 |
| Blinding | `minecraft:carrot` | 24 |
| Bulwark | `minecraft:shield` | 1 |
| Busted | `minecraft:cracked_stone_bricks` | 3 |
| Catalyst | `minecraft:cinnabar` | 4 |
| Chaotic | `minecraft:amethyst_shard` | 1 |
| Charged | `minecraft:lightning_rod` | 1 |
| Clunky | `minecraft:iron_chain` | 1 |
| Dexterous | `minecraft:chorus_fruit` | 1 |
| Critical | `minecraft:ghast_tear` | 1 |
| Crowd Pleaser | `minecraft:firework_star` | 1 |
| Detecting | `minecraft:spyglass` | 1 |
| Forger's Grace | `minecraft:dirt` | 64 |
| Early Bird | `minecraft:sunflower` | 1 |
| Excavator | `minecraft:piston` | 1 |
| Executioner | `minecraft:iron_sword` | 1 |
| Explosive | `minecraft:tnt` | 8 |
| Feasting | `minecraft:golden_carrot` | 1 |
| Featherweight | `minecraft:feather` | 16 |
| Fierce | `minecraft:flint` | 1 |
| Filling | `minecraft:cake` | 1 |
| Fire Starter | `minecraft:flint_and_steel` | 1 |
| Heat Resistant | `minecraft:magma_cream` | 1 |
| Flame Thrower | `minecraft:fire_charge` | 12 |
| Flaming | `minecraft:blaze_rod` | 1 |
| Fragile | `minecraft:glass` | 1 |
| Frozen | `minecraft:packed_ice` | 1 |
| Hailey's Wrath | `minecraft:honeycomb` | 1 |
| Hasty | `minecraft:sugar` | 16 |
| Hunter | `minecraft:spider_eye` | 1 |
| Learning | `minecraft:book` | 12 |
| Living | `minecraft:moss_block` | 4 |
| Lumbering | `minecraft:stripped_oak_log` | 1 |
| Magnetized | `minecraft:lodestone` | 1 |
| Melting | `minecraft:lava_bucket` | 1 |
| Munchies | `minecraft:cookie` | 1 |
| Naturalist | `minecraft:bone_meal` | 1 |
| Necrotic | `minecraft:wither_skeleton_skull` | 1 |
| Nemesis | `minecraft:ender_eye` | 1 |
| Overgrown | `minecraft:vine` | 1 |
| Poisonous | `minecraft:poisonous_potato` | 4 |
| Prospector | `minecraft:raw_gold` | 1 |
| Pummeling | `minecraft:anvil` | 1 |
| Rainy | `minecraft:cauldron` | 1 |
| Healing | `minecraft:glowstone` | 8 |
| Resistant | `minecraft:turtle_scute` | 5 |
| Scorched | `minecraft:blaze_powder` | 1 |
| Soulbound | `minecraft:nether_star` | 1 |
| Tomb Raider | `minecraft:mossy_cobblestone` | 12 |
| Stench | `minecraft:sulfur` | 4 |
| Thorny | `minecraft:cactus` | 4 |
| Spelunking | `minecraft:torch` | 64 |
| Unbreaking | `minecraft:obsidian` | 8 |
| Veiny | `minecraft:diamond_pickaxe` | 1 |
| Void-Touched | `minecraft:ender_pearl` | 1 |
| Withering | `minecraft:wither_rose` | 1 |
