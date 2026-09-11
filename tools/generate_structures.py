"""Builds the 3.0 March structure templates (design §2 Camps, §3 The March).

Writes gzip NBT structure templates to data/tribalpower/structure/:
  tribe_camp_<id>.nbt x9   ancestor_hall.nbt   drum_circle.nbt   crystal_spire.nbt
plus the worldgen JSON (structure, template_pool, structure_set, biome tags) and the
Ancestor Hall chest loot table.  Palettes are randomised with a seeded RNG so no two
camps are identical while the output stays reproducible.

Cross-agent NBT contract (pure data, no Java dependency):
  entity tribalpower:tribal_kin  {Tribe:int 0-8, Role:"ELDER|DRUMMER|HUNTER|WEAVER", PersistenceRequired:1b}
  block  tribalpower:tribe_hearth  BE nbt {Tribe:int}
  block  tribalpower:tribe_banner  properties tribe=<int> facing=<nsew> wall=<bool>
  block  tribalpower:lore_tablet   BE nbt {Tablet:int 0-11}, property facing (points away from the wall)
  block  tribalpower:silent_drum

Every template is validated before it is written (see Validator): block ids must exist
(vanilla table in tools/vanilla_blockstates_1_21_1.json, mod blocks listed in MOD_BLOCKS),
property names/values must be legal, wall-mounted blocks need a sturdy block behind them,
and Kin must stand on solid ground inside the footprint with two air blocks above.

Camp frame (all camps): template y=0 is sub-soil, y=1 the surface block, air from y=2.
Jigsaw placement puts the piece's minY at start_height + heightmap - 1 (rigid pieces have a
ground-level delta of 1) and WORLD_SURFACE_WG is the first air block, so start_height -1
lands template y=1 exactly on the old surface block.  Everything above is explicit air, so
trees/terrain never pierce the camp.  The hall uses -11: its layer 11 is the surface.

Run: pip install nbtlib && python3 tools/generate_structures.py [project root]
"""
from pathlib import Path
import gzip
import io
import json
import math
import random
import sys

from nbtlib import File, Compound, List, Int, Long, String, Byte, Double

ROOT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[1]
DATA = ROOT / 'src/main/resources/data/tribalpower'
STRUCT = DATA / 'structure'
VANILLA_TABLE = Path(__file__).resolve().parent / 'vanilla_blockstates_1_21_1.json'
DATA_VERSION = 3955  # 1.21.1

TRIBES = ['soil', 'stone', 'sprout', 'claw', 'spark', 'clock', 'swarm', 'sigil', 'spindle']
ATTUNEMENT = {'soil': 'earth', 'stone': 'earth', 'sprout': 'water', 'claw': 'fire', 'spark': 'fire',
              'clock': 'air', 'swarm': 'air', 'sigil': 'spirit', 'spindle': 'loom'}
WOOL = {'soil': 'brown', 'stone': 'light_gray', 'sprout': 'green', 'claw': 'red', 'spark': 'orange',
        'clock': 'blue', 'swarm': 'yellow', 'sigil': 'purple', 'spindle': 'cyan'}
# Overworld placement by biome tag (design §2) + one March biome each.
BIOMES = {
    'soil': ['minecraft:plains', 'minecraft:sunflower_plains', 'tribalpower:march_steppe'],
    'stone': ['#minecraft:is_mountain', 'minecraft:stony_shore', 'minecraft:stony_peaks', 'tribalpower:march_highlands'],
    'sprout': ['minecraft:forest', 'minecraft:jungle', 'minecraft:sparse_jungle', 'minecraft:bamboo_jungle', 'tribalpower:march_steppe'],
    'claw': ['#minecraft:is_taiga', 'tribalpower:march_highlands'],
    'spark': ['#minecraft:is_savanna', 'tribalpower:march_steppe'],
    'clock': ['minecraft:birch_forest', 'minecraft:old_growth_birch_forest', 'tribalpower:march_crystal_fields'],
    'swarm': ['minecraft:flower_forest', 'minecraft:meadow', 'minecraft:cherry_grove', 'tribalpower:march_steppe'],
    'sigil': ['minecraft:dark_forest', 'tribalpower:march_highlands'],
    'spindle': ['tribalpower:march_crystal_fields'],
}
ROLES = ['ELDER', 'DRUMMER', 'HUNTER', 'WEAVER']

# Mod blocks the templates may use -> their block state properties (mirrors the Java classes).
NSEW = ['north', 'south', 'east', 'west']
BOOL = ['true', 'false']
MOD_BLOCKS = {
    'tribe_hearth': {}, 'tribe_banner': {'tribe': [str(i) for i in range(9)], 'facing': NSEW, 'wall': BOOL},
    'kinship_totem': {'tribe': [str(i) for i in range(9)]},
    'lore_tablet': {'facing': NSEW}, 'silent_drum': {}, 'drumheart': {},
    'spirit_lantern': {'lit': BOOL}, 'wayanchor': {'lit': BOOL}, 'hush_totem': {'lit': BOOL}, 'rain_chime': {'lit': BOOL},
    'offering_table': {'lit': BOOL}, 'grove_tender': {'lit': BOOL}, 'summoning_cradle': {'lit': BOOL},
    'resonance_totem_earth': {}, 'resonance_totem_fire': {}, 'resonance_totem_water': {}, 'resonance_totem_air': {},
    'resonance_totem_spirit': {}, 'resonance_totem_loom': {},
    'march_stone': {}, 'march_cobble': {}, 'march_soil': {}, 'march_grass': {}, 'march_moss': {}, 'march_log': {},
    'march_planks': {}, 'march_ore': {}, 'march_crystal': {}, 'march_leaf': {}, 'spirit_reed': {},
    'march_leaves': {'distance': [str(i) for i in range(1, 8)], 'persistent': BOOL, 'waterlogged': BOOL},
    'song_bench': {}, 'spirit_light': {},
    # 3.1: the camp props. Each tribe that keeps a voice shows its own craft working.
    'stone_font': {}, 'resonance_mesh': {}, 'anchor_stone': {}, 'ritual_mark': {}, 'pulse_cairn': {},
    'ember_horn': {'lit': BOOL}, 'wind_harp': {'lit': BOOL}, 'wave_drum': {'lit': BOOL},
    'wake_bell': {'lit': BOOL}, 'loom_anchor': {'lit': BOOL},
    'gate_frame': {}, 'gate_keystone': {}, 'ancestral_cache': {}, 'spirit_cistern': {},
}
MOD_ENTITIES = {'tribal_kin', 'the_unsung', 'hollow_sentinel', 'echo_weaver', 'march_walker', 'spirit_wisp', 'dawn_stag',
                'lantern_fox', 'mossback', 'ashbound', 'rootbound', 'reed_stalker', 'shardback', 'storm_moth',
                'cinder_imp', 'mourning_bell', 'rift_hound'}
# support checks: mod blocks that are full cubes, and vanilla blocks that are NOT (by exact name / suffix)
SOLID_MOD = {'march_stone', 'march_cobble', 'march_soil', 'march_grass', 'march_moss', 'march_log', 'march_planks',
             'march_ore', 'tribe_hearth', 'drumheart', 'silent_drum', 'kinship_totem', 'march_leaves',
             'anchor_stone', 'gate_frame', 'gate_keystone', 'ancestral_cache', 'spirit_cistern'}
NON_SOLID_MOD = {'ritual_mark'}
NON_SOLID_EXACT = {
    'air', 'cave_air', 'water', 'lava', 'fire', 'soul_fire', 'campfire', 'soul_campfire', 'lantern', 'soul_lantern',
    'torch', 'wall_torch', 'ladder', 'chain', 'rail', 'powered_rail', 'detector_rail', 'activator_rail', 'cobweb',
    'vine', 'glow_lichen', 'snow', 'short_grass', 'tall_grass', 'fern', 'large_fern', 'dead_bush', 'dandelion', 'poppy',
    'blue_orchid', 'allium', 'azure_bluet', 'oxeye_daisy', 'cornflower', 'lily_of_the_valley', 'sunflower', 'lilac',
    'rose_bush', 'peony', 'torchflower', 'wither_rose', 'brown_mushroom', 'red_mushroom', 'azalea', 'flowering_azalea',
    'sweet_berry_bush', 'wheat', 'carrots', 'potatoes', 'beetroots', 'lever', 'tripwire', 'tripwire_hook', 'scaffolding',
    'lightning_rod', 'end_rod', 'flower_pot', 'decorated_pot', 'skeleton_skull', 'iron_bars', 'glass_pane', 'anvil',
    'chipped_anvil', 'damaged_anvil', 'grindstone', 'bell', 'cauldron', 'lectern', 'stonecutter', 'enchanting_table',
    'daylight_detector', 'repeater', 'comparator', 'hopper', 'composter', 'moss_carpet', 'pink_petals', 'small_dripleaf',
    'big_dripleaf', 'bamboo', 'sugar_cane', 'kelp', 'seagrass', 'spore_blossom', 'candle', 'sea_pickle', 'turtle_egg',
    'honey_block', 'chest', 'trapped_chest', 'ender_chest', 'farmland', 'dirt_path', 'bee_nest', 'beehive', 'brewing_stand',
}
NON_SOLID_SUFFIX = ('_fence', '_fence_gate', '_pane', '_wall', '_slab', '_stairs', '_door', '_trapdoor', '_carpet',
                    '_sapling', '_bed', '_candle', '_button', '_pressure_plate', '_sign', '_banner', '_skull', '_head',
                    '_tulip', '_leaves', '_coral', '_coral_fan', '_wall_sign', '_hanging_sign', '_shulker_box',
                    '_glazed_terracotta', '_minecart')


def _load_vanilla():
    if not VANILLA_TABLE.exists():
        print(f'warning: {VANILLA_TABLE} missing, vanilla ids are not validated', file=sys.stderr)
        return None
    return json.loads(VANILLA_TABLE.read_text())


VANILLA = _load_vanilla()


class Validator:
    """Checks a template against the known block/entity registries and physical common sense."""

    def __init__(self, name, template):
        self.name = name
        self.t = template
        self.errors = []

    def err(self, msg):
        self.errors.append(f'{self.name}: {msg}')

    def solid(self, x, y, z):
        entry = self.t.blocks.get((x, y, z))
        if entry is None:
            return False
        bname = entry[0]
        ns, _, path = bname.partition(':')
        if bname == 'minecraft:air':
            return False
        if ns == 'tribalpower':
            return path in SOLID_MOD
        return path not in NON_SOLID_EXACT and not path.endswith(NON_SOLID_SUFFIX) and not path.startswith('potted_')

    def standable(self, x, y, z):
        """Entities can stand here even if the top face is not sturdy enough for wall/floor attachments."""
        if self.solid(x, y, z):
            return True
        entry = self.t.blocks.get((x, y, z))
        if entry is None:
            return False
        path = entry[0].partition(':')[2]
        return path in ('dirt_path', 'farmland', 'honey_block', 'chest', 'composter', 'bee_nest', 'beehive') \
            or path.endswith(('_slab', '_stairs', '_leaves'))

    def air(self, x, y, z):
        return self.t.blocks.get((x, y, z), ('minecraft:air',))[0] == 'minecraft:air'

    def check_block(self, pos, bname, props):
        ns, _, path = bname.partition(':')
        if ns == 'tribalpower':
            if path not in MOD_BLOCKS:
                self.err(f'unknown mod block {bname} at {pos}'); return
            table = MOD_BLOCKS[path]
        elif ns == 'minecraft':
            if VANILLA is None:
                return
            if bname not in VANILLA['blocks']:
                self.err(f'unknown vanilla block {bname} at {pos}'); return
            table = VANILLA['blocks'][bname]
        else:
            self.err(f'unknown namespace {bname} at {pos}'); return
        for k, v in props:
            if k not in table:
                self.err(f'{bname} has no property {k!r} at {pos}')
            elif str(v) not in table[k]:
                self.err(f'{bname} property {k}={v!r} invalid at {pos} (valid: {table[k]})')
        x, y, z = pos
        # attachment sanity for the wall-mounted blocks we use
        opposite = {'north': (0, 1), 'south': (0, -1), 'east': (-1, 0), 'west': (1, 0)}
        p = dict(props)
        if (path == 'lore_tablet' or (path == 'tribe_banner' and p.get('wall') == 'true')
                or path == 'ladder' or path == 'wall_torch' or path.endswith('_wall_sign')):
            f = p.get('facing', 'north')
            dx, dz = opposite[f]
            if not self.solid(x + dx, y, z + dz):
                self.err(f'{bname} at {pos} facing {f} has nothing sturdy behind it')
        if path == 'tribe_banner' and p.get('wall') != 'true' and not self.solid(x, y - 1, z):
            self.err(f'floor banner at {pos} has no sturdy block below')
        if path in ('lantern', 'soul_lantern') and p.get('hanging') == 'true' and self.air(x, y + 1, z):
            self.err(f'hanging lantern at {pos} has nothing above')
        if path in ('lantern', 'soul_lantern', 'campfire', 'torch') and p.get('hanging') != 'true' and not self.solid(x, y - 1, z) \
                and not self.t.blocks.get((x, y - 1, z), ('minecraft:air',))[0].endswith(('_slab', '_wall', '_fence', 'crystal')):
            self.err(f'{bname} at {pos} has nothing below')
        if path.endswith('_bed'):
            f = p.get('facing', 'north'); dx, dz = {'north': (0, -1), 'south': (0, 1), 'east': (1, 0), 'west': (-1, 0)}[f]
            other = (x + dx, y, z + dz) if p.get('part') == 'foot' else (x - dx, y, z - dz)
            o = self.t.blocks.get(other)
            if not o or o[0] != bname:
                self.err(f'bed half at {pos} is missing its partner at {other}')

    def check_entity(self, x, y, z, eid, nbt):
        ns, _, path = eid.partition(':')
        if ns == 'tribalpower':
            if path not in MOD_ENTITIES:
                self.err(f'unknown mod entity {eid}')
        elif VANILLA is not None and eid not in VANILLA['entities']:
            self.err(f'unknown vanilla entity {eid}')
        bx, by, bz = int(math.floor(x)), int(math.floor(y)), int(math.floor(z))
        if path in ('tribal_kin', 'hollow_sentinel', 'the_unsung') or ns == 'minecraft':
            if not self.standable(bx, by - 1, bz):
                self.err(f'{eid} at {(x, y, z)} is not standing on a solid block')
            for dy in (0, 1):
                inside = self.solid(bx, by + dy, bz) if ns == 'minecraft' else not self.air(bx, by + dy, bz)
                if inside:
                    self.err(f'{eid} at {(x, y, z)} is inside {self.t.blocks.get((bx, by + dy, bz))} at +{dy}')
            if path == 'the_unsung':
                for dy in (2, 3):
                    if not self.air(bx, by + dy, bz):
                        self.err(f'the_unsung needs 4 air blocks at {(x, y, z)}')
        if path == 'tribal_kin':
            if 'Tribe' not in nbt or 'Role' not in nbt:
                self.err('tribal_kin without Tribe/Role')

    def run(self):
        for pos, (bname, props, _) in self.t.blocks.items():
            self.check_block(pos, bname, props)
        for (x, y, z, eid, nbt) in self.t.entities:
            self.check_entity(x, y, z, eid, nbt)
        return self.errors


