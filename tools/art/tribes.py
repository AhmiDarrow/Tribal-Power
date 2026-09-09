"""Painter for the Nine Tribes (design 3.0 §2, art direction §8).

32x32 blocks/items in the Living Lattice palette (top-left light, 1 px ink outline, 2-3 value steps, no noise) and
64x64 Kin entity skins. Glyphs come from tools/art/glyphs.py, shared primitives from tools/art/lattice.py.

  block/tribe_hearth.png, tribe_hearth_top.png, tribe_hearth_embers.png (greyscale; tinted in-game by tribe colour)
  block/tribe_banner_<id>.png x9          block/kinship_totem_<id>.png x9, kinship_totem_top.png
  item/tribe_mark_<id>.png x9             item/tribe_banner_<id>.png x9   item/tribe_hearth.png (+ _embers)
Kin entity skins are no longer painted here: textures/entity/* come from tools/art/creatures.py and the Blender
bestiary (--entities prints a notice and writes nothing).
Run: python3 tools/art/tribes.py [project root]
"""
from pathlib import Path
import sys
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
from glyphs import TRIBES, COLOURS, GLYPH_NAMES, draw_glyph  # noqa: E402
from lattice import *  # noqa: E402,F403

ARGS = [a for a in sys.argv[1:] if not a.startswith('--')]
ROOT = Path(ARGS[0]) if ARGS else Path(__file__).resolve().parents[2]
TEX = ROOT / 'src/main/resources/assets/tribalpower/textures'


def rgba(tribe):
    return COLOURS[tribe] + (255,)


def save(rel, img):
    save_to(TEX, rel, img)


# ---------------------------------------------------------------- hearth
#
# The hearth model (tools/generate_tribe_data.py) is a stone plinth [1,0,1]-[15,4,15], a square bowl of four 2 px walls
# [2,4,2]-[14,9,14] and ember planes inside. Every face uses the model's default UV, so the side texture is painted as
# an elevation of the whole block and the top texture as its plan view:
#   side  rows  0..7  unused        rows  8..17 bowl inner wall (v 4..9)   rows 14..17 rim band
#         rows 14..23 bowl belly    rows 24..31 plinth (v 12..16)
#   top   texels 2..29 plinth, 4..27 bowl rim, 8..23 ash floor
#   embers rows 0..7 x cols 6..25: flame planes (uv [3,0,13,4]); rows 8..31 x cols 4..27: ember bed (uv [2,4,14,16]).

def hearth_side():
    im = blank(); d = ImageDraw.Draw(im)
    # bowl interior (seen on the inner faces of the walls): soot-dark stone with a lighter top lip
    d.rectangle((2, 8, 29, 17), fill=ASH)
    d.line((2, 8, 29, 8), fill=ASH_LIGHT)
    for x in range(5, 28, 6):
        d.line((x, 10, x, 16), fill=ASH_DARK)
    # rim band: copper trim on the outer wall top edge
    d.rectangle((4, 14, 27, 15), fill=COPPER); d.line((4, 14, 27, 14), fill=COPPER_LIGHT)
    # bowl belly: carved stone with an incised tick band
    bevel(d, (4, 16, 27, 23), STONE, STONE_LIGHT, STONE_DARK)
    for x in range(7, 26, 4):
        d.rectangle((x, 18, x, 21), fill=STONE_DARK); d.point((x + 1, 18), fill=STONE_LIGHT)
    d.line((4, 23, 27, 23), fill=INK)
    # plinth: two courses of darker stone blocks with a light top edge
    d.rectangle((2, 24, 29, 31), fill=STONE_DARK)
    d.line((2, 24, 29, 24), fill=STONE_LIGHT)
    for x in range(2, 30, 7):
        d.line((x, 25, x, 27), fill=INK)
    for x in range(5, 30, 7):
        d.line((x, 29, x, 31), fill=INK)
    d.line((2, 28, 29, 28), fill=INK)
    d.line((2, 24, 2, 31), fill=STONE_LIGHT)
    d.line((29, 25, 29, 31), fill=INK)
    d.line((2, 31, 29, 31), fill=INK)
    return im


def hearth_top():
    im = blank(); d = ImageDraw.Draw(im)
    # plinth ledge
    bevel(d, (2, 2, 29, 29), STONE_DARK, STONE, INK)
    # bowl rim: stone with a copper inlay line
    bevel(d, (4, 4, 27, 27), STONE, STONE_LIGHT, STONE_DARK, ink=INK)
    d.rectangle((6, 6, 25, 25), outline=COPPER); d.line((6, 6, 25, 6), fill=COPPER_LIGHT); d.line((6, 6, 6, 25), fill=COPPER_LIGHT)
    # ash floor with dark coals
    d.rectangle((8, 8, 23, 23), fill=ASH, outline=ASH_DARK)
    for (x, y) in ((10, 10), (17, 11), (13, 15), (19, 18), (10, 20), (15, 21)):
        d.rectangle((x, y, x + 2, y + 1), fill=ASH_DARK); d.point((x, y), fill=ASH_LIGHT)
    return im


