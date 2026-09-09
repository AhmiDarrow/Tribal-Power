"""Painter for The March structures and The Unsung (design 3.0 §3, art direction §8).

32x32 block textures in the Living Lattice palette (top-left light, 1 px ink outline, 2-3 value
steps per material, no noise) plus the boss sheet:
  block/silent_drum.png  block/silent_drum_top.png  block/lore_tablet.png
  entity/the_unsung.png (128x128)  entity/the_unsung_glow.png
The boss UV layout mirrors boss/client/TheUnsungModel (half-scale boxes, vanilla cube unwrap).
Run: python3 tools/art/march.py [project root]   (--entities is retired: the creature art pass owns textures/entity)
"""
from pathlib import Path
import math
import sys
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
from lattice import COPPER, COPPER_LIGHT, LIGHT, LOOM_PALE, blank, shade  # noqa: E402

ARGS = [a for a in sys.argv[1:] if not a.startswith('--')]
ROOT = Path(ARGS[0]) if ARGS else Path(__file__).resolve().parents[2]
TEX = ROOT / 'src/main/resources/assets/tribalpower/textures'

INK = (17, 26, 34, 255)
WOOD = (0x5a, 0x3b, 0x2e, 255)
WOOD_LIGHT = (0x7a, 0x51, 0x38, 255)
STONE = (0x3a, 0x4a, 0x55, 255)
STONE_LIGHT = (0x55, 0x66, 0x72, 255)
LOOM = (0x62, 0xd1, 0xc9, 255)
LACQUER = (0x4a, 0x1f, 0x22, 255)        # deep lacquered drum shell
LACQUER_LIGHT = (0x6e, 0x2f, 0x30, 255)
LACQUER_DEEP = (0x2d, 0x12, 0x16, 255)
HIDE = (0xc9, 0xa9, 0x74, 255)           # taut hide
HIDE_LIGHT = (0xe3, 0xc6, 0x92, 255)
HIDE_DARK = (0x9d, 0x7f, 0x52, 255)
GOLD = (0xd9, 0xa4, 0x3a, 255)
GOLD_LIGHT = (0xf2, 0xc9, 0x6b, 255)
BONE = (225, 217, 189, 255)
BONE_DARK = (170, 160, 130, 255)
RUNE = (0x9b, 0x6c, 0xff, 255)           # spirit violet rune lines
RUNE_PALE = (0xd8, 0xc4, 0xff, 255)
GLOW_EYE = (0xff, 0xf0, 0xa0, 255)
BLACK = (0, 0, 0, 255)


def save(rel, img):
    path = TEX / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


# ---------------------------------------------------------------- blocks
#
# Silent Drum model (models/block/silent_drum.json): plinth [1,0,1]-[15,3,15], body [2,3,2]-[14,13,14], two bands
# [1,5..7] and [1,9..11], rim [1,13,1]-[15,16,15]. Every side face uses the default UV, so the side texture is one
# elevation: rows 0..5 top hoop, 6..9 / 14..17 / 22..25 laced body, 10..13 / 18..21 bands, 26..31 plinth.

