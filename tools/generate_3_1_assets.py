"""Authored 32px art and vanilla JSON for Tribal Power 3.1 -- The Listening Pit and the Gates.

Additive on purpose. `overhaul_art.py` regenerates the whole 3.0 catalogue and would flatten models
that were hand-tuned since it last ran, so this writes only the names 3.1 introduces, reusing that
module's Living Lattice palette and helpers so the new art sits in the same world as the old.

Idempotent: every file is derived from its own name, so re-running it produces identical bytes.

    python tools/generate_3_1_assets.py
"""
from pathlib import Path
import json
import random
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
from overhaul_art import (  # noqa: E402  -- palette and helpers are the point of the import
    INK, WOOD, STONE, COPPER, GOLD, BONE, TEAL, LIGHT, ELEMENTS,
    blank, shade, save, write, material, glyph,
)

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/tribalpower"
DATA = ROOT / "src/main/resources/data/tribalpower"
NS = "tribalpower"

# Which voice each new block speaks with, for glyphs and trim colour.
VOICE = {
    "resonance_mesh": "earth",
    "anchor_stone": "stone",
    "ritual_mark": "spirit",
    "stone_font": "earth",
    "pulse_cairn": "spirit",
    "gate_frame": "spindle",
    "gate_keystone": "spindle",
    "gate_portal": "spindle",
    "ember_horn": "fire",
    "wind_harp": "air",
    "wave_drum": "water",
    "wake_bell": "spirit",
    "loom_anchor": "spindle",
}

BLOCKS = list(VOICE)
ITEMS = ["mineral_grit", "gate_sigil", "rite_spring_calling"]


# ---- textures ---------------------------------------------------------------------------------

