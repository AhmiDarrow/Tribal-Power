"""Reproducible, authored 32px pixel art and vanilla cuboid models for the Loom projects.

Replaces the old procedural placeholders from native drawing definitions; no upscaling,
filters, external texture packs or shader dependencies. Run with an optional project path.
"""
from pathlib import Path
import json
import math
import random
import sys
from PIL import Image, ImageDraw, ImageFont

INK = (17, 26, 34, 255)
WOOD = (69, 49, 44, 255)
STONE = (48, 65, 72, 255)
COPPER = (171, 115, 73, 255)
GOLD = (214, 179, 111, 255)
BONE = (225, 217, 189, 255)
TEAL = (86, 183, 167, 255)
LIGHT = (177, 239, 212, 255)
ELEMENTS = {'earth':(132,173,94,255), 'fire':(235,132,73,255), 'water':(95,184,217,255),
            'air':(192,218,194,255), 'spirit':(166,136,221,255), 'soil':(132,173,94,255),
            'stone':(159,173,181,255), 'sprout':(107,190,111,255), 'claw':(221,151,117,255),
            'spark':(232,194,97,255), 'clock':COPPER, 'swarm':(230,191,111,255),
            'sigil':(166,136,221,255), 'spindle':TEAL, 'rewoven':LIGHT}


def shade(c, amount): return tuple(max(0,min(255,x+amount)) for x in c[:3]) + (c[3] if len(c)>3 else 255,)
def blank(size=(32,32)): return Image.new('RGBA', size, (0,0,0,0))
def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n', encoding='utf-8')
def save(path, img):
    path.parent.mkdir(parents=True, exist_ok=True); img.save(path)


def material(name, color=STONE):
    im = blank(); d = ImageDraw.Draw(im); d.rectangle((0,0,31,31), fill=color)
    rng = random.Random(name)
    if any(t in name for t in ['wood','log','planks','post','bench','frame','barrel','cache','drum']):
        for x in range(0,32,8):
            d.line((x,0,x,31),fill=shade(color,-20)); d.line((x+1,0,x+1,31),fill=shade(color,13))
            for _ in range(6):
                xx=x+rng.randint(2,6); y=rng.randint(0,26)
                d.line((xx,y,xx,y+rng.randint(2,6)),fill=shade(color,rng.choice([-9,8,14])))
        if 'planks' in name:
            for x in range(0,32,8): d.line((x,(x*3)%24+4,x+7,(x*3)%24+4),fill=shade(color,-28))
    else:
        for _ in range(70):
            x,y=rng.randrange(32),rng.randrange(32)
            d.rectangle((x,y,min(31,x+rng.randrange(1,4)),min(31,y+1)),fill=shade(color,rng.choice([-9,-6,5,8])))
        if 'cobble' in name:
            for y in range(0,32,8):
                for x in range(-4 if y%16 else 0,32,10):
                    d.rectangle((x,y,x+9,y+7),outline=shade(color,-22))
                    d.line((x+1,y+1,x+8,y+1),fill=shade(color,13))
    return im