def drum_side():
    im = blank(); d = ImageDraw.Draw(im)
    # body staves (cols 4..27)
    d.rectangle((4, 6, 27, 25), fill=LACQUER)
    for x in (4, 10, 16, 22):
        d.line((x, 6, x, 25), fill=LACQUER_DEEP); d.line((x + 1, 6, x + 1, 25), fill=LACQUER_LIGHT)
    d.line((27, 6, 27, 25), fill=LACQUER_DEEP)
    # rope lacing: zigzag cords between the hoops, knotted where they meet a band
    for y0 in (6, 14, 22):
        for x in range(5, 27, 6):
            d.line((x, y0, x + 3, y0 + 3), fill=HIDE_DARK); d.line((x + 1, y0, x + 3, y0 + 2), fill=HIDE)
            d.line((x + 3, y0 + 3, x + 6, y0), fill=HIDE_DARK); d.line((x + 4, y0 + 2, x + 6, y0), fill=HIDE)
            d.point((x + 3, y0 + 3), fill=BONE)
    # lacquered bands (cols 2..29) with brass edge and rivets
    for y in (10, 18):
        d.rectangle((2, y, 29, y + 3), fill=LACQUER_LIGHT)
        d.line((2, y, 29, y), fill=GOLD)
        d.line((2, y + 3, 29, y + 3), fill=LACQUER_DEEP)
        for x in range(4, 30, 6):
            d.point((x, y + 2), fill=INK); d.point((x + 1, y + 2), fill=GOLD_LIGHT)
        d.line((2, y, 2, y + 3), fill=INK); d.line((29, y, 29, y + 3), fill=INK)
    # top hoop (rows 0..5): hide folded over the edge, lacquer below, bone lacing pegs
    d.rectangle((2, 0, 29, 1), fill=HIDE); d.line((2, 0, 29, 0), fill=HIDE_LIGHT)
    d.rectangle((2, 2, 29, 5), fill=LACQUER_LIGHT); d.line((2, 5, 29, 5), fill=LACQUER_DEEP)
    for x in range(5, 28, 6):
        d.rectangle((x - 1, 2, x + 1, 4), fill=INK); d.rectangle((x, 2, x, 3), fill=BONE); d.point((x, 4), fill=BONE_DARK)
    d.line((2, 0, 2, 5), fill=INK); d.line((29, 0, 29, 5), fill=INK)
    # plinth (rows 26..31): stone courses
    d.rectangle((2, 26, 29, 31), fill=STONE); d.line((2, 26, 29, 26), fill=STONE_LIGHT)
    d.line((2, 29, 29, 29), fill=shade(STONE, -30))
    for x in (8, 17, 26):
        d.line((x, 27, x, 28), fill=shade(STONE, -30))
    for x in (4, 13, 22):
        d.line((x, 30, x, 31), fill=shade(STONE, -30))
    d.line((2, 26, 2, 31), fill=STONE_LIGHT); d.line((29, 27, 29, 31), fill=INK); d.line((2, 31, 29, 31), fill=INK)
    # a faint violet rune on the front stave, between the bands
    d.line((14, 15, 17, 15), fill=RUNE); d.point((15, 14), fill=RUNE_PALE); d.point((15, 16), fill=RUNE)
    return im


def drum_top():
    """Plan view for the rim's up face (uv [1,1,15,15] -> texels 2..29): lacquered hoop, laced hide with a crack."""
    im = blank(); d = ImageDraw.Draw(im)
    d.rectangle((2, 2, 29, 29), fill=LACQUER_LIGHT)
    d.line((2, 2, 29, 2), fill=GOLD_LIGHT); d.line((2, 2, 2, 29), fill=GOLD_LIGHT)
    d.line((29, 2, 29, 29), fill=LACQUER_DEEP); d.line((2, 29, 29, 29), fill=LACQUER_DEEP)
    d.ellipse((4, 4, 27, 27), fill=INK)
    d.ellipse((5, 5, 26, 26), fill=HIDE)
    d.arc((5, 5, 26, 26), 195, 290, fill=HIDE_LIGHT, width=2)
    d.arc((5, 5, 26, 26), 15, 110, fill=HIDE_DARK, width=2)
    # lacing holes just inside the hoop
    for i in range(12):
        a = i * math.tau / 12 + math.tau / 24
        x, y = 15.5 + 10 * math.cos(a), 15.5 + 10 * math.sin(a)
        d.point((round(x), round(y)), fill=INK)
    # worn centre and a crack running from the rim with a lit edge
    d.ellipse((11, 11, 20, 20), fill=HIDE_DARK)
    d.ellipse((12, 12, 19, 19), fill=HIDE)
    d.line([(21, 6), (18, 11), (20, 15), (17, 20), (19, 25)], fill=INK)
    d.line([(22, 6), (19, 11), (21, 15), (18, 20), (20, 25)], fill=HIDE_LIGHT)
    # rune ring
    d.ellipse((13, 13, 18, 18), outline=RUNE); d.point((13, 13), fill=RUNE_PALE)
    return im


def tablet_face():
    """Etched slate tablet for the north face (uv [1,2,15,14] -> texels 2..29 x 4..27): chamfered edges, copper pegs,
    rows of carved script with a teal title line, one crack."""
    im = blank(); d = ImageDraw.Draw(im)
    d.rectangle((2, 4, 29, 27), fill=INK)
    d.rectangle((3, 5, 28, 26), fill=STONE)
    d.line((3, 5, 28, 5), fill=STONE_LIGHT); d.line((3, 5, 3, 26), fill=STONE_LIGHT)
    d.line((28, 6, 28, 26), fill=shade(STONE, -30)); d.line((4, 26, 28, 26), fill=shade(STONE, -30))
    d.rectangle((5, 7, 26, 24), fill=shade(STONE, -12))
    d.line((5, 7, 26, 7), fill=shade(STONE, -30)); d.line((5, 7, 5, 24), fill=shade(STONE, -30))
    for (x, y) in ((4, 6), (27, 6), (4, 25), (27, 25)):
        d.point((x, y), fill=COPPER); d.point((x, y - 1) if y == 6 else (x, y + 1), fill=COPPER_LIGHT if y == 6 else INK)
    # etched script: a teal title glyph line, then dashes of varying length
    d.line((7, 9, 12, 9), fill=LOOM); d.line((14, 9, 15, 9), fill=LOOM); d.line((17, 9, 22, 9), fill=LOOM)
    d.point((7, 10), fill=LOOM_PALE)
    for y in (12, 15, 18, 21):
        x = 7
        while x < 24:
            w = 1 + ((x * 5 + y * 3) % 3)
            d.line((x, y, x + w, y), fill=STONE_LIGHT); d.line((x, y + 1, x + w, y + 1), fill=shade(STONE, -34))
            x += w + 2
    # crack with a lit edge
    d.line([(21, 5), (19, 11), (22, 16), (20, 26)], fill=INK)
    d.line([(22, 5), (20, 11), (23, 16), (21, 26)], fill=STONE_LIGHT)
    return im


