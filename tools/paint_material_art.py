#!/usr/bin/env python3
"""Authored 32px sprites for creature reagents, grits, camp props, the Glasswing particle and the Resonant Core.

The thirteen creature reagents were one tilted crystal recoloured per creature; each is now drawn as what
it is (a moth wing, a bell shard, a tooth...). The echo catalysts left on the generic crystal fallback get
their own silhouettes too. Grits are clean ore heaps instead of chip noise. The camp
prop materials (urn, mat, charm) were 16px. Same conventions as polish_gear_art.py: five-tone hue-shifted
ramps, light from the top left, a tinted outline, alpha 0 or 255 only.

Owns the PNGs in OWNED; overhaul_art.py skips them. The Resonant Core stays a reviewed pixel map in
art/reviewed-pixel-items.json: `--reviewed` rewrites that map from the drawing here, and
tools/export_reviewed_pixel_art.py --write exports it. Run with --sheet for a review sheet in build/tmp/artpass.
"""
from __future__ import annotations

import json
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
from polish_gear_art import INK, BONE, COPPER, GOLD, GLOW, dots, ellipse, outline, poly, put, rect, region  # noqa: E402
from _repo_guard import REPO, check_inside  # noqa: E402

ROOT = REPO
TEX = ROOT / "src/main/resources/assets/tribalpower/textures"
ITEM, BLOCK, PARTICLE = TEX / "item", TEX / "block", TEX / "particle"


def ramp(*values: int) -> list[tuple[int, int, int]]:
    return [((v >> 16) & 255, (v >> 8) & 255, v & 255) for v in values]


# Five tones, dark to light: shadows cooler, lights warmer.
LAVENDER = ramp(0x2A2944, 0x4A4870, 0x76729E, 0xA99CC3, 0xDCD4EC)
FROST = ramp(0x2C3A58, 0x4C5E82, 0x7486AA, 0xA2B6D2, 0xD6E8F4)
IVORY = ramp(0x5E5046, 0x958470, 0xC4B498, 0xE4D8BC, 0xFAF4E0)
RIFT = ramp(0x2A1E44, 0x4E3A7A, 0x7C62B4, 0xB5A3FF, 0xE8E0FF)
SLATE = ramp(0x243044, 0x3A4A66, 0x55698A, 0x7F97B0, 0xB6CCD6)
PRISM = ramp(0x1A5A66, 0x2A8C92, 0x4EC2BC, 0x87FFF0, 0xE0FFF8)
BRASS = ramp(0x4A3424, 0x7A5A38, 0xB8966B, 0xD8BC8C, 0xF4E4BC)
CHAR = ramp(0x1E1618, 0x2E2426, 0x473C42, 0x625258, 0x7E6C70)
EMBER = ramp(0x6A2018, 0xAE4A2E, 0xE0803E, 0xFFB46C, 0xFFE6B0)
IMP = ramp(0x2A1A20, 0x442A30, 0x563943, 0x7A4E4C, 0x9C6656)
SILK = ramp(0x3A2E4E, 0x5E4E7A, 0x8C78A8, 0xC0A6D8, 0xF0E0FF)
TWIG = ramp(0x2E2020, 0x4A3430, 0x6E5244, 0x94725A, 0xB8967A)
ROOTWOOD = ramp(0x2A2618, 0x494735, 0x6A6A48, 0x859364, 0xAEB888)
MOSS = ramp(0x2C4A2A, 0x4A7A42, 0x74A860, 0xB1E0A0, 0xE0F8D0)
FOXFUR = ramp(0x6A3A2C, 0x9E6040, 0xCC8A58, 0xE8B480, 0xFCE0B8)
PLUM = ramp(0x2E1E34, 0x4A3050, 0x603E68, 0x7E5886, 0x9E7AA6)
TEALGLOW = ramp(0x1E6A66, 0x34A69A, 0x5AD8C4, 0x7AFFE1, 0xD8FFF4)
VELVET = ramp(0x3E4A36, 0x66744E, 0x949E6C, 0xBEC991, 0xE4ECC0)
SHELL = ramp(0x1E3030, 0x344F44, 0x55724E, 0x8EAA68, 0xC0D69A)
IRON = ramp(0x3A3638, 0x5E585A, 0x8A8280, 0xB4ACA4, 0xDCD6CC)
RUST = ramp(0x4A2418, 0x7A3C22, 0xA85A34)
MINERAL = ramp(0x2E3E4C, 0x4E6272, 0x7C909C, 0xAABCC2, 0xDCE8E8)
URN = ramp(0x3A2420, 0x5E3A2C, 0x8A5A3E, 0xB47E54, 0xD8A878)
REED = ramp(0x1E3A2E, 0x2E5642, 0x467A58, 0x6A9E70, 0x98C48E)

