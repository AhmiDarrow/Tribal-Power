"""Builds the 3.0 March structure templates (design §2 Camps, §3 The March).

Writes gzip NBT structure templates to data/tribalpower/structure/:
  tribe_camp_<id>.nbt x9   ancestor_hall.nbt   drum_circle.nbt   crystal_spire.nbt
plus the worldgen JSON (structure, template_pool, structure_set, biome tags) and the
Ancestor Hall chest loot table.  Palettes are randomised with a seeded RNG so no two
camps are identical while the output stays reproducible.

Cross-agent NBT contract (pure data, no Java dependency):
  entity tribalpower:tribal_kin  {Tribe:int 0-8, Role:"ELDER|DRUMMER|HUNTER|WEAVER", PersistenceRequired:1b}
  block  tribalpower:tribe_hearth  BE nbt {Tribe:int}
  block  tribalpower:tribe_banner  property tribe=<int>
  block  tribalpower:lore_tablet   BE nbt {Tablet:int 0-11}, property facing
  block  tribalpower:silent_drum

Run: pip install nbtlib && python3 tools/generate_structures.py [project root]
"""
from pathlib import Path
import json
import math
import random
import sys

from nbtlib import File, Compound, List, Int, String, Byte, Double

ROOT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[1]
DATA = ROOT / 'src/main/resources/data/tribalpower'
STRUCT = DATA / 'structure'
DATA_VERSION = 3955  # 1.21.1

TRIBES = ['soil', 'stone', 'sprout', 'claw', 'spark', 'clock', 'swarm', 'sigil', 'spindle']
ATTUNEMENT = {'soil': 'earth', 'stone': 'earth', 'sprout': 'water', 'claw': 'fire', 'spark': 'fire',
              'clock': 'air', 'swarm': 'air', 'sigil': 'spirit', 'spindle': 'loom'}
WOOL = {'soil': 'brown', 'stone': 'light_gray', 'sprout': 'green', 'claw': 'red', 'spark': 'orange',
        'clock': 'blue', 'swarm': 'yellow', 'sigil': 'purple', 'spindle': 'cyan'}
# Overworld placement by biome tag (design §2) + one March biome each.
BIOMES = {
    'soil': ['minecraft:plains', 'minecraft:sunflower_plains', 'tribalpower:march_steppe'],
    'stone': ['#minecraft:is_mountain', 'minecraft:stony_shore', 'tribalpower:march_highlands'],
    'sprout': ['minecraft:forest', 'minecraft:jungle', 'minecraft:sparse_jungle', 'tribalpower:march_steppe'],
    'claw': ['#minecraft:is_taiga', 'tribalpower:march_highlands'],
    'spark': ['#minecraft:is_savanna', 'tribalpower:march_steppe'],
    'clock': ['minecraft:birch_forest', 'minecraft:old_growth_birch_forest', 'tribalpower:march_crystal_fields'],
    'swarm': ['minecraft:flower_forest', 'minecraft:meadow', 'minecraft:cherry_grove', 'tribalpower:march_steppe'],
    'sigil': ['minecraft:dark_forest', 'tribalpower:march_highlands'],
    'spindle': ['tribalpower:march_crystal_fields'],
}
ROLES = ['ELDER', 'DRUMMER', 'HUNTER', 'WEAVER']


