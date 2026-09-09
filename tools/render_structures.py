"""Renders the 3.0 structure templates (data/tribalpower/structure/*.nbt) as isometric
voxel previews with Blender (CPU Cycles) so the templates can be reviewed without a client.

Every non-air block becomes a cube coloured by a palette map (block id -> colour).  Mod
blocks take the average colour of their texture in assets/tribalpower/textures/block;
vanilla blocks fall back to a name-keyed table (planks brown, wool by colour, stone grey…).
Entities are drawn as small coloured markers (Kin = tribe colour, sentinels = slate,
anything else = magenta).  Output:

  art/structures/<name>.png                         one render per template
  art/structures/structures-3.0-contact-sheet.png   labelled sheet of all of them

Run: python3 tools/render_structures.py [--samples N] [--size WxH] [--only name,name]
(needs `import bpy`, nbtlib, Pillow).
"""
from pathlib import Path
import argparse
import gzip
import math
import sys

import nbtlib
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
STRUCT = RES / 'data/tribalpower/structure'
TEX = RES / 'assets/tribalpower/textures/block'
OUT = ROOT / 'art/structures'

TRIBES = ['soil', 'stone', 'sprout', 'claw', 'spark', 'clock', 'swarm', 'sigil', 'spindle']
TRIBE_COLOURS = {'soil': 0x8b5a2b, 'stone': 0x7d8791, 'sprout': 0x4f9a5a, 'claw': 0xb8512f, 'spark': 0xe0a32d,
                 'clock': 0x4a7fb5, 'swarm': 0xd7b23c, 'sigil': 0x8a5fc7, 'spindle': 0x62d1c9}
