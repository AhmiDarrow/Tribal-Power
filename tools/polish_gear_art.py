#!/usr/bin/env python3
"""Authored 32px sprites for Spiritgear, Spiritweave and the carried equipment, plus the worn armor layers.

Every rank is painted from the same drawing with its own fittings (thread, copper, bound light, gold)
instead of brightening the rank 0 PNG. Owns the PNGs listed in OWNED; generate_rank_textures.py and
overhaul_art.py skip those names. Run with --sheet to also write a contact sheet to build/tmp/artpass.
"""
from __future__ import annotations

import math
import random
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
         "spiritgear_shears", "spiritgear_hoe",
         "spiritgear_spear", "spiritgear_halberd", "spiritgear_battle_axe", "spiritgear_warhammer",
         "spiritgear_dagger", "spiritgear_scythe", "spiritgear_greatsword", "spiritgear_trident",
         "spiritgear_rattle"]
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
    haft(img, 4, 27, 6, ramp=BONE, wraps=grip, wrap_ramp=fit if rank else LEATHER)
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


# --- Spiritgear weapons ---------------------------------------------------------------------------

def frame(hx, hy):
    """Map (along, across) to pixels around a head centred on the haft: along runs up the haft (up-right),
    across runs out to the leading side (up-left). Negative across is the trailing side."""
    k = 0.7071
    return lambda pts: [(round(hx + a * k - b * k), round(hy - a * k - b * k)) for a, b in pts]


def collar(img, hx, hy, rank):
    at = frame(hx, hy)
    region(img, poly(at([(-1.5, -2.5), (1.5, -2.5), (1.5, 2.5), (-1.5, 2.5)])), RANKS[rank][1] if rank else LEATHER, lo=1.5, hi=3.5)


def long_wraps(rank, length):
    """The rank's haft bindings spread along a longer haft: the usual set near the hand, one more below the head.
    Weapon hafts are bone, so a plain piece is bound in leather and a ranked one in its rank's metal."""
    wraps, ramp, cap = fittings(rank)
    k = length / 17
    spread = tuple(sorted({round(w * k * 0.6) for w in wraps} | ({length - 3} if rank else set())))
    bind = ramp if rank else LEATHER
    return spread, bind, bind


def stroke(img, pts, widths, ramp, lo=1.0, hi=3.6):
    """A tapering stroke along a polyline of pixel points: one taper per segment, widths per point."""
    for (a, wa), (b, wb) in zip(zip(pts, widths), zip(pts[1:], widths[1:])):
        taper(img, a, b, wa, wb, ramp, lo=lo, hi=hi)


def spear(rank):
    """A long pole: at the head a long slim blade that runs true from the collar and curves out toward its point,
    a teardrop cut through its root, and a broad fin behind it hooking back toward the pole with a round hole;
    collars at both ends, bindings near both ends and a conical spike on the butt."""
    img = new((64, 64))
    wraps, ramp, cap = long_wraps(rank, 34)
    haft(img, 7, 57, 34, ramp=BONE, wraps=wraps + (4, 5, 6, 27, 28, 29), wrap_ramp=ramp)
    at = frame(41, 23)
    fit = ramp if rank else METAL
    length = 30

    def c(a):
        return 0.00028 * a ** 3

    def width(a):
        return 1.3 + 1.1 * math.sin(math.pi * min(a, length - 5) / (length - 2))

    lead = [(a, c(a) + width(a) * (1 if a < length - 5 else (length - a) / 5)) for a in range(0, length + 1)]
    back = [(a, c(a) - (1.2 if a < length - 8 else 1.2 * (length - a) / 8)) for a in range(length, -1, -1)]
    region(img, poly(at(lead + back)), METAL, lo=1.0, hi=3.6)
    dots(img, at([(a, c(a) + width(a) - 0.6) for a in range(3, length - 4)]), METAL[4])     # the edge
    dots(img, at([(a, c(a) - 0.5) for a in range(4, length - 7)]), METAL[1])               # the spine
    # A teardrop cut through the blade's root.
    for a, r in ((4.5, 0.9), (6, 0.7), (7.2, 0.5)):
        x, y = at([(a, c(a) + 0.3)])[0]
        hole(img, ellipse(x - r, y - r, x + r, y + r))
    # The fin: out from the collar on the trailing side, hooking back toward the pole.
    fin = [(0.5, -1.2), (3, -4), (4.5, -8), (3.5, -11.5), (0.5, -14), (-4.5, -15),
           (-2, -12.5), (-0.5, -9.5), (-1, -6), (-2.5, -1.2)]
    region(img, poly(at(fin)), METAL, lo=0.8, hi=3.4)
    dots(img, at([(3.6, -5), (4, -8), (3, -11), (0.5, -13.4), (-3, -14.5)]), METAL[4])
    x, y = at([(1.2, -6.5)])[0]
    hole(img, ellipse(x - 1.4, y - 1.4, x + 1.4, y + 1.4))
    region(img, poly(at([(-3, -1.8), (0.5, -1.8), (0.5, 1.8), (-3, 1.8)])), fit, lo=1.2, hi=3.6)   # head collar
    rune(img, at([(a, c(a) + 0.6) for a in (11, 14, 17, 20)]), rank)
    # Butt collar and a conical spike.
    stroke(img, [(8, 56), (5, 59), (2, 62)], [3.2, 2.4, 0.6], fit, lo=1.2, hi=3.6)
    if rank == 3:
        glint(img, *at([(16, c(16) + 1.6)])[0])
    outline(img)
    return img


