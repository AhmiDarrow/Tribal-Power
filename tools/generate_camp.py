"""Author native cuboid camp models, recipes, loot and guide metadata."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
def write(path,data):
    p=RES/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
def box(a,b,texture):return {'from':a,'to':b,'faces':{f:{'texture':'#'+texture} for f in ['north','south','east','west','up','down']}}
base=[box([2,0,2],[14,2,14],'stone'),box([3,2,3],[13,3,13],'trim')]
models={
 'wayanchor':base+[box([6,3,6],[10,13,10],'wood'),box([4,6,4],[12,9,12],'trim'),box([5,13,5],[11,16,11],'light')]+[box([x,3,z],[x+2,12,z+2],'trim') for x,z in [(2,2),(12,2),(2,12),(12,12)]],
 'hush_totem':base+[box([5,3,5],[11,10,11],'wood'),box([3,10,4],[13,15,12],'stone'),box([4,12,3],[6,14,4],'light'),box([10,12,3],[12,14,4],'light'),box([1,9,6],[15,11,10],'trim')],
 'summoning_cradle':base+[box([4,3,4],[12,5,12],'wood'),box([6,5,6],[10,9,10],'light')]+[box([x,3,z],[x+2,14,z+2],'trim') for x,z in [(2,2),(12,2),(2,12),(12,12)]]+[box([2,13,2],[14,15,4],'wood'),box([2,13,12],[14,15,14],'wood')],
 'grove_tender':base+[box([6,3,6],[10,13,10],'wood'),box([3,8,6],[13,10,10],'trim'),box([3,13,3],[13,15,13],'leaf'),box([6,15,6],[10,16,10],'light')],
 'spirit_lantern':base+[box([5,3,5],[11,11,11],'light')]+[box([x,3,z],[x+1,13,z+1],'trim') for x,z in [(3,3),(12,3),(3,12),(12,12)]]+[box([3,12,3],[13,14,13],'wood'),box([6,14,6],[10,16,10],'trim')],
 'rain_chime':[box([2,13,2],[14,15,14],'wood'),box([7,0,7],[9,14,9],'wood')]+[box([x,5+(x%3),5],[x+1,13,7],'trim') for x in [3,6,9,12]]+[box([4,2,4],[12,3,12],'stone')],
 'offering_table':[box([1,11,1],[15,13,15],'wood'),box([2,13,2],[14,14,14],'trim'),box([5,14,5],[11,15,11],'light')]+[box([x,0,z],[x+2,11,z+2],'stone') for x,z in [(2,2),(12,2),(2,12),(12,12)]],
 'binding_effigy':[box([6,3,6],[10,10,10],'wood'),box([5,10,5],[11,15,11],'stone'),box([3,8,7],[13,10,9],'trim'),box([6,0,7],[7,4,9],'wood'),box([9,0,7],[10,4,9],'wood'),box([7,6,5],[9,8,6],'light')],
}
names={k:k.replace('_',' ').title() for k in models};names['wayanchor']='Wayanchor'
for key,elements in models.items():
    model={'parent':'minecraft:block/block','render_type':'minecraft:cutout','textures':{'particle':'tribalpower:block/loom_wood','wood':'tribalpower:block/loom_wood','stone':'tribalpower:block/loom_stone','trim':'tribalpower:block/loom_copper','light':'tribalpower:block/loom_light','leaf':'tribalpower:block/march_leaf'},'elements':elements}
    write(Path(f'assets/tribalpower/models/block/{key}.json'),model)
    write(Path(f'assets/tribalpower/models/item/{key}.json'),{'parent':f'tribalpower:block/{key}'})
    if key!='binding_effigy':
        write(Path(f'assets/tribalpower/blockstates/{key}.json'),{'variants':{'':{'model':f'tribalpower:block/{key}'}}})
        write(Path(f'data/tribalpower/loot_table/blocks/{key}.json'),{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'tribalpower:{key}'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
recipes={
 'wayanchor':(['CMC','IRI','SSS'],{'C':'march_crystal','M':'minecraft:ender_eye','I':'manifested_ingot','R':'resonant_core','S':'march_stone'}),
 'hush_totem':([' S ','BEB','WWW'],{'S':'spirit_seal','B':'bound_echo','E':'echo_shard','W':'march_planks'}),
 'summoning_cradle':(['ICI','BRB','WWW'],{'I':'manifested_ingot','C':'march_crystal','B':'bound_echo','R':'resonant_core','W':'march_planks'}),
 'grove_tender':([' E ','CHC','WBW'],{'E':'earth_seal','C':'minecraft:copper_ingot','H':'minecraft:iron_hoe','W':'minecraft:oak_planks','B':'bound_echo'}),
 'spirit_lantern':([' C ','CSC',' W '],{'C':'minecraft:copper_ingot','S':'spirit_shard','W':'minecraft:oak_planks'}),
 'rain_chime':(['WWW','CBC',' C '],{'W':'minecraft:oak_planks','C':'minecraft:copper_ingot','B':'bone_chime'}),
 'offering_table':(['WBW',' C ','S S'],{'W':'minecraft:oak_planks','B':'bound_echo','C':'minecraft:chest','S':'minecraft:stone'}),
 'binding_effigy':([' B ','SRS',' E '],{'B':'bone_chime','S':'spiritweave','R':'resonant_core','E':'bound_echo'}),
}
for key,(pattern,ingredients) in recipes.items():
    mapping={k:({'tag':'minecraft:planks'} if v=='minecraft:oak_planks' else {'item':v if ':' in v else 'tribalpower:'+v}) for k,v in ingredients.items()}
    write(Path(f'data/tribalpower/recipe/{key}.json'),{'type':'minecraft:crafting_shaped','pattern':pattern,'key':mapping,'result':{'id':'tribalpower:'+key,'count':1}})
for key in ['bind_effigy','first_summon']:
    write(Path(f'data/tribalpower/advancement/{key}.json'),{'criteria':{'done':{'trigger':'minecraft:impossible'}}})
langpath=RES/'assets/tribalpower/lang/en_us.json';lang=json.loads(langpath.read_text(encoding='utf-8'))
lang.update({('item' if k=='binding_effigy' else 'block')+'.tribalpower.'+k:v for k,v in names.items()});write(Path('assets/tribalpower/lang/en_us.json'),lang)
print('Generated 8 camp models, recipes, block loot, and ritual milestones.')
shapes='package tk.darrow.tribalpower.camp;\nimport net.minecraft.world.phys.shapes.*;\nimport net.minecraft.world.level.block.Block;\npublic final class CampShapes {\n private static final java.util.Map<String,VoxelShape> SHAPES=java.util.Map.ofEntries(\n'
entries=[]
for key,elements in models.items():
    if key=='binding_effigy':continue
    parts=['Block.box('+','.join(str(x) for x in e['from']+e['to'])+')' for e in elements]
    entries.append('java.util.Map.entry("'+key+'",Shapes.or('+','.join(parts)+').optimize())')
shapes+=',\n'.join(entries)+');\n public static VoxelShape shape(String id){return SHAPES.getOrDefault(id,Shapes.block());}\n}\n'
(ROOT/'src/main/java/tk/darrow/tribalpower/camp/CampShapes.java').write_text(shapes,encoding='utf-8')
