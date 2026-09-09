"""Painted creature atlases for the Blender -> Minecraft bestiary pipeline (design 3.0 §8).

Every roster box (art/creatures/roster.json, art/creatures/roster_tribes.json) is given a deterministic
64x32 tile in a 256x256 atlas (tile index = box order, u=(tile%4)*64, v=(tile//4)*32) and unwrapped with
Minecraft's standard box UV:

        u      u+d      u+d+w    u+2d+w   u+2d+2w
    v   .      [ top  ] [bottom]
    v+d [west] [north ] [ east ] [south ]
    v+d+h

`north` is the creature's front (min z), `top` the visual top (min y).  Inside every side face the left
pixel column is the corner nearest the previous face in the strip, so the whole strip reads as the box
unwrapped from the outside.

The painter never uses noise: coats are built from ramped palettes derived from each roster entry's three
colours, deliberate stroke patterns (fur, scale, bark, stone, crystal, chitin, lacquer, cloth), a 1-2 px rim
light from the top-left, darker dorsal / lighter belly gradients, 2x2 eyes with a catchlight and species
markings.  Emissive pixels are mirrored into a `_glow` atlas (transparent elsewhere).

    import creatures
    boxes = creatures.layout(entry)                      # part, index, box, u, v, color
    atlas, glow = creatures.paint(entry, boxes)          # PIL images
    creatures.write_textures(entry, boxes, texdir)       # all PNGs for the entry (variants + overlays)
"""
import math
import sys
from pathlib import Path
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from glyphs import GLYPHS, GLYPH_NAMES, TRIBES  # noqa: E402

ATLAS = 256
TILE_W, TILE_H = 64, 32
INK = (16, 18, 24)
WHITE = (246, 244, 236)


# ----------------------------------------------------------------------------------------------- colour
def rgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def clamp(v):
    return max(0, min(255, int(round(v))))


def lighten(c, f):
    return tuple(clamp(v + (255 - v) * f) for v in c[:3])


def darken(c, f):
    return tuple(clamp(v * (1 - f)) for v in c[:3])


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def step(c, k):
    """Ramp a colour by k steps (negative = darker). Fractional steps allowed."""
    k = max(-3.0, min(3.0, k))
    if k < 0:
        return darken(c, -k * 0.21)
    if k > 0:
        return lighten(c, k * 0.22)
    return tuple(c[:3])


class Palette:
    """Three roster colours, each extended into a five step ramp (-2..+2)."""

    def __init__(self, colours):
        self.base = [rgb(c) if isinstance(c, str) else tuple(c) for c in colours]

    def c(self, i, k=0):
        return step(self.base[i], k)


# ----------------------------------------------------------------------------------------------- layout
def layout(entry):
    """Deterministic UV tiles for every box of a roster entry.

    Boxes are placed in roster order by first-fit over the 4x8 grid of 64x32 tiles; a box whose unwrap is
    wider than 64 px (or taller than 32 px) claims a 2x1 / 1x2 / 2x2 block of tiles, so u=(tile%4)*64 and
    v=(tile//4)*32 still hold for every box."""
    boxes = []
    used = set()
    for part in entry['parts']:
        for index, b in enumerate(part['boxes']):
            x, y, z, w, h, d = b['box']
            need_w, need_h = 2 * (w + d), h + d
            cw, ch = math.ceil(need_w / TILE_W), math.ceil(need_h / TILE_H)
            assert cw <= 2 and ch <= 2, (entry['id'], part['name'], index, 'box unwrap exceeds 128x64', b['box'])
            for tile in range(32):
                col, row = tile % 4, tile // 4
                cells = [(col + i, row + j) for i in range(cw) for j in range(ch)]
                if col + cw <= 4 and row + ch <= 8 and not any(c in used for c in cells):
                    used.update(cells)
                    boxes.append(dict(part=part['name'], index=index, box=list(b['box']), u=col * TILE_W, v=row * TILE_H, color=b.get('color', 0)))
                    break
            else:
                raise AssertionError((entry['id'], 'atlas is full', part['name'], index))
    return boxes


