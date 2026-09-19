# Tribal Power 3.6.1 - Safe keeping

Minecraft 1.21.1, NeoForge 21.1.249. A bug-fix release; existing saves load as they are.

- **Owned devices stay yours.** Other players can no longer break your Camp blocks, Stone Font, Gate Keystone or Resonance Mesh. Before, they could break them and take what was inside.
- **Flight from other mods works again.** The air charm used to switch off flight from rings, jetpacks and similar items every tick. It now only takes away flight that it gave.
- **Seated Pulse cells are not lost.** When a Spiritweave piece breaks, the cell seated in it comes back to you.
- **The March reset is safer.** Players are only marked as moved once they really are out of the regenerated March. A reset interrupted halfway is rolled back. Gates that still pointed at March keystones the reset erased unlink instead of failing.
- **Dock stalls keep their stock.** Stall stock no longer refills when you relog or leave the area. Stall trades no longer give standing; Elder trades still do.
- **Offerings and rites.** The hearth no longer takes offerings once its daily limit is reached. Weather and dawn rites now work when used outside the Overworld. A Drum Rite that the server refuses on a lagging server now tells you so.
- **The Unsung can always be finished.** Without a Silent Drum (or after its drum is broken), it opens up to damage on a timer.
- **Name-tagged March creatures stay wild.** Naming a hostile creature no longer makes it peaceful.
- **Ley Lens.** Pulse mode shows real stored amounts.
- **Performance.** Song Benches, Echo Station screens, following companions and the Ley Lens do much less work each tick. Wireless relays no longer load chunks by themselves.
- **Other fixes.**
  - Side output no longer loses fluid.
  - Charms now drop with the rest of your items, so grave mods collect them.
  - Spawn handling no longer touches world data from world-generation threads.
  - A damaged Codex entry no longer blanks the whole book.

GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v3.6.1
