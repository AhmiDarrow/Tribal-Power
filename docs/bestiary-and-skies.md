# The Returning Song

Three new animals and ten hostiles join the existing March Walker and Spirit Wisp. Dawn Stags, Lantern Foxes and Mossbacks breed with wheat, sweet berries and seagrass. Adults yield one reagent per minute when brushed; their cooldown survives saving. Different species cannot interbreed.

Overworld forests: Dawn Stag, Lantern Fox, Ashbound, Rootbound. Swamps: Mossback, Reed Stalker. Every species also occurs in a March biome. The steppe carries Ashbound, Rootbound, Rift Hound and Echo Weaver; highlands carry Hollow Sentinel, Storm Moth, Cinder Imp and Rift Hound; crystal fields carry Shardback, Mourning Bell, Reed Stalker and Echo Weaver. Hostiles obey the normal darkness and difficulty rules.

The three ranged spirits telegraph for 16 ticks and require clear line of sight within twelve blocks. Their cooldown is 70 ticks. They never edit terrain. Reagents yield Spiritweave or Attuned Echoes through native Lattice recipes. Original resource routes remain available.

## Editable Blender source

`art/creatures/tribal_bestiary.blend` contains named creature collections, pivot rigs, simple walk/wing animation, materials, packed atlas textures, camera and studio lighting. `tools/create_bestiary.py` authors the profile/rig specification. Run `tools/blender_bestiary.py` with Blender 4.5 LTS to rebuild the scene, export native Java cuboid layers and render the contact sheet. Geometry is read back from Blender object transforms for export. Blender is an authoring dependency only; players do not need it or an animation library.

Runtime animation lives in LatticeCreatureModel. Runtime content profiles, AI and spawn configuration live in entity/, recipe/lattice/, loot_table/entities/, neoforge/biome_modifier/ and the three March biome JSON files. When changing a species, update its corresponding profile and data alongside the rig specification. Run `tools/verify_bestiary_assets.py`, regenerate the Spirit Codex and run the GameTests.

## Skies

The night veil uses three animated translucent ribbons plus nine small constellations. Opacity follows Minecraft day time and fades in rain/thunder. Terrain and roofs occlude it because it draws before world geometry. It is hidden underwater and outside the Overworld/March. The March uses a distinct twilight and fog palette.

Client configuration: `tribalpower-client.toml`; auroraEnabled, overworldAurora, marchAurora, auroraIntensity, auroraQuality (0/1/2). Disable the aurora if a shader pack supplies its own sky. Standard detail uses 5,760 ribbon vertices, with no world particles or chunk loading.

The pack adds a fifteen-quest Returning Song branch after the Gate Drum. It provides a brush, explains each habitat and threat, and completes with all thirteen reagents. This branch does not replace the main progression.

Run `tools/blender_icon_pass.py` with Blender after the main model export to refresh the FTB creature thumbnails. `tools/verify_bestiary_assets.py` compares every exported cuboid against its authored dimensions to catch stale Blender transforms.
