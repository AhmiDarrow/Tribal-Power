#!/usr/bin/env python3
"""Authored 32px sprites for Spiritgear, Spiritweave and the carried equipment, plus the worn armor layers.

Every rank is painted from the same drawing with its own fittings (thread, copper, bound light, gold)
instead of brightening the rank 0 PNG. Owns the PNGs listed in OWNED; generate_rank_textures.py and
overhaul_art.py skip those names. Run with --sheet to also write a contact sheet to build/tmp/artpass.
"""
from __future__ import annotations

import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ITEM = ROOT / "src/main/resources/assets/tribalpower/textures/item"
ARMOR = ROOT / "src/main/resources/assets/tribalpower/textures/models/armor"

INK = (17, 26, 34)
# Five tones each, dark to light, hue-shifted toward blue in shadow and toward yellow in light.
METAL = [(24, 62, 78), (38, 104, 110), (66, 156, 146), (112, 204, 180), (190, 244, 220)]
CLOTH = [(18, 48, 60), (26, 78, 86), (40, 112, 108), (66, 146, 130), (114, 184, 156)]
WOOD = [(40, 27, 31), (66, 44, 40), (98, 66, 50), (134, 96, 64), (170, 130, 88)]
LEATHER = [(34, 22, 24), (58, 36, 32), (88, 56, 42), (120, 82, 56), (152, 112, 78)]
COPPER = [(82, 42, 38), (128, 70, 48), (178, 112, 66), (218, 158, 98), (246, 210, 150)]
GOLD = [(108, 70, 38), (166, 118, 50), (218, 174, 82), (244, 214, 128), (255, 243, 196)]
BONE = [(96, 84, 78), (146, 130, 110), (194, 180, 150), (226, 216, 188), (248, 242, 224)]
GLOW = [(20, 92, 110), (34, 150, 160), (74, 219, 203), (160, 246, 232), (236, 255, 250)]
VOID = [(8, 14, 22), (12, 22, 32), (18, 34, 46), (26, 50, 62), (38, 70, 80)]
VOICES = [(132, 173, 94), (235, 132, 73), (95, 184, 217), (192, 218, 194), (166, 136, 221), (98, 209, 201)]

RANKS = [("", BONE, 0), ("_attuned", COPPER, 1), ("_bound", GLOW, 2), ("_manifested", GOLD, 3)]
TOOLS = ["spiritgear_pickaxe", "spiritgear_axe", "spiritgear_shovel", "spiritgear_blade",
         "spiritgear_shears", "spiritgear_hoe"]
WEAVE = ["spiritweave_hood", "spiritweave_robe", "spiritweave_leggings", "spiritweave_boots"]
SINGLES = ["resonance_maul", "spirit_staff", "wayfarer_satchel", "spiritweave",
           "spirit_flask", "greater_spirit_flask", "totem_wrench", "weavers_wand"]
OWNED = {n + s for n in TOOLS + WEAVE for s, _, _ in RANKS} | set(SINGLES)


def new(size=(32, 32)) -> Image.Image:
    return Image.new("RGBA", size, (0, 0, 0, 0))


def put(img, x, y, c, a=255):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), (c[0], c[1], c[2], a))


def region(img, shape, ramp, lo=1.0, hi=3.2, light=(-1, -1), bevel=True):
    """Fill a mask with a ramp: a gradient toward the light plus a one pixel lit/shadowed bevel."""
    mask = Image.new("L", img.size, 0)
    shape(ImageDraw.Draw(mask))
    m = mask.load()
    pts = [(x, y) for y in range(img.height) for x in range(img.width) if m[x, y]]
    if not pts:
        return
    proj = [x * light[0] + y * light[1] for x, y in pts]
    pmin, pmax = min(proj), max(proj)
    span = max(1, pmax - pmin)

    def inside(x, y):
        return 0 <= x < img.width and 0 <= y < img.height and m[x, y]

    for (x, y), p in zip(pts, proj):
        level = lo + (hi - lo) * (p - pmin) / span
        if bevel:
            if not inside(x + light[0], y) or not inside(x, y + light[1]):
                level += 1
            if not inside(x - light[0], y) or not inside(x, y - light[1]):
                level -= 1
        put(img, x, y, ramp[max(0, min(len(ramp) - 1, round(level)))])


def poly(points):
    return lambda d: d.polygon(points, fill=255)


def rect(x0, y0, x1, y1):
    return lambda d: d.rectangle((x0, y0, x1, y1), fill=255)


def ellipse(x0, y0, x1, y1):
    return lambda d: d.ellipse((x0, y0, x1, y1), fill=255)


def dots(img, pts, c):
    for x, y in pts:
        put(img, x, y, c)


