# Tribal Power 4.4.0 - The world tells its own story

The March writes its history down, hands you a path through it, and opens up to the tools a pack expects.

## The Chronicle

- **Sixteen fragments** of the Loom's history are carved into the March: eight **Carved Stones** set into the walls of its ruins, from the Loom Ruin and the Stilt Village to the Ossuary and the Glimmer Vault, and eight two-by-two **Murals** standing at the guardians' grounds, each painting the country it stands in. Right-click one and you read it; the Codex keeps it, and its new **Chronicle** section assembles the fragments in order. Read all sixteen and the story is whole.
- Both can be picked up and set on any wall of your own. A mural is placed as one piece and comes down as one.
- **Six paintings** of the March hang among the ordinary ones: the aurora, the Drum Circle, nine hearths, the Loom, the edge and the Singing Fracture.
- **Nine crests.** Each tribe hands its crest to its Kin as a banner pattern: the Pad-keepers' hearth, the Grit-singers' mesh, the Rootbinders' root and the rest, for a loom and any dye.

## The guided path

- The Spirit Codex's **Where to go next** page now opens with a **Next step** panel: one concrete thing to do, read from where you are on the path from the first hearth to the Ninth Agreement, and it moves as you go.
- Two advancements join the path: **A story told** for the first tribe's story finished, and **Nine relics** for all of them; the Ninth Agreement follows from there.

## Recipe viewers and tooltips

- **JEI** shows three more families: the world rites (tablet, seal and Pulse), the guardian calls (what to lay on which altar, and what rises) and anointing (which reagents give which power). Relics, crests and carvings explain themselves.
- **EMI** is supported with every family the JEI plugin shows, and the Tribal Bench takes a recipe with a click there too.
- **Jade** tooltips show a machine's Pulse and a generator's rate and voice, how far a March crop has grown, a Kin's tribe and role with your standing, and whether a Guardian Altar will answer and what it wants.
- The mod's tools and weapons carry the common `c:tools` tags, so other mods recognise them.

## Small things

- Every guardian's boss bar takes its tribe's voice colour, the Drummers, Hunters and Weavers of every camp have a word for you, and the Hearth Pot has a handle and a ladle.

## Fixes from the sweep

