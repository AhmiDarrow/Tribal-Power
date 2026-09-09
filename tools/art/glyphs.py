"""Nine 9x9 tribe glyphs (design 3.0 §8) shared by banners, hearths, marks, Kinship Totems and the Codex.

Each glyph is a list of nine 9-character strings; '#' is ink, '.' is empty. Order matches
tribe/TribeDefinition: soil, stone, sprout, claw, spark, clock, swarm, sigil, spindle.

    from glyphs import GLYPHS, draw_glyph, TRIBES, COLOURS
    draw_glyph(draw, 'drum', x, y, colour, scale=2)
"""

TRIBES = ['soil', 'stone', 'sprout', 'claw', 'spark', 'clock', 'swarm', 'sigil', 'spindle']
NAMES = ['Pad-keepers', 'Grit-singers', 'Rootbinders', 'Edge-walkers', 'Drumhearts',
         'Pattern-weavers', 'Colony-keepers', 'Seal-carvers', 'Loom-stitchers']
COLOURS = {
    'soil': (0x8b, 0x5a, 0x2b), 'stone': (0x7d, 0x87, 0x91), 'sprout': (0x4f, 0x9a, 0x5a),
    'claw': (0xb8, 0x51, 0x2f), 'spark': (0xe0, 0xa3, 0x2d), 'clock': (0x4a, 0x7f, 0xb5),
    'swarm': (0xd7, 0xb2, 0x3c), 'sigil': (0x8a, 0x5f, 0xc7), 'spindle': (0x62, 0xd1, 0xc9),
}
GLYPH_NAMES = {'soil': 'hearth', 'stone': 'mesh', 'sprout': 'root', 'claw': 'boot', 'spark': 'drum',
               'clock': 'cog', 'swarm': 'comb', 'sigil': 'seal', 'spindle': 'spindle'}

GLYPHS = {
    # hearth: a bowl with three flames
    'hearth': [
        '....#....',
        '..#.#.#..',
        '..#.#.#..',
        '.#.###.#.',
        '#########',
        '.#######.',
        '..#####..',
        '...###...',
        '..#####..',
    ],
    # mesh: a woven lattice
    'mesh': [
        '#.#.#.#.#',
        '.#.#.#.#.',
        '#.#.#.#.#',
        '.#.#.#.#.',
        '#.#.#.#.#',
        '.#.#.#.#.',
        '#.#.#.#.#',
        '.#.#.#.#.',
        '#.#.#.#.#',
    ],
    # root: a sprout above spreading roots
    'root': [
        '....#....',
        '...##....',
        '..#.#.#..',
        '....##...',
        '....#....',
        '#########',
        '.#..#..#.',
        '#...#...#',
        '#..#.#..#',
    ],
    # boot: a walking boot with a spur
    'boot': [
        '...###...',
        '...#.#...',
        '...#.#...',
        '...#.#...',
        '...#.##..',
        '..##..#..',
        '.#....#..',
        '#......#.',
        '#########',
    ],
    # drum: a standing drum with a strike line
    'drum': [
        '....#....',
        '.#######.',
        '#.......#',
        '#########',
        '#.#...#.#',
        '#..#.#..#',
        '#...#...#',
        '#########',
        '.#######.',
    ],
    # cog: a gear with a hollow hub
    'cog': [
        '..#.#.#..',
        '.#######.',
        '##.....##',
        '.#..#..#.',
        '##.###.##',
        '.#..#..#.',
        '##.....##',
        '.#######.',
        '..#.#.#..',
    ],
    # comb: three hexagonal cells
    'comb': [
        '..#...#..',
        '.#.#.#.#.',
        '#...#...#',
        '#...#...#',
        '.#.#.#.#.',
        '..#...#..',
        '.#.#.#.#.',
        '#...#...#',
        '.#.#.#.#.',
    ],
    # seal: a ring around a dotted centre
    'seal': [
        '..#####..',
        '.#.....#.',
        '#..###..#',
        '#.#...#.#',
        '#.#.#.#.#',
        '#.#...#.#',
        '#..###..#',
        '.#.....#.',
        '..#####..',
    ],
    # spindle: a shaft through a diamond bobbin with crossing thread
    'spindle': [
        '#...#...#',
        '.#..#..#.',
        '..#.#.#..',
        '...###...',
        '..#####..',
        '...###...',
        '..#.#.#..',
        '.#..#..#.',
        '#...#...#',
    ],
}


def glyph_for_tribe(tribe):
    return GLYPHS[GLYPH_NAMES[tribe]]


def draw_glyph(draw, name, x, y, colour, scale=1, shadow=None):
    """Paint glyph `name` with its top-left at (x, y); each cell is `scale` px. Optional 1 px drop shadow."""
    rows = GLYPHS[name]
    if shadow is not None:
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                if ch == '#':
                    draw.rectangle((x + i * scale + 1, y + j * scale + 1, x + i * scale + scale, y + j * scale + scale), fill=shadow)
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch == '#':
                draw.rectangle((x + i * scale, y + j * scale, x + i * scale + scale - 1, y + j * scale + scale - 1), fill=colour)


def shade(c, amount):
    return tuple(max(0, min(255, v + amount)) for v in c[:3]) + (255,)


def lighten(c, f=0.35):
    return tuple(int(v + (255 - v) * f) for v in c[:3]) + (255,)


def darken(c, f=0.35):
    return tuple(int(v * (1 - f)) for v in c[:3]) + (255,)
