# Tribal Power 5.3.15 - Hearth and Wire

Fourteen new meals, plates that sing to each other across the camp, honest bows, and a Spirit Codex that reads cleanly at any size.

- **Fourteen new Hearth Pot meals**, from March and ordinary ingredients: Drover's Flatbread and Glimmer Crisps for the road, Steppe Pemmican for long marches, a Hunter's Skewer for the chase, Frostberry Preserve against fire, Thistle Tea that cures poison, hunger and nausea, Highland Crumble for climbing, Drift-bell Dumplings for long drops, Delver's Pottage for mining, Fen-caller's Chowder for diving, Reed-wrapped Eel for the dark, Ember-roast Squash before a fight, Long-hunt Stew before a hard night and Bloomsong Custard to heal on the move. Each has its own use, none outdoes the tribe dishes, and every effect length is a setting under `cuisine.fare`.
- **Wireless song plates.** Crouch-use a plate with the Totem Wrench to pick it up, then use the wrench on another plate: the second now hears the first with no thread between. Where you click picks the input. A plate hears up to 8 others within 32 blocks (`plateLinkMax`, `plateLinkRange`); use the wrench on a plate with nothing held to list its links. Loops stay bounded.
- **Plates have owners.** A song plate or Verse plate belongs to whoever set it down, and their camp. Anyone may read one, but strangers can no longer sync, hold, retune or turn your plates.
- **Pulse Bow and Pulse Crossbow, reforged.** Both are now made from Manifested Ingots, last 1,024 shots and mend at the Spirit Anvil like the rest of Spiritgear. Tap-firing no longer deals near-full damage: a bow shot scales with the draw (7 at full), verses add at most 3, the crossbow hits for 10, and bolts fly about 60 blocks. Every number is a setting. The crossbow is held the right way round.
- **Eels you can find.** March swimmers could spawn in water sealed under the land, where nobody sees them, and those hidden fish filled the caps, so open pools stayed almost empty; Silt Eels also shared a tiny cap with the Drift Bells and Veil Rays. Swimmers now come only to open water, and eels count as fish, so the Reed Fen's pools, the Shallows and the Ember Wastes' shore water have eels again.
- **Lighter on the server.** Tribal Benches stop rebuilding their outline on every call (the busiest cost in a town full of benches), Echo Stations look recipes up from an index instead of sorting the whole list for every hopper insert, Ley Collectors read the ley lines once per beat, Lattice Conductors stop rereading redstone and rescanning their chalk network every tick, and familiars stop recomputing their marks.
- **Spawn eggs you can tell apart.** Fifty-four March spawn eggs were near-black with pale spots, many of them identical. Each now wears its creature's own colour, and the Spirit Wisp and March Walker have eggs.
- **The Spirit Codex, polished.**
  - It fits at the default GUI size on a 1080p screen: scenes, captions, the bottom buttons and every chapter on the landing page, which now turns pages.
  - Back keeps your search and your page. Search ignores case and accents and matches every word.
  - Clicking an item opens the page that actually teaches it. The whole spotlight item answers the cursor.
  - The next-step, March events and tribe quest panels are readable; long names shorten instead of running into the facing page; search has its own heading.
  - Pulse costs are what your machines really spend at the default settings, in the book, its recipe panel and recipe viewers alike.
  - Facts corrected throughout, a new Lattice Converter page, and every change of the last two releases described.

Minecraft 1.21.1, NeoForge 21.1.249.

GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v5.3.15
