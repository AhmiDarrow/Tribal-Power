# Tribal Power identity

The master is `tribal-power-logo.png`. The CurseForge upload is `../../docs/public/tribal-power-icon-400.png`, exactly 400 by 400 pixels. The upload was resampled directly from the master with high-quality bicubic interpolation, preserving the complete square composition.

Generated with the built-in image-generation tool on 2026-09-07.

Prompt: A polished square TRIBAL POWER game-mod cover. Large two-line carved ivory and gold lettering; an original stone and dark wood spirit totem with a radiant turquoise crystal heart, five elemental glyph stones in a broken bronze ring, and ley threads joining roots and magical circuitry. Indigo twilight, teal aurora, ancient trees and floating-rock silhouettes. Painterly voxel fantasy, bold silhouette, readable at small sizes, generous margins, full dark square background. No subtitle, official Minecraft logo, real indigenous insignia, or furnace/firewood engine.

Published to canonical CurseForge project 1684851 with the updated standalone and modpack feature description.

## Review sheets (3.0)

Generated renders for reviewing the 3.0 art without a client. Regenerate with the named tool; every one is deterministic.

- `art/blocks-3.0-contact-sheet.png` and `art/items-3.0-contact-sheet.png` — every block model and item texture added or changed since v2.3.0, rendered as the inventory shows them (`tools/render_blocks.py`, Blender + Pillow).
- `art/living-lattice-contact-sheet.png` — the 2.x Living Lattice block set (`tools/overhaul_art.py`).
- `art/structures/structures-3.0-contact-sheet.png` plus one isometric voxel render per template (`tribe_camp_<id>.png` x9, `ancestor_hall.png`, `ancestor_hall_cutaway.png`, `drum_circle.png`, `crystal_spire.png`) — `tools/render_structures.py`.
- `art/creatures/bestiary-contact-sheet.png` — the thirteen bestiary rigs; `art/creatures/kin-and-unsung.png` — the four Kin roles with tinted cloaks and The Unsung; `art/creatures/preview/` — single-creature previews; `art/creatures/atlas-sheet.png` — every painted entity atlas (`tools/blender_bestiary.py`, `tools/art/creatures.py`).
- `art/skies/aurora-overworld.png`, `art/skies/aurora-march.png` — sky reference captures.
- Codex art lives in `src/main/resources/assets/tribalpower/textures/gui/codex/` (`tools/art/codex.py`): Kin, Unsung, structure vignettes and the nine tribe crests.