def glyph(d, name, c, x=16, y=16, size=7):
    # Distinct silhouettes remain readable in inventory, on seals, and across a camp.
    if name in ('earth','soil','stone'):
        d.line([(x-size,y+4),(x,y-size),(x+size,y+4),(x-size,y+4)], fill=c,width=2)
        d.line((x-5,y+7,x+5,y+7),fill=c,width=2)
    elif name in ('fire','spark'):
        d.polygon([(x+2,y-size),(x-5,y+1),(x-1,y+1),(x-3,y+size),(x+6,y-2),(x+1,y-2)], fill=c)
    elif name in ('water','spindle'):
        for off in [-3,3]: d.line([(x-size,y+off),(x-3,y+off-2),(x+3,y+off+2),(x+size,y+off)],fill=c,width=2)
    elif name in ('air','swarm'):
        d.line([(x-size,y-3),(x+5,y-3),(x+7,y-5),(x+5,y-7)],fill=c,width=2)
        d.line([(x-size+2,y+3),(x+4,y+3),(x+6,y+5),(x+4,y+7)],fill=c,width=2)
    elif name=='sprout':
        d.line((x,y+7,x,y-3),fill=c,width=2);d.polygon([(x,y-1),(x-7,y-5),(x-6,y),(x,y+2)],fill=c);d.polygon([(x,y-3),(x+6,y-7),(x+6,y-2),(x,y)],fill=c)
    elif name=='claw':
        for a in [-4,0,4]:d.line([(x+a-2,y-6),(x+a,y-1),(x+a-2,y+6)],fill=c,width=2)
    elif name=='clock':
        d.ellipse((x-7,y-7,x+7,y+7),outline=c,width=2);d.line([(x,y-5),(x,y),(x+4,y+2)],fill=c,width=2)
    else:
        d.line([(x,y-size),(x+size,y),(x,y+size),(x-size,y),(x,y-size)],fill=c,width=2)
        d.rectangle((x-2,y-2,x+2,y+2),fill=c)


def tile(name):
    element = next((k for k in ELEMENTS if name.endswith('_'+k)), 'spirit')
    element = {'echo_shatter':'earth','echo_attune':'fire','echo_bind':'water','echo_manifest':'spirit',
               'ley_collector':'air','lattice_conductor':'clock','pulse_adapter':'spark','fluid_relay':'water','item_relay':'air'}.get(name,element)
    c=ELEMENTS[element]
    if 'fluid_relay' in name: element='water';c=ELEMENTS[element]
    elif 'item_relay' in name: element='air';c=ELEMENTS[element]
    elif name=='spirit_cistern': element='water';c=ELEMENTS[element]
    if name.startswith('notch_'):
        im=blank();glyph(ImageDraw.Draw(im),element,c);return im
    if name in ('spirit_reed','echo_bloom','ley_thistle','march_leaf'):
        im=blank();d=ImageDraw.Draw(im)
        for x,h in [(10,10),(17,4),(23,13)]:
            d.line((x,31,x,h+3),fill=shade(TEAL,-40),width=2)
            d.polygon([(x,h+10),(x-6,h+5),(x-5,h+10),(x,h+15)],fill=TEAL)
            d.polygon([(x,h+7),(x+5,h+2),(x+5,h+7),(x,h+11)],fill=shade(TEAL,15))
            if name!='spirit_reed':
                d.polygon([(x,h-2),(x+4,h+2),(x,h+6),(x-4,h+2)],fill=ELEMENTS['spirit'] if name=='echo_bloom' else LIGHT)
        return im
    if name in ('march_leaves','march_moss','march_grass'):
        im=material(name,(40,76,68,255));d=ImageDraw.Draw(im);rng=random.Random(name)
        for _ in range(34):
            x,y=rng.randrange(30),rng.randrange(30)
            d.line([(x,y+2),(x+1,y),(x+3,y)],fill=rng.choice([TEAL,shade(TEAL,-30),shade(TEAL,-55)]))
        return im
    woody=any(t in name for t in ['log','planks','post','bench','frame','barrel','cache','drum','totem'])
    im=material(name,(75,63,56,255) if name=='march_soil' else WOOD if woody else STONE);d=ImageDraw.Draw(im)
    if name.startswith('march_') and name not in ('march_crystal','march_ore'):
        return im
    if name in ('march_crystal','march_ore'):
        for x,y in [(5,7),(20,6),(12,22),(27,24)]:
            d.polygon([(x,y-4),(x+3,y),(x,y+4),(x-3,y)],fill=TEAL)
            d.line((x,y-3,x,y+2),fill=LIGHT)
        return im
    d.rectangle((0,0,31,2),fill=COPPER);d.line((0,0,31,0),fill=GOLD)
    d.rectangle((0,29,31,31),fill=shade(COPPER,-30));d.line((0,29,31,29),fill=COPPER)
    for x in [3,28]:
        for y in [5,26]:d.rectangle((x-1,y-1,x+1,y+1),fill=INK);d.point((x,y-1),fill=GOLD)
    if name.endswith('_top') or 'drum' in name:
        d.ellipse((4,4,27,27),fill=shade(BONE,-28),outline=COPPER,width=2)
        d.ellipse((7,7,24,24),outline=shade(BONE,-49));glyph(d,'spirit',shade(COPPER,-35),size=5)
    elif 'cache' in name:
        d.line((0,11,31,11),fill=INK,width=2)
        d.rectangle((12,9,20,20),fill=INK,outline=COPPER)
        d.polygon([(16,11),(19,15),(16,19),(13,15)],fill=c if name=='deep_cache' else GOLD)
    elif 'frame' in name:
        for a in range(6,27,4):d.line((5,a,26,a),fill=BONE);d.line((a,5,a,26),fill=shade(BONE,-48))
    else:
        d.rectangle((7,6,24,25),fill=INK,outline=shade(COPPER,-32));glyph(d,element,c,size=6)
        if 'relay' in name:
            rank=3 if name.startswith('astral') else 2 if name.startswith('longreach') else 1
            for i in range(rank):d.rectangle((12+i*4,26,14+i*4,27),fill=ELEMENTS['spirit'] if rank==3 else GOLD)
    return im


