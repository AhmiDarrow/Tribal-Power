"""Painter for World Rites, Ley sight and Pulse logic (design 3.0 §4 and §7, art direction §8).

Produces 32x32 textures in the Living Lattice palette with top-left light, a 1 px ink outline,
2-3 value steps per material and no noise:
  block/pulse_gauge_side.png  block/pulse_gauge_front.png  block/pulse_gauge_back.png
  block/pulse_threshold_side.png  block/pulse_threshold_front.png  block/pulse_threshold_back.png
  item/rite_rain_calling.png  item/rite_sky_clearing.png  item/rite_dawn_calling.png
  item/rite_green_blessing.png  item/rite_still_night.png  item/rite_ley_binding.png
  item/ley_lens.png
Run: python3 tools/art/qol.py [project root]
"""
from pathlib import Path
import sys
from PIL import Image, ImageDraw

ROOT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[2]
TEX = ROOT / 'src/main/resources/assets/tribalpower/textures'

INK = (17, 26, 34, 255)
STONE = (0x3a, 0x4a, 0x55, 255)
STONE_LIGHT = (0x55, 0x66, 0x72, 255)
COPPER = (0xc0, 0x8a, 0x4e, 255)
LIGHT = (0x7e, 0xff, 0xcb, 255)
RED = (0xc8, 0x3a, 0x2e, 255)
BONE = (225, 217, 189, 255)

# Element glyph colours (SpiritEffects.color) for the tablets.
ELEMENTS = {
    'rite_rain_calling': (0x47, 0xbf, 0xe8, 255),   # water
    'rite_sky_clearing': (0xcf, 0xeb, 0xd4, 255),   # air
    'rite_dawn_calling': (0xff, 0x7a, 0x3b, 255),   # fire
    'rite_green_blessing': (0x87, 0xb8, 0x5c, 255), # earth
    'rite_still_night': (0xb3, 0x7a, 0xf2, 255),    # spirit
    'rite_ley_binding': (0x62, 0xd1, 0xc9, 255),    # loom
}


def shade(c, amount):
    return tuple(max(0, min(255, x + amount)) for x in c[:3]) + (255,)


def blank():
    return Image.new('RGBA', (32, 32), (0, 0, 0, 0))


def save(rel, img):
    path = TEX / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def stone_panel():
    """Flat stone device face: two value steps, top-left light, ink outline, copper corner rivets."""
    im = blank(); d = ImageDraw.Draw(im)
    d.rectangle((0, 0, 31, 31), fill=STONE)
    d.rectangle((2, 2, 29, 29), fill=shade(STONE, -10))
    d.line((1, 1, 30, 1), fill=STONE_LIGHT); d.line((1, 1, 1, 30), fill=STONE_LIGHT)
    d.line((1, 30, 30, 30), fill=shade(STONE, -26)); d.line((30, 1, 30, 30), fill=shade(STONE, -26))
    d.rectangle((0, 0, 31, 31), outline=INK)
    for x in (4, 27):
        for y in (4, 27):
            d.point((x, y), fill=COPPER); d.point((x - 1, y - 1), fill=shade(COPPER, 35)); d.point((x + 1, y + 1), fill=INK)
    return im, d


def arrow(d, c, cx=16, cy=16):
    """Copper arrow glyph pointing up (toward the faced block once the model rotates it)."""
    d.polygon([(cx, cy - 9), (cx + 7, cy - 1), (cx + 3, cy - 1), (cx + 3, cy + 9), (cx - 3, cy + 9), (cx - 3, cy - 1), (cx - 7, cy - 1)], fill=c)
    d.line((cx - 6, cy - 1, cx, cy - 8), fill=shade(c, 40))
    d.line((cx - 2, cy, cx - 2, cy + 8), fill=shade(c, 40))
    d.line((cx + 3, cy, cx + 3, cy + 8), fill=shade(c, -40))
    d.polygon([(cx, cy - 9), (cx + 7, cy - 1), (cx + 3, cy - 1), (cx + 3, cy + 9), (cx - 3, cy + 9), (cx - 3, cy - 1), (cx - 7, cy - 1)], outline=INK)


def dial_ring(d, notches, lit_c=LIGHT):
    """Copper dial ring with an ink well in the middle (the model's animated light plate sits there)."""
    d.ellipse((5, 5, 26, 26), fill=shade(COPPER, -30), outline=INK)
    d.ellipse((7, 7, 24, 24), fill=COPPER)
    d.arc((7, 7, 24, 24), 200, 290, fill=shade(COPPER, 40), width=2)
    d.rectangle((10, 10, 21, 21), fill=INK)
    d.rectangle((11, 11, 20, 20), fill=shade(STONE, -30))
    for i in range(notches):
        y = 27 - i * 6 if notches == 4 else None
        if y is None:
            break
        d.line((2, y, 3, y), fill=lit_c)
        d.line((28, y, 29, y), fill=lit_c)


def gauge_side():
    im, d = stone_panel()
    arrow(d, COPPER)
    return im


def gauge_front():
    im, d = stone_panel()
    dial_ring(d, 0)
    for a in range(0, 32, 4):  # small lit ticks around the ring like a meter scale
        pass
    d.point((16, 3), fill=LIGHT); d.point((3, 16), fill=LIGHT); d.point((28, 16), fill=LIGHT)
    return im


def gauge_back():
    im, d = stone_panel()
    d.rectangle((12, 12, 19, 19), fill=shade(RED, -30), outline=INK)
    d.rectangle((13, 13, 17, 17), fill=RED)
    d.line((13, 13, 17, 13), fill=shade(RED, 50))
    for x in (8, 23):
        d.line((x, 12, x, 19), fill=shade(RED, -60))
    return im