DYE = {
    'white': 0xe9ecec, 'light_gray': 0x8e8e86, 'gray': 0x3e4447, 'black': 0x141519, 'brown': 0x724728,
    'red': 0xa02722, 'orange': 0xf07613, 'yellow': 0xf8c527, 'lime': 0x70b919, 'green': 0x546d1b,
    'cyan': 0x158991, 'light_blue': 0x3aafd9, 'blue': 0x35399d, 'purple': 0x792aac, 'magenta': 0xbd44b3,
    'pink': 0xed8dac,
}
# vanilla fallbacks, matched by exact path first, then by longest keyword
VANILLA = {
    'grass_block': 0x6fa14a, 'dirt': 0x79553a, 'coarse_dirt': 0x6b4c33, 'dirt_path': 0x977f4d, 'rooted_dirt': 0x8f6b4e,
    'podzol': 0x5d3f27, 'mud': 0x3c3a3d, 'moss_block': 0x596e2b, 'moss_carpet': 0x64792f, 'mycelium': 0x6f6a75,
    'gravel': 0x847f7e, 'sand': 0xdbcfa0, 'clay': 0x9fa4b1, 'stone': 0x7d7d7d, 'cobblestone': 0x7a7a7a,
    'mossy_cobblestone': 0x66775a, 'stone_bricks': 0x7b7b7b, 'cracked_stone_bricks': 0x6f6f6f,
    'mossy_stone_bricks': 0x6c7a5d, 'chiseled_stone_bricks': 0x828282, 'andesite': 0x888a86, 'polished_andesite': 0x8b8e8a,
    'diorite': 0xbdbdbd, 'granite': 0x996a55, 'deepslate': 0x4d4d51, 'deepslate_bricks': 0x46464a, 'deepslate_tiles': 0x38383b,
    'polished_deepslate': 0x484849, 'tuff': 0x6d6e66, 'calcite': 0xdfe0dd, 'smooth_stone': 0xa0a0a0,
    'blackstone': 0x2a252c, 'basalt': 0x515156, 'obsidian': 0x100b1a, 'bedrock': 0x555555,
    'oak_planks': 0xb08a56, 'spruce_planks': 0x735531, 'birch_planks': 0xc8b77a, 'dark_oak_planks': 0x492f17,
    'jungle_planks': 0xb18a5d, 'acacia_planks': 0xb26237, 'cherry_planks': 0xe3b4ad, 'mangrove_planks': 0x773636,
    'bamboo_planks': 0xc8b155, 'oak_log': 0x6e5530, 'spruce_log': 0x3b2a18, 'birch_log': 0xd7d3cb, 'dark_oak_log': 0x3a2814,
    'jungle_log': 0x554319, 'acacia_log': 0x6a6058, 'cherry_log': 0x3b1f28, 'stripped_spruce_log': 0x745a35,
    'stripped_oak_log': 0xb39a67, 'stripped_dark_oak_log': 0x4b3618,
    'oak_leaves': 0x3f7a2a, 'spruce_leaves': 0x2f5a35, 'birch_leaves': 0x5d8a3a, 'dark_oak_leaves': 0x2f6320,
    'jungle_leaves': 0x3c8f27, 'azalea_leaves': 0x5f8f3c, 'flowering_azalea_leaves': 0x6f8f5c, 'cherry_leaves': 0xe7a9c4,
    'campfire': 0xd2792b, 'soul_campfire': 0x3fa9a8, 'lantern': 0xf5c26b, 'soul_lantern': 0x63c2c0, 'torch': 0xffd27a,
    'glass': 0xc8e6f2, 'glass_pane': 0xc8e6f2, 'tinted_glass': 0x2b2530, 'glowstone': 0xf3c56d, 'sea_lantern': 0xc9e7e1,
    'chest': 0x9b6d34, 'barrel': 0x7f5d34, 'composter': 0x6b4f2b, 'hay_block': 0xc9a23a, 'loom': 0x8c6a45,
    'note_block': 0x6c4a2e, 'jukebox': 0x5b3d24, 'crafting_table': 0x7c5b36, 'smithing_table': 0x3c3c48,
    'cartography_table': 0x6d5138, 'fletching_table': 0xc7b183, 'furnace': 0x6f6f6f, 'blast_furnace': 0x555555,
    'anvil': 0x484848, 'grindstone': 0x777777, 'stonecutter': 0x7d7d7d, 'lectern': 0xa07d4b, 'bookshelf': 0x8a6a3f,
    'bee_nest': 0xc9a24a, 'beehive': 0xb59650, 'honey_block': 0xf7ac2e, 'honeycomb_block': 0xe7a52e,
    'bell': 0xd7a53a, 'cauldron': 0x3b3b3b, 'flower_pot': 0x7a4a33, 'ladder': 0x9a7b4a, 'scaffolding': 0xc7a45b,
    'oak_fence': 0xb08a56, 'spruce_fence': 0x735531, 'dark_oak_fence': 0x492f17, 'oak_fence_gate': 0xb08a56,
    'spruce_fence_gate': 0x735531, 'iron_bars': 0x8f9294, 'chain': 0x50545a, 'lightning_rod': 0xc0743c,
    'oak_slab': 0xb08a56, 'spruce_slab': 0x735531, 'dark_oak_slab': 0x492f17, 'oak_stairs': 0xb08a56,
    'spruce_stairs': 0x735531, 'dark_oak_stairs': 0x492f17, 'stone_slab': 0x7d7d7d, 'cobblestone_slab': 0x7a7a7a,
    'stone_brick_slab': 0x7b7b7b, 'polished_andesite_slab': 0x8b8e8a, 'stone_brick_stairs': 0x7b7b7b,
    'cobblestone_stairs': 0x7a7a7a, 'cobblestone_wall': 0x7a7a7a, 'mossy_cobblestone_wall': 0x66775a,
    'stone_brick_wall': 0x7b7b7b, 'deepslate_brick_wall': 0x46464a, 'copper_block': 0xc0743c, 'cut_copper': 0xbf6f3d,
    'oxidized_copper': 0x52a28a, 'exposed_copper': 0xa17c5f, 'weathered_copper': 0x6fa075, 'raw_copper_block': 0x9e6d4d,
    'raw_iron_block': 0xbba48f, 'iron_block': 0xd8d8d8, 'gold_block': 0xf5d43e, 'amethyst_block': 0x8a63d0,
    'redstone_block': 0xb01b0a, 'redstone_lamp': 0x7a4b28, 'target': 0xd9c0b0, 'observer': 0x606060,
    'repeater': 0x9a9a9a, 'comparator': 0x9a9a9a, 'lever': 0x7d7d7d, 'daylight_detector': 0x8f7d61,
    'coal_ore': 0x5e5e5e, 'iron_ore': 0x9c8a7a, 'copper_ore': 0x9a8067, 'gold_ore': 0xa8935a, 'redstone_ore': 0x9a5b5b,
    'raw_copper': 0x9e6d4d, 'minecart': 0x8c8c8c, 'rail': 0x8a7a5a,
    'water': 0x3f76e4, 'lava': 0xd8631a, 'fire': 0xff8f2a, 'soul_fire': 0x3fa9a8, 'snow': 0xf5fbfb, 'snow_block': 0xf5fbfb,
    'ice': 0x91b8f5, 'packed_ice': 0x8bb5f0, 'blue_ice': 0x74a8f4, 'hay': 0xc9a23a, 'sponge': 0xc4c24a,
    'skeleton_skull': 0xd9d9d9, 'bone_block': 0xe1dcc4, 'candle': 0xe6d7b2, 'end_rod': 0xe9e2d0,
    'dandelion': 0xf8dc3f, 'poppy': 0xd8331d, 'blue_orchid': 0x2eb0e0, 'allium': 0xb26bd9, 'azure_bluet': 0xdfe6d5,
    'red_tulip': 0xc52b26, 'orange_tulip': 0xe08a2e, 'white_tulip': 0xe6ecdc, 'pink_tulip': 0xe8a3c2,
    'oxeye_daisy': 0xdfe6cf, 'cornflower': 0x4a7fd8, 'lily_of_the_valley': 0xe0e8d6, 'sunflower': 0xe2c22a,
    'lilac': 0xc9a1d3, 'rose_bush': 0xc23324, 'peony': 0xe0a8c8, 'short_grass': 0x6fa14a, 'tall_grass': 0x6fa14a,
    'fern': 0x5e9440, 'large_fern': 0x5e9440, 'dead_bush': 0x8b6a3a, 'sweet_berry_bush': 0x4f7a3a,
    'azalea': 0x5f8f3c, 'flowering_azalea': 0x7f8f5c, 'oak_sapling': 0x4c7f2c, 'spruce_sapling': 0x2f5a35,
    'cobweb': 0xe8e8e8, 'vine': 0x3d6b2a, 'glow_lichen': 0x6f9a8a, 'brown_mushroom': 0x9c7550, 'red_mushroom': 0xd12f28,
    'pumpkin': 0xd88a1e, 'carved_pumpkin': 0xd88a1e, 'jack_o_lantern': 0xe8a03a, 'melon': 0x6f9b2c,
    'wheat': 0xd2b358, 'carrots': 0x5f9b2c, 'potatoes': 0x5f9b2c, 'farmland': 0x6e4a2a,
    'terracotta': 0x985e43, 'bricks': 0x9a5b4e, 'nether_bricks': 0x2c161a, 'red_nether_bricks': 0x4b1a1c,
    'prismarine': 0x63a09a, 'dark_prismarine': 0x335b4e, 'purpur_block': 0xa77ba7, 'end_stone': 0xdbdfa2,
    'sandstone': 0xd9cc9d, 'red_sandstone': 0xb8612a, 'smooth_sandstone': 0xdcd1a3,
    'quartz_block': 0xece6df, 'quartz_pillar': 0xece6df, 'smooth_quartz': 0xefe9e2,
    'crimson_planks': 0x6a3149, 'warped_planks': 0x2b6a68, 'cherry_sapling': 0xe4a5be, 'mangrove_roots': 0x4f3a24,
    'stripped_cherry_log': 0xd5a49b, 'shroomlight': 0xf1a850, 'ochre_froglight': 0xf0e7b5, 'verdant_froglight': 0xd9f0d4,
    'pearlescent_froglight': 0xf3e3ec, 'spore_blossom': 0xd66fa9, 'big_dripleaf': 0x5c8a3a, 'small_dripleaf': 0x5c8a3a,
    'decorated_pot': 0x9b6a4b, 'suspicious_gravel': 0x847f7e, 'cracked_deepslate_bricks': 0x3f3f43,
    'polished_blackstone_bricks': 0x2f2a33, 'cracked_polished_blackstone_bricks': 0x2a262e,
    'polished_blackstone': 0x35303a, 'gilded_blackstone': 0x3b3230, 'chiseled_polished_blackstone': 0x2f2a33,
    'lodestone': 0x8b8a8b, 'tripwire_hook': 0x8f8f8f, 'string': 0xffffff,
    'packed_mud': 0x8b6b4e, 'mud_bricks': 0x8f7359, 'dark_oak_door': 0x4a3018, 'spruce_door': 0x6f5230, 'oak_door': 0x9a7a4a,
    'spruce_trapdoor': 0x6f5230, 'dark_oak_sapling': 0x2f5a20, 'cobbled_deepslate': 0x4f4f53, 'chiseled_deepslate': 0x3b3b3f,
    'polished_deepslate_slab': 0x484849, 'polished_deepslate_stairs': 0x484849, 'deepslate_brick_wall': 0x46464a,
    'raw_gold_block': 0xd9a640, 'chiseled_bookshelf': 0x8a6a3f, 'bone_block': 0xe1dcc4, 'red_sand': 0xbe6b2a,
    'honeycomb_block': 0xe7a52e, 'moss_carpet': 0x64792f, 'carved_pumpkin': 0xd88a1e, 'fletching_table': 0xc7b183,
    'smithing_table': 0x3c3c48, 'lectern': 0xa07d4b, 'observer': 0x606060, 'target': 0xd9c0b0, 'daylight_detector': 0x8f7d61,
    'stone_brick_wall': 0x7b7b7b, 'stone_brick_stairs': 0x7b7b7b, 'cut_copper': 0xbf6f3d, 'birch_slab': 0xc8b77a,
    'mossy_stone_bricks': 0x6c7a5d, 'cracked_stone_bricks': 0x6f6f6f, 'deepslate_tiles': 0x38383b, 'soul_lantern': 0x63c2c0,
    'birch_sapling': 0x5d8a3a, 'jungle_sapling': 0x3c8f27, 'orange_terracotta': 0xa25526, 'stone_stairs': 0x7d7d7d,
}
KEYWORDS = [  # (substring, colour), matched longest-first when no exact entry exists
    ('cobblestone', 0x7a7a7a), ('stone_brick', 0x7b7b7b), ('deepslate', 0x4d4d51), ('stone', 0x7d7d7d),
    ('planks', 0xb08a56), ('log', 0x6e5530), ('wood', 0x6e5530), ('leaves', 0x3f7a2a), ('fence', 0x9a7b4a),
    ('slab', 0x9a9a9a), ('stairs', 0x9a9a9a), ('wall', 0x8a8a8a), ('glass', 0xc8e6f2), ('lantern', 0xf5c26b),
    ('torch', 0xffd27a), ('copper', 0xc0743c), ('iron', 0xd8d8d8), ('gold', 0xf5d43e), ('ore', 0x8a8a8a),
    ('sand', 0xdbcfa0), ('dirt', 0x79553a), ('grass', 0x6fa14a), ('flower', 0xe08ab8), ('tulip', 0xe08a2e),
    ('crystal', 0x8fe8e0), ('water', 0x3f76e4), ('wool', 0xe9ecec), ('carpet', 0xe9ecec), ('concrete', 0x9a9a9a),
    ('terracotta', 0x985e43), ('candle', 0xe6d7b2), ('bed', 0xb03030), ('banner', 0xe9ecec),
]
EMISSIVE = {'campfire', 'lantern', 'soul_lantern', 'torch', 'glowstone', 'sea_lantern', 'spirit_lantern', 'march_crystal',
            'fire', 'soul_fire', 'lava', 'jack_o_lantern', 'shroomlight', 'redstone_lamp', 'end_rod', 'spirit_light',
            'ochre_froglight', 'verdant_froglight', 'pearlescent_froglight', 'soul_campfire', 'candle'}
