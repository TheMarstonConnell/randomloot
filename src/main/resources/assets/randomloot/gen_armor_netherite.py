"""Generates armor sets 11-15: recolors of vanilla netherite armor.

Netherite art is very dark and low-contrast, so opaque-pixel luminance is
min/max-normalized per source texture before gradient-mapping into each
palette - otherwise every recolor collapses into the shadow color.

Source textures are extracted from the Minecraft client jar:

    mkdir -p /tmp/nethsrc && cd /tmp/nethsrc
    unzip -o ~/.gradle/caches/neoformruntime/artifacts/minecraft_26.1.2_client.jar \
        'assets/minecraft/textures/item/netherite_*.png' \
        'assets/minecraft/textures/entity/equipment/humanoid/netherite.png' \
        'assets/minecraft/textures/entity/equipment/humanoid_leggings/netherite.png'

items/armor.json (and items/tool.json) are written by gen.py - run it after
this script. Run from this directory: python3 gen_armor_netherite.py
"""

import json

from PIL import Image

SRC = "/tmp/nethsrc/assets/minecraft/textures"

TOTAL_SETS = 15
PIECES = ["helmet", "chestplate", "leggings", "boots"]

# (set number, name, shadow RGB, highlight RGB) - dark, premium ramps so the
# netherite feel survives; distinct from the brighter palettes of sets 1-10.
PALETTES = [
    (11, "ember", (0x2B, 0x0D, 0x06), (0xFF, 0x7A, 0x3C)),
    (12, "abyss", (0x06, 0x26, 0x2B), (0x4F, 0xD8, 0xC4)),
    (13, "violet", (0x1C, 0x0A, 0x33), (0x9A, 0x5B, 0xFF)),
    (14, "garnet", (0x33, 0x0A, 0x14), (0xE0, 0x3A, 0x5C)),
    (15, "bronze", (0x33, 0x22, 0x08), (0xE0, 0xA8, 0x3C)),
]


def gradient_map_normalized(img, shadow, highlight):
    img = img.convert("RGBA")

    lums = []
    for x in range(img.width):
        for y in range(img.height):
            r, g, b, a = img.getpixel((x, y))
            if a > 0:
                lums.append(0.299 * r + 0.587 * g + 0.114 * b)
    lo, hi = min(lums), max(lums)
    span = max(1.0, hi - lo)

    out = Image.new("RGBA", img.size)
    for x in range(img.width):
        for y in range(img.height):
            r, g, b, a = img.getpixel((x, y))
            if a == 0:
                out.putpixel((x, y), (0, 0, 0, 0))
                continue
            t = (0.299 * r + 0.587 * g + 0.114 * b - lo) / span
            px = tuple(round(s + (h - s) * t) for s, h in zip(shadow, highlight))
            out.putpixel((x, y), px + (a,))
    return out


def write_set_assets(n):
    with open(f"equipment/set{n}.json", "w") as f:
        json.dump({
            "layers": {
                "humanoid": [{"texture": f"randomloot:set{n}"}],
                "humanoid_leggings": [{"texture": f"randomloot:set{n}"}],
            }
        }, f, indent=4)
        f.write("\n")

    for piece in PIECES:
        with open(f"models/item/armor/{piece}/{n}.json", "w") as f:
            json.dump({
                "parent": "minecraft:item/generated",
                "textures": {"layer0": f"randomloot:item/{piece}{n}"},
            }, f, indent=4)
            f.write("\n")


def main():
    for n, name, shadow, highlight in PALETTES:
        for piece in PIECES:
            src = Image.open(f"{SRC}/item/netherite_{piece}.png")
            gradient_map_normalized(src, shadow, highlight).save(f"textures/item/{piece}{n}.png")

        for layer_dir in ("humanoid", "humanoid_leggings"):
            src = Image.open(f"{SRC}/entity/equipment/{layer_dir}/netherite.png")
            gradient_map_normalized(src, shadow, highlight).save(
                f"textures/entity/equipment/{layer_dir}/set{n}.png")

        write_set_assets(n)

    print(f"Generated netherite-based sets 11-15 of {TOTAL_SETS}; now run gen.py for the item definitions.")


if __name__ == "__main__":
    main()
