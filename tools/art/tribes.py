"""Painter for the Nine Tribes (design 3.0 §2, art direction §8).

32x32 blocks/items in the Living Lattice palette (top-left light, 1 px ink outline, 2-3 value steps, no noise) and
64x64 Kin entity skins. Glyphs come from tools/art/glyphs.py.

  block/tribe_hearth.png, tribe_hearth_top.png, tribe_hearth_embers.png (white; tinted in-game by tribe colour)
  block/tribe_banner_<id>.png x9          block/kinship_totem_<id>.png x9, kinship_totem_top.png
  item/tribe_mark_<id>.png x9             item/tribe_banner_<id>.png x9   item/tribe_hearth.png
  entity/kin_elder.png kin_drummer.png kin_hunter.png kin_weaver.png kin_cloak.png
Run: python3 tools/art/tribes.py [project root]
"""
from pathlib import Path
import sys
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
from glyphs import TRIBES, COLOURS, GLYPH_NAMES, draw_glyph, shade, lighten, darken  # noqa: E402

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
ASH = (0x2a, 0x2c, 0x30, 255)
EMBER = (255, 255, 255, 255)


def rgba(tribe):
    return COLOURS[tribe] + (255,)


def blank(size=32):
    return Image.new('RGBA', (size, size), (0, 0, 0, 0))


def save(rel, img):
    path = TEX / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def stone_face(color=STONE, light=STONE_LIGHT):
    im = blank(); d = ImageDraw.Draw(im)
    d.rectangle((0, 0, 31, 31), fill=color)
    for y in range(0, 32, 8):
        for x in range(-4 if y % 16 else 0, 32, 10):
            d.rectangle((x, y, x + 9, y + 7), outline=shade(color, -18))
            d.line((x + 1, y + 1, x + 8, y + 1), fill=light)
            d.line((x + 1, y + 1, x + 1, y + 6), fill=light)
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


# ---------------------------------------------------------------- hearth

def hearth_side():
    """Stone bowl: a rimmed basin with a dark ash band and a copper trim line."""
    im = stone_face(); d = ImageDraw.Draw(im)
    d.rectangle((0, 0, 31, 31), outline=INK)
    d.rectangle((2, 10, 29, 31), fill=STONE, outline=shade(STONE, -24))
    d.line((3, 11, 28, 11), fill=STONE_LIGHT); d.line((3, 11, 3, 30), fill=STONE_LIGHT)
    d.rectangle((0, 8, 31, 9), fill=COPPER); d.line((0, 8, 31, 8), fill=shade(COPPER, 35))
    d.rectangle((6, 2, 25, 7), fill=ASH)
    d.line((6, 2, 25, 2), fill=shade(ASH, 20))
    for x in (5, 26):
        d.rectangle((x - 1, 14, x + 1, 16), fill=INK); d.point((x, 14), fill=shade(COPPER, 35))
    # carved band
    for x in range(8, 24, 4):
        d.rectangle((x, 20, x + 1, 24), fill=shade(STONE, -30)); d.point((x, 20), fill=STONE_LIGHT)
    return im


def hearth_top():
    im = stone_face(); d = ImageDraw.Draw(im)
    d.rectangle((0, 0, 31, 31), outline=INK)
    d.ellipse((3, 3, 28, 28), fill=shade(STONE, -20), outline=COPPER, width=2)
    d.ellipse((7, 7, 24, 24), fill=ASH, outline=shade(ASH, -12))
    d.arc((4, 4, 27, 27), 200, 290, fill=STONE_LIGHT)
    for (x, y) in ((11, 11), (19, 13), (14, 19), (20, 20)):
        d.rectangle((x, y, x + 1, y + 1), fill=shade(ASH, 40))
    return im


def hearth_embers():
    """White ember clusters over transparency; tinted in-game by the tribe colour."""
    im = blank(); d = ImageDraw.Draw(im)
    d.ellipse((9, 9, 22, 22), fill=(255, 255, 255, 120))
    for (x, y, s) in ((12, 12, 3), (17, 11, 2), (19, 16, 3), (13, 18, 2), (16, 15, 2)):
        d.rectangle((x, y, x + s, y + s), fill=EMBER)
    for (x, y) in ((11, 16), (20, 13), (15, 20)):
        d.point((x, y), fill=(255, 255, 255, 200))
    return im


