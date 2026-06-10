"""Generates armor sets 11-15: original hand-drawn pixel art, not palette swaps.

Item sprites are authored as ASCII pixel grids below (one 16x16 grid per piece).
Worn layers are painted from scratch onto the vanilla 64x32 humanoid armor UV
layout with per-set patterns (scales, studs, facets, ribs, gold trim).

Designs are original, loosely inspired by classic fantasy armor archetypes
(CC0 references browsed on OpenGameArt: "Helmets CC0", "Armor Icons by
Equipment Slot"). No third-party pixels are copied.

Also (re)writes items/armor.json for ALL sets (recolor sets 1-10 from
gen_armor.py plus these), so run gen_armor.py first if regenerating from
scratch. Run from this directory: python3 gen_armor_unique.py
"""

import json

from PIL import Image

TOTAL_SETS = 15
PIECES = ["helmet", "chestplate", "leggings", "boots"]
PIECE_OFFSET = {"helmet": 0.5, "chestplate": 0.6, "leggings": 0.7, "boots": 0.8}


def c(hexstr, a=255):
    return (int(hexstr[0:2], 16), int(hexstr[2:4], 16), int(hexstr[4:6], 16), a)


# ---------------------------------------------------------------------------
# Item sprites: '.' transparent, '#' outline, d/m/l/h dark->highlight ramp,
# a/A accent dark/light, e emissive/special.
# ---------------------------------------------------------------------------

