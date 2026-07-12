"""Writes items/tool.json AND items/armor.json from the model variants on disk.

Both files carry the IDENTICAL union of every tool and armor entry: while a
freshly opened item is rolling (LootUtils.rollingTexture), the cosmetic
property spins through every variant of every type, so a sword stack must be
able to display helmet looks and vice versa. Only the fallback model differs.

Tool counts come from listing models/item/tools/<type>; armor sets are the
fixed 1..ARMOR_SETS from gen_armor.py + gen_armor_netherite.py. Offsets must
match LootUtils.typeOffset(). Run from this directory: python3 gen.py
"""

import json
import os

ARMOR_SETS = 15

# (type offset, model path prefix) in LootUtils.typeOffset() order.
TOOL_TYPES = [
    (0.1, "tools/pickaxe"),
    (0.2, "tools/shovel"),
    (0.3, "tools/axe"),
    (0.4, "tools/sword"),
]
ARMOR_TYPES = [
    (0.5, "armor/helmet"),
    (0.6, "armor/chestplate"),
    (0.7, "armor/leggings"),
    (0.8, "armor/boots"),
]


def entries_for(offset, prefix, count):
    return [
        {
            "threshold": round(offset + i / 10000, 4),
            "model": {
                "type": "minecraft:model",
                "model": f"randomloot:item/{prefix}/{i + 1}",
            },
        }
        for i in range(count)
    ]


def main():
    entries = []
    for offset, prefix in TOOL_TYPES:
        count = len(os.listdir(f"models/item/{prefix}"))
        print(f"{count} {prefix}")
        entries += entries_for(offset, prefix, count)
    for offset, prefix in ARMOR_TYPES:
        entries += entries_for(offset, prefix, ARMOR_SETS)

    for name in ("tool", "armor"):
        with open(f"items/{name}.json", "w") as f:
            json.dump({
                "model": {
                    "type": "minecraft:range_dispatch",
                    "property": "randomloot:cosmetic",
                    "scale": 1,
                    "fallback": {"type": "minecraft:model", "model": f"randomloot:item/{name}"},
                    "entries": entries,
                }
            }, f, indent=4)
            f.write("\n")


if __name__ == "__main__":
    main()