def crescent_blade(centre_b, radius, top, bottom, thick):
    """A crescent in (along, across) terms: the outer arc of a circle about (0, centre_b) from `top` degrees down
    to `bottom`, with an inner line that is `thick` deep at the middle and meets the outer arc at the horns."""
    outer, inner = [], []
    for d in range(top, bottom - 1, -4):
        t = math.radians(d)
        span = top if d >= 0 else -bottom
        depth = thick * (1 - (d / span) ** 2)
        outer.append((radius * math.sin(t), centre_b + radius * math.cos(t)))
        inner.append(((radius - depth) * math.sin(t), centre_b + (radius - depth) * math.cos(t)))
    return outer + inner[::-1]


def halberd(rank):
    """A great poleaxe: a broad crescent on the leading side, its lower horn reaching down the pole and its
    upper horn arcing back over the top, carried on a scrolled arm; a short spike behind and a capped butt."""
    img = new((60, 60))
    wraps, ramp, cap = long_wraps(rank, 40)
    haft(img, 3, 57, 40, ramp=BONE, wraps=wraps, wrap_ramp=ramp, cap=cap)
    at = frame(41, 19)
    fit = ramp if rank else LEATHER
    region(img, poly(at(crescent_blade(-6, 20, 80, -60, 7.0))), METAL, lo=0.8, hi=3.4)
    dots(img, at([(19.4 * math.sin(math.radians(d)), -6 + 19.4 * math.cos(math.radians(d))) for d in range(76, -57, -4)]), METAL[4])
    dots(img, at([(15.5 * math.sin(math.radians(d)), -6 + 15.5 * math.cos(math.radians(d))) for d in range(40, -30, -8)]), METAL[1])
    # The scrolled arm carrying the blade, the spike behind, a short point above.
    region(img, poly(at([(-3, 1.2), (3, 1.2), (4.5, 5), (1, 7.5), (-4, 6.5), (-2, 4)])), fit, lo=1.0, hi=3.4)
    region(img, poly(at([(-1, -1.4), (0.5, -4.5), (3.5, -7), (3, -3.5), (3.5, -1.4)])), METAL, lo=0.6, hi=2.8)
    taper(img, at([(3, 0)])[0], at([(9, 0)])[0], 2.6, 0.6, METAL, lo=1.2, hi=3.6)
    region(img, poly(at([(-5, -1.9), (4, -1.9), (4, 1.9), (-5, 1.9)])), fit, lo=1.2, hi=3.6)
    rune(img, at([(15.5 * math.sin(math.radians(d)), -6 + 17 * math.cos(math.radians(d))) for d in (30, 10, -10, -30)]), rank)
    for dx, dy, tone in ((0, 0, 3), (-1, 1, 2), (1, 0, 2), (0, 1, 1), (-1, 0, 3)):
        put(img, 3 + dx, 57 + dy, fit[tone])
    if rank == 3:
        glint(img, *at([(4, 10)])[0])
    outline(img)
    return img


