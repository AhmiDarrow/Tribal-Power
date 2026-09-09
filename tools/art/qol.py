"""Painter for World Rites, Ley sight and Pulse logic (design 3.0 §4 and §7, art direction §8).

Produces 32x32 textures in the Living Lattice palette with top-left light, a 1 px ink outline,
2-3 value steps per material and no noise:
  block/pulse_gauge_side.png  block/pulse_gauge_front.png  block/pulse_gauge_back.png
  block/pulse_threshold_side.png  block/pulse_threshold_front.png  block/pulse_threshold_back.png
  item/rite_rain_calling.png  item/rite_sky_clearing.png  item/rite_dawn_calling.png
  item/rite_green_blessing.png  item/rite_still_night.png  item/rite_ley_binding.png
  item/ley_lens.png
The gauge fronts are brass dials; the model's animated loom_light plate sits in the ink well at texels 11..21.
Run: python3 tools/art/qol.py [project root]
"""
from pathlib import Path
import sys
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
from lattice import *  # noqa: E402,F403

ROOT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[2]
TEX = ROOT / 'src/main/resources/assets/tribalpower/textures'

RED = (0xc8, 0x3a, 0x2e, 255)
RED_LIGHT = (0xe8, 0x6a, 0x55, 255)
RED_DARK = (0x7a, 0x22, 0x1c, 255)

# Element glyph colours (SpiritEffects.color) for the tablets.
ELEMENTS = {
    'rite_rain_calling': (0x47, 0xbf, 0xe8, 255),   # water
    'rite_sky_clearing': (0xcf, 0xeb, 0xd4, 255),   # air
    'rite_dawn_calling': (0xff, 0x7a, 0x3b, 255),   # fire
    'rite_green_blessing': (0x87, 0xb8, 0x5c, 255), # earth
    'rite_still_night': (0xb3, 0x7a, 0xf2, 255),    # spirit
    'rite_ley_binding': (0x62, 0xd1, 0xc9, 255),    # loom
}


def save(rel, img):
    save_to(TEX, rel, img)


# ---------------------------------------------------------------- pulse gauge / threshold

def stone_panel():
    """Flat stone device face: two value steps, top-left light, ink outline, copper corner rivets."""
    im = blank(); d = ImageDraw.Draw(im)
    d.rectangle((0, 0, 31, 31), fill=STONE)
    d.rectangle((2, 2, 29, 29), fill=shade(STONE, -10))
    d.line((1, 1, 30, 1), fill=STONE_LIGHT); d.line((1, 1, 1, 30), fill=STONE_LIGHT)
    d.line((1, 30, 30, 30), fill=STONE_DARK); d.line((30, 1, 30, 30), fill=STONE_DARK)
    d.rectangle((0, 0, 31, 31), outline=INK)
    for x in (4, 27):
        for y in (4, 27):
            rivet(d, x, y)
    return im, d


def arrow(d, c, cx=16, cy=16):
    """Copper flow arrow pointing up (toward the faced block once the model rotates it)."""
    body = [(cx, cy - 9), (cx + 7, cy - 1), (cx + 3, cy - 1), (cx + 3, cy + 9), (cx - 3, cy + 9), (cx - 3, cy - 1), (cx - 7, cy - 1)]
    d.polygon([(x, y + 1) for x, y in body], fill=INK)
    d.polygon(body, fill=c, outline=INK)
    d.line((cx - 5, cy - 2, cx, cy - 7), fill=lighten(c, 0.4))
    d.line((cx - 2, cy, cx - 2, cy + 8), fill=lighten(c, 0.4))
    d.line((cx + 2, cy, cx + 2, cy + 8), fill=darken(c, 0.3))