# blocks that are not full cubes: rendered smaller so they read as props
SMALL = {'lantern', 'soul_lantern', 'torch', 'campfire', 'flower_pot', 'candle', 'lever', 'tripwire_hook', 'end_rod',
         'skeleton_skull', 'bell', 'lightning_rod', 'chain', 'ladder', 'rail'}
FLAT = {'carpet', 'moss_carpet', 'dirt_path', 'farmland', 'snow', 'repeater', 'comparator', 'daylight_detector',
        'pressure_plate', 'trapdoor', 'rail'}
THIN = {'fence', 'glass_pane', 'iron_bars', 'wall', 'fence_gate', 'chain', 'lightning_rod', 'end_rod', 'rail', 'door'}
PLANT = {'short_grass', 'tall_grass', 'fern', 'large_fern', 'dead_bush', 'sweet_berry_bush', 'azalea', 'flowering_azalea',
         'dandelion', 'poppy', 'blue_orchid', 'allium', 'azure_bluet', 'red_tulip', 'orange_tulip', 'white_tulip',
         'pink_tulip', 'oxeye_daisy', 'cornflower', 'lily_of_the_valley', 'sunflower', 'lilac', 'rose_bush', 'peony',
         'torchflower', 'wither_rose', 'brown_mushroom', 'red_mushroom', 'march_leaf', 'spirit_reed', 'ley_thistle',
         'cobweb', 'vine', 'glow_lichen', 'wheat', 'carrots', 'potatoes', 'big_dripleaf', 'small_dripleaf',
         'spore_blossom', 'pink_petals', 'sugar_cane', 'bamboo', 'seagrass', 'kelp'}