def outline(img):
    """Tinted outline: the local colour pushed toward ink, a touch softer on the lit side."""
    src = img.copy().load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            if src[x, y][3]:
                continue
            near = []
            lit = False
            for dx, dy in ((1, 0), (0, 1), (-1, 0), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and src[nx, ny][3]:
                    near.append(src[nx, ny])
                    lit |= dx == 1 or dy == 1
            if not near:
                continue
            k = 0.30 if lit else 0.16
            c = [sum(n[i] for n in near) / len(near) for i in range(3)]
            put(img, x, y, tuple(int(INK[i] * (1 - k) + c[i] * k * 0.6) for i in range(3)))


def haft(img, x0, y0, length, ramp=WOOD, wraps=(), wrap_ramp=BONE, cap=None):
    """Three pixel diagonal haft rising to the right; wraps are step indices bound in wrap_ramp."""
    for i in range(length):
        r = wrap_ramp if i in wraps else ramp
        x, y = x0 + i, y0 - i
        for a, tone in ((-1, 3), (0, 2), (1, 1)):
            put(img, x + a, y + a, r[tone])
            put(img, x + a + 1, y + a, r[max(0, tone - 1)] if a < 1 else r[0])
        if i in wraps:
            put(img, x - 1, y - 1, r[4])
    if cap:
        for dx, dy, tone in ((-1, 0, 3), (0, 0, 2), (-1, 1, 2), (0, 1, 1), (1, 1, 1), (0, 2, 0), (-2, 1, 3), (-1, 2, 1)):
            put(img, x0 + dx, y0 + dy, cap[tone])


def hole(img, shape):
    """Clear a shape back to nothing — an open jaw or a ring's eye, not a dark blob."""
    mask = Image.new("L", img.size, 0)
    shape(ImageDraw.Draw(mask))
    m = mask.load()
    for y in range(img.height):
        for x in range(img.width):
            if m[x, y]:
                img.putpixel((x, y), (0, 0, 0, 0))


def taper(img, a, b, w0, w1, ramp, lo=1.0, hi=3.4, light=(-1, -1)):
    """A stroke from a to b whose width runs from w0 to w1 — blades, shanks and slim shafts."""
    pts = set()
    steps = 96
    for i in range(steps + 1):
        t = i / steps
        cx, cy = a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t
        r = (w0 + (w1 - w0) * t) / 2
        span = int(math.ceil(r))
        for dx in range(-span, span + 1):
            for dy in range(-span, span + 1):
                if dx * dx + dy * dy <= r * r:
                    pts.add((round(cx) + dx, round(cy) + dy))
    region(img, lambda d: d.point(sorted(pts), fill=255), ramp, lo=lo, hi=hi, light=light)
    return pts


def ring(img, cx, cy, outer, inner, ramp):
    region(img, ellipse(cx - outer, cy - outer, cx + outer, cy + outer), ramp, lo=1.4, hi=3.6)
    hole(img, ellipse(cx - inner, cy - inner, cx + inner, cy + inner))


def glint(img, x, y, c=(255, 255, 255)):
    put(img, x, y, c)
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        put(img, x + dx, y + dy, c, 150)


def fittings(rank):
    """(haft wraps, wrap ramp, pommel cap) grow with rank."""
    _, ramp, n = RANKS[rank]
    wraps = [(4,), (3, 4, 9), (3, 4, 8, 9), (2, 3, 6, 7, 10, 11)][n]
    return wraps, ramp, (None if n == 0 else ramp)


def rune(img, pts, rank):
    """The bound light: a channel cut into the head from rank 2, white hot at rank 3."""
    if rank < 2:
        return
    for i, (x, y) in enumerate(pts):
        put(img, x, y, GLOW[4] if rank == 3 and i % 2 == 0 else GLOW[3])


def flip(pts):
    """Mirror across the haft axis (x + y = 32). Vanilla handheld heads face up-left; that side leads in the hand."""
    return [(32 - y, 32 - x) for x, y in pts]


# --- Spiritgear -----------------------------------------------------------------------------------

def pickaxe(rank):
    img = new()
    wraps, ramp, cap = fittings(rank)
    haft(img, 5, 27, 17, wraps=wraps, wrap_ramp=ramp, cap=cap)
    # One slim arm is rasterised along a shallow droop; the other is its mirror across the haft.
    arm = []
    for x in range(8, 25):
        y = 6 + round(((24 - x) / 16) ** 2 * 3)
        arm += [(x, y + k) for k in ((1,) if x < 10 else (0, 1) if x < 13 else (-1, 0, 1))]
    head = arm + flip(arm) + [(x, y) for x in range(23, 28) for y in range(5, 10) if 28 <= x + y <= 36 and y - x <= -16]
    region(img, lambda d: d.point(head, fill=255), METAL, lo=1.4, hi=3.0)
    # Socket collar where the haft passes through the head.
    collar = ramp if rank else LEATHER
    region(img, poly([(19, 10), (21, 8), (24, 11), (22, 13)]), collar, lo=1.5, hi=3.5)
    if rank >= 2:
        dots(img, [(21, 10), (22, 10)], GLOW[4])
        dots(img, [(21, 11), (22, 11)], GLOW[3])
    outline(img)
    return img


def axe(rank):
    img = new()
    wraps, ramp, cap = fittings(rank)
    haft(img, 5, 27, 18, wraps=wraps, wrap_ramp=ramp, cap=cap)
    # The bit leads up-left of the haft, as on the vanilla axe, so it faces forward in the hand.
    bit = [(21, 9), (19, 5), (16, 2), (12, 2), (8, 4), (6, 8), (6, 13), (8, 17), (11, 14), (14, 13), (17, 14)]
    region(img, poly(bit), METAL, lo=0.8, hi=3.2)
    edge = [(15, 2), (14, 2), (13, 2), (12, 2), (11, 3), (10, 3), (9, 4), (8, 4), (8, 5), (7, 6), (7, 7), (6, 8), (6, 9),
            (6, 10), (6, 11), (6, 12), (6, 13), (7, 14), (7, 15), (8, 16)]
    dots(img, edge, METAL[4])
    dots(img, [(x + 1, y + 1) for x, y in edge[2:-2]], METAL[3])
    dots(img, [(11, 7), (12, 8), (13, 9), (14, 10), (15, 11)], METAL[1])
    # Poll behind the haft, then the socket binding over both.
    region(img, poly([(23, 9), (26, 11), (27, 14), (25, 16), (22, 15)]), METAL, lo=0.6, hi=2.4)
    poll = ramp if rank else LEATHER
    region(img, poly([(17, 13), (21, 8), (25, 12), (21, 17)]), poll, lo=1.4, hi=3.6)
    rune(img, [(10, 6), (9, 8), (9, 10), (9, 12), (10, 14)], rank)
    if rank == 3:
        glint(img, 12, 4)
    outline(img)
    return img


def shovel(rank):
    img = new()
    wraps, ramp, cap = fittings(rank)
    haft(img, 5, 27, 17, wraps=wraps, wrap_ramp=ramp, cap=cap)
    spade = [(17, 8), (19, 4), (23, 2), (27, 2), (30, 4), (29, 8), (27, 12), (23, 14), (20, 13)]
    region(img, poly(spade), METAL, lo=1.0, hi=3.3)
    # Pressed rib down the centre of the spade.
    dots(img, [(21, 11), (22, 10), (23, 9), (24, 8), (25, 7), (26, 6), (27, 5)], METAL[1])
    dots(img, [(21, 10), (22, 9), (23, 8), (24, 7), (25, 6), (26, 5), (27, 4)], METAL[4])
    dots(img, [(20, 4), (21, 3), (22, 3), (23, 2), (24, 2), (25, 2)], METAL[4])
    sock = ramp if rank else LEATHER
    region(img, poly([(18, 10), (20, 9), (23, 12), (21, 14), (19, 13)]), sock, lo=1.5, hi=3.5)
    rune(img, [(28, 3), (28, 5), (28, 7), (27, 9), (26, 11)], rank)
    if rank == 3:
        glint(img, 24, 4)
    outline(img)
    return img


def blade(rank):
    """Single-edged long blade with a slight sabre curve: ground edge leading (up-left), thick spine behind, runes on the flat."""
    img = new()
    _, ramp, _ = fittings(rank)
    fit = ramp if rank else METAL
    grip = [(1, 3), (1, 3), (0, 2, 4), (0, 2, 4)][rank]
    haft(img, 4, 27, 6, ramp=LEATHER, wraps=grip, wrap_ramp=fit if rank else LEATHER[1:] + [LEATHER[4]])
    # Centreline bows toward the edge; the spine runs true and the edge sweeps back to meet it at the point.
    ax, ay, bx, by = 10.5, 20.5, 27.9, 1.2
    length = math.hypot(bx - ax, by - ay)
    tx, ty = (bx - ax) / length, (by - ay) / length
    nx, ny = ty, -tx  # toward the leading edge (up-left)
    steps = 240
    curve = []
    for q in range(steps + 1):
        t = q / steps
        bow = 0.9 * math.sin(math.pi * t) - 0.8 * t * t
        curve.append((ax + tx * length * t + nx * bow, ay + ty * length * t + ny * bow, t))
    runes = {}
    for y in range(32):
        for x in range(32):
            cx, cy, t = min(curve, key=lambda c: (c[0] - x) ** 2 + (c[1] - y) ** 2)
            if (cx - x) ** 2 + (cy - y) ** 2 > 9 or t in (0.0, 1.0) and ((x - cx) * tx + (y - cy) * ty) * (1 if t else -1) > 0.3:
                continue
            d = (x - cx) * nx + (y - cy) * ny
            spine = -2.3
            edge = spine + 4.9 * min(1.0, (1 - t) / 0.34)
            if not spine <= d <= edge:
                continue
            if d > edge - 1.0:
                tone = 4
            elif d > 0.7:
                tone = 3
            elif d > -1.2:
                tone = 2
            else:
                tone = 1 if d > -1.9 else 0
            put(img, x, y, METAL[tone])
            if -1.9 < d <= 1.3 and t < 0.74:
                runes[(x, y)] = (t, d)
    # Four small runes cut into the flat, inlaid in the rank's metal or light.
    ink = [METAL[0], COPPER[3], GLOW[4], GOLD[4]][rank]
    glyphs = [[(0, 0), (0, 1), (0, 2), (1, 0)], [(0, 0), (1, 1), (0, 2)], [(0, 0), (0, 1), (0, 2), (1, 1)],
              [(1, 0), (0, 1), (1, 2)]]
    for k, glyph in enumerate(glyphs):
        t = 0.19 + k * 0.17
        cx, cy, _ = curve[round(t * steps)]
        ox, oy = round(cx - nx * 0.2), round(cy - ny * 0.2) - 1
        dots(img, [(ox + gx, oy + gy) for gx, gy in glyph if (ox + gx, oy + gy) in runes], ink)
    # Compact guard across the shoulder of the blade, boss toward the edge.
    bar = [(7, 16), (8, 17), (9, 18), (10, 19), (11, 20), (12, 21), (13, 22)]
    for i, (x, y) in enumerate(bar):
        put(img, x, y, fit[4 if i < 3 else 3])
        put(img, x + 1, y, fit[3 if i < 3 else 2])
        put(img, x, y + 1, fit[2])
        put(img, x + 1, y + 1, fit[1])
    dots(img, [(7, 15), (6, 16)], fit[3])
    dots(img, [(14, 24), (15, 23), (15, 24)], fit[1])
    boss = GLOW if rank >= 2 else fit
    dots(img, [(10, 19), (11, 19)], boss[4])
    dots(img, [(11, 20), (12, 20)], boss[2])
    # Pommel.
    for dx, dy, tone in ((0, 0, 3), (1, 0, 2), (-1, 1, 4), (0, 1, 3), (1, 1, 1), (-1, 2, 2), (0, 2, 1), (1, -1, 2), (2, 0, 1)):
        put(img, 2 + dx, 28 + dy, fit[tone])
    outline(img)
    return img


def maul():
    img = new()
    haft(img, 5, 27, 15, wraps=(3, 4, 9, 10), wrap_ramp=COPPER, cap=COPPER)
    region(img, rect(14, 3, 29, 15), METAL, lo=1.0, hi=3.0)
    region(img, rect(14, 3, 17, 15), COPPER, lo=1.4, hi=3.6)
    region(img, rect(26, 3, 29, 15), COPPER, lo=1.0, hi=3.0)
    dots(img, [(15, 5), (15, 13), (27, 5), (27, 13)], COPPER[4])
    dots(img, [(16, 6), (16, 14), (28, 6), (28, 14)], COPPER[0])
    # Resonant core seated in the face.
    region(img, ellipse(19, 6, 24, 12), VOID, lo=0, hi=1, bevel=False)
    region(img, ellipse(20, 7, 23, 11), GLOW, lo=1.5, hi=4, bevel=False)
    put(img, 21, 8, GLOW[4])
    dots(img, [(18, 4), (19, 4), (20, 4), (21, 4), (22, 4), (23, 4), (24, 4), (25, 4)], METAL[4])
    outline(img)
    return img


def staff():
    img = new()
    haft(img, 4, 28, 17, wraps=(5, 6, 12, 13), wrap_ramp=COPPER, cap=COPPER)
    region(img, ellipse(16, 2, 29, 15), COPPER, lo=1.0, hi=3.6)
    region(img, ellipse(19, 5, 26, 12), VOID, lo=0, hi=1.2, bevel=False)
    crystal = [(22, 4), (26, 8), (22, 13), (19, 8)]
    region(img, poly(crystal), GLOW, lo=0.8, hi=3.6)
    dots(img, [(22, 5), (21, 6), (22, 6), (21, 7)], GLOW[4])
    # Six voices set around the ring.
    for (x, y), c in zip([(22, 2), (28, 5), (28, 11), (23, 15), (17, 12), (17, 5)], VOICES):
        put(img, x, y, c)
    outline(img)
    return img


def satchel():
    img = new()
    region(img, lambda d: d.arc((8, 2, 23, 20), 180, 360, fill=255, width=2), LEATHER, lo=1.6, hi=3.6)
    region(img, poly([(5, 12), (26, 12), (28, 16), (28, 26), (26, 29), (5, 29), (3, 26), (3, 16)]), CLOTH, lo=0.8, hi=2.6)
    region(img, poly([(4, 11), (27, 11), (27, 17), (22, 21), (9, 21), (4, 17)]), CLOTH, lo=1.8, hi=3.8)
    for x in range(5, 27):
        y = 17 + (x - 4 if x < 9 else 27 - x if x > 22 else 4) - (1 if x < 9 or x > 22 else 0)
        put(img, x, min(y, 20), COPPER[3] if x % 3 else COPPER[4])
    dots(img, [(x, 12) for x in range(6, 26, 2)], CLOTH[4])
    region(img, rect(14, 19, 17, 24), GOLD, lo=1.4, hi=3.8)
    put(img, 15, 22, GOLD[0])
    put(img, 16, 22, GOLD[0])
    dots(img, [(6, 24), (6, 26), (25, 24), (25, 26)], LEATHER[3])
    dots(img, [(x, 27) for x in range(6, 26, 2)], CLOTH[0])
    outline(img)
    return img


def bolt():
    img = new()
    sheet = [(5, 9), (23, 4), (28, 22), (9, 28)]
    region(img, poly(sheet), CLOTH, lo=0.8, hi=3.4)
    # Warp and weft follow the skew of the sheet; copper thread every third pick.
    for y in range(32):
        for x in range(32):
            if not img.getpixel((x, y))[3]:
                continue
            u = x - (y - 4) * 4 // 18
            v = y + (x - 5) * 5 // 18
            if u % 4 == 0:
                put(img, x, y, CLOTH[4] if v % 4 < 2 else CLOTH[3])
            elif v % 6 == 0:
                put(img, x, y, COPPER[3] if u % 4 == 2 else COPPER[2])
    # Turned corner showing the paler underside.
    region(img, poly([(22, 20), (27, 19), (28, 22), (24, 25)]), CLOTH, lo=3, hi=4.4, bevel=False)
    dots(img, [(22, 21), (23, 23), (24, 25)], CLOTH[0])
    outline(img)
    return img


def shears(rank):
    """Vanilla's read: the closed blades as one round head with the seam between them, a thin grip
    curling around its lower left to the rivet, and the grey tail below the corner. The grip carries the
    rank the way vanilla's red one carries its colour."""
    img = new()
    _, ramp, _ = fittings(rank)
    fit = ramp if rank else LEATHER
    # Closed blades: a round head with a hairline of daylight between the two.
    head = set()
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).ellipse((13, 3, 28, 18), fill=255)
    m = mask.load()
    for y in range(32):
        for x in range(32):
            if m[x, y]:
                head.add((x, y))
    region(img, ellipse(13, 3, 28, 18), METAL, lo=1.2, hi=3.4)
    seam = taper(img, (16, 15), (26, 5), 2, 1, VOID, lo=0.4, hi=1.8)
    dots(img, [(x, y) for x, y in head
               if (x, y - 1) not in head and (x - 1, y) not in head and (x, y) not in seam], METAL[4])
    dots(img, [(x, y) for x, y in head
               if (x, y + 1) not in head and (x + 1, y) not in head and (x, y) not in seam], METAL[1])
    dots(img, [(22, 4), (23, 4), (24, 5)], METAL[4])
    # Grip: a thin loop curling round the lower left of the head, its mouth where the blades point.
    region(img, lambda d: d.arc((6, 4, 25, 25), 30, 276, fill=255, width=4), fit, lo=1.4, hi=3.6)
    region(img, lambda d: d.arc((8, 6, 23, 23), 36, 270, fill=255, width=1), fit, lo=3.4, hi=4.4, bevel=False)
    # The grey tail below the corner, ending in its rivet.
    taper(img, (7, 22), (5, 27), 4, 4, METAL, lo=0.8, hi=2.6)
    dots(img, [(4, 27), (5, 27)], METAL[0])
    put(img, 6, 23, METAL[4])
    # Rivet where the grip closes on the blades.
    region(img, ellipse(17, 18, 21, 22), fit, lo=1.4, hi=3.6)
    core = GLOW if rank >= 2 else fit
    put(img, 19, 20, core[4])
    put(img, 19, 21, core[1])
    rune(img, [(19, 9), (21, 7), (23, 5)], rank)
    if rank == 3:
        glint(img, 24, 4)
    outline(img)
    return img