def icon(name):
    im=blank();d=ImageDraw.Draw(im)
    element=next((k for k in ELEMENTS if name.endswith('_'+k) or name.startswith(k+'_')), 'spirit')
    c=ELEMENTS[element]
    c={'echo_shard':BONE,'attuned_echo':ELEMENTS['fire'],'spirit_shard':TEAL,'iron_grit':ELEMENTS['stone'],
       'gold_grit':GOLD,'copper_grit':COPPER}.get(name,c)
    def line(points,color,width=2):d.line(points,fill=color,width=width)
    if 'compass' in name:
        c=TEAL if name=='waystone_compass' else GOLD if name=='horizon_compass' else ELEMENTS['spirit']
        d.ellipse((3,3,28,28),fill=INK,outline=COPPER,width=2);d.ellipse((6,6,25,25),outline=GOLD)
        d.polygon([(16,5),(21,21),(16,18),(11,21)],fill=c);line((16,7,16,17),BONE,1)
        for x,y in [(16,1),(16,30),(1,16),(30,16)]:d.point((x,y),fill=GOLD)
    elif name=='ritual_chalk':
        d.polygon([(5,22),(20,4),(26,5),(27,11),(12,28),(6,27)],fill=INK)
        d.polygon([(7,22),(21,6),(24,7),(25,11),(11,26),(8,26)],fill=BONE);line([(8,22),(21,7)],shade(BONE,20),2)
        line([(12,20),(17,24)],COPPER,2)
    elif name=='lattice_tuner':
        line([(7,28),(18,13)],INK,6);line([(7,27),(18,13)],COPPER,3)
        line([(18,15),(12,9),(18,2)],GOLD,3);line([(18,15),(26,9),(25,3)],GOLD,3)
        d.polygon([(20,4),(23,8),(20,12),(17,8)],fill=TEAL)
    elif name.endswith('_grit'):
        for x,y in [(10,12),(20,11),(15,21),(24,23),(6,23)]:
            d.polygon([(x,y-4),(x+4,y),(x+2,y+4),(x-4,y+2)],fill=INK)
            d.polygon([(x,y-2),(x+2,y),(x+1,y+2),(x-2,y+1)],fill=c);d.point((x,y-1),fill=shade(c,40))
    elif 'token' in name or 'seal' in name or name=='strand_banner_pattern':
        d.polygon([(8,2),(23,2),(29,8),(29,23),(23,29),(8,29),(2,23),(2,8)],fill=INK)
        d.polygon([(9,4),(22,4),(27,9),(27,22),(22,27),(9,27),(4,22),(4,9)],fill=shade(BONE,-45) if 'seal' in name else shade(COPPER,-40),outline=GOLD)
        d.rectangle((7,7,24,24),fill=INK)
        if name=='blank_seal':d.rectangle((12,12,19,19),outline=shade(BONE,-80))
        else:glyph(d,element,c)
    elif 'codex' in name or name in ('island_charter','codex_page'):
        d.polygon([(6,3),(26,3),(26,26),(22,29),(4,29),(4,6)],fill=INK)
        d.rectangle((7,4,24,26),fill=shade(TEAL,-60));d.rectangle((5,5,8,27),fill=COPPER)
        d.line((9,27,23,27),fill=BONE,width=2);d.rectangle((11,6,22,23),outline=COPPER)
        glyph(d,'spirit',GOLD,x=17,y=14,size=4)
    elif name=='hub_key':
        d.ellipse((3,2,17,16),fill=COPPER,outline=INK,width=2);d.ellipse((7,6,13,12),fill=INK)
        line([(14,13),(26,25)],INK,6);line([(14,13),(26,25)],GOLD,3);line([(20,19),(23,16)],GOLD,3);line([(24,23),(27,20)],GOLD,3)
    elif 'mesh' in name:
        d.rectangle((3,3,28,28),fill=INK,outline=COPPER,width=2)
        for a in range(6,28,4):line((a,5,a,26),BONE if 'string' in name else STONE,1);line((5,a,26,a),BONE if 'string' in name else STONE,1)
    elif 'cell' in name:
        d.polygon([(11,2),(21,2),(25,7),(25,25),(21,29),(11,29),(7,25),(7,7)],fill=INK)
        d.rectangle((9,7,23,24),fill=shade(TEAL,-65),outline=COPPER,width=2)
        d.rectangle((12,8,20,23),fill=TEAL);d.rectangle((13,9,15,22),fill=LIGHT)
        for y in [3,25]:d.rectangle((10,y,22,y+3),fill=COPPER);line((11,y,21,y),GOLD,1)
        if name=='greater_pulse_cell':glyph(d,'spirit',INK,x=16,y=15,size=4)
    elif any(t in name for t in ['staff','maul','pickaxe','axe','shovel','blade','hammer','crook']):
        line([(6,27),(22,6)],INK,6);line([(6,27),(22,6)],WOOD,4);line([(6,26),(21,6)],COPPER,1)
        for x,y in [(9,23),(12,19),(15,15)]:line([(x-1,y-1),(x+2,y+1)],GOLD,2)
        if 'staff' in name or 'crook' in name:
            d.ellipse((15,1,28,13),outline=COPPER,width=3);d.polygon([(21,1),(26,6),(21,11),(17,6)],fill=TEAL);line((20,3,20,7),LIGHT,2)
        elif 'blade' in name:
            d.polygon([(12,18),(23,2),(28,1),(28,6),(17,21)],fill=INK);d.polygon([(14,18),(24,4),(26,3),(26,6),(17,19)],fill=TEAL);line([(25,4),(16,17)],LIGHT,1);line([(10,16),(19,23)],GOLD,3)
        elif 'pickaxe' in name:
            d.polygon([(8,4),(15,1),(23,4),(29,12),(27,15),(20,9),(15,7),(9,7)],fill=INK);line([(10,5),(15,3),(22,6),(27,12)],TEAL,4);line([(11,4),(16,3),(22,6)],LIGHT,1)
        elif 'shovel' in name:
            d.polygon([(19,3),(26,2),(29,9),(23,14),(17,8)],fill=INK);d.polygon([(20,4),(25,4),(27,9),(23,12),(19,8)],fill=TEAL)
        else:
            d.polygon([(11,3),(22,1),(29,9),(23,16),(12,9)],fill=INK);d.polygon([(13,4),(21,3),(27,9),(22,13),(14,8)],fill=TEAL);line([(14,4),(21,3),(26,8)],LIGHT,2)
    elif name.startswith('spiritweave_'):
        if 'hood' in name:
            d.polygon([(7,4),(24,4),(28,12),(26,25),(21,25),(21,14),(11,14),(11,25),(5,25),(3,12)],fill=INK)
            d.polygon([(8,6),(23,6),(25,13),(23,22),(21,12),(11,12),(8,22),(5,13)],fill=TEAL)
            line((9,7,22,7),GOLD,2)
        elif 'boots' in name:
            for x in [4,18]:d.rectangle((x,6,x+8,25),fill=shade(TEAL,-25),outline=INK,width=2);d.rectangle((x,24,x+10,28),fill=WOOD,outline=INK);line((x+2,12,x+6,12),GOLD,2)
        elif 'leggings' in name:
            d.rectangle((5,3,26,12),fill=shade(TEAL,-25),outline=INK,width=2)
            for x in [5,18]:d.rectangle((x,10,x+8,28),fill=shade(TEAL,-25),outline=INK,width=2);line((x+2,12,x+2,26),COPPER,1)
        else:
            d.polygon([(9,3),(13,6),(18,6),(22,3),(29,9),(25,15),(23,12),(24,29),(7,29),(8,12),(5,15),(1,9)],fill=INK)
            d.polygon([(9,5),(13,8),(18,8),(22,5),(26,9),(22,12),(22,26),(9,26),(10,11),(5,10)],fill=shade(TEAL,-25))
            line([(12,9),(16,14),(20,9)],GOLD,2);line((9,20,22,20),COPPER,3)
    elif name=='spiritweave':
        d.polygon([(4,8),(23,3),(28,23),(8,29)],fill=INK)
        d.polygon([(6,9),(22,5),(26,22),(9,26)],fill=shade(TEAL,-30))
        for y in [10,15,20]:line([(8,y),(22,y-3)],COPPER,1)
        for x in [11,16,21]:line([(x,9),(x+3,22)],TEAL,1)
    elif 'satchel' in name:
        if 'satchel' in name: d.arc((7,1,24,20),180,360,fill=COPPER,width=3)
        d.rectangle((5,10,27,28),fill=shade(TEAL,-45),outline=INK,width=2)
        d.polygon([(6,10),(26,10),(24,19),(9,19)],fill=TEAL,outline=COPPER)
        d.rectangle((14,17,18,22),fill=GOLD)
    elif 'ingot' in name or name=='copper_resonator':
        d.polygon([(3,12),(9,6),(25,6),(29,16),(23,23),(5,23)],fill=INK)
        d.polygon([(5,13),(10,8),(23,8),(26,15),(22,20),(6,20)],fill=COPPER if 'copper' in name else GOLD)
        line([(6,12),(11,9),(22,9)],BONE,2);line((7,17,23,17),shade(COPPER,-25),2)
    elif name=='bone_chime':
        line((5,6,26,6),COPPER,3)
        for x,h in [(8,26),(16,22),(24,27)]:line((x,7,x,h),INK,5);line((x,8,x,h-1),BONE,3);line((x-1,10,x-1,h-2),shade(BONE,-36),1)
    elif any(t in name for t in ['yarn','thread','cord','knot','filament','lint']):
        for a in range(5):
            y=7+a*4;line([(6,y+3),(10,y),(21,y),(26,y+3),(21,y+6),(10,y+6),(6,y+3)],INK,4)
            line([(7,y+3),(11,y+1),(21,y+1),(25,y+3),(21,y+5),(11,y+5)],TEAL if 'void' in name else BONE,2)
        line([(6,23),(3,29)],BONE,2)
    else:
        # Faceted intermediates gain a binding ring only once the Echo has been bound.
        d.polygon([(15,2),(25,10),(27,22),(16,29),(5,24),(5,12)],fill=INK)
        d.polygon([(15,4),(23,11),(24,21),(16,26),(8,22),(7,13)],fill=c)
        d.polygon([(15,4),(15,18),(7,13)],fill=shade(c,36));d.polygon([(15,18),(24,21),(16,26)],fill=shade(c,-40))
        if any(t in name for t in ['bound','core','fragment']): d.ellipse((3,12,28,23),outline=GOLD,width=2)
        d.rectangle((11,9,13,12),fill=BONE)
    return im