DARK = [VOID[2], VOID[3], METAL[0], METAL[1], METAL[2]]


# One swept bit, in (along, across) terms with the haft as the along axis: the outer edge runs from the upper
# horn (reaching up toward the point) out to the widest belly and back down to the lower horn, which reaches
# down toward the hand. The inner line returns through a fin that points back down the haft.
SWEPT_EDGE = [(16, 11), (12, 13.5), (7, 15), (2, 15.5), (-3, 15.2), (-8, 14.4), (-13, 13)]
SWEPT_INNER = [(-10, 9.5), (-12.5, 6), (-8, 5.5), (-3, 2.2), (3, 2.2), (6.5, 5.5), (10.5, 5), (10, 8.5)]
SWEPT_HOLES = [(8, 9.6, 0.9), (4.5, 10.8, 1.2), (0, 11.2, 1.8)]


def swept(pts, scale, sign):
    return [(a * scale, b * scale * sign) for a, b in pts]


def battle_axe(rank):
    """A long two-hander after the swept double-bit style: twin crescents, each a dark body behind a wide
    bright edge, three cut-outs through it and a fin pointing back at the haft; a barbed point above, a slim
    socket, bone haft and a spiked butt."""
    img = new((64, 64))
    wraps, ramp, cap = long_wraps(rank, 38)
    haft(img, 5, 59, 38, ramp=BONE, wraps=wraps, wrap_ramp=ramp, cap=cap)
    at = frame(40, 25)
    fit = ramp if rank else DARK
    # The point first, so the bits sit over its root.
    region(img, poly(at([(4, -1.4), (17, -3.6), (27, 0), (17, 3.6), (4, 1.4)])), METAL, lo=1.2, hi=3.6)
    dots(img, at([(k, 0) for k in range(8, 26)]), METAL[4])
    for scale, sign in ((1.25, 1), (1.1, -1)):
        region(img, poly(at(swept(SWEPT_EDGE + SWEPT_INNER, scale, sign))), DARK, lo=0.6, hi=3.2)
        band = SWEPT_EDGE + [(a, b - 3.2) for a, b in reversed(SWEPT_EDGE[1:-1])]
        region(img, poly(at(swept(band, scale, sign))), METAL, lo=2.0, hi=4.0)
        dots(img, at(swept([(a, b - 0.6) for a, b in SWEPT_EDGE[1:-1]], scale, sign)), METAL[4])
        for a, b, r in SWEPT_HOLES:
            x, y = at(swept([(a, b)], scale, sign))[0]
            r *= scale
            hole(img, ellipse(x - r, y - r, x + r, y + r))
    # A slim socket over the haft, and two small hooks under the head.
    region(img, poly(at([(-5, -1.8), (5, -1.8), (5, 1.8), (-5, 1.8)])), fit, lo=1.2, hi=3.6)
    for sign in (1, -1):
        region(img, poly(at([(-9, 1.4 * sign), (-6.5, 4.2 * sign), (-5.5, 2.6 * sign), (-7.5, 1.4 * sign)])), fit, lo=1.0, hi=3.4)
    rune(img, at(swept([(6, 8), (2.5, 8.6), (-1, 8.6), (-4.5, 8.2)], 1.25, 1)), rank)
    for dx, dy, tone in ((0, 0, 3), (-1, 1, 2), (1, -1, 2), (-2, 2, 1), (-1, 0, 3), (0, 1, 1)):
        put(img, 5 + dx, 59 + dy, fit[tone])
    if rank == 3:
        glint(img, *at([(3, 17)])[0])
    outline(img)
    return img