SETS = {
    11: {  # Dragonscale: green overlapping scales, swept horns
        "name": "dragonscale",
        "palette": {
            "#": c("12270f"), "d": c("1e4a1c"), "m": c("2f7029"), "l": c("46a03a"),
            "h": c("7cc862"), "a": c("c2a24a"), "A": c("e8d27c"), "e": c("dff0b8"),
        },
        "helmet": [
            "................",
            "..A..........A..",
            ".#a#........#a#.",
            ".#aa#......#aa#.",
            "..#a########a#..",
            "..#mllhhhllm m#.".replace(" ", ""),
            ".#mlhmmmmmhlm#..",
            ".#mlmlmlmlmlm#..",
            ".#dmlmlmlmlmd#..",
            ".#dm#######md#..",
            ".#dd#.....#dd#..",
            ".#dd#.....#dd#..",
            "..##.......##...",
            "................",
            "................",
            "................",
        ],
        "chestplate": [
            "................",
            "..###......###..",
            ".#mmm######mmm#.",
            ".#mhm#dddd#mhm#.",
            ".#mmm#mmmm#mmm#.",
            "..###llhhll###..",
            "...#mlmlmlml#...",
            "...#lmlmlmlh#...",
            "...#mlmlmlml#...",
            "...#lmlmlmlh#...",
            "...#dmlmlmld#...",
            "...#dlmlmlmd#...",
            "...#ddmlmldd#...",
            "....########....",
            "................",
            "................",
        ],
        "leggings": [
            "................",
            "...########....",
            "..#aAaAaAaA#....",
            "..#mlmlmlml#....",
            "..#lmlmlmlm#....",
            "..#mlm##mlm#....",
            "..#lml##lml#....",
            "..#mlm#.#lm#....",
            "..#dmd#.#md#....",
            "..#lml#.#ml#....",
            "..#dmd#.#dd#....",
            "..#dld#.#ld#....",
            "...###...##.....",
            "................",
            "................",
            "................",
        ],
        "boots": [
            "................",
            "................",
            "................",
            "................",
            "..####....####..",
            ".#mlm#...#mlm#..",
            ".#lml#...#lml#..",
            ".#mlm#...#mlm#..",
            ".#dmd##..#dmd##.",
            ".#dmmlm#.#dmmlm#",
            ".#ddddd#.#ddddd#",
            ".#AdAd.#.#AdAd.#",
            "..####...####...",
            "................",
            "................",
            "................",
        ],
    },
    12: {  # Studded leather: brown hide, iron rivets, buckled strap
        "name": "studded",
        "palette": {
            "#": c("2a1709"), "d": c("4a2c12"), "m": c("6e451f"), "l": c("935f2e"),
            "h": c("b87f45"), "a": c("8f9aa3"), "A": c("d7dde2"), "e": c("c9a227"),
        },
        "helmet": [
            "................",
            "................",
            "....########....",
            "...#llhhhhll#...",
            "..#lmamllmaml#..",
            "..#mllllllllm#..",
            ".#dmamllllamd#..",
            ".#dmmmmmmmmmd#..",
            ".#ddeeddddeed#..",
            ".#dm#......#d#..",
            ".#dd#......#d#..",
            "..##........##..",
            "................",
            "................",
            "................",
            "................",
        ],
        "chestplate": [
            "................",
            "..###......###..",
            ".#lll######lll#.",
            ".#lal#hhhh#lal#.",
            ".#lll#llll#lll#.",
            "..###hllllh###..",
            "...#lalllall#...",
            "...#llllllll#...",
            "...#mlallalm#...",
            "...#mmmmmmmm#...",
            "...#eeeeeeee#...",
            "...#mamllmam#...",
            "...#dmmmmmmd#...",
            "....########....",
            "................",
            "................",
        ],
        "leggings": [
            "................",
            "...########....",
            "..#eeeeeeee#....",
            "..#llllllll#....",
            "..#lallllal#....",
            "..#lll##lll#....",
            "..#mlm##mlm#....",
            "..#lal#.#al#....",
            "..#mmm#.#mm#....",
            "..#lll#.#ll#....",
            "..#mam#.#am#....",
            "..#dmd#.#md#....",
            "...###...##.....",
            "................",
            "................",
            "................",
        ],
        "boots": [
            "................",
            "................",
            "................",
            "................",
            "..####....####..",
            ".#lll#...#lll#..",
            ".#eee#...#eee#..",
            ".#lll#...#lll#..",
            ".#mll##..#mll##.",
            ".#mllll#.#mllll#",
            ".#ammma#.#ammma#",
            ".#ddddd#.#ddddd#",
            "..####...####...",
            "................",
            "................",
            "................",
        ],
    },
    13: {  # Crystal: faceted ice-violet shards, glowing core
        "name": "crystal",
        "palette": {
            "#": c("1d1440"), "d": c("3a2d7a"), "m": c("5a4fc0"), "l": c("8a7fe8"),
            "h": c("c4bcff"), "a": c("46c8e0"), "A": c("9ff0ff"), "e": c("ffffff"),
        },
        "helmet": [
            "................",
            ".....A....A.....",
            "....#a#..#a#....",
            "...#Aa#..#aA#...",
            "...#aa####aa#...",
            "..#ahhlhhlhha#..",
            "..#mhlmmmmlhm#..",
            ".#dmlm#ee#mlmd#.",
            ".#dmlm#mm#mlmd#.",
            ".#ddm######mdd#.",
            ".#dd#......#dd#.",
            "..##........##..",
            "................",
            "................",
            "................",
            "................",
        ],
        "chestplate": [
            "................",
            "..###......###..",
            ".#hmm######mmh#.",
            ".#mAm#dddd#mAm#.",
            ".#mmm#mmmm#mmm#.",
            "..###hlhhlh###..",
            "...#mhlmmlhm#...",
            "...#lmhAAhml#...",
            "...#mlhAAhlm#...",
            "...#dmlhhlmd#...",
            "...#dmmllmmd#...",
            "...#ddmlmldd#...",
            "...#dddmmddd#...",
            "....########....",
            "................",
            "................",
        ],
        "leggings": [
            "................",
            "...########....",
            "..#hlhlhlhl#....",
            "..#mhmlmlhm#....",
            "..#lmAmlAml#....",
            "..#mlm##mlm#....",
            "..#hml##lmh#....",
            "..#mAm#.#Am#....",
            "..#lml#.#ml#....",
            "..#dmd#.#md#....",
            "..#dld#.#ld#....",
            "..#ddd#.#dd#....",
            "...###...##.....",
            "................",
            "................",
            "................",
        ],
        "boots": [
            "................",
            "................",
            "................",
            "...A.......A....",
            "..#a##....#a##..",
            ".#ham#...#ham#..",
            ".#mlm#...#mlm#..",
            ".#lAl#...#lAl#..",
            ".#mlm##..#mlm##.",
            ".#dmlll#.#dmlll#",
            ".#ddmmd#.#ddmmd#",
            ".#ddddd#.#ddddd#",
            "..####...####...",
            "................",
            "................",
            "................",
        ],
    },
    14: {  # Bone: ivory skull helm, ribcage cuirass
        "name": "bone",
        "palette": {
            "#": c("2b2620"), "d": c("6e6354"), "m": c("a89a82"), "l": c("d6cab0"),
            "h": c("f3ecd8"), "a": c("433a2e"), "A": c("857663"), "e": c("1a1511"),
        },
        "helmet": [
            "................",
            "................",
            "....########....",
            "...#hhhhhhhh#...",
            "..#hllllllllh#..",
            "..#lllllllllm#..",
            ".#dl#ee#l#ee#d#.",
            ".#dl#ee#l#ee#d#.",
            ".#dmllllllllmd#.",
            ".#ddl#l#l#ldd#..",
            "..##m#m#m#m##...",
            "...##########...",
            "................",
            "................",
            "................",
            "................",
        ],
        "chestplate": [
            "................",
            "..###......###..",
            ".#mmh######hmm#.",
            ".#mlm#aaaa#mlm#.",
            ".#mmm#aaaa#mmm#.",
            "..###aalhaa###..",
            "...#alhhhhla#...",
            "...#aalllaaa#...",
            "...#alhhhhla#...",
            "...#aallllaa#...",
            "...#aalhhlaa#...",
            "...#aaallaaa#...",
            "...#aaahhaaa#...",
            "....########....",
            "................",
            "................",
        ],
        "leggings": [
            "................",
            "...########....",
            "..#hhhhhhhh#....",
            "..#mlmlmlml#....",
            "..#allllllla#..".rstrip("."),
            "..#lhl##lhl#....",
            "..#mlm##mlm#....",
            "..#lhl#.#hl#....",
            "..#aaa#.#aa#....",
            "..#lhl#.#hl#....",
            "..#mlm#.#lm#....",
            "..#dad#.#ad#....",
            "...###...##.....",
            "................",
            "................",
            "................",
        ],
        "boots": [
            "................",
            "................",
            "................",
            "................",
            "..####....####..",
            ".#hlh#...#hlh#..",
            ".#lml#...#lml#..",
            ".#aaa#...#aaa#..",
            ".#lhl##..#lhl##.",
            ".#mlhlh#.#mlhlh#",
            ".#dmmmm#.#dmmmm#",
            ".#hdhdh#.#hdhdh#",
            "..####...####...",
            "................",
            "................",
            "................",
        ],
    },
    15: {  # Gilded plate: polished silver, gold trim, ruby crest
        "name": "gilded",
        "palette": {
            "#": c("23242e"), "d": c("565a6e"), "m": c("868da0"), "l": c("b6bcc9"),
            "h": c("e6e9ef"), "a": c("c9941a"), "A": c("f2c84b"), "e": c("c83244"),
        },
        "helmet": [
            "................",
            ".......ee.......",
            "......#ee#......",
            "....###AA###....",
            "...#llAhhAll#...",
            "..#lhhlhhlhhl#..",
            "..#mllllllllm#..",
            ".#dmAAAAAAAAmd#.",
            ".#dml##ll##lmd#.",
            ".#dml##ll##lmd#.",
            ".#ddmAAAAAAmdd#.",
            "..###......###..",
            "................",
            "................",
            "................",
            "................",
        ],
        "chestplate": [
            "................",
            "..###......###..",
            ".#AAA######AAA#.",
            ".#lhl#dddd#lhl#.",
            ".#mlm#mmmm#mlm#.",
            "..###AhhhA####..",
            "...#lAhhhAll#...",
            "...#lhAeeAhl#...",
            "...#mlAeeAlm#...",
            "...#mllAAllm#...",
            "...#dmllllmd#...",
            "...#dAmllmAd#...",
            "...#ddAAAAdd#...",
            "....########....",
            "................",
            "................",
        ],
        "leggings": [
            "................",
            "...########....",
            "..#AAAAAAAA#....",
            "..#lhlhlhlh#....",
            "..#mllllllm#....",
            "..#lhl##lhl#....",
            "..#mlm##mlm#....",
            "..#lAl#.#Al#....",
            "..#mlm#.#lm#....",
            "..#lhl#.#hl#....",
            "..#mlm#.#lm#....",
            "..#dAd#.#Ad#....",
            "...###...##.....",
            "................",
            "................",
            "................",
        ],
        "boots": [
            "................",
            "................",
            "................",
            "................",
            "..####....####..",
            ".#lhl#...#lhl#..",
            ".#AAA#...#AAA#..",
            ".#mlm#...#mlm#..",
            ".#lhl##..#lhl##.",
            ".#mlhlh#.#mlhlh#",
            ".#AmmmA#.#AmmmA#",
            ".#ddddd#.#ddddd#",
            "..####...####...",
            "................",
            "................",
            "................",
        ],
    },
}