def hoe(rank):
    """Flat blade set square across the top of the haft, edge down-left, socket binding over the joint."""
    img = new()
    wraps, ramp, cap = fittings(rank)
    haft(img, 5, 27, 17, wraps=wraps, wrap_ramp=ramp, cap=cap)
    region(img, poly([(6, 3), (19, 3), (21, 7), (20, 10), (8, 9)]), METAL, lo=1.2, hi=3.2)
    # Ground edge along the underside, and the hammered ridge above it.
    dots(img, [(x, 9) for x in range(9, 19)] + [(8, 8), (19, 9)], METAL[4])
    dots(img, [(x, 8) for x in range(10, 19)], METAL[3])
    dots(img, [(x, 5) for x in range(8, 18)], METAL[1])
    dots(img, [(x, 4) for x in range(7, 18)], METAL[3])
    sock = ramp if rank else LEATHER
    region(img, poly([(18, 8), (20, 5), (24, 9), (21, 12)]), sock, lo=1.5, hi=3.5)
    if rank >= 2:
        dots(img, [(20, 8), (21, 8)], GLOW[4])
        dots(img, [(20, 9), (21, 9)], GLOW[3])
    rune(img, [(9, 6), (11, 6), (13, 6), (15, 6), (17, 6)], rank)
    if rank == 3:
        glint(img, 12, 3)
    outline(img)
    return img