def hex_rgb(c):
    return ((c >> 16) & 255) / 255, ((c >> 8) & 255) / 255, (c & 255) / 255


def texture_average(name):
    """Average colour of the mod block's textures (side + top), alpha-weighted. None if absent."""
    files = [TEX / f'{name}.png', TEX / f'{name}_top.png']
    files = [f for f in files if f.exists()]
    if not files:
        return None
    total = [0.0, 0.0, 0.0]; weight = 0.0
    for f in files:
        raw = Image.open(f).convert('RGBA').tobytes()
        for i in range(0, len(raw), 4):
            r, g, b, a = raw[i], raw[i + 1], raw[i + 2], raw[i + 3]
            if a < 32:
                continue
            w = a / 255.0
            total[0] += r * w; total[1] += g * w; total[2] += b * w; weight += w
    if weight == 0:
        return None
    return tuple(v / weight / 255.0 for v in total)


_cache = {}


def colour_for(block_id, props):
    """Palette lookup: block id (+ properties for tribe-keyed blocks) -> linear-ish RGB tuple."""
    ns, _, path = block_id.partition(':')
    key = (block_id, props.get('tribe'), props.get('color'))
    if key in _cache:
        return _cache[key]
    col = None
    if ns == 'tribalpower':
        if 'tribe' in props and path in ('tribe_banner', 'kinship_totem'):
            col = texture_average(f'{path}_{TRIBES[int(props["tribe"])]}')
        if col is None:
            col = texture_average(path)
    if col is None:
        if path in VANILLA:
            col = hex_rgb(VANILLA[path])
        else:
            for dye, c in DYE.items():
                if path.startswith(dye + '_'):
                    col = hex_rgb(c); break
    if col is None:
        best = None
        for kw, c in KEYWORDS:
            if kw in path and (best is None or len(kw) > len(best[0])):
                best = (kw, c)
        col = hex_rgb(best[1]) if best else (0.95, 0.15, 0.85)  # magenta = unknown
        if best is None:
            print(f'  ! no palette colour for {block_id}, drawing magenta', file=sys.stderr)
    _cache[key] = col
    return col