def warhammer(rank):
    """A big block, gently waisted and flared at both ends, hammered and inlaid with a gold border and scrolls;
    a spike rising from its top and a long spike straight out the back; a short collar, a bone haft banded with
    rings, and a spiked butt."""
    img = new((52, 52))
    fit = RANKS[rank][1] if rank else GOLD
    haft(img, 5, 47, 22, ramp=BONE, wraps=(2, 3, 8, 13, 18, 21), wrap_ramp=fit)
    at = frame(34, 18)
    stroke(img, at([(6, 0), (10.5, 0), (15, 0)]), [3.6, 2.4, 0.3], METAL, lo=1.0, hi=3.6)       # the top spike
    stroke(img, at([(1, -7), (1, -11.5), (1, -16)]), [3.2, 2.0, 0.3], METAL, lo=0.9, hi=3.6)    # the back spike
    region(img, poly(at([(-0.2, -7.4), (2.2, -7.4), (2.2, -9), (-0.2, -9)])), fit, lo=1.2, hi=3.6)
    body = [(7, 11), (6.3, 6), (6, 2), (6, -3), (7, -8), (-7, -8), (-6, -3), (-6, 2), (-6.3, 6), (-7, 11)]
    region(img, poly(at(body)), METAL, lo=0.7, hi=3.0)
    region(img, poly(at([(7, 11), (6.6, 9.2), (-6.6, 9.2), (-7, 11)])), METAL, lo=2.2, hi=3.9)   # the striking face
    region(img, poly(at([(7, -8), (6.6, -6.8), (-6.6, -6.8), (-7, -8)])), METAL, lo=0.3, hi=1.6)  # the back plate
    rng = random.Random(7331 + rank)
    for _ in range(40):                                                                     # hammered finish
        a, b = rng.uniform(-5, 5), rng.uniform(-6, 8.5)
        x, y = at([(a, b)])[0]
        put(img, x, y, METAL[rng.choice((0, 1, 1, 3))])
    border = ([(4.6, b) for b in range(-5, 9)] + [(-4.6, b) for b in range(-5, 9)]
              + [(a, -5.6) for a in range(-4, 5)] + [(a, 8.2) for a in range(-4, 5)])
    dots(img, at(border), fit[3])
    for cb in (-1.5, 3.5):
        dots(img, at([(1.5, cb), (2.3, cb + 1), (1.5, cb + 2), (0.5, cb + 1.2), (-0.5, cb + 0.4), (-1.8, cb + 0.8)]), fit[4])
    rune(img, at([(-2.5, 0), (-2.5, 2.5), (-2.5, 5), (-2.5, 7)]), rank)
    region(img, poly(at([(-10, -2), (-6, -2), (-6, 2), (-10, 2)])), METAL, lo=1.0, hi=3.4)     # the collar
    dots(img, at([(-8, -1.3), (-8, 0), (-8, 1.3)]), fit[3])
    stroke(img, [(6, 46), (4, 48), (2, 50)], [3.0, 2.0, 0.4], METAL, lo=1.2, hi=3.6)            # butt spike
    if rank == 3:
        glint(img, *at([(4.5, 10)])[0])
    outline(img)
    return img


