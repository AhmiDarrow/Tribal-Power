# Asset review — 2026-09-13

Resonant Core was pixel-identical to Bound Echo. A hand-authored 16×16 ring-and-core sprite now distinguishes the crafted component from the purple echo shard. Removed the unused missing anchor_stone_top texture reference from the Anchor Stone model; all faces already use its existing side texture.

Sources: `art/reviewed-pixel-items.json`. Export: `python tools/export_reviewed_pixel_art.py --write`; check: `python tools/export_reviewed_pixel_art.py`. Narrow .gitignore exceptions preserve this small source file and exporter while other authoring files remain ignored. The local ignored overhaul_art.py has also been guarded against repainting reviewed item names.

Retain the creature rigs, codex portraits, tribe insignia, ranked equipment and material palette. The existing bestiary checker passes for 13 rigs plus Kin and Unsung, including UV bounds, overlays and portraits. Duplicate FTB portraits are intentional integration copies. Empty elder/hunter glow overlays are retained; empty overlay files alone do not prove an asset defect.

Some machine side faces reuse patterns. Evaluate their complete models and front/top cues in-game before changing these shared materials. Source image/JSON/animation/reference checks pass. Gradle resource processing is blocked by a local Java loopback error; in-game GUI scale, animation and material seam checks are outstanding.