def block_tile(name, top=False):
    """A block face in the Living Lattice idiom: stone or wood ground, copper trim, a voice glyph."""
    voice = VOICE[name]
    colour = ELEMENTS[voice]
    woody = name in ("wave_drum", "wind_harp", "ember_horn")
    base = WOOD if woody else STONE
    if name == "anchor_stone":
        im = material(name, shade(STONE, 8))
        d = ImageDraw.Draw(im)
        # A brace, not a machine: four corner pegs and a cut line, no copper at all.
        for x, y in [(4, 4), (25, 4), (4, 25), (25, 25)]:
            d.rectangle((x - 2, y - 2, x + 2, y + 2), fill=shade(STONE, -26), outline=shade(STONE, 22))
        d.line((0, 16, 31, 16), fill=shade(STONE, -30))
        d.line((16, 0, 16, 31), fill=shade(STONE, -30))
        return im

    if name == "ritual_mark":
        im = blank()
        d = ImageDraw.Draw(im)
        rng = random.Random(name)
        # Chalk: a drawn line, deliberately imperfect, transparent where the floor shows through.
        for a in range(3, 29):
            d.point((a, 16 + (rng.randint(-1, 1))), fill=shade(BONE, -10))
            d.point((16 + rng.randint(-1, 1), a), fill=shade(BONE, -10))
        d.ellipse((8, 8, 23, 23), outline=BONE)
        for x, y in [(16, 6), (16, 25), (6, 16), (25, 16)]:
            d.rectangle((x - 1, y - 1, x + 1, y + 1), fill=shade(BONE, 18))
        return im

    if name == "gate_portal":
        im = blank()
        d = ImageDraw.Draw(im)
        # Greyscale so the block tint can colour it per destination; woven rather than swirled.
        for y in range(32):
            for x in range(32):
                v = 150 + int(38 * ((x * 3 + y * 5) % 11) / 11.0)
                d.point((x, y), fill=(v, v, v, 190))
        for a in range(0, 32, 6):
            d.line((a, 0, a, 31), fill=(230, 230, 230, 150))
            d.line((0, a, 31, a), fill=(120, 120, 120, 130))
        return im

    im = material(name + ("_top" if top else ""), base)
    d = ImageDraw.Draw(im)
    d.rectangle((0, 0, 31, 2), fill=COPPER)
    d.line((0, 0, 31, 0), fill=GOLD)
    d.rectangle((0, 29, 31, 31), fill=shade(COPPER, -30))
    d.line((0, 29, 31, 29), fill=COPPER)
    for x in [3, 28]:
        for y in [5, 26]:
            d.rectangle((x - 1, y - 1, x + 1, y + 1), fill=INK)
            d.point((x, y - 1), fill=GOLD)

    if top:
        if name == "resonance_mesh":
            # The mesh itself: a sieve you can see the weave of.
            d.rectangle((3, 3, 28, 28), fill=INK, outline=COPPER, width=2)
            for a in range(6, 28, 3):
                d.line((a, 5, a, 26), fill=shade(BONE, -30))
                d.line((5, a, 26, a), fill=shade(BONE, -55))
        elif name == "stone_font":
            d.ellipse((5, 5, 26, 26), fill=shade(STONE, -34), outline=COPPER, width=2)
            d.ellipse((9, 9, 22, 22), outline=shade(BONE, -50))
            glyph(d, "earth", ELEMENTS["earth"], size=4)
        elif name == "gate_frame":
            for a in range(6, 27, 4):
                d.line((5, a, 26, a), fill=BONE)
                d.line((a, 5, a, 26), fill=shade(BONE, -48))
        else:
            d.ellipse((4, 4, 27, 27), fill=shade(BONE, -28), outline=COPPER, width=2)
            glyph(d, voice, shade(colour, -25), size=5)
        return im

    if name == "resonance_mesh":
        d.rectangle((4, 5, 27, 26), fill=INK, outline=shade(COPPER, -32))
        for a in range(7, 26, 3):
            d.line((a, 6, a, 25), fill=shade(TEAL, -30))
        for a in range(8, 26, 4):
            d.line((5, a, 26, a), fill=shade(TEAL, -55))
    elif name == "stone_font":
        d.polygon([(8, 26), (11, 8), (20, 8), (23, 26)], fill=INK, outline=shade(COPPER, -32))
        d.polygon([(11, 24), (13, 11), (18, 11), (20, 24)], fill=shade(STONE, 14))
        glyph(d, "earth", ELEMENTS["earth"], y=17, size=5)
    elif name == "pulse_cairn":
        for i, (w, y) in enumerate([(11, 24), (9, 17), (7, 10)]):
            d.ellipse((16 - w, y - 4, 16 + w, y + 4), fill=shade(STONE, -18 + i * 10), outline=INK)
        d.polygon([(16, 4), (19, 8), (16, 12), (13, 8)], fill=ELEMENTS["spirit"])
    elif name == "gate_frame":
        for a in range(6, 27, 4):
            d.line((5, a, 26, a), fill=shade(BONE, -20))
            d.line((a, 5, a, 26), fill=shade(BONE, -55))
        d.rectangle((5, 5, 26, 26), outline=COPPER, width=2)
    elif name == "gate_keystone":
        d.rectangle((4, 5, 27, 26), fill=INK, outline=COPPER, width=2)
        d.polygon([(16, 8), (23, 16), (16, 24), (9, 16)], fill=shade(TEAL, -35), outline=TEAL)
        d.line((16, 10, 16, 22), fill=LIGHT)
        glyph(d, "spindle", LIGHT, y=16, size=4)
    elif name == "anchor_stone":
        pass
    else:
        d.rectangle((7, 6, 24, 25), fill=INK, outline=shade(COPPER, -32))
        glyph(d, voice, colour, size=6)
        if name == "ember_horn":
            for x in [11, 16, 21]:
                d.line((x, 25, x, 21), fill=ELEMENTS["fire"], width=2)
        elif name == "wind_harp":
            for x in range(9, 24, 3):
                d.line((x, 7, x, 24), fill=shade(BONE, -20))
        elif name == "wave_drum":
            d.ellipse((9, 8, 22, 23), outline=ELEMENTS["water"], width=2)
        elif name == "wake_bell":
            d.polygon([(16, 7), (23, 22), (9, 22)], fill=shade(COPPER, -20), outline=GOLD)
            d.rectangle((15, 22, 17, 25), fill=GOLD)
    return im