def trident(rank):
    """Three points: two tall side blades rising from a crescent whose ends curl down into horns, a central
    barbed point on a slim shank above them, gold scrollwork, and a spiral-carved bone pole."""
    img = new((60, 60))
    _, ramp, cap = long_wraps(rank, 34)
    spiral = [BONE[0], BONE[1], BONE[1], BONE[2], BONE[3]]
    haft(img, 3, 57, 34, ramp=BONE, wraps=tuple(range(2, 32, 3)), wrap_ramp=spiral, cap=cap)
    at = frame(37, 23)
    fit = ramp if rank else GOLD
    # The crescent: flat on top, curling down into horns at both ends.
    region(img, poly(at([(-1, -9.5), (3, -9.5), (3, 9.5), (-1, 9.5), (-5.5, 10.5), (-3, 7), (-2.5, 3), (-3, 0),
                         (-2.5, -3), (-3, -7), (-5.5, -10.5)])), METAL, lo=0.8, hi=3.2)
    for sign in (1, -1):
        # A side blade, inner edge straight, outer edge bellied, point leaning in.
        region(img, poly(at([(2, 4.5 * sign), (2, 9.5 * sign), (9, 9.6 * sign), (15, 8.8 * sign), (21, 6.2 * sign),
                             (16, 5.6 * sign), (9, 5 * sign)])), METAL, lo=0.8, hi=3.4)
        dots(img, at([(a, 8.8 * sign) for a in range(4, 15)]), METAL[4])
        rune(img, at([(6, 7 * sign), (9, 7.2 * sign), (12, 7 * sign)]), max(rank, 2) if rank else 0)
        dots(img, at([(1, 6 * sign), (0.5, 8 * sign)]), fit[3])
    # The central shank and barbed point.
    taper(img, at([(1, 0)])[0], at([(13, 0)])[0], 3.4, 1.8, METAL, lo=1.2, hi=3.4)
    region(img, poly(at([(12, -1.1), (15, -3), (16.5, -1.3), (27.5, 0), (16.5, 1.3), (15, 3), (12, 1.1)])), METAL, lo=1.2, hi=3.6)
    dots(img, at([(k, 0) for k in range(14, 26)]), METAL[4])
    region(img, poly(at([(-2, -2.2), (1.5, -2.2), (1.5, 2.2), (-2, 2.2)])), fit, lo=1.2, hi=3.6)
    for dx, dy, tone in ((0, 0, 3), (-1, 1, 2), (1, 0, 2), (0, 1, 1), (-1, 0, 3)):
        put(img, 3 + dx, 57 + dy, fit[tone])
    if rank == 3:
        glint(img, *at([(20, 0)])[0])
    outline(img)
    return img


def dagger(rank):
    """A kukri: straight from the bolster for a third of its length, then angled hard forward and swelling into
    a heavy belly, the point on the line of the spine. Edge on the inside of the bend, a notch at its root, a
    bone grip with a flared butt."""
    img = new()
    _, ramp, _ = fittings(rank)
    fit = ramp if rank else METAL
    haft(img, 5, 27, 6, ramp=BONE, wraps=(2,), wrap_ramp=fit if rank else LEATHER)
    at = frame(11, 21)

    def bend(a):
        return 0.0 if a < 6 else (a - 6) * 0.42

    def belly(a):
        return 1.4 if a < 5 else 1.4 + 3.2 * math.sin(math.pi * min(1.0, (a - 5) / 13) * 0.85)

    length = 19
    spine = [(a, bend(a) - (1.5 if a < 15 else 1.5 * (length - a) / 4)) for a in range(0, length + 1)]
    edge = [(a, bend(a) + belly(a) * (1 if a < 16 else (length - a) / 3)) for a in range(length, -1, -1)]
    region(img, poly(at(spine + edge)), METAL, lo=1.0, hi=3.6)
    dots(img, at([(a, bend(a) + belly(a) - 0.6) for a in range(4, 16)]), METAL[4])     # the ground edge
    dots(img, at([(a, bend(a) - 0.9) for a in range(1, 15)]), METAL[1])                 # the spine
    x, y = at([(1.4, 1.2)])[0]
    hole(img, ellipse(x - 0.7, y - 0.7, x + 0.7, y + 0.7))                            # the notch
    region(img, poly(at([(-1.2, -2.6), (0.8, -2.6), (0.8, 2.8), (-1.2, 2.8)])), fit, lo=1.2, hi=3.6)   # bolster
    for dx, dy, tone in ((0, 0, 3), (-1, 0, 2), (0, 1, 1), (-1, 1, 2), (1, 1, 1), (-2, 1, 3)):
        put(img, 4 + dx, 28 + dy, BONE[tone])                                         # flared butt
    rune(img, at([(8, bend(8) + 0.8), (11, bend(11) + 1.2), (14, bend(14) + 1.4)]), rank)
    if rank == 3:
        glint(img, *at([(12, bend(12) + 2.6)])[0])
    outline(img)
    return img


HORN = [(28, 20, 22), (58, 40, 36), (92, 66, 52), (128, 96, 70), (170, 136, 98)]
FEATHER = [(120, 96, 60), (164, 132, 84), (204, 174, 116), (232, 208, 150), (248, 236, 196)]


