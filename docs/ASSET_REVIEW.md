# Asset review — 2026-09-13

Resonant Core was pixel-identical to Bound Echo. A hand-authored 16×16 ring-and-core sprite now distinguishes the crafted component from the purple echo shard. Removed the unused missing anchor_stone_top texture reference from the Anchor Stone model; all faces already use its existing side texture.

Sources: `art/reviewed-pixel-items.json`. Export: `python tools/export_reviewed_pixel_art.py --write`; check: `python tools/export_reviewed_pixel_art.py`. Narrow .gitignore exceptions preserve this small source file and exporter while other authoring files remain ignored. The local ignored overhaul_art.py has also been guarded against repainting reviewed item names.

Retain the creature rigs, codex portraits, tribe insignia, ranked equipment and material palette. The existing bestiary checker passes for 13 rigs plus Kin and Unsung, including UV bounds, overlays and portraits. Duplicate FTB portraits are intentional integration copies. Empty elder/hunter glow overlays are retained; empty overlay files alone do not prove an asset defect.

Some machine side faces reuse patterns. Evaluate their complete models and front/top cues in-game before changing these shared materials. Source image/JSON/animation/reference checks pass. Gradle resource processing is blocked by a local Java loopback error; in-game GUI scale, animation and material seam checks are outstanding.

## Gear and armor pass — 2026-09-18

Spiritgear, Spiritweave, the Resonance Maul, Sixfold Staff, Wayfarer Satchel and the Spiritweave bolt were flat fills; rank sprites were brightened copies of rank 0 with a tinted rim. All 36 are now painted by `tools/polish_gear_art.py` from five-tone hue-shifted ramps with a lit bevel and a tinted outline. Ranks share one drawing and differ by fittings: thread (base), copper (Attuned), bound light (Bound), gold (Manifested). Tool heads follow the vanilla handheld convention: the axe bit leads up-left of the haft and the pick is symmetric about it, so both face forward in the hand. The blade is a slightly curved single-edged long sword, ground on the leading edge only, with four runes on the flat inlaid per rank. Names, sizes (32px) and item models are unchanged.

The worn layers were one tiled pattern across the whole canvas, so every cuboid face showed the same stripes. `spiritweave_layer_1/2.png` are now painted per face on the vanilla 64×32 unwrap at 2× (128×64): open-faced hood with a brow brim on the hat shell, robe with collar V, belt, hem and back sigil, cuffed sleeves, bound boots with leather soles, and leggings with waist sash, knee wraps and an outer seam.

Run: `python tools/polish_gear_art.py` (`--sheet` writes review sheets to build/tmp/artpass). `generate_rank_textures.py` and `overhaul_art.py` skip the names this tool owns. Worn layers and held tools were reviewed in a creative dev client.

## Visual cohesion pass — 2026-09-19

Target: 32px native, five-tone hue-shifted ramps, top-left light, tinted outline, alpha 0 or 255.

- **Creature reagents.** The thirteen reagents were one tilted crystal recoloured per creature. Each now shows what it is: Storm Wing (moth wing with eyespot), Bell Fragment (bell shard, lip bead and engraved band), Rift Tooth (canine with a rift crack), Reed Fang (curved fang, reed-bound), Prism Carapace (shell plate with prisms), Sentinel Sigil (brass plate with an eye), Ember Heart, Cinder Knot, Echo Silk (hank on a twig), Knotted Root, Lantern Down (plume), Dawn Velvet (velvet antler fork) and Mossback Scale (scute). The echo catalysts left on the generic crystal fallback (Echo Shard, Attuned Echo, Bound Echo, Spirit Shard) have their own silhouettes.
- **Grits** are clean heaps with a lit crown, defined grains and a few loose chips instead of chip noise.
- **Resonant Core** is redrawn at 32px; it stays a reviewed pixel map (`art/reviewed-pixel-items.json`, `additional_sheets`).
- **Camp props** `spirit_urn`, `woven_mat` and `wind_charm` are 32px. **Glasswing** particles are a lilac-rimmed clear-wing butterfly (the old fill read as a missing-texture checker).
- **Woods.** Log ends use pixel growth rings, planks use muted five-tone ramps with seams, lit lips and staggered joints, and each wood has its own door, trapdoor and door item: willow lattice and weave, hearthoak arch and Z-brace, bellcap porthole and bell panel, frostpine herringbone and slits, cinder iron straps and grille, strider louvres.
- **Machine tops** were one bone disc. Caches have lids, relays a port with rank pips, drums a laced drumhead, totems their element, and each Echo station, brazier, pedestal, cistern and bench its own working surface.
- Removed seven `*_top` textures that no model used: echo_bloom, ley_thistle, march_crystal, march_leaf, march_leaves, pulse_resonator_on and spirit_reed.

Run `python tools/paint_material_art.py` (reagents, grits, props; `--reviewed` rewrites the Resonant Core map, then `python tools/export_reviewed_pixel_art.py --write`). `tools/_repo_guard.py` keeps the authoring tools inside this repository: a project path argument outside the repo is refused unless `--allow-foreign-root` is passed, and even then only `tribalpower` assets are written. The catalogue pass paints only the `tribalpower` namespace unless `--allow-namespace=<ns>` is given.