def hearth_item():
    im = blank(); d = ImageDraw.Draw(im)
    d.ellipse((3, 14, 28, 29), fill=STONE, outline=INK)
    d.ellipse((5, 15, 26, 25), fill=shade(STONE, -20), outline=COPPER)
    d.ellipse((8, 16, 23, 23), fill=ASH)
    d.arc((4, 15, 27, 28), 20, 160, fill=STONE_LIGHT)
    return im


def hearth_item_embers():
    im = blank(); d = ImageDraw.Draw(im)
    for (x, y, w, h) in ((14, 5, 3, 13), (10, 9, 2, 9), (19, 8, 3, 10), (12, 13, 8, 6)):
        d.rectangle((x, y, x + w, y + h), fill=EMBER)
    d.rectangle((15, 3, 16, 5), fill=(255, 255, 255, 200))
    return im


# ---------------------------------------------------------------- banners

def banner_cloth(tribe):
    """Cloth panel with a bone glyph, notched hem, darker fold on the right. Full 32x32 is the cloth face."""
    c = rgba(tribe); im = blank(); d = ImageDraw.Draw(im)
    d.rectangle((0, 0, 31, 31), fill=c)
    d.rectangle((0, 0, 31, 31), outline=darken(c, 0.55))
    d.rectangle((1, 1, 30, 30), outline=lighten(c, 0.25))
    d.rectangle((22, 2, 29, 29), fill=darken(c, 0.18))
    d.line((22, 2, 22, 29), fill=darken(c, 0.35))
    # top hem loops
    for x in range(3, 30, 6):
        d.rectangle((x, 0, x + 2, 2), fill=WOOD)
    # notched hem
    for x in range(0, 32, 4):
        d.polygon([(x, 31), (x + 2, 27), (x + 4, 31)], fill=(0, 0, 0, 0))
    draw_glyph(d, GLYPH_NAMES[tribe], 7, 8, BONE, scale=2, shadow=darken(c, 0.45))
    return im


def banner_item(tribe):
    """Icon: a pole with a hanging cloth."""
    c = rgba(tribe); im = blank(); d = ImageDraw.Draw(im)
    d.rectangle((6, 1, 8, 30), fill=WOOD, outline=INK)
    d.line((7, 2, 7, 29), fill=WOOD_LIGHT)
    d.rectangle((5, 0, 9, 2), fill=COPPER)
    d.rectangle((9, 3, 27, 25), fill=c, outline=darken(c, 0.55))
    d.line((10, 4, 26, 4), fill=lighten(c, 0.25)); d.line((10, 4, 10, 24), fill=lighten(c, 0.25))
    d.rectangle((23, 4, 26, 24), fill=darken(c, 0.18))
    for x in range(9, 28, 4):
        d.polygon([(x, 26), (x + 2, 23), (x + 4, 26)], fill=(0, 0, 0, 0))
    draw_glyph(d, GLYPH_NAMES[tribe], 13, 8, BONE, scale=1, shadow=darken(c, 0.45))
    return im


# ---------------------------------------------------------------- kinship totem

def rails(d):
    d.rectangle((0, 0, 31, 2), fill=COPPER); d.line((0, 0, 31, 0), fill=shade(COPPER, 35))
    d.rectangle((0, 29, 31, 31), fill=shade(COPPER, -30)); d.line((0, 29, 31, 29), fill=COPPER)
    for x in (3, 28):
        for y in (5, 26):
            d.rectangle((x - 1, y - 1, x + 1, y + 1), fill=INK); d.point((x, y - 1), fill=shade(COPPER, 35))


def kinship_side(tribe):
    c = rgba(tribe); im = plank_face(); d = ImageDraw.Draw(im)
    rails(d)
    d.rectangle((6, 5, 25, 26), fill=INK, outline=shade(COPPER, -32))
    d.rectangle((7, 6, 24, 25), fill=darken(c, 0.55))
    d.line((7, 6, 24, 6), fill=darken(c, 0.3)); d.line((7, 6, 7, 25), fill=darken(c, 0.3))
    # woven thread band behind the glyph
    for yy in (8, 23):
        d.line((8, yy, 23, yy), fill=darken(c, 0.15))
    draw_glyph(d, GLYPH_NAMES[tribe], 7, 7, lighten(c, 0.3), scale=2, shadow=darken(c, 0.7))
    return im


