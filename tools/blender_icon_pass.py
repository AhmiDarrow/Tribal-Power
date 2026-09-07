import bpy,json
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1]
bpy.ops.wm.open_mainfile(filepath=str(ROOT/'art/creatures/tribal_bestiary.blend'))
rows=json.loads((ROOT/'art/creatures/roster.json').read_text(encoding='utf-8'))
scene=bpy.context.scene;scene.render.resolution_x=128;scene.render.resolution_y=128;scene.render.film_transparent=True;scene.cycles.samples=16
bpy.data.objects['Studio floor'].hide_render=True
for obj in bpy.data.objects:
    if obj.type=='FONT':obj.hide_render=True
out=ROOT/'src/main/resources/assets/ftblibrary/textures/faces/tribalpower';out.mkdir(parents=True,exist_ok=True)
for r in rows:
    for row in rows:bpy.data.collections[row['name']].hide_render=row['id']!=r['id']
    origin=bpy.data.objects[r['id']].location
    scene.camera.location=origin+Vector((1.8,4,1.9));scene.camera.rotation_euler=(origin+Vector((0,0,.9))-scene.camera.location).to_track_quat('-Z','Y').to_euler();scene.camera.data.ortho_scale=2.6
    scene.render.filepath=str(out/(r['id']+'.png'));bpy.ops.render.render(write_still=True)
print('Rendered 13 FTB creature icons')
