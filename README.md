# Tribal Power

**Shamanic Technomancy** for Minecraft **1.21.1** (NeoForge **21.1.249**).

Tribal Power binds spirit and machine. Energy is **Spirit Pulse** — rhythmic beats, not FE/RF furnace heat. Processing flows through a **Totem Lattice**: Resonance Totems of elemental attunement linked by song, chalk, and proximity. Ores walk **Echo Stages** (Shatter → Attune → Bind → Manifest) on the **Song Bench** instead of vanishing into a magic smelter.

Authors: **Ahmi & Risika Darrow**  
License: GNU GPL v3 (see `License.txt`)

## Loom-born steward-tribes

Shamanic technomancy was born from a great Loom tended by steward-tribes. Along its threads they kept rhythm as law: drumming Spirit Pulse into stone, raising Resonance Totems as attuned posts, and walking ore through Echo until metal remembered the tribe. Tribal Power is that craft — drum, chalk, and honest work — not furnace vanity and not an outside pantheon.

## Core systems

| System | Role |
| --- | --- |
| **Spirit Pulse (SP)** | Native power API (`PulseHandler`). Drumheart / Ley Collector / **Pulse Resonator** (coal-fueled) generation; Totem buffers. |
| **Totem Lattice** | Resonance Totems + Ritual Chalk links + Lattice Conductor Pulse routing + Song Bench pulse draw. |
| **Echo Stages** | Live Song Bench refine loop: Shatter → Attune → Bind → Manifest. |
| **Ancestral Cache** | Local chest-like storage. |
| **Deep Cache** | March-linked bulk vault (spirit link after visiting The March; Pulse-cell link costs 5 when unvisited). |
| **Seals & Rites** | Seal items + Rite Pedestal spirit boons (amplify with Pulse Cells). |
| **Spiritgear** | Pulse-fed tools (Pulse Cells in inventory; mining 2 / use-on 1 / blade hit 3). |
| **Gate Drum** | Portal into **The March** (`tribalpower:the_march`); travel costs **20 Pulse**. |

## Echo refine loop (Song Bench)

Live ore processing is **not** the Echo Shatter / Attune / Bind / Manifest shrine blocks — those are camp markers. Refinement runs on the **Song Bench**:

1. Place a **Drumheart**, **Ley Collector**, and/or **Pulse Resonator** within 8 blocks (and optionally charge Totems).
2. Place the stage’s **Resonance Totem** nearby (or chalk-link one within 16).
3. Right-click the bench with a feed item to seat one stack unit.
4. Empty-hand click to start the song. Shift-click to remove the item.

| Stage | Attunement | Input | Output | Pulse / tick | Work ticks |
| --- | --- | --- | --- | --- | --- |
| Shatter | Earth | Raw ore, cobble/stone, iron-like metal, March grit | `echo_shard` | 8 | 40 |
| Attune | Fire | `echo_shard` | `attuned_echo` | 10 | 60 |
| Bind | Water | `attuned_echo` | `bound_echo` | 12 | 80 |
| Manifest | Spirit | `bound_echo` | `manifested_ingot` | 16 | 100 |

Stage table is code-defined in `tk.darrow.tribalpower.echo.EchoStage`. The song can chain stages while singing if the next attunement and Pulse remain available; it stops when a **Manifested Ingot** is produced.

## The March

A shamanic otherworld reached by **Gate Drum** (20 Pulse). **Honest status:** The March still uses a **thickened flat** chunk generator (`minecraft:flat` with deep March Stone / cobble / soil / grass), not full noise terrain. That keeps Gate Drum travel, Deep Cache spirit-link, and recipes stable while biome decoration does the “extensive” work:

- Surface moss patches (`march_moss`), Ley Thistle, March Leaf, Spirit Reed, Echo Bloom
- March trees, crystal clusters, and sparse crystal-capped **ruin pillars**
- Ore veins in the deep stone/cobble band
- Registered spawn placements for **March Walker** / **Spirit Wisp**

Charge a **Gate Drum** with Pulse (sneak-drum or Pulse Cell), then strike it to travel; use it again to return.

## Getting started in-game

1. Open the creative tab **Tribal Power** or craft / take a **Spirit Codex**.
2. Place a **Drumheart** and right-click to store Pulse (redstone tempo also works). Feed a **Pulse Resonator** with coal/charcoal for steadier fueled generation.
3. Plant the needed **Resonance Totems** near a **Song Bench**; seal lasting links with **Ritual Chalk**.
4. Place a **Lattice Conductor** near the chalk-linked ring to push Pulse into totem buffers (and assist distant Song Benches).
5. Seat raw ore / cobble / Echo grit on the bench and start the song.
6. Offer seals on a **Rite Pedestal**.
7. Charge and strike a **Gate Drum** (20 Pulse) to enter The March.

### Pulse Resonator

Coal-fueled Spirit Pulse generator (Ley alternative — no FE API):

- Buffer **2500** Pulse; while burning, gains **~4 Pulse / 20 ticks** (~320 per coal/charcoal at 1600 burn ticks).
- Right-click with coal/charcoal to load fuel; empty-hand for status; shift-click to remove unused fuel.
- Pulse Cells charge from it like a Drumheart (up to 25 / click).
- Lattice Conductor / Song Bench treat it as a generator alongside Drumheart and Ley Collector.

## Development

```bat
gradlew build
gradlew runClient
```

- Mod id: `tribalpower`
- Package: `tk.darrow.tribalpower`
- Legacy 1.9.4 Forge sources: `legacy_1_9_4/` (reference only; ignored by the new build)

Optional: **Patchouli** is declared as an optional dependency. The **Spirit Codex** ships a full multi-page written guidebook regardless (`GuidePages` + `data/tribalpower/guide/spirit_codex.json`).

## Rewrite status

Branch `rewrite/shamanic-technomancy`: Pulse API, Drumheart / Ley Collector / **Pulse Resonator**, Totem Lattice + chalk links, **Lattice Conductor Pulse routing / Song Bench assist / Echo item handoff**, **working Song Bench Echo refine loop**, Ancestral / Deep Cache, Gate Drum travel (20 Pulse) + The March, Spiritgear Pulse cells, seals/rites, and Spirit Codex pages aligned to live behavior.

### Lattice Conductor (live)

With **2+ chalk-linked Resonance Totems** and a Conductor within 8 blocks of the ring:

1. Right-click the Conductor to see **network size** and burst-transfer Pulse from nearby **Drumheart / Ley Collector / Pulse Resonator** into linked **totem buffers**.
2. While generators supply Pulse, the Conductor keeps pushing (~10 / second) and **assists** Song Benches on the network that have seated grit (priority fill for totems near those benches).
3. When powered, it can move Echo grit: finished products Song Bench → Ancestral Cache, processable feeds Cache → empty Bench, and idle Bench → Bench handoff.

Still stubs / thin: **March ambient noise / spirit-link tick effects** are reserved hooks, not live systems. The March is intentionally **flat+features** until a noise generator can be added without risking Gate Drum / Echo / recipe breakage.