DROPS = ["storm_wing", "bell_fragment", "rift_tooth", "reed_fang", "prism_carapace", "sentinel_sigil",
         "ember_heart", "cinder_knot", "echo_silk", "knotted_root", "lantern_down", "dawn_velvet", "mossback_scale"]
GRITS = ["copper_grit", "gold_grit", "iron_grit", "mineral_grit"]
ECHOES = ["echo_shard", "attuned_echo", "bound_echo", "spirit_shard"]  # were the generic crystal fallback
BLOCKS = ["spirit_urn", "woven_mat", "wind_charm"]
OWNED = set(DROPS) | set(ECHOES) | set(GRITS) | set(BLOCKS) | {"resonant_core"}


def new(size=32) -> Image.Image:
    return Image.new("RGBA", (size, size), (0, 0, 0, 0))


def stroke(points, width):
    return lambda d: d.line(points, fill=255, width=width, joint="curve")


def ring(box, width):
    return lambda d: d.ellipse(box, outline=255, width=width)


def union(*shapes):
    def draw(d):
        for s in shapes:
            s(d)
    return draw


def line(img, pts, c):
    """A crisp 1px polyline (Bresenham via ImageDraw on an opaque layer)."""
    layer = new(img.width)
    ImageDraw.Draw(layer).line(pts, fill=tuple(c) + (255,), width=1)
    img.alpha_composite(layer)


def clip_line(img, pts, c):
    """Like line() but only over pixels already painted (veins, seams, cracks)."""
    layer = new(img.width)
    ImageDraw.Draw(layer).line(pts, fill=tuple(c) + (255,), width=1)
    src, lay = img.load(), layer.load()
    for y in range(img.height):
        for x in range(img.width):
            if lay[x, y][3] and src[x, y][3]:
                src[x, y] = lay[x, y]


def finish(img):
    outline(img)
    px = img.load()
    for y in range(img.height):  # alpha is 0 or 255 only
        for x in range(img.width):
            r, g, b, a = px[x, y]
            px[x, y] = (r, g, b, 255) if a >= 128 else (0, 0, 0, 0)
    return img


# ---------------------------------------------------------------- creature reagents

def storm_wing():
    """A Storm Moth wing: long pointed forewing over a rounded hindwing, veined from the root, one eyespot."""
    im = new()
    region(im, poly([(5, 27), (12, 17), (22, 15), (26, 21), (21, 28), (10, 30)]), LAVENDER, 0.4, 2.0)
    region(im, poly([(4, 26), (10, 15), (19, 7), (29, 2), (27, 9), (22, 15), (13, 21)]), LAVENDER, 1.4, 3.8)
    for tip in [(28, 3), (26, 9), (21, 15), (14, 11)]:
        clip_line(im, [(5, 25), tip], LAVENDER[1])
    for tip in [(24, 21), (18, 28)]:
        clip_line(im, [(6, 27), tip], LAVENDER[0])
    dots(im, [(27, 5), (26, 8), (24, 11), (21, 14), (17, 18), (25, 23), (22, 26), (16, 29)], LAVENDER[4])
    region(im, ellipse(15, 9, 21, 15), SLATE, 0.5, 1.5, bevel=False)
    region(im, ellipse(16, 10, 20, 14), FROST, 3.0, 4.0, bevel=False)
    put(im, 17, 11, (255, 255, 255))
    return finish(im)