- Elders now rotate through every request in their pool over the week (half of them never came up), and a player may finish three requests a day across the tribes (`requestsPerDay`); the Elder says so when the day's work is done. A wall raised for a tribe has to be built: a chalk mark set into a hillside no longer counts.
- A story whose next step is a rank you already hold moves straight on instead of waiting for standing to change.
- Guardian Altars no longer take your reagents or start their rest when a ward refuses the guardian; the Silent Drum likewise. A guardian that wanders out of loaded chunks still counts as awake; one that goes back to sleep unbeaten frees its altar at once. Bolts stop at walls. Spectators no longer finish trials. The Storm Roc fights from the air instead of trying to bite.
- March weather no longer spawns spirits under torchlight; its fog never sees further than what was already set; its ambience plays one sound at a time and fades between weathers. Weather chance is per game hour, as the config says. Festival days spread evenly over any cycle length; a feast pays its festival thanks once per festival.
- Murals drop once, keep their chapter when picked up, and come down whole when their wall or any part goes; carved stones fall with their wall. Reading a carving opens its page only once the read has counted.
- The Codex "Next step" no longer pins on an optional step already skipped, and the March page keeps counting down a surge while you read.
- The Song Bench keeps a seated stick of chalk (it used to throw it back out at once, so nothing could be written). A song's riders no longer land on the singer when there is no target: a ward with an ember note does not set you alight, and a call's riders go to everyone it struck. The hush is told before a song is paid for, and a bolt or bind that finds nothing names the song's own reach. A blade already carrying an anointment refuses the same one from any reagent of that note.
- The Hearth Pot counts a stack in one seat: two emberroot together answer for "emberroot, emberroot". Bread, jerky, tart and honeycake no longer hand back a bowl they never came in. The kettle's heat setting is its own; the pot and the sweat stones read the fire under them.
- A brazier struck by redstone fires its rite (it used to call itself paused by the very signal that struck it). Tier-two Ley Binding looks past the circle's own totems. The Ninth Agreement draws its Pulse before it takes. The Listening Pit honours camp standing and the Ninth Agreement's floor. A festival feast eaten on an odd day is remembered apart from the Elder's gift.
- The Song Bench anoints Spiritgear only: vanilla swords, axes, tridents and maces (and other mods' weapons) no longer take its work unless a pack opens the `tribalpower:anointable` tag.
- **Every automated hand answers to a voice.** A kept Resonance Totem of its own voice within eight blocks, or the device stands idle and its status names the totem it wants: Earth for the Grove Tender and Wayanchor, Spirit for the Ward Drum, Hush Totem and Summoning Cradle, Water for the Tide Pump, Air for the Wind Snare and the relays, Loom for the Seal Loom and Astral relays. Packs can waive it with `automationNeedsVoices`.
- **The Grove Tender waters.** With a Water Resonance Totem kept within eight blocks, the tender keeps every furrow of its bed wet, soaking the dry ones in one beat for `groveWaterCost` Pulse (4).
- **AgriCraft, when it is in the pack.** The Grove Tender works AgriCraft's crops as its own: it sets crop sticks from its store onto the bed, plants AgriCraft seeds into empty sticks (or straight into the soil where AgriCraft allows it), rakes weeds, urges growth, and harvests a ripe plant the way a click does, products to its store and the plant left on its sticks, cut back. The five March crops are AgriCraft plants of their own, and a plain crop AgriCraft knows waits for empty sticks before it goes into bare soil: the plain crop seeds crop sticks, the analyzer reads it, the harvest comes back as itself, and each can be bred from two farm crops (Emberroot from potato and nether wart, Fen Rice from wheat and sugar cane, Frostberry from sweet berries and beetroot, Glimmer Bean from beetroot and sea pickle, Steppe Grain from wheat and carrot). March Soil, March Grass and March Moss count as soils under the sticks. Nothing changes without AgriCraft.
- Every Spiritgear weapon now swings the way its shape asks, in hand and on the body: the spear and trident thrust, the dagger stabs, the battle axe and warhammer come down over the head, the halberd and scythe sweep level, and the greatsword cuts a wide diagonal.
- The March crops draw their leaves again (their stage models drew every clear pixel black). The Hearth Pot is reshaped and repainted: a thrown clay pot in bands on copper feet, a riveted band, lugs, a bail and a ladle leaning out, its interior open. The wandering spirit had no texture of its own and drew the missing-texture checker; it now wears a pale, bone-faded wisp. The Pulse Bow is carried, drawn and fired the way vanilla's is, and the Pulse Crossbow like vanilla's crossbow: cranked low across the body while it loads, the string drawing back through three stages, then held level and sideways with its bolt of light seated. A Spirit Flask used from creative fills without emptying the tank and pours without running dry. A Ley Lens sneak-used on your own familiar reads its threads instead of telling it to wait. An Elder whose customer shut the counter talks again. The Pulse Crossbow's load ends on its own a beat after it is complete, as the game's crossbow does.
- The Earth voice's absorption on a remedy now holds (bare absorption points fell away at once). Sneaking with an empty hand switches worn ley goggles off again, as the tooltip says. Guardian altars and the Silent Drum want their candles lit. An unbound Binding Effigy reads as unbound. The eight Spiritgear weapons unweave like the rest of the set, and the Weeping Colossus counts as a boss to a Manifested blade. The Spirit Blessing has its name. Paused workshops go dark. Elder conversations are forgotten when their player leaves.

## For pack makers

- **Guardians can be switched off** (`guardiansEnabled`), and every event family, request reward, guardian number and rite cost has a config line. The README's new *Pack-maker configuration* section lists every section and what a datapack can override: Elder dialogue, hearth recipes, structure spacing, spawn weights, paintings and crests.