def flask(big=False):
    """A gourd bound in leather with a copper collar and a window of light down its front."""
    img = new()
    band = GOLD if big else COPPER
    body = ellipse(6, 10, 25, 29) if not big else ellipse(3, 8, 28, 30)
    region(img, body, LEATHER, lo=1.0, hi=3.4)
    # Neck and collar.
    region(img, rect(13, 3, 18, 11), LEATHER, lo=1.4, hi=3.0)
    region(img, rect(12, 2, 19, 4), band, lo=1.4, hi=3.8)
    region(img, rect(12, 8, 19, 10), band, lo=1.2, hi=3.4)
    # The window: void behind, then the fluid light inside it.
    win = rect(13, 13, 18, 26) if not big else rect(11, 12, 20, 27)
    region(img, win, VOID, lo=0, hi=1.0, bevel=False)
    inner = rect(14, 15, 17, 25) if not big else rect(12, 14, 19, 26)
    region(img, inner, GLOW, lo=1.2, hi=3.6, bevel=False)
    dots(img, [(14, 17), (14, 19)] if not big else [(13, 16), (13, 18), (13, 20)], GLOW[4])
    # Straps around the body and a stitched seam down each flank.
    for y in (14, 22) if not big else (13, 21, 27):
        region(img, rect(5 if big else 7, y, 27 if big else 24, y + 1), band, lo=1.0, hi=3.2)
    dots(img, [(8, y) for y in range(16, 26, 2)], LEATHER[4])
    dots(img, [(23, y) for y in range(16, 26, 2)], LEATHER[1])
    if big:
        dots(img, [(5, 19), (6, 17), (26, 19), (25, 17)], GOLD[4])
        for (x, y), c in zip([(9, 11), (22, 11), (9, 29), (22, 29)], VOICES[::2]):
            put(img, x, y, c)
    outline(img)
    return img


