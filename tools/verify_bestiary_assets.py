"""Verify Blender geometry, texture bounds, creature content and native recipe links."""
import json
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
rows=json.loads((ROOT/'art/creatures/roster.json').read_text(encoding='utf-8'))
export=json.loads((ROOT/'art/creatures/blender-export.json').read_text(encoding='utf-8'))
assert len(rows)==13 and sum(r['kind']=='animal' for r in rows)==3
assert {r['id'] for r in rows}=={r['id'] for r in export}
res=ROOT/'src/main/resources'
for r,e in zip(rows,export):
    assert len(e['cubes'])==sum(len(p['boxes']) for p in r['parts']),r['id']
    authored=[b['box'] for p in r['parts'] for b in p['boxes']]
    for expected,cube in zip(authored,e['cubes']):
        assert all(abs(a-b)<0.0001 for a,b in zip(expected,cube['box'])),(r['id'],'Blender scene has not evaluated authored scale')
        x,y,z,w,h,d=cube['box'];u,v=cube['uv']
        assert w>0 and h>0 and d>0
        assert u>=0 and v>=0 and u+2*(w+d)<=256 and v+h+d<=256,(r['id'],cube)
    for suffix in ['', '_glow']:
        with Image.open(res/f"assets/tribalpower/textures/entity/{r['id']}{suffix}.png") as im:assert im.size==(256,256)
    for path in [f"assets/tribalpower/models/item/{r['id']}_spawn_egg.json",f"data/tribalpower/loot_table/entities/{r['id']}.json",f"data/tribalpower/recipe/lattice/{r['reagent']}.json"]:
        json.loads((res/path).read_text(encoding='utf-8'))
    if r['habitat']!='march':assert (res/f"data/tribalpower/neoforge/biome_modifier/{r['id']}.json").exists()
assert (ROOT/'art/creatures/tribal_bestiary.blend').stat().st_size>100000
print('PASS: 13 Blender rigs, texture bounds, glow atlases, eggs, loot, recipes and Overworld habitats')
