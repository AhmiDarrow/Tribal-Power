# 3.1.0 — internal verification notes

Not for the changelog. The public notes are `docs/RELEASE_3.1.0.md`.

## Build and test state

Java 21 / Minecraft 1.21.1 / NeoForge 21.1.249. Production build plus the server GameTest suites:
Lattice, Bestiary, Camp, Familiar, Loom, March, Rite, Tribe, Tribe-sweep, Codex-unlock, and the 3.1
holders — Pattern, Grit, Pit, Gate, Generator. **121 GameTests pass** after the bug sweep (up from 113
on the first 3.1 commit). Codex and lang regeneration checks (`verify_codex.py`, `verify_lang.py`).

## Still open

- **A 3.0 save has been opened against 3.1 (constructed, not a player save).** No CurseForge / `.minecraft`
  / `run/world` save was last played on 3.0.0. A disposable world was generated with tag `v3.0.0`
  (`Tribal Power 3.0.0`) via `build/src-3.0.0` `gradlew runServer --offline`, then frozen at
  `build/migration-3.0`. Chunk `0,0` NBT has `tribalpower:rite_pedestal` in the section palette and **no**
  block entity at `4,70,4`; the 3.0 RCON check was `The target block is not a block entity`. The same
  world was copied to `build/migration-3.1-load` and loaded with 3.1 (`gradlew runMigration31 --offline`).
  It came up without a crash (`tribalpower (version 3.0.0 -> 3.1.0)`). The pedestal woke as an empty
  working `RitePedestalBlockEntity` (`Items: []`, then accepted a Spirit Shard in `container.0`); the
  3.0 brazier, Drumheart, earth totem and cistern kept their block ids and block entities. Logs:
  `build/migration-3.0/evidence/3.0-server-latest.log` and
  `build/migration-3.0/evidence/3.1-server-latest.log`. The GameTest
  `aPedestalWithNoSavedBlockEntityWakesUpWorking` is still only the in-memory strip, not this load.
- **Drum Circle music disc** from design 3.1 §15 is still unauthored (no original track; do not ship a
  silent disc). Device hits are in; see Done below.

## Done — Sounds / Wind Harp

- **Wind Harp model** is a folk-harp cuboid (plinth, forepillar, neck, soundbox, five strings) from
  `tools/generate_3_1_assets.py`. Blender was not on this machine, so there is no `.blend` rig; block id
  stays `tribalpower:wind_harp`.
- **Device hits** are original short OGGs (`tools/generate_3_1_sounds.py`) wired through `ModSounds` and
  `sounds.json`: Drumheart on/off tempo, Ember Horn roar, Wind Harp string, Wave Drum slap, Wake Bell
  toll, gate hum and transit, mesh sift, font form, chalk draw. Vanilla `SoundEvents` remain only where
  nothing 3.1-specific was specified.

## Fixed in the 3.1 bug sweep

Regression tests were added for each of these; see `PitGameTests`, `GritGameTests`, `PatternGameTests`.

1. Ritual Marks could not be drawn at all — `RitualChalkItem` still only did 3.0 totem linking, so every
   pattern with an `m` cell (Stone Font, Listening Pit, Rite Circle) was unbuildable outside creative.
2. The Resonance Mesh registered no capabilities: its water tank was unreachable by bucket, cistern or
   fluid relay, so Washing could never fire, and no item relay could feed it or the Stone Font.
3. The hot band was unreachable, and `c:ores/quartz` (the Nether's ore) sat in the deep band, so a Kin pit
   produced Nether Quartz Ore at deep's price with no Fire lit.
4. `finish()` voided any bonus output that did not fit, breaking the never-voids guarantee.
5. Washing drained the tank before checking whether it held enough, eating a trickle supply forever.
6. A mesh with no owner was treated as Voice standing — the top rank — instead of Stranger.
7. `EchoStationBlockEntity.drainToCache` marked only the cache dirty; an unload after an early return
   could restore the stacks the station had already given away.
8. The gate keystone's two-tick transit pulse never fell, latching the comparator at 15.
9. The synthesised shatter gave one grit where the written recipes give two, so silk-touching a metal ore
   was worse than mining it and every modded metal ran at half the vanilla rate.
10. Redstone next to the Ancestral Cache stalled the pit, and any stall wiped the cycle's paid-for Pulse.
11. `PatternState` added its stagger to the period instead of using it as a phase: revalidation ran every
    40-79 ticks rather than every 40.
12. The brazier and the keystone read their first neighbour update as a rising edge, so one placed into an
    already-powered spot fired a rite or spent 400 Pulse unbidden.
13. Mesh and Stone Font `state` was not persisted, so the comparator read 0 for a second after a reload.
14. The mesh comparator spanned 1-13, never reaching 15.
15. `Predicates.tag` described a missing tag with an untranslated key.