def greater_flask():
    return flask(big=True)


def wrench():
    """A ring head with its mouth open to the up-right, a plain shank and a copper-bound grip."""
    img = new()
    taper(img, (6, 28), (21, 13), 5, 4, METAL, lo=1.0, hi=3.0)
    region(img, ellipse(17, 1, 30, 14), METAL, lo=1.2, hi=3.4)
    hole(img, ellipse(20, 4, 27, 11))
    hole(img, poly([(25, 0), (33, 3), (33, 8), (26, 9)]))
    dots(img, [(21, 2), (22, 2), (20, 3), (19, 4)], METAL[4])
    dots(img, [(22, 13), (23, 13), (24, 12)], METAL[1])
    dots(img, [(19, 9), (20, 10)], METAL[1])
    # Grip: three copper wraps across the lower shank, and the rivet above them.
    for i in range(3):
        region(img, poly([(6 + i * 3, 27 - i * 3), (9 + i * 3, 24 - i * 3), (11 + i * 3, 26 - i * 3), (8 + i * 3, 29 - i * 3)]),
               COPPER, lo=1.4, hi=3.4)
    put(img, 17, 17, COPPER[4])
    put(img, 18, 18, COPPER[1])
    outline(img)
    return img


def wand():
    """A slim rod whose forked head holds a small stone caught in a weave of thread."""
    img = new()
    taper(img, (4, 29), (19, 14), 4, 3, WOOD, lo=1.0, hi=3.0)
    # Cloth bindings down the rod.
    for i, (x, y) in enumerate(((7, 26), (9, 24), (14, 19), (16, 17))):
        region(img, poly([(x, y), (x + 2, y - 2), (x + 3, y - 1), (x + 1, y + 1)]), CLOTH, lo=1.6, hi=3.8)
    # Two tines opening from the head, thread strung between them.
    taper(img, (19, 14), (22, 4), 3, 1, WOOD, lo=1.2, hi=3.0)
    taper(img, (19, 14), (29, 11), 3, 1, WOOD, lo=1.0, hi=2.6)
    for a, b in (((21, 6), (27, 11)), ((22, 8), (26, 13)), ((20, 10), (24, 14))):
        taper(img, a, b, 1, 1, CLOTH, lo=2.4, hi=4.0)
    # The stone: small, cut, and lit from within.
    region(img, poly([(23, 7), (27, 10), (24, 14), (20, 11)]), GLOW, lo=1.0, hi=3.8)
    dots(img, [(23, 9), (23, 10), (24, 9)], GLOW[4])
    put(img, 25, 12, GLOW[1])
    for (x, y), c in zip([(21, 5), (29, 11), (5, 28)], VOICES[1::2]):
        put(img, x, y, c)
    outline(img)
    return img


# --- Spiritweave icons ----------------------------------------------------------------------------

def folds(img, lines, ramp=CLOTH):
    """Fold marks only ever land on cloth that is already painted, so they cannot break the silhouette."""
    for pts, tone in lines:
        dots(img, [p for p in pts if img.getpixel(p)[3]], ramp[tone])