def shape_for(path):
    if path in PLANT or path.endswith('_sapling') or path.startswith('potted_'):
        return 'plant'
    if path.endswith('_bed'):
        return 'slab'
    if path in SMALL:
        return 'small'
    if any(path == k or path.endswith('_' + k) for k in FLAT):
        return 'flat'
    if any(path == k or path.endswith('_' + k) for k in THIN):
        return 'thin'
    if path.endswith('_slab'):
        return 'slab'
    if path == 'tribe_banner' or path == 'lore_tablet':
        return 'banner'
    return 'cube'


# buried / roofed structures also get a cutaway render with everything above this layer removed
CUTAWAY = {'ancestor_hall': 4}


def load_template(path):
    nbt = nbtlib.load(path)
    size = [int(v) for v in nbt['size']]
    palette = [(str(p['Name']), {k: str(v) for k, v in p.get('Properties', {}).items()}) for p in nbt['palette']]
    blocks = []
    for b in nbt['blocks']:
        name, props = palette[int(b['state'])]
        if name == 'minecraft:air' or name == 'minecraft:cave_air':
            continue
        x, y, z = (int(v) for v in b['pos'])
        blocks.append((x, y, z, name, props))
    ents = []
    for e in nbt['entities']:
        x, y, z = (float(v) for v in e['pos'])
        tag = e['nbt']
        ents.append((x, y, z, str(tag['id']), {k: tag[k] for k in tag if k != 'id'}))
    return size, blocks, ents


