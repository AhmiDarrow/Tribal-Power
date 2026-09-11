---
name: tribal-power
description: Develop, gauntlet-test, and ship this Tribal Power clone. Use when editing this repo, running GameTests, generating 3.1 assets, or cutting a release.
---

# Tribal Power

Minecraft 1.21.1 / NeoForge 21.1.249 / Java 21. Mod id `tribalpower`. Version: `gradle.properties` `mod_version`. GitHub `AhmiDarrow/Tribal-Power`, linear **`master`**.

Also use `minecraft-modding` and `minecraft-testing`. This file wins on **this repo's** commands.

## Do not commit

`Claude outputs/`, `tribal-power-*.bundle*`, `_review_copy.py`, `_review_copy.bat`, `fetch_3.0.1_and_3.1.0.py`, `fetch_3.0.1_and_3.1.0.bat`. Never `git add -A`.

## Gauntlet

```powershell
python tools\verify_lang.py
python tools\verify_codex.py
.\gradlew.bat compileJava --offline
.\gradlew.bat runVerification --offline
.\gradlew.bat build --offline -x runVerification
```

GameTests live in `src/main/java/tk/darrow/tribalpower/verification/`. The Gradle task is **`runVerification`**, not `runGameTestServer`. After a failed command, `claimidx ask` first.

## Generators

`tools/generate_3_1_assets.py`, `tools/generate_3_1_sounds.py`, `tools/generate_3_1_disc.py`, `tools/generate_structures.py`, `tools/verify_lang.py`, `tools/verify_codex.py`. Blender: `tools/blender_*.py` (Wind Harp: `blender_wind_harp.py`).

## 3.1

Public: `docs/RELEASE_3.1.0.md`. Internal: `docs/RELEASE_3.1.0_INTERNAL.md`.  
3.0 pedestal load, device hits, Drum Circle disc, and Wind Harp cuboid+Blender rig are checked off in the internal notes. Do not ship silent oggs.

When asked to land on master: `git checkout master && git merge --ff-only <feature-branch>`. Keep README version in lockstep with `mod_version`.