class Template:
    """A structure template: sparse block map + entities, serialised in the vanilla format."""

    def __init__(self):
        self.blocks = {}
        self.entities = []

    def set(self, x, y, z, name, props=None, nbt=None):
        self.blocks[(x, y, z)] = (name, tuple(sorted((props or {}).items())), nbt)

    def get(self, x, y, z):
        return self.blocks.get((x, y, z), (None, (), None))[0]

    def is_air(self, x, y, z):
        return self.get(x, y, z) == 'minecraft:air'

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
        errors = Validator(name, self).run()
        if errors:
            for e in errors:
                print('ERROR', e, file=sys.stderr)
            raise SystemExit(f'{name}: {len(errors)} validation error(s)')
        xs = [p[0] for p in self.blocks]; ys = [p[1] for p in self.blocks]; zs = [p[2] for p in self.blocks]
        assert min(xs) >= 0 and min(ys) >= 0 and min(zs) >= 0, name
        size = [max(xs) + 1, max(ys) + 1, max(zs) + 1]
        assert max(size) <= 48, f'{name}: {size} exceeds the 48 block jigsaw piece limit'
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
        # gzip with a fixed mtime so a rerun produces byte-identical files (keeps `git status` clean)
        raw = io.BytesIO()
        File(root).write(raw)
        with open(STRUCT / f'{name}.nbt', 'wb') as out:
            with gzip.GzipFile(fileobj=out, mode='wb', mtime=0) as gz:
                gz.write(raw.getvalue())
        return size


def write_json(rel, data):
    path = DATA / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')


# ---------------------------------------------------------------- block helpers

AXIS = {'x': 'x', 'y': 'y', 'z': 'z'}


def log_props(name, axis='y'):
    """Vanilla logs have an axis; tribalpower:march_log is a plain block with no properties."""
    return {} if name.startswith('tribalpower:') else {'axis': axis}


def log(name, axis='y'):
    return name, log_props(name, axis)


def fence(name='minecraft:spruce_fence', n=False, s=False, e=False, w=False):
    return name, {'north': str(n).lower(), 'south': str(s).lower(), 'east': str(e).lower(), 'west': str(w).lower(),
                  'waterlogged': 'false'}


def pane(n=False, s=False, e=False, w=False):
    return 'minecraft:glass_pane', {'north': str(n).lower(), 'south': str(s).lower(), 'east': str(e).lower(),
                                    'west': str(w).lower(), 'waterlogged': 'false'}


def slab(name, top=False):
    return name, {'type': 'top' if top else 'bottom', 'waterlogged': 'false'}


def stairs(name, facing, top=False):
    return name, {'facing': facing, 'half': 'top' if top else 'bottom', 'shape': 'straight', 'waterlogged': 'false'}


def leaves(name='tribalpower:march_leaves'):
    return name, {'persistent': 'true', 'distance': '7', 'waterlogged': 'false'}


def lantern(hanging=False):
    return 'minecraft:lantern', {'hanging': str(hanging).lower(), 'waterlogged': 'false'}


def campfire(facing='north', lit=True):
    return 'minecraft:campfire', {'facing': facing, 'lit': str(lit).lower(), 'signal_fire': 'false', 'waterlogged': 'false'}


def spirit_lantern():
    return 'tribalpower:spirit_lantern', {'lit': 'true'}


def banner_floor(tribe, facing='south'):
    return 'tribalpower:tribe_banner', {'tribe': str(tribe), 'facing': facing, 'wall': 'false'}


def banner_wall(tribe, facing):
    return 'tribalpower:tribe_banner', {'tribe': str(tribe), 'facing': facing, 'wall': 'true'}


def hearth_block(tribe):
    return 'tribalpower:tribe_hearth', {}, {'id': String('tribalpower:tribe_hearth'), 'Tribe': Int(tribe)}


def tablet(facing, index):
    return 'tribalpower:lore_tablet', {'facing': facing}, {'id': String('tribalpower:lore_tablet'), 'Tablet': Int(index)}


def loot_chest(facing, rng, table='tribalpower:chests/ancestor_hall'):
    return 'minecraft:chest', {'facing': facing, 'type': 'single', 'waterlogged': 'false'}, \
        {'id': String('minecraft:chest'), 'LootTable': String(table), 'LootTableSeed': Long(rng.randint(1, 1 << 30))}


DIRS = {'north': (0, -1), 'south': (0, 1), 'east': (1, 0), 'west': (-1, 0)}
OPPOSITE = {'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east'}


def place(t, x, y, z, spec):
    """spec = (name, props) or (name, props, nbt)"""
    if len(spec) == 3:
        t.set(x, y, z, spec[0], spec[1], spec[2])
    else:
        t.set(x, y, z, spec[0], spec[1])


def bed(t, x, y, z, colour, facing):
    dx, dz = DIRS[facing]
    t.set(x, y, z, f'minecraft:{colour}_bed', {'facing': facing, 'part': 'foot', 'occupied': 'false'})
    t.set(x + dx, y, z + dz, f'minecraft:{colour}_bed', {'facing': facing, 'part': 'head', 'occupied': 'false'})


def door(t, x, y, z, facing, wood='spruce'):
    for half in ('lower', 'upper'):
        t.set(x, y + (half == 'upper'), z, f'minecraft:{wood}_door',
              {'facing': facing, 'half': half, 'hinge': 'left', 'open': 'false', 'powered': 'false'})


# ---------------------------------------------------------------- camps

S = 29          # camp footprint
C = S // 2      # centre = 14
GROUND = 1      # surface block layer
FLOOR = 2       # first walkable air layer
SKY = 13        # air is cleared up to this layer


