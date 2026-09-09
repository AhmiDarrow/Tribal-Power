"""Shared Living Lattice palette and pixel primitives for the tools/art painters (design 3.0 §8).

Rules baked in here: 32x32, top-left light, 1 px ink outline, 2-3 value steps per material, no noise.

    from lattice import *          # colours, blank(), save_to(), bevel(), disc(), plank_face(), stone_face(), rails(), outline()
"""
import math
from pathlib import Path
from PIL import Image, ImageDraw

INK = (17, 26, 34, 255)
WOOD_DARK = (0x3e, 0x28, 0x1f, 255)
WOOD = (0x5a, 0x3b, 0x2e, 255)
WOOD_LIGHT = (0x7a, 0x51, 0x38, 255)
STONE_DARK = (0x2a, 0x36, 0x3f, 255)
STONE = (0x3a, 0x4a, 0x55, 255)
STONE_LIGHT = (0x55, 0x66, 0x72, 255)
STONE_PALE = (0x74, 0x86, 0x92, 255)
COPPER_DARK = (0x8a, 0x5e, 0x30, 255)
COPPER = (0xc0, 0x8a, 0x4e, 255)
COPPER_LIGHT = (0xe3, 0xb1, 0x71, 255)
BRASS_DARK = (0x9a, 0x74, 0x2e, 255)
BRASS = (0xd2, 0xa5, 0x4a, 255)
BRASS_LIGHT = (0xf1, 0xd0, 0x82, 255)
LIGHT_DEEP = (0x3f, 0xb3, 0x8e, 255)
LIGHT = (0x7e, 0xff, 0xcb, 255)
LIGHT_PALE = (0xd6, 0xff, 0xef, 255)
LOOM_DEEP = (0x2f, 0x8a, 0x86, 255)
LOOM = (0x62, 0xd1, 0xc9, 255)
LOOM_PALE = (0xc9, 0xf6, 0xf1, 255)
BONE_SHADE = (176, 165, 136, 255)
BONE = (225, 217, 189, 255)
BONE_LIGHT = (246, 241, 222, 255)
ASH_DARK = (0x1c, 0x1e, 0x22, 255)
ASH = (0x2a, 0x2c, 0x30, 255)
ASH_LIGHT = (0x45, 0x47, 0x4c, 255)
CLEAR = (0, 0, 0, 0)


def shade(c, amount):
    return tuple(max(0, min(255, v + amount)) for v in c[:3]) + (255,)


def lighten(c, f=0.35):
    return tuple(int(v + (255 - v) * f) for v in c[:3]) + (255,)


def darken(c, f=0.35):
    return tuple(int(v * (1 - f)) for v in c[:3]) + (255,)


def ramp(c, f=0.3):
    """(dark, base, light) three-value ramp of a colour."""
    return darken(c, f), tuple(c[:3]) + (255,), lighten(c, f)


def blank(size=32):
    return Image.new('RGBA', (size, size) if isinstance(size, int) else size, CLEAR)


def save_to(tex_root: Path, rel, img):
    path = Path(tex_root) / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def bevel(d, box, base, light, dark, ink=None):
    """Filled rectangle with a light top-left edge and a dark bottom-right edge (optionally an ink outline outside)."""
    x0, y0, x1, y1 = box
    if ink is not None:
        d.rectangle((x0 - 1, y0 - 1, x1 + 1, y1 + 1), fill=ink)
    d.rectangle(box, fill=base)
    d.line((x0, y0, x1, y0), fill=light)
    d.line((x0, y0, x0, y1), fill=light)
    d.line((x1, y0 + 1, x1, y1), fill=dark)
    d.line((x0 + 1, y1, x1, y1), fill=dark)


def disc(d, box, base, light, dark, ink=None, width=2):
    """Filled ellipse with a top-left highlight arc and a bottom-right shade arc."""
    x0, y0, x1, y1 = box
    if ink is not None:
        d.ellipse((x0 - 1, y0 - 1, x1 + 1, y1 + 1), fill=ink)
    d.ellipse(box, fill=base)
    d.arc(box, 195, 285, fill=light, width=width)
    d.arc(box, 15, 105, fill=dark, width=width)


def ring(d, box, colour, width=2):
    d.ellipse(box, outline=colour, width=width)


def outline(im, colour=INK):
    """1 px ink outline: every transparent pixel touching an opaque one becomes ink."""
    px = im.load()
    w, h = im.size
    edges = []
    for y in range(h):
        for x in range(w):
            if px[x, y][3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < w and 0 <= ny < h and px[nx, ny][3] and px[nx, ny] != colour:
                        edges.append((x, y))
                        break
    for x, y in edges:
        px[x, y] = colour
    return im


def plank_face(color=WOOD, light=WOOD_LIGHT):
    im = blank(); d = ImageDraw.Draw(im)
    d.rectangle((0, 0, 31, 31), fill=color)
    for x in range(0, 32, 8):
        d.line((x, 0, x, 31), fill=shade(color, -22))
        d.line((x + 1, 0, x + 1, 31), fill=light)
    d.line((0, 0, 31, 0), fill=light)
    d.line((0, 0, 0, 31), fill=light)
    return im


def stone_face(color=STONE, light=STONE_LIGHT):
    im = blank(); d = ImageDraw.Draw(im)
    d.rectangle((0, 0, 31, 31), fill=color)
    for y in range(0, 32, 8):
        for x in range(-4 if y % 16 else 0, 32, 10):
            d.rectangle((x, y, x + 9, y + 7), outline=shade(color, -18))
            d.line((x + 1, y + 1, x + 8, y + 1), fill=light)
            d.line((x + 1, y + 1, x + 1, y + 6), fill=light)
    return im


def rails(d):
    """Copper rails top and bottom with ink rivets, as on every lattice device."""
    d.rectangle((0, 0, 31, 2), fill=COPPER); d.line((0, 0, 31, 0), fill=COPPER_LIGHT)
    d.rectangle((0, 29, 31, 31), fill=COPPER_DARK); d.line((0, 29, 31, 29), fill=COPPER)
    for x in (3, 28):
        for y in (5, 26):
            d.rectangle((x - 1, y - 1, x + 1, y + 1), fill=INK); d.point((x, y - 1), fill=COPPER_LIGHT)


def rivet(d, x, y, c=COPPER):
    d.rectangle((x - 1, y - 1, x + 1, y + 1), fill=INK)
    d.point((x, y), fill=c)
    d.point((x - 1, y - 1), fill=lighten(c, 0.4))


def polar(cx, cy, r, deg):
    a = math.radians(deg)
    return (round(cx + r * math.cos(a)), round(cy + r * math.sin(a)))