def render_sprite(grid, palette):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        row = row.ljust(16, ".")
        for x in range(16):
            ch = row[x]
            if ch != ".":
                img.putpixel((x, y), palette[ch])
    return img


# ---------------------------------------------------------------------------
# Worn layers: paint the vanilla 64x32 humanoid armor UV boxes from scratch.
# Each set provides a pattern(px, py) -> palette char for plate detail.
# ---------------------------------------------------------------------------

HEAD_FACES = {  # (x0, y0, w, h) regions of the 8x8x8 head box
    "top": (8, 0, 8, 8), "front": (8, 8, 8, 8), "right": (0, 8, 8, 8),
    "left": (16, 8, 8, 8), "back": (24, 8, 8, 8),
}
BODY_FACES = {
    "top": (20, 16, 8, 4), "front": (20, 20, 8, 12), "right": (16, 20, 4, 12),
    "left": (28, 20, 4, 12), "back": (32, 20, 8, 12),
}
ARM_FACES = {
    "top": (44, 16, 4, 4), "front": (44, 20, 4, 12), "right": (40, 20, 4, 12),
    "left": (48, 20, 4, 12), "back": (52, 20, 4, 12),
}
LEG_FACES = {
    "front": (4, 20, 4, 12), "right": (0, 20, 4, 12),
    "left": (8, 20, 4, 12), "back": (12, 20, 4, 12),
    "bottom": (8, 16, 4, 4),
}


