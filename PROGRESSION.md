# Tool Progression

This document explains how Random Loot tools improve over time.

## Tool Stats (Goodness)

Each tool has a "goodness" value that determines its base stats. Higher goodness means:
- Higher mining/digging speed
- More attack damage
- More durability

**Speed Formula:** `(goodness / 2) + 6`

| Goodness | Speed | Damage (Sword) | Durability |
|----------|-------|----------------|------------|
| 0 | 6.00 | 1.00 | 800 |
| 2 | 7.00 | 3.00 | 960 |
| 5 | 8.50 | 6.00 | 1,200 |
| 10 | 11.00 | 11.00 | 1,600 |
| 20 | 16.00 | 21.00 | 2,400 |

**Damage Formula:** `goodness + 1` (modified by tool type)
- Pickaxe: 50% damage
- Axe: 120% damage
- Shovel: 60% damage
- Sword: 100% damage

**Durability Formula:** `(goodness + 10) × 80`

## Leveling System

Tools gain XP from mining blocks and attacking enemies. Each level grants a **10% stat boost**.

**XP Required per Level:** `500 × 2^level`

| Level | XP Required | Total XP | Stat Multiplier |
|-------|-------------|----------|-----------------|
| 0 | 500 | 0 | 1.00x |
| 1 | 1,000 | 500 | 1.10x |
| 2 | 2,000 | 1,500 | 1.21x |
| 3 | 4,000 | 3,500 | 1.33x |
| 4 | 8,000 | 7,500 | 1.46x |
| 5 | 16,000 | 15,500 | 1.61x |
| 6 | 32,000 | 31,500 | 1.77x |
| 7 | 64,000 | 63,500 | 1.95x |
| 8 | 128,000 | 127,500 | 2.14x |
| 9 | 256,000 | 255,500 | 2.36x |
| 10 | 512,000 | 511,500 | 2.59x |

## Progression Curve

The more cases you open, the better your new tools become. Goodness is calculated as:

**Goodness Formula:** `√(cases_opened + 1) × goodness_rate`

**Starting Traits:** `floor(goodness / 2)`

| Cases Opened | Goodness | Starting Traits |
|--------------|----------|-----------------|
| 0 | 1.00 | 0 |
| 1 | 1.41 | 0 |
| 5 | 2.45 | 1 |
| 10 | 3.32 | 1 |
| 25 | 5.10 | 2 |
| 50 | 7.14 | 3 |
| 100 | 10.05 | 5 |
| 200 | 14.18 | 7 |
| 500 | 22.38 | 11 |
| 1000 | 31.64 | 15 |

## Tool Types

Random Loot generates four types of tools, each with multiple texture variants:

| Tool Type | Texture Variants | Primary Use |
|-----------|------------------|-------------|
| Pickaxe | 22 | Mining stone and ores |
| Axe | 18 | Chopping wood |
| Shovel | 12 | Digging dirt and sand |
| Sword | 53 | Combat |

Armor comes in 19 sets, each covering all four pieces.

