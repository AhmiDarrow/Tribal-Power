"""Author creature profiles and cuboid rigs for the Blender -> Minecraft pipeline."""
import json, random
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
# id, kind, habitat, shape, health, speed, damage, armor, attack, colors, reagent, field notes
ROWS=[
('dawn_stag','animal','forest','stag',24,.25,0,1,'none',['405d55','bec991','7effcb'],'dawn_velvet','A dawn grazer that carries the old forest song in its antlers. Breed with wheat; brush adults for Dawn Velvet.'),
('lantern_fox','animal','forest','fox',14,.30,0,0,'none',['603e68','dca77b','7affe1'],'lantern_down','A shy dusk forager whose tail marks safe paths. Breed with sweet berries; brush adults for Lantern Down.'),
('mossback','animal','swamp','turtle',30,.15,0,6,'none',['344f44','8eaa68','c4ffad'],'mossback_scale','A patient wetland gardener with a living shell. Breed with seagrass; brush adults for a naturally shed Mossback Scale.'),
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

def main():
 data=[]
 for row in ROWS:
  keys=['id','kind','habitat','shape','health','speed','damage','armor','attack','colors','reagent','notes'];r=dict(zip(keys,row));r['name']=r['id'].replace('_',' ').title();r['parts']=rig(r['shape']);data.append(r)
 out=ROOT/'art/creatures';out.mkdir(parents=True,exist_ok=True);(out/'roster.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
 print('Authored',len(data),'creatures for Blender')
if __name__=='__main__':main()