# ---------------------------------------------------------------- blender

def build_scene(size, blocks, ents, res=(900, 680)):
    import bpy
    import bmesh

    bpy.ops.wm.read_factory_settings(use_empty=True)
    scene = bpy.context.scene
    scene.render.engine = 'CYCLES'
    scene.cycles.device = 'CPU'
    scene.render.resolution_x, scene.render.resolution_y = res
    W, H, D = size
    solid = {(x, y, z) for (x, y, z, n, p) in blocks if shape_for(n.partition(':')[2]) in ('cube',)}

    materials = {}

    def material(colour, emissive=False, alpha=1.0):
        key = (tuple(round(c, 3) for c in colour), emissive, alpha)
        if key in materials:
            return materials[key]
        mat = bpy.data.materials.new(f'm{len(materials)}')
        mat.use_nodes = True
        bsdf = mat.node_tree.nodes['Principled BSDF']
        bsdf.inputs['Base Color'].default_value = (*[c ** 2.2 for c in colour], 1.0)
        bsdf.inputs['Roughness'].default_value = 0.85
        if emissive:
            bsdf.inputs['Emission Color'].default_value = (*[min(1.0, c ** 2.2 * 1.6 + 0.15) for c in colour], 1.0)
            bsdf.inputs['Emission Strength'].default_value = 1.4
        if alpha < 1.0:
            bsdf.inputs['Alpha'].default_value = alpha
            mat.blend_method = 'BLEND'
        materials[key] = mat
        return mat

    # group faces by material into one mesh each (fast to build + render)
    groups = {}

    def add_box(mat, x0, y0, z0, x1, y1, z1):
        groups.setdefault(mat, []).append((x0, y0, z0, x1, y1, z1))

    # blender axes: X = template x, Y = template z (mirrored so north is "back"), Z = template y
    for (x, y, z, name, props) in blocks:
        path = name.partition(':')[2]
        col = colour_for(name, props)
        shape = shape_for(path)
        mat = material(col, path in EMISSIVE, 0.45 if 'glass' in path else 1.0)
        if shape == 'cube':
            add_box(mat, x, y, z, x + 1, y + 1, z + 1)
        elif shape == 'slab':
            top = props.get('type') == 'top'
            add_box(mat, x, y + (0.5 if top else 0), z, x + 1, y + (1 if top else 0.5), z + 1)
        elif shape == 'flat':
            add_box(mat, x, y, z, x + 1, y + 0.12, z + 1)
        elif shape == 'thin':
            add_box(mat, x + 0.35, y, z + 0.35, x + 0.65, y + 1, z + 0.65)
            for d, (dx, dz) in (('north', (0, -1)), ('south', (0, 1)), ('west', (-1, 0)), ('east', (1, 0))):
                if props.get(d) == 'true':
                    add_box(mat, x + 0.5 + min(0, dx) * 0.5, y + 0.3, z + 0.5 + min(0, dz) * 0.5,
                            x + 0.5 + max(0, dx) * 0.5, y + 0.9, z + 0.5 + max(0, dz) * 0.5)
        elif shape == 'small':
            add_box(mat, x + 0.3, y, z + 0.3, x + 0.7, y + 0.5, z + 0.7)
        elif shape == 'plant':
            add_box(mat, x + 0.3, y, z + 0.3, x + 0.7, y + 0.7, z + 0.7)
        elif shape == 'banner':
            facing = props.get('facing', 'north')
            wall = props.get('wall') == 'true' or path == 'lore_tablet'
            if wall:
                # attached to the block behind (opposite of facing)
                if facing == 'north':
                    add_box(mat, x + 0.1, y, z + 0.8, x + 0.9, y + 1, z + 1)
                elif facing == 'south':
                    add_box(mat, x + 0.1, y, z, x + 0.9, y + 1, z + 0.2)
                elif facing == 'west':
                    add_box(mat, x + 0.8, y, z + 0.1, x + 1, y + 1, z + 0.9)
                else:
                    add_box(mat, x, y, z + 0.1, x + 0.2, y + 1, z + 0.9)
            else:
                add_box(mat, x + 0.42, y, z + 0.42, x + 0.58, y + 1.0, z + 0.58)
                add_box(mat, x + 0.15, y + 0.3, z + 0.35, x + 0.85, y + 1.0, z + 0.65)

    for mat, boxes in groups.items():
        bm = bmesh.new()
        for (x0, y0, z0, x1, y1, z1) in boxes:
            # convert to blender coords: (x, -z, y) => mirror z so north (z-) is away from the camera
            bx0, bx1 = x0, x1
            by0, by1 = -z1, -z0
            bz0, bz1 = y0, y1
            verts = [bm.verts.new(v) for v in (
                (bx0, by0, bz0), (bx1, by0, bz0), (bx1, by1, bz0), (bx0, by1, bz0),
                (bx0, by0, bz1), (bx1, by0, bz1), (bx1, by1, bz1), (bx0, by1, bz1))]
            faces = ((0, 3, 2, 1), (4, 5, 6, 7), (0, 1, 5, 4), (1, 2, 6, 5), (2, 3, 7, 6), (3, 0, 4, 7))
            is_unit = (x1 - x0 == 1 and y1 - y0 == 1 and z1 - z0 == 1)
            for fi, f in enumerate(faces):
                if is_unit:
                    # cull faces hidden by neighbouring full cubes
                    nb = ((x0, y0 - 1, z0), (x0, y0 + 1, z0), (x0, y0, z0 + 1), (x0 + 1, y0, z0), (x0, y0, z0 - 1), (x0 - 1, y0, z0))[fi]
                    if nb in solid:
                        continue
                bm.faces.new([verts[i] for i in f])
        mesh = bpy.data.meshes.new(mat.name)
        bm.to_mesh(mesh)
        bm.free()
        obj = bpy.data.objects.new(mat.name, mesh)
        obj.data.materials.append(mat)
        scene.collection.objects.link(obj)

    # entity markers: a small capsule-ish stack of cubes in a colour
    for (x, y, z, eid, tag) in ents:
        path = eid.partition(':')[2]
        if path == 'tribal_kin':
            col = hex_rgb(TRIBE_COLOURS[TRIBES[int(tag.get('Tribe', 0))]])
            h = 1.9
        elif path == 'hollow_sentinel':
            col = hex_rgb(0x313f53); h = 1.8
        elif path == 'the_unsung':
            col = hex_rgb(0x8a5fc7); h = 4.2
        elif 'minecart' in path:
            col = hex_rgb(0x8c8c8c); h = 0.7
        else:
            col = (0.95, 0.15, 0.85); h = 1.5
        mat = material(col, True)
        bpy.ops.mesh.primitive_cylinder_add(vertices=12, radius=0.28, depth=h, location=(x, -z, y + h / 2))
        ob = bpy.context.active_object
        ob.data.materials.append(mat)
        bpy.ops.mesh.primitive_uv_sphere_add(segments=12, ring_count=8, radius=0.3, location=(x, -z, y + h + 0.1))
        bpy.context.active_object.data.materials.append(material((1, 1, 1), True))

    # floor plane
    bpy.ops.mesh.primitive_plane_add(size=1, location=(W / 2, -D / 2, -0.02))
    floor = bpy.context.active_object
    floor.scale = (W * 12 + 200, D * 12 + 200, 1)
    floor.data.materials.append(material((0.16, 0.17, 0.19)))

    # lighting: warm sun + cool sky
    bpy.ops.object.light_add(type='SUN', location=(W, -D * 2, H + 30))
    sun = bpy.context.active_object
    sun.data.energy = 3.5
    sun.data.angle = math.radians(6)
    sun.rotation_euler = (math.radians(50), math.radians(-18), math.radians(28))
    world = bpy.data.worlds.new('w')
    scene.world = world
    world.use_nodes = True
    bg = world.node_tree.nodes['Background']
    bg.inputs['Color'].default_value = (0.42, 0.5, 0.62, 1)
    bg.inputs['Strength'].default_value = 0.9

    # isometric orthographic camera looking from the south-east, above
    # frame the whole box: isometric projected width ~ (W+D)*0.71, projected height ~ H*0.82 + (W+D)*0.41
    proj_w = (W + D) * 0.72
    proj_h = H * 0.82 + (W + D) * 0.41
    extent = max(proj_w, proj_h * scene.render.resolution_x / max(1, scene.render.resolution_y))
    cam_data = bpy.data.cameras.new('cam')
    cam_data.type = 'ORTHO'
    cam_data.ortho_scale = extent * 1.12
    cam_data.clip_end = 1000
    cam = bpy.data.objects.new('cam', cam_data)
    scene.collection.objects.link(cam)
    cx, cy, cz = W / 2, -D / 2, H * 0.45
    dist = 200
    yaw = math.radians(45)
    pitch = math.radians(35.264)
    cam.location = (cx + dist * math.cos(pitch) * math.sin(yaw), cy - dist * math.cos(pitch) * math.cos(yaw), cz + dist * math.sin(pitch))
    cam.rotation_euler = (math.radians(90) - pitch, 0, yaw)
    scene.camera = cam
    return scene


