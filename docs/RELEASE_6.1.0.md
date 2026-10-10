# Tribal Power 6.1.0 - Cut and Bound

The March's stones get the rest of their building sets, every block has a way to be made, Spiritweave waits on a totem, and the whole mod does less work per tick.

- **Stairs, slabs and walls for the wider March.** Pale Stone, Frost Shale, Ochre Sandstone, Salt Crust and all four Kiln Clays now have stairs, slabs and walls, crafted the usual way or cut on the Stonecutter. Twenty-four new blocks, each next to its stone in the creative tab.
- **Every building block can be made.** Moonstone Bricks, Polished Moonstone, Moss Agate Bricks and Polished Moss Agate had no recipe, so neither did anything cut from them. They now build like March stone: four stone in a square give four bricks, four bricks give four polished, and the Stonecutter turns the plain stone into anything in its set. Two Salt Crust stacked give two Salt Pillars.
- **The March's sands are sand.** Pale Sand, Ochre Sand and Glass Sand smelt to glass and make TNT. Pale and Glass Sand make sandstone and concrete powder; Ochre Sand makes Ochre Sandstone, the way red sand makes red sandstone. All three count as sand to other mods.
- **Spiritgear shears are shears.** Ribbon Weed, Veil Lichen, Echo Roots, Willow Strands and the March's leaves now drop to Spiritgear shears as they do to iron ones, and so does anything else that asks for shears. The shears also take Efficiency, Unbreaking and Mending from books, and a dispenser shears with them.
- **Spiritweave waits on a totem.** An unlinked piece is now plain diamond-tough armour: no boon, and no Pulse spent. Bind it to a Resonance Totem and that voice's boon wakes, and the piece shines with it. An unlinked piece's tooltip lists what each voice would give it, and the Air robe, Earth boots and Spirit boots now say what they do.
- **Fixes.**
  - The Healer's Rattle no longer charges mining Pulse for a block broken with it in hand, and a Fire rattle no longer smelts the drops.
  - Fire smelting is for the pickaxe, shovel, axe and hoe, as their tooltips say, and no longer for the blade, its weapon family or the shears.
  - Shift-clicking into the Hearth Pot sends each vessel where the recipes want it.
  - Any brush now brushes a March creature.
  - A Mossback's saddlebag and an Echo Weaver's pouch no longer spill when another mod saves the creature from death.
  - A guardian or The Unsung that resets no longer calls fresh followers in the same moment.
  - A Still Night ward can no longer trip up a world save.
  - The Ward Drum's damage is now in the config.
- **Faster.**
  - Floating islands and Weeping Colossi write only their own chunk's share as the world generates, instead of walking the whole formation for every chunk it crosses. The world they make is the same.
  - The March's ranged spirits path-find a few times a second instead of every tick.
  - Machines and hoppers remember their redstone between neighbour changes.
  - Pulse adapters, converters and cisterns stop waking their neighbours on every transfer.
  - Each dimension keeps its own lattice change queue.
  - The sky, ley threads, creature models, tooltips and the Songkeeper screen draw with less work per frame.

Minecraft 1.21.1, NeoForge 21.1.249.

GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v6.1.0