def item_icon(name):
    im = blank()
    d = ImageDraw.Draw(im)
    if name == "mineral_grit":
        # Greyscale, so one sprite serves every metal the tag scan discovers, tinted per material.
        for x, y in [(10, 12), (20, 11), (15, 21), (24, 23), (6, 23)]:
            d.polygon([(x, y - 4), (x + 4, y), (x + 2, y + 4), (x - 4, y + 2)], fill=(58, 58, 58, 255))
            d.polygon([(x, y - 2), (x + 2, y), (x + 1, y + 2), (x - 2, y + 1)], fill=(210, 210, 210, 255))
            d.point((x, y - 1), fill=(255, 255, 255, 255))
    elif name == "gate_sigil":
        d.polygon([(8, 2), (23, 2), (29, 8), (29, 23), (23, 29), (8, 29), (2, 23), (2, 8)], fill=INK)
        d.polygon([(9, 4), (22, 4), (27, 9), (27, 22), (22, 27), (9, 27), (4, 22), (4, 9)],
                  fill=shade(TEAL, -55), outline=GOLD)
        d.rectangle((7, 7, 24, 24), fill=INK)
        d.polygon([(16, 9), (23, 16), (16, 23), (9, 16)], outline=TEAL, width=2)
        d.line((16, 11, 16, 21), fill=LIGHT)
        d.line((11, 16, 21, 16), fill=LIGHT)
    else:  # rite_spring_calling -- a tablet, matching the other six
        d.polygon([(6, 2), (25, 2), (27, 5), (27, 27), (25, 30), (6, 30), (4, 27), (4, 5)], fill=INK)
        d.rectangle((7, 5, 24, 27), fill=shade(STONE, -18), outline=shade(COPPER, -30))
        glyph(d, "water", ELEMENTS["water"], y=15, size=6)
        d.line((10, 25, 21, 25), fill=shade(BONE, -40))
    return im


# ---- models -----------------------------------------------------------------------------------

def cuboid(name):
    """A vanilla cuboid model in the same shape the 3.0 blocks use: parts keyed to shared textures."""
    parts = []
    textures = {
        "particle": f"{NS}:block/{name}",
        "side": f"{NS}:block/{name}",
        "top": f"{NS}:block/{name}_top",
        "wood": f"{NS}:block/loom_wood",
        "stone": f"{NS}:block/loom_stone",
        "trim": f"{NS}:block/loom_copper",
        "light": f"{NS}:block/loom_light",
    }

    def box(a, b, tex):
        parts.append({"from": a, "to": b, "faces": {f: {"texture": "#" + tex} for f in
                                                    ("north", "south", "east", "west", "up", "down")}})

    def box_top(a, b, tex, up):
        faces = {f: {"texture": "#" + tex} for f in ("north", "south", "east", "west", "down")}
        faces["up"] = {"texture": "#" + up}
        parts.append({"from": a, "to": b, "faces": faces})

    if name == "resonance_mesh":
        box([0, 0, 0], [16, 2, 16], "stone")
        box([1, 2, 1], [15, 8, 15], "side")
        box_top([0, 8, 0], [16, 10, 16], "trim", "top")
    elif name == "stone_font":
        box([2, 0, 2], [14, 3, 14], "stone")
        box([5, 3, 5], [11, 9, 11], "side")
        box_top([1, 9, 1], [15, 14, 15], "side", "top")
        box([2, 13, 2], [14, 14, 14], "trim")
    elif name == "pulse_cairn":
        box([3, 0, 3], [13, 5, 13], "side")
        box([4, 5, 4], [12, 10, 12], "side")
        box([5, 10, 5], [11, 14, 11], "side")
        box([7, 14, 7], [9, 16, 9], "light")
    elif name == "gate_frame":
        box([0, 0, 0], [16, 16, 16], "side")
    elif name == "gate_keystone":
        box([0, 0, 0], [16, 16, 16], "side")
    elif name == "anchor_stone":
        box([0, 0, 0], [16, 16, 16], "side")
    elif name == "ember_horn":
        box([2, 0, 2], [14, 3, 14], "stone")
        box([4, 3, 4], [12, 12, 12], "side")
        box([3, 12, 3], [13, 14, 13], "trim")
        box([6, 14, 6], [10, 16, 10], "light")
    elif name == "wind_harp":
        # Standing folk harp: plinth, forepillar, neck, soundbox, and a fan of strings.
        box([2, 0, 5], [14, 2, 11], "stone")
        box([10, 2, 5], [14, 14, 11], "side")
        box_top([10, 14, 5], [14, 15, 11], "trim", "top")
        box([2, 2, 7], [4, 15, 9], "wood")
        box([2, 15, 7], [12, 16, 9], "wood")
        box([4, 2, 7], [10, 3.5, 9], "trim")
        box([5, 4, 9], [10, 13, 10.5], "wood")
        for i, x in enumerate((4.6, 5.8, 7.0, 8.2, 9.4)):
            box([x, 3.6 + i * 0.5, 7.7], [x + 0.35, 15.2, 8.15], "light")
    elif name == "wave_drum":
        box([2, 0, 2], [14, 2, 14], "wood")
        box([2, 2, 2], [14, 12, 14], "side")
        box_top([1, 12, 1], [15, 14, 15], "trim", "top")
    elif name == "wake_bell":
        box([3, 0, 3], [13, 2, 13], "stone")
        box([2, 12, 2], [14, 14, 14], "trim")
        box([5, 2, 5], [11, 12, 11], "side")
        box([7, 14, 7], [9, 16, 9], "trim")
    elif name == "loom_anchor":
        box([1, 0, 1], [15, 3, 15], "stone")
        box([4, 3, 4], [12, 11, 12], "side")
        box([6, 11, 6], [10, 16, 10], "light")
        box([2, 10, 2], [14, 12, 14], "trim")
    else:
        return None
    return {"parent": "minecraft:block/block", "textures": textures, "elements": parts}