def threshold_side():
    im, d = stone_panel()
    arrow(d, COPPER)
    d.rectangle((6, 25, 25, 27), fill=INK)
    for i in range(4):
        d.rectangle((7 + i * 5, 26, 9 + i * 5, 26), fill=LIGHT if i < 2 else shade(STONE_LIGHT, 10))
    return im


def threshold_front():
    im, d = stone_panel()
    dial_ring(d, 4)
    return im


def threshold_back():
    im, d = stone_panel()
    d.rectangle((11, 11, 20, 20), fill=shade(RED, -30), outline=INK)
    d.rectangle((12, 12, 19, 19), fill=RED)
    d.line((12, 12, 19, 12), fill=shade(RED, 50))
    d.line((14, 15, 17, 15), fill=BONE); d.line((14, 17, 17, 17), fill=BONE)
    return im


def tablet(glyph, colour):
    """Carved stone tablet, rounded shoulders, coloured element glyph, ink outline."""
    im = blank(); d = ImageDraw.Draw(im)
    body = [(9, 4), (22, 4), (25, 7), (25, 28), (6, 28), (6, 7)]
    d.polygon(body, fill=STONE, outline=INK)
    d.polygon([(10, 5), (21, 5), (24, 8), (24, 27), (7, 27), (7, 8)], fill=STONE)
    d.line((8, 7, 8, 26), fill=STONE_LIGHT); d.line((10, 5, 21, 5), fill=STONE_LIGHT)
    d.line((24, 8, 24, 27), fill=shade(STONE, -26)); d.line((7, 27, 24, 27), fill=shade(STONE, -26))
    d.rectangle((9, 9, 22, 24), fill=shade(STONE, -12))
    glyph(d, colour)
    return im


def g_rain(d, c):
    d.arc((10, 9, 21, 19), 180, 360, fill=c, width=2)
    d.line((10, 14, 21, 14), fill=c)
    for x in (12, 16, 20):
        d.line((x, 17, x - 1, 22), fill=c)


def g_sky(d, c):
    d.ellipse((12, 10, 19, 17), outline=c)
    for x, y in ((15, 7), (15, 20), (9, 13), (22, 13)):
        d.point((x, y), fill=c)
    d.line((11, 21, 21, 21), fill=c)


def g_dawn(d, c):
    d.arc((10, 11, 21, 22), 180, 360, fill=c, width=2)
    d.line((9, 17, 22, 17), fill=c)
    for x0, y0, x1, y1 in ((15, 8, 15, 10), (10, 10, 12, 12), (21, 10, 19, 12)):
        d.line((x0, y0, x1, y1), fill=c)


def g_green(d, c):
    d.line((15, 22, 15, 12), fill=c, width=2)
    d.polygon([(15, 14), (10, 11), (11, 16)], fill=c)
    d.polygon([(16, 12), (21, 9), (20, 14)], fill=c)
    d.line((12, 22, 19, 22), fill=c)


def g_still(d, c):
    d.pieslice((10, 10, 21, 21), 60, 300, fill=c)
    d.pieslice((13, 10, 24, 21), 60, 300, fill=shade(STONE, -12))
    d.point((21, 12), fill=c); d.point((22, 18), fill=c)


def g_ley(d, c):
    d.line((15, 8, 15, 24), fill=c, width=2)
    d.polygon([(15, 12), (19, 16), (15, 20), (11, 16)], fill=c)
    d.line((10, 10, 21, 22), fill=shade(c, 40))


def ley_lens():
    """A round glass lens in a copper ring on a short bone handle, gold-blue ley gradient inside."""
    im = blank(); d = ImageDraw.Draw(im)
    d.line((20, 20, 27, 27), fill=INK, width=4)
    d.line((21, 20, 27, 26), fill=BONE, width=2)
    d.ellipse((3, 3, 22, 22), fill=INK)
    d.ellipse((4, 4, 21, 21), fill=shade(COPPER, -20))
    d.arc((4, 4, 21, 21), 200, 320, fill=shade(COPPER, 40), width=2)
    d.ellipse((6, 6, 19, 19), fill=(0x2a, 0x5a, 0xc8, 255))
    d.pieslice((6, 6, 19, 19), 200, 330, fill=(0xf0, 0xc8, 0x4a, 255))
    d.ellipse((8, 8, 17, 17), fill=(0x4a, 0x8a, 0xd8, 255))
    d.pieslice((8, 8, 17, 17), 210, 320, fill=(0xff, 0xd8, 0x66, 255))
    d.line((9, 10, 12, 8), fill=(255, 255, 255, 200), width=2)
    return im


def main():
    save('block/pulse_gauge_side.png', gauge_side())
    save('block/pulse_gauge_front.png', gauge_front())
    save('block/pulse_gauge_back.png', gauge_back())
    save('block/pulse_threshold_side.png', threshold_side())
    save('block/pulse_threshold_front.png', threshold_front())
    save('block/pulse_threshold_back.png', threshold_back())
    glyphs = {
        'rite_rain_calling': g_rain, 'rite_sky_clearing': g_sky, 'rite_dawn_calling': g_dawn,
        'rite_green_blessing': g_green, 'rite_still_night': g_still, 'rite_ley_binding': g_ley,
    }
    for name, glyph in glyphs.items():
        save(f'item/{name}.png', tablet(glyph, ELEMENTS[name]))
    save('item/ley_lens.png', ley_lens())
    print('qol art written to', TEX)


if __name__ == '__main__':
    main()