def cuboid(lo,hi,tex='side'):
    return {'from':lo,'to':hi,'faces':{f:{'texture':'#'+tex} for f in ['north','south','east','west','up','down']}}


def model(asset,ns,name):
    common='wood' if any(t in name for t in ['drum','bench','totem','cache','frame','barrel']) else 'stone'
    textures={'particle':f'{ns}:block/{name}', 'side':f'{ns}:block/{name}',
              'wood':f'{ns}:block/loom_wood','stone':f'{ns}:block/loom_stone','trim':f'{ns}:block/loom_copper',
              'light':f'{ns}:block/loom_light','top':f'{ns}:block/{name}_top'}
    parts=[]
    def box(lo,hi,t=common):parts.append(cuboid(lo,hi,t))
    if 'totem' in name:
        box([3,0,3],[13,2,13],'stone');box([5,2,5],[11,14,11],'wood');box([3,6,3],[13,12,13],'side')
        box([2,13,2],[14,15,14],'trim');box([6,15,6],[10,16,10],'light')
    elif name in ('drumheart','gate_drum','tension_barrel'):
        box([3,0,3],[13,2,13],'wood');box([2,2,2],[14,13,14],'side');box([1,3,1],[15,4,15],'trim')
        box([1,12,1],[15,14,15],'trim');box([2,14,2],[14,15,14],'top')
    elif name in ('song_bench','loomframe'):
        for x in [1,12]:
            for z in [1,12]:box([x,0,z],[x+3,11,z+3],'wood')
        box([0,10,0],[16,13,16],'side');box([2,13,2],[14,14,14],'top')
        if name=='song_bench':
            for x in [2,12]:box([x,14,3],[x+2,16,13],'trim')
    elif name in ('ancestral_cache','deep_cache'):
        box([1,0,1],[15,11,15],'side');box([0.5,11,0.5],[15.5,14,15.5],'wood')
        box([1,11,1],[15,12,15],'trim');box([6,8,0],[10,13,1],'light' if name=='deep_cache' else 'trim')
    elif name in ('rite_pedestal','ritual_brazier'):
        box([2,0,2],[14,2,14],'stone');box([5,2,5],[11,10,11],'side');box([1,10,1],[15,12,15],'trim')
        box([3,12,3],[13,13,13],'light')
        if name=='ritual_brazier':
            for x,z in [(2,2),(12,2),(2,12),(12,12)]:box([x,12,z],[x+2,16,z+2],'trim')
    elif 'relay' in name or name in ('pulse_adapter','spirit_cistern'):
        box([1,0,1],[15,3,15],'stone');box([3,3,3],[13,11,13],'side');box([2,11,2],[14,13,14],'trim')
        if name=='spirit_cistern':box([4,4,2],[12,10,3],'light')
        else:
            box([7,13,7],[9,16,9],'light')
            for x,z in [(2,2),(12,2),(2,12),(12,12)]:box([x,12,z],[x+2,15,z+2],'trim')
    elif name in ('ley_collector','pulse_resonator','lattice_conductor'):
        box([1,0,1],[15,3,15],'stone');box([3,3,3],[13,7,13],'side')
        for x in [2,12]:box([x,7,4],[x+2,14,12],'trim')
        box([6,7,6],[10,14,10],'light');box([3,14,3],[13,15,13],'trim')
    elif name.startswith('echo_') and name not in ('echo_bloom',):
        box([0,0,0],[16,3,16],'stone');box([1,3,1],[15,12,15],'side');box([0,11,0],[16,13,16],'trim')
        if name=='echo_shatter':
            box([4,13,4],[12,16,12],'stone');box([6,13,0],[10,15,4],'trim')
        elif name=='echo_attune':
            box([3,13,3],[13,14,13],'light');box([1,13,1],[3,16,15],'trim');box([13,13,1],[15,16,15],'trim')
        elif name=='echo_bind':
            for z in [3,7,11]:box([2,13,z],[14,14,z+1],'trim')
        else:box([5,13,5],[11,16,11],'light')
    elif name=='spirit_door':
        box([2,0,5],[5,16,11],'stone');box([11,0,5],[14,16,11],'stone');box([5,13,5],[11,16,11],'trim');box([7,2,7],[9,13,9],'light')
    else:return
    write(asset/f'models/block/{name}.json', {'parent':'minecraft:block/block','textures':textures,'elements':parts})
    write(asset/f'models/item/{name}.json', {'parent':f'{ns}:block/{name}'})
    if name=='pulse_resonator':
        on={'parent':f'{ns}:block/{name}','textures':{'light':f'{ns}:block/loom_light'}}
        write(asset/'models/block/pulse_resonator_on.json',on)
    if not (asset/f'blockstates/{name}.json').exists():write(asset/f'blockstates/{name}.json',{'variants':{'':{'model':f'{ns}:block/{name}'}}})


