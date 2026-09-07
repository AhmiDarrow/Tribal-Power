"""Single source for the standalone written book and machine-readable guide."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CHAPTERS = [
    ('Spirit Codex', 'Shamanic technomancy begins with a rhythm. Raise a camp that answers you: living power, elemental workshops, woven equipment and paths between worlds.'),
    ('First camp', 'Craft a Bone Chime, Spirit Shard and Drumheart. Add a Pulse Cell. JEI shows crafting recipes; every system also works without another mod installed.'),
    ('Spirit Pulse', 'Pulse comes from rhythm, landscape and harmony. Keep a generator within 8 blocks of your workshop. Machines draw from generators first, then totem buffers.'),
    ('Drumheart', 'Strike empty-handed. Beats about one second apart yield 24 Pulse; offbeat hits yield 10. Redstone rising edges give 5, with an 8-tick cooldown. Build a clock to automate rhythm.'),
    ('Ley Collector', 'Open sky, night, rain, nearby water and living greenery strengthen collection. Place it thoughtfully. A redstone signal pauses generation.'),
    ('Pulse Resonator', 'Seat an Echo catalyst and place at least two different elemental totems within 8 blocks. More distinct voices and stronger catalysts increase generation. The catalyst is reusable.'),
    ('Harmonic ranks', 'Catalysts: Echo Shard, Attuned Echo, Bound Echo, Resonant Core. Coal and wood are not fuels. Sneak with an empty hand to recover the catalyst. Redstone pauses generation.'),
    ('Carry the beat', 'Use a Pulse Cell on a Drumheart or Resonator to charge it. Ordinary cells hold 200 Pulse; Greater Cells hold 1,200. Carry charged cells for spells, tools and travel.'),
    ('Five voices', 'Earth shatters. Fire attunes. Water binds. Spirit manifests. Air lends motion. Place the needed totem within 8 blocks of the station; missing voices pause work safely.'),
    ('Chalk and conductor', 'Mark two totems with Ritual Chalk to link them within 16 blocks. A Conductor moves Pulse through linked totems and routes Song Bench items through nearby Ancestral Caches.'),
    ('Echo workshops', 'The four Echo stations process real batches. Feed slot 0 from above or the sides; extract finished items below. Each has a work display and eight output slots. Full output pauses work.'),
    ('Shatter', 'Earth turns stone into Echo Shards. Raw iron, copper and gold become two grits each. Smelt or blast grits into ingots. Shared raw-material tags accept compatible ores from other mods.'),
    ('Attune and Bind', 'Fire turns Echo Shards into Attuned Echoes. Water binds them into Bound Echoes. Bind also turns wool into Spiritweave and dirt into clay. Check JEI for time and Pulse costs.'),
    ('Manifest', 'Spirit turns Bound Echoes into Manifested Ingots, then ingots into Resonant Cores. Cores unlock stronger cells, equipment and long-distance automation.'),
    ('Song Bench', 'The original single-item workshop remains. Insert an Echo-stage ingredient and click empty-handed to sing; sneak-click to retrieve it. Dedicated Echo stations are better for sustained batches.'),
    ('Ancestral Cache', 'A local 54-slot inventory for camp supplies. Hoppers and item pipes work normally. A redstone signal locks access and automation. A comparator reads how full it is.'),
    ('Deep Cache', 'A personal 54-slot vault shared between your Deep Caches. Visiting The March attunes you; otherwise opening costs 5 Pulse. A Wayfarer Satchel opens the same personal vault.'),
    ('Spirit Cistern', 'Stores 16 buckets of one fluid. Buckets and standard fluid pipes both work. Redstone locks filling and draining, including remote relays. A comparator reads fullness.'),
    ('Wireless cargo', 'Place an item or fluid relay above its source inventory or tank. Use the Lattice Tuner on a destination face, then on the relay. Sneak-use the tuner to replace a previous mark.'),
    ('Cargo tiers', 'Local relays reach 32 blocks. Longreach relays reach 128. Astral relays have no distance limit and can cross dimensions. Transfers cost 4, 8 or 16 Pulse per successful beat.'),
    ('Cargo rhythm', 'Each second, a relay moves up to 16 items or 250 mB. Both ends must already be loaded. Unloaded or full destinations pause safely. Relays never keep distant chunks loaded themselves.'),
    ('Cargo controls', 'Redstone pauses a relay. Comparator: 0 unlinked, 1 linked but waiting, 15 transferring. Lock the receiver with redstone when you need a remote stop. Bound links survive pickup.'),
    ('Pulse to FE', 'The Pulse Adapter converts 1 Pulse into 100 FE, up to 20 Pulse each second. It buffers 16,000 FE and exports to standard energy receivers. Redstone stops conversion and extraction.'),
    ('Waystone paths', 'Sneak-use a compass on the top of a solid floor to bind a landing. Use it to return. Keep two clear dry blocks above the floor. Power the landing floor with redstone to lock arrival.'),
    ('Travel tiers', 'Waystone Compass: 128 blocks, same world, 20 Pulse. Horizon Compass: any distance in one world, 40 Pulse. Astral Compass: across dimensions, 100 Pulse. Travel has a 5-second cooldown.'),
    ('The March', 'The Gate Drum opens the way to The March. Bring charged cells and prepare a return route. March materials unlock Astral transport, the final tier for both travellers and cargo.'),
    ('Fivefold Staff', 'Sneak-use to choose a voice; use to cast. Earth slows foes, Fire strikes and ignites, Water heals and cleanses, Air grants a short leap and slow falling, Spirit reveals nearby enemies.'),
    ('Staff costs', 'Earth 12, Fire 18, Water 24, Air 16, Spirit 20 Pulse. Water rests for 8 seconds; other voices for 2. Offensive spells target hostile creatures and stop at solid obstacles.'),
    ('Resonance Maul', 'Hold the maul in your main hand. Sneak-use a stone face to excavate a 3-by-3 plane. Each block costs 8 Pulse. Tool requirements and normal player block protection still apply.'),
    ('Spiritweave', 'Hood: night sight. Robe: resistance. Leggings: speed. Boots: slow falling when descending. Each active piece spends 2 Pulse every four seconds. Carry spare cells for long expeditions.'),
    ('Ritual Brazier', 'Seat a reusable elemental seal. With its matching totem and Pulse nearby, the brazier sustains a blessing for players within 6 blocks. It spends 8 Pulse every two seconds.'),
    ('Camp blessings', 'Earth grants haste; Fire resists flame; Water regenerates; Air slows falls; Spirit grants night sight. Redstone pauses the brazier. Sneak empty-handed to recover its seal.'),
    ('Redstone language', 'High signal pauses workshops, conductors, resonators, collectors, relays and braziers; it locks storage and travel destinations. The Drumheart instead listens for rising edges to make beats.'),
    ('Read the camp', 'Comparators read stored Pulse, inventory or fluid fullness, FE buffer, and relay activity. A pause keeps stored resources intact. Use clocks, levers and comparators to choreograph the camp.'),
    ('Other workshops', 'Lattice recipes use the normal recipe manager and sync to clients. Datapacks can add tribalpower:lattice recipes using shared ingredient tags. JEI displays them when installed.'),
    ('The veil at night', 'After dusk, the sky unfolds in slow curtains of green and violet. The March carries a colder sky. Rain veils the aurora. Client settings can reduce its detail or disable it for shader packs.'),
    ('Gentle harvesting', 'Brush adult Dawn Stags, Lantern Foxes and Mossbacks for renewable reagents. Each rests for one minute between harvests. Young animals do not yield materials. Breed with wheat, sweet berries and seagrass respectively.'),
    ('Broken guardians', 'Ashbound, Rootbound and Reed Stalkers haunt forests and marshes in the Overworld. All can also be found in the March. Build lighted paths: hostile creatures require darkness.'),
    ('Beyond the Gate', 'Steppe: Ashbound, Rootbound, Rift Hounds and Echo Weavers. Highlands: Hollow Sentinels, Storm Moths, Cinder Imps and Rift Hounds. Crystal Fields: Shardbacks, Mourning Bells, Reed Stalkers and Echo Weavers.'),
    ('Answering a spell', 'Storm Moths, Cinder Imps and Mourning Bells gather visible motes before casting. Step behind solid cover to interrupt. Their magic never burns or replaces terrain. A charged staff is useful for keeping distance.'),
    ('Returning the song', 'Bring creature reagents to the Echo workshops. Gentle animal harvests, Storm Wings and Echo Silk bind into Spiritweave under Water. Other hostile remnants attune into two Attuned Echoes under Spirit. Existing recipes remain available.'),
]

def main():
    roster=json.loads((ROOT/'art/creatures/roster.json').read_text(encoding='utf-8'))
    chapters=CHAPTERS+[(r['name'],r['notes']) for r in roster]
    pages = [title.upper() + '\n\n' + body for title, body in chapters]
    source = 'package tk.darrow.tribalpower.guide;\n\nimport java.util.List;\n\n/** Generated by tools/generate_guide.py. */\npublic final class GuidePages {\n    private GuidePages() {}\n    public static List<String> allPages() {\n        return List.of(\n'
    source += ',\n'.join('            ' + json.dumps(page, ensure_ascii=True) for page in pages)
    source += '\n        );\n    }\n}\n'
    (ROOT/'src/main/java/tk/darrow/tribalpower/guide/GuidePages.java').write_text(source, encoding='utf-8')
    data = {'book':'Spirit Codex', 'chapters':[{'id':f'chapter_{i+1}', 'title':title, 'text':body} for i,(title,body) in enumerate(chapters)]}
    (ROOT/'src/main/resources/data/tribalpower/guide/spirit_codex.json').write_text(json.dumps(data,indent=2)+'\n', encoding='utf-8')
    print(f'Generated {len(pages)} guide pages.')

if __name__ == '__main__': main()