def write_block(name):
    if name == "gate_portal":
        # A plane, not a cuboid: a thin translucent pane the tint handler colours per destination.
        model = {
            "parent": "minecraft:block/block",
            "render_type": "minecraft:translucent",
            "textures": {"particle": f"{NS}:block/gate_portal", "plane": f"{NS}:block/gate_portal"},
            "elements": [{
                "from": [0, 0, 7],
                "to": [16, 16, 9],
                "faces": {f: {"texture": "#plane", "tintindex": 0} for f in
                          ("north", "south", "east", "west", "up", "down")},
            }],
        }
        write(ASSETS / "models/block/gate_portal.json", model)
        write(ASSETS / "blockstates/gate_portal.json", {"variants": {"": {"model": f"{NS}:block/gate_portal"}}})
        return

    if name == "ritual_mark":
        model = {
            "parent": "minecraft:block/block",
            "render_type": "minecraft:cutout",
            "textures": {"particle": f"{NS}:block/ritual_mark", "mark": f"{NS}:block/ritual_mark"},
            "elements": [{
                "from": [0, 0, 0],
                "to": [16, 1, 16],
                "faces": {f: {"texture": "#mark"} for f in ("north", "south", "east", "west", "up", "down")},
            }],
        }
        write(ASSETS / "models/block/ritual_mark.json", model)
        write(ASSETS / "blockstates/ritual_mark.json", {"variants": {"": {"model": f"{NS}:block/ritual_mark"}}})
        return

    model = cuboid(name)
    if model is None:
        return
    write(ASSETS / f"models/block/{name}.json", model)
    write(ASSETS / f"models/item/{name}.json", {"parent": f"{NS}:block/{name}"})

    if name in ("ember_horn", "wind_harp", "wave_drum", "wake_bell", "loom_anchor"):
        # The generators carry a lit state; only the Ember Horn changes it, but the state exists on all
        # five, so every one of them needs both variants declared.
        off = dict(model)
        off["textures"] = dict(model["textures"])
        off["textures"]["light"] = f"{NS}:block/loom_stone"
        write(ASSETS / f"models/block/{name}_off.json", off)
        write(ASSETS / f"blockstates/{name}.json", {"variants": {
            "lit=false": {"model": f"{NS}:block/{name}_off"},
            "lit=true": {"model": f"{NS}:block/{name}"},
        }})
    else:
        write(ASSETS / f"blockstates/{name}.json", {"variants": {"": {"model": f"{NS}:block/{name}"}}})


# ---- loot -------------------------------------------------------------------------------------

def simple_drop(name):
    return {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": f"{NS}:{name}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    }


def generate():
    for name in BLOCKS:
        save(ASSETS / f"textures/block/{name}.png", block_tile(name))
        if name not in ("ritual_mark", "gate_portal", "anchor_stone", "gate_frame", "gate_keystone"):
            save(ASSETS / f"textures/block/{name}_top.png", block_tile(name, top=True))
        elif name in ("gate_frame",):
            save(ASSETS / f"textures/block/{name}_top.png", block_tile(name, top=True))
        write_block(name)

    for name in ITEMS:
        save(ASSETS / f"textures/item/{name}.png", item_icon(name))
        write(ASSETS / f"models/item/{name}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}})

    for name in BLOCKS:
        # The chalk mark drops nothing: the chalk was spent when the mark was made. The plane has no
        # loot table at all, which the block declares in code.
        if name in ("ritual_mark", "gate_portal"):
            continue
        write(DATA / f"loot_table/blocks/{name}.json", simple_drop(name))

    print(f"tribal-power 3.1: {len(BLOCKS)} block textures, {len(ITEMS)} item icons, models, blockstates, loot.")


if __name__ == "__main__":
    generate()