class Template:
    """A structure template: sparse block map + entities, serialised in the vanilla format."""

    def __init__(self):
        self.blocks = {}
        self.entities = []

    def set(self, x, y, z, name, props=None, nbt=None):
        self.blocks[(x, y, z)] = (name, tuple(sorted((props or {}).items())), nbt)

    def get(self, x, y, z):
        return self.blocks.get((x, y, z), (None, (), None))[0]

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, name, props)

    def hollow(self, x0, y0, z0, x1, y1, z1, name, props=None):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    edge = x in (x0, x1) or y in (y0, y1) or z in (z0, z1)
                    self.set(x, y, z, name if edge else 'minecraft:air', props if edge else None)

    def entity(self, x, y, z, entity_id, **nbt):
        self.entities.append((x, y, z, entity_id, nbt))

    def kin(self, x, y, z, tribe, role):
        self.entity(x + 0.5, y, z + 0.5, 'tribalpower:tribal_kin',
                    Tribe=Int(tribe), Role=String(role), PersistenceRequired=Byte(1))

    def save(self, name):
        xs = [p[0] for p in self.blocks]; ys = [p[1] for p in self.blocks]; zs = [p[2] for p in self.blocks]
        assert min(xs) >= 0 and min(ys) >= 0 and min(zs) >= 0, name
        size = [max(xs) + 1, max(ys) + 1, max(zs) + 1]
        palette, index = [], {}
        blocks = List[Compound]()
        for (x, y, z), (bname, props, nbt) in sorted(self.blocks.items()):
            key = (bname, props)
            if key not in index:
                index[key] = len(palette)
                entry = Compound({'Name': String(bname)})
                if props:
                    entry['Properties'] = Compound({k: String(str(v)) for k, v in props})
                palette.append(entry)
            block = Compound({'pos': List[Int]([Int(x), Int(y), Int(z)]), 'state': Int(index[key])})
            if nbt:
                block['nbt'] = Compound(nbt)
            blocks.append(block)
        entities = List[Compound]()
        for x, y, z, eid, nbt in self.entities:
            tag = Compound({'id': String(eid)})
            tag.update(nbt)
            entities.append(Compound({
                'pos': List[Double]([Double(x), Double(y), Double(z)]),
                'blockPos': List[Int]([Int(int(math.floor(x))), Int(int(math.floor(y))), Int(int(math.floor(z)))]),
                'nbt': tag,
            }))
        root = Compound({
            'DataVersion': Int(DATA_VERSION),
            'size': List[Int]([Int(v) for v in size]),
            'palette': List[Compound](palette),
            'blocks': blocks,
            'entities': entities,
        })
        STRUCT.mkdir(parents=True, exist_ok=True)
        File(root, gzipped=True).save(STRUCT / f'{name}.nbt', gzipped=True)
        return size


def write_json(rel, data):
    path = DATA / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')


# ---------------------------------------------------------------- camps

def hut(t, rng, x, z, w, d, planks, wool, log, facing):
    """A small hut with plank frame, wool or plank walls, a pitched-flat roof and a door gap."""
    wall = rng.choice([wool, planks, wool])
    t.fill(x, 1, z, x + w - 1, 1, z + d - 1, planks)           # floor
    for (cx, cz) in ((x, z), (x + w - 1, z), (x, z + d - 1), (x + w - 1, z + d - 1)):
        t.fill(cx, 1, cz, cx, 4, cz, log, {'axis': 'y'})
    for y in (2, 3):
        for xx in range(x + 1, x + w - 1):
            t.set(xx, y, z, wall); t.set(xx, y, z + d - 1, wall)
        for zz in range(z + 1, z + d - 1):
            t.set(x, y, zz, wall); t.set(x + w - 1, y, zz, wall)
    t.fill(x + 1, 2, z + 1, x + w - 2, 4, z + d - 2, 'minecraft:air')
    t.fill(x, 4, z, x + w - 1, 4, z + d - 1, planks)            # roof
    t.fill(x + 1, 5, z + 1, x + w - 2, 5, z + d - 2, rng.choice([planks, wool]))
    # door + banner beside it on the facing side
    mx, mz = x + w // 2, z + d // 2
    if facing == 'south':
        t.set(mx, 2, z + d - 1, 'minecraft:air'); t.set(mx, 3, z + d - 1, 'minecraft:air'); bx, bz = mx - 1, z + d
    elif facing == 'north':
        t.set(mx, 2, z, 'minecraft:air'); t.set(mx, 3, z, 'minecraft:air'); bx, bz = mx + 1, z - 1
    elif facing == 'east':
        t.set(x + w - 1, 2, mz, 'minecraft:air'); t.set(x + w - 1, 3, mz, 'minecraft:air'); bx, bz = x + w, mz - 1
    else:
        t.set(x, 2, mz, 'minecraft:air'); t.set(x, 3, mz, 'minecraft:air'); bx, bz = x - 1, mz + 1
    # window
    t.set(x + 1 if facing in ('north', 'south') else x, 3, z if facing == 'south' else (z + d - 1 if facing == 'north' else z + 1),
          'minecraft:glass_pane')
    # a lantern inside and a bed/loom prop
    t.set(mx, 3, mz, 'minecraft:lantern', {'hanging': 'true'})
    return bx, bz


