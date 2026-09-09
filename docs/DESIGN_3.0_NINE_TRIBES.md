# Tribal Power 3.0 — The Nine Tribes

Design specification for the 3.0 expansion. Standalone mod (Minecraft 1.21.1, NeoForge 21.1.x). Nothing here depends on Ninjacat Skies; the pack may build quests on top later.

## Lore foundation (public-safe canon)

The skies were once held by the **Loom of Worlds**, a lattice of living thread. Nine tribes kept it, one Strand each. From their shared pull on the Loom came Tribal Power: pulse, lattice, seal, and the road into the March. Something severed the Loom — **the Cut**. Continents fell, the tribes scattered, and their orphan engines kept humming. The March is where the thread frayed thinnest; the tribes' last camps and halls are there, half-sunk, still keeping time.

Writing rules: concrete verbs (hum, bind, strike, offer, route, reweave); no chosen-one prophecy; tribes are forgotten craft lines, not gods. Codex voice, short pages.

### The Nine Tribes

| id | Tribe | Strand | Voice (Attunement) | Favoured offerings | Craft |
|---|---|---|---|---|---|
| soil | Pad-keepers | Soil | Earth | dirt/moss blocks, bread, Echo Shards | hearths, caches |
| stone | Grit-singers | Stone | Earth | raw ores, grits, Attuned Echo | echo shatter, meshes |
| sprout | Rootbinders | Sprout | Water | saplings, seeds, Mossback Moss | living anchors, March flora |
| claw | Edge-walkers | Claw | Fire | leather, iron, Rift Fang | Spiritgear, footholds |
| spark | Drumhearts | Spark | Fire | copper, Pulse Cells (charged), Bone Chimes | drums, Pulse |
| clock | Pattern-weavers | Clock | Air | redstone, clocks, Storm Moth Dust | timed songs, automation |
| swarm | Colony-keepers | Swarm | Air | honey, flowers, Lantern Fox Ember | hives, March flowers |
| sigil | Seal-carvers | Sigil | Spirit | blank/element seals, Spirit Shards | seals and rites |
| spindle | Loom-stitchers | Spindle | Loom | March Crystal, Loom Thread, compasses | gate-paths, reweave |

Each tribe has a margin line (used by Codex and Elder greeting):
- Pad-keepers: "We never called it dirt. We called it what was left, and we kept it warm."
- Grit-singers: "The mesh does not find the ore. The mesh gives the ore somewhere to land."
- Rootbinders: "Roots are the only rope the void respects."
- Edge-walkers: "Boots first. Then the bridge. Then the courage; it arrives on its own."
- Drumhearts: "The drum is not loud. The drum is steady. Be the drum."
- Pattern-weavers: "A factory is a song that has stopped needing the singer."
- Colony-keepers: "You do not own a hive. You are on good terms with it."
- Seal-carvers: "Spirit goes where it is asked politely and stays where it is fed."
- Loom-stitchers: "The Loom was never one thread. It was nine agreeing."

Tribe colours (used for masks, cloaks, banners, hearth trim): soil `8b5a2b`, stone `7d8791`, sprout `4f9a5a`, claw `b8512f`, spark `e0a32d`, clock `4a7fb5`, swarm `d7b23c`, sigil `8a5fc7`, spindle `62d1c9`.

## 1. The sixth voice — Loom

`Attunement.LOOM` joins Earth/Fire/Water/Air/Spirit. It is the voice of the thread itself: rare, March-born, boss-gated at the top.

- **Loom Thread** (item): found in Ancestor Hall loot (2–4 per hall), dropped by The Unsung (16–24), and 1 per Loom-stitcher trade at Friend rank.
- **Unsung Heart** (item): boss drop, 1 per kill. Crafts the Loom totem.
- **Resonance Totem (Loom)** — `resonance_totem_loom`: 1 Unsung Heart + 2 Loom Thread + 4 March Crystal + 2 March Planks. Counts as a distinct voice for the Pulse Resonator.
- **Loom Seal**: Blank Seal + Loom Thread + Spirit Shard. Brazier blessing "Tension": every 2 s, refill 2 Pulse into carried cells of players in range (net positive only if the brazier is fed by a stronger source). Also Luck I.
- **Echo Unweave** station (`echo_unweave`, 5th Echo station, Loom attunement): reverses the lattice — Manifested Ingot → 2 Bound Echo, Bound Echo → 2 Attuned Echo, Attuned Echo → 2 Echo Shards, Spiritweave → 2 Wool, Resonant Core → 3 Manifested Ingots; plus tool salvage: any damaged Spiritgear tool → 1 Manifested Ingot. Datapack type `tribalpower:lattice` with `"station": "echo_unweave"`.
- **Fivefold Staff → Sixfold Staff** (id stays `spirit_staff`, lang becomes "Sixfold Staff"): Loom mode = *Tether*: pulls the targeted entity 8 blocks toward the caster (cost 6 Pulse); sneak-cast with no target = *Stitch*: blink 6 blocks forward through air (cost 10).
- Rite Tablet "Ley Binding" uses Loom (see §4).
- Colour: `62d1c9` teal-white; particle `0.38, 0.82, 0.79`.
- Every enumerating site listed in the architecture notes (`SpiritStaffItem` `% 5`, totem BE builder, brazier seal chain, RiteHelper, JEI/lang/textures/Codex "Five voices" → "Six voices") is updated.

