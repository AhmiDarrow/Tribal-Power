"""Painter for the sixth voice — Loom (design 3.0 §1, art direction §8).

Produces 32x32 textures in the Living Lattice palette with top-left light, a 1 px ink
outline, 2-3 value steps per material and no noise:
  block/resonance_totem_loom.png (+ _top)   block/echo_unweave.png (+ _top)
  item/loom_seal.png  item/loom_thread.png  item/unsung_heart.png
The totem panel element shows texels 6..26 x 8..20 of the side texture and the unweave body texels 2..30 x 8..26
(default model UVs), so the glyphs sit inside those bands.
Run: python3 tools/art/loom.py [project root]
"""
from pathlib import Path
import sys
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
from lattice import *  # noqa: E402,F403
from glyphs import draw_glyph  # noqa: E402

ROOT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[2]
TEX = ROOT / 'src/main/resources/assets/tribalpower/textures'

HEART_DARK = (0x5a, 0x1c, 0x28, 255)
HEART = (0x8c, 0x2e, 0x3f, 255)
HEART_LIGHT = (0xc4, 0x4f, 0x5c, 255)
HIDE = (0xc9, 0xa9, 0x74, 255)
HIDE_LIGHT = (0xe3, 0xc6, 0x92, 255)
HIDE_DARK = (0x9d, 0x7f, 0x52, 255)


def save(rel, img):
    save_to(TEX, rel, img)


def spindle_glyph(d, x=16, y=16, c=LOOM, size=6, pale=LOOM_PALE, deep=LOOM_DEEP):
    """Loom glyph: a spindle — vertical shaft with a diamond bobbin and a crossing thread."""
    d.line((x, y - size - 1, x, y + size + 1), fill=c, width=2)
    d.polygon([(x, y - 4), (x + 4, y), (x, y + 4), (x - 4, y)], fill=c)
    d.polygon([(x, y - 2), (x + 2, y), (x, y + 2), (x - 2, y)], fill=pale)
    d.line([(x - size, y + size), (x - 2, y + 2)], fill=deep, width=1)
    d.line([(x + 2, y - 2), (x + size, y - size)], fill=deep, width=1)