def render(name, size, blocks, ents, out, samples, res):
    import bpy
    scene = build_scene(size, blocks, ents, res)
    scene.render.resolution_percentage = 100
    scene.cycles.samples = samples
    scene.cycles.use_denoising = True
    scene.render.film_transparent = False
    scene.view_settings.view_transform = 'Filmic' if 'Filmic' in [i.identifier for i in scene.view_settings.bl_rna.properties['view_transform'].enum_items] else 'Standard'
    scene.view_settings.look = 'None'
    scene.render.image_settings.file_format = 'PNG'
    scene.render.filepath = str(out)
    bpy.ops.render.render(write_still=True)


def contact_sheet(entries, out):
    cols = 4
    thumbs = []
    for name, path in entries:
        img = Image.open(path).convert('RGB')
        img.thumbnail((440, 330))
        thumbs.append((name, img))
    cw, ch = 460, 372
    rows = math.ceil(len(thumbs) / cols)
    sheet = Image.new('RGB', (cols * cw + 20, rows * ch + 70), (26, 28, 32))
    draw = ImageDraw.Draw(sheet)
    try:
        font = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', 20)
        small = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 15)
    except OSError:
        font = small = ImageFont.load_default()
    draw.text((20, 18), 'Tribal Power 3.0 — structure templates (isometric review)', fill=(236, 236, 230), font=font)
    for i, (name, img) in enumerate(thumbs):
        r, c = divmod(i, cols)
        x, y = 20 + c * cw, 62 + r * ch
        sheet.paste(img, (x + (440 - img.width) // 2, y + (330 - img.height) // 2))
        draw.rectangle((x, y, x + 440, y + 330), outline=(70, 74, 82))
        draw.text((x + 6, y + 334), name, fill=(220, 224, 214), font=small)
    sheet.save(out)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--samples', type=int, default=24)
    ap.add_argument('--size', default='900x680')
    ap.add_argument('--only', default='')
    args = ap.parse_args()
    res = tuple(int(v) for v in args.size.lower().split('x'))
    OUT.mkdir(parents=True, exist_ok=True)
    only = {s for s in args.only.split(',') if s}
    entries = []
    for path in sorted(STRUCT.glob('*.nbt')):
        name = path.stem
        if name == 'empty' or (only and name not in only):
            continue
        size, blocks, ents = load_template(path)
        print(f'{name}: size {size}, {len(blocks)} blocks, {len(ents)} entities')
        out = OUT / f'{name}.png'
        top = max(b[1] for b in blocks) + 1   # frame the built height, not the cleared air above it
        render(name, [size[0], top, size[2]], blocks, ents, out, args.samples, res)
        entries.append((f'{name}  {size[0]}x{size[1]}x{size[2]}, {len(ents)} ent', out))
        if name in CUTAWAY:
            cut = CUTAWAY[name]
            out2 = OUT / f'{name}_cutaway.png'
            render(name, [size[0], cut + 1, size[2]], [b for b in blocks if b[1] <= cut], ents, out2, args.samples, res)
            entries.append((f'{name} cutaway (y<={cut})', out2))
    if not only:
        contact_sheet(entries, OUT / 'structures-3.0-contact-sheet.png')
        print('sheet:', OUT / 'structures-3.0-contact-sheet.png')


if __name__ == '__main__':
    main()