def armor(asset):
    for layer in [1,2]:
        im=blank((64,32));d=ImageDraw.Draw(im)
        # Correct vanilla humanoid armor UV canvas, with woven hems and a copper sash.
        d.rectangle((0,0,63,31),fill=shade(TEAL,-77))
        for y in range(0,32,4):d.line((0,y,63,y),fill=shade(TEAL,-68))
        for x in [0,8,16,24,32,40,48,56]:d.line((x,0,x,31),fill=shade(TEAL,-90))
        for y in [7,19,29]:d.line((0,y,63,y),fill=COPPER);d.line((0,y+1,63,y+1),fill=GOLD)
        glyph(d,'spirit',TEAL,x=24,y=23,size=3)
        # Open the front of the hood so the player's face remains visible.
        if layer==1:d.rectangle((9,10,14,15),fill=(0,0,0,0))
        save(asset/f'textures/models/armor/spiritweave_layer_{layer}.png',im)


def creatures(asset):
    for name in ['march_walker','spirit_wisp']:
        im=blank((64,64));d=ImageDraw.Draw(im);rng=random.Random(name)
        base=(54,67,65,255) if name=='march_walker' else (55,137,131,255)
        d.rectangle((0,0,63,63),fill=base)
        for _ in range(230):
            x,y=rng.randrange(64),rng.randrange(64);d.rectangle((x,y,x+1,y+2),fill=shade(base,rng.choice([-12,8,16])))
        if name=='march_walker':
            # Head front UV (7..15,7..15): luminous eyes and a carved brow.
            d.line((8,8,13,8),fill=COPPER)
            for x in [8,12]:d.rectangle((x,10,x+1,11),fill=LIGHT)
            d.rectangle((48,0,63,18),fill=BONE)
            d.rectangle((48,20,63,40),fill=shade(TEAL,-15))
            for y in [23,29,35]:d.line((0,y,45,y),fill=shade(TEAL,-55))
            for x in [9,22,35]:d.line([(x,29),(x+3,34),(x,39)],fill=COPPER,width=1)
            d.rectangle((0,56,11,58),fill=WOOD)
        else:
            for x in [0,6,12,18]:
                d.rectangle((x,6,x+5,11),fill=TEAL,outline=LIGHT)
            d.rectangle((24,0,39,9),fill=BONE)
            for x in [8,10]:d.point((x,8),fill=INK)
        save(asset/f'textures/entity/{name}.png',im)