def emblem(img, x, y, rank):
    """Clasp: thread knot, copper stud, bound gem, gold-set gem."""
    trim = RANKS[rank][1]
    if rank == 0:
        dots(img, [(x, y), (x + 1, y)], trim[3])
        return
    dots(img, [(x - 1, y), (x + 2, y), (x, y - 1), (x + 1, y - 1), (x, y + 1), (x + 1, y + 1)], trim[2])
    core = GLOW if rank >= 2 else COPPER
    put(img, x, y, core[4])
    put(img, x + 1, y, core[3])
    if rank == 3:
        put(img, x - 1, y - 1, GOLD[4])
        put(img, x + 2, y + 1, GOLD[1])


def hood(rank):
    img = new()
    trim = RANKS[rank][1]
    # Shoulder mantle first, then the cowl over it with its peak falling back to the right.
    mantle = [(7, 19), (24, 19), (29, 24), (30, 28), (1, 28), (2, 24)]
    region(img, poly(mantle), CLOTH, lo=0.8, hi=3.0)
    cowl = [(9, 6), (13, 3), (19, 3), (24, 3), (28, 2), (27, 6), (25, 9), (25, 17), (23, 22), (8, 22), (6, 17), (6, 11)]
    region(img, poly(cowl), CLOTH, lo=1.2, hi=3.6)
    face = [(11, 10), (14, 8), (18, 8), (21, 10), (22, 14), (21, 18), (19, 21), (13, 21), (11, 18), (10, 14)]
    region(img, poly(face), VOID, lo=0, hi=2.2, light=(1, 1), bevel=False)
    # Hem of the opening carries the rank thread.
    ring = [(11, 9), (12, 8), (13, 7), (14, 7), (15, 7), (16, 7), (17, 7), (18, 7), (19, 7), (20, 8), (21, 9), (22, 10),
            (22, 12), (23, 14), (22, 16), (22, 18), (21, 19), (20, 21), (19, 22), (17, 22), (15, 22), (13, 22), (12, 21),
            (11, 19), (10, 18), (10, 16), (9, 14), (10, 12), (10, 10)]
    for i, (x, y) in enumerate(ring):
        put(img, x, y, trim[3 if x + y < 30 else 2] if rank or i % 2 == 0 else CLOTH[4])
    folds(img, [([(8, 12), (7, 14), (7, 16), (8, 19)], 1), ([(24, 10), (24, 13), (24, 16), (23, 19)], 0),
                ([(11, 5), (12, 4), (14, 3), (16, 3), (25, 3), (26, 3)], 4), ([(23, 5), (24, 6), (25, 7)], 1),
                ([(8, 23), (10, 23), (12, 23), (20, 23), (22, 23), (24, 23)], 0),
                ([(6, 25), (5, 27), (12, 26), (20, 26), (26, 25), (27, 27)], 1), ([(4, 24), (5, 23), (9, 25), (17, 26)], 3)])
    dots(img, [(x, 27) for x in range(2, 30, 1 if rank else 2)], trim[2])
    if rank == 3:
        dots(img, [(x, 27) for x in range(3, 30, 4)], GOLD[4])
    emblem(img, 15, 24, rank)
    if rank >= 2:
        dots(img, [(15, 4), (16, 4), (15, 5), (16, 5)], GLOW[3])
        put(img, 15, 4, GLOW[4])
    outline(img)
    return img


def robe(rank):
    img = new()
    trim = RANKS[rank][1]
    body = [(10, 3), (13, 5), (18, 5), (21, 3), (27, 6), (30, 12), (26, 15), (23, 12), (23, 20), (25, 29), (6, 29), (8, 20),
            (8, 12), (5, 15), (1, 12), (4, 6)]
    region(img, poly(body), CLOTH, lo=1.0, hi=3.4)
    # Shadow under the sleeves and down the right flank.
    folds(img, [([(9, 13), (9, 15), (9, 17), (22, 13), (22, 15), (22, 17)], 1),
                ([(11, 22), (11, 24), (10, 26), (15, 23), (15, 25), (15, 27), (20, 22), (21, 24), (21, 26)], 1),
                ([(12, 22), (12, 24), (16, 23), (16, 25)], 3), ([(5, 7), (6, 6), (8, 5), (22, 5), (24, 5)], 4)])
    # Collar and V of the rank thread.
    dots(img, [(13, 6), (14, 6), (15, 6), (16, 6), (17, 6), (18, 6)], VOID[2])
    for i in range(5):
        put(img, 12 + i, 7 + i, trim[3])
        put(img, 19 - i, 7 + i, trim[3 if i < 3 else 2])
        put(img, 12 + i, 8 + i, trim[1])
        put(img, 19 - i, 8 + i, trim[1])
    # Cuffs and hem.
    dots(img, [(2, 12), (3, 13), (4, 14), (5, 14), (29, 12), (28, 13), (27, 14), (26, 14)], trim[2])
    dots(img, [(x, 28) for x in range(7, 25, 1 if rank else 2)], trim[2])
    # Belt.
    belt = COPPER if rank == 1 else GOLD if rank == 3 else LEATHER
    region(img, rect(8, 18, 23, 20), belt, lo=1.2, hi=3.2)
    emblem(img, 15, 19, rank)
    if rank >= 2:
        dots(img, [(15, 14), (16, 14), (15, 15), (16, 15)], GLOW[3])
        dots(img, [(11, 24), (20, 24)], GLOW[2])
    if rank == 3:
        glint(img, 21, 8)
    outline(img)
    return img


