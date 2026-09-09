"""Painter for the sixth voice — Loom (design 3.0 §1, art direction §8).

Produces 32x32 textures in the Living Lattice palette with top-left light, a 1 px ink
outline, 2-3 value steps per material and no noise:
  block/resonance_totem_loom.png (+ _top)   block/echo_unweave.png (+ _top)
  item/loom_seal.png  item/loom_thread.png  item/unsung_heart.png
Run: python3 tools/art/loom.py [project root]
"""
from pathlib import Path
import sys
from PIL import Image, ImageDraw

ROOT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[2]
TEX = ROOT / 'src/main/resources/assets/tribalpower/textures'

INK = (17, 26, 34, 255)
WOOD = (0x5a, 0x3b, 0x2e, 255)
WOOD_LIGHT = (0x7a, 0x51, 0x38, 255)
STONE = (0x3a, 0x4a, 0x55, 255)
STONE_LIGHT = (0x55, 0x66, 0x72, 255)
COPPER = (0xc0, 0x8a, 0x4e, 255)
LIGHT = (0x7e, 0xff, 0xcb, 255)
BONE = (225, 217, 189, 255)
LOOM = (0x62, 0xd1, 0xc9, 255)          # teal-white, the Loom colour
LOOM_DEEP = (0x2f, 0x8a, 0x86, 255)
LOOM_PALE = (0xc9, 0xf6, 0xf1, 255)
HEART = (0x8c, 0x2e, 0x3f, 255)
HEART_LIGHT = (0xc4, 0x4f, 0x5c, 255)


def shade(c, amount):
    return tuple(max(0, min(255, x + amount)) for x in c[:3]) + (255,)


def blank():
    return Image.new('RGBA', (32, 32), (0, 0, 0, 0))


def save(rel, img):
    path = TEX / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def plank_face(color, light):
    """Flat wood face: three value steps, top-left light, no noise."""
    im = blank(); d = ImageDraw.Draw(im)
    d.rectangle((0, 0, 31, 31), fill=color)
    for x in range(0, 32, 8):
        d.line((x, 0, x, 31), fill=shade(color, -22))
        d.line((x + 1, 0, x + 1, 31), fill=light)
    d.line((0, 0, 31, 0), fill=light)
    d.line((0, 0, 0, 31), fill=light)
    return im


def stone_face(color, light):
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
    d.rectangle((0, 0, 31, 2), fill=COPPER); d.line((0, 0, 31, 0), fill=shade(COPPER, 35))
    d.rectangle((0, 29, 31, 31), fill=shade(COPPER, -30)); d.line((0, 29, 31, 29), fill=COPPER)
    for x in (3, 28):
        for y in (5, 26):
            d.rectangle((x - 1, y - 1, x + 1, y + 1), fill=INK); d.point((x, y - 1), fill=shade(COPPER, 35))


def spindle_glyph(d, x=16, y=16, c=LOOM, size=6):
    """Loom glyph: a spindle — vertical shaft with a diamond bobbin and a crossing thread."""
    d.line((x, y - size - 1, x, y + size + 1), fill=c, width=2)
    d.polygon([(x, y - 4), (x + 4, y), (x, y + 4), (x - 4, y)], fill=c)
    d.polygon([(x, y - 2), (x + 2, y), (x, y + 2), (x - 2, y)], fill=LOOM_PALE)
    d.line([(x - size, y + size), (x - 2, y + 2)], fill=LOOM_DEEP, width=1)
    d.line([(x + 2, y - 2), (x + size, y - size)], fill=LOOM_DEEP, width=1)


def totem_side():
    im = plank_face(WOOD, WOOD_LIGHT); d = ImageDraw.Draw(im)
    rails(d)
    d.rectangle((7, 6, 24, 25), fill=INK, outline=shade(COPPER, -32))
    d.rectangle((8, 7, 23, 24), fill=shade(STONE, -12))
    d.line((8, 7, 23, 7), fill=STONE_LIGHT); d.line((8, 7, 8, 24), fill=STONE_LIGHT)
    # taut threads behind the glyph
    for yy in (10, 21):
        d.line((9, yy, 22, yy), fill=LOOM_DEEP)
    spindle_glyph(d, 16, 16)
    return im


def totem_top():
    im = plank_face(WOOD, WOOD_LIGHT); d = ImageDraw.Draw(im)
    rails(d)
    d.ellipse((4, 4, 27, 27), fill=shade(BONE, -28), outline=COPPER, width=2)
    d.ellipse((7, 7, 24, 24), outline=shade(BONE, -49))
    d.line((5, 5, 12, 12), fill=shade(BONE, -8))
    # a wound thread around the drum head
    for r in (8, 5):
        d.ellipse((16 - r, 16 - r, 16 + r, 16 + r), outline=LOOM_DEEP)
    d.polygon([(16, 12), (19, 16), (16, 20), (13, 16)], fill=LOOM)
    d.point((16, 13), fill=LOOM_PALE)
    return im


