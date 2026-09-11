# Tribal Power 3.1.0 — The Listening Pit and the Gates

## Two balance changes you should read first

- **The Drumheart's redstone path is retuned.** It now pays by the same tempo the hand already used: a rising edge 17 to 23 ticks after the last one is worth 24 Pulse, anything else 10, and nothing closer than 8 ticks counts at all. **A one-second clock is a large buff (5/pulse on an 8-tick cooldown became 24 a second). A fast spam clock is a nerf.** If you have a Drumheart farm, look at its clock.
- **Tablet rites now need a Rite Circle.** Brazier blessings are unchanged and still need nothing but a seated seal. A *tablet* rite wants the brazier in the middle, four Rite Pedestals on the diagonals, and chalk joining them. Seat four totems of the rite's own voice on the cardinals and the rite costs a quarter less and lasts twice as long. Existing braziers keep their seals; the first tablet you use will tell you the circle is missing.

## Placement is the ritual

Devices that claim to be rites now have to be arranged, not scattered. Seven shapes, all rotation-independent: the Stone Font, the Listening Pit, the Rite Circle, the Voice Ring, the Shatter Array and both gates. Sneak-use the Spirit Codex on any anchor block and it names the tier, the facing, and the first three things that are missing — with particle ghosts where they should go.

Two of the shapes are opt-in bonuses on things that already worked, and neither changes anything if you ignore it: a **Voice Ring** of totems at radius 3 around a Pulse Resonator (a scattered heap still works, but counts at most five voices, so six-voice resonance now means *arranged*), and a **Shatter Array** around an Echo station — four matching totems at the corners and a cache beneath, for a quarter off the Pulse and output that empties itself.

## The Listening Pit

The Grit-singers' machine. Lay stone, brace four corners with **Anchor Stones**, chalk from each edge to the middle, and set a **Resonance Mesh** at the centre with an Ancestral Cache beside it and the Earth voice in reach. It calls up exactly what a silk-touch mining trip would: the raw item for a metal, the ore block for a gem.