def leggings(rank):
    img = new()
    trim = RANKS[rank][1]
    shape = [(6, 3), (25, 3), (26, 12), (27, 28), (18, 28), (17, 14), (14, 14), (13, 28), (4, 28), (5, 12)]
    region(img, poly(shape), CLOTH, lo=1.0, hi=3.4)
    band = COPPER if rank == 1 else GOLD if rank == 3 else LEATHER
    region(img, rect(6, 3, 25, 5), band, lo=1.4, hi=3.4)
    emblem(img, 15, 4, rank)
    folds(img, [([(15, 8), (16, 8), (15, 10), (16, 10), (15, 12), (16, 12)], 0),
                ([(9, 9), (9, 11), (8, 14), (22, 9), (22, 11), (23, 14)], 3),
                ([(11, 22), (11, 24), (11, 26), (24, 21), (24, 23), (24, 25)], 1)])
    # Knee wraps and the side seams take the rank thread.
    for x0 in (5, 18):
        dots(img, [(x, 18) for x in range(x0, x0 + 9)], trim[2])
        dots(img, [(x, 19) for x in range(x0, x0 + 9)], trim[1])
        put(img, x0 + 1, 18, trim[4])
    dots(img, [(6, y) for y in range(7, 17, 2)] + [(5, y) for y in range(21, 28, 2)], trim[3])
    dots(img, [(25, y) for y in range(7, 17, 2)] + [(26, y) for y in range(21, 28, 2)], trim[2])
    dots(img, [(x, 27) for x in range(5, 13)] + [(x, 27) for x in range(19, 27)], CLOTH[0])
    if rank >= 2:
        dots(img, [(9, 18), (22, 18)], GLOW[4])
    outline(img)
    return img


