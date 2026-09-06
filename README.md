# Tribal Power

**Shamanic Technomancy** for Minecraft **1.21.1** (NeoForge **21.1.249**).

Tribal Power binds spirit and machine. Energy is **Spirit Pulse** — rhythmic beats, not FE/RF furnace heat. Processing flows through a **Totem Lattice**: Resonance Totems of elemental attunement linked by song, chalk, and proximity. Ores walk **Echo Stages** (Shatter → Attune → Bind → Manifest) on the **Song Bench** instead of vanishing into a magic smelter.

Authors: **Ahmi & Risika Darrow**  
License: GNU GPL v3 (see `License.txt`)

## Loom-born steward-tribes

Along the Loom, steward-tribes kept rhythm as law. They drummed Spirit Pulse into stone, raised Resonance Totems as attuned posts, and walked ore through Echo until metal remembered the tribe. Tribal Power is that craft — drum, chalk, and honest work — not furnace vanity and not an outside pantheon.

## Core systems

| System | Role |
| --- | --- |
| **Spirit Pulse (SP)** | Native power API (`PulseHandler`). Drumheart / Ley Collector generation; Totem buffers. |
| **Totem Lattice** | Resonance Totems + Ritual Chalk links + Song Bench pulse draw. |
| **Echo Stages** | Live Song Bench refine loop: Shatter → Attune → Bind → Manifest. |
| **Ancestral Cache** | Local chest-like storage. |
| **Deep Cache** | March-linked bulk vault (spirit link after visiting The March). |
| **Seals & Rites** | Seal items + Rite Pedestal spirit boons. |
| **Spiritgear** | Pulse-fed tools (Pulse Cells in inventory). |
| **Gate Drum** | Portal into **The March** (`tribalpower:the_march`). |

## Echo refine loop (Song Bench)

Live ore processing is **not** the Echo Shatter / Attune / Bind / Manifest shrine blocks — those are camp markers. Refinement runs on the **Song Bench**:

1. Place a **Drumheart** and/or **Ley Collector** within 8 blocks (and optionally charge Totems).
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

A shamanic otherworld with March Stone, cobble, woods, leaves, Spirit Reeds, and fauna (**Spirit Wisp**, **March Walker**). Charge a **Gate Drum** with Pulse and strike it to travel; use it again to return.

## Getting started in-game

1. Open the creative tab **Tribal Power** or craft / take a **Spirit Codex**.
2. Place a **Drumheart** and right-click to store Pulse (redstone tempo also works).
3. Plant the needed **Resonance Totems** near a **Song Bench**.
4. Seat raw ore / cobble / Echo grit on the bench and start the song.
5. Offer seals on a **Rite Pedestal**.
6. Charge and strike a **Gate Drum** to enter The March.

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

Branch `rewrite/shamanic-technomancy`: Pulse API, Drumheart / Ley Collector, Totem Lattice + chalk links, **working Song Bench Echo refine loop**, Ancestral / Deep Cache, Gate Drum + The March, Spiritgear Pulse cells, seals/rites, and Spirit Codex pages aligned to live behavior.
