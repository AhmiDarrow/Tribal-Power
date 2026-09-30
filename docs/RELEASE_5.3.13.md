# Tribal Power 5.3.13 - Mended and Measured

A long sweep through the camp, the March and the Codex: exploits closed, owners respected, stuck machines unstuck, and a lighter tick for big bases.

- **Exploits closed.** Items taken by hand from lattice machines, relays, horns and benches no longer come back after a reload. A shift-clicked Song Bench result that did not fit is dropped at your feet instead of lost. Re-mining a silk-touched ore no longer pays its XP again, and auto-smelting keeps a recipe's full output. One marked wall can no longer be handed in for every building request a tribe asks for.
- **What's yours stays yours.** Strangers can no longer re-link or cut your gate keystones, re-bind your Wireless Relay or flip its link, or brush your bonded animals for the double reagent. Relays placed before this update stay open to everyone.
- **Machines that tell the truth.** Comparators hear a Stone Font or Resonance Mesh stall, and the Silent Drum's comparator drops when its glow fades. The Wind Snare and Ward Drum no longer waste Pulse on a draw that comes up short. The Codex and Ley Lens show the Ley Collector's real rate, and the Pulse Gauge no longer reads nonsense for huge stores. Logic plates only hear signals pointed at them.
- **Voices agree.** A Kinship Totem now counts for the voice gate everywhere a kept Resonance Totem does, even beside a quiet one. A machine that is waiting for its voice no longer hands items to its neighbours.
- **The Grove Tender fells trees, not houses.** It cuts only grown trees and their wild leaves, leaves log builds alone, and fells tall and wide trees to the ground. The Spiritgear Hoe replants only when a seed actually came out of the harvest.
- **Tools behave.** The Weaver's Wand respects protected land and skips blocks that could not stand where it puts them. The Totem Wrench will not turn an extended piston, and it turns wall torches and ladders to a wall that holds them.
- **Generators count honestly.** Wave Drums and Wind Harps count crowding neighbours within their full range, as the Codex says (8 and 12 blocks).
- **Duels and rites.** A drum practice that lost its result no longer locks you out of playing until you relog, and neither practice nor rite can be finished from another dimension.
- **Familiars and guardians.** A revived familiar sheds the stat changes of effects it no longer has. Wild remnants fight back against someone else's familiar. A Mossback's saddlebag closes when the Mossback leaves the dimension. A guardian cleared away while its altar was unloaded no longer leaves the altar refusing forever, and a guardian that returns to sleep unbeaten does not make its altar rest (the Codex now says so).
- **Boss themes play.** They faded in from silence, and the sound engine threw away every sound that started silent.
- **Screens.**
  - The Codex no longer piles wrapped recipe labels on top of each other or runs a scene caption under the step buttons. The surge timer no longer reads "about 0 minutes".
  - Recipe viewer panels wrap their cost and circle text and grow to fit.
  - The familiar panel trims long names, and Elder dialogue no longer runs under the answers.
  - The Songkeeper survives a server with fewer songs, and duel invites count down properly.
- **The Codex, proofread.** About sixty item and block names are now spelled exactly as the game names them, so they link. Pages that packed two bullets into one now show both. Relic names no longer print twice. Several facts were corrected: the Grand Pulse Cell recipe, the Attuned rank's cost floor and the Harmonic Energizer's name.
- **Quieter logs.** March Stools no longer ask for a block entity they never had, which logged a warning for every stool on every chunk load.
- **Lighter on the server.** Machines find their Pulse in one pass instead of two. Conductor lines are remembered until something on them changes. The Echo Station finds its totems once per beat. Pipe-fed Spirit Cisterns stop sending an update twenty times a second. Ley ropes are sent only when they change. Item data read every tick or frame is no longer copied.

Minecraft 1.21.1, NeoForge 21.1.249.

GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v5.3.13