def dial(d, needle_deg, marks):
    """Brass gauge: bezel ring, bone dial face with an arc scale, a needle and the ink well for the light plate."""
    d.ellipse((3, 3, 28, 28), fill=INK)
    disc(d, (4, 4, 27, 27), BRASS, BRASS_LIGHT, BRASS_DARK)
    d.ellipse((6, 6, 25, 25), fill=INK)
    d.ellipse((7, 7, 24, 24), fill=BONE_SHADE)
    d.pieslice((7, 7, 24, 24), 200, 340, fill=BONE)
    # scale: ticks along the upper arc from 8 o'clock to 4 o'clock
    for i, deg in enumerate(range(150, 391, 30)):
        x, y = polar(15.5, 15.5, 8, deg)
        d.point((x, y), fill=INK)
        if i in marks:
            d.point(polar(15.5, 15.5, 7, deg), fill=marks[i])
    # needle from the centre well toward its reading
    x, y = polar(15.5, 15.5, 8, needle_deg)
    d.line((16, 16, x, y), fill=RED, width=1)
    d.point(polar(15.5, 15.5, 8, needle_deg), fill=RED_LIGHT)
    # ink well (model light plate covers texels 11..21)
    d.rectangle((10, 10, 21, 21), fill=INK)
    d.rectangle((11, 11, 20, 20), fill=STONE_DARK)


def gauge_side():
    im, d = stone_panel()
    arrow(d, COPPER)
    return im


def gauge_front():
    im, d = stone_panel()
    dial(d, 300, {0: LIGHT_DEEP, 4: LIGHT, 8: LIGHT})
    return im


def gauge_back():
    im, d = stone_panel()
    bevel(d, (10, 12, 21, 19), RED_DARK, RED, INK, ink=INK)
    d.rectangle((13, 14, 18, 17), fill=RED); d.line((13, 14, 18, 14), fill=RED_LIGHT)
    for x in (7, 24):
        d.rectangle((x - 1, 13, x + 1, 18), fill=INK); d.line((x, 14, x, 17), fill=RED_DARK)
    return im


def threshold_side():
    im, d = stone_panel()
    arrow(d, COPPER, cy=14)
    d.rectangle((6, 25, 25, 28), fill=INK)
    for i in range(4):
        d.rectangle((7 + i * 5, 26, 9 + i * 5, 27), fill=LIGHT if i < 2 else STONE_DARK)
        if i < 2:
            d.point((7 + i * 5, 26), fill=LIGHT_PALE)
    return im


def threshold_front():
    im, d = stone_panel()
    dial(d, 240, {2: RED, 4: RED, 6: RED})
    # threshold set-point wedge on the bezel
    d.polygon([(21, 4), (24, 4), (23, 7)], fill=RED)
    return im


def threshold_back():
    im, d = stone_panel()
    bevel(d, (10, 11, 21, 20), RED_DARK, RED, INK, ink=INK)
    d.rectangle((12, 13, 19, 18), fill=RED); d.line((12, 13, 19, 13), fill=RED_LIGHT)
    d.line((14, 15, 17, 15), fill=BONE); d.line((14, 17, 17, 17), fill=BONE)
    return im


# ---------------------------------------------------------------- rite tablets

def tablet(glyph, colour):
    """Carved stone tablet, rounded shoulders, chiselled border, coloured element rune with a glow, ink outline."""
    im = blank(); d = ImageDraw.Draw(im)
    d.rounded_rectangle((6, 3, 25, 28), radius=4, fill=STONE)
    d.rounded_rectangle((7, 4, 24, 27), radius=3, outline=STONE_LIGHT)
    d.line((24, 6, 24, 25), fill=STONE_DARK); d.line((9, 27, 22, 27), fill=STONE_DARK)
    d.rectangle((9, 7, 22, 24), fill=STONE_DARK)
    d.line((9, 7, 22, 7), fill=INK); d.line((9, 7, 9, 24), fill=INK)
    # chisel notches on the shoulders and foot
    for x in (11, 15, 19):
        d.point((x, 5), fill=STONE_DARK); d.point((x, 26), fill=STONE_LIGHT)
    glyph(d, colour, darken(colour, 0.45), lighten(colour, 0.45))
    return outline(im)


def g_rain(d, c, dark, light):
    d.rounded_rectangle((11, 9, 20, 15), radius=3, fill=c)
    d.line((12, 10, 16, 10), fill=light)
    d.line((12, 15, 19, 15), fill=dark)
    for x, y in ((12, 18), (16, 19), (20, 18)):
        d.line((x, y, x, y + 3), fill=c); d.point((x, y + 3), fill=light)


