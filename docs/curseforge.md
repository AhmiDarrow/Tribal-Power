# CurseForge publishing

Canonical project ID: **1684851**.

Project: https://www.curseforge.com/minecraft/mc-mods/tribalpower

This ID was explicitly confirmed by the owner. Do not infer the project from similarly named search results.

## Branding refresh - 2026-09-07
Canonical project 1684851 now uses docs/public/tribal-power-icon-400.png (400x400). The Author Console description was refreshed for 2.2.1: renewable Pulse, batch workshops, redstone, standard capabilities, tiered cargo/player travel, equipment, thirteen creatures, auroras, and optional Ninjacat Skies integration. Project moderator approval remains pending.


## 3.0.0 — The Nine Tribes
The description gains a Nine Tribes paragraph (docs/public/store-description.md): tribe camps and Kin, standing and trades, Kinship Totems, Ancestor Halls and Lore Tablets, the Silent Drum and The Unsung, the Loom voice with Echo Unweave and the Sixfold Staff, world rites, familiars, shared camps and the ley/logic tools. The one-line summary now ends "The March and the Nine Tribes". Release notes: docs/RELEASE_3.0.0.md. Upload the 3.0.0 jar for 1.21.1 / NeoForge 21.1.249 with the same project icon.

Uploaded 2026-09-09: tribalpower-3.0.0.jar as file 8843562 ("Tribal Power 3.0.0 - The Nine Tribes", release, 1.21.1 / NeoForge / Client+Server, changelog = docs/RELEASE_3.0.0.md). Awaiting CurseForge approval.

## 3.1.0 — The Listening Pit and the Gates
The live project page (2.3.0, file 8828297) is the style to copy: display name `Tribal Power {version} - {subtitle}`, markdown changelog as a heading plus short player-facing bullets, tags 1.21.1 / NeoForge / Client+Server, release. The long project description is the Living Lattice prose, not the local store-description bullets.

Uploaded 2026-09-10: `tribalpower-3.1.0.jar` as file **8855150** ("Tribal Power 3.1.0 - The Listening Pit and the Gates", release, 1.21.1 / NeoForge / Client+Server). Changelog matches 8828297's bullet form (not the long `docs/RELEASE_3.1.0.md`). Awaiting CurseForge approval; the public files list still shows 2.3.0 until then.

File 8855150 then went to **Under Manual Review** with "failed processing… obfuscated code or corrupt files". Ninjacat Skies Core (five nested jars under `META-INF/jarjar/`) is a thin wrapper and is not a useful comparison: Tribal Power is one content jar. The 2.3.0 jar that auto-approved had no `.ogg` files; 3.1.0 adds twelve Vorbis hits plus a 1.2 MB music disc next to the existing 2.1 MB Codex `book.png`. Published packaging now excludes GameTests from the player jar, strips encoder metadata from the oggs, and keeps ASCII in `mods.toml`.

## 3.2.0 — The Listening Pit and the Gates
Uploaded 2026-09-11: `tribalpower-3.2.0.jar` as file **8855281** ("Tribal Power 3.2.0 - The Listening Pit and the Gates", release, 1.21.1 / NeoForge / Client+Server). Changelog is `docs/RELEASE_3.2.0.md`. This is the cleaner 3.1 jar under a new version so CurseForge can process a new file. GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v3.2.0

## 3.2.1 — The Listening Pit and the Gates
Uploaded 2026-09-11: `tribalpower-3.2.1.jar` as file **8858201** ("Tribal Power 3.2.1 - The Listening Pit and the Gates", release, 1.21.1 / NeoForge / Client+Server). Changelog is `docs/RELEASE_3.2.1.md`. Packaging-only: screenshot drivers, GameTest empty structure, unused written-book guide copies, and FTB Library faces omitted from the jar. GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v3.2.1

## 3.2.2 — Furnace grit and Mekanism
Uploaded 2026-09-11: `tribalpower-3.2.2.jar` as file **8861850** ("Tribal Power 3.2.2 - Furnace grit and Mekanism", release, 1.21.1 / NeoForge / Client+Server). Changelog is `docs/RELEASE_3.2.2.md`. Grit cooking subclasses vanilla `SmeltingRecipe` / `BlastingRecipe` so Mekanism 10.7.19's incomplete-recipe scan can iterate furnace recipes without a ClassCastException. GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v3.2.2

