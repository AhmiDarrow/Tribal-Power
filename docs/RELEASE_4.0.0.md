# Tribal Power 4.0.0 - The camp answers

The March keeps a bestiary, the camp learns to sing, Pulse is measured against the machines it has to feed, and the hearth workshops and totems are carved in wood, stone and copper.

## The bestiary

- **Fifty-four new creatures.** Thirty-two beasts from fourteen body plans, the folk each biome's stories would put in it — an ent for every tree the March grows, trolls, goblins, kobolds, fairies and the dead — and the **Weeping Colossus**, a walking boss that keeps an escort and is worth killing. Each one has a reagent, loot and a habitat. Nothing that already lived in the March was removed.
- **Twelve creatures had been dropping nothing.** Their loot tables named `minecraft:looting_enchant`, which is not a loot function in 1.21.1, so the whole table failed to parse. They drop again, and a test fails the build if a loot table names a function the game does not have.

## Songs

- **The Song Bench empowers reagents, writes sheets, and fletches verse arrows.** It does not refine Echo; that work stays on the Echo stations. Empowering costs 16 Pulse. A sheet is 3 to 7 empowered reagents, paper and one Ritual Chalk mark, at 12 Pulse a reagent. Fletching spends 8 Pulse and makes 4 verse arrows. A nearby Resonance Totem supplies the voice. The first reagent picks the shape of the song; repeating it makes it stronger.
- **Songbooks.** The first holds one page of up to 3 reagents, a bound book holds 3 pages of up to 5, and a chorus book holds 5 pages of up to 7. Right-click sings the open page. Crouch and right-click turns the page.
- **A Reagent Pouch** on the hotbar is collect-only: one slot a reagent, raw and empowered counted apart, and it absorbs reagent pickups.
- **The Pulse Bow** throws a sonic bolt. A full draw spends 6 Pulse. A verse arrow in the inventory rides that bolt, costs 6 Pulse more, and is spent. With no arrow, the bolt is only the note.

## Pulse, measured against the pack

Pulse had been balanced against itself. A finished Resonator made 20 a second while a ranked Ember Kiln drew more than that, and the bridge into Forge Energy could not turn one Mekanism machine over. The rates below are checked against Powah, Mekanism, Solar Flux and Create Crafts & Additions, and a test holds them in that band.

- **The Resonator multiplies.** Voices multiply, and the catalyst multiplies on top. Six voices in a ring under a Resonant Core make **144 Pulse a second** rather than 20, and every catalyst tier is a real step up. Echo Shard, Attuned Echo, Bound Echo, Resonant Core.
- **The catalyst is spent.** It used to sit forever once seated. It now wears by the Pulse it has actually made, and crumbles when that life is gone. A Resonant Core under a full ring lasts about thirty-five minutes. Sitting idle does not wear it.
- **Drums crowd each other out.** Two Drumhearts in one zone earn. Any further drum still beats and still sounds, and pays nothing. Two drums and a repeater loop used to out-earn every other generator in the mod.
- **The Ley Collector yields three times what it did.** The scale the Ley Lens reads a site's strength from is unchanged: the land's share of a beat is still capped at 6, and the beat is still capped at 16 before that yield. A perfect site pays 48 Pulse a beat.
- **The Pulse Adapter is the Harmonic Energizer.** The block id is unchanged, so old worlds keep theirs. The ratio is still 1 Pulse = 100 FE. What changed is throughput: unranked it draws 60 Pulse a second and puts out **300 FE a tick** rather than 100. A rank 3 energizer reaches about 545 FE a tick. It stores 48,000 FE and still cannot take FE back in.
- **The Lattice Converter** turns FE back into Pulse, up to 40 a second. It starts at 220 FE a Pulse. Every distinct Resonance Totem voice within 8 blocks knocks 20 off, down to a floor of 120. That floor sits above the Energizer's payout, so Pulse out and FE back in is always a loss.
- **A Tribal Bench** is a crafting table that keeps its grid, two blocks wide, with a shelf along the back that holds items the way a Wall Shelf does. Eighteen woods: the eleven vanilla ones and the seven the March grows. Each palette is read out of that wood's own planks.

## Ley

- **The lines are one colour each.** Every vein is a single thread in a Resonance Totem's voice: earth green, fire ember, water teal, air pale, spirit violet, loom gold. They are dense enough to cross the world, in every dimension, folded from the seed. There is nothing to place and nothing saved.
- **A totem pulls its own colour.** A Resonance Totem bends a matching thread through itself. A ring of the six around a Ley Collector steers lines across it. The collector still counts at most six lines, and the beat is still capped at 16 before the yield.
- **The Ley Lens has five sights.** Use cycles ley, the Pulse zone, voices, machines, and off. Ley sight draws the threads. Pulse sight lists what each machine near you holds and whether the zone's incoming Pulse covers what it is spending. Off hides the ropes and the panel. Craft the lens into a Spiritweave Hood for goggles; sneak-use the hood, or sneak and right-click with an empty hand while wearing it, to close them. A held lens that is not off overrides the hood.
- **Lattice Conductors extend the draw.** Place them within 8 of each other and a machine within 8 of any of them can draw the generators, Pulse Cairns and totem buffers that line can reach. A station's own buffer stays off-limits. A redstone signal cuts that conductor out of the line. One craft yields four.

## The hearth

- **The workshops have a face.** The Ember Kiln, Echo Shatter, Echo Attune, Echo Bind and Echo Manifest are squat hearth blocks: charred oak, deepslate courses, worn copper, and a mouth, vise, dish, lashing or ring that faces you when you place them. Echo Unweave turns with them and keeps its own look. A station that was already in a world, from before it had a facing, comes back facing north.
- **The totems are carved poles.** Each of the six voices wears the face of that voice, on hearth stone and hearth wood, two blocks tall. A copper band sits on the shoulder where the wide head meets the shaft, and another on the top rim, with a thin copper lip across the crown. The same copper the workshops use.

## Rites, the drum, and what a full hand used to eat

- **Builder's Chalk ghosts the camp rites.** The stone font, the listening pit, the rite circle, the voice ring, the shatter array and both gates can be stood up as a hologram, the same way the older shapes are. Sneak steps the rite's tier. The chalk is still never spent, and the ghosts stay while it sits anywhere in the hotbar.
- **The Unsung rises only from its altar.** The Silent Drum keeps its rhythm anywhere: four beats, the thud, the cooldown it already had. It calls The Unsung only when that drum stands on polished deepslate with a candle at each of the four corners, and only for the fight bound to that drum. A drum carried off the March altar and set down in the Overworld does not wake it. An Unsung that is already alive can still be resynced by the four-beat of its own drum.
- **A full pack no longer eats the rest of a stack.** Handing out from a pouch, a weaver bag, or a gear refund used to treat a partial fit as success and drop the remainder on the floor of the code. What does not fit is kept, or dropped in the world, and the slot clears only once the stack is actually empty.
- **Spirit Codex search keeps the caret.** The first letter used to open the search page and steal focus, so you had to click the field again. You can keep typing.

Minecraft 1.21.1, NeoForge 21.1.249.

GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v4.0.0
