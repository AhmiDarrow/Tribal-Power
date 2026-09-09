"""Authoritative recipes, progression and text for the Living Lattice expansion."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / 'src/main/resources/data'
ASSET = ROOT / 'src/main/resources/assets/tribalpower'


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def shaped(name, pattern, key):
    write(DATA / f'tribalpower/recipe/{name}.json', {'type': 'minecraft:crafting_shaped', 'category': 'equipment',
          'pattern': pattern, 'key': {k: {'tag' if v.startswith('#') else 'item': v.lstrip('#')} for k, v in key.items()},
          'result': {'id': 'tribalpower:' + name, 'count': 1}})


def process(name, station, feed, output, element, seconds, pulse, count=1):
    write(DATA / f'tribalpower/recipe/lattice/{name}.json', {'type':'tribalpower:lattice', 'station': 'echo_' + station,
          'ingredient': {'tag' if feed.startswith('#') else 'item': feed.lstrip('#')}, 'result': {'id':output,'count':count},
          'attunement': element, 'seconds': seconds, 'pulse_per_second': pulse})


def main():
    shaped('pulse_resonator', ['ICI','RSR','ICI'], {'I':'minecraft:iron_ingot','C':'minecraft:amethyst_shard','R':'tribalpower:copper_resonator','S':'tribalpower:spirit_shard'})
    shaped('lattice_tuner', [' C ', ' R ', ' R '], {'C':'tribalpower:spirit_shard','R':'tribalpower:copper_resonator'})
    shaped('spirit_cistern', ['CGC','G G','CGC'], {'C':'minecraft:copper_ingot','G':'#c:glass_blocks'})
    shaped('pulse_adapter', ['CRC','RMR','CCC'], {'C':'minecraft:copper_ingot','R':'minecraft:redstone','M':'tribalpower:resonant_core'})
    for kind, middle in [('item','minecraft:hopper'),('fluid','tribalpower:spirit_cistern')]:
        shaped(kind+'_relay', [' C ','RMR',' I '], {'C':'tribalpower:copper_resonator','R':'tribalpower:ritual_chalk','M':middle,'I':'tribalpower:manifested_ingot'})
        shaped('longreach_'+kind+'_relay', [' C ','RMR',' C '], {'C':'tribalpower:resonant_core','R':'minecraft:ender_pearl','M':'tribalpower:'+kind+'_relay'})
        shaped('astral_'+kind+'_relay', [' C ','RMR',' C '], {'C':'tribalpower:march_crystal','R':'tribalpower:spirit_seal','M':'tribalpower:longreach_'+kind+'_relay'})
    shaped('waystone_compass', [' I ','ICI',' I '], {'I':'tribalpower:manifested_ingot','C':'minecraft:compass'})
    shaped('horizon_compass', [' R ','RCR',' R '], {'R':'tribalpower:resonant_core','C':'tribalpower:waystone_compass'})
    shaped('astral_compass', [' R ','SCS',' R '], {'R':'tribalpower:march_crystal','S':'tribalpower:spirit_seal','C':'tribalpower:horizon_compass'})
    utility_blocks = ['item_relay','fluid_relay','longreach_item_relay','longreach_fluid_relay','astral_item_relay','astral_fluid_relay','pulse_adapter','spirit_cistern']
    for name in utility_blocks + ['resonance_totem_loom', 'echo_unweave']:
        write(DATA/f'tribalpower/loot_table/blocks/{name}.json', {'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'tribalpower:'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    for feed in ['minecraft:cobblestone', 'minecraft:stone', 'minecraft:cobbled_deepslate', 'tribalpower:march_stone', 'tribalpower:march_cobble']:
        process('echo_from_' + feed.split(':')[1], 'shatter', feed, 'tribalpower:echo_shard', 'earth', 2, 8)
    for metal in ['iron', 'gold', 'copper']:
        process(metal + '_grit', 'shatter', '#c:raw_materials/' + metal, 'tribalpower:' + metal + '_grit', 'earth', 4, 10, 2)
        for kind, time in [('smelting', 200), ('blasting', 100)]:
            write(DATA / f'tribalpower/recipe/{metal}_grit_{kind}.json', {'type': 'minecraft:' + kind,
                  'category': 'misc', 'ingredient': {'item': 'tribalpower:' + metal + '_grit'},
                  'result': {'id': 'minecraft:' + metal + '_ingot'}, 'experience': 0.35, 'cookingtime': time})
    process('attuned_echo', 'attune', 'tribalpower:echo_shard', 'tribalpower:attuned_echo', 'fire', 3, 10)
    process('bound_echo', 'bind', 'tribalpower:attuned_echo', 'tribalpower:bound_echo', 'water', 4, 12)
    process('manifested_ingot', 'manifest', 'tribalpower:bound_echo', 'tribalpower:manifested_ingot', 'spirit', 5, 16)
    process('spiritweave', 'bind', '#minecraft:wool', 'tribalpower:spiritweave', 'water', 4, 12)
    process('resonant_core', 'manifest', 'tribalpower:manifested_ingot', 'tribalpower:resonant_core', 'spirit', 8, 20)
    process('spirit_shard', 'attune', 'minecraft:amethyst_shard', 'tribalpower:spirit_shard', 'fire', 3, 8)
    process('quartz', 'shatter', 'minecraft:diorite', 'minecraft:quartz', 'earth', 4, 10)
    process('clay', 'bind', 'minecraft:dirt', 'minecraft:clay_ball', 'water', 3, 8, 4)
    # The sixth voice — Echo Unweave reverses the lattice (Loom attunement).
    process('unweave_manifested_ingot', 'unweave', 'tribalpower:manifested_ingot', 'tribalpower:bound_echo', 'loom', 4, 12, 2)
    process('unweave_bound_echo', 'unweave', 'tribalpower:bound_echo', 'tribalpower:attuned_echo', 'loom', 3, 10, 2)
    process('unweave_attuned_echo', 'unweave', 'tribalpower:attuned_echo', 'tribalpower:echo_shard', 'loom', 2, 8, 2)
    process('unweave_spiritweave', 'unweave', 'tribalpower:spiritweave', 'minecraft:white_wool', 'loom', 3, 10, 2)
    process('unweave_resonant_core', 'unweave', 'tribalpower:resonant_core', 'tribalpower:manifested_ingot', 'loom', 6, 16, 3)
    process('unweave_spiritgear', 'unweave', '#tribalpower:spiritgear_tools', 'tribalpower:manifested_ingot', 'loom', 8, 16)
    write(DATA / 'tribalpower/tags/item/spiritgear_tools.json', {'replace': False, 'values': ['tribalpower:spiritgear_' + t for t in ['pickaxe', 'axe', 'shovel', 'blade']]})
    shaped('resonance_totem_loom', ['CTC', 'PHP', 'CTC'], {'C':'tribalpower:march_crystal', 'T':'tribalpower:loom_thread', 'H':'tribalpower:unsung_heart', 'P':'tribalpower:march_planks'})
    shaped('echo_unweave', ['STS', 'CPC', 'SSS'], {'S':'tribalpower:march_stone', 'T':'tribalpower:loom_thread', 'C':'tribalpower:copper_resonator', 'P':'tribalpower:spirit_shard'})
    write(DATA / 'tribalpower/recipe/loom_seal.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
          'ingredients': [{'item': 'tribalpower:blank_seal'}, {'item': 'tribalpower:loom_thread'}, {'item': 'tribalpower:spirit_shard'}],
          'result': {'count': 1, 'id': 'tribalpower:loom_seal'}})
    shaped('greater_pulse_cell', [' C ', 'IPI', ' C '], {'C':'tribalpower:resonant_core', 'I':'tribalpower:manifested_ingot', 'P':'tribalpower:pulse_cell'})
    shaped('spirit_staff', [' SC', ' RI', 'R  '], {'S':'tribalpower:spirit_seal', 'C':'tribalpower:resonant_core', 'R':'#minecraft:logs', 'I':'tribalpower:manifested_ingot'})
    shaped('wayfarer_satchel', ['WWW', 'CDC', 'WWW'], {'W':'tribalpower:spiritweave', 'C':'tribalpower:resonant_core', 'D':'tribalpower:deep_cache'})
    shaped('resonance_maul', ['ICI', ' R ', ' R '], {'I':'tribalpower:manifested_ingot', 'C':'tribalpower:resonant_core', 'R':'tribalpower:copper_resonator'})
    shaped('ritual_brazier', ['I I', 'CRC', 'SSS'], {'I':'tribalpower:manifested_ingot', 'C':'minecraft:copper_ingot', 'R':'tribalpower:resonant_core', 'S':'minecraft:stone'})
    for name, pattern in [('hood',['WWW','W W']),('robe',['W W','WCW','WWW']),('leggings',['WCW','W W','W W']),('boots',['W W','W W'])]:
        key = {'W':'tribalpower:spiritweave'}
        if any('C' in p for p in pattern): key['C'] = 'tribalpower:resonant_core'
        shaped('spiritweave_' + name, pattern, key)
    for tag in ['mineable/pickaxe', 'needs_stone_tool']:
        path = DATA / ('minecraft/tags/block/' + tag + '.json')
        obj = json.loads(path.read_text()) if path.exists() else {'replace': False, 'values': []}
        for name in ['ritual_brazier', 'echo_unweave'] + utility_blocks:
            if 'tribalpower:'+name not in obj['values']: obj['values'].append('tribalpower:'+name)
        write(path, obj)
    write(DATA / 'tribalpower/loot_table/blocks/ritual_brazier.json', {'type':'minecraft:block','pools':[{'rolls':1,
          'entries':[{'type':'minecraft:item','name':'tribalpower:ritual_brazier'}], 'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    for metal in ['iron', 'gold', 'copper']:
        write(DATA / f'c/tags/item/dusts/{metal}.json', {'replace':False,'values':['tribalpower:' + metal + '_grit']})
    names = {'spiritweave':'Spiritweave', 'resonant_core':'Resonant Core', 'greater_pulse_cell':'Greater Pulse Cell',
             'spirit_staff':'Sixfold Staff', 'wayfarer_satchel':'Wayfarer Satchel', 'resonance_maul':'Resonance Maul',
             'spiritweave_hood':'Spiritweave Hood', 'spiritweave_robe':'Spiritweave Robe',
             'spiritweave_leggings':'Spiritweave Leggings', 'spiritweave_boots':'Spiritweave Boots',
             'iron_grit':'Iron Grit','gold_grit':'Gold Grit','copper_grit':'Copper Grit'}
    lang = json.loads((ASSET/'lang/en_us.json').read_text(encoding='utf-8'))
    lang.update({'item.tribalpower.' + k:v for k,v in names.items()})
    lang.update({'block.tribalpower.'+k:k.replace('_',' ').title() for k in utility_blocks})
    lang.update({'item.tribalpower.'+k:k.replace('_',' ').title() for k in ['lattice_tuner','waystone_compass','horizon_compass','astral_compass']})
    lang.update({
        'message.tribalpower.redstone.locked':'Silenced by redstone.',
        'gui.tribalpower.lattice_recipes':'Lattice Processing',
        'gui.tribalpower.recipe_cost':'%s seconds · %s Pulse total',
        'message.tribalpower.pulse_resonator.status':'Resonator %s/%s Pulse · %s per second · %s voices',
        'message.tribalpower.pulse_resonator.need_fuel':'Resonator %s/%s · seat an Echo catalyst and place 2 different totems nearby.',
        'message.tribalpower.pulse_resonator.fueled':'Catalyst seated (%s) · Pulse %s/%s. The echo is not consumed.',
        'message.tribalpower.pulse_resonator.full_fuel':'Lift the seated catalyst with sneak + empty hand first.',
        'message.tribalpower.pulse_resonator.removed_fuel':'Catalyst lifted. Old fuel can also be recovered this way.',
        'message.tribalpower.relay.unlinked':'Use the Lattice Tuner: mark a destination face, then link this relay.',
        'message.tribalpower.relay.unloaded':'Destination is not loaded. Relays never force-load chunks.',
        'message.tribalpower.relay.paused':'Relay silenced by redstone.',
        'message.tribalpower.relay.pulse':'Relay needs Pulse within 8 blocks.',
        'message.tribalpower.relay.working':'Transfer complete · comparator 15',
        'message.tribalpower.relay.waiting':'Linked · waiting for source below or destination capacity · comparator 1',
        'message.tribalpower.tuner.marked':'Destination face marked. Use the tuner on a relay to link it.',
        'message.tribalpower.tuner.linked':'Wireless route bound. Source goes below the relay; redstone pauses transfer.',
        'message.tribalpower.tuner.range':'Outside this relay tier: local 32 blocks, longreach 128, astral cross-dimensional.',
        'item.tribalpower.lattice_tuner.desc':'Mark a target face, then use on a relay. Sneak-use to replace the mark.',
        'message.tribalpower.adapter.status':'Pulse Adapter %s/%s FE · 1 Pulse → 100 FE · redstone pauses',
        'message.tribalpower.cistern.status':'Spirit Cistern %s/%s mB · buckets and standard fluid pipes accepted',
        'message.tribalpower.waystone.bound':'Waypoint bound. Redstone at its floor locks arrival.',
        'message.tribalpower.waystone.unbound':'Sneak-use the top of solid ground to bind a waypoint.',
        'message.tribalpower.waystone.blocked':'Arrival is obstructed, wet, or locked by redstone. No Pulse spent.',
        'message.tribalpower.waystone.range':'Beyond this compass tier. Horizon crosses distance; Astral crosses dimensions.',
        'item.tribalpower.waystone_compass.desc':'Bind: sneak-use ground. Travel: use. Local 128 blocks / 20 Pulse; Horizon any distance / 40; Astral dimensions / 100.'})
    lang.update({
        'block.tribalpower.ritual_brazier':'Ritual Brazier', 'gui.tribalpower.pulse':'Pulse · %s',
        'item.tribalpower.spirit_staff.desc':'Use to cast (%s Pulse). Sneak-use to change spell.',
        'item.tribalpower.wayfarer_satchel.desc':'Opens your 54-slot Deep Cache. Visit The March, or spend 5 Pulse per opening.',
        'item.tribalpower.resonance_maul.desc':'Sneak-use a stone face: excavate 3×3. 8 Pulse per block.',
        'item.tribalpower.spiritweave_armor.desc':'Worn pieces draw 2 Pulse every 4 seconds for their spirit boon.',
        'spell.tribalpower.earth':'Earthbind', 'spell.tribalpower.fire':'Ember Lance', 'spell.tribalpower.water':'Mending Tide',
        'spell.tribalpower.air':'Windstep', 'spell.tribalpower.spirit':'Spirit Sight',
        'spell.tribalpower.loom':'Tether', 'spell.tribalpower.loom.stitch':'Stitch', 'attunement.tribalpower.loom':'Loom',
        'item.tribalpower.spirit_staff.stitch':'Sneak-use with no target: Stitch, blink 6 blocks forward (%s Pulse).',
        'message.tribalpower.staff.stitch_blocked':'No open air to stitch through.',
        'message.tribalpower.brazier.tension':'Tension · threading 2 Pulse into carried cells every 2 seconds',
        'item.tribalpower.loom_thread':'Loom Thread', 'item.tribalpower.unsung_heart':'Unsung Heart', 'item.tribalpower.loom_seal':'Loom Seal',
        'item.tribalpower.loom_thread.desc':'A strand of the thread itself. Found in Ancestor Halls, torn from The Unsung.',
        'item.tribalpower.unsung_heart.desc':'The stilled drum of The Unsung. Crafts the Loom totem.',
        'block.tribalpower.resonance_totem_loom':'Resonance Totem (Loom)', 'block.tribalpower.echo_unweave':'Echo Unweave',
        'message.tribalpower.staff.selected':'Attuned: %s', 'message.tribalpower.staff.no_target':'Aim at a hostile within 18 blocks.',
        'message.tribalpower.station.idle':'Seat feed in the left slot', 'message.tribalpower.station.working':'Working · %s beats',
        'message.tribalpower.station.paused':'Paused by redstone', 'message.tribalpower.station.full':'Output is full',
        'message.tribalpower.station.attunement':'Needs its matching totem', 'message.tribalpower.station.pulse':'Waiting for Spirit Pulse',
        'message.tribalpower.brazier.occupied':'Sneak with an empty hand to lift the seated seal first.',
        'message.tribalpower.brazier.empty':'Seat an elemental seal. Match its totem and supply Pulse within 8 blocks.',
        'message.tribalpower.brazier.active':'Ritual sustained · 8 Pulse every 2 seconds · radius 6',
        'message.tribalpower.brazier.waiting':'Waiting for players, matching totem and Pulse. Redstone pauses the rite.'})
    write(ASSET/'lang/en_us.json', lang)
    milestones = [('root','bone_chime','A beat in the quiet','Craft a Bone Chime. The first rhythm belongs to you.'),
                  ('hum','drumheart','The camp answers','Make a Drumheart and charge a Pulse Cell.'),
                  ('shatter','echo_shatter','Teach stone to sing','Build an Earth-powered Echo Shatter.'),
                  ('attune','echo_attune','A warmer note','Build the Fire-powered Echo Attune.'),
                  ('bind','echo_bind','Thread that remembers','Build Echo Bind for echoes and Spiritweave.'),
                  ('manifest','manifested_ingot','Metal with a memory','Walk an echo through all four stages.'),
                  ('core','resonant_core','A living lattice','Manifest a Resonant Core for advanced craft.'),
                  ('staff','spirit_staff','Six voices, one hand','Craft the Sixfold Staff and carry charged cells.'),
                  ('ritual','ritual_brazier','A hearth worth returning to','Build a sustained ritual for your camp.'),
                  ('march','gate_drum','Beyond the veil','Build a Gate Drum to reach The March.')]
    parent = None
    for key, item, title, desc in milestones:
        obj = {'display': {'icon':{'id':'tribalpower:' + item}, 'title':title, 'description':desc, 'frame':'task',
               'show_toast':True,'announce_to_chat':False,'hidden':False},
               'criteria':{'acquired':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':['tribalpower:'+item]}]}}}}
        if parent: obj['parent'] = 'tribalpower:journey/' + parent
        else: obj['display']['background'] = 'tribalpower:textures/block/march_stone.png'
        write(DATA / f'tribalpower/advancement/journey/{key}.json', obj)
        parent = key
    print('Living Lattice recipes, tags, advancements and language generated.')


if __name__ == '__main__': main()
