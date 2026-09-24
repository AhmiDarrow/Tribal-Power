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

## For pack makers

- **Guardians can be switched off** (`guardiansEnabled`), and every event family, request reward, guardian number and rite cost has a config line. The README's new *Pack-maker configuration* section lists every section and what a datapack can override: Elder dialogue, hearth recipes, structure spacing, spawn weights, paintings and crests.