def build_camp(tribe_index, tribe):
    rng = random.Random(0x7A1B + tribe_index * 97)
    t = Template()
    S = 25
    planks = rng.choice(['tribalpower:march_planks', 'minecraft:spruce_planks', 'tribalpower:march_planks'])
    log = rng.choice(['tribalpower:march_log', 'minecraft:spruce_log'])
    wool = f'minecraft:{WOOL[tribe]}_wool'
    march_only = tribe == 'spindle'
    ground = ['tribalpower:march_soil', 'tribalpower:march_grass'] if march_only else ['minecraft:coarse_dirt', 'minecraft:grass_block', 'minecraft:dirt_path']
    # base layer and cleared air above so trees never pierce the huts
    for x in range(S):
        for z in range(S):
            t.set(x, 0, z, rng.choice(ground))
    t.fill(0, 1, 0, S - 1, 7, S - 1, 'minecraft:air')
    cx = cz = S // 2
    # fire pit: campfire in a cobble ring
    ring = 'tribalpower:march_cobble' if march_only else rng.choice(['minecraft:cobblestone', 'tribalpower:march_cobble'])
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            if dx or dz:
                t.set(cx + dx, 0, cz + dz, ring)
    t.set(cx, 1, cz, 'minecraft:campfire', {'lit': 'true', 'facing': 'north', 'signal_fire': 'false', 'waterlogged': 'false'})
    # hearth two blocks north of the fire, totem two south
    t.set(cx, 1, cz - 3, 'tribalpower:tribe_hearth', nbt={'id': String('tribalpower:tribe_hearth'), 'Tribe': Int(tribe_index)})
    t.set(cx, 0, cz - 3, ring)
    t.set(cx, 1, cz + 3, f'tribalpower:resonance_totem_{ATTUNEMENT[tribe]}')
    t.set(cx, 0, cz + 3, ring)
    # seating logs around the fire
    for (lx, lz, axis) in ((cx - 3, cz, 'z'), (cx + 3, cz, 'z')):
        t.fill(lx, 1, lz - 1, lx, 1, lz + 1, log, {'axis': axis})
    # huts at the corners (3 or 4)
    corners = [(1, 1, 'south'), (S - 8, 1, 'south'), (1, S - 8, 'north'), (S - 8, S - 8, 'north')]
    rng.shuffle(corners)
    count = 3 if rng.random() < 0.5 else 4
    banners = []
    for (hx, hz, facing) in corners[:count]:
        w = rng.choice([5, 6, 7]); d = rng.choice([5, 6, 7])
        banners.append(hut(t, rng, hx, hz, w, d, planks, wool, log, facing))
    # tribe banners beside doors and one at the fire
    for (bx, bz) in banners[:3]:
        if 0 <= bx < S and 0 <= bz < S and t.get(bx, 1, bz) in (None, 'minecraft:air'):
            t.set(bx, 1, bz, 'tribalpower:tribe_banner', {'tribe': tribe_index})
    t.set(cx + 2, 1, cz - 2, 'tribalpower:tribe_banner', {'tribe': tribe_index})
    # drying racks / a loom for the weaver and a little garden
    t.set(cx - 2, 1, cz + 2, 'minecraft:loom', {'facing': 'north'})
    for i in range(rng.randint(2, 4)):
        gx, gz = rng.randint(2, S - 3), rng.randint(2, S - 3)
        if t.get(gx, 1, gz) == 'minecraft:air':
            t.set(gx, 1, gz, rng.choice(['minecraft:hay_block', 'minecraft:barrel', 'minecraft:composter', wool]))
    # spirit lanterns on posts at the camp edge
    for (px, pz) in ((2, cz), (S - 3, cz), (cx, 2), (cx, S - 3)):
        if t.get(px, 1, pz) == 'minecraft:air':
            t.set(px, 1, pz, 'minecraft:spruce_fence', {'north': 'false', 'south': 'false', 'east': 'false', 'west': 'false', 'waterlogged': 'false'})
            t.set(px, 2, pz, 'tribalpower:spirit_lantern', {'lit': 'true'})
    # four Kin around the fire
    t.kin(cx - 1, 1, cz - 1, tribe_index, 'ELDER')
    t.kin(cx + 2, 1, cz + 1, tribe_index, 'DRUMMER')
    t.kin(cx - 2, 1, cz + 1, tribe_index, 'HUNTER')
    t.kin(cx - 2, 1, cz + 3, tribe_index, 'WEAVER')
    return t.save(f'tribe_camp_{tribe}')