def bell_fragment():
    """A shard broken from a bell: the flared lip curves along the bottom, jagged breaks meet at the top,
    and the dark inner face shows along the right-hand break."""
    im = new()
    region(im, poly([(4, 24), (8, 16), (11, 13), (13, 7), (16, 9), (19, 3), (22, 10), (26, 14), (28, 22), (22, 26), (15, 27), (9, 27)]), FROST, 1.0, 3.2)
    region(im, poly([(22, 10), (26, 14), (28, 22), (26, 21), (24, 14)]), SLATE, 0.2, 1.0, bevel=False)  # inner face
    region(im, poly([(4, 24), (9, 26), (15, 26), (22, 25), (27, 22), (28, 23), (22, 27), (15, 28), (9, 28), (4, 26)]), FROST, 2.6, 3.8, bevel=False)
    clip_line(im, [(4, 23), (9, 25), (15, 25), (22, 24), (27, 21)], FROST[0])
    clip_line(im, [(7, 19), (12, 20), (18, 20), (24, 18)], FROST[1])  # engraved band
    dots(im, [(9, 21), (13, 21), (17, 21), (21, 20)], FROST[4])
    dots(im, [(12, 12), (13, 9), (18, 5), (19, 6), (8, 18), (9, 16)], FROST[4])  # lit fracture edge
    return finish(im)


def rift_tooth():
    """A hound's canine: pointed ivory crown, gum-violet band, one long tapering root, a rift crack glowing."""
    im = new()
    region(im, poly([(11, 18), (21, 18), (20, 23), (18, 28), (16, 30), (14, 28), (12, 23)]), IVORY, 0.2, 1.6)
    region(im, poly([(17, 2), (21, 7), (23, 13), (21, 18), (11, 18), (10, 12), (13, 6)]), IVORY, 1.2, 3.8)
    region(im, rect(11, 17, 21, 19), RIFT, 1.2, 2.6, bevel=False)
    crack = [(17, 4), (16, 8), (18, 11), (16, 14), (17, 17), (16, 22), (17, 26)]
    clip_line(im, crack, RIFT[3])
    for x, y in crack[1:-1]:
        put(im, x + 1, y, RIFT[2])
    dots(im, [(14, 7), (13, 9), (12, 11)], IVORY[4])
    return finish(im)


def reed_fang():
    """A long curved marsh fang, venom-green at the tip, its root bound in reed."""
    im = new()
    fang = [(3, 25), (6, 18), (11, 12), (18, 7), (29, 3), (24, 9), (17, 14), (12, 20), (10, 27), (6, 29)]
    region(im, poly(fang), IVORY, 1.0, 3.4)
    region(im, poly([(22, 5), (29, 3), (24, 9), (21, 10)]), MOSS, 1.6, 3.4, bevel=False)
    clip_line(im, [(6, 20), (11, 14), (18, 9), (24, 6)], IVORY[4])
    # Reed binding across the root.
    for off in (0, 3):
        clip_line(im, [(3, 22 + off), (10, 25 + off)], REED[3 if off else 2])
        clip_line(im, [(3, 23 + off), (10, 26 + off)], REED[0])
    return finish(im)


def prism_carapace():
    """A crab's shell plate, segmented, with singing crystal prisms growing from the ridge."""
    im = new()
    for x0, top, w in [(6, 5, 5), (13, 2, 6), (21, 7, 5)]:
        c = x0 + w // 2
        region(im, poly([(x0, 18), (x0, top + 3), (c, top), (x0 + w, top + 3), (x0 + w, 18)]), PRISM, 1.0, 3.6)
        clip_line(im, [(c, top + 1), (c, 16)], PRISM[4])
    region(im, poly([(3, 26), (4, 20), (9, 15), (16, 13), (23, 15), (28, 20), (29, 26), (25, 24), (21, 27), (16, 25), (11, 27), (7, 24)]), SLATE, 1.0, 3.4)
    for pts in ([(9, 16), (11, 25)], [(16, 14), (16, 24)], [(23, 16), (21, 25)]):
        clip_line(im, pts, SLATE[1])
    dots(im, [(7, 19), (13, 16), (19, 16)], SLATE[4])
    return finish(im)


