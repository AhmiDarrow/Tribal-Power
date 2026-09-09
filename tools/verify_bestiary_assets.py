"""Verify Blender geometry, texture bounds, creature content and native recipe links."""
import json,sys
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools/art'))
import creatures
rows=json.loads((ROOT/'art/creatures/roster.json').read_text(encoding='utf-8'))
tribes=json.loads((ROOT/'art/creatures/roster_tribes.json').read_text(encoding='utf-8'))
export=json.loads((ROOT/'art/creatures/blender-export.json').read_text(encoding='utf-8'))
assert len(rows)==13 and sum(r['kind']=='animal' for r in rows)==3
assert [r['id'] for r in tribes]==['tribal_kin','the_unsung']
assert {r['id'] for r in rows+tribes}=={r['id'] for r in export}
res=ROOT/'src/main/resources'
tex=res/'assets/tribalpower/textures/entity'
layers=(ROOT/'src/main/java/tk/darrow/tribalpower/client/GeneratedCreatureLayers.java').read_text(encoding='utf-8')
exported={e['id']:e for e in export}
def painted(path):
    with Image.open(path) as im:
        assert im.size==(256,256),path
        colours={px[:3] for px in im.get_flattened_data() if px[3]}
    return colours
for r in rows+tribes:
    e=exported[r['id']]
    assert 'case "'+r['id']+'"' in layers,r['id']
    assert len(e['cubes'])==sum(len(p['boxes']) for p in r['parts']),r['id']
    authored=[b['box'] for p in r['parts'] for b in p['boxes']]
    for expected,cube in zip(authored,e['cubes']):
        assert all(abs(a-b)<0.0001 for a,b in zip(expected,cube['box'])),(r['id'],'Blender scene has not evaluated authored scale',expected,cube['box'])
        x,y,z,w,h,d=cube['box'];u,v=cube['uv']
        assert w>0 and h>0 and d>0
        assert u>=0 and v>=0 and u+2*(w+d)<=256 and v+h+d<=256,(r['id'],cube)
    assert [(b['u'],b['v']) for b in creatures.layout(r)]==[tuple(c['uv']) for c in e['cubes']],(r['id'],'painter tiles differ from the Blender export')
    for name in creatures.texture_names(r):
        colours=painted(tex/f'{name}.png')
        if not name.endswith('_glow') and not name.startswith(('bonded_collar','kin_cloak')):assert len(colours)>=12,(name,'texture is not painted',len(colours))
    if r['kind']=='animal':painted(tex/f"bonded_collar_{r['id']}.png")
    for path in [f"assets/tribalpower/models/item/{r['id']}_spawn_egg.json",f"data/tribalpower/loot_table/entities/{r['id']}.json",f"data/tribalpower/recipe/lattice/{r['reagent']}.json"] if r['kind'] in ('animal','monster') else []:
        json.loads((res/path).read_text(encoding='utf-8'))
    if r.get('habitat') and r['habitat']!='march':assert (res/f"data/tribalpower/neoforge/biome_modifier/{r['id']}.json").exists()
for name in ['tribal_kin','tribal_kin_elder','tribal_kin_drummer','tribal_kin_hunter','tribal_kin_weaver','the_unsung']+[r['id'] for r in rows]:
    with Image.open(res/f'assets/tribalpower/textures/gui/codex/{name}.png') as im:assert im.size==(128,128) and im.mode=='RGBA',name
for name in ['tribal_kin_elder','the_unsung']+[r['id'] for r in rows]:
    with Image.open(res/f'assets/tribalpower/textures/gui/codex/{name}.png') as im:assert any(px[3] for px in im.get_flattened_data()),(name,'empty portrait')
assert (ROOT/'art/creatures/tribal_bestiary.blend').stat().st_size>100000
for sheet in ('bestiary-contact-sheet.png','kin-and-unsung.png'):assert (ROOT/'art/creatures'/sheet).stat().st_size>10000
print('PASS: 13 Blender rigs + Kin and Unsung, painted atlases, glow maps, cloak overlays, collars, portraits, eggs, loot, recipes and Overworld habitats')