def scythe(rank):
    """A bone snath with a braided grip and a pointed butt; at the head a long heavy blade reaching up and
    forward then hooking down to a needle point, edge bright on its inside and teeth along its spine; behind,
    a banded horn curving up, with feathers and a bead tied at the neck."""
    img = new((56, 56))
    wraps, ramp, cap = long_wraps(rank, 34)
    braid = tuple(range(3, 16))
    haft(img, 6, 51, 34, ramp=BONE, wraps=braid + (18, 30), wrap_ramp=ramp if rank else GOLD)
    at = frame(40, 17)
    fit = ramp if rank else LEATHER
    # The horn behind, banded, curving up to a point.
    horn = at([(0.5, -1.5), (2.5, -4.5), (5, -7), (8, -8.4), (11, -8.2)])
    for i, (p0, p1) in enumerate(zip(horn, horn[1:])):
        w0, w1 = [3.2, 2.6, 2.0, 1.3, 0.4][i:i + 2]
        taper(img, p0, p1, w0, w1, HORN if i % 2 == 0 else [HORN[1], HORN[2], HORN[3], HORN[4], FEATHER[2]], lo=0.8, hi=3.4)
    for root, tip, w in (((-1, -1.8), (-9.5, -4.6), 1.9), ((-1.5, -1.2), (-10.5, -2.4), 1.6)):
        stroke(img, at([root, tip]), [w, 0.4], FEATHER, lo=1.0, hi=3.8)
        dots(img, at([((root[0] + tip[0]) / 2, (root[1] + tip[1]) / 2)]), FEATHER[0])
    # The blade: spine outside with barbed teeth, the edge on the concave inside.
    spine = [(2, 1.5), (4, 6), (3.8, 11), (2, 16), (-1.5, 20.5), (-6.5, 24.2), (-13, 26.5)]
    edge = [(-13, 26.5), (-8.5, 22), (-5, 18), (-2.8, 14), (-1.6, 10), (-1.4, 6), (-2, 1.5)]
    region(img, poly(at(spine + edge)), METAL, lo=0.8, hi=3.4)
    for a, b in ((4.6, 8.5), (3.6, 13.5), (1.2, 18)):
        region(img, poly(at([(a - 0.8, b - 1.2), (a + 1.8, b + 0.4), (a - 0.6, b + 1.2)])), METAL, lo=1.0, hi=3.0)
    dots(img, at([(-10, 24.4), (-7.4, 21.6), (-5, 18.8), (-3.2, 15.6), (-2.1, 12.2), (-1.7, 9), (-1.6, 6)]), METAL[4])
    dots(img, at([(-11.5, 25.4), (-8.6, 23)]), (255, 255, 255))
    dots(img, at([(2.8, 7), (2.6, 11), (1.2, 15), (-1.4, 19)]), METAL[1])
    region(img, poly(at([(-2.5, -1.8), (1.5, -1.8), (1.5, 1.8), (-2.5, 1.8)])), fit, lo=1.0, hi=3.4)   # binding
    dots(img, at([(-1, -2.2), (-0.5, -2.6)]), GLOW[3])                                                  # a bead
    stroke(img, [(7, 50), (4, 53), (2, 55)], [3.0, 2.0, 0.5], ramp if rank else BONE, lo=1.2, hi=3.6)   # butt point
    rune(img, at([(1.5, 7), (1.2, 11), (-0.2, 15), (-3, 19)]), rank)
    if rank == 3:
        glint(img, *at([(0.5, 12)])[0])
    outline(img)
    return img