# ---------------------------------------------------------------- ancestor hall

def build_hall():
    rng = random.Random(0xA11)
    t = Template()
    W, H, D = 33, 11, 23
    stone = 'tribalpower:march_stone'
    cobble = 'tribalpower:march_cobble'
    beam = 'tribalpower:march_log'
    # three rooms in a row, each hollow, joined by doorways
    rooms = [(0, 0, 0, 10, 8, 14), (11, 0, 4, 21, 6, 18), (22, 0, 0, 32, 8, 14)]
    for (x0, y0, z0, x1, y1, z1) in rooms:
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    edge = x in (x0, x1) or y in (y0, y1) or z in (z0, z1)
                    if edge:
                        t.set(x, y, z, rng.choice([stone, stone, stone, cobble, 'minecraft:mossy_cobblestone'] if y < y1 else [stone, cobble]))
                    else:
                        t.set(x, y, z, 'minecraft:air')
        # Loom-wood beams along the ceiling and corner pillars
        for x in range(x0 + 1, x1, 3):
            t.fill(x, y1 - 1, z0 + 1, x, y1 - 1, z1 - 1, beam, {'axis': 'z'})
        for (px, pz) in ((x0 + 1, z0 + 1), (x1 - 1, z0 + 1), (x0 + 1, z1 - 1), (x1 - 1, z1 - 1)):
            t.fill(px, y0 + 1, pz, px, y1 - 1, pz, beam, {'axis': 'y'})
    # doorways between rooms
    for x in (10, 11, 21, 22):
        t.fill(x, 1, 9, x, 3, 10, 'minecraft:air')
    # sunken entrance shaft from the middle room roof up to the surface (roof at y=6, surface ~ y=8-10)
    t.fill(15, 6, 10, 17, H - 1, 12, 'minecraft:air')
    for y in range(7, H):
        for (x, z) in ((14, 10), (14, 11), (14, 12), (18, 10), (18, 11), (18, 12), (15, 9), (16, 9), (17, 9), (15, 13), (16, 13), (17, 13)):
            t.set(x, y, z, cobble if rng.random() < 0.7 else 'minecraft:mossy_cobblestone')
    t.fill(15, 1, 10, 17, 5, 12, 'minecraft:air')
    for y in range(1, H - 1):
        t.set(16, y, 11, 'minecraft:ladder', {'facing': 'north', 'waterlogged': 'false'})
        t.set(16, y, 12, stone)
    # lore tablets: four on the walls, facing into the rooms
    tablets = [(1, 3, 7, 'east', 2), (31, 3, 7, 'west', 9), (16, 3, 5, 'south', 10), (5, 3, 13, 'north', 11)]
    for (x, y, z, facing, tablet) in tablets:
        t.set(x, y, z, 'tribalpower:lore_tablet', {'facing': facing},
              nbt={'id': String('tribalpower:lore_tablet'), 'Tablet': Int(tablet)})
    # chests with loot in the side rooms
    for (x, z, facing) in ((3, 2, 'south'), (29, 12, 'north')):
        t.set(x, 1, z, 'minecraft:chest', {'facing': facing, 'type': 'single', 'waterlogged': 'false'},
              nbt={'id': String('minecraft:chest'), 'LootTable': String('tribalpower:chests/ancestor_hall'),
                   'LootTableSeed': Int(rng.randint(1, 1 << 30))})
    # lanterns and a dais with the Loom totem in the middle room
    for (x, z) in ((3, 7), (7, 7), (25, 7), (29, 7), (13, 11), (19, 11)):
        t.set(x, 1, z, 'tribalpower:spirit_lantern', {'lit': 'true'})
    t.set(16, 1, 15, 'tribalpower:resonance_totem_loom')
    for dx in (-1, 0, 1):
        t.set(16 + dx, 0, 15, 'minecraft:chiseled_stone_bricks')
    # rubble
    for _ in range(18):
        x, z = rng.randint(1, W - 2), rng.randint(1, D - 2)
        if t.get(x, 1, z) == 'minecraft:air':
            t.set(x, 1, z, rng.choice(['minecraft:cobblestone_slab', 'minecraft:mossy_cobblestone', 'minecraft:gravel']))
    # hollow sentinels keep the side rooms
    t.entity(5.5, 1, 4.5, 'tribalpower:hollow_sentinel', PersistenceRequired=Byte(1))
    t.entity(27.5, 1, 10.5, 'tribalpower:hollow_sentinel', PersistenceRequired=Byte(1))
    return t.save('ancestor_hall')