class Camp:
    """Shared camp frame: ground, cleared air, central hearth plaza, four Kin, banners, edge lanterns."""

    def __init__(self, tribe_index, tribe):
        self.i = tribe_index
        self.tribe = tribe
        self.rng = random.Random(0x7A1B + tribe_index * 97)
        self.t = Template()
        rng = self.rng
        self.march = tribe == 'spindle'
        self.planks = rng.choice(['tribalpower:march_planks', 'minecraft:spruce_planks', 'tribalpower:march_planks', 'minecraft:dark_oak_planks'])
        self.log = 'tribalpower:march_log' if self.planks == 'tribalpower:march_planks' else \
            ('minecraft:dark_oak_log' if 'dark_oak' in self.planks else 'minecraft:spruce_log')
        self.wool = f'minecraft:{WOOL[tribe]}_wool'
        self.carpet = f'minecraft:{WOOL[tribe]}_carpet'
        self.cobble = 'tribalpower:march_cobble' if self.march else rng.choice(['minecraft:cobblestone', 'tribalpower:march_cobble', 'minecraft:mossy_cobblestone'])
        self.fence = 'minecraft:spruce_fence' if 'spruce' in self.planks else ('minecraft:dark_oak_fence' if 'dark_oak' in self.planks else 'minecraft:spruce_fence')
        self.kin_spots = []
        # interiors (x0, z0, x1, z1) of the huts in build order; the first one is the Weaver's hut
        self.huts = []

    # --- ground
    def ground(self, top=None, sub=None):
        rng = self.rng
        top = top or (['tribalpower:march_grass', 'tribalpower:march_grass', 'tribalpower:march_soil'] if self.march
                      else ['minecraft:grass_block'] * 7 + ['minecraft:coarse_dirt'])
        sub = sub or (['tribalpower:march_soil'] if self.march else ['minecraft:dirt'])
        for x in range(S):
            for z in range(S):
                self.t.set(x, 0, z, rng.choice(sub))
                self.t.set(x, GROUND, z, rng.choice(top))
        self.t.fill(0, FLOOR, 0, S - 1, SKY, S - 1, 'minecraft:air')
        # a paved plaza around the hearth
        pave = [self.cobble, self.cobble, 'minecraft:dirt_path' if not self.march else 'tribalpower:march_cobble']
        if self.march:
            pave = ['tribalpower:march_cobble', 'tribalpower:march_stone']
        for x in range(C - 4, C + 5):
            for z in range(C - 4, C + 5):
                if math.hypot(x - C, z - C) <= 4.3:
                    self.t.set(x, GROUND, z, rng.choice(pave))
        # worn paths from the plaza to the four sides
        for k in range(5, C):
            for (x, z) in ((C, C - k), (C, C + k), (C - k, C), (C + k, C)):
                if rng.random() < 0.8 and not self.march:
                    self.t.set(x, GROUND, z, 'minecraft:dirt_path')

    # --- the hearth plaza: tribe hearth at the exact centre, fire pit beside it, totem opposite
    def plaza(self, fire_offset=(0, 3), totem_offset=(0, -3)):
        t, rng, i = self.t, self.rng, self.i
        place(t, C, FLOOR, C, hearth_block(i))
        t.set(C, GROUND, C, 'minecraft:chiseled_stone_bricks')
        fx, fz = C + fire_offset[0], C + fire_offset[1]
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                t.set(fx + dx, GROUND, fz + dz, self.cobble if (dx or dz) else 'minecraft:stone')
        place(t, fx, FLOOR, fz, campfire())
        tx, tz = C + totem_offset[0], C + totem_offset[1]
        t.set(tx, GROUND, tz, 'minecraft:chiseled_stone_bricks')
        t.set(tx, FLOOR, tz, f'tribalpower:resonance_totem_{ATTUNEMENT[self.tribe]}')
        # two floor banners flanking the hearth (on a sturdy block: dirt paths are not full cubes)
        for bx in (C - 2, C + 2):
            t.set(bx, GROUND, C - 2, self.cobble)
            place(t, bx, FLOOR, C - 2, banner_floor(i, 'south'))
        # seating logs either side of the fire
        for lx in (fx - 3, fx + 3):
            for dz in (-1, 0, 1):
                t.set(lx, FLOOR, fz + dz, self.log, log_props(self.log, 'z'))
        # loom for the weaver, west of the hearth
        t.set(C - 3, FLOOR, C, 'minecraft:loom', {'facing': 'east'})
        self.kin_spots = [(C - 1, C + 1, 'ELDER'), (fx + 1, fz + 1, 'DRUMMER'), (C + 3, C - 1, 'HUNTER'), (C - 2, C + 1, 'WEAVER')]

    # --- huts
    def hut(self, x, z, w, d, facing, style='frame'):
        """Hut with its floor at ground level, 3-high interior, door + wall banner on the `facing` side."""
        t, rng = self.t, self.rng
        planks, wool, logw = self.planks, self.wool, self.log
        x1, z1 = x + w - 1, z + d - 1
        top = FLOOR + 3  # roof layer
        t.fill(x, GROUND, z, x1, GROUND, z1, planks if style != 'stone' else 'minecraft:stone_bricks')
        wall = rng.choice([wool, wool, planks]) if style == 'frame' else \
            rng.choice(['minecraft:cobblestone', 'minecraft:stone_bricks', 'tribalpower:march_cobble', 'minecraft:mossy_cobblestone'])
        for y in range(FLOOR, top):
            for xx in range(x, x1 + 1):
                t.set(xx, y, z, wall); t.set(xx, y, z1, wall)
            for zz in range(z, z1 + 1):
                t.set(x, y, zz, wall); t.set(x1, y, zz, wall)
        for (cx, cz) in ((x, z), (x1, z), (x, z1), (x1, z1)):
            t.fill(cx, FLOOR, cz, cx, top, cz, logw, log_props(logw, 'y'))
        t.fill(x + 1, FLOOR, z + 1, x1 - 1, top - 1, z1 - 1, 'minecraft:air')
        slab_name = 'minecraft:stone_brick_slab' if style == 'stone' else \
            ('minecraft:spruce_slab' if 'spruce' in planks else ('minecraft:dark_oak_slab' if 'dark_oak' in planks else 'minecraft:spruce_slab'))
        # hipped roof: flat plank ceiling, then rings of stairs stepping in and up, with a one-block overhang
        roof = planks if style == 'frame' else 'minecraft:stone_bricks'
        stair = ('minecraft:spruce_stairs' if 'spruce' in planks else ('minecraft:dark_oak_stairs' if 'dark_oak' in planks else 'minecraft:spruce_stairs')) \
            if style == 'frame' else 'minecraft:stone_brick_stairs'
        t.fill(x, top, z, x1, top, z1, roof)
        k = 0
        while True:
            rx0, rz0, rx1, rz1 = x - 1 + k, z - 1 + k, x1 + 1 - k, z1 + 1 - k
            y = top + k
            if rx1 - rx0 < 2 or rz1 - rz0 < 2:
                # ridge: a line of slabs (or a single block) on the last layer
                for xx in range(rx0, rx1 + 1):
                    for zz in range(rz0, rz1 + 1):
                        if 0 <= xx < S and 0 <= zz < S:
                            place(t, xx, y, zz, slab(slab_name))
                break
            for xx in range(rx0, rx1 + 1):
                for zz in range(rz0, rz1 + 1):
                    if not (0 <= xx < S and 0 <= zz < S):
                        continue
                    on_w, on_e, on_n, on_s = xx == rx0, xx == rx1, zz == rz0, zz == rz1
                    if on_w or on_e or on_n or on_s:
                        facing = 'east' if on_w else 'west' if on_e else 'south' if on_n else 'north'
                        if k == 0 and (xx < x or xx > x1 or zz < z or zz > z1):
                            place(t, xx, y, zz, stairs(stair, facing))       # overhang ring
                        elif k > 0:
                            place(t, xx, y, zz, stairs(stair, facing))
                    elif k > 0:
                        t.set(xx, y, zz, roof)
            k += 1
        # doorway on the facing side (mid wall), 2 high, real door
        mx, mz = x + w // 2, z + d // 2
        if facing == 'south':
            dx, dz = mx, z1
        elif facing == 'north':
            dx, dz = mx, z
        elif facing == 'east':
            dx, dz = x1, mz
        else:
            dx, dz = x, mz
        door(t, dx, FLOOR, dz, OPPOSITE[facing] if facing in ('north', 'south') else facing,
             'spruce' if style != 'stone' else 'dark_oak')
        # banner on the wall beside the door, outside
        ox, oz = DIRS[facing]
        side = (1, 0) if facing in ('north', 'south') else (0, 1)
        bx, bz = dx + ox + side[0], dz + oz + side[1]
        if t.is_air(bx, FLOOR, bz):
            place(t, bx, FLOOR, bz, banner_wall(self.i, facing))
        # windows: panes on the two side walls
        wx, wz = side
        for (px, pz) in ((dx + wx * 2, dz + wz * 2), (dx - wx * 2, dz - wz * 2)):
            if t.get(px, FLOOR + 1, pz) == wall:
                place(t, px, FLOOR + 1, pz, pane(n=wx == 1, s=wx == 1, e=wz == 1, w=wz == 1))
        # interior: hanging lantern, bed, a storage barrel and a carpet
        place(t, mx, top - 1, mz, lantern(hanging=True))
        bed_dir = OPPOSITE[facing]
        bfx, bfz = mx - (1 if facing in ('north', 'south') else 0), mz - (1 if facing in ('east', 'west') else 0)
        # bed along the back wall: foot nearer the door, head against the wall opposite the door
        bx0, bz0 = (bfx, (z + 2 if facing == 'south' else z1 - 2)) if facing in ('north', 'south') else \
            ((x + 2 if facing == 'east' else x1 - 2), bfz)
        if w >= 5 and d >= 5:
            bed(t, bx0, FLOOR, bz0, WOOL[self.tribe], bed_dir)
        if facing in ('north', 'south'):
            barrel_at = (x1 - 1, z + 1 if facing == 'south' else z1 - 1)
        else:
            barrel_at = (x + 1 if facing == 'east' else x1 - 1, z1 - 1)
        t.set(barrel_at[0], FLOOR, barrel_at[1], 'minecraft:barrel', {'facing': 'up', 'open': 'false'})
        if t.is_air(mx, FLOOR, mz):
            t.set(mx, FLOOR, mz, self.carpet)
        self.huts.append((x + 1, z + 1, x1 - 1, z1 - 1))
        return dx, dz

    def round_hut(self, cx, cz, r=3):
        """Pad-keeper roundhouse: mud-brick ring, low turf dome, south door."""
        t, rng = self.t, self.rng
        wall = rng.choice(['minecraft:packed_mud', 'minecraft:mud_bricks'])
        turf = ['minecraft:grass_block', 'minecraft:moss_block', 'minecraft:grass_block']
        for x in range(cx - r, cx + r + 1):
            for z in range(cz - r, cz + r + 1):
                dist = math.hypot(x - cx, z - cz)
                if dist <= r + 0.5:
                    t.set(x, GROUND, z, 'minecraft:packed_mud' if dist < r - 0.5 else wall)
                    if r - 0.6 <= dist <= r + 0.5:
                        t.fill(x, FLOOR, z, x, FLOOR + 1, z, wall)
                    else:
                        t.fill(x, FLOOR, z, x, FLOOR + 1, z, 'minecraft:air')
        # dome: rings of turf stepping inward
        for k, y in enumerate(range(FLOOR + 2, FLOOR + 5)):
            rr = r - k * 0.9
            for x in range(cx - r, cx + r + 1):
                for z in range(cz - r, cz + r + 1):
                    dist = math.hypot(x - cx, z - cz)
                    if dist <= rr + 0.5:
                        t.set(x, y, z, rng.choice(turf) if (dist > rr - 0.9 or k == 2) else self.log)
        t.set(cx, FLOOR + 2, cz, self.log, log_props(self.log, 'y'))
        # door (south), a lantern and a bed inside
        door(t, cx, FLOOR, cz + r, 'north')
        place(t, cx, FLOOR + 1, cz, lantern(hanging=True))
        bed(t, cx - 1, FLOOR, cz, WOOL[self.tribe], 'north')
        t.set(cx + 1, FLOOR, cz - 1, 'minecraft:barrel', {'facing': 'up', 'open': 'false'})
        if t.is_air(cx + 1, FLOOR, cz + r + 1):
            place(t, cx + 1, FLOOR, cz + r + 1, banner_wall(self.i, 'south'))
        self.huts.append((cx - r + 1, cz - r + 1, cx + r - 1, cz + r - 1))
        return cx, cz + r

    # --- the Weaver's loom
    def weaver_loom(self):
        """Every camp keeps a loom inside the Weaver's hut (the first hut built) and the Weaver spawns beside it."""
        if not self.huts:
            raise AssertionError(f'{self.tribe}: no hut for the Weaver')
        t = self.t
        v = Validator(self.tribe, t)
        x0, z0, x1, z1 = self.huts[0]
        inside = [(x, z) for x in range(x0, x1 + 1) for z in range(z0, z1 + 1)]
        loom = next(((x, z) for (x, z) in inside if t.get(x, FLOOR, z) == 'minecraft:loom'), None)

        def free(x, z):
            return t.is_air(x, FLOOR, z) and t.is_air(x, FLOOR + 1, z) and v.standable(x, FLOOR - 1, z)

        def neighbours(x, z):
            return [((x + dx, z + dz), f) for (dx, dz, f) in ((1, 0, 'east'), (-1, 0, 'west'), (0, 1, 'south'), (0, -1, 'north'))
                    if (x + dx, z + dz) in inside and free(x + dx, z + dz)]

        if loom is None:
            # a wall-side floor tile with a free tile beside it to stand on; the loom faces the free tile
            edge = [(x, z) for (x, z) in inside if (x in (x0, x1) or z in (z0, z1)) and free(x, z) and neighbours(x, z)]
            if not edge:
                raise AssertionError(f'{self.tribe}: no room for a loom in the Weaver\'s hut')
            loom = edge[0]
            (sx, sz), facing = neighbours(*loom)[0]
            t.set(loom[0], FLOOR, loom[1], 'minecraft:loom', {'facing': facing})
        else:
            spots = neighbours(*loom)
            if not spots:
                raise AssertionError(f'{self.tribe}: nowhere to stand beside the loom')
            (sx, sz), _ = spots[0]
        self.kin_spots = [(x, z, role) if role != 'WEAVER' else (sx, sz, role) for (x, z, role) in self.kin_spots]

    # --- the four Kin
    def kin(self):
        """Place the four Kin; each stands on the first solid block found around the plaza level."""
        v = Validator(self.tribe, self.t)
        for (x, z, role) in self.kin_spots:
            for y in (FLOOR, FLOOR - 1, FLOOR + 1):
                if v.standable(x, y - 1, z) and self.t.is_air(x, y, z) and self.t.is_air(x, y + 1, z):
                    self.t.kin(x, y, z, self.i, role)
                    break
            else:
                raise AssertionError(f'{self.tribe}: no standing room for {role} at {(x, z)}')

    def edge_lanterns(self):
        t = self.t
        for (px, pz) in ((1, C), (S - 2, C), (C, 1), (C, S - 2)):
            if t.is_air(px, FLOOR, pz) and t.is_air(px, FLOOR + 1, pz):
                place(t, px, FLOOR, pz, fence(self.fence))
                place(t, px, FLOOR + 1, pz, spirit_lantern())

    def scatter(self, choices, n, avoid_radius=6):
        t, rng = self.t, self.rng
        placed = 0
        tries = 0
        while placed < n and tries < 200:
            tries += 1
            gx, gz = rng.randint(1, S - 2), rng.randint(1, S - 2)
            if math.hypot(gx - C, gz - C) < avoid_radius:
                continue
            if t.is_air(gx, FLOOR, gz) and t.is_air(gx, FLOOR + 1, gz) and t.get(gx, GROUND, gz) not in ('minecraft:air',):
                spec = rng.choice(choices)
                if isinstance(spec, str):
                    t.set(gx, FLOOR, gz, spec)
                else:
                    place(t, gx, FLOOR, gz, spec)
                placed += 1

    def save(self):
        self.weaver_loom()
        self.kin()
        return self.t.save(f'tribe_camp_{self.tribe}')


# hut corners: (x, z, facing toward the plaza)
CORNERS = [(2, 2, 'south'), (S - 9, 2, 'south'), (2, S - 9, 'north'), (S - 9, S - 9, 'north')]