def thread_wrap(d, box, step=3):
    """Teal thread wound horizontally across a panel: alternating light/deep passes with a twist highlight."""
    x0, y0, x1, y1 = box
    for i, y in enumerate(range(y0, y1 + 1)):
        d.line((x0, y, x1, y), fill=LOOM_DEEP if i % step else LOOM)
        if i % step == 0:
            d.point((x0 + (i // step * 5) % (x1 - x0), y), fill=LOOM_PALE)


def totem_side():
    """Loom totem: the glyph panel is wrapped in teal thread with a bone spindle laid across it."""
    im = plank_face(WOOD, WOOD_LIGHT); d = ImageDraw.Draw(im)
    rails(d)
    d.rectangle((5, 5, 26, 26), fill=COPPER_DARK); d.line((5, 5, 26, 5), fill=COPPER); d.line((5, 5, 5, 26), fill=COPPER)
    d.rectangle((6, 6, 25, 25), fill=INK)
    thread_wrap(d, (7, 7, 24, 24))
    # bone spindle across the wrap, with a copper whorl
    d.rectangle((15, 8, 16, 23), fill=INK)
    d.rectangle((15, 9, 16, 22), fill=BONE); d.line((15, 9, 15, 22), fill=BONE_LIGHT)
    d.polygon([(16, 11), (20, 14), (16, 17), (11, 14)], fill=INK)
    d.polygon([(16, 12), (19, 14), (16, 16), (12, 14)], fill=COPPER); d.point((15, 13), fill=COPPER_LIGHT)
    return im


def totem_top():
    im = plank_face(WOOD, WOOD_LIGHT); d = ImageDraw.Draw(im)
    rails(d)
    disc(d, (4, 4, 27, 27), BONE_SHADE, BONE, darken(BONE_SHADE, 0.3), ink=COPPER)
    for r in (9, 6):
        d.ellipse((16 - r, 16 - r, 16 + r, 16 + r), outline=LOOM_DEEP)
    d.polygon([(16, 12), (19, 16), (16, 20), (13, 16)], fill=LOOM)
    d.point((16, 13), fill=LOOM_PALE)
    return im


def unweave_side():
    """Echo Unweave: a woven teal square on the left of the panel loosening into loose strands on the right."""
    im = stone_face(STONE, STONE_LIGHT); d = ImageDraw.Draw(im)
    rails(d)
    d.rectangle((6, 6, 25, 25), fill=COPPER_DARK); d.line((6, 6, 25, 6), fill=COPPER); d.line((6, 6, 6, 25), fill=COPPER)
    d.rectangle((7, 7, 24, 24), fill=INK)
    d.rectangle((8, 8, 23, 23), fill=STONE_DARK)
    # tight weave (left half): warp and weft in two teal values
    for y in range(9, 24, 2):
        d.line((9, y, 15, y), fill=LOOM_DEEP)
    for x in range(9, 16, 2):
        d.line((x, 9, x, 23), fill=LOOM)
    for y in range(9, 24, 4):
        for x in range(9, 16, 4):
            d.point((x, y), fill=LOOM_PALE)
    # loosening strands (right half): the weft rows fray outward and curl
    for i, y in enumerate(range(9, 24, 3)):
        d.line((16, y, 19 + i % 2 * 2, y + (1 if i % 2 else -1)), fill=LOOM)
        d.point((21 + i % 2, y + (2 if i % 2 else -2)), fill=LOOM_DEEP)
    d.line((22, 10, 23, 14), fill=LOOM_DEEP); d.point((23, 20), fill=LOOM_PALE)
    return im


def unweave_top():
    im = stone_face(STONE, STONE_LIGHT); d = ImageDraw.Draw(im)
    rails(d)
    disc(d, (4, 4, 27, 27), BONE_SHADE, BONE, darken(BONE_SHADE, 0.3), ink=COPPER)
    spindle_glyph(d, 16, 16, c=LOOM_DEEP, size=5)
    return im


def seal_icon():
    """Bone octagon seal like the five element seals, with the Loom spindle glyph on an ink field."""
    im = blank(); d = ImageDraw.Draw(im)
    d.polygon([(8, 2), (23, 2), (29, 8), (29, 23), (23, 29), (8, 29), (2, 23), (2, 8)], fill=INK)
    d.polygon([(9, 4), (22, 4), (27, 9), (27, 22), (22, 27), (9, 27), (4, 22), (4, 9)], fill=BONE_SHADE, outline=BONE)
    d.line((9, 4, 22, 4), fill=BONE_LIGHT); d.line((4, 9, 4, 22), fill=BONE_LIGHT)
    d.line((27, 10, 27, 22), fill=darken(BONE_SHADE, 0.3)); d.line((10, 27, 22, 27), fill=darken(BONE_SHADE, 0.3))
    d.rectangle((7, 7, 24, 24), fill=INK)
    d.rectangle((8, 8, 23, 23), fill=STONE_DARK)
    draw_glyph(d, 'spindle', 7, 7, LOOM, scale=2, shadow=LOOM_DEEP)
    d.rectangle((15, 15, 16, 16), fill=LOOM_PALE)
    return im


def thread_icon():
    """A skein of teal-white thread wound on a bone drop spindle, one loose end trailing."""
    im = blank(); d = ImageDraw.Draw(im)
    # spindle shaft and whorl
    d.rectangle((14, 1, 17, 30), fill=INK)
    d.rectangle((15, 2, 16, 29), fill=BONE_SHADE); d.line((15, 2, 15, 29), fill=BONE)
    d.ellipse((9, 24, 22, 29), fill=INK); d.ellipse((10, 25, 21, 28), fill=COPPER); d.arc((10, 25, 21, 28), 190, 330, fill=COPPER_LIGHT)
    # skein: an oval of wound thread
    d.ellipse((5, 6, 26, 23), fill=INK)
    d.ellipse((6, 7, 25, 22), fill=LOOM_DEEP)
    for y in range(8, 22, 2):
        d.line((7, y, 24, y + 1), fill=LOOM)
    d.arc((6, 7, 25, 22), 200, 300, fill=LOOM_PALE, width=2)
    d.arc((6, 7, 25, 22), 20, 130, fill=darken(LOOM_DEEP, 0.3), width=2)
    # loose end
    d.line([(24, 16), (28, 13), (29, 8), (27, 4)], fill=LOOM, width=2)
    d.point((28, 5), fill=LOOM_PALE)
    return outline(im)


def heart_icon():
    """The Unsung's stilled heart: a small hollow drum with a hide face, a lacquer band, a crack and one teal rune."""
    im = blank(); d = ImageDraw.Draw(im)
    # drum shell (heart-shaped: two lobes at the top, tapered foot)
    d.ellipse((3, 5, 17, 19), fill=HEART); d.ellipse((14, 5, 28, 19), fill=HEART)
    d.polygon([(4, 14), (27, 14), (16, 29)], fill=HEART)
    d.ellipse((5, 7, 12, 13), fill=HEART_LIGHT); d.ellipse((17, 6, 22, 10), fill=HEART_LIGHT)
    d.polygon([(8, 20), (24, 20), (16, 28)], fill=HEART_DARK)
    # hide face laced on the front with a copper band
    d.ellipse((9, 9, 22, 22), fill=INK)
    d.ellipse((10, 10, 21, 21), fill=HIDE); d.arc((10, 10, 21, 21), 195, 290, fill=HIDE_LIGHT, width=2); d.arc((10, 10, 21, 21), 15, 110, fill=HIDE_DARK, width=2)
    for x, y in ((11, 12), (20, 12), (11, 19), (20, 19), (15, 10), (15, 21)):
        d.point((x, y), fill=INK)
    d.rectangle((9, 15, 22, 16), fill=COPPER); d.line((9, 15, 22, 15), fill=COPPER_LIGHT)
    # crack and rune
    d.line([(20, 5), (18, 9), (19, 12)], fill=INK); d.line([(21, 5), (19, 9), (20, 12)], fill=HEART_LIGHT)
    d.line((15, 12, 15, 19), fill=LOOM); d.line((13, 17, 17, 17), fill=LOOM); d.point((15, 12), fill=LOOM_PALE)
    return outline(im)


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