def tablet_back():
    im = blank(); d = ImageDraw.Draw(im)
    d.rectangle((0, 0, 31, 31), fill=STONE)
    for y in range(0, 32, 8):
        for x in range(-4 if y % 16 else 0, 32, 10):
            d.rectangle((x, y, x + 9, y + 7), outline=shade(STONE, -18))
            d.line((x + 1, y + 1, x + 8, y + 1), fill=STONE_LIGHT)
    d.rectangle((0, 0, 31, 31), outline=INK)
    return im


# ---------------------------------------------------------------- the unsung

class Sheet:
    """Paints boxes with the vanilla cube unwrap: [west][north][east][south] under [down][up]."""

    def __init__(self, size=128):
        self.im = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.im)

    def box(self, u, v, w, h, dp, side, top=None, bottom=None, front=None):
        top = top or side; bottom = bottom or side; front = front or side
        bottom(self.d, u + dp, v, w, dp)
        top(self.d, u + dp + w, v, w, dp)
        side(self.d, u, v + dp, dp, h)
        front(self.d, u + dp, v + dp, w, h)
        side(self.d, u + dp + w, v + dp, dp, h)
        side(self.d, u + dp + w + dp, v + dp, w, h)


def flat(color, light=None, dark=None):
    def paint(d, x, y, w, h):
        if w <= 0 or h <= 0:
            return
        d.rectangle((x, y, x + w - 1, y + h - 1), fill=color)
        if light and w > 2 and h > 2:
            d.line((x, y, x + w - 1, y), fill=light); d.line((x, y, x, y + h - 1), fill=light)
        if dark and w > 2 and h > 2:
            d.line((x + w - 1, y, x + w - 1, y + h - 1), fill=dark); d.line((x, y + h - 1, x + w - 1, y + h - 1), fill=dark)
    return paint


def shell(d, x, y, w, h):
    """Drum panel: lacquered staves with a faint rune column."""
    flat(LACQUER, LACQUER_LIGHT, LACQUER_DEEP)(d, x, y, w, h)
    if w < 6 or h < 6:
        return
    for sx in range(x + 4, x + w - 1, 5):
        d.line((sx, y + 1, sx, y + h - 2), fill=LACQUER_DEEP); d.line((sx + 1, y + 1, sx + 1, y + h - 2), fill=LACQUER_LIGHT)
    cx, cy = x + w // 2, y + h // 2
    d.line((cx, y + 3, cx, y + h - 4), fill=RUNE)
    for ry in range(y + 5, y + h - 4, 4):
        d.line((cx - 2, ry, cx + 2, ry), fill=RUNE)
    d.point((cx, y + 3), fill=RUNE_PALE)


