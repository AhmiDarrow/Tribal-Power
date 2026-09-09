"""Author creature profiles and cuboid rigs for the Blender -> Minecraft pipeline."""
import json, random
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
# id, kind, habitat, shape, health, speed, damage, armor, attack, colors, reagent, field notes
ROWS=[
('dawn_stag','animal','forest','stag',24,.25,0,1,'none',['405d55','bec991','7effcb'],'dawn_velvet','A dawn grazer that carries the old forest song in its antlers. Breed with wheat; brush adults for Dawn Velvet. Bond an adult with a Bonding Charm to ride it without a saddle.'),
('lantern_fox','animal','forest','fox',14,.30,0,0,'none',['603e68','dca77b','7affe1'],'lantern_down','A shy dusk forager whose tail marks safe paths. Breed with sweet berries; brush adults for Lantern Down. Bond an adult with a Bonding Charm for a lantern that follows you.'),
('mossback','animal','swamp','turtle',30,.15,0,6,'none',['344f44','8eaa68','c4ffad'],'mossback_scale','A patient wetland gardener with a living shell. Breed with seagrass; brush adults for a naturally shed Mossback Scale. Bond an adult with a Bonding Charm for a walking saddlebag.'),
('ashbound','monster','forest','biped',24,.23,4,2,'ember',['473c42','ae6b4e','ffb46c'],'ember_heart','The remains of a broken fire rite. Its melee strikes briefly ignite; keep water close.'),
('rootbound','monster','forest','root',38,.18,6,5,'root',['494735','859364','b1e0a0'],'knotted_root','A grove guardian twisted by a severed ley line. Slow but sturdy; its blows root travellers briefly.'),
('reed_stalker','monster','swamp','stalker',22,.30,4,1,'venom',['294b4a','879c73','a2e8b9'],'reed_fang','A marsh hunter concealed among spirit reeds. Fast strikes carry a short poison.'),
('shardback','monster','march','crab',36,.18,5,8,'shove',['455272','8baab7','87fff0'],'prism_carapace','A crystal-field scavenger armored in singing stone. Keep your distance from its heavy shove.'),
('hollow_sentinel','monster','march','sentinel',44,.20,6,6,'weaken',['313f53','b8966b','bafbe8'],'sentinel_sigil','An empty watchman that still guards the ancient roads. Its touch weakens the grip of attackers.'),
('storm_moth','monster','march','moth',20,.24,4,0,'gust',['42415e','a99cc3','adffff'],'storm_wing','A storm-fed spirit with vast veil wings. A bright gathering of motes warns of its gust; cover breaks the attack.'),
('cinder_imp','monster','march','imp',18,.27,4,1,'bolt',['563943','bd7851','ffcb89'],'cinder_knot','A nimble ember spirit. It gathers sparks before a burning bolt; the spell never ignites terrain.'),
('mourning_bell','monster','march','bell',28,.20,4,3,'chill',['364760','8e97b5','b6e4ff'],'bell_fragment','A floating relic that remembers the lost tribes. Its telegraphed chime slows; shelter interrupts the song.'),
('rift_hound','monster','march','hound',28,.34,5,2,'hound',['323848','79658d','b5a3ff'],'rift_tooth','A hunter drawn to tears in the lattice. It closes distance quickly but cannot cross worlds by itself.'),
('echo_weaver','monster','march','spider',26,.25,4,3,'weave',['463b59','b6958f','e4baff'],'echo_silk','An eight-legged keeper of broken songs. Its bite slows prey without placing webs or damaging the camp.')
]
def box(x,y,z,w,h,d,color=0):return dict(box=[x,y,z,w,h,d],color=color)
def part(name,pivot,*boxes):return dict(name=name,pivot=list(pivot),boxes=list(boxes))
def rig(shape):
 p=[]
 if shape in ('stag','fox','hound','stalker'):
  stag=shape=='stag'; fox=shape=='fox'; bodyY=12 if stag else 17; length=14 if stag else 12
  p+=[part('body',(0,bodyY,1),box(-4,-3,-7,8,6,length)),part('head',(0,bodyY-2,-7),box(-3,-3,-4,6,6,5),box(-2,0,-7,4,3,3,1),box(-3,-5,-2,2,3,2,1),box(1,-5,-2,2,3,2,1),box(-3.2,-1,-4.2,1,1,1,2),box(2.2,-1,-4.2,1,1,1,2))]
  for i in range(4):p.append(part('leg'+str(i),((-3 if i%2==0 else 3),bodyY+2,(-4 if i<2 else 6)),box(-1,0,-1,2,24-bodyY-2,2,1)))
  p.append(part('tail',(0,bodyY,7),box(-2,-2,0,4,4,8,1),box(-1.5,-1.5,7,3,3,3,2)))
  if stag:
   for side in (-1,1):
    p[1]['boxes'] += [box(side*3-0.5,-12,-1,1,9,1,1),box(-7 if side<0 else 3,-9,-1,4,1,1,1),box(-6.5 if side<0 else 5.5,-12,-1,1,4,1,2)]
  if shape=='hound':p[0]['boxes'] += [box(-1,-6,-4,2,3,10,1),box(-2,-5,0,4,1,4,2)]
  if shape=='stalker':p[0]['boxes'] += [box(-3,-10,3,1,8,1,1),box(2,-8,5,1,6,1,1),box(-.5,-12,4,1,10,1,2)]
 elif shape=='turtle':
  p=[part('body',(0,19,1),box(-7,-4,-8,14,6,16),box(-5,-7,-6,10,3,12,1),box(-3,-8,-4,6,1,8,1)),part('head',(0,20,-8),box(-2,-2,-4,4,4,5,1),box(-2.2,-1,-4.2,1,1,1,2),box(1.2,-1,-4.2,1,1,1,2)),part('tail',(0,20,8),box(-1,0,0,2,2,4,1))]
  for i in range(4):p.append(part('leg'+str(i),((-6 if i%2==0 else 6),21,(-5 if i<2 else 5)),box(-2,0,-2,4,3,4,1)))
  p[0]['boxes'] += [box(-4,-10,2,1,3,1,1),box(-6,-11,0,5,1,5,2)]
 elif shape in ('biped','root','sentinel','imp'):
  tall=shape=='sentinel';small=shape=='imp';y=9 if tall else 16 if small else 12; w=12 if shape=='root' else 8
  p=[part('body',(0,y,0),box(-w/2,-5,-3,w,10,6),box(-2,-3,-3.3,4,4,1,2)),part('head',(0,y-6,0),box(-3,-5,-3,6,6,6,1),box(-2,-2,-3.3,4,1,1,2))]
  for i in range(2):p.append(part('leg'+str(i),((-2.5 if i==0 else 2.5),y+4,0),box(-1.5,0,-1.5,3,24-y-4,3)))
  for i in range(2):p.append(part('arm'+str(i),((-w/2-2 if i==0 else w/2+2),y-4,0),box(-1.5,0,-1.5,3,10 if not small else 6,3,1)))
  if shape=='root':p[0]['boxes'] += [box(-8,-8,0,2,8,2,1),box(6,-10,0,2,10,2,1),box(-10,-9,-1,6,2,4,1)]
  if tall:p[1]['boxes'] += [box(-5,-8,-1,2,9,2,1),box(3,-8,-1,2,9,2,1),box(-1,-6,-3.3,2,1,1,2)]
  if small:p[1]['boxes'] += [box(-5,-6,-1,2,5,2,1),box(3,-6,-1,2,5,2,1)]
 elif shape in ('crab','spider'):
  p=[part('body',(0,17,2),box(-6,-4,-5,12,7,12)),part('head',(0,18,-5),box(-4,-2,-4,8,4,5,1),box(-3,-1,-4.2,2,1,1,2),box(1,-1,-4.2,2,1,1,2))]
  for i in range(8):
   side=-1 if i%2==0 else 1;p.append(part('leg'+str(i),(side*5,17,-3+i//2*3),box(-7 if side<0 else 0,0,-.7,7,2,1.4,1),box(-7 if side<0 else 5,1,-.7,2,6,1.4)))
  if shape=='crab':p[0]['boxes'] += [box(-4,-9,0,2,6,2,2),box(1,-12,3,3,9,3,2),box(-1,-7,-3,2,4,2,1)]
 elif shape=='moth':
  p=[part('body',(0,14,0),box(-2,-5,-2,4,11,4)),part('head',(0,8,0),box(-3,-3,-3,6,4,5,1),box(-4,-7,-1,1,5,1,2),box(3,-7,-1,1,5,1,2))]
  for i,side in enumerate((-1,1)):p.append(part('wing'+str(i),(side*2,12,0),box(-14 if side<0 else 0,-6,0,14,11,1,1),box(-10 if side<0 else 0,5,0,10,6,1),box(-10 if side<0 else 4,-3,-.2,6,4,1,2)))
 elif shape=='bell':
  p=[part('body',(0,14,0),box(-5,-7,-5,10,9,10),box(-7,2,-7,14,2,14,1),box(-2,-11,-2,4,4,4,1),box(-4,-3,-5.2,8,1,1,2)),part('tail',(0,18,0),box(-1,0,-1,2,6,2,1),box(-2,5,-2,4,2,4,2))]
 return p

def kin_rig():
 """Masked, cloaked humanoid (design 3.0 §8). Flat rig: every part is a root child; the role only changes the skin."""
 return [
  part('head',(0,0,0),box(-4,-8,-4,8,8,8,1),box(-4.5,-8.5,-5,9,9,1,1),box(-4.5,-9,-4.5,9,5,9,0),box(-4.5,-2,3.5,1,6,1,1),box(3.5,-2,3.5,1,6,1,1)),
  part('body',(0,0,0),box(-4,0,-2,8,12,4,0),box(-4.5,7,-2.5,9,2,5,1)),
  part('cloak',(0,0,2.2),box(-5,0,0,10,14,2,0)),
  part('arm0',(-5,2,0),box(-3,-2,-2,4,12,4,1)),
  part('arm1',(5,2,0),box(-1,-2,-2,4,12,4,1)),
  part('leg0',(-1.9,12,0),box(-2,0,-2,4,12,4,0)),
  part('leg1',(1.9,12,0),box(-2,0,-2,4,12,4,0))]

def unsung_rig():
 """Hollow standing drum authored at half scale (renderer draws it at 2x; feet at y=24, ~3 blocks wide, body
 ~2.75 tall, mask floating half a block above the hide, hands reaching the floor). Shell = eight lacquer panels
 with a 2 px slit in the middle of every side showing the black interior; wide top/bottom bands; taut hide;
 floating mask head with horns; a thin rune halo; two long rune arms from shoulder pivots at the top band
 ending in three-fingered hands."""
 shell=[]
 for x0 in (-10,1):shell+=[box(x0,-6,-10,9,13,1),box(x0,-6,9,9,13,1)]
 for z0 in (-9,1):shell+=[box(-10,-6,z0,1,13,8),box(9,-6,z0,1,13,8)]
 body=part('body',(0,13,0),*shell,box(-11,-11,-11,22,1,22,1))
 band0=part('band0',(0,13,0),box(-12,-10,-12,24,4,24))
 band1=part('band1',(0,13,0),box(-12,7,-12,24,4,24))
 head=part('head',(0,-3,0),box(-4,-7,-4,8,7,8),box(-4,-8,-5,8,8,1,1),box(-6,-6,-1,2,6,2),box(4,-6,-1,2,6,2))
 halo=part('halo',(0,-12,0),box(-6,0,-6,12,1,1,2),box(-6,0,5,12,1,1,2),box(-6,0,-5,1,1,10,2),box(5,0,-5,1,1,10,2))
 arms=[part('arm'+str(i),(-14 if i==0 else 14,3,0),box(-1.5,0,-1.5,3,15,3,1),box(-3,15,-2,6,3,4),box(-3,18,-1,1.5,4,2),box(-.75,18,-1,1.5,4,2),box(1.5,18,-1,1.5,4,2)) for i in range(2)]
 return [body,band0,band1,head,halo]+arms

TRIBE_ROWS=[
 dict(id='tribal_kin',name='Tribal Kin',kind='kin',shape='kin',colors=['4a3b33','a87a58','e8d9a8'],variants=['elder','drummer','hunter','weaver'],textures='kin_{variant}',preview_overlay={'elder':'spindle','drummer':'spark','hunter':'claw','weaver':'sigil'},notes='Masked camp folk in four roles; the cloak overlay is tinted by the tribe colour.'),
 dict(id='the_unsung',name='The Unsung',kind='boss',shape='drum',colors=['4a1f22','c9a974','9b6cff'],scale=2,pulse=True,notes='Hollow drum-spirit with rune arms and a cracked mask; authored at half scale, rendered at 2x.')]

def main():
 data=[]
 for row in ROWS:
  keys=['id','kind','habitat','shape','health','speed','damage','armor','attack','colors','reagent','notes'];r=dict(zip(keys,row));r['name']=r['id'].replace('_',' ').title();r['parts']=rig(r['shape']);data.append(r)
 out=ROOT/'art/creatures';out.mkdir(parents=True,exist_ok=True);(out/'roster.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
 tribes=[]
 for row in TRIBE_ROWS:
  r=dict(row);r['parts']=kin_rig() if r['shape']=='kin' else unsung_rig();tribes.append(r)
 (out/'roster_tribes.json').write_text(json.dumps(tribes,indent=2)+'\n',encoding='utf-8')
 print('Authored',len(data),'creatures and',len(tribes),'tribe rigs for Blender')
if __name__=='__main__':main()
