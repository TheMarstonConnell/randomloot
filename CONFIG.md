# Configuration Guide

This document describes all configuration options available for Random Loot.

## Loot Chances

These settings control how often Random Loot items appear in structure chests.

| Option | Default | Range | Description |
|--------|---------|-------|-------------|
| `caseChance` | 0.25 | 0.0-1.0 | Chance to find a Loot Case in a chest |
| `modChance` | 0.15 | 0.0-1.0 | Chance to find a Trait Template in a chest |

## Progression

These settings affect how tools improve over time.

| Option | Default | Range | Description |
|--------|---------|-------|-------------|
| `goodness_rate` | 1.0 | 0.01-10.0 | Multiplier for tool improvement rate per player |

## Modifier Toggles

Each modifier can be individually enabled or disabled. Set to `false` to disable a modifier.

| Config Option | Modifier | Description |
|---------------|----------|-------------|
| `absorption_enabled` | [Appley](MODIFIERS.md#appley) | While holding the tool, get the absorption I effect. |
| `aquatic_enabled` | [Aquatic](MODIFIERS.md#aquatic) | Grants water breathing and Haste II when underwater. |
| `attracting_enabled` | [Magnetic](MODIFIERS.md#magnetic) | Upon breaking a block (allowed by tool type), all items at that block's position will teleport to you. |
| `bezerk_enabled` | [Bezerk](MODIFIERS.md#bezerk) | Deals more damage at lower player health. |
| `blinding_enabled` | [Blinding](MODIFIERS.md#blinding) | When attacking with tool, apply the blindness I effect to the target for 4 seconds. |
| `busted_enabled` | [Busted](MODIFIERS.md#busted) | Dig speed is increased as tool durability drops. |
| `charged_enabled` | [Charged](MODIFIERS.md#charged) | After 10 seconds, hitting and enemy will summon a lightning bolt and empty the charge meter. |
| `combo_enabled` | [Dexterous](MODIFIERS.md#dexterous) | Hitting enemies within 2 seconds after hitting them deals an extra 25% damage. |
| `critical_enabled` | [Critical](MODIFIERS.md#critical) | Always critically strikes enemy. |
| `detecting_enabled` | [Detecting](MODIFIERS.md#detecting) | While holding the tool, ores around you will glow. |
| `dirt_place_enabled` | [Heartha's Grace](MODIFIERS.md#heartha's-grace) | Right clicking on a block while crouching with the tool in hand will place a dirt block and use 1 durability points. |
| `explode_enabled` | [Explosive](MODIFIERS.md#explosive) | Upon breaking a block (allowed by tool type), the current block position will explode causing damage to surrounding blocks. |
| `filling_enabled` | [Filling](MODIFIERS.md#filling) | While holding the tool, get the saturation I effect. |
| `fire_place_enabled` | [Fire Starter](MODIFIERS.md#fire-starter) | Right clicking on the top of a block while crouching with the tool in hand will start a fire and use 2 durability points. |
| `fire_resistance_enabled` | [Heat Resistant](MODIFIERS.md#heat-resistant) | While holding the tool, get the fire resistance I effect. |
| `flame_thrower_enabled` | [Flame Thrower](MODIFIERS.md#flame-thrower) | Right clicking throws a fire ball. |
| `flaming_enabled` | [Flaming](MODIFIERS.md#flaming) | Sets enemy on fire for 2 seconds. |
| `frozen_enabled` | [Frozen](MODIFIERS.md#frozen) | Slows enemies on hit. Creates 3 block radius of frosted ice on water. |
| `hasty_enabled` | [Hasty](MODIFIERS.md#hasty) | While holding the tool, get the Haste I effect. |
| `learning_enabled` | [Learning](MODIFIERS.md#learning) | After breaking 10 blocks as allowed by this tool, gain 3 experience points. |
| `living_enabled` | [Living](MODIFIERS.md#living) | While holding the tool, it will randomly heal itself |
| `melting_enabled` | [Melting](MODIFIERS.md#melting) | Items dropped by blocks broken with this tool will be smelted. |
| `necrotic_enabled` | [Necrotic](MODIFIERS.md#necrotic) | Heals 10% of damage dealt to target. |
| `nemesis_enabled` | [Nemesis](MODIFIERS.md#nemesis) | Tracks mob kills and deals 5% bonus damage to your most killed mob type. |
| `overgrown_enabled` | [Overgrown](MODIFIERS.md#overgrown) | Grants poison immunity. Deals 2.5 bonus damage to arthropods. |
| `poison_enabled` | [Poisonous](MODIFIERS.md#poisonous) | When attacking with tool, apply the poison I effect to the target for 5 seconds. |
| `rainy_enabled` | [Rainy](MODIFIERS.md#rainy) | While holding the tool in the rain, mine faster! |
| `regeneration_enabled` | [Healing](MODIFIERS.md#healing) | While holding the tool, get the regeneration I effect. |
| `resistance_enabled` | [Resistant](MODIFIERS.md#resistant) | While holding the tool, get the resistance I effect. |
| `scorched_enabled` | [Scorched](MODIFIERS.md#scorched) | Sets enemies on fire for 4 seconds. Grants fire resistance while held. |
| `spawner_enabled` | [Tomb Raider](MODIFIERS.md#tomb-raider) | While holding the spawners around you will glow. |
| `torch_place_enabled` | [Spelunking](MODIFIERS.md#spelunking) | Right clicking on a block while crouching with the tool in hand will place a torch and use 10 durability points. |
| `unbreaking_enabled` | [Unbreaking](MODIFIERS.md#unbreaking) | This tool has a 20% chance of not taking damage. |
| `veiny_enabled` | [Veiny](MODIFIERS.md#veiny) | Breaking any block while crouching will cause all blocks of the same type adjacent to it to break up to 5 in each direction. |
| `void_touched_enabled` | [Void-Touched](MODIFIERS.md#void-touched) | Right-click to teleport up to 8.0 blocks. Costs 10 durability. |
| `wither_enabled` | [Withering](MODIFIERS.md#withering) | When attacking with tool, apply the wither I effect to the target for 3 seconds. |