def unweave_side():
    im = stone_face(STONE, STONE_LIGHT); d = ImageDraw.Draw(im)
    rails(d)
    d.rectangle((7, 6, 24, 25), fill=INK, outline=shade(COPPER, -32))
    d.rectangle((8, 7, 23, 24), fill=shade(STONE, -18))
    d.line((8, 7, 23, 7), fill=STONE_LIGHT); d.line((8, 7, 8, 24), fill=STONE_LIGHT)
    # unravelling: a woven block on the left loosening into loose strands on the right
    for yy in range(9, 23, 3):
        d.line((10, yy, 15, yy), fill=LOOM_DEEP)
        d.line((16, yy, 21, yy + (1 if (yy // 3) % 2 else -1)), fill=LOOM)
    for xx in (11, 14):
        d.line((xx, 9, xx, 22), fill=LOOM_DEEP)
    d.point((21, 9), fill=LOOM_PALE); d.point((21, 16), fill=LOOM_PALE)
    return im


def unweave_top():
    im = stone_face(STONE, STONE_LIGHT); d = ImageDraw.Draw(im)
    rails(d)
    d.ellipse((4, 4, 27, 27), fill=shade(BONE, -28), outline=COPPER, width=2)
    d.ellipse((7, 7, 24, 24), outline=shade(BONE, -49))
    d.line((5, 5, 12, 12), fill=shade(BONE, -8))
    spindle_glyph(d, 16, 16, c=LOOM_DEEP, size=5)
    return im


def seal_icon():
    im = blank(); d = ImageDraw.Draw(im)
    d.polygon([(8, 2), (23, 2), (29, 8), (29, 23), (23, 29), (8, 29), (2, 23), (2, 8)], fill=INK)
    d.polygon([(9, 4), (22, 4), (27, 9), (27, 22), (22, 27), (9, 27), (4, 22), (4, 9)],
              fill=shade(BONE, -45), outline=shade(COPPER, 35))
    d.line((9, 4, 22, 4), fill=BONE); d.line((4, 9, 4, 22), fill=BONE)
    d.rectangle((7, 7, 24, 24), fill=INK)
    spindle_glyph(d, 16, 16, size=7)
    return im


def thread_icon():
    im = blank(); d = ImageDraw.Draw(im)
    # a hank of teal-white thread wound around a small bone spindle
    d.polygon([(9, 5), (23, 5), (25, 8), (25, 26), (23, 28), (9, 28), (7, 26), (7, 8)], fill=INK)
    d.rectangle((8, 7, 24, 26), fill=LOOM_DEEP)
    d.line((8, 7, 24, 7), fill=LOOM); d.line((8, 7, 8, 26), fill=LOOM)
    for yy in range(9, 26, 3):
        d.line((9, yy, 23, yy + 1), fill=LOOM)
        d.point((10, yy), fill=LOOM_PALE)
    d.rectangle((14, 2, 18, 30), fill=INK)
    d.rectangle((15, 3, 17, 29), fill=shade(BONE, -20)); d.line((15, 3, 15, 29), fill=BONE)
    d.line([(24, 12), (29, 8), (30, 4)], fill=LOOM, width=2)
    return im


def heart_icon():
    im = blank(); d = ImageDraw.Draw(im)
    # a stilled drum-heart: hollow ring band, taut hide top, a crack, one teal rune line
    d.ellipse((3, 5, 28, 30), fill=INK)
    d.ellipse((5, 7, 26, 28), fill=HEART)
    d.ellipse((7, 9, 24, 26), fill=HEART_LIGHT)
    d.ellipse((7, 9, 20, 22), outline=shade(HEART_LIGHT, 30))
    d.ellipse((10, 12, 21, 23), fill=shade(HEART, -20), outline=INK)
    d.line([(12, 4), (14, 9), (12, 14)], fill=INK, width=1)
    d.line([(20, 5), (18, 9), (21, 13)], fill=INK, width=1)
    d.rectangle((9, 3, 22, 6), fill=INK); d.rectangle((10, 4, 21, 5), fill=shade(COPPER, -20)); d.line((10, 4, 21, 4), fill=COPPER)
    d.line([(15, 12), (15, 18), (18, 21)], fill=LOOM, width=2)
    d.point((15, 12), fill=LOOM_PALE)
    return im


def main():
    save('block/resonance_totem_loom.png', totem_side())
    save('block/resonance_totem_loom_top.png', totem_top())
    save('block/echo_unweave.png', unweave_side())
    save('block/echo_unweave_top.png', unweave_top())
    save('item/loom_seal.png', seal_icon())
    save('item/loom_thread.png', thread_icon())
    save('item/unsung_heart.png', heart_icon())
    print('Loom textures painted.')


if __name__ == '__main__':
    main()
