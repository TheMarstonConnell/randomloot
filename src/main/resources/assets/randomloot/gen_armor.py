"""Generates armor sets 1-10 (recolors) plus their models and equipment assets.

Item + worn-layer template art comes from the original Random Loot mod
(https://github.com/TheMarstonConnell/randomlootmod, assets/randomloot/textures):
sets 1-5 are gradient-mapped from the titanium set, sets 6-10 from the heavy set.
Sets 11-15 (netherite recolors) live in gen_armor_netherite.py; the item
definitions (items/tool.json + items/armor.json) are owned by gen.py. Run from
this directory with the old repo checked out at OLD_REPO:

    python3 gen_armor.py && python3 gen_armor_netherite.py && python3 gen.py
"""

import json
import os

from PIL import Image

OLD_REPO = "/tmp/randomlootmod/src/main/resources/assets/randomloot/textures"

# (name, shadow RGB, highlight RGB) - luminance gradient map endpoints.
PALETTES = [
    ("crimson", (0x3D, 0x0A, 0x0A), (0xE0, 0x4B, 0x4B)),
    ("gold", (0x5A, 0x3D, 0x08), (0xF2, 0xC8, 0x4B)),
    ("emerald", (0x0A, 0x3D, 0x1A), (0x4B, 0xE0, 0x7B)),
    ("azure", (0x0A, 0x1F, 0x3D), (0x4B, 0x8D, 0xE0)),
    ("amethyst", (0x2D, 0x0A, 0x3D), (0xB4, 0x4B, 0xE0)),
    ("copper", (0x3D, 0x1F, 0x0A), (0xE0, 0x8E, 0x4B)),
    ("frost", (0x1F, 0x3D, 0x3D), (0x9B, 0xE0, 0xE0)),
    ("obsidian", (0x0A, 0x0A, 0x14), (0x6B, 0x6B, 0x8C)),
    ("rose", (0x3D, 0x0A, 0x26), (0xE0, 0x4B, 0x9B)),
    ("verdant", (0x1F, 0x3D, 0x0A), (0x9B, 0xE0, 0x4B)),
]

# Old-mod template per set index: titanium for 1-5, heavy for 6-10.
TEMPLATES = ["titanium"] * 5 + ["heavy"] * 5

PIECES = ["helmet", "chestplate", "leggings", "boots"]
# Maps our piece names to the old mod's item texture names.
OLD_ITEM_NAMES = {"helmet": "helmet", "chestplate": "chest", "leggings": "legs", "boots": "boots"}

# range_dispatch offsets must match LootUtils.getTexture().
PIECE_OFFSET = {"helmet": 0.5, "chestplate": 0.6, "leggings": 0.7, "boots": 0.8}


def gradient_map(img, shadow, highlight):
    img = img.convert("RGBA")
    out = Image.new("RGBA", img.size)
    for x in range(img.width):
        for y in range(img.height):
            r, g, b, a = img.getpixel((x, y))
            if a == 0:
                out.putpixel((x, y), (0, 0, 0, 0))
                continue
            t = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            px = tuple(round(s + (h - s) * t) for s, h in zip(shadow, highlight))
            out.putpixel((x, y), px + (a,))
    return out


def main():
    os.makedirs("textures/entity/equipment/humanoid", exist_ok=True)
    os.makedirs("textures/entity/equipment/humanoid_leggings", exist_ok=True)
    os.makedirs("equipment", exist_ok=True)
    for piece in PIECES:
        os.makedirs(f"models/item/armor/{piece}", exist_ok=True)

    for i, (name, shadow, highlight) in enumerate(PALETTES):
        n = i + 1
        template = TEMPLATES[i]

        # Item sprites.
        for piece in PIECES:
            src = Image.open(f"{OLD_REPO}/items/{template}_{OLD_ITEM_NAMES[piece]}.png")
            gradient_map(src, shadow, highlight).save(f"textures/item/{piece}{n}.png")

        # Worn layers: layer_1 covers head/chest/feet, layer_2 covers legs.
        for old_layer, new_dir in (("layer_1", "humanoid"), ("layer_2", "humanoid_leggings")):
            src = Image.open(f"{OLD_REPO}/models/armor/{template}_{old_layer}.png")
            gradient_map(src, shadow, highlight).save(f"textures/entity/equipment/{new_dir}/set{n}.png")

        # Equipment asset wiring the worn layers together.
        with open(f"equipment/set{n}.json", "w") as f:
            json.dump({
                "layers": {
                    "humanoid": [{"texture": f"randomloot:set{n}"}],
                    "humanoid_leggings": [{"texture": f"randomloot:set{n}"}],
                }
            }, f, indent=4)
            f.write("\n")

        # Per-variant item models.
        for piece in PIECES:
            with open(f"models/item/armor/{piece}/{n}.json", "w") as f:
                json.dump({
                    "parent": "minecraft:item/generated",
                    "textures": {"layer0": f"randomloot:item/{piece}{n}"},
                }, f, indent=4)
                f.write("\n")

    # Fallback model for typeless armor (creative menu base item).
    with open("models/item/armor.json", "w") as f:
        json.dump({
            "parent": "minecraft:item/generated",
            "textures": {"layer0": "randomloot:item/chestplate1"},
        }, f, indent=4)
        f.write("\n")

    # items/armor.json is written by gen.py, which knows the full set count.
    print(f"Generated {len(PALETTES)} sets x {len(PIECES)} pieces.")


if __name__ == "__main__":
    main()