def sentinel_sigil():
    """The brass warding plate of a Hollow Sentinel, one watchful eye cut into its slate field."""
    im = new()
    region(im, poly([(4, 3), (27, 3), (27, 17), (22, 24), (16, 29), (10, 24), (4, 17)]), BRASS, 1.0, 3.6)
    region(im, poly([(7, 6), (24, 6), (24, 16), (20, 21), (16, 25), (12, 21), (7, 16)]), SLATE, 0.2, 1.4, bevel=False)
    region(im, poly([(9, 13), (12, 10), (16, 9), (20, 10), (23, 13), (20, 16), (16, 17), (12, 16)]), IVORY, 2.4, 3.8, bevel=False)
    region(im, ellipse(14, 10, 18, 16), PRISM, 2.0, 3.0, bevel=False)
    dots(im, [(16, 13), (16, 12)], INK)
    put(im, 15, 11, (255, 255, 255))
    dots(im, [(6, 5), (25, 5), (16, 26)], BRASS[4])
    dots(im, [(16, 19), (15, 20), (17, 20), (16, 21)], BRASS[2])
    return finish(im)


def ember_heart():
    """A heart-shaped coal from a broken fire rite, cracked open on its embers."""
    im = new()
    region(im, poly([(16, 9), (19, 5), (24, 4), (28, 7), (29, 12), (26, 18), (16, 28), (6, 18), (3, 12), (4, 7), (8, 4), (13, 5)]), CHAR, 1.0, 3.6)
    cracks = [[(16, 10), (15, 14), (17, 18), (16, 24)], [(15, 14), (10, 12), (7, 14)], [(17, 18), (22, 15), (25, 11)], [(11, 12), (10, 8)]]
    for c in cracks:
        clip_line(im, c, EMBER[2])
    for x, y in [(15, 14), (17, 18), (16, 21), (10, 12), (22, 15)]:
        put(im, x, y, EMBER[4])
        put(im, x + 1, y + 1, EMBER[1])
    dots(im, [(6, 7), (7, 6), (9, 5), (21, 6), (22, 6)], CHAR[4])
    return finish(im)


def cinder_knot():
    """An overhand knot of smouldering cord from a Cinder Imp, embers glowing in its twists."""
    im = new()
    region(im, stroke([(3, 27), (9, 21), (13, 17)], 4), IMP, 0.8, 2.4)
    region(im, ring((9, 5, 25, 21), 4), IMP, 1.0, 3.4)
    region(im, stroke([(17, 17), (22, 22), (28, 27)], 4), IMP, 1.4, 3.0)  # crosses over the loop
    for x, y in [(11, 8), (16, 5), (22, 7), (24, 13), (21, 19), (7, 23), (24, 24), (10, 15)]:
        put(im, x, y, EMBER[3])
        put(im, x + 1, y + 1, EMBER[1])
    dots(im, [(27, 26), (28, 27), (3, 27), (4, 27)], EMBER[2])  # frayed, still-burning ends
    return finish(im)


def echo_silk():
    """A hank of Echo Weaver silk hung over a twig, twisted once at the waist."""
    im = new()
    region(im, stroke([(3, 7), (29, 5)], 3), TWIG, 1.0, 3.4)
    region(im, poly([(11, 7), (21, 7), (23, 13), (20, 18), (22, 24), (19, 29), (13, 29), (10, 24), (12, 18), (9, 13)]), SILK, 1.2, 3.6)
    for x in (13, 16, 19):
        clip_line(im, [(x, 8), (x - 1, 17)], SILK[2])
        clip_line(im, [(x - 1, 19), (x, 28)], SILK[2])
    region(im, poly([(11, 17), (21, 17), (20, 20), (12, 20)]), SILK, 0.4, 1.4, bevel=False)
    dots(im, [(12, 10), (11, 12), (12, 22), (13, 25)], SILK[4])
    dots(im, [(16, 30), (17, 30)], SILK[3])
    return finish(im)