def kinship_top():
    im = plank_face(); d = ImageDraw.Draw(im)
    rails(d)
    d.ellipse((5, 5, 26, 26), fill=shade(BONE, -28), outline=COPPER, width=2)
    d.ellipse((9, 9, 22, 22), outline=shade(BONE, -49))
    d.line((6, 6, 12, 12), fill=shade(BONE, -8))
    d.rectangle((14, 14, 17, 17), fill=LIGHT)
    return im


# ---------------------------------------------------------------- tribe mark

def mark_base():
    """Bone disc on a leather cord; the tint layer is added per tribe."""
    im = blank(); d = ImageDraw.Draw(im)
    d.line((15, 0, 9, 8), fill=WOOD, width=2); d.line((16, 0, 22, 8), fill=WOOD, width=2)
    d.ellipse((5, 7, 26, 28), fill=BONE, outline=INK)
    d.ellipse((7, 9, 24, 26), outline=shade(BONE, -35))
    d.arc((6, 8, 25, 27), 200, 300, fill=(255, 255, 255, 255))
    d.rectangle((14, 5, 17, 8), fill=COPPER, outline=INK)
    return im


def mark(tribe, base):
    c = rgba(tribe); im = base.copy(); d = ImageDraw.Draw(im)
    d.ellipse((9, 11, 22, 24), fill=darken(c, 0.1), outline=darken(c, 0.5))
    draw_glyph(d, GLYPH_NAMES[tribe], 11, 13, BONE, scale=1, shadow=darken(c, 0.5))
    return im


# ---------------------------------------------------------------- Kin skins (64x64)

SKIN = (0xa8, 0x7a, 0x58, 255)
SKIN_DARK = (0x86, 0x5e, 0x42, 255)
CLOTH = (0x4a, 0x3b, 0x33, 255)
CLOTH_LIGHT = (0x66, 0x52, 0x47, 255)
LEATHER = (0x6b, 0x44, 0x2c, 255)
LEATHER_LIGHT = (0x8a, 0x5a, 0x3a, 255)

ROLE_TRIM = {
    'elder': (0xe8, 0xd9, 0xa8, 255),   # bone-pale sashes
    'drummer': (0xe0, 0xa3, 0x2d, 255),  # ochre wraps
    'hunter': (0xb8, 0x51, 0x2f, 255),   # rust straps
    'weaver': (0x62, 0xd1, 0xc9, 255),   # teal thread
}


def box(d, x, y, w, h, dp, top, side, front, outline=INK):
    """Paint the six unwrapped faces of a w*h*dp box whose texOffs is (x, y)."""
    # top / bottom
    d.rectangle((x + dp, y, x + dp + w - 1, y + dp - 1), fill=top, outline=outline)
    d.rectangle((x + dp + w, y, x + dp + 2 * w - 1, y + dp - 1), fill=darken(top, 0.3), outline=outline)
    # right, front, left, back
    d.rectangle((x, y + dp, x + dp - 1, y + dp + h - 1), fill=side, outline=outline)
    d.rectangle((x + dp, y + dp, x + dp + w - 1, y + dp + h - 1), fill=front, outline=outline)
    d.rectangle((x + dp + w, y + dp, x + dp + w + dp - 1, y + dp + h - 1), fill=side, outline=outline)
    d.rectangle((x + dp + w + dp, y + dp, x + dp + 2 * w + dp - 1, y + dp + h - 1), fill=darken(front, 0.2), outline=outline)


