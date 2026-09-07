"""Run with Blender --background --python tools/blender_bestiary.py.
Creates an editable, animated .blend roster and exports its cuboids to native model layers.
"""
import bpy,json,math,random
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1]
roster=json.loads((ROOT/'art/creatures/roster.json').read_text(encoding='utf-8'))
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
scene=bpy.context.scene
scene.render.engine='CYCLES';scene.cycles.samples=24
scene.render.resolution_x=1800;scene.render.resolution_y=1450;scene.render.resolution_percentage=100
scene.world.color=(.10,.12,.16)
scene.view_settings.view_transform='Standard'
scene.render.image_settings.file_format='PNG'
scene.render.film_transparent=False
java=['package tk.darrow.tribalpower.client;','import net.minecraft.client.model.geom.*;','import net.minecraft.client.model.geom.builders.*;','/** Exported from art/creatures/tribal_bestiary.blend by tools/blender_bestiary.py. */','public final class GeneratedCreatureLayers {','public static LayerDefinition create(String id) {','MeshDefinition mesh=new MeshDefinition(); var root=mesh.getRoot();','switch(id) {']
texdir=ROOT/'src/main/resources/assets/tribalpower/textures/entity';texdir.mkdir(parents=True,exist_ok=True)
exports=[]
def f(n):return str(round(n,4))+'F'
for index,r in enumerate(roster):
 col=bpy.data.collections.new(r['name']);scene.collection.children.link(col)
 def link(obj):
  for c in list(obj.users_collection):c.objects.unlink(obj)
  col.objects.link(obj)
 origin=bpy.data.objects.new(r['id'],None);col.objects.link(origin);origin.location=((index%4-1.5)*3.8,(index//4)*4.4,0)
 image=bpy.data.images.new(r['id']+'_atlas',256,256,alpha=True);pixels=[0.0]*(256*256*4);glow=[0.0]*len(pixels)
 materials=[]
 for color in r['colors']:
  mat=bpy.data.materials.new(r['id']+'_'+color);mat.diffuse_color=tuple(int(color[i:i+2],16)/255 for i in (0,2,4))+(1,);mat.use_nodes=True
  bsdf=mat.node_tree.nodes.get('Principled BSDF');bsdf.inputs['Base Color'].default_value=mat.diffuse_color;bsdf.inputs['Roughness'].default_value=.78
  if len(materials)==2:bsdf.inputs['Emission Color'].default_value=mat.diffuse_color;bsdf.inputs['Emission Strength'].default_value=.6
  materials.append(mat)
 java.append('case "'+r['id']+'" -> {');tile=0;geometry=[]
 for p in r['parts']:
  pivot=bpy.data.objects.new(p['name'],None);col.objects.link(pivot);pivot.parent=origin;px,py,pz=p['pivot'];pivot.location=(px/16,-pz/16,(24-py)/16)
  chunks=[]
  for b in p['boxes']:
   x,y,z,w,h,d=b['box'];u=(tile%4)*64;v=(tile//4)*32;tile+=1
   bpy.ops.mesh.primitive_cube_add(size=1);obj=bpy.context.object;obj.name=p['name']+'_cube';link(obj);obj.parent=pivot
   obj.location=((x+w/2)/16,-(z+d/2)/16,-(y+h/2)/16);obj.dimensions=(w/16,d/16,h/16);obj.data.materials.append(materials[b['color']]);obj['texture_uv']=[u,v];obj['palette_index']=b['color']
   bpy.context.view_layer.update()
   # Read the actual Blender scene transforms back into Minecraft coordinates.
   bw,bd,bh=[a*16 for a in obj.dimensions];bx=obj.location.x*16-bw/2;by=-obj.location.z*16-bh/2;bz=-obj.location.y*16-bd/2
   chunks.append('.texOffs('+str(u)+','+str(v)+').addBox('+','.join(f(n) for n in (bx,by,bz,bw,bh,bd))+')')
   geometry.append(dict(part=p['name'],box=[bx,by,bz,bw,bh,bd],uv=[u,v]))
   color=materials[b['color']].diffuse_color
   for yy in range(v,min(v+32,256)):
    for xx in range(u,min(u+64,256)):
     shade=.90+((xx*7+yy*11+index*3)%9)/80
     if (xx-u)%16==0 or (yy-v)%12==0:shade*=.83
     k=((255-yy)*256+xx)*4
     pixels[k:k+4]=[min(1,color[j]*shade) for j in range(3)]+[1]
     if b['color']==2:glow[k:k+4]=list(color)
  pivot_mc=(pivot.location.x*16,24-pivot.location.z*16,-pivot.location.y*16)
  java.append('root.addOrReplaceChild("'+p['name']+'",CubeListBuilder.create()'+''.join(chunks)+',PartPose.offset('+','.join(f(n) for n in pivot_mc)+'));')
  # Lightweight named pivots are the editable animation rig.
  if p['name'].startswith(('leg','arm','wing')):
   axis=1 if p['name'].startswith('wing') else 0
   for frame,angle in [(1,-.32),(13,.32),(25,-.32)]:
    pivot.rotation_euler[axis]=angle*(-1 if p['name'][-1:] in ('1','3','5','7') else 1);pivot.keyframe_insert('rotation_euler',frame=frame)
 java.append('}')
 image.pixels=pixels;image.filepath_raw=str(texdir/(r['id']+'.png'));image.file_format='PNG';image.save();image.pack()
 glowimage=bpy.data.images.new(r['id']+'_glow',256,256,alpha=True);glowimage.pixels=glow;glowimage.filepath_raw=str(texdir/(r['id']+'_glow.png'));glowimage.file_format='PNG';glowimage.save();glowimage.pack()
 bpy.ops.object.text_add(location=(origin.location.x-1.5,origin.location.y+1.5,.025));label=bpy.context.object;label.name='Label '+r['name'];label.data.body=r['name'].upper();label.data.size=.20;label.rotation_euler=(0,0,math.pi);link(label)
 exports.append(dict(id=r['id'],cubes=geometry))
java+=['default -> throw new IllegalArgumentException("Unknown creature: "+id);','}','return LayerDefinition.create(mesh,256,256);','}','}']
(ROOT/'src/main/java/tk/darrow/tribalpower/client/GeneratedCreatureLayers.java').write_text('\n'.join(java)+'\n',encoding='utf-8')
(ROOT/'art/creatures/blender-export.json').write_text(json.dumps(exports,indent=2),encoding='utf-8')
scene.frame_set(7)
bpy.ops.mesh.primitive_plane_add(size=200,location=(0,5,-.03));floor=bpy.context.object;floor.name='Studio floor';mat=bpy.data.materials.new('Midnight slate');mat.diffuse_color=(.045,.067,.09,1);floor.data.materials.append(mat)
for loc,power,size in [((2,-1,13),2400,10),((-10,8,8),1800,9),((8,14,11),2600,8)]:
 bpy.ops.object.light_add(type='AREA',location=loc);light=bpy.context.object;light.data.energy=power;light.data.shape='DISK';light.data.size=size;light.rotation_euler=(Vector((0,5,0))-light.location).to_track_quat('-Z','Y').to_euler()
bpy.ops.object.camera_add(location=(15,28,24));cam=bpy.context.object;cam.rotation_euler=(Vector((0,6,0.7))-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.type='ORTHO';cam.data.ortho_scale=28;scene.camera=cam
scene.render.filepath=str(ROOT/'art/creatures/bestiary-contact-sheet.png')
bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/'art/creatures/tribal_bestiary.blend'))
bpy.ops.render.render(write_still=True)
print('BLENDER EXPORT COMPLETE:',len(roster),'creatures')
