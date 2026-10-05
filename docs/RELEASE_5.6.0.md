# Tribal Power 5.6.0 - The Woven Lattice

Pulse now travels only along the lattice: Lattice Conductors weave it, rank up to carry more, and Pulse Cairns store it.

**Heads up for existing worlds: this is a hard switch.** Machines no longer draw Pulse straight from generators near them. A base built the old way stops until you place a **Lattice Conductor** within 8 blocks of its generators and machines; one conductor in the middle of a small camp is usually enough. Everyone who played before gets a one-time chat message about it.

- **Joining the lattice.** Anything that makes, holds or spends Pulse is on the lattice when it stands within **8 blocks** of a Lattice Conductor: generators, workshops, camp hands, relays, totems and Pulse Cairns.
- **Weaving a network.** Conductors within 8 blocks of each other link into one network. Two groups out of reach of each other share nothing. A redstone signal lifts a conductor out of the weave until it ends.
- **Drawing.** A machine draws only from the generators on its network, then from the network's Pulse Cairns, never straight from a generator beside it.
- **Ranked conductors.** Each conductor carries a set amount a second: **64** Woven, **256** Attuned, **1,024** Bound, **4,096** Manifested. Pulse counts against the conductor it leaves its generator through and the one it reaches its machine through. A top Ley Heart wants a Manifested conductor or four Bound ones. Rank conductors with Echo Attune, Bind and Manifest like any machine.
- **Ranked Pulse Cairns.** Each stone holds **4,000** / **16,000** / **64,000** / **256,000** and moves Pulse at the same rate as a conductor of its rank. A pile fills from its network's generator surplus and lends it back to the network's machines.
- **Totems keep their voice on the lattice.** A Resonance Totem's 250 Pulse buffer fills and sits full while its network holds Pulse. Off the lattice, or on a dry network, it drains and the totem goes silent: no voice, even when its keeping clock says awake. Keeping works as before, separately. A new totem starts full.
- **Ley Binding** joins the conductors around one bound totem to the network around the other while its thread hums.
- **Readouts.** The Spirit Codex diagnosis, the Ley Lens Pulse sight and Jade show which network a block is on, the conductor it draws through and its rate, and cairn and totem state. Stations say when they are off the lattice or their totem is silent.
- **Early game.** The Lattice Conductor costs what your first Echo Shatter does. The guided path gains a step, "Weave the lattice", and the Codex's first workshop places a conductor between the drum and the station.
- **The Ley Heart** counts only voiced totems, so give it a conductor within 8 blocks.
- **Lighter on servers.** Networks are woven once and cached; nothing rescans the world per machine per tick.

Minecraft 1.21.1, NeoForge 21.1.249.

GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v5.6.0