def kin_skin(role):
    im = Image.new('RGBA', (64, 64), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    trim = ROLE_TRIM[role]
    # head (0,0) 8x8x8
    box(d, 0, 0, 8, 8, 8, SKIN_DARK, SKIN, SKIN)
    d.rectangle((8, 0, 23, 7), fill=CLOTH, outline=INK)  # hair/top wrap
    d.rectangle((9, 1, 22, 2), fill=CLOTH_LIGHT)
    for fx in (10, 13):  # eyes on front face (x 8..15, y 8..15)
        d.rectangle((fx, 11, fx + 1, 12), fill=INK); d.point((fx + 1, 11), fill=(255, 255, 255, 255))
    d.line((10, 14, 13, 14), fill=SKIN_DARK)
    # body (16,16) 8x12x4
    box(d, 16, 16, 8, 12, 4, CLOTH_LIGHT, CLOTH, CLOTH)
    d.rectangle((20, 20, 27, 22), fill=trim)  # front sash
    d.rectangle((21, 23, 26, 31), fill=LEATHER); d.line((21, 23, 26, 23), fill=LEATHER_LIGHT)
    d.rectangle((28, 20, 31, 22), fill=trim)  # side band
    # arms (40,16) right, (0,16) left
    for ax in (40, 0):
        box(d, ax, 16, 4, 12, 4, CLOTH_LIGHT, CLOTH, CLOTH)
        d.rectangle((ax + 4, 24, ax + 7, 27), fill=SKIN)  # bare forearm
        d.rectangle((ax + 4, 22, ax + 7, 23), fill=trim)  # armband
        d.rectangle((ax + 4, 28, ax + 7, 30), fill=SKIN_DARK)  # hand
    # legs (0,32) right, (16,32) left
    for lx in (0, 16):
        box(d, lx, 32, 4, 12, 4, LEATHER_LIGHT, LEATHER, LEATHER)
        d.rectangle((lx + 4, 41, lx + 7, 43), fill=darken(LEATHER, 0.35))  # boot
        d.rectangle((lx + 4, 37, lx + 7, 37), fill=trim)  # knee wrap
    # role details
    if role == 'elder':
        d.rectangle((20, 18, 27, 19), fill=BONE)  # bead collar
        d.rectangle((10, 13, 13, 13), fill=BONE)  # grey beard line
    elif role == 'drummer':
        d.rectangle((44, 24, 47, 27), fill=trim)  # wrapped hands
        d.rectangle((4, 24, 7, 27), fill=trim)
    elif role == 'hunter':
        d.line((20, 16, 27, 27), fill=trim, width=1)  # cross strap
        d.rectangle((44, 22, 47, 25), fill=LEATHER)  # bracer
    elif role == 'weaver':
        for yy in (25, 27, 29):
            d.line((21, yy, 26, yy), fill=trim)  # threaded skirt
    return im


def kin_cloak():
    """Mask (32,0) 9x9x1, cloak (32,32) 10x14x2 and hood (0,48) 9x5x9 as light grey (tinted by tribe colour)."""
    im = Image.new('RGBA', (64, 64), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    base = (210, 210, 210, 255); shade1 = (170, 170, 170, 255); ink = (60, 60, 60, 255)
    # mask front face: x 33..41, y 1..9 (dp=1)
    box(d, 32, 0, 9, 9, 1, shade1, shade1, base, outline=ink)
    for ex in (35, 38):
        d.rectangle((ex, 4, ex, 5), fill=ink)  # eye slits
    d.line((35, 8, 38, 8), fill=ink)
    d.rectangle((36, 2, 37, 3), fill=(255, 255, 255, 255))  # forehead mark
    # cloak: 10x14x2 at (32,32)
    box(d, 32, 32, 10, 14, 2, shade1, shade1, base, outline=ink)
    for yy in range(36, 47, 3):
        d.line((35, yy, 42, yy), fill=shade1)  # fold lines on the back panel
    d.rectangle((34, 34, 43, 35), fill=(240, 240, 240, 255))  # top-left light
    # hood: 9x5x9 at (0,48)
    box(d, 0, 48, 9, 5, 9, base, shade1, base, outline=ink)
    d.rectangle((10, 49, 17, 49), fill=(240, 240, 240, 255))
    return im


def main():
    save('block/tribe_hearth.png', hearth_side())
    save('block/tribe_hearth_top.png', hearth_top())
    save('block/tribe_hearth_embers.png', hearth_embers())
    save('item/tribe_hearth.png', hearth_item())
    save('item/tribe_hearth_embers.png', hearth_item_embers())
    save('block/kinship_totem_top.png', kinship_top())
    base = mark_base()
    for tribe in TRIBES:
        save(f'block/tribe_banner_{tribe}.png', banner_cloth(tribe))
        save(f'item/tribe_banner_{tribe}.png', banner_item(tribe))
        save(f'block/kinship_totem_{tribe}.png', kinship_side(tribe))
        save(f'item/tribe_mark_{tribe}.png', mark(tribe, base))
    for role in ROLE_TRIM:
        save(f'entity/kin_{role}.png', kin_skin(role))
    save('entity/kin_cloak.png', kin_cloak())
    print('tribes: textures written to', TEX)


if __name__ == '__main__':
    main()