# ---------------------------------------------------------------- drum circle

def build_drum_circle():
    rng = random.Random(0xD2C)
    t = Template()
    S = 31
    c = S // 2
    stone = 'tribalpower:march_stone'
    cobble = 'tribalpower:march_cobble'
    # a paved disc
    for x in range(S):
        for z in range(S):
            r = math.hypot(x - c, z - c)
            if r <= 14.5:
                t.set(x, 0, z, rng.choice([stone, stone, cobble, 'minecraft:stone_bricks', 'minecraft:cracked_stone_bricks']))
    t.fill(0, 1, 0, S - 1, 8, S - 1, 'minecraft:air')
    # twelve pillars with Spirit Lanterns on top
    for i in range(12):
        a = i * math.tau / 12
        px, pz = round(c + 12 * math.cos(a)), round(c + 12 * math.sin(a))
        h = rng.choice([4, 5, 6])
        t.fill(px, 1, pz, px, h, pz, rng.choice(['minecraft:stone_bricks', stone]))
        t.set(px, h + 1, pz, 'tribalpower:spirit_lantern', {'lit': 'true'})
    # seating stones ring
    for i in range(16):
        a = i * math.tau / 16 + 0.1
        sx, sz = round(c + 7 * math.cos(a)), round(c + 7 * math.sin(a))
        t.set(sx, 1, sz, rng.choice(['minecraft:stone_brick_slab', 'minecraft:cobblestone_slab', 'minecraft:polished_andesite_slab']), {'type': 'bottom', 'waterlogged': 'false'})
    # the silent drum on a dais
    t.fill(c - 1, 0, c - 1, c + 1, 0, c + 1, 'minecraft:chiseled_stone_bricks')
    t.set(c, 1, c, 'tribalpower:silent_drum', nbt={'id': String('tribalpower:silent_drum')})
    # a lore tablet on a stele
    t.fill(c + 3, 1, c - 9, c + 3, 3, c - 9, stone)
    t.set(c + 3, 2, c - 8, 'tribalpower:lore_tablet', {'facing': 'south'},
          nbt={'id': String('tribalpower:lore_tablet'), 'Tablet': Int(4)})
    return t.save('drum_circle')


