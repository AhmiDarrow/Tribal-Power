"""Painter for bonded spirits and shared camps (design 3.0 §5-6, art direction §8).

32x32 items in the Living Lattice palette (top-left light, 1 px ink outline, 2-3 value steps, no noise):
  item/bonding_charm.png   a spirit-cord loop with a small mask bead
  item/camp_charter.png    a rolled parchment with a teal wax seal
256x256 entity overlays (greyscale; tinted teal by BondedCollarLayer at render time):
  entity/bonded_collar_<dawn_stag|lantern_fox|mossback>.png  a cord ring around the front of the body box
  entity/bonded_collar.png (copy of the fox layout, kept for the spec name)
The Spirit Light block is invisible and needs no texture.
Run: python3 tools/art/familiars.py [project root]
"""
from pathlib import Path
import sys
from PIL import Image, ImageDraw

ROOT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[2]
TEX = ROOT / 'src/main/resources/assets/tribalpower/textures'

INK = (17, 26, 34, 255)
WOOD = (0x5a, 0x3b, 0x2e, 255)
WOOD_LIGHT = (0x7a, 0x51, 0x38, 255)
COPPER = (0xc0, 0x8a, 0x4e, 255)
LIGHT = (0x7e, 0xff, 0xcb, 255)
LIGHT_DEEP = (0x3f, 0xb3, 0x8e, 255)
BONE = (225, 217, 189, 255)
BONE_SHADE = (176, 165, 136, 255)
PARCHMENT = (0xd9, 0xc4, 0x93, 255)
PARCHMENT_LIGHT = (0xef, 0xdf, 0xb5, 255)
PARCHMENT_DEEP = (0xa8, 0x8e, 0x5e, 255)
TEAL = (0x62, 0xd1, 0xc9, 255)
TEAL_DEEP = (0x2f, 0x8a, 0x86, 255)
TEAL_PALE = (0xc9, 0xf6, 0xf1, 255)
MASK = (0x3a, 0x4a, 0x55, 255)
MASK_LIGHT = (0x55, 0x66, 0x72, 255)

# Body box (w, h, d) of each bondable species, all at UV (0, 0) in GeneratedCreatureLayers.
BODIES = {'dawn_stag': (8, 6, 14), 'lantern_fox': (8, 6, 12), 'mossback': (14, 6, 16)}


def shade(c, amount):
    return tuple(max(0, min(255, x + amount)) for x in c[:3]) + (255,)


def blank(size=32):
    return Image.new('RGBA', (size, size), (0, 0, 0, 0))


def save(rel, img):
    path = TEX / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def outline(im, colour=INK):
    """1 px ink outline around every opaque pixel that touches transparency."""
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


def bonding_charm():
    im = blank(); d = ImageDraw.Draw(im)
    # Spirit cord: a loop of twisted teal thread, lit from the top-left.
    d.ellipse((5, 4, 26, 25), outline=LIGHT_DEEP, width=3)
    d.arc((5, 4, 26, 25), 200, 320, fill=LIGHT, width=2)
    d.arc((5, 4, 26, 25), 20, 140, fill=shade(LIGHT_DEEP, -28), width=1)
    for a in range(0, 360, 30):  # twist ticks
        import math
        x = 15.5 + 10.5 * math.cos(math.radians(a)); y = 14.5 + 10.5 * math.sin(math.radians(a))
        d.point((round(x), round(y)), fill=shade(LIGHT_DEEP, -40))
    # Knot at the top of the loop.
    d.rectangle((13, 2, 18, 6), fill=LIGHT_DEEP)
    d.rectangle((14, 3, 16, 4), fill=LIGHT)
    # Small mask bead hanging in the loop.
    d.rectangle((12, 11, 19, 21), fill=MASK)
    d.rectangle((13, 12, 15, 14), fill=MASK_LIGHT)
    d.rectangle((13, 15, 14, 16), fill=LIGHT)
    d.rectangle((17, 15, 18, 16), fill=LIGHT)
    d.line((14, 19, 17, 19), fill=COPPER)
    d.line((15, 7, 15, 10), fill=BONE_SHADE)
    d.point((15, 8), fill=BONE)
    return outline(im)


def camp_charter():
    im = blank(); d = ImageDraw.Draw(im)
    # Rolled parchment, slightly tilted: a wide scroll with a curled top edge.
    d.rounded_rectangle((6, 7, 25, 27), radius=2, fill=PARCHMENT)
    d.rectangle((7, 8, 24, 9), fill=PARCHMENT_LIGHT)
    d.line((7, 8, 7, 26), fill=PARCHMENT_LIGHT)
    d.rectangle((7, 25, 24, 26), fill=PARCHMENT_DEEP)
    d.line((24, 9, 24, 26), fill=PARCHMENT_DEEP)
    # Curled roll across the top.
    d.rounded_rectangle((4, 3, 27, 8), radius=2, fill=WOOD_LIGHT)
    d.line((5, 4, 26, 4), fill=shade(WOOD_LIGHT, 30))
    d.line((5, 7, 26, 7), fill=WOOD)
    d.rectangle((4, 4, 5, 7), fill=WOOD)
    # Ink lines of the charter.
    for y in (12, 15, 18):
        d.line((10, y, 21, y), fill=PARCHMENT_DEEP)
    d.line((10, 21, 15, 21), fill=PARCHMENT_DEEP)
    # Teal wax seal with a raised hearth glyph.
    d.ellipse((16, 18, 24, 26), fill=TEAL_DEEP)
    d.ellipse((17, 19, 22, 24), fill=TEAL)
    d.point((18, 20), fill=TEAL_PALE)
    d.line((19, 21, 21, 21), fill=TEAL_DEEP)
    d.point((20, 22), fill=TEAL_DEEP)
    return outline(im)


def collar(body):
    """Greyscale cord ring around the front of the body box: top-face front edge + both side-face front edges."""
    w, h, d = body
    im = blank(256); px = im.load()
    cord = (236, 236, 236, 255); knot = (150, 150, 150, 255); glint = (255, 255, 255, 255)
    # Top face: rows just before the side row, spanning the body width.
    for x in range(d, d + w):
        px[x, d - 2] = cord; px[x, d - 1] = knot if x % 3 == 0 else cord
    px[d + w // 2, d - 2] = glint
    # Side faces: the two columns bordering the front face, full box height.
    for y in range(d, d + h):
        for x in (d - 2, d - 1, d + w, d + w + 1):
            px[x, y] = knot if (y - d) % 3 == 1 else cord
        px[d - 2, y] = glint if y == d else px[d - 2, y]
    # A bead below the chin on the left side, where the cord is tied.
    px[d - 1, d + h - 1] = (90, 90, 90, 255)
    px[d + w, d + h - 1] = (90, 90, 90, 255)
    return im


def main():
    save('item/bonding_charm.png', bonding_charm())
    save('item/camp_charter.png', camp_charter())
    for name, body in BODIES.items():
        save(f'entity/bonded_collar_{name}.png', collar(body))
    save('entity/bonded_collar.png', collar(BODIES['lantern_fox']))
    print('Painted familiars & camps textures into', TEX)


if __name__ == '__main__':
    main()