def knotted_root():
    """A gnarled root tied in a knot by a severed ley line, rootlets trailing, moss in the crook."""
    im = new()
    region(im, stroke([(3, 5), (8, 9), (11, 14)], 4), ROOTWOOD, 1.2, 3.4)
    region(im, ring((9, 8, 21, 20), 4), ROOTWOOD, 1.0, 3.2)
    region(im, stroke([(18, 18), (22, 23), (28, 27)], 5), ROOTWOOD, 0.8, 2.6)
    for pts in ([(21, 24), (19, 29)], [(25, 26), (27, 30)], [(24, 23), (29, 21)]):
        region(im, stroke(pts, 1), ROOTWOOD, 1.0, 2.0, bevel=False)
    for pts in ([(5, 7), (8, 10)], [(12, 10), (18, 10)], [(21, 22), (26, 26)]):
        clip_line(im, pts, ROOTWOOD[1])
    dots(im, [(10, 8), (11, 8), (9, 9), (20, 9), (21, 10)], MOSS[3])
    dots(im, [(10, 9), (20, 10)], MOSS[1])
    return finish(im)


def lantern_down():
    """A plume of Lantern Fox down curling up from its quill, fluffed edges, the tip lit teal."""
    im = new()
    plume = [(6, 29), (5, 23), (7, 17), (10, 12), (14, 8), (19, 5), (24, 3), (28, 4), (27, 8), (25, 7), (26, 11),
             (22, 11), (23, 15), (19, 15), (19, 19), (15, 19), (15, 23), (11, 23), (10, 27)]
    region(im, poly(plume), FOXFUR, 1.0, 3.6)
    region(im, poly([(21, 5), (24, 3), (28, 4), (27, 8), (25, 7), (26, 11), (22, 11), (21, 8)]), TEALGLOW, 2.2, 3.8, bevel=False)
    clip_line(im, [(7, 28), (9, 20), (13, 13), (19, 8), (25, 5)], FOXFUR[1])  # rachis
    for a, b in [((9, 20), (12, 22)), ((12, 15), (16, 18)), ((16, 11), (20, 14)), ((10, 17), (7, 18))]:
        clip_line(im, [a, b], FOXFUR[2])
    region(im, stroke([(6, 29), (8, 25)], 2), PLUM, 1.6, 2.8, bevel=False)  # quill
    dots(im, [(7, 19), (9, 15), (12, 11), (16, 8)], FOXFUR[4])
    return finish(im)


def dawn_velvet():
    """Antler velvet from a Dawn Stag: a forked tine in sage velvet, its tips aglow."""
    im = new()
    region(im, stroke([(8, 29), (12, 21), (15, 15)], 5), VELVET, 0.8, 2.6)
    region(im, stroke([(15, 16), (12, 10), (10, 3)], 4), VELVET, 1.4, 3.6)
    region(im, stroke([(15, 16), (21, 12), (27, 4)], 4), VELVET, 1.2, 3.2)
    region(im, stroke([(20, 13), (26, 15)], 3), VELVET, 1.0, 2.6)
    for x, y in [(10, 3), (27, 4), (26, 15)]:
        region(im, ellipse(x - 1, y - 1, x + 1, y + 1), TEALGLOW, 2.6, 3.6, bevel=False)
    # A peeled strip shows bone beneath; the burr ring at the base.
    region(im, poly([(19, 11), (23, 8), (24, 10), (20, 13)]), IVORY, 2.4, 3.6, bevel=False)
    region(im, poly([(5, 26), (12, 26), (12, 28), (5, 28)]), VELVET, 0.2, 1.2, bevel=False)
    dots(im, [(11, 8), (11, 5), (13, 19), (10, 23)], VELVET[4])
    return finish(im)


def mossback_scale():
    """A shed Mossback scute: rounded hexagon with growth rings and a living moss cap."""
    im = new()
    hexa = [(9, 5), (23, 5), (29, 15), (24, 27), (8, 27), (3, 15)]
    region(im, poly(hexa), SHELL, 1.0, 3.4)
    for s in (3, 6):
        pts = [(9 + s * .6, 5 + s), (23 - s * .6, 5 + s), (29 - s, 15), (24 - s * .6, 27 - s), (8 + s * .6, 27 - s), (3 + s, 15)]
        clip_line(im, [tuple(map(round, p)) for p in pts + pts[:1]], SHELL[1] if s == 3 else SHELL[2])
    region(im, poly([(8, 7), (12, 3), (16, 6), (20, 2), (24, 6), (22, 9), (10, 9)]), MOSS, 1.4, 3.6)
    dots(im, [(14, 14), (15, 14), (16, 15), (6, 15), (7, 13)], SHELL[4])
    return finish(im)