def hearth_embers():
    """Greyscale glow over transparency; tinted in-game by the tribe colour (tint index 0)."""
    im = blank(); d = ImageDraw.Draw(im)
    W = (255, 255, 255, 255); G = (205, 205, 205, 255); K = (140, 140, 140, 255)
    # flame planes (rows 0..7, cols 6..25): three tongues rising from the coals
    for (x, h) in ((9, 4), (15, 7), (21, 5)):
        for i in range(h):
            w = max(1, (h - i) // 2)
            d.rectangle((x - w, 7 - i, x + w, 7 - i), fill=G if i < h - 2 else W)
        d.point((x, 7 - h + 1), fill=W)
    d.rectangle((6, 7, 25, 7), fill=K)
    # ember bed (rows 8..31, cols 4..27): a cracked crust with a glowing heart
    d.rectangle((4, 8, 27, 31), fill=(0, 0, 0, 0))
    for (x, y, w, h) in ((6, 10, 20, 20),):
        d.rounded_rectangle((x, y, x + w - 1, y + h - 1), radius=5, fill=K)
    d.ellipse((9, 13, 22, 26), fill=G)
    d.ellipse((12, 16, 19, 23), fill=W)
    # dark cracks between the coals
    for (x0, y0, x1, y1) in ((8, 20, 14, 22), (18, 12, 20, 18), (16, 22, 23, 24), (11, 11, 13, 15), (21, 25, 24, 28)):
        d.line((x0, y0, x1, y1), fill=(0, 0, 0, 0), width=1)
    return im


def hearth_item():
    im = blank(); d = ImageDraw.Draw(im)
    # a wide stone bowl on a plinth, seen slightly from above
    d.rectangle((6, 25, 25, 29), fill=STONE_DARK); d.line((6, 25, 25, 25), fill=STONE_LIGHT)
    d.ellipse((2, 13, 29, 27), fill=STONE)
    d.arc((2, 13, 29, 27), 200, 320, fill=STONE_LIGHT, width=2)
    d.arc((2, 13, 29, 27), 20, 150, fill=STONE_DARK, width=2)
    d.ellipse((4, 12, 27, 20), fill=COPPER); d.arc((4, 12, 27, 20), 190, 330, fill=COPPER_LIGHT, width=1)
    d.ellipse((6, 13, 25, 19), fill=ASH); d.arc((6, 13, 25, 19), 20, 160, fill=ASH_LIGHT)
    for x in range(9, 24, 4):
        d.line((x, 22, x, 24), fill=STONE_DARK)
    return outline(im)


def hearth_item_embers():
    im = blank(); d = ImageDraw.Draw(im)
    W = (255, 255, 255, 255); G = (200, 200, 200, 255)
    d.ellipse((8, 14, 23, 18), fill=G)
    for (x, h) in ((11, 6), (16, 11), (21, 8)):
        for i in range(h):
            w = max(0, (h - i) // 3)
            d.rectangle((x - w, 15 - i, x + w, 15 - i), fill=G if i < h - 3 else W)
    d.ellipse((12, 14, 19, 17), fill=W)
    return im


# ---------------------------------------------------------------- banners
#
# Cloth texture layout: the cloth is the region cols 4..27 x rows 0..26 (uv [2,0,14,13.5]); everything outside is
# transparent so the 1 px thick element's edge faces stay clean.

def banner_cloth(tribe):
    c = rgba(tribe); dark, base, light = ramp(c, 0.28); deep = darken(c, 0.5)
    im = blank(); d = ImageDraw.Draw(im)
    # cloth body with three vertical fold bands (light, base, shadow) and a darker rolled hem at the top
    d.rectangle((4, 0, 27, 26), fill=base)
    d.rectangle((5, 1, 9, 25), fill=light)
    d.rectangle((14, 1, 16, 25), fill=dark)
    d.rectangle((23, 1, 26, 25), fill=dark)
    d.rectangle((4, 0, 27, 2), fill=deep); d.line((4, 0, 27, 0), fill=dark)
    for x in range(6, 27, 5):  # stitch loops along the hem
        d.point((x, 1), fill=BONE_SHADE)
    # fringe: alternating tassels, notched by transparency
    for x in range(4, 28, 3):
        d.rectangle((x, 24, x + 1, 26), fill=dark)
        d.rectangle((x + 2, 24, x + 2, 26), fill=(0, 0, 0, 0))
    d.line((4, 23, 27, 23), fill=deep)
    # glyph panel: bone glyph with a deep drop shadow, centred on the cloth
    draw_glyph(d, GLYPH_NAMES[tribe], 7, 4, BONE, scale=2, shadow=deep)
    # outline
    d.rectangle((4, 0, 4, 23), fill=deep); d.rectangle((27, 0, 27, 23), fill=deep)
    return im


def banner_item(tribe):
    """Icon: a copper-capped pole with a bracket and a hanging, fringed cloth."""
    c = rgba(tribe); dark, base, light = ramp(c, 0.28); deep = darken(c, 0.5)
    im = blank(); d = ImageDraw.Draw(im)
    # pole
    d.rectangle((5, 2, 7, 30), fill=WOOD); d.line((5, 2, 5, 30), fill=WOOD_LIGHT); d.line((7, 2, 7, 30), fill=WOOD_DARK)
    d.rectangle((4, 0, 8, 2), fill=COPPER); d.line((4, 0, 8, 0), fill=COPPER_LIGHT)
    d.rectangle((5, 4, 24, 5), fill=WOOD_LIGHT); d.line((5, 5, 24, 5), fill=WOOD_DARK)  # crossbar
    d.point((24, 4), fill=COPPER)
    # cloth
    d.rectangle((9, 6, 24, 24), fill=base)
    d.rectangle((10, 7, 12, 23), fill=light)
    d.rectangle((16, 7, 17, 23), fill=dark)
    d.rectangle((22, 7, 23, 23), fill=dark)
    d.rectangle((9, 6, 24, 7), fill=deep)
    for x in range(9, 25, 3):
        d.rectangle((x, 24, x + 1, 26), fill=dark)
    d.line((9, 23, 24, 23), fill=deep)
    draw_glyph(d, GLYPH_NAMES[tribe], 12, 10, BONE, scale=1, shadow=deep)
    return outline(im)


# ---------------------------------------------------------------- kinship totem
#
# The glyph panel element [3,4,3]-[13,13,13] shows texels 6..26 x 6..24 of the side texture (default UV).

def kinship_side(tribe):
    c = rgba(tribe); dark, base, light = ramp(c, 0.3)
    im = plank_face(); d = ImageDraw.Draw(im)
    rails(d)
    # panel: copper frame, ink field, tribe-coloured woven band top and bottom
    d.rectangle((5, 5, 26, 24), fill=COPPER_DARK)
    d.line((5, 5, 26, 5), fill=COPPER); d.line((5, 5, 5, 24), fill=COPPER)
    d.rectangle((6, 6, 25, 23), fill=INK)
    d.rectangle((7, 7, 24, 22), fill=darken(c, 0.62))
    for yy in (7, 22):
        for x in range(7, 25, 2):
            d.point((x, yy), fill=dark)
    draw_glyph(d, GLYPH_NAMES[tribe], 7, 6, light, scale=2, shadow=darken(c, 0.8))
    return im


def kinship_top():
    im = plank_face(); d = ImageDraw.Draw(im)
    rails(d)
    disc(d, (5, 5, 26, 26), BONE_SHADE, BONE, darken(BONE_SHADE, 0.3), ink=COPPER)
    d.ellipse((9, 9, 22, 22), outline=darken(BONE_SHADE, 0.3))
    d.rectangle((14, 14, 17, 17), fill=LIGHT); d.point((14, 14), fill=LIGHT_PALE)
    return im


# ---------------------------------------------------------------- tribe mark

def mark(tribe):
    """A fired clay pendant in the tribe colour, bone-rimmed, hung on a knotted leather cord."""
    c = rgba(tribe); dark, base, light = ramp(c, 0.3)
    im = blank(); d = ImageDraw.Draw(im)
    # cord
    d.line((9, 9, 15, 1), fill=WOOD_LIGHT, width=2); d.line((16, 1, 22, 9), fill=WOOD, width=2)
    d.rectangle((14, 0, 17, 2), fill=WOOD_DARK); d.point((15, 0), fill=WOOD_LIGHT)
    # bone rim and clay disc
    disc(d, (5, 8, 26, 29), BONE, BONE_LIGHT, BONE_SHADE, ink=INK)
    disc(d, (8, 11, 23, 26), base, light, dark)
    d.ellipse((8, 11, 23, 26), outline=darken(c, 0.55))
    # hanging loop
    d.rectangle((14, 6, 17, 9), fill=COPPER, outline=INK); d.point((15, 7), fill=COPPER_LIGHT)
    draw_glyph(d, GLYPH_NAMES[tribe], 11, 14, BONE_LIGHT, scale=1, shadow=darken(c, 0.55))
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


DEPRECATED = ('{name}: --entities is retired; textures/entity/* are painted by tools/art/creatures.py and '
              'tools/blender_bestiary.py (see art/creatures). Nothing was written.')


def main():
    save('block/tribe_hearth.png', hearth_side())
    save('block/tribe_hearth_top.png', hearth_top())
    save('block/tribe_hearth_embers.png', hearth_embers())
    save('item/tribe_hearth.png', hearth_item())
    save('item/tribe_hearth_embers.png', hearth_item_embers())
    save('block/kinship_totem_top.png', kinship_top())
    for tribe in TRIBES:
        save(f'block/tribe_banner_{tribe}.png', banner_cloth(tribe))
        save(f'item/tribe_banner_{tribe}.png', banner_item(tribe))
        save(f'block/kinship_totem_{tribe}.png', kinship_side(tribe))
        save(f'item/tribe_mark_{tribe}.png', mark(tribe))
    if '--entities' in sys.argv:
        print(DEPRECATED.format(name='tribes'))
    print('tribes: textures written to', TEX)


if __name__ == '__main__':
    main()