## 2. Tribes

### Tribal Kin (entity `tribal_kin`)
One entity type, two data fields: `tribe` (0–8) and `role` (ELDER, DRUMMER, HUNTER, WEAVER). Humanoid, 0.6×1.9, masked and cloaked; base skin per role + a cloak/mask overlay tinted with the tribe colour. Neutral; `MobCategory.MISC` (never despawns; persistence required). Goals: wander near camp anchor (≤ 16 blocks of spawn pos), look at players, Hunter attacks hostiles within 12 blocks (melee 4), Drummer periodically "drums" (plays basedrum note + particle ring every ~6 s, and when a Drumheart is within 8 blocks inserts 2 Pulse per beat — camps are a small Pulse source), Weaver idles at a loom, Elder trades.

### Camps (structures)
Nine `tribe_camp_<id>` jigsaw structures (single-piece pools, entities embedded in NBT): 3–4 huts (March Planks / spruce / wool of tribe colour), a fire pit, a **Tribe Hearth** block, a Resonance Totem of the tribe's voice, a **Tribe Banner** decoration, and 4 Kin (1 Elder, 1 Drummer, 1 Hunter, 1 Weaver). Overworld placement by biome tag: soil → plains; stone → mountains/stony; sprout → forests; claw → taiga; spark → savanna; clock → birch; swarm → flower/meadow; sigil → dark forest; spindle → **March only** (crystal fields). Every tribe also has a March variant in one March biome. Spacing 40 / separation 24, salt distinct per tribe.