## 3.3.0 — Ember Kiln and Dock stalls
Uploaded 2026-09-12: `tribalpower-3.3.0.jar` as file **8868094** ("Tribal Power 3.3.0 - Ember Kiln and Dock stalls", release, 1.21.1 / NeoForge / Client+Server). Changelog is `docs/RELEASE_3.3.0.md`. Pulse `consumptionMultiplier` default 2.0; Echo Shatter hammer chain (sand → Ex Deorum dust, else clay); Ember Kiln; dock stalls. GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v3.3.0

## 3.3.1 — Machines drop themselves
Uploaded 2026-09-12: `tribalpower-3.3.1.jar` as file **8869162** ("Tribal Power 3.3.1 - Machines drop themselves", release, 1.21.1 / NeoForge / Client+Server). Changelog is `docs/RELEASE_3.3.1.md`. Player machines drop themselves and their inventories when broken. GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v3.3.1

## 3.3.2 — Ember Kiln smelts grit
Uploaded 2026-09-12: `tribalpower-3.3.2.jar` as file **8869249** ("Tribal Power 3.3.2 - Ember Kiln smelts grit", release, 1.21.1 / NeoForge / Client+Server). Changelog is `docs/RELEASE_3.3.2.md`. Kiln runs every furnace smelting recipe, including mineral grit. GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v3.3.2