# ---------------------------------------------------------------- crystal spire

def build_spire():
    rng = random.Random(0x5B1E)
    t = Template()
    S = 17
    c = S // 2
    crystal = 'tribalpower:march_crystal'
    stone = 'tribalpower:march_stone'
    for x in range(S):
        for z in range(S):
            t.set(x, 0, z, rng.choice(['tribalpower:march_soil', 'tribalpower:march_grass', stone]))
    t.fill(0, 1, 0, S - 1, 9, S - 1, 'minecraft:air')
    # a tapering spire of March crystal
    for y in range(1, 41):
        r = max(0.6, 3.6 - y * 0.08)
        for x in range(S):
            for z in range(S):
                if math.hypot(x - c, z - c) <= r:
                    t.set(x, y, z, crystal if rng.random() < 0.85 else stone)
    # crystal shards leaning out from the spire
    for i in range(6):
        a = i * math.tau / 6
        sx, sz = round(c + 3 * math.cos(a)), round(c + 3 * math.sin(a))
        t.fill(sx, 1, sz, sx, rng.randint(2, 4), sz, crystal)
    # Loom-stitcher waystation: a small open shelter at the base
    planks = 'tribalpower:march_planks'
    log = 'tribalpower:march_log'
    x0, z0 = 1, 1
    for (px, pz) in ((x0, z0), (x0 + 5, z0), (x0, z0 + 5), (x0 + 5, z0 + 5)):
        t.fill(px, 1, pz, px, 3, pz, log, {'axis': 'y'})
    t.fill(x0, 4, z0, x0 + 5, 4, z0 + 5, planks)
    t.fill(x0 + 1, 1, z0 + 1, x0 + 4, 1, z0 + 4, planks)
    t.set(x0 + 2, 2, z0 + 2, 'tribalpower:tribe_hearth', nbt={'id': String('tribalpower:tribe_hearth'), 'Tribe': Int(8)})
    t.set(x0 + 4, 2, z0 + 4, 'tribalpower:tribe_banner', {'tribe': 8})
    t.set(x0 + 1, 2, z0 + 4, 'minecraft:loom', {'facing': 'east'})
    t.set(x0 + 4, 2, z0 + 1, 'minecraft:cyan_wool')
    t.set(x0 + 2, 3, z0 + 3, 'minecraft:lantern', {'hanging': 'true'})
    t.set(S - 3, 1, S - 3, 'tribalpower:resonance_totem_loom')
    t.set(S - 3, 1, 2, 'tribalpower:spirit_lantern', {'lit': 'true'})
    t.set(2, 1, S - 3, 'tribalpower:spirit_lantern', {'lit': 'true'})
    t.set(S - 2, 2, c, 'tribalpower:lore_tablet', {'facing': 'west'},
          nbt={'id': String('tribalpower:lore_tablet'), 'Tablet': Int(8)})
    t.set(S - 1, 1, c, stone); t.set(S - 1, 2, c, stone); t.set(S - 1, 3, c, stone)
    t.kin(x0 + 3, 2, z0 + 2, 8, 'ELDER')
    return t.save('crystal_spire')


# ---------------------------------------------------------------- worldgen json

def structure_json(name, biome_tag, adaptation, start_height, step='surface_structures', project=True):
    data = {
        'type': 'minecraft:jigsaw',
        'biomes': f'#tribalpower:has_structure/{biome_tag}',
        'step': step,
        'spawn_overrides': {},
        'terrain_adaptation': adaptation,
        'start_pool': f'tribalpower:{name}',
        'size': 1,
        'start_height': {'absolute': start_height},
        'max_distance_from_center': 80,
        'use_expansion_hack': False,
    }
    if project:
        data['project_start_to_heightmap'] = 'WORLD_SURFACE_WG'
    write_json(f'worldgen/structure/{name}.json', data)
    write_json(f'worldgen/template_pool/{name}.json', {
        'fallback': 'minecraft:empty',
        'elements': [{
            'weight': 1,
            'element': {
                'element_type': 'minecraft:single_pool_element',
                'location': f'tribalpower:{name}',
                'processors': 'minecraft:empty',
                'projection': 'rigid',
            },
        }],
    })