def g_sky(d, c, dark, light):
    d.ellipse((12, 10, 19, 17), fill=c)
    d.point((13, 11), fill=light); d.point((18, 16), fill=dark)
    for x, y in ((15, 7), (15, 20), (9, 13), (22, 13), (11, 9), (20, 9), (11, 18), (20, 18)):
        d.point((x, y), fill=c)
    d.line((11, 22, 20, 22), fill=dark); d.line((13, 23, 18, 23), fill=dark)


def g_dawn(d, c, dark, light):
    d.pieslice((10, 12, 21, 23), 180, 360, fill=c)
    d.arc((10, 12, 21, 23), 200, 260, fill=light)
    d.line((9, 18, 22, 18), fill=dark); d.line((9, 20, 22, 20), fill=dark)
    d.rectangle((11, 21, 20, 22), fill=STONE_DARK)
    for x0, y0, x1, y1 in ((15, 8, 15, 10), (10, 10, 12, 12), (20, 10, 18, 12)):
        d.line((x0, y0, x1, y1), fill=c); d.point((x0, y0), fill=light)


def g_green(d, c, dark, light):
    d.line((15, 22, 15, 11), fill=c)
    d.point((15, 11), fill=light)
    d.polygon([(14, 15), (10, 11), (11, 17)], fill=c); d.point((11, 13), fill=light)
    d.polygon([(16, 13), (21, 9), (20, 15)], fill=c); d.point((19, 11), fill=light)
    d.line((11, 22, 20, 22), fill=dark); d.line((13, 23, 18, 23), fill=dark)


def g_still(d, c, dark, light):
    d.ellipse((10, 10, 21, 21), fill=c)
    d.ellipse((13, 9, 24, 20), fill=STONE_DARK)
    d.arc((10, 10, 21, 21), 120, 220, fill=light)
    for x, y in ((20, 12), (21, 19), (18, 22)):
        d.point((x, y), fill=light)


def g_ley(d, c, dark, light):
    d.line((15, 8, 15, 24), fill=c); d.point((15, 8), fill=light)
    d.polygon([(15, 12), (20, 16), (15, 20), (10, 16)], fill=c)
    d.polygon([(15, 14), (18, 16), (15, 18), (12, 16)], fill=light)
    d.line((10, 10, 13, 13), fill=dark); d.line((21, 22, 18, 19), fill=dark)
    d.line((21, 10, 18, 13), fill=dark); d.line((10, 22, 13, 19), fill=dark)


# ---------------------------------------------------------------- ley lens

def ley_lens():
    """A round glass lens in a brass ring on a bone handle; the glass shows the ley reading as a gold-to-blue sweep."""
    im = blank(); d = ImageDraw.Draw(im)
    # handle: bone with a copper ferrule, running to the bottom-right
    d.line((21, 21, 28, 28), fill=BONE_SHADE, width=4)
    d.line((21, 20, 27, 26), fill=BONE, width=2)
    d.rectangle((26, 26, 29, 29), fill=COPPER); d.point((26, 26), fill=COPPER_LIGHT)
    d.line((20, 19, 23, 22), fill=COPPER, width=2)
    # brass ring with four set screws
    d.ellipse((2, 2, 22, 22), fill=INK)
    disc(d, (3, 3, 21, 21), BRASS, BRASS_LIGHT, BRASS_DARK)
    for deg in (0, 90, 180, 270):
        d.point(polar(12, 12, 8, deg), fill=INK)
    # glass: a deep blue disc with a gold ley sweep from the top-left and a bright rim reflection
    d.ellipse((6, 6, 18, 18), fill=INK)
    d.ellipse((7, 7, 17, 17), fill=(0x24, 0x4e, 0xa8, 255))
    d.pieslice((7, 7, 17, 17), 180, 300, fill=(0xf0, 0xc8, 0x4a, 255))
    d.pieslice((9, 9, 15, 15), 180, 300, fill=(0xff, 0xe4, 0x8a, 255))
    d.ellipse((10, 10, 14, 14), fill=(0x4a, 0x8a, 0xd8, 255))
    d.point((12, 12), fill=(0xa8, 0xd0, 0xff, 255))
    d.arc((7, 7, 17, 17), 200, 250, fill=(255, 255, 255, 255), width=2)
    return outline(im)


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
