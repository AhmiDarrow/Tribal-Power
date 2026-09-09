"""Render 128x128 transparent portraits from art/creatures/tribal_bestiary.blend (Cycles, 16 samples):
FTB Library faces for the 13 bestiary creatures and Spirit Codex portraits for every rig, including the four
Tribal Kin roles (tribal_kin.png is the Elder) and The Unsung. Framing matches the original codex portraits.
Run after tools/blender_bestiary.py: python3 tools/blender_icon_pass.py
"""
import bpy,json
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1]
bpy.ops.wm.open_mainfile(filepath=str(ROOT/'art/creatures/tribal_bestiary.blend'))
rows=json.loads((ROOT/'art/creatures/roster.json').read_text(encoding='utf-8'))
tribes=json.loads((ROOT/'art/creatures/roster_tribes.json').read_text(encoding='utf-8'))
scene=bpy.context.scene;scene.render.resolution_x=128;scene.render.resolution_y=128;scene.render.film_transparent=True;scene.cycles.samples=16
bpy.data.objects['Studio floor'].hide_render=True
for obj in bpy.data.objects:
    if obj.type=='FONT':obj.hide_render=True
faces=ROOT/'src/main/resources/assets/ftblibrary/textures/faces/tribalpower';faces.mkdir(parents=True,exist_ok=True)
codex=ROOT/'src/main/resources/assets/tribalpower/textures/gui/codex';codex.mkdir(parents=True,exist_ok=True)
collections=[c for c in bpy.data.collections if not c.name.startswith('Label')]
def height(r):
    top=min(p['pivot'][1]+b['box'][1] for p in r['parts'] for b in p['boxes'])
    return (24-top)/16*r.get('scale',1)
def render(r,origin_name,path):
    origin=bpy.data.objects[origin_name]
    for c in collections:c.hide_render=origin.name not in c.objects
    loc=origin.location;s=max(1.0,height(r)/2.0)  # taller rigs get a wider frame; the 13 keep the original framing
    scene.camera.location=loc+Vector((1.8,4,1.9))*s;scene.camera.rotation_euler=(loc+Vector((0,0,.9*s))-scene.camera.location).to_track_quat('-Z','Y').to_euler();scene.camera.data.ortho_scale=2.6*s
    scene.render.filepath=str(path);bpy.ops.render.render(write_still=True)
count=0
for r in rows:
    render(r,r['id'],faces/(r['id']+'.png'));(codex/(r['id']+'.png')).write_bytes((faces/(r['id']+'.png')).read_bytes());count+=1
for r in tribes:
    for variant in r.get('variants') or [None]:
        name=r['id']+('_'+variant if variant else '')
        render(r,name,codex/(name+'.png'));count+=1
    if r.get('variants'):(codex/(r['id']+'.png')).write_bytes((codex/(r['id']+'_'+r['variants'][0]+'.png')).read_bytes())
print('Rendered',count,'creature portraits')