def faces(u, v, w, h, d):
    """Pixel rectangles (x0, y0, width, height) for the six faces of a box unwrapped at (u, v)."""
    fl, cl = math.floor, math.ceil

    def r(x0, y0, x1, y1):
        return (fl(x0), fl(y0), cl(x1) - fl(x0), cl(y1) - fl(y0))

    return {
        'top': r(u + d, v, u + d + w, v + d),
        'bottom': r(u + d + w, v, u + d + 2 * w, v + d),
        'west': r(u, v + d, u + d, v + d + h),
        'north': r(u + d, v + d, u + d + w, v + d + h),
        'east': r(u + d + w, v + d, u + 2 * d + w, v + d + h),
        'south': r(u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
    }


SIDES = ('north', 'south', 'west', 'east')


# ----------------------------------------------------------------------------------------------- canvas
class Canvas:
    def __init__(self, size=ATLAS):
        self.im = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        self.glow = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        self.px = self.im.load()
        self.gp = self.glow.load()
        self.size = size

    def put(self, x, y, c, glow=False):
        if 0 <= x < self.size and 0 <= y < self.size:
            self.px[x, y] = tuple(c[:3]) + (255,)
            self.gp[x, y] = (tuple(c[:3]) + (255,)) if glow else (0, 0, 0, 0)

    def get(self, x, y):
        return self.px[x, y][:3]

    def is_glow(self, x, y):
        return self.gp[x, y][3] > 0

    def adjust(self, x, y, k):
        if 0 <= x < self.size and 0 <= y < self.size and self.px[x, y][3] and not self.is_glow(x, y):
            self.px[x, y] = step(self.px[x, y][:3], k) + (255,)


class Face:
    """One unwrapped face: (i, j) run left->right, top->bottom inside the face rectangle."""

    def __init__(self, cv, name, rect, box):
        self.cv, self.name = cv, name
        self.x0, self.y0, self.w, self.h = rect
        self.box = box

    def put(self, i, j, c, glow=False):
        if 0 <= i < self.w and 0 <= j < self.h:
            self.cv.put(self.x0 + i, self.y0 + j, c, glow)

    def get(self, i, j):
        return self.cv.get(self.x0 + i, self.y0 + j)

    def adjust(self, i, j, k):
        if 0 <= i < self.w and 0 <= j < self.h:
            self.cv.adjust(self.x0 + i, self.y0 + j, k)

    def fill(self, c, glow=False):
        for j in range(self.h):
            for i in range(self.w):
                self.put(i, j, c, glow)

    def rect(self, i, j, w, h, c, glow=False):
        for jj in range(j, j + h):
            for ii in range(i, i + w):
                self.put(ii, jj, c, glow)

    def hline(self, j, c, i0=0, i1=None, glow=False):
        for i in range(i0, self.w if i1 is None else i1):
            self.put(i, j, c, glow)

    def vline(self, i, c, j0=0, j1=None, glow=False):
        for j in range(j0, self.h if j1 is None else j1):
            self.put(i, j, c, glow)

    def points(self, pts, c, glow=False):
        for i, j in pts:
            self.put(i, j, c, glow)

    def path(self, pts, c, glow=False, halo=None):
        """A 1 px polyline (4-connected) with an optional non-glowing halo colour beside it."""
        cells = []
        for (a, b), (c2, d2) in zip(pts, pts[1:]):
            x, y = a, b
            cells.append((x, y))
            while (x, y) != (c2, d2):
                if x != c2:
                    x += 1 if c2 > x else -1
                else:
                    y += 1 if d2 > y else -1
                cells.append((x, y))
        if halo is not None:
            for x, y in cells:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    if (x + dx, y + dy) not in cells:
                        self.put(x + dx, y + dy, halo)
        for x, y in cells:
            self.put(x, y, c, glow)

    def each(self, fn):
        for j in range(self.h):
            for i in range(self.w):
                fn(i, j)


# ----------------------------------------------------------------------------------------------- patterns
# Each pattern returns a ramp delta for pixel (i, j) of a face; None means "base colour".
def pat_fur(i, j, w, h):
    s = (i + 2 * j) % 5
    if s == 0:
        return -0.8
    if s == 3 and j % 2 == 0:
        return 0.6
    return 0


def pat_scale(i, j, w, h):
    row = j // 2
    off = (row % 2) * 2
    if j % 2 == 1:
        return -0.9 if (i + off) % 3 != 1 else 0
    return 0.7 if (i + off) % 3 == 1 else 0


def pat_bark(i, j, w, h):
    if i % 4 == 0:
        return -1.0
    if i % 4 == 2 and j % 3 == 0:
        return 0.7
    return 0


def pat_char(i, j, w, h):
    if i % 3 == 0:
        return -1.2 if j % 5 != 2 else -0.4
    if (i + j) % 7 == 4:
        return -0.6
    return 0


def pat_stone(i, j, w, h):
    if j % 4 == 0:
        return -1.0
    if i % 5 == ((j // 4) * 2) % 5:
        return -1.0
    if j % 4 == 1 and i % 5 == ((j // 4) * 2 + 1) % 5:
        return 0.7
    return 0


def pat_crystal(i, j, w, h):
    f = ((i + j) // 3 + (i - j + 96) // 4) % 3
    if (i + j) % 6 == 0:
        return 1.6
    return (f - 1) * 0.9


def pat_chitin(i, j, w, h):
    m = j % 4
    if m == 0:
        return 0.9
    if m == 3:
        return -1.1
    return 0


def pat_cloth(i, j, w, h):
    return -0.45 if (i + j) % 2 else 0


def pat_weave(i, j, w, h):
    if j % 3 == 2:
        return -0.7
    return 0.4 if (i + j // 3) % 2 == 0 else 0


def pat_metal(i, j, w, h):
    if i == 1 or (w >= 8 and i == 2):
        return 1.3
    if i >= w - 2:
        return -1.0
    if i == w - 3:
        return -0.4
    return 0


def pat_lacquer(i, j, w, h):
    m = j % 6
    if m == 1:
        return 1.2
    if m == 2:
        return 0.5
    if m == 5:
        return -0.9
    return 0


def pat_bone(i, j, w, h):
    return -0.9 if (j % 3 == 2 and i % 2 == 0) else 0


def pat_hide(i, j, w, h):
    return 0.5 if (i + j) % 6 == 0 else 0


def pat_flat(i, j, w, h):
    return 0


PATTERNS = dict(fur=pat_fur, scale=pat_scale, bark=pat_bark, char=pat_char, stone=pat_stone, crystal=pat_crystal,
                chitin=pat_chitin, cloth=pat_cloth, weave=pat_weave, metal=pat_metal, lacquer=pat_lacquer, bone=pat_bone,
                hide=pat_hide, flat=pat_flat, glow=pat_flat, skin=pat_flat)
COATS = {'fur', 'scale', 'chitin'}  # materials that get a darker dorsal / lighter belly


# ----------------------------------------------------------------------------------------------- creature
class Creature:
    """All faces of one painted creature; species painters read boxes and faces by (part, local index)."""

    def __init__(self, entry, boxes, canvas, palette):
        self.entry, self.boxes, self.cv, self.pal = entry, boxes, canvas, palette
        self.faces = {}
        for b in boxes:
            x, y, z, w, h, d = b['box']
            fs = faces(b['u'], b['v'], w, h, d)
            self.faces[(b['part'], b['index'])] = {name: Face(canvas, name, rect, b) for name, rect in fs.items()}

    def face(self, part, index, name):
        return self.faces[(part, index)][name]

    def box(self, part, index):
        for b in self.boxes:
            if b['part'] == part and b['index'] == index:
                return b
        raise KeyError((part, index))

    def part_boxes(self, part):
        return [b for b in self.boxes if b['part'] == part]

    def parts(self, prefix):
        return sorted({b['part'] for b in self.boxes if b['part'].startswith(prefix)})

    # -- painting primitives --------------------------------------------------------------------
    def coat(self, part, index, material, ramp, gradient=None, glow=False, only=None):
        """Fill every face of a box with a material in a palette ramp."""
        pattern = PATTERNS[material]
        base = self.pal.base[ramp] if isinstance(ramp, int) else tuple(ramp)
        gradient = (material in COATS) if gradient is None else gradient
        for name, face in self.faces[(part, index)].items():
            if only and name not in only:
                continue
            for j in range(face.h):
                for i in range(face.w):
                    k = pattern(i, j, face.w, face.h) or 0
                    if gradient:
                        if name == 'top':
                            k -= 0.8
                        elif name == 'bottom':
                            k += 1.2
                        elif face.h >= 4:
                            k += (j / max(1, face.h - 1) - 0.5) * 1.6
                    face.put(i, j, step(base, k), glow)

    def rim(self, part, index):
        """1-2 px rim light from the top-left and a darker bottom/right edge on every face."""
        for name, face in self.faces[(part, index)].items():
            if face.w < 2 or face.h < 2:
                continue
            strong = face.w >= 6 and face.h >= 6
            for i in range(face.w):
                face.adjust(i, 0, 0.9)
                face.adjust(i, face.h - 1, -0.9)
                if strong:
                    face.adjust(i, 1, 0.35)
            for j in range(face.h):
                face.adjust(0, j, 0.6)
                face.adjust(face.w - 1, j, -0.6)
                if strong:
                    face.adjust(1, j, 0.25)

    def eyes(self, part, index, style, colour=None, positions=None):
        """2x2 eyes with a bright catchlight on the front face of a head box.

        Positions default to the small colour-2 cubes of the same part that sit in front of the box."""
        head = self.box(part, index)
        hx, hy, hz, hw, hh, hd = head['box']
        front = self.face(part, index, 'north')
        glow_c = colour or self.pal.c(2, 0)
        if positions is None:
            positions = []
            for b in self.part_boxes(part):
                x, y, z, w, h, d = b['box']
                if b['color'] == 2 and w <= 2 and h <= 1.5 and z < hz and b['index'] != index:
                    i = int(round(x - hx))
                    j = int(round(y - hy))
                    positions.append((i, j, int(round(w))))
                    for face in self.faces[(part, b['index'])].values():
                        face.fill(step(glow_c, 0.6), glow=True)
        for i, j, w in positions:
            inward = 1 if i < hw / 2 else -1
            cols = [i, i + inward] if w <= 1 else [i, i + 1]
            left = min(cols)
            if style == 'animal':
                front.rect(left, j, 2, 2, INK)
                front.put(i, j, step(glow_c, 1.2), glow=True)
                front.put(left + 1, j + 1, step(glow_c, -1.0))
            else:
                front.rect(left, j, 2, 2, step(glow_c, 0), glow=True)
                front.put(i, j, step(glow_c, 2.0), glow=True)
                front.put(left + (1 if i == left else 0), j + 1, step(glow_c, -1.2), glow=True)
            front.hline(j - 1, step(self.pal.c(0), -1.5), left, left + 2)  # brow


# ----------------------------------------------------------------------------------------------- species
def hooves(cr, prefix='leg', rows=2, ramp=0, k=-1.8):
    for part in cr.parts(prefix):
        for b in cr.part_boxes(part):
            for name in SIDES:
                f = cr.face(part, b['index'], name)
                for j in range(max(0, f.h - rows), f.h):
                    f.hline(j, cr.pal.c(ramp, k))
            cr.face(part, b['index'], 'bottom').fill(cr.pal.c(ramp, k))


def socks(cr, colour, rows=3, prefix='leg'):
    for part in cr.parts(prefix):
        for b in cr.part_boxes(part):
            for name in SIDES:
                f = cr.face(part, b['index'], name)
                for j in range(max(0, f.h - rows), f.h):
                    f.hline(j, colour)
            cr.face(part, b['index'], 'bottom').fill(step(colour, -0.6))


def snout(cr, part, index, ramp=1, nose=INK):
    f = cr.face(part, index, 'north')
    f.rect(1, 0, f.w - 2, 1, cr.pal.c(ramp, 1.0))
    f.rect((f.w - 2) // 2, f.h - 1, 2, 1, nose)
    for name in ('west', 'east'):
        cr.face(part, index, name).put(0 if name == 'east' else cr.face(part, index, name).w - 1, 0, cr.pal.c(ramp, 1.0))


def ears(cr, part, indices, inner):
    for idx in indices:
        f = cr.face(part, idx, 'north')
        f.rect(0, 1, f.w, max(1, f.h - 1), inner)
        cr.face(part, idx, 'top').fill(step(cr.pal.c(0), -1.5))


def antlers(cr, part, indices, tips):
    for idx in indices:
        cr.coat(part, idx, 'bone', cr.pal.c(1, 0.8), gradient=False)
    for idx in tips:
        cr.coat(part, idx, 'glow', 2, glow=True)
        for f in cr.faces[(part, idx)].values():
            f.put(0, 0, cr.pal.c(2, 2), glow=True)


def default_coat(cr, spec):
    for b in cr.boxes:
        key = (b['part'], b['index'])
        mat, ramp = spec['over'].get(key, spec['mat'][b['color']])
        cr.coat(b['part'], b['index'], mat, ramp, glow=(mat == 'glow'))


def sp_dawn_stag(cr):
    p = cr.pal
    body = cr.faces[('body', 0)]
    for name in ('west', 'east'):
        body[name].points([(2, 1), (3, 1), (6, 2), (10, 1), (11, 1), (4, 3), (8, 3), (12, 2)], p.c(1, 0.4))
    body['top'].points([(1, 2), (5, 3), (6, 3), (2, 6), (6, 8), (1, 10), (2, 10), (5, 12)], p.c(1, 0.2))
    body['top'].vline(3, p.c(0, -1.4), 1, 13)
    body['top'].vline(4, p.c(0, -1.2), 1, 13)
    body['bottom'].fill(p.c(1, -0.4))
    body['bottom'].rect(2, 4, 4, 8, p.c(1, 0.3))
    body['south'].rect(3, 2, 2, 3, p.c(1, 0.9))  # pale rump patch
    head = cr.faces[('head', 0)]
    head['north'].rect(1, 4, 4, 2, p.c(1, 0.3))
    head['top'].vline(2, p.c(0, -1.2))
    head['top'].vline(3, p.c(0, -1.2))
    snout(cr, 'head', 1)
    ears(cr, 'head', (2, 3), p.c(1, 0.6))
    antlers(cr, 'head', (6, 7, 9, 10), (8, 11))
    cr.eyes('head', 0, 'animal')
    hooves(cr, ramp=0, k=-2.0)
    cr.coat('tail', 1, 'glow', 2, glow=True)
    cr.face('tail', 0, 'bottom').fill(p.c(1, 0.9))


def sp_lantern_fox(cr):
    p = cr.pal
    cream = p.c(1, 0.9)
    body = cr.faces[('body', 0)]
    body['north'].rect(2, 1, 4, 5, cream)
    body['north'].rect(3, 0, 2, 1, cream)
    for name in ('west', 'east'):
        f = body[name]
        f.rect(0, 4, f.w, 2, p.c(1, 0.2))
        f.hline(5, p.c(2, 0.4), glow=True)
        f.points([(2, 5), (5, 5), (8, 5), (11, 5)], p.c(2, 1.4), glow=True)
    bot = body['bottom']
    bot.fill(p.c(1, -0.2))
    for j in range(bot.h):
        for i in range(bot.w):
            dist = max(abs(i - 3.5) / 2.2, abs(j - 6) / 3.6)
            if dist < 1.0:
                bot.put(i, j, step(p.c(2), 1.6 - dist * 2.4), glow=True)
    body['top'].points([(0, 1), (7, 1), (0, 5), (7, 5), (0, 9), (7, 9)], p.c(0, -1.2))  # dark flank streaks
    head = cr.faces[('head', 0)]
    head['north'].rect(1, 3, 4, 3, cream)
    head['north'].rect(0, 5, 6, 1, cream)
    head['top'].vline(2, p.c(0, -1.0), 2, 6)
    head['top'].vline(3, p.c(0, -1.0), 2, 6)
    snout(cr, 'head', 1)
    ears(cr, 'head', (2, 3), p.c(1, 0.4))
    for idx in (2, 3):
        cr.face('head', idx, 'north').hline(0, INK)
    cr.eyes('head', 0, 'animal')
    socks(cr, p.c(0, -1.6), rows=3)
    cr.coat('tail', 1, 'glow', 2, glow=True)
    tail = cr.faces[('tail', 0)]
    for name in SIDES + ('top', 'bottom'):
        tail[name].rect(0, 0, 2, tail[name].h, p.c(0, 0.2))


def sp_mossback(cr):
    p = cr.pal
    line = p.c(1, -2.0)
    for idx, cols, rows in ((1, (3, 6), (4, 8)), (2, (2, 4), (3, 6))):
        top = cr.face('body', idx, 'top')
        for c in cols:
            top.vline(c, line)
        for r in rows:
            top.hline(r, line)
        for j in range(top.h):
            for i in range(top.w):
                if i not in cols and j not in rows and (i % 3 == 1 and j % 4 == 1):
                    top.put(i, j, p.c(1, 0.9))
        for name in SIDES:
            f = cr.face('body', idx, name)
            f.hline(f.h - 1, line)
            for i in range(0, f.w, 3):
                f.put(i, 0, line)
    moss = cr.face('body', 1, 'top')
    for (i, j) in ((1, 1), (2, 1), (1, 2), (7, 2), (8, 2), (4, 9), (5, 9), (4, 10), (8, 10), (0, 6)):
        moss.put(i, j, p.c(2, -0.9))
    moss.points([(1, 1), (7, 2), (4, 9), (8, 10)], p.c(2, 0.4))
    for name in ('west', 'east', 'north'):
        f = cr.face('body', 1, name)
        f.points([(1, 0), (2, 0), (f.w - 3, 0), (f.w - 2, 0)], p.c(2, -0.9))
    bot = cr.face('body', 0, 'bottom')
    bot.fill(p.c(1, -0.3))
    for c in (4, 9):
        bot.vline(c, p.c(1, -1.6))
    for r in (5, 10):
        bot.hline(r, p.c(1, -1.6))
    cr.coat('body', 3, 'bark', p.c(1, -1.2), gradient=False)
    cr.coat('body', 4, 'glow', 2, glow=True)
    leaf = cr.face('body', 4, 'top')
    leaf.vline(2, p.c(2, -1.6), glow=True)
    leaf.hline(2, p.c(2, -1.6), glow=True)
    head = cr.face('head', 0, 'north')
    head.hline(3, p.c(1, -1.8), 1, 3)
    cr.face('head', 0, 'top').rect(1, 1, 2, 3, p.c(1, -0.7))
    cr.eyes('head', 0, 'animal')
    for part in cr.parts('leg'):
        f = cr.face(part, 0, 'north')
        f.points([(0, f.h - 1), (2, f.h - 1)], p.c(1, -1.8))
        cr.face(part, 0, 'bottom').fill(p.c(1, -1.2))


def ember_cracks(cr, part, index, lines, halo_k=0.2):
    p = cr.pal
    halo = mix(p.c(0, 0.4), p.c(1, halo_k), 0.45)
    for name, pts in lines:
        cr.face(part, index, name).path(pts, p.c(2, 0.6), glow=True, halo=halo)


def sp_ashbound(cr):
    p = cr.pal
    ember_cracks(cr, 'body', 0, [
        ('north', [(1, 2), (1, 4), (2, 5), (2, 8)]),
        ('north', [(6, 1), (6, 3)]),
        ('south', [(3, 1), (3, 4), (4, 5), (4, 8)]),
        ('west', [(1, 2), (2, 3), (2, 6)]),
        ('east', [(4, 1), (3, 3), (3, 6), (4, 8)]),
    ])
    heart = cr.faces[('body', 1)]
    heart['north'].fill(p.c(2, -0.6), glow=True)
    heart['north'].rect(1, 1, 2, 2, p.c(2, 1.6), glow=True)
    heart['north'].points([(0, 0), (3, 3)], p.c(1, -0.8))
    ember_cracks(cr, 'head', 0, [
        ('north', [(1, 0), (1, 1)]),
        ('north', [(2, 5), (3, 4), (4, 5)]),
        ('west', [(2, 0), (2, 2), (3, 3)]),
        ('east', [(3, 1), (2, 3), (2, 5)]),
        ('top', [(1, 1), (2, 2), (2, 4), (4, 5)]),
    ])
    for name in SIDES:
        cr.face('head', 0, name).hline(0, p.c(0, -1.6))
    cr.faces[('head', 1)]['north'].fill(p.c(2, 1.2), glow=True)
    for part in cr.parts('arm'):
        ember_cracks(cr, part, 0, [('north', [(1, 3), (1, 6)]), ('west', [(1, 2), (1, 5)]), ('east', [(1, 1), (2, 4)])])
        for name in SIDES:
            cr.face(part, 0, name).hline(8, p.c(2, -0.3), glow=True)
        cr.face(part, 0, 'bottom').fill(p.c(2, 0.4), glow=True)
    for part in cr.parts('leg'):
        ember_cracks(cr, part, 0, [('north', [(1, 2), (1, 4)]), ('south', [(1, 2), (2, 5)])])
        cr.face(part, 0, 'bottom').fill(p.c(2, -0.8), glow=True)


def rings(face, ci, cj, colour_a, colour_b, spread=1.0):
    for j in range(face.h):
        for i in range(face.w):
            dist = int(max(abs(i - ci), abs(j - cj) * spread))
            if dist % 3 == 0:
                face.put(i, j, colour_a)
            elif dist % 3 == 1:
                face.put(i, j, colour_b)


def sp_rootbound(cr):
    p = cr.pal
    rings(cr.face('body', 0, 'north'), 6, 6, p.c(0, -1.2), p.c(0, 0.6), 1.3)
    rings(cr.face('body', 0, 'south'), 5, 3, p.c(0, -1.2), p.c(0, 0.6), 1.3)
    heart = cr.face('body', 1, 'north')
    heart.fill(p.c(2, -0.3), glow=True)
    heart.rect(1, 1, 2, 2, p.c(2, 1.5), glow=True)
    heart.points([(0, 3), (3, 0)], p.c(0, -1.5))
    for idx in (2, 3, 4):
        cr.coat('body', idx, 'bark', 0)
        for name in SIDES:
            f = cr.face('body', idx, name)
            f.rect(0, 0, f.w, min(2, f.h), p.c(1, 0.3))
            f.put(0, 0, p.c(2, 0.5), glow=True)
        cr.face('body', idx, 'top').fill(p.c(2, -0.2), glow=True)
    for name in ('west', 'east'):
        cr.face('body', 0, name).points([(1, 2), (1, 3), (4, 6), (4, 7), (2, 8)], p.c(1, -0.4))  # moss patches
    cr.coat('head', 0, 'bark', 0)
    rings(cr.face('head', 0, 'north'), 3, 3, p.c(0, -1.2), p.c(0, 0.5), 1.0)
    cr.face('head', 0, 'top').fill(p.c(1, 0.1))
    cr.face('head', 0, 'top').points([(1, 1), (4, 2), (2, 4), (5, 5)], p.c(1, 1.0))
    for name in SIDES:
        cr.face('head', 0, name).hline(0, p.c(1, -0.2))
    cr.face('head', 1, 'north').fill(p.c(2, 1.3), glow=True)
    for part in cr.parts('leg') + cr.parts('arm'):
        cr.coat(part, 0, 'bark', 0)
        for name in SIDES:
            f = cr.face(part, 0, name)
            f.vline(1, p.c(0, -1.8), f.h // 2, f.h)
            f.points([(0, f.h - 1), (2, f.h - 2)], p.c(0, -1.8))
        if part.startswith('arm'):
            cr.face(part, 0, 'top').fill(p.c(1, 0.2))
            cr.face(part, 0, 'north').points([(0, 0), (1, 1), (2, 0)], p.c(1, 0.4))


def stripes(face, colour, axis='v', every=3, offset=0):
    if axis == 'v':
        for i in range(offset, face.w, every):
            face.vline(i, colour)
    else:
        for j in range(offset, face.h, every):
            face.hline(j, colour)


def sp_reed_stalker(cr):
    p = cr.pal
    dark = p.c(0, -1.5)
    body = cr.faces[('body', 0)]
    for name in ('west', 'east'):
        stripes(body[name], dark, 'v', 3, 1)
        body[name].rect(0, 4, body[name].w, 2, p.c(1, -0.6))
    stripes(body['top'], dark, 'h', 3, 1)
    stripes(body['north'], dark, 'v', 3, 1)
    stripes(body['south'], dark, 'v', 3, 1)
    body['bottom'].fill(p.c(1, -0.4))
    for idx in (1, 2):
        cr.coat('body', idx, 'bark', 1, gradient=False)
        for name in SIDES:
            f = cr.face('body', idx, name)
            for j in range(2, f.h, 3):
                f.hline(j, p.c(1, -1.8))
    cr.coat('body', 3, 'glow', 2, glow=True)
    for name in SIDES:
        f = cr.face('body', 3, name)
        f.rect(0, 3, 1, 7, p.c(1, -0.5))  # stalk below the glowing seed head
    head = cr.faces[('head', 0)]
    stripes(head['top'], dark, 'h', 3, 1)
    for name in ('west', 'east', 'north'):
        stripes(head[name], dark, 'v', 3, 1)
    snout(cr, 'head', 1)
    cr.face('head', 1, 'north').points([(0, 2), (3, 2)], p.c(1, 1.8))  # fangs
    ears(cr, 'head', (2, 3), p.c(1, 0.3))
    cr.eyes('head', 0, 'monster')
    for part in cr.parts('leg'):
        for name in SIDES:
            stripes(cr.face(part, 0, name), p.c(1, -1.4), 'h', 3, 1)
    cr.coat('tail', 1, 'glow', 2, glow=True)
    for name in SIDES:
        stripes(cr.face('tail', 0, name), dark, 'v', 3, 1)


def sp_shardback(cr):
    p = cr.pal
    body = cr.faces[('body', 0)]
    body['bottom'].fill(p.c(0, -1.6))
    for name in SIDES:
        body[name].rect(0, body[name].h - 2, body[name].w, 2, p.c(0, -1.2))
    for idx in (1, 2):
        cr.coat('body', idx, 'crystal', 2, glow=True)
        cr.face('body', idx, 'top').fill(p.c(2, 2.0), glow=True)
    cr.coat('body', 3, 'crystal', 1, gradient=False)
    head = cr.faces[('head', 0)]
    stripes(head['top'], p.c(1, -1.4), 'h', 2, 1)
    head['north'].hline(3, p.c(1, -1.8))
    head['north'].points([(1, 3), (3, 3), (5, 3)], p.c(1, 1.5))  # mandible teeth
    cr.eyes('head', 0, 'monster')
    for part in cr.parts('leg'):
        upper, lower = cr.part_boxes(part)
        cr.coat(part, lower['index'], 'crystal', 0, gradient=False)
        for name in SIDES:
            cr.face(part, lower['index'], name).hline(cr.face(part, lower['index'], name).h - 1, p.c(2, 0.5), glow=True)
        cr.face(part, lower['index'], 'bottom').fill(p.c(2, 1.0), glow=True)
        for name in ('north', 'south'):
            f = cr.face(part, upper['index'], name)
            f.hline(0, p.c(1, 1.0))


def rune_line(cr, part, index, name, pts, ramp=2):
    cr.face(part, index, name).path(pts, cr.pal.c(ramp, 0.4), glow=True)


def sp_hollow_sentinel(cr):
    p = cr.pal
    rune_line(cr, 'body', 0, 'north', [(1, 1), (1, 8)])
    rune_line(cr, 'body', 0, 'north', [(1, 3), (2, 3)])
    rune_line(cr, 'body', 0, 'north', [(1, 6), (2, 6)])
    rune_line(cr, 'body', 0, 'north', [(6, 1), (6, 8)])
    rune_line(cr, 'body', 0, 'south', [(1, 2), (6, 2)])
    rune_line(cr, 'body', 0, 'south', [(2, 5), (5, 5)])
    rune_line(cr, 'body', 0, 'south', [(3, 8), (4, 8)])
    rune_line(cr, 'body', 0, 'west', [(2, 1), (2, 8)])
    rune_line(cr, 'body', 0, 'east', [(3, 1), (3, 8)])
    slot = cr.face('body', 1, 'north')
    slot.fill(p.c(2, -0.4), glow=True)
    slot.rect(1, 1, 2, 2, p.c(2, 1.6), glow=True)
    slot.points([(0, 0), (3, 0), (0, 3), (3, 3)], p.c(0, -1.8))
    for idx in (0, 2, 3):
        cr.coat('head', idx, 'metal', 1, gradient=False)
    mask = cr.face('head', 0, 'north')
    mask.hline(4, p.c(1, -1.8), 1, 5)
    mask.vline(0, p.c(1, -1.5))
    mask.vline(5, p.c(1, -1.5))
    mask.points([(2, 1), (3, 1)], p.c(1, -1.2))
    cr.face('head', 1, 'north').fill(p.c(2, 1.4), glow=True)
    cr.face('head', 4, 'north').fill(p.c(2, 1.8), glow=True)
    for idx in (2, 3):
        f = cr.face('head', idx, 'north')
        for j in range(1, f.h, 3):
            f.hline(j, p.c(1, -1.4))
        cr.face('head', idx, 'top').fill(p.c(1, -1.2))
    for part in cr.parts('arm'):
        cr.coat(part, 0, 'stone', 0, gradient=False)
        outer = 'west' if part.endswith('0') else 'east'
        rune_line(cr, part, 0, outer, [(1, 1), (1, 8)])
        rune_line(cr, part, 0, 'north', [(1, 2), (1, 3)])
        rune_line(cr, part, 0, 'north', [(1, 6), (1, 7)])
        cr.face(part, 0, 'bottom').fill(p.c(1, -0.4))
    for part in cr.parts('leg'):
        rune_line(cr, part, 0, 'north', [(1, 2), (1, 4)])


def eyespot(face, ci, cj, cr):
    p = cr.pal
    for j in range(face.h):
        for i in range(face.w):
            d = max(abs(i - ci), abs(j - cj))
            if d == 2:
                face.put(i, j, p.c(0, -0.5))
            elif d == 1:
                face.put(i, j, p.c(2, 0.2), glow=True)
            elif d == 0:
                face.put(i, j, p.c(0, -1.5))


def sp_storm_moth(cr):
    p = cr.pal
    for name in SIDES:
        f = cr.face('body', 0, name)
        for j in range(0, f.h, 3):
            f.hline(j, p.c(1, -0.5))
    head = cr.face('head', 0, 'north')
    cr.eyes('head', 0, 'monster', positions=[(0, 1, 1), (5, 1, 1)])
    head.hline(3, p.c(1, -1.6), 2, 4)
    for wing in ('wing0', 'wing1'):
        upper, lower, spot = cr.part_boxes(wing)
        body_side_right = wing == 'wing0'
        for name in ('north', 'south'):
            f = cr.face(wing, upper['index'], name)
            far = body_side_right if name == 'north' else not body_side_right
            for j in range(f.h):
                for i in range(f.w):
                    t = (f.w - 1 - i) / (f.w - 1) if far else i / (f.w - 1)
                    k = -1.0 + t * 2.4
                    if j in (2, 5, 8) and i % 2 == 0:
                        k -= 1.2
                    f.put(i, j, step(p.c(1), k))
            f.hline(0, p.c(0, -0.6))
            f.hline(f.h - 1, p.c(0, -0.6))
            f = cr.face(wing, lower['index'], name)
            for j in range(f.h):
                for i in range(f.w):
                    k = 0.3 if (i + j) % 4 == 0 and j > 1 else -0.2
                    f.put(i, j, step(p.c(0), k + j * 0.15))
            f.hline(f.h - 1, p.c(1, -0.3))
        panel = cr.face(wing, spot['index'], 'north')
        panel.fill(p.c(1, 0.2))
        eyespot(panel, 2 if body_side_right else 3, 1, cr)
        panel = cr.face(wing, spot['index'], 'south')
        panel.fill(p.c(1, 0.2))
        eyespot(panel, 3 if body_side_right else 2, 1, cr)
    for idx in (1, 2):
        for f in cr.faces[('head', idx)].values():
            f.put(0, 0, p.c(2, 2.0), glow=True)


def sp_cinder_imp(cr):
    p = cr.pal
    ember_cracks(cr, 'body', 0, [
        ('north', [(1, 2), (2, 4), (1, 6), (2, 8)]),
        ('south', [(3, 0), (4, 3), (3, 6), (4, 9)]),
        ('west', [(1, 1), (2, 3), (1, 6), (2, 8)]),
        ('east', [(4, 0), (3, 3), (4, 5), (3, 8)]),
    ], halo_k=-0.4)
    heart = cr.face('body', 1, 'north')
    heart.fill(p.c(2, -0.3), glow=True)
    heart.rect(1, 1, 2, 2, p.c(2, 1.6), glow=True)
    cr.coat('head', 0, 'char', 0)
    grin = cr.face('head', 0, 'north')
    for i in range(1, 5):
        grin.put(i, 4, p.c(2, 1.2) if i % 2 else p.c(0, -2.0), glow=bool(i % 2))
    ember_cracks(cr, 'head', 0, [('west', [(1, 1), (2, 4)]), ('east', [(3, 0), (2, 3)]), ('top', [(1, 1), (3, 3), (4, 4)])], halo_k=-0.4)
    cr.face('head', 1, 'north').fill(p.c(2, 1.6), glow=True)
    for idx in (2, 3):
        cr.coat('head', idx, 'bone', 1, gradient=False)
        cr.face('head', idx, 'top').fill(p.c(1, -1.4))
        for name in SIDES:
            cr.face('head', idx, name).hline(0, p.c(1, -1.2))
    for part in cr.parts('arm'):
        cr.coat(part, 0, 'char', 0)
        ember_cracks(cr, part, 0, [('north', [(1, 0), (1, 5)]), ('west', [(1, 1), (1, 4)])], halo_k=-0.4)
        cr.face(part, 0, 'bottom').fill(p.c(2, 0.6), glow=True)
    for part in cr.parts('leg'):
        ember_cracks(cr, part, 0, [('north', [(1, 0), (1, 3)])], halo_k=-0.4)
        cr.face(part, 0, 'bottom').fill(p.c(2, -0.5), glow=True)


def sp_mourning_bell(cr):
    p = cr.pal
    verdigris = mix(p.c(2, -0.5), (70, 150, 120), 0.45)
    body = cr.faces[('body', 0)]
    for name in SIDES:
        f = body[name]
        f.hline(2, p.c(0, -1.8))
        f.hline(3, p.c(0, 0.9))
        f.hline(6, p.c(0, -1.8))
        f.hline(7, p.c(0, 0.6))
        f.points([(0, 7), (1, 8), (0, 8), (f.w - 2, 5), (f.w - 1, 6), (f.w - 1, 7), (4, 8), (5, 8)], verdigris)
        f.points([(1, 8), (f.w - 1, 6)], step(verdigris, 0.8))
    body['top'].fill(p.c(0, 0.4))
    body['bottom'].fill(p.c(0, -2.2))
    body['bottom'].rect(2, 2, 6, 6, INK)
    for idx in (1, 2):
        cr.coat('body', idx, 'metal', 1, gradient=False)
    rim = cr.faces[('body', 1)]
    for name in SIDES:
        rim[name].hline(rim[name].h - 1, p.c(1, -1.8))
        for i in range(1, rim[name].w, 3):
            rim[name].put(i, 0, verdigris)
    rim['bottom'].fill(p.c(0, -1.5))
    crown = cr.faces[('body', 2)]
    crown['top'].rect(1, 1, 2, 2, p.c(1, -1.6))
    for name in SIDES:
        crown[name].vline(1, p.c(1, -1.2))
    cr.face('body', 3, 'north').fill(p.c(2, 1.0), glow=True)
    cr.face('body', 3, 'north').points([(1, 0), (4, 0), (6, 0)], p.c(2, 2.0), glow=True)
    cr.coat('tail', 0, 'metal', 1, gradient=False)
    cr.coat('tail', 1, 'glow', 2, glow=True)
    for f in cr.faces[('tail', 1)].values():
        f.put(1, 0, p.c(2, 2.0), glow=True)


def sp_rift_hound(cr):
    p = cr.pal
    body = cr.faces[('body', 0)]
    body['west'].path([(2, 1), (3, 2), (3, 3), (5, 4), (6, 4)], p.c(2, 0.5), glow=True, halo=p.c(1, -0.6))
    body['west'].path([(9, 2), (9, 4)], p.c(2, 0.2), glow=True, halo=p.c(1, -0.6))
    body['east'].path([(3, 3), (4, 2), (6, 2), (7, 1)], p.c(2, 0.5), glow=True, halo=p.c(1, -0.6))
    body['east'].path([(9, 3), (10, 4)], p.c(2, 0.2), glow=True, halo=p.c(1, -0.6))
    body['bottom'].fill(p.c(0, 1.0))
    body['north'].rect(2, 1, 4, 3, p.c(0, 0.6))
    spine = cr.faces[('body', 1)]
    for name in ('west', 'east'):
        for i in range(0, spine[name].w, 2):
            spine[name].put(i, 0, p.c(1, 1.2))
            spine[name].put(i + 1, 0, p.c(1, -1.0))
    spine['top'].fill(p.c(1, 0.6))
    for j in range(0, spine['top'].h, 2):
        spine['top'].hline(j, p.c(1, -1.2))
    cr.coat('body', 2, 'glow', 2, glow=True)
    cr.face('body', 2, 'top').rect(1, 1, 2, 2, p.c(2, 2.0), glow=True)
    head = cr.faces[('head', 0)]
    head['north'].path([(0, 0), (1, 1)], p.c(2, 0.4), glow=True, halo=p.c(1, -0.6))
    head['top'].path([(1, 1), (2, 2), (2, 4)], p.c(2, 0.4), glow=True, halo=p.c(1, -0.6))
    cr.coat('head', 1, 'fur', 1)
    snout(cr, 'head', 1)
    cr.face('head', 1, 'north').points([(0, 2), (3, 2)], p.c(1, 1.8))
    ears(cr, 'head', (2, 3), p.c(2, -0.5))
    cr.eyes('head', 0, 'monster')
    for part in cr.parts('leg'):
        cr.coat(part, 0, 'fur', 0)
        socks_face = cr.face(part, 0, 'north')
        socks_face.hline(socks_face.h - 1, p.c(1, -1.6))
        cr.face(part, 0, 'bottom').fill(p.c(1, -1.6))
    cr.coat('tail', 1, 'glow', 2, glow=True)
    cr.coat('tail', 0, 'fur', 0)


def sp_echo_weaver(cr):
    p = cr.pal
    body = cr.faces[('body', 0)]
    top = body['top']
    top.points([(5, 3), (6, 3), (5, 4), (6, 4), (4, 5), (7, 5), (4, 6), (7, 6), (5, 7), (6, 7), (5, 8), (6, 8)], p.c(1, 0.4))
    top.points([(5, 3), (7, 5), (5, 8)], p.c(1, 1.3))
    body['south'].points([(3, 2), (5, 2), (7, 2), (4, 4), (6, 4), (8, 4)], p.c(2, 0.6), glow=True)
    body['bottom'].fill(p.c(0, 0.8))
    head = cr.faces[('head', 0)]
    cr.eyes('head', 0, 'monster')
    head['north'].points([(0, 0), (7, 0), (2, 0), (5, 0)], p.c(2, 0.8), glow=True)
    head['north'].points([(3, 3), (4, 3)], p.c(1, -1.8))  # fangs
    head['north'].points([(3, 2), (4, 2)], p.c(1, 1.6))
    for part in cr.parts('leg'):
        upper, lower = cr.part_boxes(part)
        for name in ('north', 'south'):
            f = cr.face(part, upper['index'], name)
            for i in range(0, f.w, 3):
                f.vline(i, p.c(1, -1.2))
            f.hline(0, p.c(1, 0.9))
        for name in SIDES:
            f = cr.face(part, lower['index'], name)
            f.hline(f.h - 1, p.c(2, 0.2), glow=True)


SPECIES = {
    'dawn_stag': dict(mat={0: ('fur', 0), 1: ('fur', 1), 2: ('glow', 2)}, over={}, paint=sp_dawn_stag),
    'lantern_fox': dict(mat={0: ('fur', 0), 1: ('fur', 1), 2: ('glow', 2)}, over={}, paint=sp_lantern_fox),
    'mossback': dict(mat={0: ('scale', 0), 1: ('scale', 1), 2: ('glow', 2)}, over={('body', 1): ('flat', 1), ('body', 2): ('flat', 1)}, paint=sp_mossback),
    'ashbound': dict(mat={0: ('char', 0), 1: ('char', 0), 2: ('glow', 2)}, over={}, paint=sp_ashbound),
    'rootbound': dict(mat={0: ('bark', 0), 1: ('bark', 0), 2: ('glow', 2)}, over={}, paint=sp_rootbound),
    'reed_stalker': dict(mat={0: ('fur', 0), 1: ('fur', 1), 2: ('glow', 2)}, over={}, paint=sp_reed_stalker),
    'shardback': dict(mat={0: ('crystal', 0), 1: ('chitin', 1), 2: ('glow', 2)}, over={}, paint=sp_shardback),
    'hollow_sentinel': dict(mat={0: ('stone', 0), 1: ('metal', 1), 2: ('glow', 2)}, over={}, paint=sp_hollow_sentinel),
    'storm_moth': dict(mat={0: ('fur', 0), 1: ('fur', 1), 2: ('glow', 2)}, over={}, paint=sp_storm_moth),
    'cinder_imp': dict(mat={0: ('char', 0), 1: ('bone', 1), 2: ('glow', 2)}, over={}, paint=sp_cinder_imp),
    'mourning_bell': dict(mat={0: ('metal', 0), 1: ('metal', 1), 2: ('glow', 2)}, over={}, paint=sp_mourning_bell),
    'rift_hound': dict(mat={0: ('fur', 0), 1: ('fur', 1), 2: ('glow', 2)}, over={}, paint=sp_rift_hound),
    'echo_weaver': dict(mat={0: ('chitin', 0), 1: ('chitin', 1), 2: ('glow', 2)}, over={}, paint=sp_echo_weaver),
}


# ----------------------------------------------------------------------------------------------- tribal kin
SKIN = {'elder': (0x9c, 0x74, 0x55), 'drummer': (0xa8, 0x7a, 0x58), 'hunter': (0x8a, 0x64, 0x48), 'weaver': (0xb0, 0x86, 0x62)}
CLOTH = {'elder': (0x4a, 0x3b, 0x33), 'drummer': (0x6b, 0x3a, 0x30), 'hunter': (0x5e, 0x42, 0x2c), 'weaver': (0x3b, 0x4a, 0x50)}
TRIM = {'elder': (0xe8, 0xd9, 0xa8), 'drummer': (0xd7, 0x7a, 0x3a), 'hunter': (0xc8, 0x9a, 0x5e), 'weaver': (0x62, 0xd1, 0xc9)}
BONE = (0xe1, 0xd9, 0xbd)
LEATHER = (0x6b, 0x44, 0x2c)
GREY = (0xb9, 0xb4, 0xa9)
KIN_ROLES = ('elder', 'drummer', 'hunter', 'weaver')


def paint_kin(cr, role):
    skin, cloth, trim = SKIN[role], CLOTH[role], TRIM[role]
    # head: skin with a hair cap, mask plate in front, hood over the crown, two hanging braids
    cr.coat('head', 0, 'skin', skin, gradient=False)
    hair = GREY if role == 'elder' else darken(cloth, 0.35)
    for name in SIDES:
        f = cr.face('head', 0, name)
        f.rect(0, 0, f.w, 2, hair)
        for i in range(0, f.w, 2):
            f.put(i, 2, hair)
    cr.face('head', 0, 'top').fill(hair)
    front = cr.face('head', 0, 'north')
    front.rect(2, 3, 1, 1, INK)
    front.rect(5, 3, 1, 1, INK)
    front.hline(6, darken(skin, 0.3), 3, 5)
    # mask: bone plate with role marks and dark eye slits
    cr.coat('head', 1, 'bone', BONE, gradient=False)
    mask = cr.face('head', 1, 'north')
    mask.fill(BONE)
    mask.rect(0, 0, 9, 9, BONE)
    for i in (2, 6):
        mask.rect(i, 3, 1, 2, INK)
    mask.hline(7, darken(BONE, 0.35), 3, 6)
    if role == 'elder':
        for i in (1, 4, 7):
            mask.vline(i, trim, 0, 3)
        mask.hline(8, trim, 2, 7)
        mask.points([(0, 5), (8, 5), (0, 6), (8, 6)], darken(BONE, 0.3))
    elif role == 'drummer':
        for ci, cj in ((2, 1), (6, 1)):
            mask.points([(ci - 1, cj), (ci + 1, cj), (ci, cj - 1), (ci, cj + 1)], trim)
        mask.hline(6, trim, 2, 7)
    elif role == 'hunter':
        mask.points([(1, 5), (1, 6), (3, 5), (5, 5), (7, 5), (7, 6)], INK)  # fang marks under the eyes
        mask.points([(2, 6), (6, 6)], darken(trim, 0.2))
        mask.hline(1, darken(trim, 0.3), 1, 8)
    else:
        mask.path([(1, 8), (7, 1)], trim)  # weaver needle
        mask.points([(7, 0), (8, 1)], lighten(trim, 0.5))
        mask.points([(4, 6)], darken(trim, 0.2))
    # hood
    cr.coat('head', 2, 'weave', darken(cloth, 0.1), gradient=False)
    for name in SIDES:
        f = cr.face('head', 2, name)
        f.hline(f.h - 1, darken(cloth, 0.45))
        f.hline(0, lighten(cloth, 0.2))
    # braids / feathers / beads / tassels
    for idx in (3, 4):
        if role == 'elder':
            cr.coat('head', idx, 'flat', GREY, gradient=False)
            for name in SIDES:
                f = cr.face('head', idx, name)
                for j in range(1, f.h, 2):
                    f.put(0, j, darken(GREY, 0.35))
                f.put(0, f.h - 1, trim)
        elif role == 'drummer':
            cr.coat('head', idx, 'flat', trim, gradient=False)
            for name in SIDES:
                f = cr.face('head', idx, name)
                f.put(0, 0, LEATHER)
                f.put(0, f.h - 1, lighten(trim, 0.5))
        elif role == 'hunter':
            cr.coat('head', idx, 'flat', LEATHER, gradient=False)
            for name in SIDES:
                f = cr.face('head', idx, name)
                for j in range(1, f.h, 2):
                    f.put(0, j, BONE)
        else:
            cr.coat('head', idx, 'flat', trim, gradient=False)
            for name in SIDES:
                f = cr.face('head', idx, name)
                f.put(0, 0, darken(cloth, 0.2))
                f.put(0, 2, lighten(trim, 0.4))
                f.put(0, 4, lighten(trim, 0.4))
    # torso
    torso_mat = 'weave' if role in ('elder', 'weaver') else 'cloth'
    torso_col = cloth if role != 'hunter' else LEATHER
    cr.coat('body', 0, torso_mat, torso_col, gradient=False)
    chest = cr.face('body', 0, 'north')
    back = cr.face('body', 0, 'south')
    if role == 'elder':
        for i in range(0, 8, 2):
            chest.put(i, 1, BONE)
            chest.put(i + 1, 1, darken(BONE, 0.3))
        chest.rect(3, 3, 2, 8, trim)
        chest.vline(3, darken(trim, 0.25), 3, 11)
        back.rect(3, 2, 2, 9, trim)
    elif role == 'drummer':
        chest.path([(0, 0), (7, 7)], LEATHER)
        chest.path([(7, 0), (0, 7)], LEATHER)
        chest.rect(3, 3, 2, 2, trim)
        back.path([(0, 0), (7, 7)], LEATHER)
        back.path([(7, 0), (0, 7)], LEATHER)
        for name in ('west', 'east'):
            cr.face('body', 0, name).hline(4, LEATHER)
        chest.rect(0, 7, 8, 1, darken(cloth, 0.4))
    elif role == 'hunter':
        for i in range(0, 8):
            chest.put(i, 1 if i in (0, 7) else 2 if i in (1, 6) else 3, BONE if i % 2 == 0 else darken(BONE, 0.3))
        chest.points([(3, 4), (4, 4)], BONE)
        chest.put(3, 5, BONE)
        chest.path([(6, 0), (1, 11)], darken(LEATHER, 0.35))
        for name in ('west', 'east'):
            cr.face('body', 0, name).hline(6, darken(LEATHER, 0.35))
        back.rect(1, 2, 6, 7, darken(LEATHER, 0.2))
        back.hline(4, trim, 2, 6)
    else:
        for j in (2, 5, 8):
            chest.hline(j, trim, 1, 7)
            chest.hline(j + 1, darken(cloth, 0.3), 1, 7)
        chest.points([(2, 2), (5, 5), (2, 8)], lighten(trim, 0.5))
        back.hline(4, trim, 1, 7)
        back.hline(8, trim, 1, 7)
    cr.face('body', 0, 'top').fill(darken(cloth, 0.2))
    # belt
    cr.coat('body', 1, 'flat', LEATHER, gradient=False)
    belt = cr.face('body', 1, 'north')
    belt.rect(4, 0, 1, 2, trim if role != 'hunter' else BONE)
    if role == 'weaver':
        for i, col in ((0, trim), (2, (0xd7, 0x8a, 0x3a)), (6, (0x8a, 0x5f, 0xc7)), (8, lighten(trim, 0.4))):
            belt.rect(i, 0, 1, 2, col)
            belt.put(i, 0, lighten(col, 0.45))
    elif role == 'hunter':
        for name in ('west', 'east'):
            cr.face('body', 1, name).rect(1, 0, 2, 2, BONE)
    elif role == 'drummer':
        for name in ('west', 'east'):
            cr.face('body', 1, name).rect(1, 0, 3, 2, trim)
    # arms
    for part in cr.parts('arm'):
        outer = 'west' if part.endswith('0') else 'east'
        if role == 'drummer':
            cr.coat(part, 0, 'skin', skin, gradient=False)
            for name in SIDES:
                f = cr.face(part, 0, name)
                f.rect(0, 0, f.w, 2, cloth)
                f.rect(0, 8, f.w, 4, trim)
                f.hline(9, darken(trim, 0.35))
                f.hline(11, darken(trim, 0.35))
            cr.face(part, 0, 'bottom').fill(trim)
        else:
            sleeve = LEATHER if role == 'hunter' else cloth
            cr.coat(part, 0, 'cloth', sleeve, gradient=False)
            for name in SIDES:
                f = cr.face(part, 0, name)
                f.rect(0, 7, f.w, 3, skin)
                f.rect(0, 10, f.w, 2, darken(skin, 0.3))
                if role == 'elder':
                    f.hline(6, trim)
                elif role == 'hunter':
                    f.rect(0, 4, f.w, 3, darken(LEATHER, 0.3))
                    f.hline(5, BONE, 1, 3)
                else:
                    f.hline(6, trim)
                    f.put(1, 3, trim)
            cr.face(part, 0, 'bottom').fill(darken(skin, 0.3))
        f = cr.face(part, 0, outer)
        f.hline(0, lighten(cloth, 0.25))
    # legs
    for part in cr.parts('leg'):
        cr.coat(part, 0, 'cloth', LEATHER if role != 'weaver' else cloth, gradient=False)
        for name in SIDES:
            f = cr.face(part, 0, name)
            f.rect(0, 9, f.w, 3, darken(LEATHER, 0.4))
            f.hline(5, trim if role != 'hunter' else darken(LEATHER, 0.25))
            if role == 'weaver':
                f.hline(2, trim, 1, 3)
        cr.face(part, 0, 'bottom').fill(darken(LEATHER, 0.5))
    # cloak (base skin shows a dark underside; the tinted overlay carries the tribe cloth)
    cr.coat('cloak', 0, 'weave', darken(cloth, 0.25), gradient=False)


def paint_kin_cloak(cr, tribe=None):
    """White-ish overlay for the tint layer: woven hood + cloak with the tribe glyph on the back plate."""
    base, dark, light = (226, 224, 216), (188, 186, 178), (246, 246, 240)
    ink = (96, 92, 86)
    for part, idx in (('head', 2), ('cloak', 0)):
        for name, f in cr.faces[(part, idx)].items():
            for j in range(f.h):
                for i in range(f.w):
                    k = pat_weave(i, j, f.w, f.h)
                    f.put(i, j, light if k > 0 else dark if k < 0 else base)
            f.hline(0, light)
            f.hline(f.h - 1, dark)
            f.vline(f.w - 1, dark)
    back = cr.face('cloak', 0, 'south')
    back.rect(0, 2, back.w, 11, base)
    back.rect(0, 2, back.w, 1, dark)
    back.rect(0, 12, back.w, 1, dark)
    if tribe:
        rows = GLYPHS[GLYPH_NAMES[tribe]]
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                if ch == '#':
                    back.put(i, j + 3, ink)
    else:
        back.rect(1, 4, 8, 7, dark)
        back.rect(2, 5, 6, 5, base)
    hood = cr.face('head', 2, 'north')
    hood.hline(hood.h - 1, ink)
    cr.face('cloak', 0, 'north').fill(dark)  # inside of the cloak
    cr.face('cloak', 0, 'bottom').fill(dark)


# ----------------------------------------------------------------------------------------------- the unsung
def paint_unsung(cr):
    p = cr.pal
    lac, hide, rune = p.base
    black = darken(lac, 0.78)

    def rune_column(f, c, j0, j1, bright=0.5):
        for j in range(j0, j1):
            f.put(c, j, step(rune, bright if (j // 3) % 2 else -0.1), glow=True)
        for j in range(j0 + 1, j1 - 1, 4):
            f.put(c - 1, j, step(rune, -0.3), glow=True)
            f.put(c + 1, j, step(rune, -0.3), glow=True)
        f.rect(c - 1, j0 - 1, 3, 1, step(rune, 0.9), glow=True)

    # shell panels: 0/1 north+south left, 2/3 north+south right, 4/5 west+east front, 6/7 west+east back
    outer = {0: 'north', 1: 'south', 2: 'north', 3: 'south', 4: 'west', 5: 'east', 6: 'west', 7: 'east'}
    opposite = {'north': 'south', 'south': 'north', 'west': 'east', 'east': 'west'}
    for idx in range(8):
        cr.coat('body', idx, 'lacquer', lac, gradient=False)
        for name in ('top', 'bottom', opposite[outer[idx]]):
            cr.face('body', idx, name).fill(black)
        f = cr.face('body', idx, outer[idx])
        # the slit edge (the panel end facing the middle of the side) is lined with a pale rune rim
        slit_left = idx in (1, 2, 4, 7)  # which end of the unwrapped outer face touches the slit
        edge = 0 if slit_left else f.w - 1
        f.vline(edge, step(hide, -0.9))
        for j in range(0, f.h, 3):
            f.put(edge, j, step(rune, 0.6), glow=True)
        rune_column(f, 4 if slit_left else f.w - 5, 2, f.h - 2)
        f.hline(f.h - 1, black)
        f.hline(0, step(lac, 1.4))
        # the panel end faces beside the slit show the shell thickness in dark lacquer
        for name in SIDES:
            if name not in (outer[idx], opposite[outer[idx]]):
                cr.face('body', idx, name).fill(darken(lac, 0.4))
    # taut hide top, a crack across, stitching along the rim; underside black (hollow)
    cr.coat('body', 8, 'hide', hide, gradient=False)
    top = cr.face('body', 8, 'top')
    for j in range(top.h):
        for i in range(top.w):
            d = math.hypot((i - 10.5) / 11.0, (j - 10.5) / 11.0)
            if d > 0.78:
                top.put(i, j, step(hide, -0.8 - (d - 0.78) * 3))
            elif d < 0.28:
                top.put(i, j, step(hide, 0.6))
    for k in range(0, top.w, 2):
        top.put(k, 0, darken(hide, 0.5))
        top.put(k + 1, top.h - 1, darken(hide, 0.5))
        top.put(0, k + 1, darken(hide, 0.5))
        top.put(top.w - 1, k, darken(hide, 0.5))
    top.path([(5, 6), (8, 8), (9, 11), (12, 13), (13, 16), (16, 17)], darken(hide, 0.55))
    top.path([(9, 10), (11, 10)], darken(hide, 0.35))
    top.path([(12, 13), (13, 12)], darken(hide, 0.35))
    for name in SIDES:
        cr.face('body', 8, name).fill(darken(lac, 0.25))
    cr.face('body', 8, 'bottom').fill(black)
    # lacquered bands: glossy red with a bright specular row, gold-hide seam pins, black underside
    for band in ('band0', 'band1'):
        for name, f in cr.faces[(band, 0)].items():
            if name in SIDES:
                f.fill(step(lac, 0.9))
                f.hline(0, step(lac, 2.2))
                f.hline(1, step(lac, 1.4))
                f.hline(f.h - 1, black)
                for i in range(2, f.w, 5):
                    f.put(i, 2, step(hide, -0.6))
            elif name == 'top':
                f.fill(step(lac, 1.5))
                f.rect(1, 1, f.w - 2, f.h - 2, step(lac, 0.6))
            else:
                f.fill(black)
    # floating head: dark hollow skull, bone mask with two glowing slits and a crack, lacquer horns
    cr.coat('head', 0, 'flat', darken(lac, 0.45), gradient=False)
    cr.face('head', 0, 'bottom').fill(step(rune, -1.2), glow=True)  # spirit light under the floating skull
    cr.coat('head', 1, 'bone', hide, gradient=False)
    mask = cr.face('head', 1, 'north')
    mask.fill(hide)
    mask.rect(1, 3, 2, 1, step(rune, 1.4), glow=True)
    mask.rect(5, 3, 2, 1, step(rune, 1.4), glow=True)
    mask.points([(1, 4), (6, 4)], step(rune, 0.2), glow=True)
    mask.path([(4, 0), (4, 2), (3, 3), (3, 5), (4, 6), (4, 7)], darken(hide, 0.55))
    mask.hline(6, darken(hide, 0.4), 2, 6)
    mask.points([(0, 0), (7, 0), (0, 7), (7, 7)], darken(hide, 0.5))
    mask.points([(2, 6), (5, 6)], INK)
    for idx in (2, 3):
        cr.coat('head', idx, 'lacquer', lac, gradient=False)
        cr.face('head', idx, 'top').fill(step(hide, -0.3))
        for name in SIDES:
            cr.face('head', idx, name).hline(0, step(hide, -0.6))
    # rune halo
    for b in cr.part_boxes('halo'):
        for name, f in cr.faces[('halo', b['index'])].items():
            for j in range(f.h):
                for i in range(f.w):
                    f.put(i, j, step(rune, 0.9 if (i + j) % 3 == 0 else -0.2), glow=True)
    # rune arms: lacquer shaft with a glowing rune column, dark palm with a hide knuckle line, glowing fingertips
    for part in cr.parts('arm'):
        arm, palm, *fingers = cr.part_boxes(part)
        cr.coat(part, arm['index'], 'lacquer', darken(lac, 0.15), gradient=False)
        outer_face = 'west' if part.endswith('0') else 'east'
        for name in (outer_face, 'north'):
            f = cr.face(part, arm['index'], name)
            rune_column(f, 1, 2, f.h - 1, bright=0.4)
        cr.coat(part, palm['index'], 'flat', darken(lac, 0.35), gradient=False)
        for name in SIDES:
            f = cr.face(part, palm['index'], name)
            f.hline(0, step(hide, -0.4))
            f.hline(1, step(rune, 0.3), glow=True)
        cr.face(part, palm['index'], 'top').fill(step(lac, 0.4))
        for b in fingers:
            cr.coat(part, b['index'], 'flat', darken(lac, 0.3), gradient=False)
            for name in SIDES:
                f = cr.face(part, b['index'], name)
                f.hline(f.h - 1, step(rune, 0.6), glow=True)
                f.hline(1, step(hide, -0.7))
            cr.face(part, b['index'], 'bottom').fill(step(rune, 1.4), glow=True)


# ----------------------------------------------------------------------------------------------- collars
def paint_collar(entry, boxes):
    """Transparent overlay: a woven spirit cord (white, tinted in-game) with a small mask bead on the chest."""
    cv = Canvas()
    cr = Creature(entry, boxes, cv, Palette(entry['colors']))
    cord_a, cord_b, ink = (236, 236, 230), (196, 198, 194), (92, 94, 96)
    body = cr.faces[('body', 0)]
    x, y, z, w, h, d = cr.box('body', 0)['box']
    rows = 2

    def weave(face, cells):
        for i, j in cells:
            face.put(i, j, cord_a if (i + j) % 2 == 0 else cord_b)

    top = body['top']
    weave(top, [(i, j) for j in range(top.h - rows, top.h) for i in range(top.w)])
    bot = body['bottom']
    weave(bot, [(i, j) for j in range(bot.h - rows, bot.h) for i in range(bot.w)])
    west = body['west']
    weave(west, [(i, j) for j in range(west.h) for i in range(west.w - rows, west.w)])
    east = body['east']
    weave(east, [(i, j) for j in range(east.h) for i in range(rows)])
    north = body['north']
    weave(north, [(i, j) for j in range(north.h) for i in range(north.w)])
    for j in range(north.h):
        north.put(0, j, ink)
        north.put(north.w - 1, j, ink)
    bi, bj = north.w // 2 - 1, max(1, north.h // 2 - 1)
    north.rect(bi - 1, bj - 1, 4, 4, ink)
    north.rect(bi, bj, 2, 2, cord_a)
    north.put(bi, bj, (255, 255, 255))
    north.points([(bi, bj + 1), (bi + 1, bj + 1)], ink)  # mask bead's eye line
    return cv.im


# ----------------------------------------------------------------------------------------------- entry points
def paint(entry, boxes, variant=None):
    """Paint a roster entry; returns (atlas, glow) PIL images."""
    cv = Canvas()
    pal = Palette(entry['colors'])
    cr = Creature(entry, boxes, cv, pal)
    if entry['id'] == 'tribal_kin':
        paint_kin(cr, variant or 'elder')
    elif entry['id'] == 'the_unsung':
        paint_unsung(cr)
    else:
        spec = SPECIES[entry['id']]
        default_coat(cr, spec)
        for b in boxes:
            cr.rim(b['part'], b['index'])
        spec['paint'](cr)
        return cv.im, cv.glow
    for b in boxes:
        cr.rim(b['part'], b['index'])
    return cv.im, cv.glow


def overlay(entry, boxes, name):
    cv = Canvas()
    cr = Creature(entry, boxes, cv, Palette(entry['colors']))
    if name == 'kin_cloak':
        paint_kin_cloak(cr)
    elif name.startswith('kin_cloak_'):
        paint_kin_cloak(cr, name[len('kin_cloak_'):])
    else:
        raise KeyError(name)
    return cv.im


def texture_names(entry):
    """Every PNG (without extension) written for an entry, in the order produced by write_textures."""
    if entry['id'] == 'tribal_kin':
        names = []
        for role in KIN_ROLES:
            names += [f'kin_{role}', f'kin_{role}_glow']
        return names + ['kin_cloak'] + [f'kin_cloak_{t}' for t in TRIBES]
    names = [entry['id'], entry['id'] + '_glow']
    if entry.get('kind') == 'animal':
        names.append('bonded_collar_' + entry['id'])
    return names


def write_textures(entry, boxes, texdir):
    """Write every PNG for an entry into texdir; returns {name: path}."""
    texdir = Path(texdir)
    texdir.mkdir(parents=True, exist_ok=True)
    written = {}

    def save(name, im):
        path = texdir / (name + '.png')
        im.save(path)
        written[name] = path

    if entry['id'] == 'tribal_kin':
        for role in KIN_ROLES:
            atlas, glow = paint(entry, boxes, role)
            save(f'kin_{role}', atlas)
            save(f'kin_{role}_glow', glow)
        save('kin_cloak', overlay(entry, boxes, 'kin_cloak'))
        for tribe in TRIBES:
            save(f'kin_cloak_{tribe}', overlay(entry, boxes, f'kin_cloak_{tribe}'))
    else:
        atlas, glow = paint(entry, boxes)
        save(entry['id'], atlas)
        save(entry['id'] + '_glow', glow)
        if entry.get('kind') == 'animal':
            save('bonded_collar_' + entry['id'], paint_collar(entry, boxes))
    return written


def main():
    import json
    root = Path(__file__).resolve().parents[2]
    texdir = root / 'src/main/resources/assets/tribalpower/textures/entity'
    entries = json.loads((root / 'art/creatures/roster.json').read_text(encoding='utf-8'))
    tribes = root / 'art/creatures/roster_tribes.json'
    if tribes.exists():
        entries += json.loads(tribes.read_text(encoding='utf-8'))
    for entry in entries:
        boxes = layout(entry)
        paths = write_textures(entry, boxes, texdir)
        print(entry['id'], '->', ', '.join(sorted(p.name for p in paths.values())))
    if entries:
        # contact strip of every atlas for quick review
        strip = Image.new('RGBA', (256 * min(4, len(entries)), 256 * ((len(entries) + 3) // 4)), (30, 34, 40, 255))
        for n, entry in enumerate(entries):
            name = 'kin_elder' if entry['id'] == 'tribal_kin' else entry['id']
            strip.alpha_composite(Image.open(texdir / f'{name}.png'), ((n % 4) * 256, (n // 4) * 256))
        (root / 'art/creatures').mkdir(parents=True, exist_ok=True)
        strip.save(root / 'art/creatures/atlas-sheet.png')


if __name__ == '__main__':
    main()