def echo_shard():
    """Rank 1 catalyst: one rough bone-pale shard struck from stone, a teal vein through it."""
    im = new()
    region(im, poly([(12, 29), (8, 20), (10, 11), (15, 3), (19, 6), (23, 14), (22, 23), (18, 29)]), IVORY, 1.0, 3.8)
    region(im, poly([(15, 3), (19, 6), (23, 14), (17, 12)]), IVORY, 3.4, 4.0, bevel=False)
    clip_line(im, [(15, 6), (16, 13), (14, 20), (15, 27)], PRISM[2])
    clip_line(im, [(17, 12), (19, 26)], IVORY[1])
    return finish(im)


def attuned_echo():
    """Rank 2: the shard fired in Echo Attune: a stout double-pointed amber crystal, ember heart, copper wire."""
    im = new()
    region(im, poly([(4, 27), (6, 17), (13, 9), (27, 3), (26, 14), (19, 22), (9, 28)]), EMBER, 1.0, 3.8)
    region(im, poly([(6, 17), (13, 9), (27, 3), (15, 14)]), EMBER, 3.2, 4.0, bevel=False)
    clip_line(im, [(15, 14), (6, 26)], EMBER[1])
    region(im, ellipse(14, 14, 19, 19), EMBER, 4.0, 4.0, bevel=False)
    for off in (0, 2):
        clip_line(im, [(9 + off, 12 + off), (21 + off, 24 + off)], COPPER[3 - off])
    return finish(im)


def bound_echo():
    """Rank 3: an upright violet prism bound by Echo Bind in a gold band with a water-blue clasp."""
    im = new()
    region(im, poly([(16, 2), (22, 7), (22, 25), (16, 30), (10, 25), (10, 7)]), RIFT, 1.2, 3.8)
    region(im, poly([(16, 2), (22, 7), (16, 10), (10, 7)]), RIFT, 3.6, 4.0, bevel=False)
    clip_line(im, [(16, 10), (16, 28)], RIFT[1])
    region(im, rect(7, 15, 25, 18), GOLD, 1.0, 3.6)
    region(im, rect(14, 14, 18, 19), PRISM, 2.0, 3.6)
    return finish(im)


def spirit_shard():
    """A small cluster of teal spirit crystals on a stone chip."""
    im = new()
    region(im, poly([(5, 27), (9, 24), (23, 24), (27, 28), (16, 30)]), SLATE, 0.8, 2.6)
    region(im, poly([(6, 25), (6, 14), (9, 10), (12, 14), (12, 25)]), PRISM, 0.8, 2.8)
    region(im, poly([(20, 25), (20, 12), (24, 8), (27, 12), (26, 25)]), PRISM, 0.8, 2.8)
    region(im, poly([(11, 26), (11, 9), (16, 2), (21, 9), (21, 26)]), PRISM, 1.2, 3.8)
    clip_line(im, [(16, 4), (16, 25)], PRISM[4])
    clip_line(im, [(9, 12), (9, 24)], PRISM[3])
    return finish(im)


# ---------------------------------------------------------------- grits

GRIT_RAMPS = {
    "copper_grit": (COPPER, ramp(0x3E7C6A, 0x62A890)),       # verdigris flecks
    "gold_grit": (GOLD, ramp(0x6E4A2A, 0xFFFBE8)),
    "iron_grit": (IRON, RUST),
    "mineral_grit": (MINERAL, ramp(0x2A8C92, 0x87FFF0)),
}