def shade(ch, py, h):
    """Vertical light ramp: lighter rows up top, darker at the bottom."""
    if ch != "m":
        return ch
    t = py / max(1, h - 1)
    if t < 0.25:
        return "l"
    if t > 0.8:
        return "d"
    return "m"


def paint_face(img, rect, palette, pattern, rows=None, outline=True):
    x0, y0, w, h = rect
    for py in range(h):
        if rows is not None and not (rows[0] <= py <= rows[1]):
            continue
        for px in range(w):
            edge = px == 0 or px == w - 1 or py == (rows[0] if rows else 0) or py == (rows[1] if rows else h - 1)
            if outline and edge:
                ch = "#"
            else:
                ch = shade(pattern(px, py), py, h)
            img.putpixel((x0 + px, y0 + py), palette[ch])


def scales(px, py):
    return "h" if (px + (py % 4 < 2)) % 2 == 0 and py % 2 == 0 else ("d" if py % 4 == 3 else "m")


def studs(px, py):
    if py % 4 == 1 and px % 3 == 1:
        return "A"
    if py % 6 == 4:
        return "a"
    return "m"


def facets(px, py):
    v = (px * 3 + py * 5) % 8
    if v == 0:
        return "A"
    if v < 3:
        return "h"
    if v < 6:
        return "m"
    return "d"


def ribs(px, py):
    if py % 3 == 1:
        return "h"
    return "a" if py % 6 == 5 else "m"


def trim(px, py):
    if py % 5 == 0:
        return "A"
    if (px + py) % 7 == 3:
        return "h"
    return "m"


LAYER_PATTERNS = {11: scales, 12: studs, 13: facets, 14: ribs, 15: trim}


def render_layers(set_id, palette):
    pattern = LAYER_PATTERNS[set_id]
    layer1 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    layer2 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    # layer_1: helmet (head box), chestplate (body + arms), boots (lower legs).
    for rect in HEAD_FACES.values():
        paint_face(layer1, rect, palette, pattern)
    for rect in BODY_FACES.values():
        paint_face(layer1, rect, palette, pattern)
    for rect in ARM_FACES.values():
        paint_face(layer1, rect, palette, pattern)
    for name, rect in LEG_FACES.items():
        if name == "bottom":
            paint_face(layer1, rect, palette, pattern)
        else:
            paint_face(layer1, rect, palette, pattern, rows=(8, 11))

    # Eye holes so skull/visor sets read as helmets from the front.
    if set_id == 14:
        for ex in (10, 13):
            layer1.putpixel((ex, 11), palette["e"])
            layer1.putpixel((ex, 12), palette["e"])

    # layer_2: leggings (waist of body box + upper legs).
    for name, rect in BODY_FACES.items():
        if name == "top":
            continue
        paint_face(layer2, rect, palette, pattern, rows=(7, 11))
    for name, rect in LEG_FACES.items():
        if name == "bottom":
            continue
        paint_face(layer2, rect, palette, pattern, rows=(0, 9))

    return layer1, layer2


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


def write_item_definition():
    entries = []
    for piece in PIECES:
        for i in range(TOTAL_SETS):
            entries.append({
                "threshold": round(PIECE_OFFSET[piece] + i / 10000, 4),
                "model": {
                    "type": "minecraft:model",
                    "model": f"randomloot:item/armor/{piece}/{i + 1}",
                },
            })

    with open("items/armor.json", "w") as f:
        json.dump({
            "model": {
                "type": "minecraft:range_dispatch",
                "property": "randomloot:cosmetic",
                "scale": 1,
                "fallback": {"type": "minecraft:model", "model": "randomloot:item/armor"},
                "entries": entries,
            }
        }, f, indent=4)
        f.write("\n")


def main():
    for n, spec in SETS.items():
        palette = spec["palette"]
        for piece in PIECES:
            render_sprite(spec[piece], palette).save(f"textures/item/{piece}{n}.png")

        layer1, layer2 = render_layers(n, palette)
        layer1.save(f"textures/entity/equipment/humanoid/set{n}.png")
        layer2.save(f"textures/entity/equipment/humanoid_leggings/set{n}.png")

        write_set_assets(n)

    write_item_definition()
    print(f"Generated sets {min(SETS)}-{max(SETS)}; items/armor.json now covers {TOTAL_SETS} sets.")


if __name__ == "__main__":
    main()