def boots(rank):
    img = new()
    trim = RANKS[rank][1]
    for x0, flip in ((3, False), (17, True)):
        def fx(x, x0=x0, flip=flip):
            return x0 + (11 - x if flip else x)

        shaft = [(fx(3), 5), (fx(10), 5), (fx(10), 22), (fx(3), 22)]
        region(img, poly(shaft), CLOTH, lo=1.0, hi=3.4)
        foot = [(fx(0), 21), (fx(10), 19), (fx(10), 27), (fx(0), 27)]
        region(img, poly(foot), LEATHER, lo=1.2, hi=3.4)
        for x in range(0, 11):
            put(img, fx(x), 27, LEATHER[0])
        # Cuff and cross binding.
        for x in range(3, 11):
            put(img, fx(x), 6, trim[3])
            put(img, fx(x), 7, trim[1])
        for i, y in enumerate((11, 14, 17)):
            for x in range(3, 11):
                put(img, fx(x), y + (x - 3) // 3 - 1, trim[2 if rank else 1])
        put(img, fx(2), 23, LEATHER[4])
        put(img, fx(3), 22, LEATHER[4])
        if rank >= 2:
            put(img, fx(6), 9, GLOW[4])
        if rank == 3:
            put(img, fx(1), 24, GOLD[3])
            put(img, fx(2), 24, GOLD[4])
    outline(img)
    return img


# --- Worn armor layers (vanilla 64x32 layout painted at 2x) ---------------------------------------

S = 2


def weave(px, py, h, ramp=CLOTH, base=2.3):
    level = base - 1.1 * py / max(1, h - 1)
    level += {0: -0.6, 1: -0.2, 3: 0.4, 4: 0.2}.get(px % 8, 0)
    if (px + py // 2) % 2 == 0 and py % 2 == 0:
        level += 0.25
    return ramp[max(0, min(4, round(level)))]


def face_fill(img, ux, uy, uw, uh, fn):
    for py in range(uh * S):
        for px in range(uw * S):
            c = fn(px, py, uw * S, uh * S)
            if c is not None:
                put(img, ux * S + px, uy * S + py, c)


def box_faces(u, v, w, h, d):
    """Vanilla cuboid unwrap: name -> (ux, uy, uw, uh)."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h)}


def layer_1():
    img = new((64 * S, 32 * S))
    head = box_faces(0, 0, 8, 8, 8)
    for name in ("top", "right", "left", "back"):
        face_fill(img, *head[name], lambda px, py, w, h: weave(px, py, h))

    def hood_front(px, py, w, h):
        open_face = 3 <= px <= 12 and 5 <= py <= 15 and not (py == 5 and px in (3, 12))
        if open_face:
            return None
        hem = 2 <= px <= 13 and py >= 4 and (px in (2, 13) or py == 4)
        return COPPER[3 if px < 8 else 2] if hem else weave(px, py, h)

    face_fill(img, *head["front"], hood_front)
    for name in ("right", "left", "back"):
        ux, uy, uw, uh = head[name]
        for px in range(uw * S):
            put(img, ux * S + px, (uy + uh) * S - 1, COPPER[2])
            put(img, ux * S + px, (uy + uh) * S - 2, COPPER[3] if px % 4 else COPPER[4])
    # Hat shell: only a brim over the brow, so the hood reads with depth.
    hat = box_faces(32, 0, 8, 8, 8)
    for name in ("front", "right", "left"):
        ux, uy, uw, uh = hat[name]
        for px in range(uw * S):
            for py in range(4):
                put(img, ux * S + px, uy * S + py, CLOTH[3 if py < 2 else 1])
    face_fill(img, *hat["top"], lambda px, py, w, h: weave(px, py, h, base=3.0) if py >= h - 6 else None)

    body = box_faces(16, 16, 8, 12, 4)

    def robe_front(px, py, w, h):
        if 16 <= py <= 18:
            if 6 <= px <= 9:
                return GOLD[4 if py == 16 else 2]
            return LEATHER[3 if py == 16 else 2 if py == 17 else 1]
        d = abs(px - 7.5)
        if py <= 8 and abs(d - (8 - py) * 0.75) < 0.9:
            return COPPER[3 if px < 8 else 2]
        if py <= 7 and d < (7 - py) * 0.75:
            return VOID[2]
        if py >= h - 2:
            return COPPER[3 if py == h - 2 else 1]
        return weave(px, py, h)

    def robe_back(px, py, w, h):
        if 16 <= py <= 18:
            return LEATHER[3 if py == 16 else 2 if py == 17 else 1]
        if py >= h - 2:
            return COPPER[3 if py == h - 2 else 1]
        cx, cy = px - 7.5, py - 8
        if abs(abs(cx) + abs(cy) - 4) < 0.6:
            return COPPER[3]
        if abs(cx) + abs(cy) < 1.2:
            return GLOW[3]
        return weave(px, py, h)

    def robe_side(px, py, w, h):
        if 16 <= py <= 18:
            return LEATHER[3 if py == 16 else 2 if py == 17 else 1]
        if py >= h - 2:
            return COPPER[3 if py == h - 2 else 1]
        return weave(px, py, h, base=2.0)

    face_fill(img, *body["front"], robe_front)
    face_fill(img, *body["back"], robe_back)
    face_fill(img, *body["right"], robe_side)
    face_fill(img, *body["left"], robe_side)
    face_fill(img, *body["top"], lambda px, py, w, h: weave(px, py, h, base=3.0))

    arm = box_faces(40, 16, 4, 12, 4)

    def sleeve(px, py, w, h):
        if py in (0, 1):
            return COPPER[4 if py == 0 else 2]
        if h - 6 <= py <= h - 4:
            return COPPER[3 if py == h - 6 else 2 if py == h - 5 else 1]
        if py > h - 4:
            return weave(px, py, h, base=3.2)
        return weave(px, py, h)

    for name in ("right", "front", "left", "back"):
        face_fill(img, *arm[name], sleeve)
    face_fill(img, *arm["top"], lambda px, py, w, h: weave(px, py, h, base=3.2))

    leg = box_faces(0, 16, 4, 12, 4)

    def boot(px, py, w, h):
        if py < 12:
            return None
        if py in (12, 13):
            return COPPER[3 if py == 12 else 1]
        if py >= h - 4:
            return LEATHER[3 if py == h - 4 else 2 if py < h - 1 else 0]
        if (px + py) % 4 == 0:
            return COPPER[2]
        return weave(px, py, h)

    for name in ("right", "front", "left", "back"):
        face_fill(img, *leg[name], boot)
    face_fill(img, *leg["bottom"], lambda px, py, w, h: LEATHER[1 if (px + py) % 3 else 0])
    return img


def layer_2():
    img = new((64 * S, 32 * S))
    body = box_faces(16, 16, 8, 12, 4)

    def waist(px, py, w, h):
        if py < 14:
            return None
        if py in (14, 15, 16):
            return LEATHER[3 if py == 14 else 2 if py == 15 else 1]
        return weave(px, py, h, base=3.4)

    for name in ("right", "front", "left", "back"):
        face_fill(img, *body[name], waist)
    leg = box_faces(0, 16, 4, 12, 4)

    def trouser(px, py, w, h):
        if py in (12, 13):
            return COPPER[3 if py == 12 else 1]
        if py >= h - 2:
            return CLOTH[0]
        return weave(px, py, h)

    def trouser_outer(px, py, w, h):
        if px in (3, 4) and py not in (12, 13):
            return COPPER[3 if px == 3 else 2] if py % 3 else COPPER[4]
        return trouser(px, py, w, h)

    face_fill(img, *leg["front"], trouser)
    face_fill(img, *leg["back"], trouser)
    face_fill(img, *leg["left"], trouser)
    face_fill(img, *leg["right"], trouser_outer)
    face_fill(img, *leg["top"], lambda px, py, w, h: weave(px, py, h, base=3.0))
    return img


PAINTERS = {"spiritgear_pickaxe": pickaxe, "spiritgear_axe": axe, "spiritgear_shovel": shovel, "spiritgear_blade": blade,
            "spiritgear_shears": shears, "spiritgear_hoe": hoe,
            "spiritweave_hood": hood, "spiritweave_robe": robe, "spiritweave_leggings": leggings, "spiritweave_boots": boots}
SINGLE_PAINTERS = {"resonance_maul": maul, "spirit_staff": staff, "wayfarer_satchel": satchel, "spiritweave": bolt,
                   "spirit_flask": flask, "greater_spirit_flask": greater_flask, "totem_wrench": wrench,
                   "weavers_wand": wand}


def main() -> None:
    out = {}
    for name, painter in PAINTERS.items():
        for rank, (suffix, _, _) in enumerate(RANKS):
            out[name + suffix] = painter(rank)
    for name, painter in SINGLE_PAINTERS.items():
        out[name] = painter()
    assert set(out) == OWNED
    for name, img in out.items():
        img.save(ITEM / f"{name}.png")
    layers = {"spiritweave_layer_1": layer_1(), "spiritweave_layer_2": layer_2()}
    for name, img in layers.items():
        img.save(ARMOR / f"{name}.png")
    if "--sheet" in sys.argv:
        sheet_dir = ROOT / "build/tmp/artpass"
        sheet_dir.mkdir(parents=True, exist_ok=True)
        names = [n + s for n in TOOLS + WEAVE for s, _, _ in RANKS] + SINGLES
        sheet = Image.new("RGBA", (4 * 200, (len(names) + 3) // 4 * 200), (139, 139, 139, 255))
        for i, name in enumerate(names):
            big = out[name].resize((192, 192), Image.Resampling.NEAREST)
            sheet.paste(big, (i % 4 * 200 + 4, i // 4 * 200 + 4), big)
            small = out[name].resize((16, 16), Image.Resampling.BOX) if False else out[name]
            sheet.paste(small, (i % 4 * 200 + 164, i // 4 * 200 + 164), small)
        sheet.save(sheet_dir / "gear_after.png")
        for name, img in layers.items():
            bg = Image.new("RGBA", img.size, (139, 139, 139, 255))
            bg.alpha_composite(img)
            bg.resize((img.width * 6, img.height * 6), Image.Resampling.NEAREST).save(sheet_dir / f"{name}_after.png")
    print(f"PASS: {len(out)} gear sprites, {len(layers)} armor layers")


if __name__ == "__main__":
    main()
