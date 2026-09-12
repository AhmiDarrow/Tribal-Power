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

## 3.2.1 — The Listening Pit and the Gates
Uploaded 2026-09-11: `tribalpower-3.2.1.jar` as file **8858043** ("Tribal Power 3.2.1 - The Listening Pit and the Gates", release, 1.21.1 / NeoForge / Client+Server). Changelog is `docs/RELEASE_3.2.1.md`. Packaging-only: published jar omits leftover QA (screenshot drivers, GameTest empty structure, unused written-book copies, FTB Library face overlays). GameTests stay out of the player jar. GitHub: https://github.com/AhmiDarrow/Tribal-Power/releases/tag/v3.2.1
