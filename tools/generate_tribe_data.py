"""Generates the JSON assets/data for the Nine Tribes (design 3.0 §2): blockstates, models, loot tables, recipe,
advancements and tag entries. Textures are painted by tools/art/tribes.py. Idempotent; safe to re-run.
Run: python3 tools/generate_tribe_data.py [project root]
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent / 'art'))
from glyphs import TRIBES  # noqa: E402

ROOT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/tribalpower'
DATA = ROOT / 'src/main/resources/data/tribalpower'
MC_TAGS = ROOT / 'src/main/resources/data/minecraft/tags/block/mineable'


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + '\n')


def faces(tex, uv=None):
    f = {d: {'texture': tex} for d in ('north', 'south', 'east', 'west', 'up', 'down')}
    if uv:
        for v in f.values():
            v['uv'] = uv
    return f


def cube(fr, to, tex, tint=None):
    e = {'from': fr, 'to': to, 'faces': faces(tex)}
    if tint is not None:
        for v in e['faces'].values():
            v['tintindex'] = tint
    return e


def hearth_model():
    return {
        'parent': 'minecraft:block/block',
        'render_type': 'minecraft:cutout',
        'textures': {
            'particle': 'tribalpower:block/tribe_hearth',
            'side': 'tribalpower:block/tribe_hearth',
            'top': 'tribalpower:block/tribe_hearth_top',
            'stone': 'tribalpower:block/loom_stone',
            'trim': 'tribalpower:block/loom_copper',
            'embers': 'tribalpower:block/tribe_hearth_embers',
        },
        'elements': [
            {'from': [1, 0, 1], 'to': [15, 5, 15], 'faces': {
                'north': {'texture': '#side'}, 'south': {'texture': '#side'}, 'east': {'texture': '#side'}, 'west': {'texture': '#side'},
                'up': {'texture': '#top'}, 'down': {'texture': '#stone'}}},
            cube([3, 5, 3], [13, 6, 13], '#trim'),
            cube([3, 6, 3], [5, 9, 13], '#stone'),
            cube([11, 6, 3], [13, 9, 13], '#stone'),
            cube([5, 6, 3], [11, 9, 5], '#stone'),
            cube([5, 6, 11], [11, 9, 13], '#stone'),
            {'from': [5, 6, 5], 'to': [11, 7.5, 11], 'faces': {'up': {'texture': '#top', 'uv': [4, 4, 12, 12]}}},
            {'from': [5, 7.5, 5], 'to': [11, 8, 11], 'faces': {'up': {'texture': '#embers', 'uv': [4, 4, 12, 12], 'tintindex': 0}}},
            {'from': [8, 6, 5], 'to': [8, 9.5, 11], 'faces': {
                'east': {'texture': '#embers', 'uv': [4, 4, 12, 12], 'tintindex': 0}, 'west': {'texture': '#embers', 'uv': [4, 4, 12, 12], 'tintindex': 0}}},
            {'from': [5, 6, 8], 'to': [11, 9.5, 8], 'faces': {
                'north': {'texture': '#embers', 'uv': [4, 4, 12, 12], 'tintindex': 0}, 'south': {'texture': '#embers', 'uv': [4, 4, 12, 12], 'tintindex': 0}}},
        ],
    }


def banner_floor(tribe):
    return {
        'parent': 'minecraft:block/block',
        'render_type': 'minecraft:cutout',
        'textures': {'particle': f'tribalpower:block/tribe_banner_{tribe}', 'cloth': f'tribalpower:block/tribe_banner_{tribe}',
                     'wood': 'tribalpower:block/loom_wood', 'trim': 'tribalpower:block/loom_copper'},
        'elements': [
            cube([7, 0, 7], [9, 16, 9], '#wood'),
            cube([6, 15, 6], [10, 16, 10], '#trim'),
            cube([3, 14, 9], [13, 15, 10], '#wood'),
            {'from': [4, 1, 9.5], 'to': [12, 14.5, 10.5], 'faces': {
                'north': {'texture': '#cloth', 'uv': [0, 0, 16, 16]}, 'south': {'texture': '#cloth', 'uv': [16, 0, 0, 16]},
                'east': {'texture': '#cloth', 'uv': [14, 0, 16, 16]}, 'west': {'texture': '#cloth', 'uv': [0, 0, 2, 16]},
                'up': {'texture': '#cloth', 'uv': [0, 0, 16, 2]}, 'down': {'texture': '#cloth', 'uv': [0, 14, 16, 16]}}},
        ],
    }


def banner_wall(tribe):
    return {
        'parent': 'minecraft:block/block',
        'render_type': 'minecraft:cutout',
        'textures': {'particle': f'tribalpower:block/tribe_banner_{tribe}', 'cloth': f'tribalpower:block/tribe_banner_{tribe}',
                     'wood': 'tribalpower:block/loom_wood', 'trim': 'tribalpower:block/loom_copper'},
        'elements': [
            cube([2, 14, 13], [14, 16, 16], '#wood'),
            cube([2, 13, 13.5], [14, 14, 14.5], '#trim'),
            {'from': [3, 0.5, 13], 'to': [13, 13.5, 14], 'faces': {
                'north': {'texture': '#cloth', 'uv': [0, 0, 16, 16]}, 'south': {'texture': '#cloth', 'uv': [16, 0, 0, 16]},
                'east': {'texture': '#cloth', 'uv': [14, 0, 16, 16]}, 'west': {'texture': '#cloth', 'uv': [0, 0, 2, 16]},
                'up': {'texture': '#cloth', 'uv': [0, 0, 16, 2]}, 'down': {'texture': '#cloth', 'uv': [0, 14, 16, 16]}}},
        ],
    }


def kinship_model(tribe):
    return {
        'parent': 'minecraft:block/block',
        'textures': {
            'particle': f'tribalpower:block/kinship_totem_{tribe}', 'side': f'tribalpower:block/kinship_totem_{tribe}',
            'wood': 'tribalpower:block/loom_wood', 'stone': 'tribalpower:block/loom_stone',
            'trim': 'tribalpower:block/loom_copper', 'light': 'tribalpower:block/loom_light', 'top': 'tribalpower:block/kinship_totem_top'},
        'elements': [
            cube([3, 0, 3], [13, 2, 13], '#stone'),
            cube([5, 2, 5], [11, 14, 11], '#wood'),
            {'from': [3, 5, 3], 'to': [13, 13, 13], 'faces': {
                'north': {'texture': '#side'}, 'south': {'texture': '#side'}, 'east': {'texture': '#side'}, 'west': {'texture': '#side'},
                'up': {'texture': '#top'}, 'down': {'texture': '#wood'}}},
            cube([2, 13, 2], [14, 15, 14], '#trim'),
            cube([6, 15, 6], [10, 16, 10], '#light'),
        ],
    }


def overrides(base_model, tribe_models):
    model = dict(base_model)
    model['overrides'] = [{'predicate': {'tribalpower:tribe': i / 10 - 0.005}, 'model': tribe_models[i]} for i in range(1, len(TRIBES))]
    return model


def loot_self(name, copy_tribe):
    entry = {'type': 'minecraft:item', 'name': f'tribalpower:{name}'}
    if copy_tribe:
        entry['functions'] = [{'function': 'minecraft:copy_custom_data', 'source': 'block_entity',
                               'ops': [{'source': 'Tribe', 'target': 'Tribe', 'op': 'replace'}]}]
    return {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [entry], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]}


def advancement(icon, title, desc, parent, frame='task'):
    adv = {'display': {'icon': {'id': icon}, 'title': title, 'description': desc, 'frame': frame,
                       'show_toast': True, 'announce_to_chat': frame != 'task', 'hidden': False},
           'criteria': {'done': {'trigger': 'minecraft:impossible'}}}
    if parent:
        adv['parent'] = parent
    return adv


def add_tag(path, values):
    tag = json.loads(path.read_text()) if path.exists() else {'replace': False, 'values': []}
    for v in values:
        if v not in tag['values']:
            tag['values'].append(v)
    write(path, tag)


def main():
    # hearth
    write(ASSETS / 'blockstates/tribe_hearth.json', {'variants': {'': {'model': 'tribalpower:block/tribe_hearth'}}})
    write(ASSETS / 'models/block/tribe_hearth.json', hearth_model())
    write(ASSETS / 'models/item/tribe_hearth.json', {'parent': 'minecraft:item/generated', 'textures': {
        'layer0': 'tribalpower:item/tribe_hearth', 'layer1': 'tribalpower:item/tribe_hearth_embers'}})
    write(DATA / 'loot_table/blocks/tribe_hearth.json', loot_self('tribe_hearth', True))

    # banner
    variants = {}
    for i, tribe in enumerate(TRIBES):
        write(ASSETS / f'models/block/tribe_banner_{tribe}.json', banner_floor(tribe))
        write(ASSETS / f'models/block/tribe_banner_wall_{tribe}.json', banner_wall(tribe))
        write(ASSETS / f'models/item/tribe_banner_{tribe}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'tribalpower:item/tribe_banner_{tribe}'}})
        for facing, rot in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
            for wall in ('false', 'true'):
                model = f'tribalpower:block/tribe_banner_{"wall_" if wall == "true" else ""}{tribe}'
                v = {'model': model}
                if rot:
                    v['y'] = rot
                variants[f'facing={facing},tribe={i},wall={wall}'] = v
    write(ASSETS / 'blockstates/tribe_banner.json', {'variants': variants})
    write(ASSETS / 'models/item/tribe_banner.json', overrides(
        {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'tribalpower:item/tribe_banner_soil'}},
        [f'tribalpower:item/tribe_banner_{t}' for t in TRIBES]))
    write(DATA / 'loot_table/blocks/tribe_banner.json', loot_self('tribe_banner', False))

    # kinship totem
    write(ASSETS / 'blockstates/kinship_totem.json', {'variants': {f'tribe={i}': {'model': f'tribalpower:block/kinship_totem_{t}'} for i, t in enumerate(TRIBES)}})
    for tribe in TRIBES:
        write(ASSETS / f'models/block/kinship_totem_{tribe}.json', kinship_model(tribe))
        write(ASSETS / f'models/item/kinship_totem_{tribe}.json', {'parent': f'tribalpower:block/kinship_totem_{tribe}'})
    write(ASSETS / 'models/item/kinship_totem.json', overrides(
        {'parent': 'tribalpower:block/kinship_totem_soil'}, [f'tribalpower:item/kinship_totem_{t}' for t in TRIBES]))
    write(DATA / 'loot_table/blocks/kinship_totem.json', loot_self('kinship_totem', True))
    write(DATA / 'recipe/kinship_totem.json', {'type': 'tribalpower:kinship_totem', 'category': 'misc'})

    # tribe mark
    for tribe in TRIBES:
        write(ASSETS / f'models/item/tribe_mark_{tribe}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'tribalpower:item/tribe_mark_{tribe}'}})
    write(ASSETS / 'models/item/tribe_mark.json', overrides(
        {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'tribalpower:item/tribe_mark_soil'}},
        [f'tribalpower:item/tribe_mark_{t}' for t in TRIBES]))

    # spawn eggs
    for role in ('elder', 'drummer', 'hunter', 'weaver'):
        write(ASSETS / f'models/item/tribal_kin_{role}_spawn_egg.json', {'parent': 'minecraft:item/template_spawn_egg'})

    # tags
    add_tag(MC_TAGS / 'pickaxe.json', ['tribalpower:tribe_hearth'])
    add_tag(MC_TAGS / 'axe.json', ['tribalpower:kinship_totem', 'tribalpower:tribe_banner'])

    # advancements
    write(DATA / 'advancement/tribes/offering.json', advancement('tribalpower:tribe_hearth', 'Kept warm',
          'Make an offering at a Tribe Hearth.', 'tribalpower:journey/root'))
    write(DATA / 'advancement/tribes/friend.json', advancement('tribalpower:tribe_banner', 'On good terms',
          'Reach Friend standing with a tribe.', 'tribalpower:tribes/offering'))
    write(DATA / 'advancement/tribes/mark.json', advancement('tribalpower:tribe_mark', 'Nine agreeing',
          'Earn a Tribe Mark from an Elder at Voice standing.', 'tribalpower:tribes/friend', 'goal'))
    print('tribes: data written')


if __name__ == '__main__':
    main()