def structure_set(name, spacing, separation, salt):
    write_json(f'worldgen/structure_set/{name}.json', {
        'structures': [{'structure': f'tribalpower:{name}', 'weight': 1}],
        'placement': {'type': 'minecraft:random_spread', 'spacing': spacing, 'separation': separation, 'salt': salt},
    })


def biome_tag(name, values):
    write_json(f'tags/worldgen/biome/has_structure/{name}.json', {'replace': False, 'values': values})


def loot_table():
    def item(name, lo, hi, weight=1):
        return {'type': 'minecraft:item', 'name': name, 'weight': weight,
                'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]}
    write_json('loot_table/chests/ancestor_hall.json', {
        'type': 'minecraft:chest',
        'pools': [
            {'rolls': 1, 'entries': [item('tribalpower:loom_thread', 2, 4)]},
            {'rolls': {'type': 'minecraft:uniform', 'min': 2, 'max': 4}, 'entries': [
                item('tribalpower:bound_echo', 1, 3, 4),
                item('tribalpower:attuned_echo', 2, 5, 6),
                item('tribalpower:spiritweave', 1, 3, 4),
                item('tribalpower:echo_shard', 3, 8, 6),
                item('tribalpower:blank_seal', 1, 1, 3),
                item('tribalpower:spirit_shard', 1, 3, 4),
                item('tribalpower:march_crystal', 1, 4, 3),
            ]},
            {'rolls': {'type': 'minecraft:uniform', 'min': 0, 'max': 2}, 'entries': [
                item('tribalpower:earth_seal', 1, 1, 1), item('tribalpower:fire_seal', 1, 1, 1),
                item('tribalpower:water_seal', 1, 1, 1), item('tribalpower:air_seal', 1, 1, 1),
                item('tribalpower:spirit_seal', 1, 1, 1), item('minecraft:bone', 2, 6, 5),
                item('minecraft:candle', 1, 3, 4),
            ]},
        ],
    })


def main():
    sizes = {}
    for i, tribe in enumerate(TRIBES):
        sizes[f'tribe_camp_{tribe}'] = build_camp(i, tribe)
        structure_json(f'tribe_camp_{tribe}', f'tribe_camp_{tribe}', 'beard_thin', -1)
        structure_set(f'tribe_camp_{tribe}', 40, 24, 0x7A1B00 + i * 7919)
        biome_tag(f'tribe_camp_{tribe}', BIOMES[tribe])
    sizes['ancestor_hall'] = build_hall()
    structure_json('ancestor_hall', 'ancestor_hall', 'bury', -8, step='underground_structures')
    structure_set('ancestor_hall', 48, 28, 0xA11CE5)
    biome_tag('ancestor_hall', ['tribalpower:march_steppe', 'tribalpower:march_highlands'])
    sizes['drum_circle'] = build_drum_circle()
    structure_json('drum_circle', 'drum_circle', 'beard_thin', -1)
    structure_set('drum_circle', 64, 40, 0xD2C1E7)
    biome_tag('drum_circle', ['tribalpower:march_highlands'])
    sizes['crystal_spire'] = build_spire()
    structure_json('crystal_spire', 'crystal_spire', 'beard_thin', -1)
    structure_set('crystal_spire', 56, 32, 0x5B1E93)
    biome_tag('crystal_spire', ['tribalpower:march_crystal_fields'])
    loot_table()
    for name, size in sizes.items():
        print(f'{name}: {size}')


if __name__ == '__main__':
    main()