def greatsword(rank):
    """A 60px two-hander with a broad slab blade: one even width from guard to an angled chisel point, a ground
    edge on the leading side, two sockets at the base that carry the rank's light, a short heavy guard and a
    two-hand grip."""
    img = new((60, 60))
    _, ramp, _ = fittings(rank)
    fit = ramp if rank else METAL
    haft(img, 4, 56, 11, ramp=BONE, wraps=(1, 3, 5, 7, 9), wrap_ramp=fit if rank else LEATHER)
    at = frame(17, 43)
    # The edge runs down the trailing side and the chisel is cut from the leading side down to the point.
    region(img, poly(at([(0, 5), (0, -5), (52, -5), (37, 5)])), METAL, lo=0.9, hi=3.2)
    dots(img, at([(k, -4.4) for k in range(1, 50)]), METAL[4])                # the ground edge
    dots(img, at([(k, -3.2) for k in range(2, 48)]), METAL[3])
    dots(img, at([(k, 4.4) for k in range(1, 37)]), METAL[1])                 # the back of the slab
    dots(img, at([(37 + k, 4.4 - k * 0.62) for k in range(15)]), METAL[4])    # the long chisel cut to the point
    # Two sockets near the base, dark when plain, lit in the rank's metal or light as it rises.
    sock = [VOID, COPPER, GLOW, GOLD][rank]
    for centre in (6.5, 13.0):
        region(img, poly(at([(centre + 2.4 * math.cos(t), 2.4 * math.sin(t)) for t in (i * math.pi / 6 for i in range(12))])), METAL, lo=0.2, hi=1.0, bevel=False)
        region(img, poly(at([(centre + 1.4 * math.cos(t), 1.4 * math.sin(t)) for t in (i * math.pi / 4 for i in range(8))])),
               sock, lo=1.0 if rank else 0.0, hi=4.0 if rank else 2.0)
    at_guard = frame(16, 44)
    region(img, poly(at_guard([(-2, -6.5), (1, -6.5), (1, 6.5), (-2, 6.5)])), fit, lo=1.2, hi=3.6)
    for dx, dy, tone in ((0, 0, 3), (1, 0, 2), (0, 1, 1), (-1, 0, 2), (0, -1, 3), (1, 1, 1), (-1, -1, 4)):
        put(img, 3 + dx, 57 + dy, fit[tone])
    rune(img, at([(k, 1.5) for k in range(18, 38, 3)]), rank)
    if rank == 3:
        glint(img, *at([(40, -3)])[0])
    outline(img)
    return img


def rattle(rank):
    """The Healer's Rattle: a dried gourd on a bone handle, banded in the rank's paint, beads at the neck and
    two feathers tied beneath."""
    img = new()
    wraps, ramp, cap = fittings(rank)
    bind = ramp if rank else LEATHER
    haft(img, 5, 27, 12, ramp=BONE, wraps=(2, 3) if not rank else wraps[:3], wrap_ramp=bind, cap=bind)
    at = frame(21, 11)
    gourd = [(7 * math.cos(t) + 1, 5.6 * math.sin(t)) for t in (i * math.pi / 10 for i in range(20))]
    region(img, poly(at(gourd)), CLAY_GOURD, lo=0.8, hi=3.6)
    band = ramp if rank else LEATHER
    for a in (-1.5, 2.5):
        dots(img, at([(a, b) for b in range(-5, 6)]), band[3])
    dots(img, at([(4, 0), (4.5, 1.5), (4.5, -1.5), (0.5, 3.5), (0.5, -3.5)]), band[4])
    dots(img, at([(-6.2, -1), (-6.2, 1), (-6.6, 0)]), GLOW[3] if rank >= 2 else BONE[3])   # beads at the neck
    for tip in ((-11, 4.5), (-12, 2.5)):
        stroke(img, at([(-6.5, 1), tip]), [1.6, 0.4], FEATHER, lo=1.0, hi=3.8)
    if rank == 3:
        glint(img, *at([(3, 3)])[0])
    outline(img)
    return img


CLAY_GOURD = [(92, 58, 34), (132, 88, 48), (170, 122, 66), (204, 160, 96), (232, 200, 140)]


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
            "spiritgear_spear": spear, "spiritgear_halberd": halberd, "spiritgear_battle_axe": battle_axe,
            "spiritgear_warhammer": warhammer, "spiritgear_dagger": dagger, "spiritgear_scythe": scythe,
            "spiritgear_greatsword": greatsword, "spiritgear_trident": trident, "spiritgear_rattle": rattle,
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
