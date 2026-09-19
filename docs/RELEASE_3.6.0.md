# Tribal Power 3.6.0 - The living March

- **Spirit Codex readability overhaul.** Every entry is rewritten in plain language, as one player would explain it to another. How-to steps live on the same page as the thing they explain instead of in separate walkthroughs. Any mention of another entry or item is a link you can click. Scenes animate one clear step at a time, and fluids now draw properly in them.
- **Gear worth the grind.** Spiritgear and Spiritweave ranks now need **catalysts** in the Echo station and take real time and Pulse: Attune needs Attuned Echo, Bind needs Bound Echo, Manifest needs Resonant Cores and Loom Thread from The Unsung. Each rank adds a lot more. A **Manifested blade** gains +10 damage, hits bosses 25% harder and heals you for a tenth of every blow. A **full Manifested Spiritweave set** gives about 29 armour, 20 toughness and 8 extra hearts, takes 20% less damage and mends you as you fight.
- **A Pulse cell in every piece.** Right-click a Pulse Cell onto any Spiritgear tool, Spiritweave piece, the Resonance Maul or the Sixfold Staff to seat it. The piece spends from its own cell first and only reaches for the cells you carry once it runs dry. Right-click a Greater Pulse Cell onto it to upgrade, and the old cell comes back to you.
- **Totem-bound gear glows.** Voiced Spiritweave and Spiritgear shine in their voice's colour, brighter with each rank.
- **Station screens polished.** Clear Input, Catalyst and Output areas, a progress arrow, live Pulse cost and time left, and a cleaner side input/output pad with hover help.
- **Machines repainted.** Wind Snare, Ward Drum, Tide Pump and Seal Loom have proper models to match the rest of the mod. The Pulse Gauge and Threshold are restyled.
- **Fixes.** The Listening Pit feeds itself from a nearby Ancestral Cache. The Unsung now drops the Seal-Carver's Rite tablet.

## The March

- **The Gate Rite.** A Gate Drum no longer runs on Pulse: the rhythm is its power. Strike the drum and a drumming song begins, one of six original tracks of drums, drone, flute and chant, 20 to 30 seconds long. Its drum beats fall toward the line on four drums in a row, and you strike them with the music: A for the deep frame drum, S the low tom, D the high hand drum, F the rattle (the arrow keys work too). Land 60% and the gate opens and carries you through. Fail or stop, and simply strike the drum again. Drums from earlier versions keep working; any Pulse they held is no longer needed.
- **27 new places to find.** Shrines, standing stones, watchtowers, quarries, a fallen drum tower, frozen longhalls, forges in the ash, a stilt village, a sunken shrine, an observatory under a broken dome and more, spread across every land. Three deep places, the Old Gate Ruin, the Ash Tomb and the Crystal Grotto, keep guarded vaults that can hold a Resonant Core or Loom Thread. See **Places of the March** in the Codex.
- **Six trees of its own, each with its own wood.**
  - The **Weeping Colossus** grows in groves of two to eight in the Reed Fen, Highlands and Steppe. It stands over a hundred blocks tall with a hollow trunk, and its crown lets fall curtains of climbable **Willow Strands**. Plant four Willow Saplings in a square to grow your own.
  - The **Hearthoak**, **Bellcap**, **Frostpine**, **Cinder Snag** (whose wood does not burn) and **Strider**, which stands on stilt roots over the fen.
  - Every tree has its own logs, bark, stripped wood, planks, stairs, slabs, fence, gate, door, trapdoor, pressure plate, button, leaves and sapling.
- **Build with the March.** March Stone, Cobble, Stone Bricks and Polished March Stone come as stairs, slabs and walls. Chiseled March Stone Bricks are new, and March planks make a full wood set. The Stonecutter works on all of them.
- **Water and what lives in it.** Meres, pools and tarns across the March. **Glimmerfin** swim in schools (bucket them, or cook them). **Drift Bells** glow in still water, and their jelly lets you breathe underwater and see in the dark. **Veil Rays** glide away from you. **Silt Eels** bite anyone wading through the Reed Fen.
- **Water and caves come alive.** Lily pads and the glowing Moon Lily float on the meres, Ribbon Weed sways below them over beds of March Silt and clay. Underground, Veil Lichen glows faintly on cave walls, Echo Roots hang from the rock and Lantern Cap mushrooms light the floors.
- **Life in the air.** Flocks of **Loom Swifts** wheel over the open land. Fireflies, reed darters, glasswings and cinder gnats fill the air near the ground. Insects are drawn only on your own screen, capped and turned off on Minimal particles, so they cost the server nothing.
- **Companions, like wolves.** Win a wild March creature over by feeding it its favourite food: the gentle animals take their usual food, remnants their own reagent (Voice still required). The Bonding Charm still works, and faster. Feed a hurt companion to heal it. The young of two companions you own are born into your company.
- **Stats you can see, and breeding that pays.** Sneak and look at any March creature, or simply look at your own, to see its Frame, Stride, Fang, Hum and Keep, its Marks, its generation and its bloodline total. Two parents strong in a stat can give a child a step better than either: breeding is now the only way to reach 4, and the new **Exalted** 5 (+48%).
- **Water life that actually spawns.** Glimmerfin, Drift Bells, Veil Rays and Silt Eels now spawn in shallow meres and pools, not only deep water. Vanilla fish that could never spawn in March lakes were removed from the Reed Fen.
- **The March is dangerous.** Its spirits now walk by day as well as by night, though never where torchlight falls, so a lit camp is safe. They come in bigger packs, and they are stronger after dark. Some rise as **Elites**, named in gold: much tougher, harder-hitting and worth triple experience.

**Existing worlds:** the March has changed enough that it regenerates once more. The first time a save loads 3.6.0, its old March is moved to `tribalpower_backups` inside the save and the dimension regenerates as you explore. Nothing is deleted. To keep an old March exactly as it is, set `retrogenMarch = false` in `tribalpower-common.toml` before loading. The Overworld is untouched.

Minecraft 1.21.1, NeoForge 21.1.249.

GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v3.6.0
