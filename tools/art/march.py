"""Painter for The March structures and The Unsung (design 3.0 §3, art direction §8).

32x32 block textures in the Living Lattice palette (top-left light, 1 px ink outline, 2-3 value
steps per material, no noise) plus the boss sheet:
  block/silent_drum.png  block/silent_drum_top.png  block/lore_tablet.png
  entity/the_unsung.png (128x128)  entity/the_unsung_glow.png
The boss UV layout mirrors boss/client/TheUnsungModel (half-scale boxes, vanilla cube unwrap).
Run: python3 tools/art/march.py [project root]
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
LOOM = (0x62, 0xd1, 0xc9, 255)
LOOM_PALE = (0xc9, 0xf6, 0xf1, 255)
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


def shade(c, amount):
    return tuple(max(0, min(255, x + amount)) for x in c[:3]) + (255,)


def blank(size=32):
    return Image.new('RGBA', (size, size), (0, 0, 0, 0))


def save(rel, img):
    path = TEX / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


# ---------------------------------------------------------------- blocks

def drum_side():
    """Lacquered drum shell: dark red body, two gold ring bands with rivets, a base plinth."""
    im = blank(); d = ImageDraw.Draw(im)
    d.rectangle((0, 0, 31, 31), fill=LACQUER)
    d.line((0, 0, 31, 0), fill=LACQUER_LIGHT); d.line((0, 0, 0, 31), fill=LACQUER_LIGHT)
    d.line((31, 0, 31, 31), fill=LACQUER_DEEP); d.line((0, 31, 31, 31), fill=LACQUER_DEEP)
    # vertical stave seams
    for x in (8, 16, 24):
        d.line((x, 3, x, 28), fill=LACQUER_DEEP); d.line((x + 1, 3, x + 1, 28), fill=LACQUER_LIGHT)
    # ring bands
    for y in (5, 22):
        d.rectangle((0, y, 31, y + 3), fill=GOLD)
        d.line((0, y, 31, y), fill=GOLD_LIGHT); d.line((0, y + 3, 31, y + 3), fill=shade(GOLD, -50))
        for x in range(3, 32, 7):
            d.point((x, y + 1), fill=INK); d.point((x, y + 2), fill=GOLD_LIGHT)
    # rim and plinth
    d.rectangle((0, 0, 31, 1), fill=shade(HIDE_DARK, -20)); d.line((0, 0, 31, 0), fill=HIDE)
    d.rectangle((0, 29, 31, 31), fill=STONE); d.line((0, 29, 31, 29), fill=STONE_LIGHT)
    # a faint violet rune in the middle band gap
    d.line((12, 13, 19, 13), fill=RUNE); d.line((15, 10, 15, 17), fill=RUNE); d.point((16, 11), fill=RUNE_PALE)
    d.rectangle((0, 0, 31, 31), outline=INK)
    return im


def drum_top():
    """Taut hide stretched over the rim, laced with a gold ring and a centre rune."""
    im = blank(); d = ImageDraw.Draw(im)
    d.rectangle((0, 0, 31, 31), fill=LACQUER)
    d.ellipse((1, 1, 30, 30), fill=GOLD, outline=INK)
    d.ellipse((3, 3, 28, 28), fill=HIDE, outline=shade(GOLD, -50))
    d.arc((3, 3, 28, 28), 200, 320, fill=HIDE_LIGHT, width=2)
    d.arc((3, 3, 28, 28), 20, 140, fill=HIDE_DARK, width=2)
    # lacing marks around the rim
    for i in range(12):
        import math
        a = i * math.tau / 12
        x, y = 15.5 + 12.5 * math.cos(a), 15.5 + 12.5 * math.sin(a)
        d.point((round(x), round(y)), fill=INK)
    # centre rune: a circle crossed by a stroke
    d.ellipse((11, 11, 20, 20), outline=RUNE)
    d.line((15, 8, 15, 23), fill=RUNE); d.point((15, 9), fill=RUNE_PALE)
    d.rectangle((0, 0, 31, 31), outline=INK)
    return im


def tablet_face():
    """A cracked slate tablet in a copper frame carrying rows of carved script."""
    im = blank(); d = ImageDraw.Draw(im)
    d.rectangle((0, 0, 31, 31), fill=STONE)
    d.rectangle((1, 1, 30, 30), outline=COPPER); d.line((1, 1, 30, 1), fill=shade(COPPER, 35)); d.line((1, 1, 1, 30), fill=shade(COPPER, 35))
    d.rectangle((3, 3, 28, 28), fill=shade(STONE, -14))
    d.line((3, 3, 28, 3), fill=STONE_LIGHT); d.line((3, 3, 3, 28), fill=STONE_LIGHT)
    # carved script rows
    for y in range(7, 26, 4):
        x = 6
        while x < 25:
            w = 2 + ((x * 7 + y * 3) % 3)
            d.line((x, y, x + w, y), fill=LOOM if y == 7 else STONE_LIGHT)
            x += w + 2
    # crack
    d.line([(20, 3), (18, 10), (21, 16), (19, 28)], fill=INK)
    d.line([(21, 3), (19, 10), (22, 16), (20, 28)], fill=STONE_LIGHT)
    d.rectangle((0, 0, 31, 31), outline=INK)
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


def main():
    save('block/silent_drum.png', drum_side())
    save('block/silent_drum_top.png', drum_top())
    save('block/lore_tablet.png', tablet_face())
    save('block/lore_tablet_back.png', tablet_back())
    save('entity/the_unsung.png', paint_unsung(False))
    save('entity/the_unsung_glow.png', paint_unsung(True))
    print('march art written to', TEX)


if __name__ == '__main__':
    main()