def grit(name):
    """A clean heap of crushed ore: one silhouette, a lit crown, a few defined grains and loose chips."""
    tones, fleck = GRIT_RAMPS[name]
    im = new()
    heap = [(3, 27), (5, 22), (8, 18), (11, 14), (14, 11), (18, 11), (21, 13), (24, 17), (27, 21), (29, 27)]
    region(im, poly(heap), tones, 0.6, 3.4)
    region(im, poly([(12, 14), (15, 11), (18, 11), (20, 14), (16, 16)]), tones, 3.0, 4.0, bevel=False)
    # Defined grains: a lit pixel over a shadow pixel, two tones apart from the heap around them.
    grains = [(9, 20), (14, 18), (19, 17), (23, 22), (12, 24), (18, 23), (7, 25), (25, 26), (16, 14)]
    for i, (x, y) in enumerate(grains):
        put(im, x, y, tones[4] if i % 3 else tones[3])
        put(im, x + 1, y, tones[3])
        put(im, x, y + 1, tones[1])
        put(im, x + 1, y + 1, tones[0])
    dots(im, [(11, 21), (21, 20), (15, 26)], fleck[0])
    dots(im, [(20, 19), (10, 17)], fleck[-1])
    # Loose chips on the ground beside the heap.
    for x, y in [(1, 28), (30, 26), (27, 29)]:
        put(im, x, y, tones[2])
        put(im, x + (1 if x < 30 else -1), y, tones[1])
    return finish(im)


# ---------------------------------------------------------------- camp prop materials (cuboid faces)

GLAZE = ramp(0x14202A, 0x1C2E38, 0x264048, 0x34585C, 0x467470)


def spirit_urn():
    """Fired clay rim and foot, teal glaze between with a zigzag band, a painted spirit star and drips."""
    im = new()
    region(im, rect(0, 0, 31, 5), URN, 1.0, 3.0, light=(0, -1))
    region(im, rect(0, 6, 31, 25), GLAZE, 1.4, 3.2, light=(0, -1), bevel=False)
    region(im, rect(0, 26, 31, 31), URN, 0.4, 2.2, light=(0, -1))
    for x in range(32):
        put(im, x, 0, URN[4] if x % 8 else URN[3])
        put(im, x, 6, GLAZE[0])
        put(im, x, 26, URN[4])
        put(im, x, 8 + (x % 4 if x % 4 < 3 else 1), URN[3])  # painted zigzag under the rim
    star = [(16, 12), (16, 13), (16, 18), (16, 19), (16, 20), (11, 16), (12, 16), (20, 16), (21, 16), (16, 11)]
    dots(im, star, TEALGLOW[3])
    dots(im, [(15, 14), (16, 14), (17, 14), (14, 15), (15, 15), (17, 15), (18, 15), (13, 16), (14, 16),
              (18, 16), (19, 16), (14, 17), (15, 17), (17, 17), (18, 17), (15, 18), (17, 18)], TEALGLOW[2])
    dots(im, [(16, 15), (16, 16), (15, 16), (17, 16), (16, 17)], TEALGLOW[4])
    for x, n in [(3, 2), (8, 1), (24, 2), (28, 1)]:  # drips over the foot
        for y in range(24, 24 + n + 1):
            put(im, x, y, TEALGLOW[1] if y < 24 + n else TEALGLOW[2])
    return im


def woven_mat():
    """Tileable 8px over-under weave: reed weft across, copper-dyed warp down."""
    im = new()
    for cy in range(4):
        for cx in range(4):
            across = (cx + cy) % 2 == 0
            tones = REED if across else URN
            for v in range(8):
                for u in range(8):
                    t = [1, 3, 2, 2, 2, 2, 1, 0][v]
                    if u in (0, 7):
                        t = max(0, t - 1)  # the strand dips under its neighbour
                    x, y = (cx * 8 + u, cy * 8 + v) if across else (cx * 8 + v, cy * 8 + u)
                    put(im, x, y, tones[t])
    return im


def wind_charm():
    """A hanging charm: cord, copper bead, a prism crystal with a lilac heart, a short tassel."""
    im = new()
    region(im, rect(15, 0, 16, 8), TWIG, 1.0, 3.0, bevel=False)
    region(im, ellipse(13, 7, 18, 11), COPPER, 1.0, 3.6)
    region(im, poly([(16, 11), (22, 17), (16, 24), (10, 17)]), PRISM, 1.0, 3.6)
    region(im, poly([(16, 15), (18, 17), (16, 20), (14, 17)]), RIFT, 2.4, 3.6, bevel=False)
    put(im, 13, 16, PRISM[4])
    for x in (14, 16, 18):
        region(im, rect(x, 25, x, 30 - (x == 16)), TWIG, 1.0, 2.0, bevel=False)
    region(im, rect(14, 24, 18, 25), COPPER, 1.6, 3.0, bevel=False)
    return finish(im)


