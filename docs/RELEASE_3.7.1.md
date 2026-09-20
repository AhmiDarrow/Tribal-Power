# Tribal Power 3.7.1 - Put it down somewhere

Three recipes were being shadowed, and the camp furniture has stopped being only furniture.

- **Three recipes could not be crafted.** Two recipes with the same shape and the same
  ingredients are not a harmless duplicate: the recipe book picks one and the other is
  simply gone. **March Planks Slab** and the **Wall Shelf** were both a row of three March
  planks, so the shelf was unreachable. The **Drift Plate** and the **Hush Plate** were the
  same plate frame around an amethyst shard, and the **Gate Sigil** and the **Loom Seal**
  were the same three things in a shapeless grid. The shelf is now a board on a bracket
  (three planks over a stick), the Hush Plate takes **wool** — the one material that reads
  as quiet — and the Loom Seal is a blank seal and a **Loom Thread**, which is the pattern
  the other voice seals already follow: one seal, one marker. The Drift Plate and the Gate
  Sigil are unchanged. `tools/verify_recipes.py` now fails the build if two crafting
  recipes ever claim the same grid again.
- **The Wall Shelf holds things.** Four of them, one to a place along the board. Use it
  with something in hand to set that down at the end you clicked, empty-handed to take it
  back. What is on it is drawn where you put it, falls with the shelf when it breaks, and a
  comparator reads how full it is.
- **So do the March Table and the Spirit Urn.** Four on the table's top, one in the urn's
  mouth, the same way. No face of any of them is open to a hopper or a pipe — this is
  furniture, not storage, and the caches are still where things go.
- **The March Stool is for sitting on.** Use it empty-handed to sit; sneak or stand to get
  up. Only one person to a stool, and the seat disappears with the stool or the sitter, so
  nothing is left behind in the world.
- **The Wind Charm rings.** Knock it and it chimes, and it catches the air by itself now
  and then — quietly enough that a camp full of them is still somewhere you want to stand.

The Woven Mat is still just a rug.

Minecraft 1.21.1, NeoForge 21.1.249.

GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v3.7.1