### Tribe Hearth (block `tribe_hearth`)
Block entity; stores its tribe id and accepts offerings: right-click with a favoured item → consumes 1 and grants standing (+3 favoured, +8 for the tribe's reagent tier, +1 any food); right-click with a charged Pulse Cell → drains up to 40 Pulse for +2 per 10 Pulse. Emits smoke/ember particles in tribe colour. Comparator = standing rank of the last player who touched it (0–15 scaled).

### Standing
`TribeStandingSavedData` (overworld SavedData, keyed player UUID → int[9]), same malformed-record-preserving idiom as DeepCacheSavedData. Ranks: Stranger 0, Guest 50, Friend 150, Kin 400, Voice 800. Also earned by: killing a hostile within 24 blocks of a hearth (+1, cap 20/day per tribe), completing a trade (+2). Lost by: hurting Kin (−25, and Hunters turn hostile to you for 60 s), breaking camp blocks (−5 per block, hearth −40). A chat toast on rank-up. Command `/tribalpower standing [player]` prints all nine.

### Trades
Elder `implements Merchant`; `MerchantOffers` built from rank on open. Currency is the tribe's favoured items and Echo tiers. Per tribe, 6 offers (2 per rank Guest/Friend/Kin) drawn from a table in `TribeDefinition`, e.g. Grit-singers sell Echo Shards for raw ore, Attuned Echo for grit; Seal-carvers sell the tribe's Seal and Blank Seals; Loom-stitchers sell Loom Thread (Friend) and a Horizon Compass (Kin); Drumhearts sell Bone Chime / Pulse Cell; Rootbinders sell March saplings and Spirit Reed. Voice rank: Elder gives a **Tribe Mark** (once, tracked in SavedData) and unlocks the Codex tribe page.

### Kinship Totem (block `kinship_totem`)
Crafted from a Tribe Mark + Resonance Totem of that tribe's voice + 2 Spiritweave. Stores the tribe id. In `LatticeNetwork` a Kinship Totem counts as an **extra distinct voice** for the Pulse Resonator (tribe voices are tracked separately from Attunement, so nine tribes = up to 15 voices) and provides its tribe's Attunement for stations. Renders with the tribe banner colour and the tribe's glyph.

## 3. The March — structures and The Unsung

### Structures (all jigsaw, NBT generated by `tools/generate_structures.py` with nbtlib)
- **Ancestor Hall** (`ancestor_hall`, March steppe/highlands): sunken stone hall, 3 rooms, Loom-wood beams, four Lore Tablets on walls, 2 chests with `chests/ancestor_hall` loot (Loom Thread 2–4, Bound/Attuned Echo, Spiritweave, seals, one random Tribe Mark-fragment? no — Tribe Marks are only earned), Hollow Sentinels spawn inside (spawner-less: entities in NBT).
- **Drum Circle** (`drum_circle`, March highlands): ring of twelve pillars around a **Silent Drum** block, seating stones, torches of Spirit Lanterns. Boss arena.
- **Crystal Spire** (`crystal_spire`, March crystal fields): tall March-crystal spire with a Loom-stitcher waystation at its base (1 Kin Elder spindle, a hearth). Doubles as the spindle tribe camp.

### Silent Drum (block `silent_drum`)
Block entity. Striking it (right-click empty-handed or with a Bone Chime) records beat timestamps. Four beats spaced 16–28 ticks apart wake **The Unsung** (once per 20 minutes real time per drum; a second wake while alive is ignored). Emits a deep note per strike; wrong rhythm resets with a dull thud. Also used mid-fight (see below).

### The Unsung (entity `the_unsung`)
Ancestor spirit shaped like a hollow standing drum with two long arms and a masked face; 3.2 wide × 4.2 tall, 400 HP, armor 8, knockback resistant, immune to fire and fall, `MobCategory.MONSTER`, boss bar (`ServerBossEvent`, purple, NOTCHED_6), despawns and resets when no player within 48 blocks for 30 s.

Phases by health:
1. **Beat** (100–66%): every 3 s a **drumbeat shockwave** — ring particles, 6 damage + knockback to all players within 8 blocks who are not sneaking (sneaking = bracing: half damage, no knockback). Melee arm swipe 10.
2. **Chorus** (66–33%): every 8 s summons 2 Echo Weavers (max 6 alive); shockwave interval 2.5 s.
3. **Silence** (33–0%): becomes invulnerable and floats to the drum centre; players must strike the Silent Drum with the four-beat rhythm to "resync" it — on success it is stunned 8 s and takes double damage; then silence resumes after 12 s. Shockwaves stop during silence; instead it fires slow Echo bolts (`weave` effect).

Drops: 1 Unsung Heart, 16–24 Loom Thread, 4 Resonant Cores, experience 200. Advancement "The Drum Remembers".

### Lore Tablet (block `lore_tablet`)
Decorative wall block with a `tablet` id (0–11); right-click opens a small screen with that tablet's text (a tribe fragment). All twelve are also Codex entries once read (tracked in player persistent data).

## 4. World rites

**Rite Tablets** (items, consumed): Rain Calling (Water), Sky Clearing (Air), Dawn Calling (Fire), Green Blessing (Earth), Ley Binding (Loom), Still Night (Spirit). Craft: 2 stone + tribe reagent + the matching seal (seal is returned).

Use: sneak-right-click a **Ritual Brazier** that has the matching Seal seated. The rite draws Pulse from the lattice around the brazier (`LatticeNetwork.extractPulseNearby`, radius 8) — if insufficient, message and nothing consumed.
- Rain Calling — 400 Pulse; sets rain for 20 min.
- Sky Clearing — 400; clears weather for 40 min.
- Dawn Calling — 800; advances time to next sunrise (server-wide, needs OP-free rule `tribalpower:allowDawnRite` game rule, default true).
- Green Blessing — 600; blesses the 3×3 chunk area around the brazier for 20 min: crops/saplings/stems there get 3 extra random ticks per second, chunk marked in `RiteSavedData`, green sparkle particles.
- Still Night — 600; hostile spawns suppressed in a 64-block radius for 15 min (reuses Hush ward logic with a temporary ward).
- Ley Binding — 1,200; creates a temporary (30 min) **ley line** between the brazier's nearest Resonance Totem and the nearest other totem within 64 blocks; while active, `LatticeNetwork` treats the two totems as adjacent for Pulse routing and a thread of particles runs between them.

Rites are gated by a Kin-rank Seal-carver trade for the first tablet recipe? No — recipes are standalone; Seal-carvers simply sell tablets at Friend rank as a shortcut.

## 5. Familiars

Adult **Lantern Fox, Mossback, Dawn Stag** can be bonded with a **Bonding Charm** (item: 2 Spiritweave + the species' reagent + Spirit Shard; not consumed on failure). Right-click an adult → 60% chance per attempt (heart particles on success); bonded animals store `Owner`, `Sitting`. Sneak-right-click toggles stay/follow. Bonded animals follow within 3–10 blocks, teleport to the owner past 12 (like wolves), never despawn, are immune to their owner's damage, and the brushing yield doubles.
- **Lantern Fox**: owner within 8 blocks gets Night Vision (refresh); every 4 s marks ores within 6 blocks of the fox with glow particles (client-visible), and the fox emits light (dynamic light via a `spirit_light` invisible light block placed/removed at its position — light level 10, air-replacing, self-cleaning).
- **Mossback**: 9-slot saddlebag inventory (sneak-right-click with empty hand opens `MossbackMenu`); drops contents on death.
- **Dawn Stag**: rideable without saddle (right-click while bonded); rider steers, speed 0.32, jump on space (1.2 block leap), cannot be used by non-owners.
Bonded overlay: a small tinted spirit-cord collar layer. Codex entries updated.

## 6. Shared camps (camp identity)

`CampSavedData` (overworld SavedData): camps `{UUID id, String name, UUID leader, Set<UUID> members, int colour}`; invitations expire 5 min.
Commands `/tribalpower camp create <name>`, `invite <player>`, `join <name>`, `leave`, `kick <player>`, `info`, `rename`. Also a **Camp Charter** item: right-click a player to invite; crafted with a Tribal Seal? (No tribe needed: paper + Spiritweave + Spirit Shard.)
Effects of membership:
- Deep Cache / Wayfarer Satchel open the **camp vault** (54 slots keyed by camp id) when the player is in a camp; players keep a personal vault when not; a sneak-use switch toggles personal/camp.
- Camp devices (Cradle, Grove Tender, Wayanchor, etc.) with an owner in the camp treat members as owners for access and protection checks.
- Wayanchor budget: camp shares one budget of 12 anchors across all members (instead of 32 per dimension global); solo players keep the old rule.
- Hearth standing: tribe standing gains of members are mirrored to the camp at 25% (camp standing lets any member trade at the camp's rank if higher than their own).

## 7. Quality of life

- **Ley Lens** (item): while held, HUD shows "Ley: NN%" for the player's position using `LeyCollectorBlockEntity`'s factor maths refactored into a static `LeyMath.strength(level, pos)`; every 10 ticks spawns coloured particles at ground level in a 9×9 grid coloured by strength (blue weak → gold strong). Sneak-use on a Ley Collector prints its exact factor breakdown.
- **Pulse Gauge** (block): comparator-style; outputs redstone 0–15 proportional to stored/capacity of the PulseHandler it faces (directional block, arrow texture).
- **Pulse Threshold** (block): emits redstone when the faced handler is ≥ threshold; right-click cycles 25/50/75/100%; comparator reads threshold.
- **Codex diagnostics**: sneak-right-click the Spirit Codex on any Tribal block entity → chat report: stored Pulse, nearest generators and their output this second, attunements present within 8 blocks, missing attunement for current recipe, output full, redstone paused, chunk not loaded for relay endpoint. Implemented via a `Diagnosable` interface with a default fallback.

## 8. Art direction

- Blocks/items: 32×32, the Living Lattice palette (loom wood `5a3b2e`/`7a5138`, stone `3a4a55`/`556672`, copper trim `c08a4e`, light `7effcb`), top-left light, 1 px dark outline, 2–3 value steps per material, no noise textures. New devices reuse the `loom_*` shared textures for frames and add a unique inlaid glyph panel so every block is recognisable from its face.
- Tribe glyphs: nine 9×9 glyphs (hearth, mesh, root, boot, drum, cog, comb, seal, spindle) reused on banners, hearths, marks, Kinship Totems and the Codex.
- Creatures: painted per-part textures (fur/scale direction, belly lighter, dorsal darker, eye highlights, two-pixel rim light), glow maps for runes/eyes. The Kin get four role skins with a tinted cloak overlay. The Unsung is a large hollow-drum body with lacquered ring bands, taut hide top, rune arms, a cracked mask; glow map on rune lines and eyes.
- Blender: all new rigs are authored in `art/creatures/roster.json` (existing pipeline) and exported through `tools/blender_bestiary.py`; the Blender file and contact sheet are regenerated. Idle/walk/attack animation clips for Kin and the Unsung (arms drum, body pulse).
- Structures: generated NBT from a Python builder with palette randomisation so no two camps are identical.
- Codex: new 128×128 portraits for Kin roles, The Unsung, familiars, plus nine tribe crest pages.

## 9. Compatibility and data

- All new content standalone; existing IDs, slots and saves stable.
- New GameTests: Loom attunement counted; Unweave recipes reverse Manifest; standing rank thresholds; Unsung wakes on rhythm and drops Heart; familiar bond persists through save; camp vault shared between two members; Pulse Gauge output; rite consumes Pulse and sets weather.
- `BestiaryGameTests` roster assertion updated (3 animals + 10 hostiles + Kin + Unsung).
- Version 3.0.0. README section "The Nine Tribes" and RELEASE_3.0.0.md.