def overhaul(project):
    if (project/'mods').is_dir():assets=list((project/'mods').glob('*/src/main/resources/assets/*'))
    else:assets=list((project/'src/main/resources/assets').glob('*'))
    catalog=[]
    for asset in assets:
        ns=asset.name
        if not (asset/'textures').exists():continue
        block_names={p.stem for p in (asset/'textures/block').glob('*.png')}
        if ns=='tribalpower':block_names.update(['ritual_brazier','item_relay','fluid_relay','longreach_item_relay','longreach_fluid_relay','astral_item_relay','astral_fluid_relay','pulse_adapter','spirit_cistern'])
        for name in sorted(block_names):
            if name.endswith('_top') or name.startswith('loom_') and name not in ('loomframe',):continue
            im=tile(name);save(asset/f'textures/block/{name}.png',im);catalog.append((ns+'/'+name,im))
            if not name.startswith('notch_'):
                save(asset/f'textures/block/{name}_top.png',tile(name+'_top'))
                model(asset,ns,name)
        for name,col in [('loom_wood',WOOD),('loom_stone',STONE),('loom_copper',COPPER)]:save(asset/f'textures/block/{name}.png',material(name,col))
        # Four slow frames, softly breathing rather than flashing.
        animated=blank((32,128))
        for i,delta in enumerate([0,12,22,12]):
            frame=material('crystal',shade(TEAL,delta));d=ImageDraw.Draw(frame)
            for x in range(2,32,8):d.line((x,0,x+4,31),fill=shade(LIGHT,delta-15),width=2)
            animated.paste(frame,(0,i*32))
        save(asset/'textures/block/loom_light.png',animated)
        write(asset/'textures/block/loom_light.png.mcmeta',{'animation':{'frametime':12,'interpolate':True}})
        item_names={p.stem for p in (asset/'textures/item').glob('*.png')}
        if ns=='tribalpower':item_names.update(['spiritweave','resonant_core','greater_pulse_cell','spirit_staff','wayfarer_satchel','resonance_maul','spiritweave_hood','spiritweave_robe','spiritweave_leggings','spiritweave_boots','iron_grit','gold_grit','copper_grit','lattice_tuner','waystone_compass','horizon_compass','astral_compass'])
        for name in sorted(item_names):
            im=icon(name);save(asset/f'textures/item/{name}.png',im);catalog.append((ns+'/'+name,im))
            parent='handheld' if any(k in name for k in ['staff','maul','pickaxe','axe','shovel','blade','hammer','crook']) else 'generated'
            write(asset/f'models/item/{name}.json',{'parent':'minecraft:item/'+parent,'textures':{'layer0':f'{ns}:item/{name}'}})
        if ns=='tribalpower':armor(asset);creatures(asset)
    # Development contact sheet only; never shipped in a jar.
    cols=8;w=cols*144;rows=math.ceil(len(catalog)/cols)
    sheet=Image.new('RGB',(w,rows*116),(17,26,34));d=ImageDraw.Draw(sheet)
    for i,(name,im) in enumerate(catalog):
        x=(i%cols)*144;y=(i//cols)*116
        sheet.paste(im.resize((80,80),Image.Resampling.NEAREST),(x+30,y+4),im.resize((80,80),Image.Resampling.NEAREST))
        d.text((x+4,y+89),name.split('/')[-1][:23],fill=(195,205,196))
    save(project/'art/living-lattice-contact-sheet.png',sheet)
    print(f'{project.name}: authored {len(catalog)} block/item textures, native models, animated cores and armor.')


if __name__=='__main__':overhaul(Path(sys.argv[1]).resolve() if len(sys.argv)>1 else Path(__file__).resolve().parents[1])
