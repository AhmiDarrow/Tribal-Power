"""Run with python3 tools/blender_bestiary.py (Blender is importable as `bpy`).
Creates an editable, animated .blend roster and exports its cuboids to native model layers.

Rigs come from art/creatures/roster.json (the 13 bestiary creatures) and art/creatures/roster_tribes.json
(Tribal Kin, The Unsung). Every box gets a deterministic 64x32 atlas tile (tools/art/creatures.py paints the
atlases and glow maps); each Blender cube is UV mapped to the Minecraft box UV of its tile so the contact sheet
renders the real skins. Outputs: the .blend, textures/entity/*.png, GeneratedCreatureLayers.java,
art/creatures/blender-export.json, art/creatures/bestiary-contact-sheet.png and kin-and-unsung.png.
"""
import bpy,json,math,sys
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools/art'))
import creatures
roster=json.loads((ROOT/'art/creatures/roster.json').read_text(encoding='utf-8'))
tribes=json.loads((ROOT/'art/creatures/roster_tribes.json').read_text(encoding='utf-8'))
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
for block in (bpy.data.meshes,bpy.data.materials,bpy.data.images,bpy.data.collections):
 for item in list(block):block.remove(item)
scene=bpy.context.scene
scene.render.engine='CYCLES';scene.cycles.samples=24
scene.render.resolution_x=2000;scene.render.resolution_y=1800;scene.render.resolution_percentage=100
scene.world.color=(.10,.12,.16)
scene.view_settings.view_transform='Standard'
scene.render.image_settings.file_format='PNG'
scene.render.film_transparent=False
java=['package tk.darrow.tribalpower.client;','import net.minecraft.client.model.geom.*;','import net.minecraft.client.model.geom.builders.*;','/** Exported from art/creatures/tribal_bestiary.blend by tools/blender_bestiary.py. */','public final class GeneratedCreatureLayers {','public static LayerDefinition create(String id) {','MeshDefinition mesh=new MeshDefinition(); var root=mesh.getRoot();','switch(id) {']
texdir=ROOT/'src/main/resources/assets/tribalpower/textures/entity';texdir.mkdir(parents=True,exist_ok=True)
exports=[]
def f(n):return str(round(n,4))+'F'
COLS=4;SPACING=(3.8,4.4)
def slot_location(slot):return ((slot%COLS-1.5)*SPACING[0],(slot//COLS)*SPACING[1],0)

def face_uv(normal,x,y,z,box,u,v):
 """Pixel coordinates of a cube corner (local -0.5..0.5) on the Minecraft box UV of its tile."""
 _,_,_,w,h,d=box
 mx,mz,my=x+0.5,0.5-y,0.5-z   # normalised Minecraft x (along w), z (along d, 0 = front), y (along h, 0 = top)
 if normal.z>0.5:px,py=u+d+mx*w,v+(1-mz)*d
 elif normal.z<-0.5:px,py=u+d+w+mx*w,v+(1-mz)*d
 elif normal.y>0.5:px,py=u+d+mx*w,v+d+my*h
 elif normal.y<-0.5:px,py=u+2*d+w+(1-mx)*w,v+d+my*h
 elif normal.x<-0.5:px,py=u+(1-mz)*d,v+d+my*h
 else:px,py=u+d+w+mz*d,v+d+my*h
 return (px/256,1-py/256)

def painted_material(name,atlas_path,glow_path):
 mat=bpy.data.materials.new(name);mat.use_nodes=True;nodes=mat.node_tree.nodes;links=mat.node_tree.links
 bsdf=nodes.get('Principled BSDF');bsdf.inputs['Roughness'].default_value=.82;bsdf.inputs['Specular IOR Level'].default_value=.15
 tex=nodes.new('ShaderNodeTexImage');tex.image=bpy.data.images.load(str(atlas_path));tex.image.pack();tex.interpolation='Closest';tex.location=(-420,300)
 links.new(tex.outputs['Color'],bsdf.inputs['Base Color'])
 glow=nodes.new('ShaderNodeTexImage');glow.image=bpy.data.images.load(str(glow_path));glow.image.pack();glow.interpolation='Closest';glow.location=(-420,-80)
 links.new(glow.outputs['Color'],bsdf.inputs['Emission Color']);mul=nodes.new('ShaderNodeMath');mul.operation='MULTIPLY';mul.inputs[1].default_value=.55;mul.location=(-200,-80);links.new(glow.outputs['Alpha'],mul.inputs[0]);links.new(mul.outputs[0],bsdf.inputs['Emission Strength'])
 bsdf.inputs['Emission Strength'].default_value=0
 return mat

def build(r,slot,variant=None,export=True,location=None):
 """Build one creature (or one texture variant of it) at a contact-sheet slot; returns its export geometry."""
 title=r['name']+(' ('+variant+')' if variant else '')
 col=bpy.data.collections.new(title);scene.collection.children.link(col)
 def link(obj):
  for c in list(obj.users_collection):c.objects.unlink(obj)
  col.objects.link(obj)
 origin=bpy.data.objects.new(r['id']+('_'+variant if variant else ''),None);col.objects.link(origin);origin.location=location or slot_location(slot)
 scale=r.get('scale',1);origin.scale=(scale,scale,scale)
 boxes=creatures.layout(r)
 texture_id=(r.get('textures','{id}').format(variant=variant or '',id=r['id']))
 mat=painted_material(title,texdir/(texture_id+'.png'),texdir/(texture_id+'_glow.png'))
 if export:java.append('case "'+r['id']+'" -> {')
 geometry=[];lookup={(b['part'],b['index']):b for b in boxes}
 for p in r['parts']:
  pivot=bpy.data.objects.new(p['name'],None);col.objects.link(pivot);pivot.parent=origin;px,py,pz=p['pivot'];pivot.location=(px/16,-pz/16,(24-py)/16)
  chunks=[]
  for index,b in enumerate(p['boxes']):
   x,y,z,w,h,d=b['box'];u,v=lookup[(p['name'],index)]['u'],lookup[(p['name'],index)]['v']
   bpy.ops.mesh.primitive_cube_add(size=1);obj=bpy.context.object;obj.name=p['name']+'_cube';link(obj);obj.parent=pivot
   obj.location=((x+w/2)/16,-(z+d/2)/16,-(y+h/2)/16);obj.dimensions=(w/16,d/16,h/16);obj.data.materials.append(mat);obj['texture_uv']=[u,v];obj['palette_index']=b.get('color',0)
   mesh=obj.data;uv=mesh.uv_layers.new(name='minecraft') if not mesh.uv_layers else mesh.uv_layers[0]
   for poly in mesh.polygons:
    for li in poly.loop_indices:
     co=mesh.vertices[mesh.loops[li].vertex_index].co;uv.data[li].uv=face_uv(poly.normal,co.x,co.y,co.z,b['box'],u,v)
   bpy.context.view_layer.update()
   # Read the actual Blender scene transforms back into Minecraft coordinates.
   bw,bd,bh=[a*16/scale for a in obj.dimensions]  # world dimensions include the origin's display scale
   bx=obj.location.x*16-bw/2;by=-obj.location.z*16-bh/2;bz=-obj.location.y*16-bd/2
   chunks.append('.texOffs('+str(u)+','+str(v)+').addBox('+','.join(f(n) for n in (bx,by,bz,bw,bh,bd))+')')
   geometry.append(dict(part=p['name'],box=[bx,by,bz,bw,bh,bd],uv=[u,v]))
  pivot_mc=(pivot.location.x*16,24-pivot.location.z*16,-pivot.location.y*16)
  if export:java.append('root.addOrReplaceChild("'+p['name']+'",CubeListBuilder.create()'+''.join(chunks)+',PartPose.offset('+','.join(f(n) for n in pivot_mc)+'));')
  # Lightweight named pivots are the editable animation rig.
  if p['name'].startswith(('leg','arm','wing')):
   axis=1 if p['name'].startswith('wing') else 0
   for frame,angle in [(1,-.32),(13,.32),(25,-.32)]:
    pivot.rotation_euler[axis]=angle*(-1 if p['name'][-1:] in ('1','3','5','7') else 1);pivot.keyframe_insert('rotation_euler',frame=frame)
  if r.get('pulse') and p['name'] in ('body','band0','band1'):
   for frame,s in [(1,1.0),(13,1.035),(25,1.0)]:
    pivot.scale=(s,1,s);pivot.keyframe_insert('scale',frame=frame)
 if export:java.append('}')
 bpy.ops.object.text_add(location=(origin.location.x-1.5,origin.location.y+1.5,.025));label=bpy.context.object;label.name='Label '+title;label.data.body=title.upper();label.data.size=.20;label.rotation_euler=(0,0,math.pi);link(label)
 return geometry

slot=0
for r in roster:
 creatures.write_textures(r,creatures.layout(r),texdir)
 exports.append(dict(id=r['id'],cubes=build(r,slot)));slot+=1
slot=((slot+COLS-1)//COLS)*COLS;tribe_row=slot//COLS
for r in tribes:
 creatures.write_textures(r,creatures.layout(r),texdir)
 variants=r.get('variants') or [None]
 scale=r.get('scale',1)
 for n,variant in enumerate(variants):
  # large rigs get a centred slot in their own row so they do not overlap their neighbours
  location=(0,(slot//COLS)*SPACING[1]+scale*1.2,0) if scale>1 else None
  geometry=build(r,slot,variant,export=n==0,location=location)
  if n==0:exports.append(dict(id=r['id'],cubes=geometry))
  slot+=1
 slot=((slot+COLS-1)//COLS)*COLS
java+=['default -> throw new IllegalArgumentException("Unknown creature: "+id);','}','return LayerDefinition.create(mesh,256,256);','}','}']
(ROOT/'src/main/java/tk/darrow/tribalpower/client/GeneratedCreatureLayers.java').write_text('\n'.join(java)+'\n',encoding='utf-8')
(ROOT/'art/creatures/blender-export.json').write_text(json.dumps(exports,indent=2),encoding='utf-8')
scene.frame_set(7)
rows=(slot+COLS-1)//COLS
bpy.ops.mesh.primitive_plane_add(size=200,location=(0,5,-.03));floor=bpy.context.object;floor.name='Studio floor';mat=bpy.data.materials.new('Midnight slate');mat.diffuse_color=(.045,.067,.09,1);floor.data.materials.append(mat)
centre_y=(rows-1)*SPACING[1]/2
for loc,power,size in [((2,centre_y-8,13),2600,10),((-10,centre_y+2,8),1900,9),((8,centre_y+9,11),2800,8)]:
 bpy.ops.object.light_add(type='AREA',location=loc);light=bpy.context.object;light.data.energy=power;light.data.shape='DISK';light.data.size=size;light.rotation_euler=(Vector((0,centre_y,0))-light.location).to_track_quat('-Z','Y').to_euler()
def aim(cam,target,distance=(15,28,24),ortho=28):
 cam.location=Vector(target)+Vector(distance);cam.rotation_euler=(Vector(target)-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.type='ORTHO';cam.data.ortho_scale=ortho
bpy.ops.object.camera_add();cam=bpy.context.object;cam.name='Contact camera';scene.camera=cam
aim(cam,(0,centre_y+1.8,0.7),distance=(9,22,17),ortho=4.5+rows*3.7)
scene.render.filepath=str(ROOT/'art/creatures/bestiary-contact-sheet.png')
bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/'art/creatures/tribal_bestiary.blend'))
bpy.ops.render.render(write_still=True)
# Close-up of the tribe rows (Kin roles and The Unsung).
scene.render.resolution_x=1800;scene.render.resolution_y=1000
aim(cam,(0,tribe_row*SPACING[1]+2.6,1.2),distance=(8,18,12),ortho=15.5)
scene.render.filepath=str(ROOT/'art/creatures/kin-and-unsung.png')
bpy.ops.render.render(write_still=True)
print('BLENDER EXPORT COMPLETE:',len(roster),'creatures +',len(tribes),'tribe rigs')