def hide(d, x, y, w, h):
    flat(HIDE, HIDE_LIGHT, HIDE_DARK)(d, x, y, w, h)
    if w >= 12 and h >= 12:
        d.ellipse((x + 2, y + 2, x + w - 3, y + h - 3), outline=HIDE_DARK)
        d.ellipse((x + w // 2 - 3, y + h // 2 - 3, x + w // 2 + 3, y + h // 2 + 3), outline=RUNE)
        d.line((x + w // 2, y + 3, x + w // 2, y + h - 4), fill=RUNE)


def band(d, x, y, w, h):
    flat(GOLD, GOLD_LIGHT, shade(GOLD, -60))(d, x, y, w, h)
    if h <= 2 and w > 4:
        for rx in range(x + 2, x + w - 1, 4):
            d.point((rx, y), fill=INK)
    elif w >= 12 and h >= 12:
        d.ellipse((x + 1, y + 1, x + w - 2, y + h - 2), outline=shade(GOLD, -60))


def arm(d, x, y, w, h):
    flat(LACQUER_DEEP, LACQUER, BLACK)(d, x, y, w, h)
    if h > 8 and w >= 3:
        cx = x + w // 2
        for ry in range(y + 2, y + h - 2, 3):
            d.point((cx, ry), fill=RUNE)
            if (ry // 3) % 2 == 0:
                d.line((x + 1, ry + 1, x + w - 2, ry + 1), fill=RUNE)


def knuckle(d, x, y, w, h):
    flat(BONE_DARK, BONE, shade(BONE_DARK, -40))(d, x, y, w, h)
    if w >= 4 and h >= 3:
        d.line((x + 1, y + h // 2, x + w - 2, y + h // 2), fill=RUNE)


def mask_side(d, x, y, w, h):
    flat(BONE_DARK, BONE, shade(BONE_DARK, -40))(d, x, y, w, h)
    if w >= 6 and h >= 6:
        d.line((x + 2, y + 1, x + 3, y + h - 2), fill=shade(BONE_DARK, -60))


def mask_front(d, x, y, w, h):
    """The cracked mask: bone plate, two eye slits, a crack from the brow and a rune brow-line."""
    flat(BONE, shade(BONE, 18), BONE_DARK)(d, x, y, w, h)
    if w < 8 or h < 8:
        return
    d.line((x + 1, y + 2, x + w - 2, y + 2), fill=RUNE)
    d.rectangle((x + 1, y + 3, x + 2, y + 4), fill=BLACK)
    d.rectangle((x + w - 3, y + 3, x + w - 2, y + 4), fill=BLACK)
    d.point((x + 2, y + 3), fill=GLOW_EYE); d.point((x + w - 3, y + 3), fill=GLOW_EYE)
    d.line([(x + 4, y), (x + 3, y + 3), (x + 5, y + 5), (x + 4, y + h - 1)], fill=shade(BONE_DARK, -70))
    d.line((x + 2, y + 6, x + w - 3, y + 6), fill=shade(BONE_DARK, -30))


def paint_unsung(glow=False):
    s = Sheet(128)
    if glow:
        # everything black except the rune lines, eyes and band lacquer highlights
        def only(painter, keep):
            def p(d, x, y, w, h):
                painter(d, x, y, w, h)
                px = s.im.load()
                for yy in range(y, y + h):
                    for xx in range(x, x + w):
                        if 0 <= xx < 128 and 0 <= yy < 128:
                            c = px[xx, yy]
                            if c[3] and c[:3] not in keep:
                                px[xx, yy] = BLACK
            return p
        keep_rune = {RUNE[:3], RUNE_PALE[:3], GLOW_EYE[:3]}
        shell_g = only(shell, keep_rune); hide_g = only(hide, keep_rune); band_g = only(band, {GOLD_LIGHT[:3]})
        arm_g = only(arm, keep_rune); knuckle_g = only(knuckle, keep_rune); mask_f = only(mask_front, keep_rune); mask_s = only(mask_side, set())
    else:
        shell_g, hide_g, band_g, arm_g, knuckle_g, mask_f, mask_s = shell, hide, band, arm, knuckle, mask_front, mask_side
    s.box(0, 0, 20, 18, 1, shell_g)                       # north/south panels (20x18x1)
    s.box(44, 0, 1, 18, 18, shell_g)                      # west/east panels (1x18x18)
    s.box(0, 42, 20, 1, 20, band_g, top=hide_g, bottom=hide_g)  # hide top disc
    s.box(0, 64, 22, 1, 22, band_g)                       # ring bands
    s.box(0, 88, 3, 18, 3, arm_g)                         # arm0
    s.box(14, 88, 3, 18, 3, arm_g)                        # arm1
    s.box(28, 88, 8, 7, 8, mask_s)                        # head
    s.box(62, 88, 8, 8, 1, mask_s, front=mask_f)          # mask plate
    s.box(84, 88, 2, 5, 2, knuckle_g)                     # crest
    s.box(84, 96, 2, 6, 2, knuckle_g)                     # horns
    s.box(0, 110, 5, 4, 5, knuckle_g)                     # knuckle club 0
    s.box(22, 110, 5, 4, 5, knuckle_g)                    # knuckle club 1
    return s.im


DEPRECATED = ('{name}: --entities is retired; textures/entity/* are painted by tools/art/creatures.py and '
              'tools/blender_bestiary.py (see art/creatures). Nothing was written.')


def main():
    save('block/silent_drum.png', drum_side())
    save('block/silent_drum_top.png', drum_top())
    save('block/lore_tablet.png', tablet_face())
    save('block/lore_tablet_back.png', tablet_back())
    if '--entities' in sys.argv:
        print(DEPRECATED.format(name='march'))
    print('march art written to', TEX)


if __name__ == '__main__':
    main()
