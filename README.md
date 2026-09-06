# Tribal Power

**Shamanic Technomancy** for Minecraft **1.21.1** (NeoForge **21.1.249**).

Tribal Power binds spirit and machine. Energy is **Spirit Pulse** — rhythmic beats, not FE/RF furnace heat. Processing flows through a **Totem Lattice**: Resonance Totems of elemental attunement linked by song, chalk, and line-of-sight. Ores walk **Echo Stages** (Shatter → Attune → Bind → Manifest) instead of vanishing into a magic smelter.

Authors: **Ahmi & Risika Darrow**  
License: GNU GPL v3 (see `License.txt`)

## Core systems

| System | Role |
| --- | --- |
| **Spirit Pulse (SP)** | Native power API (`PulseHandler`). Drumheart / Ley Collector generation. |
| **Totem Lattice** | Resonance Totems + Song Bench / Lattice Conductor route harmonics. |
| **Echo Stages** | Shatter → Attune → Bind → Manifest ore refinement path. |
| **Ancestral Cache** | Local chest-like storage. |
| **Deep Cache** | Dimension-linked bulk vault (spirit link; UI later). |
| **Seals & Rites** | Seal items + Rite Pedestal for spirit boons / future enchant rites. |
| **Spiritgear** | Tools/armor that lean on Pulse and seals. |
| **Gate Drum** | Portal into **The March** (`tribalpower:the_march`). |

## The March

A shamanic otherworld with March Stone, cobble, woods, leaves, Spirit Reeds, and stub fauna (**Spirit Wisp**, **March Walker**). Use a **Gate Drum** to travel; use it again to return.

## Getting started in-game

1. Open the creative tab **Tribal Power** or craft a **Spirit Codex**.
2. Place a **Drumheart** and right-click to store Pulse (redstone tempo also works).
3. Plant **Resonance Totems** near a **Song Bench** and start a song.
4. Use **Echo** blocks and intermediate ores for the refinement fantasy.
5. Offer seals on a **Rite Pedestal**.
6. Strike a **Gate Drum** to enter The March.

## Development

```bat
gradlew build
gradlew runClient
```

- Mod id: `tribalpower`
- Package: `tk.darrow.tribalpower`
- Legacy 1.9.4 Forge sources: `legacy_1_9_4/` (reference only; ignored by the new build)

Optional: **Patchouli** is declared as an optional dependency. The **Spirit Codex** ships a full multi-page written guidebook regardless.

## Rewrite status

This branch (`rewrite/shamanic-technomancy`) is a literal rewrite scaffold: registrations, Pulse API, working Drumheart / Ancestral Cache behavior, Gate Drum teleport, dimension datapack, guidebook, and cohesive textures — with clear stubs for lattice routing, Deep Cache UI, full Echo recipes, and Spiritgear pulse drain.