## 3.5.0 — The March reborn
Uploaded 2026-09-19: `tribalpower-3.5.0.jar` as file **8918180** ("Tribal Power 3.5.0 - The March reborn", release, 1.21.1 / NeoForge / Client+Server) via `tools/upload_curseforge.py` (the author token is read from `tools/secrets/.env`, git-ignored, or Ninjacat Skies' copy). Changelog is `docs/RELEASE_3.5.0.md`. March performance and terrain overhaul, repainted March, March-slate vanilla ores, caves and amethyst, game meat and leather, Gate Drum return, one-time March retrogen, optional Chocobos Reborn compat (Wild Gysahl with Chocobos Reborn 1.0.4+, file 8918139). GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v3.5.0

Project description refreshed for 3.5.0: new March section (terrain, caves, March-slate ores, amethyst, game), optional Chocobos Reborn section, version line. Edit `docs/public/store-description.md`, run `python tools/render_store_html.py` (local authoring tool; `tools/` is git-ignored), and paste `docs/public/store-description.html` into the Author Console description (CurseForge has no API for the description).

## 5.0.1 — Urns in their place
Uploaded 2026-09-24: `tribalpower-5.0.1.jar` as file **8968467** ("Tribal Power 5.0.1 - Urns in their place", release, 1.21.1 / NeoForge / Client+Server) via `tools/upload_curseforge.py`. Changelog is `docs/RELEASE_5.0.1.md`. The four Soul Urn recipes said category `tools`, which the recipe book does not have; they say `equipment` now. GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v5.0.1

## 5.0.0 — The world tells its own story
Uploaded 2026-09-24: `tribalpower-5.0.0.jar` as file **8968154** ("Tribal Power 5.0.0 - The world tells its own story", release, 1.21.1 / NeoForge / Client+Server) via `tools/upload_curseforge.py`. Changelog is `docs/RELEASE_5.0.0.md` with the 4.3.0, 4.2.0 and 4.1.0 notes appended (those three were never published on their own). The spirit layer and the table, the tribes' stories, guardians and the Ninth Agreement, the living March and its music, the Chronicle, storm warnings, fluid shown in tanks and flasks, AgriCraft, weapon animations, and the bug sweeps. GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v5.0.0

## 4.0.1 — One hearth
Uploaded 2026-09-23: `tribalpower-4.0.1.jar` as file **8958670** ("Tribal Power 4.0.1 - One hearth", release, 1.21.1 / NeoForge / Client+Server) via `tools/upload_curseforge.py`. Changelog is `docs/RELEASE_4.0.1.md`. The rest of the camp uses the hearth stone, wood and copper, and idle generators and conductors no longer dirty their chunk every beat. Pulse numbers are unchanged. The first upload attempt returned HTTP 500 and did not take; the retry is this file. GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v4.0.1

## 4.0.0 — The camp answers
Uploaded 2026-09-23: `tribalpower-4.0.0.jar` as file **8958138** ("Tribal Power 4.0.0 - The camp answers", release, 1.21.1 / NeoForge / Client+Server) via `tools/upload_curseforge.py`. Changelog is `docs/RELEASE_4.0.0.md`. Bestiary of fifty-four March creatures and the Weeping Colossus, Song Bench, songbooks, Pulse Bow, Pulse rebalanced against the pack (Harmonic Energizer at 300 FE/t, Lattice Converter, spent Resonator catalysts, two earning drums a zone, Ley Collector yield tripled), colored ley web that totems can bend, hearth workshops and carved totems with copper bands, Builder's Chalk rite ghosts, and The Unsung only from its altar. GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v4.0.0

## 3.8.0 — Measure twice
Uploaded 2026-09-22: `tribalpower-3.8.0.jar` as file **8947039** ("Tribal Power 3.8.0 - Measure twice", release, 1.21.1 / NeoForge / Client+Server) via `tools/upload_curseforge.py`. Changelog is `docs/RELEASE_3.8.0.md`. Adds Builder's Chalk: a never-spent stick that stands a hologram of a shape - a ghost block in every space still wanted - set down with a click and held while the chalk is anywhere in the hotbar; nine shapes from size 2 to 128. Pulse fixes: the Lattice Conductor can draw a Pulse Cairn, so a Cairn no longer starves the lattice it shares generators with; a full lattice reports itself as full instead of borrowing the "no Pulse" line; the Conductor checks for room before drawing. Ley Lens Pulse sight reads machine by machine, and the zone outline traces the cube the scans actually use instead of a ring. GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v3.8.0

## 3.7.1 — Put it down somewhere
Uploaded 2026-09-20: `tribalpower-3.7.1.jar` as file **8934331** ("Tribal Power 3.7.1 - Put it down somewhere", release, 1.21.1 / NeoForge / Client+Server) via `tools/upload_curseforge.py`. Changelog is `docs/RELEASE_3.7.1.md`. Frees three recipes that shared a grid with another and so could not be crafted (March Planks Slab vs Wall Shelf, Drift Plate vs Hush Plate, Gate Sigil vs Loom Seal); `RecipeGameTests` now fails the build if any two crafting recipes ever claim one grid again. Camp furniture gains use: the Wall Shelf, March Table and Spirit Urn hold what you set on them, the March Stool seats you, and the Wind Charm rings. GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v3.7.1

## 3.7.0 — The carried kit
Uploaded 2026-09-20: `tribalpower-3.7.0.jar` as file **8933246** ("Tribal Power 3.7.0 - The carried kit", release, 1.21.1 / NeoForge / Client+Server) via `tools/upload_curseforge.py`. Changelog is `docs/RELEASE_3.7.0.md`. Adds Spiritgear Shears and Hoe, the Spirit Flask and Greater Spirit Flask, the Totem Wrench and the Weaver's Wand, plus a Tidy button on player, cache and vanilla container screens. Carries a broad performance pass: neighbour lookups instead of cube scans, cheaper rank and voice reads, per-tick instead of per-frame HUD and screen work. GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v3.7.0

## 3.6.2 — Plain sight
Uploaded 2026-09-19: `tribalpower-3.6.2.jar` as file **8925225** ("Tribal Power 3.6.2 - Plain sight", release, 1.21.1 / NeoForge / Client+Server) via `tools/upload_curseforge.py`. Changelog is `docs/RELEASE_3.6.2.md`. Art pass (creature drops, echo catalysts, grits, exotic wood sets, machine tops) plus bug fixes: gate bounce, Six Voices timing, bonded weavers, familiar health, unpaid charms, camp anchors, Spiritgear swing ownership, tank faces, logic plate saves, and a much cheaper per-tick Pulse draw. GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v3.6.2

## 3.6.1 — Safe keeping
Uploaded 2026-09-19: `tribalpower-3.6.1.jar` as file **8923709** ("Tribal Power 3.6.1 - Safe keeping", release, 1.21.1 / NeoForge / Client+Server) via `tools/upload_curseforge.py`. Changelog is `docs/RELEASE_3.6.1.md`. Bug sweep: owner-only breaking of claimed devices, other mods' flight no longer revoked, seated cells returned when Spiritweave breaks, safer March retrogen, persistent dock-stall stock, rite and hearth fixes, per-tick cost cuts. GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v3.6.1

## 3.6.0 — The living March
Uploaded 2026-09-19: `tribalpower-3.6.0.jar` as file **8922875** ("Tribal Power 3.6.0 - The living March", release, 1.21.1 / NeoForge / Client+Server) via `tools/upload_curseforge.py`. Changelog is `docs/RELEASE_3.6.0.md`. Codex readability overhaul, catalyst gear ranks and seated gear cells, the Gate Rite (drum music minigame; the Gate Drum needs no Pulse), 27 March structures, six trees with wood sets and Weeping Colossus groves, March building set, lakes with water and cave decoration, aquatic and aerial wildlife, a more dangerous March, wolf-like companions with line breeding, one-time March retrogen (worldgen 3). GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v3.6.0

Project description refreshed for 3.6.0 (`docs/public/store-description.md` → `.html`); paste the HTML into the Author Console.

## 3.4.3 — Spiritgear and Spiritweave art pass
Uploaded 2026-09-18: `tribalpower-3.4.3.jar` as file **8914264** ("Tribal Power 3.4.3 - Spiritgear and Spiritweave art pass", release, 1.21.1 / NeoForge / Client+Server). Changelog is `docs/RELEASE_3.4.3.md`. Art only: Spiritgear ranks, Spiritweave icons and worn body, Resonance Maul, Sixfold Staff, Wayfarer Satchel, Spiritweave bolt. GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v3.4.3

## 3.4.2 — Diagnose lockstep, Codex chrome, ranked adapter
Uploaded 2026-09-17: `tribalpower-3.4.2.jar` as file **8908289** ("Tribal Power 3.4.2 - Diagnose lockstep, Codex chrome, ranked adapter", release, 1.21.1 / NeoForge / Client+Server). Changelog is `docs/RELEASE_3.4.2.md`. Ranked Pulse Adapter rate, device status lang keys, Codex chrome i18n, teaching lockstep with live Java. GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v3.4.2

## 3.4.1 — Camp lamps and March sky
Uploaded 2026-09-13: `tribalpower-3.4.1.jar` as file **8876593** ("Tribal Power 3.4.1 - Camp lamps and March sky", release, 1.21.1 / NeoForge / Client+Server). Changelog is `docs/RELEASE_3.4.1.md`. Cheap Pulse lamps, camp furniture, dedicated-server menus, Seal Loom bottles, Side-IO ownership, March feature order, panoramic sky. GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v3.4.1

## 3.4.0 — Keeping, March biomes, ranked gear
Uploaded 2026-09-13: `tribalpower-3.4.0.jar` as file **8871059** ("Tribal Power 3.4.0 - Keeping and the March", release, 1.21.1 / NeoForge / Client+Server). Changelog is `docs/RELEASE_3.4.0.md`. March Snow Fields, Ember Wastes and Reed Fen; Keeping on Resonance Totems; Ley Collector pad; ranked Spiritgear and machines; face-mounted plates. GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v3.4.0

## 3.3.3 — Relay plates and Ley sight
Uploaded 2026-09-12: `tribalpower-3.3.3.jar` as file **8870016** ("Tribal Power 3.3.3 - Relay plates and Ley sight", release, 1.21.1 / NeoForge / Client+Server). Changelog is `docs/RELEASE_3.3.3.md`. Face-mounted relays, Ley Lens modes, per-face machine I/O, grit kiln/textures, Codex walkthroughs, Stone Font z-fight fix. GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v3.3.3

## 3.2.1 — The Listening Pit and the Gates
Uploaded 2026-09-11: `tribalpower-3.2.1.jar` as file **8858043** ("Tribal Power 3.2.1 - The Listening Pit and the Gates", release, 1.21.1 / NeoForge / Client+Server). Changelog is `docs/RELEASE_3.2.1.md`. Packaging-only: published jar omits leftover QA (screenshot drivers, GameTest empty structure, unused written-book copies, FTB Library face overlays). GameTests stay out of the player jar. GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v3.2.1
