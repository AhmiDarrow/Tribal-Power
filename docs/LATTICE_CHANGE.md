# The Pulse lattice

**Heads up for existing worlds: this is a hard switch.** Machines no longer draw Pulse straight from generators
within 8 blocks of them. Pulse now travels only along the lattice. A base built the old way stops until you place a
**Lattice Conductor** within 8 blocks of its generators and machines. One conductor in the middle of a small camp is
usually all it takes. Every player who played before the update gets a one-time chat message about it when they
next log in.

## How Pulse moves now

- **Joining.** Anything that makes, holds or spends Pulse is on the lattice when it stands within 8 blocks of a
  Lattice Conductor. That covers generators, workshops, camp hands, relays, totems and Pulse Cairns. "Within 8" is the
  same 17x17x17 cube every lattice reach already used.
- **Linking.** Conductors within 8 blocks of each other link up, and together they make one network. Two groups of
  conductors out of reach of each other are two networks and share nothing. A redstone signal lifts a conductor out
  of the weave until the signal ends.
- **Drawing.** A machine draws only from the generators on its network, then from the network's Pulse Cairns. It never
  draws straight from a generator, even one right beside it. Totem buffers and station buffers are never drawn.
- **Throughput.** Each conductor carries a set amount a second, by rank. Pulse leaves a generator through a conductor
  within 8 of it and reaches a machine through a conductor within 8 of the machine, and it counts against both. If it
  leaves and arrives through the same conductor, it counts once. The links between conductors carry freely, so a
  network carries as much as all its conductors together. A big generator needs strong or many conductors around it:
  a top Ley Heart wants a Manifested conductor, or four Bound ones.
- **Ley Binding.** While the rite's thread hums, the conductors in reach of one bound totem join the network of the
  conductors in reach of the other.

| Rank | Conductor carries | Pulse Cairn stone holds | Pulse Cairn stone moves in / out |
|---|---|---|---|
| Woven (0) | 64 / s | 4,000 | 64 / s |
| Attuned (1) | 256 / s | 16,000 | 256 / s |
| Bound (2) | 1,024 / s | 64,000 | 1,024 / s |
| Manifested (3) | 4,096 / s | 256,000 | 4,096 / s |

Conductors and Pulse Cairns rank the same way as any other machine: Echo Attune, then Echo Bind, then Echo Manifest.
Conductors and cairns from older worlds count as Woven.

## Pulse Cairns

Pulse Cairns are the lattice's storage. A pile within reach of a conductor fills from the network's generators, but
only from their surplus (whatever sits above half a generator's store), so machines keep their working Pulse. It
then lends that Pulse back to the network's machines. Touching stones are still one pile, up to 64 stones, and the
pile moves Pulse at its stones' rates added together. The old fixed 200 Pulse a second per stone is gone.

## Totems

- Each Resonance Totem still keeps a **250 Pulse** buffer. On a network that has Pulse in it, the totem fills its
  buffer and then sits full, which costs nothing more.
- Off the lattice, or on a dry network, the buffer drains 2 Pulse a second. A totem with an empty buffer is
  **silent** and gives no voice to stations, camp hands or relays, even if its keeping clock says it is awake.
- Keeping (Answered, Dim, Quiet) works exactly as before and runs separately. A quiet totem with a full buffer still
  has its voice but is quiet. An awake totem with an empty buffer has no voice at all.
- A newly placed totem starts with a full buffer, so it speaks for about two minutes before it needs a lattice.
- Totems are no longer a Pulse source for machines. The conductor no longer pushes Pulse into chalk-linked totems.
  Ritual Chalk links still carry voices.

## Readouts

- **Codex diagnosis** (crouch and right-click with the Spirit Codex) shows the lattice network a block is on, or says
  it is off the lattice. It also names the conductor it draws through, with that conductor's rank, rate and what it
  carried last second, and lists the network's generators and stored Pulse. For conductors, cairns and totems it
  adds their rank, capacity, rate and buffer state.
- **Ley Lens, Pulse sight** shows the network you stand on: its conductors and total rate, your conductor's rank and
  how much it carried, and the network's stored Pulse and flow. It outlines each conductor's reach, with the one you
  draw through outlined brightest. Off the lattice it says to place a conductor.
- **Jade** shows network membership and the tapped rate on Pulse blocks, plus rank and rate on conductors and cairns.
- Echo stations now say "No Lattice Conductor within 8 blocks" when they are off the lattice. They say "Its totem is
  silent" when their totem's buffer has run dry.

## Early game

The Lattice Conductor recipe hasn't changed. It takes four Copper Resonators around a redstone dust and makes four
conductors, using the same parts as your first Echo Shatter. The guided path has a new step between the Drumheart
and Echo Shatter: "Weave the lattice", craft a Lattice Conductor. The Codex's getting started chapter now places a
conductor between the drum and the first station.

## Performance

Networks are woven once and cached. A conductor arriving or leaving (placed, broken, chunk loaded or unloaded,
redstone lock changed) or a Ley Binding starting or ending re-weaves them. A generator, cairn or totem arriving or
leaving only makes the networks in reach re-read their members. A machine's draw is a cached lookup of the conductors
in its reach plus a walk over its network's sources. Nothing rescans the world per machine per tick any more.
