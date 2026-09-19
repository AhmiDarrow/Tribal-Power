# Tribal Power 3.6.2 - Plain sight

Minecraft 1.21.1, NeoForge 21.1.249. Bug fixes and an art pass. Existing saves load as they are.

**Art**

- **Creature drops are their own objects.** Bell Fragment, Rift Tooth, Storm Wing, Reed Fang, Prism Carapace, Ember Heart, Cinder Knot, Echo Silk, Knotted Root, Lantern Down, Dawn Velvet, Mossback Scale and Sentinel Sigil were one recoloured crystal between them. Each now has its own shape.
- **Echo catalysts too.** Echo Shard, Attuned Echo, Bound Echo and Spirit Shard are drawn, not tinted copies.
- **Grits** are clean heaps with defined grains instead of noise.
- **The six exotic woods** get pixel rings on their log tops, muted plank ramps with real seams, and a door, trapdoor and door item of their own: willow lattice, hearthoak Z-brace, bellcap porthole, frostpine herringbone, cinder straps and grille, strider louvres.
- **Machine tops** tell their machines apart: lids on caches, ranked ports on relays, a laced head on drums, the element on totems, and a separate surface for each station, brazier, pedestal, cistern and bench.
- Spirit Urn, Woven Mat, Wind Charm and Resonant Core are drawn at 32x32 like the rest, and the Glasswing particle is a butterfly, not a missing-texture checker.

**Fixes**

- **Gates no longer bounce you.** Standing in the arrival plane sent a player or item back and forth between the two gates, paying Pulse every trip. The cooldown is now kept topped up while you are inside, as vanilla portals do.
- **Six Voices** could only be awarded from 1 anchor position in 20. The check now uses the generator's own timing.
- **The Unsung** no longer discards a weaver you bonded during the fight.
- **Familiars keep their health.** An injured familiar healed to full on every chunk load or relog.
- **Ward and Fire charms** stop when their upkeep goes unpaid, like every other charm.
- **Camp anchors** from a world you closed earlier no longer count against a new world's budget.
- **Spiritgear swings belong to their player.** Another player's break paid no Pulse and cost no durability, and drops from TNT or farms in that window picked up Fire, Water and Loom perks.
- **Resonance Maul** no longer blocks the off-hand, so a torch places as it should.
- **Spirit leggings'** step height no longer stays on after you put them in a chest.
- **Air shovel** paths check spawn and claim protection, and cost durability honestly.
- **Machines cost much less each tick.** Pulse draw probed a 17x17x17 cube, about 5,000 to 20,000 lookups per working machine per tick; it now walks the chunk's block entities in the same order.
- **Stone Font and Resonance Mesh** no longer drain their water and lava into a cistern placed below.
- **Logic plates** save their memory, so a Tally survives a restart part-way through its count.
- **A sapling-grown Weeping Colossus** stays inside loaded chunks.

Minecraft 1.21.1 / NeoForge 21.1.249 / Java 21.