def standard_huts(camp, count=None, style='frame', sizes=(5, 6, 7)):
    rng = camp.rng
    corners = CORNERS[:]
    rng.shuffle(corners)
    count = count or (3 if rng.random() < 0.5 else 4)
    for (hx, hz, facing) in corners[:count]:
        w = rng.choice(sizes); d = rng.choice(sizes)
        # keep the hut inside its 7x7 corner cell
        camp.hut(hx + (7 - w) // 2, hz + (7 - d) // 2, w, d, facing, style)
    return [(hx + 3, hz + 3) for (hx, hz, _) in corners[count:]]   # centres of the unused corner cells


def corner_feature(camp, cx, cz):
    """Fills an unused corner cell (7x7 centred on cx, cz) with a small tribe-flavoured yard."""
    t, rng, tribe = camp.t, camp.rng, camp.tribe
    if tribe == 'stone':
        # quarry: a one-deep pit with exposed ore, a stone stack and steps
        for x in range(cx - 2, cx + 3):
            for z in range(cz - 2, cz + 3):
                t.set(x, 0, z, rng.choice(['minecraft:stone', 'minecraft:stone', 'minecraft:iron_ore', 'minecraft:coal_ore', 'minecraft:copper_ore', 'tribalpower:march_ore']))
                t.set(x, GROUND, z, 'minecraft:air')
        for x in range(cx - 3, cx + 4):
            for z in range(cz - 3, cz + 4):
                if max(abs(x - cx), abs(z - cz)) == 3:
                    t.set(x, GROUND, z, 'minecraft:stone')
        place(t, cx, GROUND, cz + 2, stairs('minecraft:stone_stairs', 'north'))
        t.set(cx - 1, GROUND, cz - 1, 'minecraft:cobblestone'); t.set(cx - 1, FLOOR, cz - 1, 'minecraft:cobblestone')
        t.set(cx + 1, GROUND, cz - 1, 'minecraft:raw_iron_block')
        t.set(cx + 2, FLOOR, cz + 3, 'minecraft:barrel', {'facing': 'up', 'open': 'false'})
    elif tribe == 'spark':
        # practice ring: five basedrum note blocks around a campfire on terracotta
        for x in range(cx - 2, cx + 3):
            for z in range(cz - 2, cz + 3):
                t.set(x, GROUND, z, 'minecraft:terracotta' if max(abs(x - cx), abs(z - cz)) == 2 else 'minecraft:orange_terracotta')
        place(t, cx, FLOOR, cz, campfire())
        for k in range(5):
            a = k * math.tau / 5
            nx, nz = round(cx + 2.2 * math.cos(a)), round(cz + 2.2 * math.sin(a))
            t.set(nx, FLOOR, nz, 'minecraft:note_block', {'instrument': 'basedrum', 'note': str((k * 5) % 25), 'powered': 'false'})
    elif tribe == 'clock':
        # sundial: stone disc, a lightning-rod gnomon, copper hour marks, a daylight detector
        for x in range(cx - 3, cx + 4):
            for z in range(cz - 3, cz + 4):
                if math.hypot(x - cx, z - cz) <= 3.3:
                    t.set(x, GROUND, z, 'minecraft:polished_andesite' if math.hypot(x - cx, z - cz) < 2.6 else 'minecraft:stone_bricks')
        t.set(cx, FLOOR, cz, 'minecraft:cut_copper')
        t.set(cx, FLOOR + 1, cz, 'minecraft:lightning_rod', {'facing': 'up', 'powered': 'false', 'waterlogged': 'false'})
        for (dx, dz) in ((0, -3), (3, 0), (0, 3), (-3, 0)):
            t.set(cx + dx, FLOOR, cz + dz, 'minecraft:copper_block')
        t.set(cx + 2, FLOOR, cz - 2, 'minecraft:daylight_detector', {'inverted': 'false', 'power': '0'})
    elif tribe == 'swarm':
        # apiary: three hives on oak posts with a flower strip in front
        for k, x in enumerate(range(cx - 2, cx + 3, 2)):
            t.set(x, FLOOR, cz, 'minecraft:oak_fence', {'north': 'false', 'south': 'false', 'east': 'false', 'west': 'false', 'waterlogged': 'false'})
            t.set(x, FLOOR + 1, cz, 'minecraft:beehive', {'facing': 'south', 'honey_level': str(rng.randint(0, 5))})
            for dz in (1, 2):
                t.set(x, GROUND, cz + dz, 'minecraft:grass_block')
                t.set(x, FLOOR, cz + dz, rng.choice(['minecraft:dandelion', 'minecraft:poppy', 'minecraft:cornflower', 'minecraft:oxeye_daisy']))
        t.set(cx - 3, FLOOR, cz - 1, 'minecraft:honeycomb_block')
        t.set(cx + 3, FLOOR, cz - 1, 'minecraft:hay_block', {'axis': 'y'})
    elif tribe == 'sigil':
        # carving yard: deepslate slab table, stonecutter, chiseled blocks and candles
        for x in range(cx - 2, cx + 3):
            for z in range(cz - 2, cz + 3):
                t.set(x, GROUND, z, 'minecraft:cobbled_deepslate' if (x + z) % 2 else 'minecraft:polished_deepslate')
        t.set(cx, FLOOR, cz, 'minecraft:stonecutter', {'facing': 'south'})
        for (dx, dz) in ((-2, -2), (2, -2), (-2, 2), (2, 2)):
            t.set(cx + dx, FLOOR, cz + dz, 'minecraft:chiseled_deepslate')
            t.set(cx + dx, FLOOR + 1, cz + dz, 'minecraft:candle', {'candles': str(rng.randint(1, 3)), 'lit': 'true', 'waterlogged': 'false'})
        t.set(cx - 2, FLOOR, cz, 'minecraft:lectern', {'facing': 'east', 'has_book': 'false', 'powered': 'false'})
        t.set(cx + 2, FLOOR, cz, 'minecraft:deepslate_bricks')
    elif tribe == 'sprout':
        # nursery: podzol beds with saplings, a composter and moss
        for x in range(cx - 2, cx + 3):
            for z in range(cz - 2, cz + 3):
                t.set(x, GROUND, z, 'minecraft:podzol' if (x + z) % 2 == 0 else 'minecraft:moss_block')
                if (x + z) % 2 == 0 and max(abs(x - cx), abs(z - cz)) < 2:
                    t.set(x, FLOOR, z, rng.choice(['minecraft:oak_sapling', 'minecraft:spruce_sapling', 'minecraft:birch_sapling', 'minecraft:jungle_sapling']), {'stage': '0'})
        t.set(cx - 3, FLOOR, cz, 'minecraft:composter', {'level': '4'})
        t.set(cx + 3, FLOOR, cz, 'minecraft:barrel', {'facing': 'up', 'open': 'false'})
    elif tribe == 'spindle':
        # crystal garden: a cluster of March crystal on moss
        for x in range(cx - 2, cx + 3):
            for z in range(cz - 2, cz + 3):
                if math.hypot(x - cx, z - cz) <= 2.5:
                    t.set(x, GROUND, z, 'tribalpower:march_moss')
                    if rng.random() < 0.45:
                        t.fill(x, FLOOR, z, x, FLOOR + rng.randint(0, 2), z, 'tribalpower:march_crystal')
    elif tribe == 'claw':
        # sparring yard: hay-block target dummies and a weapon rack
        for (dx, dz) in ((-2, 0), (2, 0)):
            t.fill(cx + dx, FLOOR, cz + dz, cx + dx, FLOOR + 1, cz + dz, 'minecraft:hay_block', {'axis': 'y'})
            t.set(cx + dx, FLOOR + 2, cz + dz, 'minecraft:carved_pumpkin', {'facing': 'south'})
        t.set(cx, FLOOR, cz + 2, 'minecraft:fletching_table')


def _stone_font(camp, x, z, braced=False):
    """A working tier 1 Stone Font: the font and four chalk marks. Braced adds the tier 2 anchor stones."""
    t = camp.t
    t.set(x, FLOOR, z, 'tribalpower:stone_font')
    for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        t.set(x + dx, FLOOR, z + dz, 'tribalpower:ritual_mark')
    if braced:
        for (dx, dz) in ((2, 2), (2, -2), (-2, 2), (-2, -2)):
            t.set(x + dx, FLOOR, z + dz, 'tribalpower:anchor_stone')


def camp_soil(camp):
    """Pad-keepers: roundhouses with turf domes, a sunken hearth pit lined with cobble, hay and crops."""
    t, rng, i = camp.t, camp.rng, camp.i
    camp.ground()
    # hearth pit: dig the surface out in a 7x7 disc so the hearth sits one block down
    for x in range(C - 3, C + 4):
        for z in range(C - 3, C + 4):
            d = math.hypot(x - C, z - C)
            if d <= 3.5:
                t.set(x, GROUND, z, 'minecraft:air')
                t.set(x, 0, z, 'minecraft:packed_mud' if d < 2.6 else camp.cobble)
            elif d <= 4.5:
                t.set(x, GROUND, z, camp.cobble)
    place(t, C, GROUND, C, hearth_block(i))
    t.set(C, 0, C, 'minecraft:chiseled_stone_bricks')
    place(t, C + 2, GROUND, C, campfire())
    for z in (C - 2, C + 2):
        for dx in (-1, 0, 1):
            t.set(C + dx, GROUND, z, camp.log, log_props(camp.log, 'x'))
    # steps into the pit on the west and east
    place(t, C - 4, GROUND, C, stairs('minecraft:cobblestone_stairs', 'west'))
    t.set(C - 4, 0, C, camp.cobble)
    place(t, C - 3, GROUND, C, slab('minecraft:cobblestone_slab'))
    t.set(C + 3, GROUND, C - 1, 'minecraft:air')
    # totem and banners on the rim
    t.set(C, FLOOR, C - 5, f'tribalpower:resonance_totem_earth')
    t.set(C, GROUND, C - 5, 'minecraft:chiseled_stone_bricks')
    place(t, C - 2, FLOOR, C - 5, banner_floor(i, 'south'))
    place(t, C + 2, FLOOR, C - 5, banner_floor(i, 'south'))
    t.set(C - 5, FLOOR, C + 1, 'minecraft:loom', {'facing': 'east'})
    camp.kin_spots = [(C - 1, C - 1, 'ELDER'), (C + 1, C + 1, 'DRUMMER'), (C + 5, C, 'HUNTER'), (C - 5, C, 'WEAVER')]
    # roundhouses
    for (cx, cz) in ((5, 5), (S - 6, 5), (5, S - 6), (S - 6, S - 6)):
        camp.round_hut(cx, cz, rng.choice([3, 3, 4]))
    # crop rows on the south edge, hay and a composter
    for x in range(C - 4, C + 5):
        if t.is_air(x, FLOOR, S - 3):
            t.set(x, GROUND, S - 3, 'minecraft:farmland', {'moisture': '7'})
            t.set(x, FLOOR, S - 3, 'minecraft:wheat', {'age': str(rng.randint(3, 7))})
    # their own craft: a working Stone Font, chalk and all
    _stone_font(camp, C - 6, C + 6)
    camp.scatter(['minecraft:hay_block', ('minecraft:composter', {'level': '3'}), 'minecraft:moss_block', 'minecraft:barrel'], 4)
    camp.edge_lanterns()


def camp_stone(camp):
    """Grit-singers: stone huts, an ore-cart siding with a minecart, ore crates and a stone-pick rack."""
    t, rng, i = camp.t, camp.rng, camp.i
    camp.ground(top=['minecraft:grass_block'] * 4 + ['minecraft:coarse_dirt', 'minecraft:stone', 'minecraft:gravel'] if not camp.march else None,
                sub=['minecraft:stone', 'minecraft:dirt'])
    camp.cobble = rng.choice(['minecraft:cobblestone', 'minecraft:stone_bricks'])
    camp.plaza(fire_offset=(0, 3), totem_offset=(0, -3))
    for (fx, fz) in standard_huts(camp, style='stone', sizes=(5, 6, 7)):
        corner_feature(camp, fx, fz)
    # ore-cart siding along the east edge: rails on gravel, a minecart, raw ore blocks and crates
    zr = list(range(6, S - 6))
    for z in zr:
        t.set(S - 3, GROUND, z, 'minecraft:gravel')
        t.set(S - 3, FLOOR, z, 'minecraft:rail', {'shape': 'north_south', 'waterlogged': 'false'})
    t.entity(S - 3 + 0.5, FLOOR + 0.0625, C + 0.5, 'minecraft:chest_minecart')
    ore = ['minecraft:raw_iron_block', 'minecraft:raw_copper_block', 'tribalpower:march_ore', 'minecraft:iron_ore', 'minecraft:copper_ore']
    for z in (8, 12, 17, 21):
        t.set(S - 2, FLOOR, z, rng.choice(ore))
        if rng.random() < 0.5:
            t.set(S - 2, FLOOR + 1, z, rng.choice(ore))
    for z in (10, 19):
        t.set(S - 4, FLOOR, z, 'minecraft:barrel', {'facing': 'up', 'open': 'false'})
    # buffer stops
    for z in (zr[0] - 1, zr[-1] + 1):
        place(t, S - 3, FLOOR, z, fence('minecraft:spruce_fence'))
    # a grindstone and an anvil by the fire, a standing cairn with the mesh glyph banner
    t.set(C + 4, FLOOR, C + 3, 'minecraft:grindstone', {'face': 'floor', 'facing': 'north'})
    t.set(C - 4, FLOOR, C + 3, 'minecraft:anvil', {'facing': 'east'})
    for (x, z) in ((3, C), (C, 3)):
        h = rng.randint(2, 3)
        t.fill(x, FLOOR, z, x, FLOOR + h - 1, z, 'minecraft:cobblestone')
        place(t, x, FLOOR + h, z, spirit_lantern())
    # the craft itself: a braced Stone Font, and a Listening Pit with its mesh and cache
    _stone_font(camp, C - 6, C - 5, braced=True)
    t.set(C + 6, FLOOR, C + 6, 'tribalpower:resonance_mesh')
    for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        t.set(C + 6 + dx, FLOOR, C + 6 + dz, 'tribalpower:ritual_mark')
    for (dx, dz) in ((2, 2), (2, -2), (-2, 2), (-2, -2)):
        t.set(C + 6 + dx, FLOOR, C + 6 + dz, 'tribalpower:anchor_stone')
    t.set(C + 6, FLOOR + 1, C + 6, 'tribalpower:ancestral_cache')
    camp.scatter(['minecraft:cobblestone', 'minecraft:stone', 'minecraft:gravel', 'minecraft:andesite', slab('minecraft:cobblestone_slab')], 6)
    camp.edge_lanterns()


def camp_sprout(camp):
    """Rootbinders: a great living tree over the hearth, leaf-roofed huts, moss, saplings and reeds."""
    t, rng, i = camp.t, camp.rng, camp.i
    camp.ground(top=['minecraft:grass_block'] * 5 + ['minecraft:moss_block', 'minecraft:podzol'] if not camp.march else None)
    camp.cobble = 'minecraft:mossy_cobblestone'
    camp.plaza(fire_offset=(3, 3), totem_offset=(-3, -3))
    # trunk: a 2x2 march_log column north-west of the hearth, canopy of march_leaves spread over the plaza
    tx, tz = C - 3, C - 1
    trunk_h = FLOOR + 8
    for (dx, dz) in ((0, 0), (1, 0), (0, 1), (1, 1)):
        t.fill(tx + dx, GROUND, tz + dz, tx + dx, trunk_h, tz + dz, 'tribalpower:march_log', log_props('tribalpower:march_log', 'y'))
    # roots
    for (dx, dz, axis) in ((-1, 0, 'x'), (2, 1, 'x'), (0, -1, 'z'), (1, 2, 'z')):
        t.set(tx + dx, FLOOR, tz + dz, 'tribalpower:march_log', log_props('tribalpower:march_log', axis))
    # branches out and a broad canopy: three leaf discs at different heights
    for (cy, r, ox, oz) in ((trunk_h - 2, 5, 1, 1), (trunk_h, 6, 1, 1), (trunk_h + 2, 4, 1, 1), (trunk_h + 3, 2, 1, 1)):
        for x in range(tx + ox - r, tx + ox + r + 1):
            for z in range(tz + oz - r, tz + oz + r + 1):
                d = math.hypot(x - tx - ox, z - tz - oz)
                if d <= r + 0.3 and 0 <= x < S and 0 <= z < S and t.is_air(x, cy, z) and rng.random() < 0.9:
                    place(t, x, cy, z, leaves())
    for (bx, bz, axis) in ((tx + 3, tz + 1, 'x'), (tx - 2, tz, 'x'), (tx + 1, tz + 4, 'z'), (tx, tz - 3, 'z')):
        t.set(bx, trunk_h - 1, bz, 'tribalpower:march_log', log_props('tribalpower:march_log', axis))
    # spirit lanterns hanging from the canopy
    for (lx, lz) in ((tx + 4, tz + 1), (tx - 1, tz + 3)):
        if not t.is_air(lx, trunk_h - 2, lz) and t.is_air(lx, trunk_h - 3, lz):
            place(t, lx, trunk_h - 3, lz, lantern(hanging=True))
    # weaver's loom sits by the trunk
    t.set(C - 3, FLOOR, C, 'minecraft:air')
    t.set(C - 1, FLOOR, C + 3, 'minecraft:loom', {'facing': 'north'})
    camp.kin_spots = [(C - 1, C + 1, 'ELDER'), (C + 2, C + 4, 'DRUMMER'), (C + 3, C - 2, 'HUNTER'), (C - 2, C + 3, 'WEAVER')]
    # huts get leaf roofs
    free = standard_huts(camp, count=3, sizes=(5, 6))
    for (x, y, z), (bname, props, _) in list(t.blocks.items()):
        if y >= FLOOR + 3 and bname in (camp.planks, 'minecraft:spruce_slab', 'minecraft:dark_oak_slab', 'minecraft:spruce_stairs', 'minecraft:dark_oak_stairs') \
                and (x < 10 or x > S - 11) and (z < 10 or z > S - 11):
            place(t, x, y, z, leaves())
    for (fx, fz) in free:
        corner_feature(camp, fx, fz)
    # nursery beds: saplings on podzol, moss carpets, reeds
    for x in range(C - 3, C + 4, 2):
        if t.is_air(x, FLOOR, S - 3):
            t.set(x, GROUND, S - 3, 'minecraft:podzol')
            plant = rng.choice(['minecraft:oak_sapling', 'minecraft:spruce_sapling', 'minecraft:dark_oak_sapling', 'minecraft:azalea'])
            t.set(x, FLOOR, S - 3, plant, {'stage': '0'} if plant.endswith('_sapling') else None)
    # their own craft: a Wave Drum beside water, and a cistern to pipe it properly
    t.set(C + 6, FLOOR, C - 5, 'tribalpower:wave_drum', {'lit': 'false'})
    t.set(C + 7, GROUND, C - 5, 'minecraft:water')
    t.set(C + 6, FLOOR, C - 6, 'tribalpower:spirit_cistern')
    camp.scatter(['minecraft:moss_carpet', 'minecraft:fern', 'minecraft:moss_block', 'minecraft:flowering_azalea', 'minecraft:large_fern'], 8, avoid_radius=3)
    # fix any large_fern (double plant) into a fern to keep it single-block
    for pos, (bname, props, nbt) in list(t.blocks.items()):
        if bname == 'minecraft:large_fern':
            t.set(*pos, 'minecraft:fern')
        if bname == 'minecraft:moss_carpet' or bname == 'minecraft:fern':
            # plants need a soil block below: swap paths/cobble for grass under them
            gx, gy, gz = pos
            if t.get(gx, gy - 1, gz) not in ('minecraft:grass_block', 'minecraft:moss_block', 'minecraft:podzol', 'minecraft:dirt', 'tribalpower:march_grass', 'tribalpower:march_soil'):
                t.set(gx, gy - 1, gz, 'minecraft:moss_block')
    camp.edge_lanterns()


def camp_claw(camp):
    """Edge-walkers: a watchtower at the north edge, a rope fence perimeter, weapon racks and a training post."""
    t, rng, i = camp.t, camp.rng, camp.i
    camp.ground(top=['minecraft:grass_block'] * 5 + ['minecraft:podzol', 'minecraft:coarse_dirt'] if not camp.march else None)
    camp.plaza(fire_offset=(0, 3), totem_offset=(0, -3))
    free = standard_huts(camp, count=3, sizes=(5, 6))
    tx, tz = free[0][0] - 2, free[0][1] - 2   # 5x5 tower centred in the free corner cell
    # watchtower: 5x5 base of logs, 9 high, ladder inside, platform + parapet on top, lantern on a pole
    top = FLOOR + 8
    for (dx, dz) in ((0, 0), (4, 0), (0, 4), (4, 4)):
        t.fill(tx + dx, FLOOR, tz + dz, tx + dx, top + 1, tz + dz, camp.log, log_props(camp.log, 'y'))
    for y in range(FLOOR, top):
        for k in range(1, 4):
            if y < FLOOR + 2 or (y - FLOOR) % 3 == 0:
                t.set(tx + k, y, tz, camp.planks); t.set(tx + k, y, tz + 4, camp.planks)
                t.set(tx, y, tz + k, camp.planks); t.set(tx + 4, y, tz + k, camp.planks)
            else:
                for (px, pz, spec) in ((tx + k, tz, fence(camp.fence, e=True, w=True)), (tx + k, tz + 4, fence(camp.fence, e=True, w=True)),
                                       (tx, tz + k, fence(camp.fence, n=True, s=True)), (tx + 4, tz + k, fence(camp.fence, n=True, s=True))):
                    place(t, px, y, pz, spec)
    t.fill(tx + 1, top, tz + 1, tx + 3, top, tz + 3, camp.planks)
    t.fill(tx, top, tz, tx + 4, top, tz + 4, camp.planks)
    for k in range(0, 5):
        for (px, pz, spec) in ((tx + k, tz, fence(camp.fence, e=k < 4, w=k > 0)), (tx + k, tz + 4, fence(camp.fence, e=k < 4, w=k > 0)),
                               (tx, tz + k, fence(camp.fence, n=k > 0, s=k < 4)), (tx + 4, tz + k, fence(camp.fence, n=k > 0, s=k < 4))):
            if t.get(px, top + 1, pz) != camp.log:
                place(t, px, top + 1, pz, spec)
    t.fill(tx + 1, GROUND, tz + 1, tx + 3, GROUND, tz + 3, camp.planks)
    t.fill(tx + 1, FLOOR, tz + 1, tx + 3, top - 1, tz + 3, 'minecraft:air')
    t.fill(tx + 2, top, tz + 2, tx + 2, top, tz + 2, 'minecraft:air')  # hatch
    place(t, tx + 2, top, tz + 2, ('minecraft:spruce_trapdoor', {'facing': 'north', 'half': 'top', 'open': 'true', 'powered': 'false', 'waterlogged': 'false'}))
    # tower door on the plaza-facing side (south wall if the tower is north of the plaza, else north wall);
    # the ladder climbs the wall opposite the door
    south_side = tz < C
    dz = tz + 4 if south_side else tz
    door(t, tx + 2, FLOOR, dz, 'north' if south_side else 'south')
    lz, wall_z, lface = (tz + 1, tz, 'south') if south_side else (tz + 3, tz + 4, 'north')
    for y in range(FLOOR, top + 1):
        t.set(tx + 2, y, lz, 'minecraft:ladder', {'facing': lface, 'waterlogged': 'false'})
        t.set(tx + 2, y, wall_z, camp.planks)
    t.set(tx + 2, top + 2, tz + 2, camp.fence, {'north': 'false', 'south': 'false', 'east': 'false', 'west': 'false', 'waterlogged': 'false'})
    place(t, tx + 2, top + 3, tz + 2, spirit_lantern())
    for (dx, dz2) in ((0, 0), (4, 4)):
        place(t, tx + dx, top + 2, tz + dz2, banner_floor(i, 'south'))
    # rope fence perimeter: posts every 4 blocks with chains strung between, gaps at the four paths
    for k in range(0, S, 4):
        for (px, pz) in ((k, 0), (k, S - 1), (0, k), (S - 1, k)):
            if abs(px - C) <= 1 or abs(pz - C) <= 1:
                continue
            if t.is_air(px, FLOOR, pz) and t.is_air(px, FLOOR + 1, pz):
                place(t, px, FLOOR, pz, fence(camp.fence))
                place(t, px, FLOOR + 1, pz, fence(camp.fence))
    for k in range(S):
        for (px, pz, axis) in ((k, 0, 'x'), (k, S - 1, 'x'), (0, k, 'z'), (S - 1, k, 'z')):
            if k % 4 == 0 or abs(k - C) <= 1:
                continue
            if t.is_air(px, FLOOR + 1, pz):
                t.set(px, FLOOR + 1, pz, 'minecraft:chain', {'axis': axis, 'waterlogged': 'false'})
    # training post and weapon racks
    t.fill(C + 5, FLOOR, C + 4, C + 5, FLOOR + 1, C + 4, 'minecraft:hay_block', {'axis': 'y'})
    t.set(C + 5, FLOOR + 2, C + 4, 'minecraft:carved_pumpkin', {'facing': 'west'})
    for (x, z) in ((C - 5, C + 4), (C - 5, C + 5)):
        t.set(x, FLOOR, z, 'minecraft:smithing_table' if z == C + 4 else 'minecraft:fletching_table')
    camp.scatter(['minecraft:campfire' if False else ('minecraft:barrel', {'facing': 'up', 'open': 'false'}), 'minecraft:hay_block', 'minecraft:mossy_cobblestone'], 3)
    camp.edge_lanterns()


def camp_spark(camp):
    """Drumhearts: a big raised drum platform with a Drumheart at its centre, copper trim, note blocks and braziers."""
    t, rng, i = camp.t, camp.rng, camp.i
    camp.ground(top=['minecraft:grass_block'] * 5 + ['minecraft:coarse_dirt'] if not camp.march else None)
    camp.cobble = rng.choice(['minecraft:terracotta', 'minecraft:cobblestone'])
    # platform 9x9 of planks with copper corners, one block raised, stairs on all four sides
    r = 4
    for x in range(C - r, C + r + 1):
        for z in range(C - r, C + r + 1):
            edge = x in (C - r, C + r) or z in (C - r, C + r)
            t.set(x, FLOOR, z, camp.planks if not edge else ('minecraft:copper_block' if (x in (C - r, C + r) and z in (C - r, C + r)) else 'minecraft:cut_copper'))
    for (x, z, f) in ((C, C - r - 1, 'north'), (C, C + r + 1, 'south'), (C - r - 1, C, 'west'), (C + r + 1, C, 'east')):
        place(t, x, FLOOR, z, stairs('minecraft:spruce_stairs', f))
    # drumheart at the centre on the platform, hearth beside it, campfire ring in front
    t.set(C, FLOOR + 1, C, 'tribalpower:drumheart')
    place(t, C - 2, FLOOR + 1, C, hearth_block(i))
    t.set(C + 2, FLOOR + 1, C, f'tribalpower:resonance_totem_fire')
    for (x, z) in ((C - 3, C - 3), (C + 3, C - 3), (C - 3, C + 3), (C + 3, C + 3)):
        t.set(x, FLOOR + 1, z, 'minecraft:note_block', {'instrument': 'basedrum', 'note': str(rng.randint(0, 24)), 'powered': 'false'})
        t.set(x, FLOOR + 2, z, 'minecraft:orange_carpet')
    for (x, z) in ((C - 3, C), (C + 3, C), (C, C - 3), (C, C + 3)):
        place(t, x, FLOOR + 1, z, campfire())
    place(t, C - 1, FLOOR + 1, C - 3, banner_floor(i, 'south'))
    place(t, C + 1, FLOOR + 1, C - 3, banner_floor(i, 'south'))
    # tall braziers on poles at the platform corners
    for (x, z) in ((C - r - 2, C - r - 2), (C + r + 2, C - r - 2), (C - r - 2, C + r + 2), (C + r + 2, C + r + 2)):
        t.fill(x, FLOOR, z, x, FLOOR + 2, z, 'minecraft:copper_block' if rng.random() < 0.5 else 'minecraft:cut_copper')
        place(t, x, FLOOR + 3, z, campfire())
    # loom off the platform for the weaver
    t.set(C - 6, FLOOR, C + 2, 'minecraft:loom', {'facing': 'east'})
    # their own craft: the Ember Horn, banked with fuel
    t.set(C + 6, FLOOR, C + 2, 'tribalpower:ember_horn', {'lit': 'false'})
    t.set(C + 6, FLOOR, C + 3, 'minecraft:coal_block')
    camp.kin_spots = [(C - 1, C + 1, 'ELDER'), (C + 1, C + 1, 'DRUMMER'), (C + 6, C - 1, 'HUNTER'), (C - 5, C + 2, 'WEAVER')]
    for (fx, fz) in standard_huts(camp, sizes=(5, 6, 7)):
        corner_feature(camp, fx, fz)
    camp.scatter(['minecraft:hay_block', ('minecraft:barrel', {'facing': 'up', 'open': 'false'}), 'minecraft:copper_block', 'minecraft:orange_wool'], 4, avoid_radius=8)
    camp.edge_lanterns()


def camp_clock(camp):
    """Pattern-weavers: a geometric lattice pergola over the plaza with note blocks, redstone lamps and copper rods."""
    t, rng, i = camp.t, camp.rng, camp.i
    camp.ground(top=['minecraft:grass_block'] * 6 + ['minecraft:gravel'] if not camp.march else None)
    camp.cobble = rng.choice(['minecraft:stone_bricks', 'minecraft:polished_andesite'])
    camp.plaza(fire_offset=(0, 3), totem_offset=(0, -3))
    free = standard_huts(camp, sizes=(5, 6))
    # pergola: 4x4 grid of posts (spacing 4) from C-6..C+6, lattice of fences at the top, slab checkerboard roof
    posts = [C - 6, C - 2, C + 2, C + 6]
    h = FLOOR + 4
    for px in posts:
        for pz in posts:
            t.fill(px, FLOOR, pz, px, h, pz, camp.log, log_props(camp.log, 'y'))
            t.set(px, GROUND, pz, 'minecraft:polished_andesite')
    for px in posts:
        for z in range(C - 6, C + 7):
            if z not in posts:
                place(t, px, h, z, fence(camp.fence, n=True, s=True))
    for pz in posts:
        for x in range(C - 6, C + 7):
            if x not in posts:
                place(t, x, h, pz, fence(camp.fence, e=True, w=True))
    for x in range(C - 6, C + 7):
        for z in range(C - 6, C + 7):
            if (x + z) % 4 == 0 and (x - (C - 6)) % 4 != 0 and (z - (C - 6)) % 4 != 0:
                place(t, x, h + 1, z, slab('minecraft:birch_slab' if not camp.march else 'minecraft:spruce_slab'))
    # redstone lamps and note blocks in a repeating pattern along the roof line, copper rods at the corners
    for k, (px, pz) in enumerate([(p, q) for p in posts for q in posts]):
        if (px, pz) in ((posts[0], posts[0]), (posts[-1], posts[0]), (posts[0], posts[-1]), (posts[-1], posts[-1])):
            t.set(px, h + 1, pz, 'minecraft:lightning_rod', {'facing': 'up', 'powered': 'false', 'waterlogged': 'false'})
        elif k % 2 == 0:
            t.set(px, h + 1, pz, 'minecraft:redstone_lamp', {'lit': 'true'})
        else:
            t.set(px, h + 1, pz, 'minecraft:note_block', {'instrument': 'bell', 'note': str((k * 5) % 25), 'powered': 'false'})
    # a small clockwork bench: note blocks in a row with copper, an observer and a lectern
    for k, x in enumerate(range(C - 3, C + 4, 2)):
        t.set(x, FLOOR, C + 5, 'minecraft:note_block', {'instrument': 'chime', 'note': str((k * 4 + 2) % 25), 'powered': 'false'})
    t.set(C + 5, FLOOR, C + 5, 'minecraft:lectern', {'facing': 'north', 'has_book': 'false', 'powered': 'false'})
    t.set(C - 5, FLOOR, C + 5, 'minecraft:observer', {'facing': 'north', 'powered': 'false'})
    t.set(C - 5, FLOOR, C - 5, 'minecraft:cut_copper')
    t.set(C + 5, FLOOR, C - 5, 'minecraft:cut_copper')
    place(t, C - 5, FLOOR + 1, C - 5, lantern())
    place(t, C + 5, FLOOR + 1, C - 5, lantern())
    camp.kin_spots = [(C - 1, C + 1, 'ELDER'), (C + 1, C + 4, 'DRUMMER'), (C + 3, C - 1, 'HUNTER'), (C - 2, C + 1, 'WEAVER')]
    for (fx, fz) in free:
        corner_feature(camp, fx, fz)
    # their own craft: a Wind Harp on a mast, under open sky where it is worth anything
    t.fill(C + 6, FLOOR, C + 6, C + 6, FLOOR + 3, C + 6, 'minecraft:cut_copper')
    t.set(C + 6, FLOOR + 4, C + 6, 'tribalpower:wind_harp', {'lit': 'false'})
    camp.scatter(['minecraft:copper_block', 'minecraft:target', ('minecraft:barrel', {'facing': 'up', 'open': 'false'}), 'minecraft:daylight_detector'], 4, avoid_radius=8)
    camp.edge_lanterns()


def camp_swarm(camp):
    """Colony-keepers: bee nests on log posts, honey blocks, flower beds ringing the plaza, a honey press."""
    t, rng, i = camp.t, camp.rng, camp.i
    camp.ground(top=['minecraft:grass_block'] * 5 + ['minecraft:moss_block'] if not camp.march else None)
    camp.plaza(fire_offset=(0, 3), totem_offset=(0, -3))
    flowers = ['minecraft:dandelion', 'minecraft:poppy', 'minecraft:oxeye_daisy', 'minecraft:cornflower', 'minecraft:allium',
               'minecraft:azure_bluet', 'minecraft:orange_tulip', 'minecraft:pink_tulip', 'minecraft:blue_orchid']
    # flower beds ringing the plaza at radius 6-7
    for x in range(S):
        for z in range(S):
            d = math.hypot(x - C, z - C)
            if 5.6 <= d <= 7.4 and t.is_air(x, FLOOR, z) and rng.random() < 0.7 and abs(x - C) > 1 and abs(z - C) > 1:
                t.set(x, GROUND, z, 'minecraft:grass_block')
                t.set(x, FLOOR, z, rng.choice(flowers))
    # hive posts: log columns with a bee nest on top facing the plaza, four around the camp edge
    for (hx, hz, facing) in ((5, C, 'east'), (S - 6, C, 'west'), (C, 5, 'south'), (C, S - 6, 'north')):
        t.fill(hx, FLOOR, hz, hx, FLOOR + 1, hz, 'minecraft:oak_log', {'axis': 'y'})
        t.set(hx, FLOOR + 2, hz, 'minecraft:bee_nest', {'facing': facing, 'honey_level': str(rng.randint(2, 5))},
              nbt={'id': String('minecraft:beehive'), 'bees': List[Compound]([
                  Compound({'entity_data': Compound({'id': String('minecraft:bee')}), 'ticks_in_hive': Int(0), 'min_ticks_in_hive': Int(600)})
                  for _ in range(rng.randint(1, 3))])})
        for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if t.is_air(hx + dx, FLOOR, hz + dz) and rng.random() < 0.7:
                t.set(hx + dx, GROUND, hz + dz, 'minecraft:grass_block')
                t.set(hx + dx, FLOOR, hz + dz, rng.choice(flowers))
    # honey press by the fire: honeycomb blocks, honey block, a cauldron and a campfire smoker
    t.set(C + 4, FLOOR, C + 4, 'minecraft:honeycomb_block')
    t.set(C + 4, FLOOR + 1, C + 4, 'minecraft:honey_block')
    t.set(C + 5, FLOOR, C + 4, 'minecraft:cauldron')
    t.set(C - 4, FLOOR, C + 4, 'minecraft:beehive', {'facing': 'north', 'honey_level': '5'})
    t.set(C - 4, FLOOR, C + 5, 'minecraft:honeycomb_block')
    for (fx, fz) in standard_huts(camp, sizes=(5, 6)):
        corner_feature(camp, fx, fz)
    camp.scatter(['minecraft:hay_block', 'minecraft:honeycomb_block', 'minecraft:sunflower' if False else 'minecraft:dandelion', ('minecraft:composter', {'level': '5'})], 4, avoid_radius=8)
    for pos, (bname, props, nbt) in list(t.blocks.items()):
        if bname in flowers:
            gx, gy, gz = pos
            if t.get(gx, gy - 1, gz) not in ('minecraft:grass_block', 'minecraft:dirt', 'minecraft:moss_block', 'tribalpower:march_grass', 'tribalpower:march_soil'):
                t.set(gx, gy - 1, gz, 'minecraft:grass_block')
    camp.edge_lanterns()


def camp_sigil(camp):
    """Seal-carvers: a ring of standing stones with Lore Tablets facing the hearth, candles, a carving table."""
    t, rng, i = camp.t, camp.rng, camp.i
    camp.ground(top=['minecraft:grass_block'] * 5 + ['minecraft:podzol', 'minecraft:rooted_dirt'] if not camp.march else None)
    camp.cobble = rng.choice(['minecraft:cobbled_deepslate', 'minecraft:mossy_cobblestone'])
    camp.plaza(fire_offset=(0, 3), totem_offset=(0, -3))
    stone = rng.choice(['tribalpower:march_stone', 'minecraft:polished_deepslate', 'minecraft:deepslate_bricks'])
    # eight standing stones at radius 8; the four on the diagonals carry lore tablets facing inward
    tablets = iter([0, 1, 3, 5])
    for k in range(8):
        a = k * math.tau / 8 + math.tau / 16
        sx, sz = round(C + 8 * math.cos(a)), round(C + 8 * math.sin(a))
        h = rng.choice([4, 5, 5, 6])
        t.fill(sx, FLOOR, sz, sx, FLOOR + h - 1, sz, stone)
        t.set(sx, GROUND, sz, 'minecraft:cobbled_deepslate')
        if h >= 4:
            t.set(sx, FLOOR + h - 1, sz, 'minecraft:chiseled_deepslate')
        # tablet on the face toward the centre
        dx, dz = C - sx, C - sz
        facing = ('east' if dx > 0 else 'west') if abs(dx) >= abs(dz) else ('south' if dz > 0 else 'north')
        ox, oz = DIRS[facing]
        if k % 2 == 0:
            place(t, sx + ox, FLOOR + 1, sz + oz, tablet(facing, next(tablets)))
        else:
            place(t, sx + ox, FLOOR + 1, sz + oz, banner_wall(i, facing))
        if t.is_air(sx, FLOOR + h, sz):
            t.set(sx, FLOOR + h, sz, 'minecraft:candle', {'candles': str(rng.randint(1, 4)), 'lit': 'true', 'waterlogged': 'false'})
    # carving table and seal pedestal by the hearth
    t.set(C + 3, FLOOR, C + 3, 'minecraft:stonecutter', {'facing': 'west'})
    t.set(C - 3, FLOOR, C + 3, 'minecraft:lectern', {'facing': 'east', 'has_book': 'false', 'powered': 'false'})
    t.set(C - 3, FLOOR, C - 3, 'minecraft:chiseled_deepslate')
    t.set(C - 3, FLOOR + 1, C - 3, 'minecraft:soul_lantern', {'hanging': 'false', 'waterlogged': 'false'})
    t.set(C + 3, FLOOR, C - 3, 'minecraft:chiseled_deepslate')
    t.set(C + 3, FLOOR + 1, C - 3, 'minecraft:soul_lantern', {'hanging': 'false', 'waterlogged': 'false'})
    for (fx, fz) in standard_huts(camp, count=3, sizes=(5, 6)):
        corner_feature(camp, fx, fz)
    # their own craft: a Wake Bell on a deepslate plinth, and a cairn to hold what it tolls
    t.set(C - 6, FLOOR, C + 6, 'minecraft:chiseled_deepslate')
    t.set(C - 6, FLOOR + 1, C + 6, 'tribalpower:wake_bell', {'lit': 'false'})
    t.set(C - 4, FLOOR, C + 6, 'tribalpower:pulse_cairn')
    t.set(C - 4, FLOOR + 1, C + 6, 'tribalpower:pulse_cairn')
    camp.scatter(['minecraft:cobbled_deepslate', 'minecraft:skeleton_skull' if False else 'minecraft:mossy_cobblestone', ('minecraft:barrel', {'facing': 'up', 'open': 'false'})], 4, avoid_radius=10)
    camp.edge_lanterns()


def camp_spindle(camp):
    """Loom-stitchers (March only): a crystal waystation — crystal pylons, an open pavilion, loom and cyan cloth."""
    t, rng, i = camp.t, camp.rng, camp.i
    camp.ground()
    camp.planks = 'tribalpower:march_planks'; camp.log = 'tribalpower:march_log'; camp.fence = 'minecraft:spruce_fence'
    camp.plaza(fire_offset=(0, 3), totem_offset=(0, -3))
    # four crystal pylons at the plaza rim, tall, glowing
    for (px, pz) in ((C - 5, C - 5), (C + 5, C - 5), (C - 5, C + 5), (C + 5, C + 5)):
        h = rng.randint(4, 6)
        t.set(px, GROUND, pz, 'tribalpower:march_stone')
        t.fill(px, FLOOR, pz, px, FLOOR + h - 1, pz, 'tribalpower:march_crystal')
        for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if rng.random() < 0.5 and t.is_air(px + dx, FLOOR, pz + dz):
                t.set(px + dx, FLOOR, pz + dz, 'tribalpower:march_crystal')
    # waystation pavilion north of the plaza: 7x5 open shelter with a crystal-lit roof and a lore tablet
    x0, z0 = C - 3, 3
    for (px, pz) in ((x0, z0), (x0 + 6, z0), (x0, z0 + 4), (x0 + 6, z0 + 4)):
        t.fill(px, FLOOR, pz, px, FLOOR + 3, pz, camp.log, log_props(camp.log, 'y'))
    t.fill(x0, FLOOR + 4, z0, x0 + 6, FLOOR + 4, z0 + 4, camp.planks)
    for x in range(x0 + 1, x0 + 6):
        place(t, x, FLOOR + 5, z0 + 1, slab('minecraft:spruce_slab'))
        place(t, x, FLOOR + 5, z0 + 3, slab('minecraft:spruce_slab'))
        t.set(x, FLOOR + 5, z0 + 2, 'tribalpower:march_crystal')
    t.fill(x0, GROUND, z0, x0 + 6, GROUND, z0 + 4, camp.planks)
    t.fill(x0 + 1, FLOOR, z0 + 1, x0 + 5, FLOOR + 3, z0 + 3, 'minecraft:air')
    t.fill(x0 + 1, FLOOR, z0, x0 + 5, FLOOR + 1, z0, 'tribalpower:march_stone')  # back wall
    place(t, x0 + 3, FLOOR + 1, z0 + 1, tablet('south', 8))
    t.set(x0 + 1, FLOOR, z0 + 1, 'minecraft:loom', {'facing': 'south'})
    camp.huts.append((x0 + 1, z0 + 1, x0 + 5, z0 + 3))   # the pavilion is the Weaver's hut
    t.set(x0 + 5, FLOOR, z0 + 1, 'minecraft:cyan_wool')
    t.set(x0 + 5, FLOOR + 1, z0 + 1, 'minecraft:cyan_carpet')
    place(t, x0 + 3, FLOOR + 3, z0 + 2, lantern(hanging=True))
    place(t, x0 + 6, FLOOR, z0 + 5, banner_floor(i, 'south'))
    place(t, x0, FLOOR, z0 + 5, banner_floor(i, 'south'))
    # spindle glyph on the plaza: cyan carpet cross
    for (dx, dz) in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        t.set(C + dx, FLOOR, C + dz, 'minecraft:cyan_carpet')
    t.set(C - 3, FLOOR, C, 'minecraft:air')
    camp.kin_spots = [(C - 2, C + 1, 'ELDER'), (C + 1, C + 4, 'DRUMMER'), (C + 3, C - 1, 'HUNTER'), (x0 + 2, z0 + 2, 'WEAVER')]
    # two March huts in the south corners
    camp.hut(3, S - 9, 6, 6, 'north', 'frame')
    camp.hut(S - 9, S - 9, 6, 6, 'north', 'frame')
    corner_feature(camp, 5, 5)
    corner_feature(camp, S - 6, 5)
    # their own craft: a Loom Anchor with the sixth voice standing beside it
    t.set(C + 6, FLOOR, C + 6, 'tribalpower:loom_anchor', {'lit': 'false'})
    t.set(C + 5, FLOOR, C + 6, 'tribalpower:resonance_totem_loom')
    camp.scatter(['tribalpower:march_crystal', 'minecraft:cyan_wool', 'tribalpower:march_moss', ('minecraft:barrel', {'facing': 'up', 'open': 'false'})], 6, avoid_radius=8)
    camp.edge_lanterns()


CAMP_BUILDERS = {'soil': camp_soil, 'stone': camp_stone, 'sprout': camp_sprout, 'claw': camp_claw, 'spark': camp_spark,
                 'clock': camp_clock, 'swarm': camp_swarm, 'sigil': camp_sigil, 'spindle': camp_spindle}


def build_camp(tribe_index, tribe):
    camp = Camp(tribe_index, tribe)
    CAMP_BUILDERS[tribe](camp)
    return camp.save()


# ---------------------------------------------------------------- ancestor hall

def build_hall():
    """Sunken hall: tablet gallery (west) - corridor - drum room (centre, taller, ladder shaft) - corridor - treasury (east)."""
    rng = random.Random(0xA11)
    t = Template()
    stone = 'tribalpower:march_stone'
    cobble = 'tribalpower:march_cobble'
    beam = 'tribalpower:march_log'
    H = 12        # the template ends at the surface layer so `bury` never raises a plateau over the hall
    SURFACE = 11  # template layer that lands on the old surface block (start_height -11, projected)

    def wall_block(y, ceiling):
        if y == 0:
            return rng.choice([stone, stone, 'minecraft:polished_deepslate', 'minecraft:deepslate_tiles'])
        if y == ceiling:
            return rng.choice([stone, cobble, stone])
        return rng.choice([stone, stone, stone, cobble, 'minecraft:mossy_cobblestone', 'minecraft:deepslate_bricks'])

    def room(x0, z0, x1, z1, ceiling):
        for x in range(x0, x1 + 1):
            for y in range(0, ceiling + 1):
                for z in range(z0, z1 + 1):
                    edge = x in (x0, x1) or y in (0, ceiling) or z in (z0, z1)
                    t.set(x, y, z, wall_block(y, ceiling) if edge else 'minecraft:air')
        # loom-wood beams under the ceiling and corner pillars
        for x in range(x0 + 2, x1 - 1, 3):
            t.fill(x, ceiling - 1, z0 + 1, x, ceiling - 1, z1 - 1, beam, log_props(beam, 'z'))
        for (px, pz) in ((x0 + 1, z0 + 1), (x1 - 1, z0 + 1), (x0 + 1, z1 - 1), (x1 - 1, z1 - 1)):
            t.fill(px, 1, pz, px, ceiling - 1, pz, beam, log_props(beam, 'y'))

    def corridor(x0, x1, z0, z1):
        for x in range(x0, x1 + 1):
            for y in range(0, 5):
                for z in range(z0, z1 + 1):
                    edge = y in (0, 4) or z in (z0, z1)
                    t.set(x, y, z, wall_block(y, 4) if edge else 'minecraft:air')
        # break the doorway through the room walls at both ends
        for x in (x0 - 1, x1 + 1):
            t.fill(x, 1, z0 + 1, x, 3, z1 - 1, 'minecraft:air')
        for x in range(x0, x1 + 1, 2):
            place(t, x, 1, z0 + 1, ('minecraft:soul_lantern', {'hanging': 'false', 'waterlogged': 'false'})) if rng.random() < 0.5 else None

    # west: tablet gallery 11 wide (x0..10), z 2..18, interior height 5
    room(0, 2, 10, 18, 6)
    # centre: drum room x 15..29, z 0..20, taller
    room(15, 0, 29, 20, 8)
    # east: treasury x 34..44, z 2..18
    room(34, 2, 44, 18, 6)
    corridor(11, 14, 8, 12)
    corridor(30, 33, 8, 12)

    # --- gallery: four lore tablets on the walls with candle sconces, lecterns, a reading bench
    gallery_tablets = [(1, 3, 7, 'east', 2), (1, 3, 13, 'east', 9), (5, 3, 3, 'south', 10), (5, 3, 17, 'north', 11)]
    for (x, y, z, facing, idx) in gallery_tablets:
        place(t, x, y, z, tablet(facing, idx))
        dx, dz = DIRS[facing]
        # candle pair on a deepslate plinth in front of each tablet
        t.set(x + dx, 1, z + dz, 'minecraft:polished_deepslate')
        t.set(x + dx, 2, z + dz, 'minecraft:candle', {'candles': '3', 'lit': 'true', 'waterlogged': 'false'})
    t.set(5, 1, 10, 'minecraft:lectern', {'facing': 'east', 'has_book': 'false', 'powered': 'false'})
    for z in (6, 14):
        t.set(3, 1, z, 'minecraft:spruce_slab' if False else beam, log_props(beam, 'x'))
    for (x, z) in ((3, 4), (7, 4), (3, 16), (7, 16)):
        place(t, x, 1, z, spirit_lantern())
    t.set(8, 1, 10, 'minecraft:bookshelf'); t.set(8, 2, 10, 'minecraft:bookshelf')
    t.set(8, 1, 9, 'minecraft:chiseled_bookshelf', {'facing': 'west'})

    # --- drum room: sunken dais with the Loom totem and a Drumheart, ring of note blocks, shaft to the surface
    cx, cz = 22, 10
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            t.set(cx + dx, 0, cz + dz, 'minecraft:chiseled_stone_bricks' if abs(dx) + abs(dz) < 3 else 'minecraft:polished_deepslate')
    t.set(cx, 1, cz, 'tribalpower:drumheart')
    t.set(cx, 1, cz + 4, 'tribalpower:resonance_totem_loom')
    t.set(cx, 0, cz + 4, 'minecraft:chiseled_stone_bricks')
    for (dx, dz) in ((-3, -3), (3, -3), (-3, 3), (3, 3)):
        t.set(cx + dx, 1, cz + dz, 'minecraft:note_block', {'instrument': 'basedrum', 'note': str(rng.randint(0, 24)), 'powered': 'false'})
    for (dx, dz) in ((-5, 0), (5, 0), (0, -6), (0, 6)):
        place(t, cx + dx, 1, cz + dz, spirit_lantern())
    # crystal inlays in the drum room pillars
    for (px, pz) in ((16, 1), (28, 1), (16, 19), (28, 19)):
        for y in (3, 5):
            t.set(px, y, pz, 'tribalpower:march_crystal')
    # ladder shaft from the ceiling up through the ground to a cairn on the surface
    sx, sz = cx, 4
    t.fill(sx - 1, 8, sz - 1, sx + 1, H - 1, sz + 1, 'minecraft:air')
    for y in range(8, SURFACE + 1):
        for dx in (-2, -1, 0, 1, 2):
            for dz in (-2, -1, 0, 1, 2):
                if max(abs(dx), abs(dz)) == 2:
                    t.set(sx + dx, y, sz + dz, cobble if rng.random() < 0.7 else 'minecraft:mossy_cobblestone')
    for y in range(1, SURFACE + 1):
        t.set(sx, y, sz, 'minecraft:ladder', {'facing': 'south', 'waterlogged': 'false'})
        t.set(sx, y, sz - 1, stone)
    t.fill(sx - 1, 1, sz, sx + 1, 7, sz + 1, 'minecraft:air')
    t.set(sx, 0, sz, stone)
    # surface marker: a ring of mossy stones set into the ground around the 3x3 shaft mouth, one Spirit Lantern
    for dx in (-2, -1, 0, 1, 2):
        for dz in (-2, -1, 0, 1, 2):
            if max(abs(dx), abs(dz)) == 2:
                t.set(sx + dx, SURFACE, sz + dz, rng.choice(['minecraft:mossy_cobblestone', 'minecraft:mossy_cobblestone', cobble]))
    place(t, sx + 2, SURFACE, sz + 2, spirit_lantern())
    t.fill(sx - 1, 8, sz - 1, sx + 1, SURFACE, sz + 1, 'minecraft:air')
    for y in range(8, SURFACE + 1):
        t.set(sx, y, sz - 1, stone)
        t.set(sx, y, sz, 'minecraft:ladder', {'facing': 'south', 'waterlogged': 'false'})

    # --- treasury: two loot chests behind an iron-bar gate, copper and gold hoard, decorated pots, sentinels
    for z in range(3, 18):
        if z not in (9, 10, 11):
            for y in (1, 2, 3):
                t.set(38, y, z, 'minecraft:iron_bars', {'north': 'true', 'south': 'true', 'east': 'false', 'west': 'false', 'waterlogged': 'false'})
    place(t, 42, 1, 4, loot_chest('south', rng))
    place(t, 42, 1, 16, loot_chest('north', rng))
    for (x, z, b) in ((40, 4, 'minecraft:raw_gold_block'), (41, 16, 'minecraft:copper_block'), (43, 10, 'minecraft:gold_block'),
                      (40, 16, 'minecraft:raw_copper_block'), (43, 6, 'minecraft:decorated_pot'), (43, 14, 'minecraft:decorated_pot'),
                      (41, 8, 'minecraft:decorated_pot'), (41, 12, 'minecraft:barrel')):
        if b == 'minecraft:barrel':
            t.set(x, 1, z, b, {'facing': 'up', 'open': 'false'})
        elif b == 'minecraft:decorated_pot':
            t.set(x, 1, z, b, {'facing': 'north', 'cracked': 'false', 'waterlogged': 'false'})
        else:
            t.set(x, 1, z, b)
    for (x, z) in ((36, 4), (36, 16), (42, 10)):
        place(t, x, 1, z, spirit_lantern())
    t.set(35, 1, 10, 'minecraft:skeleton_skull', {'rotation': '4', 'powered': 'false'})
    t.set(35, 0, 10, 'minecraft:chiseled_deepslate')

    # rubble across the floors
    for _ in range(40):
        x, z = rng.randint(1, 43), rng.randint(1, 19)
        if t.is_air(x, 1, z) and t.is_air(x, 2, z) and t.get(x, 0, z) not in (None, 'minecraft:air'):
            choice = rng.choice(['minecraft:cobblestone_slab', 'minecraft:mossy_cobblestone', 'minecraft:gravel', 'minecraft:cobweb', 'minecraft:moss_carpet'])
            if choice.endswith('_slab'):
                place(t, x, 1, z, slab(choice))
            else:
                t.set(x, 1, z, choice)
    # hollow sentinels: one in the gallery, one guarding the treasury gate, one in the drum room
    for (x, z) in ((5.5, 12.5), (36.5, 10.5), (18.5, 15.5)):
        bx, bz = int(x), int(z)
        t.set(bx, 1, bz, 'minecraft:air'); t.set(bx, 2, bz, 'minecraft:air')
        t.entity(x, 1, z, 'tribalpower:hollow_sentinel', PersistenceRequired=Byte(1))
    return t.save('ancestor_hall')


# ---------------------------------------------------------------- drum circle

def build_drum_circle():
    rng = random.Random(0xD2C)
    t = Template()
    S = 33
    c = S // 2
    stone = 'tribalpower:march_stone'
    cobble = 'tribalpower:march_cobble'
    # paved disc with a radial rune pattern; sub-layer of stone so the disc has a lip on slopes
    for x in range(S):
        for z in range(S):
            r = math.hypot(x - c, z - c)
            if r <= 15.5:
                t.set(x, 0, z, stone)
                a = math.atan2(z - c, x - c)
                spoke = min(abs(((a / (math.tau / 8)) % 1) - 0.0), 1 - ((a / (math.tau / 8)) % 1)) < 0.045 and r > 3
                ring = abs(r - 7) < 0.55 or abs(r - 11) < 0.55
                if spoke or ring:
                    t.set(x, 1, z, 'minecraft:deepslate_tiles' if rng.random() < 0.8 else 'minecraft:polished_deepslate')
                else:
                    t.set(x, 1, z, rng.choice(['minecraft:stone_bricks'] * 5 + [stone] * 3 + ['minecraft:cracked_stone_bricks', 'minecraft:mossy_stone_bricks']))
    t.fill(0, 2, 0, S - 1, 11, S - 1, 'minecraft:air')
    # twelve pillars with crystal inlays and Spirit Lanterns on top; low wall between pillars except at the 4 entrances
    pillars = []
    for i in range(12):
        a = i * math.tau / 12 + math.tau / 24
        px, pz = round(c + 13 * math.cos(a)), round(c + 13 * math.sin(a))
        h = rng.choice([5, 6, 6, 7])
        mat = rng.choice(['minecraft:stone_bricks', stone, 'minecraft:deepslate_bricks'])
        t.fill(px, 2, pz, px, 1 + h, pz, mat)
        t.set(px, 2, pz, 'minecraft:chiseled_stone_bricks')
        t.set(px, 3 + h // 2, pz, 'tribalpower:march_crystal')
        place(t, px, 2 + h, pz, spirit_lantern())
        pillars.append((px, pz))
        # crystal shards at the pillar foot
        for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if rng.random() < 0.35 and t.is_air(px + dx, 2, pz + dz) and t.get(px + dx, 1, pz + dz) not in (None, 'minecraft:air'):
                t.set(px + dx, 2, pz + dz, 'tribalpower:march_crystal')
    # low parapet wall on the rim with gaps on the four cardinal entrances
    for x in range(S):
        for z in range(S):
            r = math.hypot(x - c, z - c)
            if 14.2 <= r <= 15.5 and t.is_air(x, 2, z):
                if abs(x - c) <= 1 or abs(z - c) <= 1:
                    continue
                t.set(x, 2, z, 'minecraft:stone_brick_wall', {'up': 'true', 'north': 'none', 'south': 'none', 'east': 'none', 'west': 'none', 'waterlogged': 'false'})
    # entrance steps and gate posts
    for (x, z, f) in ((c, 0, 'south'), (c, S - 1, 'north'), (0, c, 'east'), (S - 1, c, 'west')):
        dx, dz = DIRS[f]
        for k in (-1, 0, 1):
            ex, ez = (x + k, z) if f in ('north', 'south') else (x, z + k)
            if t.get(ex, 1, ez):
                place(t, ex, 2, ez, stairs('minecraft:stone_brick_stairs', OPPOSITE[f]))
        for k in (-2, 2):
            gx, gz = (x + k + dx, z + dz) if f in ('north', 'south') else (x + dx, z + k + dz)
            t.fill(gx, 2, gz, gx, 4, gz, 'minecraft:deepslate_bricks')
            place(t, gx, 5, gz, lantern())
    # seating stones ring at radius 8
    for i in range(20):
        a = i * math.tau / 20 + 0.08
        sx, sz = round(c + 8.5 * math.cos(a)), round(c + 8.5 * math.sin(a))
        if t.is_air(sx, 2, sz):
            place(t, sx, 2, sz, slab(rng.choice(['minecraft:stone_brick_slab', 'minecraft:cobblestone_slab', 'minecraft:polished_deepslate_slab'])))
    # the silent drum on a two-step 7x7 dais with stairs on the four sides
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            m = max(abs(dx), abs(dz))
            t.set(c + dx, 1, c + dz, 'minecraft:polished_deepslate' if m == 3 else 'minecraft:chiseled_stone_bricks')
            if m <= 2:
                t.set(c + dx, 2, c + dz, 'minecraft:polished_deepslate' if m == 2 else 'minecraft:deepslate_tiles')
            if m <= 1:
                t.set(c + dx, 3, c + dz, 'minecraft:chiseled_stone_bricks' if (dx or dz) else 'minecraft:polished_deepslate')
    for (dx, dz, f) in ((0, -1, 'north'), (0, 1, 'south'), (-1, 0, 'west'), (1, 0, 'east')):
        place(t, c + dx * 3, 2, c + dz * 3, stairs('minecraft:polished_deepslate_stairs', OPPOSITE[f]))
        place(t, c + dx * 2, 3, c + dz * 2, stairs('minecraft:polished_deepslate_stairs', OPPOSITE[f]))
    t.set(c, 4, c, 'tribalpower:silent_drum', nbt={'id': String('tribalpower:silent_drum')})
    for (dx, dz) in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        t.set(c + dx, 4, c + dz, 'minecraft:candle', {'candles': '2', 'lit': 'true', 'waterlogged': 'false'})
    # Design 3.1 §15: the Drum Circle music disc sits in a chest just off the dais.
    if t.is_air(c + 6, 2, c):
        place(t, c + 6, 2, c, loot_chest('west', rng, 'tribalpower:chests/drum_circle'))
    # a lore stele north of the dais and a second south
    for (sx, sz, facing, idx) in ((c + 4, c - 10, 'south', 4), (c - 4, c + 10, 'north', 6)):
        t.fill(sx, 2, sz, sx, 4, sz, stone)
        t.set(sx, 4, sz, 'minecraft:chiseled_deepslate')
        dx, dz = DIRS[facing]
        place(t, sx + dx, 3, sz + dz, tablet(facing, idx))
    # bone litter and ash for atmosphere
    for _ in range(14):
        x, z = rng.randint(4, S - 5), rng.randint(4, S - 5)
        if 3.5 < math.hypot(x - c, z - c) < 12 and t.is_air(x, 2, z):
            t.set(x, 2, z, rng.choice(['minecraft:bone_block', 'minecraft:gravel', 'minecraft:skeleton_skull', 'minecraft:cobweb']),
                  {'rotation': str(rng.randint(0, 15)), 'powered': 'false'} if False else None)
    for pos, (bname, props, nbt) in list(t.blocks.items()):
        if bname == 'minecraft:skeleton_skull':
            t.set(*pos, bname, {'rotation': str(rng.randint(0, 15)), 'powered': 'false'})
        elif bname == 'minecraft:bone_block':
            t.set(*pos, bname, {'axis': rng.choice(['x', 'z'])})
    return t.save('drum_circle')


# ---------------------------------------------------------------- crystal spire

def build_spire():
    rng = random.Random(0x5B1E)
    t = Template()
    S = 21
    c = S // 2
    crystal = 'tribalpower:march_crystal'
    stone = 'tribalpower:march_stone'
    H = 46
    for x in range(S):
        for z in range(S):
            t.set(x, 0, z, rng.choice(['tribalpower:march_soil', stone]))
            t.set(x, 1, z, rng.choice(['tribalpower:march_soil', 'tribalpower:march_grass', 'tribalpower:march_grass', stone]))
    t.fill(0, 2, 0, S - 1, 12, S - 1, 'minecraft:air')
    # rocky mound at the base: stone dome radius 5
    for x in range(S):
        for z in range(S):
            d = math.hypot(x - c, z - c)
            for y in range(2, 5):
                if d <= 5.5 - (y - 2) * 1.6:
                    t.set(x, y, z, rng.choice([stone, stone, 'tribalpower:march_cobble', 'tribalpower:march_moss']))
    # main spire: tapering, slowly twisting column of crystal with stone veins
    for y in range(2, H):
        f = (y - 2) / (H - 2)
        r = max(0.9, 4.0 * (1 - f) ** 0.75)
        ox = 0.8 * math.sin(f * math.pi)
        oz = 0.8 * (1 - math.cos(f * math.pi))
        for x in range(S):
            for z in range(S):
                if math.hypot(x - c - ox, z - c - oz) <= r:
                    # stone veins only low down, pure crystal above
                    t.set(x, y, z, stone if (y < 9 and rng.random() < 0.25) else crystal)
    # crystal spurs jutting out at three heights
    for (y0, n, length) in ((6, 5, 4), (14, 4, 3), (24, 3, 2)):
        for i in range(n):
            a = i * math.tau / n + y0 * 0.3
            for k in range(1, length + 1):
                sx, sz = round(c + (2.5 + k) * math.cos(a)), round(c + (2.5 + k) * math.sin(a))
                sy = y0 + k // 2
                if 0 <= sx < S and 0 <= sz < S and t.get(sx, sy, sz) in (None, 'minecraft:air'):
                    t.set(sx, sy, sz, crystal)
    # three lesser spires around the base
    for (sx, sz, h) in ((c - 6, c + 5, 11), (c + 6, c + 4, 15), (c + 3, c - 7, 9)):
        for y in range(2, 2 + h):
            r = max(0.4, 1.6 * (1 - (y - 2) / h))
            for x in range(sx - 2, sx + 3):
                for z in range(sz - 2, sz + 3):
                    if 0 <= x < S and 0 <= z < S and math.hypot(x - sx, z - sz) <= r:
                        t.set(x, y, z, crystal)
    # Loom-stitcher waystation: open pavilion at the south-west foot with the spindle hearth
    planks = 'tribalpower:march_planks'
    logb = 'tribalpower:march_log'
    x0, z0 = 1, S - 8
    t.fill(x0, 1, z0, x0 + 6, 1, z0 + 6, planks)
    t.fill(x0, 2, z0, x0 + 6, 5, z0 + 6, 'minecraft:air')
    for (px, pz) in ((x0, z0), (x0 + 6, z0), (x0, z0 + 6), (x0 + 6, z0 + 6)):
        t.fill(px, 2, pz, px, 5, pz, logb, log_props(logb, 'y'))
    t.fill(x0, 6, z0, x0 + 6, 6, z0 + 6, planks)
    for k in range(1, 3):
        for x in range(x0 + k, x0 + 7 - k):
            for z in range(z0 + k, z0 + 7 - k):
                edge = x in (x0 + k, x0 + 6 - k) or z in (z0 + k, z0 + 6 - k)
                place(t, x, 6 + k, z, slab('minecraft:spruce_slab')) if edge else t.set(x, 6 + k, z, crystal if k == 2 else planks)
    t.fill(x0 + 1, 2, z0, x0 + 5, 3, z0, 'minecraft:cyan_wool')   # north cloth wall
    t.fill(x0, 2, z0 + 1, x0, 3, z0 + 5, 'minecraft:cyan_wool')   # west cloth wall
    place(t, x0 + 3, 2, z0 + 3, hearth_block(8))
    t.set(x0 + 3, 1, z0 + 3, 'minecraft:chiseled_stone_bricks')
    t.set(x0 + 1, 2, z0 + 5, 'minecraft:loom', {'facing': 'east'})
    t.set(x0 + 5, 2, z0 + 1, 'minecraft:barrel', {'facing': 'up', 'open': 'false'})
    bed(t, x0 + 1, 2, z0 + 2, 'cyan', 'north')
    place(t, x0 + 3, 5, z0 + 3, lantern(hanging=True))
    place(t, x0 + 3, 3, z0 + 1, tablet('south', 8))
    place(t, x0 + 2, 2, z0 - 1, banner_wall(8, 'north'))
    place(t, x0 + 7, 2, z0 + 6, banner_floor(8, 'east'))
    place(t, x0 + 7, 2, z0, banner_floor(8, 'east'))
    t.set(S - 3, 2, S - 3, 'tribalpower:resonance_totem_loom')
    t.set(S - 3, 1, S - 3, 'minecraft:chiseled_stone_bricks')
    place(t, S - 3, 2, 2, campfire())
    t.set(S - 3, 1, 2, 'tribalpower:march_cobble')
    for (lx, lz) in ((2, 2), (S - 3, c + 3), (c - 6, 2)):
        if t.is_air(lx, 2, lz) and t.is_air(lx, 3, lz):
            place(t, lx, 2, lz, fence('minecraft:spruce_fence'))
            place(t, lx, 3, lz, spirit_lantern())
    for pos, (bname, props, nbt) in list(t.blocks.items()):
        if bname == 'tribalpower:march_cobble' and pos[1] == 1 and pos != (S - 3, 1, 2):
            pass
    # crystal shards scattered around the base
    for _ in range(12):
        x, z = rng.randint(1, S - 2), rng.randint(1, S - 2)
        if t.is_air(x, 2, z) and t.get(x, 1, z) not in (None, 'minecraft:air', planks) and math.hypot(x - c, z - c) > 5:
            t.fill(x, 2, z, x, 2 + rng.randint(0, 1), z, crystal)
    t.kin(x0 + 4, 2, z0 + 4, 8, 'ELDER')
    t.kin(x0 + 5, 2, z0 + 5, 8, 'WEAVER')
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


def structure_set(name, spacing, separation, salt, exclude=None, chunks=3):
    placement = {'type': 'minecraft:random_spread', 'spacing': spacing, 'separation': separation, 'salt': salt}
    if exclude:
        placement['exclusion_zone'] = {'other_set': f'tribalpower:{exclude}', 'chunk_count': chunks}
    write_json(f'worldgen/structure_set/{name}.json', {
        'structures': [{'structure': f'tribalpower:{name}', 'weight': 1}],
        'placement': placement,
    })


def biome_tag(name, values):
    write_json(f'tags/worldgen/biome/has_structure/{name}.json', {'replace': False, 'values': values})


def loot_table():
    def item(name, lo, hi, weight=1):
        return {'type': 'minecraft:item', 'name': name, 'weight': weight,
                'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]}
    write_json('loot_table/chests/drum_circle.json', {
        'type': 'minecraft:chest',
        'pools': [
            {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'tribalpower:music_disc_drum_circle'}]},
            {'rolls': {'type': 'minecraft:uniform', 'min': 1, 'max': 3}, 'entries': [
                item('tribalpower:bone_chime', 1, 1, 2),
                item('minecraft:bone', 2, 6, 6),
                item('minecraft:candle', 1, 3, 4),
                item('tribalpower:echo_shard', 1, 4, 3),
            ]},
        ],
    })
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
    # One structure set per camp (the GameTests expect a set per template).  Sets with the same spacing but
    # different salts can land in the same chunk, so tribes that share a biome exclude their neighbour in this
    # chain (steppe: soil/sprout/spark/swarm; highlands + is_mountain: stone/claw/sigil; crystal fields: clock/spindle).
    chain = ['soil', 'sprout', 'spark', 'swarm', 'stone', 'claw', 'sigil', 'clock', 'spindle']
    for i, tribe in enumerate(TRIBES):
        sizes[f'tribe_camp_{tribe}'] = build_camp(i, tribe)
        # minY = start + firstAir - 1, so -1 puts template y=1 (the surface layer) on the old surface block
        structure_json(f'tribe_camp_{tribe}', f'tribe_camp_{tribe}', 'beard_thin', -1)
        k = chain.index(tribe)
        structure_set(f'tribe_camp_{tribe}', 40, 24, 0x7A1B00 + i * 7919, exclude=f'tribe_camp_{chain[k - 1]}' if k else None)
        biome_tag(f'tribe_camp_{tribe}', BIOMES[tribe])
    sizes['ancestor_hall'] = build_hall()
    # template layer 11 is the surface block (minY = -11 + firstAir - 1 = surface - 11), only the cairn shows
    structure_json('ancestor_hall', 'ancestor_hall', 'bury', -11, step='underground_structures')
    structure_set('ancestor_hall', 48, 28, 0xA11CE5, exclude='tribe_camp_stone')
    biome_tag('ancestor_hall', ['tribalpower:march_steppe', 'tribalpower:march_highlands'])
    sizes['drum_circle'] = build_drum_circle()
    structure_json('drum_circle', 'drum_circle', 'beard_thin', -1)
    structure_set('drum_circle', 64, 40, 0xD2C1E7, exclude='ancestor_hall', chunks=4)
    biome_tag('drum_circle', ['tribalpower:march_highlands'])
    sizes['crystal_spire'] = build_spire()
    structure_json('crystal_spire', 'crystal_spire', 'beard_thin', -1)
    structure_set('crystal_spire', 56, 32, 0x5B1E93, exclude='tribe_camp_spindle')
    biome_tag('crystal_spire', ['tribalpower:march_crystal_fields'])
    loot_table()
    for name, size in sizes.items():
        print(f'{name}: {size}')


if __name__ == '__main__':
    main()