# ---------------------------------------------------------------- Resonant Core (reviewed map)

def resonant_core():
    """Rank 4 catalyst: a gold setting with four prongs holding a teal-ringed ivory core."""
    im = new()
    region(im, ring((3, 3, 28, 28), 4), GOLD, 1.0, 3.6)
    region(im, ellipse(8, 8, 23, 23), GLOW, 0.6, 2.6)
    region(im, ellipse(11, 11, 20, 20), BONE, 1.6, 4.0)
    for x, y in [(15, 2), (15, 27), (2, 15), (27, 15)]:
        region(im, rect(x, y, x + 1, y + 2 if y in (2, 27) else y + 1), GOLD, 3.0, 4.0, bevel=False)
    region(im, rect(13, 13, 14, 14), BONE, 4.0, 4.0, bevel=False)
    dots(im, [(8, 6), (6, 8), (7, 7)], GOLD[4])
    return finish(im)


REVIEWED_KEYS = "KIiTtCWGgBbAaDdEeFfHhJjLlMmNnOoPpQqRrSsUuVvXxYyZz0123456789"


def write_reviewed(img):
    """Store the drawing as a 32px hand-editable pixel map sheet in art/reviewed-pixel-items.json."""
    path = ROOT / "art/reviewed-pixel-items.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    data["sprites"].pop("resonant_core", None)
    palette = dict(data["palette"])
    lookup = {v.lower(): k for k, v in palette.items()}
    box = img.getbbox()
    crop = img.crop(box)
    rows = []
    for y in range(crop.height):
        row = ""
        for x in range(crop.width):
            r, g, b, a = crop.getpixel((x, y))
            hexa = "00000000" if a == 0 else f"{r:02x}{g:02x}{b:02x}ff"
            if hexa not in lookup:
                key = next(k for k in REVIEWED_KEYS if k not in palette)
                palette[key] = hexa
                lookup[hexa] = key
            row += lookup[hexa]
        rows.append(row)
    data["palette"] = palette
    sheets = [s for s in data.get("additional_sheets", []) if "resonant_core" not in s.get("sprites", {})]
    sheets.append({"description": "Native 32px reviewed item maps, painted by tools/paint_material_art.py and hand-editable here.",
                   "size": 32, "destination": data["destination"], "sprites": {"resonant_core": rows}})
    data["additional_sheets"] = sheets
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


# ---------------------------------------------------------------- main

def save(img, folder, name):
    target = check_inside(folder / f"{name}.png", ROOT)
    img.save(target)
    return img


def main():
    painted = []
    for name in DROPS + ECHOES:
        painted.append((name, save(globals()[name](), ITEM, name)))
    for name in GRITS:
        painted.append((name, save(grit(name), ITEM, name)))
    for name in BLOCKS:
        painted.append((name, save(globals()[name](), BLOCK, name)))
    if "--reviewed" in sys.argv:
        core = resonant_core()
        write_reviewed(core)
        painted.append(("resonant_core", core))
        print("resonant_core map written; run tools/export_reviewed_pixel_art.py --write")
    if "--sheet" in sys.argv:
        out = ROOT / "build/tmp/artpass"
        out.mkdir(parents=True, exist_ok=True)
        cols = 8
        sheet = Image.new("RGBA", (cols * 136, math.ceil(len(painted) / cols) * 136), (128, 128, 128, 255))
        for i, (_, img) in enumerate(painted):
            sheet.alpha_composite(img.resize((128, 128), Image.NEAREST), ((i % cols) * 136 + 4, (i // cols) * 136 + 4))
        sheet.save(out / "material_art.png")
    print(f"painted {len(painted)} material sprites")


if __name__ == "__main__":
    main()