- Four bands: common (coal, copper, iron), deep (gold, redstone, lapis), hot (the Nether's quartz and gold, never ancient debris) and rare (diamond, emerald, and whatever a modded pack added).
- The seven-by-seven **Deep Listening** shape opens the deeper bands, and the Grit-singers gate them by standing: Friend, Kin, then Voice with their Kinship Totem inside the pattern.
- A **sample** on top is a filter, never an ingredient — it is not consumed, and what it names weighs six times as much.
- Every voice trades one thing for another rather than multiplying yield: Water washes for 250 mB and an extra output every third cycle, Air quickens the cycle and charges more per second so the Pulse per item does not move, Fire opens the hot band at twice the Pulse, Spirit listens a band deeper, the Loom halves the substrate.
- Substrates are all renewable on purpose. Per iron ingot the pit costs about 70 Pulse against 20 for shattering ore you mined: **mining stays efficient; the pit buys walking away from it.**

## Metals and gems are two different crafts

Grit now means one thing: crushed metal waiting for the fire. A metal goes raw item or silk-touched ore → grit → ingot in any furnace. A gem or mineral skips the fire entirely — silk-touched ore shatters straight into the gem at twice the ore's own drop (coal, diamond, emerald and quartz give 2; redstone and lapis give 8). The gem itself is never an input, because gem-to-gem would be a dupe.

Materials are discovered from the common tags on every reload, so **a modded metal works with no datapack**: it rides one `Mineral Grit` item that names its own material and fires into the right ingot in a plain furnace. `iron_grit`, `copper_grit` and `gold_grit` keep their own item ids, so old saves and old recipes mean exactly what they meant.

## The Gates

The Gate Drum is untouched — it stays the portable, personal way into the March. These are the built ones.

- **Way Gate**: a five-by-five frame ring without its corners, keystone at the bottom centre, 400 Pulse to light out of its own 600, 20 Pulse per traveller. Linked with a bound Waystone Compass or with chalk, within 64 blocks.
- **Far Gate**: a taller frame braced by four anchor stones, with the Loom voice nearby — so crossing dimensions sits behind The Unsung. Linked with a **Gate Sigil**, 120 Pulse per traveller.
- **Players, mobs and dropped items all pass**, which makes a gate pair bulk logistics where an astral relay is a trickle. Both stay useful: the relay needs no frame and no Loom.
- A gate lands you inside the far gate's own interior and never terraforms; if there is nowhere to stand, the trip refuses and the Pulse comes back. The far end is held loaded around your arrival, so an unloaded partner still answers. Break a keystone and its partner is released and anyone nearby is told.
- `/tribalpower gate list` and `/tribalpower gate unlink <name>`.

## One voice, one source

Six generators, one per voice, each with a different input — and each the craft of the tribe that keeps that voice, which is what finally makes standing pay.

| Voice | Block | Input | Output |
| --- | --- | --- | --- |
| Earth | Drumheart | rhythm | 24/s on tempo, 10/s off |
| Fire | **Ember Horn** | furnace fuel | up to 20/s while lit |
| Air | **Wind Harp** | altitude, open sky, weather | 2/s at y 80 under open sky → 6/s high in a storm |
| Water | **Wave Drum** | adjacent water, or piped | 3/s beside water, 10/s for 100 mB/s, doubled in rain |
| Spirit | **Wake Bell** | nearby deaths | 20 a hostile, 5 a passive, tolled out at ≤8/s |
| Loom | **Loom Anchor** | the system itself | 1/s per distinct voice, max 6, doubled during a Ley Binding |

Anything free is rate-limited: harps within 12 blocks divide the wind, passive wave drums within 8 divide the water, and a Wake Bell fills a 2,000 reservoir and then idles rather than scaling to absurdity. The Ember Horn and the piped Wave Drum are uncapped by design — they burn something. **No automatable loop is net positive**: coal through the pit and back costs more than it returns, and Fortune only closes that to break-even — and cannot be automated.

The Ley Collector is unchanged and keeps its place as the neutral, voice-less starter passive. New **Pulse Cairns** hold 4,000 Pulse each and stack five to a column, which is what turns a mob farm's bursty Spirit generation into the steady draw a pit wants.

## Everything a player can do by hand, a base can do on a clock

A held signal stills the work; a struck signal calls it once. Held now also stills the Resonance Mesh, Stone Font, gate keystones, Rite Pedestals and every generator. **Rite Pedestals hold and show one item**, and a comparator reads whether they are occupied — which is the whole of what an automatic rite loop needs: a relay restocks a pedestal, a clock strikes the brazier, the rite fires and spends the tablet, and the comparator reports the pedestal empty. No fake players, no hidden inventories.

Comparators read the mesh's cycle progress, the font's progress, a pedestal's occupancy, a keystone (0 unlinked, 1–14 stored Pulse, 15 mid-transit, plus a two-tick pulse on transit) and every generator's buffer.

## Also

- **Stone Font**: the starter rite, and the tutorial for patterns. A font and four chalk marks asks for cobblestone at 4 Pulse a second — slow enough to run on a hand-drummed Drumheart, which is the point. Brace it and bring Earth for stone; add Fire and Water for obsidian.
- **Spring Calling**: Rain Calling's small sibling. 300 Pulse marks an adjacent Spirit Cistern a spring, refilling 100 mB/s for ten minutes. It is where the water for a Wave Drum, for Washing, and for anything else that drinks comes from. March-side lava stays a manual haul on purpose.
- **Tribe Hearth offerings are capped at 60 standing per day per tribe**, the same idiom as the existing per-kill cap. Without it, renewable ore would feed the hearth that gates the pit that makes the ore.
- Every tribe that keeps a voice now sells that voice's craft, and shows one working in its camp: a Stone Font at the Pad-keepers, a braced font and a whole Listening Pit at the Grit-singers, an Ember Horn at the Drumhearts, a Wind Harp at the Pattern-weavers, a Wave Drum at the Rootbinders, a Wake Bell at the Seal-carvers, a Loom Anchor at the Crystal Spire. Each is swapped in for the least load-bearing offer at that rank, so every counter still holds six offers, two per rank.
- **Config** (`tribalpower-common.toml`): `generationMultiplier`, `pitSpeedMultiplier`, `gateTravelCost`, `farGateTravelCost`, `enableFarGates` and `gritDenyList`.
- **Camp membership** is honoured by every new device with an owner: gate keystones, the Resonance Mesh, the Stone Font and Rite Pedestals.
- **Advancements**: Draw the Circle, Stone Stays, Ask the Ground, Open the Way, The Long Thread, Six Voices Singing.
- **Codex**: 24 new teachings, including a diagram for each of the seven patterns drawn from the pattern itself — so a diagram can never drift from what the matcher actually wants — and a factor breakdown per generator in the same integers the server uses.
- **Sounds**: original short hits for the Drumheart, the new generators, the mesh, the font, chalk and the gates. The Drum Circle music disc from the design spec is still unauthored.

## Compatibility

Content only. No existing id, storage slot or save changes meaning. New structures appear in newly generated chunks; the seven camps that gained a prop are regenerated, so existing camps in an existing world keep the layout they were generated with.

The Rite Pedestal gains a block entity it did not have in 3.0. A pedestal placed in a 3.0 world wakes up empty and working, and keeps whatever it was holding.

Minecraft 1.21.1, NeoForge 21.1.249, client and server.
