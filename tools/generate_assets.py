#!/usr/bin/env python3
"""Generate procedural 16x16 textures and JSON assets for Tribal Power rewrite."""
from __future__ import annotations

import json
import os
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/tribalpower"
DATA = ROOT / "src/main/resources/data/tribalpower"

# Palette: teal / bone / copper / spirit-violet
TEAL = (46, 139, 140)
TEAL_DK = (28, 90, 92)
TEAL_LT = (110, 190, 188)
BONE = (232, 220, 198)
BONE_DK = (180, 164, 140)
COPPER = (184, 115, 51)
COPPER_LT = (212, 150, 90)
VIOLET = (120, 72, 168)
VIOLET_LT = (168, 120, 210)
VIOLET_DK = (72, 40, 110)
BROWN = (110, 72, 42)
BROWN_DK = (70, 44, 24)
ORANGE = (200, 90, 40)
BLUE = (60, 110, 180)
GREEN = (70, 130, 70)
GOLD = (210, 170, 60)
BLACK = (24, 24, 28)
WHITE = (245, 245, 240)
CYAN = (80, 200, 210)


def ensure(p: Path) -> None:
    p.mkdir(parents=True, exist_ok=True)


def save(img: Image.Image, path: Path) -> None:
    ensure(path.parent)
    img.save(path, "PNG")


def new_img(size=16) -> Image.Image:
    return Image.new("RGBA", (size, size), (0, 0, 0, 0))


def fill(img: Image.Image, color, alpha=255) -> None:
    d = ImageDraw.Draw(img)
    c = (*color[:3], alpha)
    d.rectangle([0, 0, img.size[0] - 1, img.size[1] - 1], fill=c)


def noise_dots(img: Image.Image, color, step=3, alpha=180) -> None:
    px = img.load()
    w, h = img.size
    r, g, b = color[:3]
    for y in range(0, h, step):
        for x in range((y // step) % 2, w, step):
            if 0 <= x < w and 0 <= y < h:
                pr, pg, pb, pa = px[x, y]
                if pa > 0:
                    px[x, y] = (min(255, (pr + r) // 2), min(255, (pg + g) // 2), min(255, (pb + b) // 2), pa)


def border(img: Image.Image, color) -> None:
    d = ImageDraw.Draw(img)
    w, h = img.size
    d.rectangle([0, 0, w - 1, h - 1], outline=(*color, 255))


def block_tex(base, accent=None, pattern="noise") -> Image.Image:
    img = new_img()
    fill(img, base)
    if accent:
        noise_dots(img, accent, 2)
    border(img, tuple(max(0, c - 40) for c in base[:3]))
    if pattern == "grid":
        d = ImageDraw.Draw(img)
        for i in range(0, 16, 4):
            d.line([(i, 0), (i, 15)], fill=(*tuple(max(0, c - 25) for c in base[:3]), 255))
            d.line([(0, i), (15, i)], fill=(*tuple(max(0, c - 25) for c in base[:3]), 255))
    return img


def item_orb(core, rim) -> Image.Image:
    img = new_img()
    d = ImageDraw.Draw(img)
    d.ellipse([2, 2, 13, 13], fill=(*core, 255), outline=(*rim, 255))
    d.point((5, 5), fill=(*WHITE, 220))
    return img


def item_ingot(metal) -> Image.Image:
    img = new_img()
    d = ImageDraw.Draw(img)
    d.rectangle([2, 5, 13, 10], fill=(*metal, 255), outline=(*tuple(max(0, c - 50) for c in metal), 255))
    d.line([(3, 6), (12, 6)], fill=(*tuple(min(255, c + 40) for c in metal), 255))
    return img


def item_seal(color) -> Image.Image:
    img = new_img()
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([2, 1, 13, 14], radius=2, fill=(*BONE, 255), outline=(*BONE_DK, 255))
    d.ellipse([5, 4, 10, 9], fill=(*color, 255))
    d.rectangle([6, 10, 9, 12], fill=(*color, 255))
    return img


def item_tool(head, haft) -> Image.Image:
    img = new_img()
    d = ImageDraw.Draw(img)
    d.line([(4, 12), (11, 5)], fill=(*haft, 255), width=2)
    d.polygon([(9, 2), (14, 4), (12, 8), (8, 6)], fill=(*head, 255))
    return img


def item_book() -> Image.Image:
    img = new_img()
    d = ImageDraw.Draw(img)
    d.rectangle([3, 2, 12, 14], fill=(*VIOLET, 255), outline=(*VIOLET_DK, 255))
    d.rectangle([4, 3, 11, 13], fill=(*BONE, 255))
    d.line([(7, 3), (7, 13)], fill=(*VIOLET_DK, 255))
    d.point((9, 6), fill=(*GOLD, 255))
    return img


def plant_tex(stem, leaf) -> Image.Image:
    img = new_img()
    d = ImageDraw.Draw(img)
    d.line([(8, 14), (8, 6)], fill=(*stem, 255))
    d.ellipse([4, 2, 11, 9], fill=(*leaf, 220))
    return img


def drum_tex() -> Image.Image:
    img = new_img()
    d = ImageDraw.Draw(img)
    fill(img, BROWN)
    d.ellipse([1, 2, 14, 13], fill=(*BONE, 255), outline=(*BROWN_DK, 255))
    d.ellipse([4, 5, 11, 10], fill=(*VIOLET_LT, 180))
    border(img, BROWN_DK)
    return img


def totem_tex(color) -> Image.Image:
    img = new_img()
    d = ImageDraw.Draw(img)
    d.rectangle([5, 1, 10, 15], fill=(*BONE_DK, 255), outline=(*BROWN_DK, 255))
    d.rectangle([6, 3, 9, 6], fill=(*color, 255))
    d.rectangle([6, 8, 9, 11], fill=(*color, 200))
    return img


def cache_tex() -> Image.Image:
    img = block_tex(BROWN, BONE, "grid")
    d = ImageDraw.Draw(img)
    d.rectangle([6, 7, 9, 10], fill=(*GOLD, 255))
    return img


def entity_skin(base, accent, size=64) -> Image.Image:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Simple steve-like layout regions tinted
    d.rectangle([0, 0, size - 1, size - 1], fill=(*base, 255))
    for y in range(0, size, 8):
        for x in range(0, size, 8):
            if (x + y) % 16 == 0:
                d.rectangle([x, y, x + 3, y + 3], fill=(*accent, 255))
    return img


BLOCKS = {
    "drumheart": drum_tex,
    "ley_collector": lambda: block_tex(CYAN, VIOLET, "grid"),
    "pulse_resonator": lambda: block_tex(COPPER, ORANGE, "grid"),
    "resonance_totem_earth": lambda: totem_tex(GREEN),
    "resonance_totem_fire": lambda: totem_tex(ORANGE),
    "resonance_totem_water": lambda: totem_tex(BLUE),
    "resonance_totem_air": lambda: totem_tex(BONE),
    "resonance_totem_spirit": lambda: totem_tex(VIOLET),
    "song_bench": lambda: block_tex(BROWN, VIOLET_LT),
    "lattice_conductor": lambda: block_tex(COPPER, COPPER_LT, "grid"),
    "echo_shatter": lambda: block_tex(BONE_DK, TEAL),
    "echo_attune": lambda: block_tex(TEAL, VIOLET),
    "echo_bind": lambda: block_tex(VIOLET, TEAL_LT),
    "echo_manifest": lambda: block_tex(GOLD, VIOLET_LT),
    "ancestral_cache": cache_tex,
    "deep_cache": lambda: block_tex(BLACK, VIOLET, "grid"),
    "rite_pedestal": lambda: block_tex(BONE_DK, GOLD),
    "gate_drum": lambda: drum_tex(),
    "spirit_door": lambda: block_tex(VIOLET, CYAN),
    "march_stone": lambda: block_tex(TEAL_DK, TEAL_LT),
    "march_cobble": lambda: block_tex(TEAL_DK, BONE_DK, "noise"),
    "march_soil": lambda: block_tex((62, 78, 58), TEAL_DK),
    "march_grass": lambda: block_tex((58, 140, 120), TEAL_LT),
    "march_log": lambda: block_tex(BROWN, TEAL_DK),
    "march_planks": lambda: block_tex(BROWN, BONE, "grid"),
    "march_leaves": lambda: block_tex(TEAL, VIOLET_LT),
    "march_leaf": lambda: plant_tex(TEAL_DK, TEAL_LT),
    "march_ore": lambda: block_tex(TEAL_DK, VIOLET_LT, "grid"),
    "march_crystal": lambda: block_tex(CYAN, VIOLET_LT),
    "spirit_reed": lambda: plant_tex(VIOLET_DK, CYAN),
    "echo_bloom": lambda: plant_tex(VIOLET_DK, VIOLET_LT),
}

ITEMS = {
    "spirit_codex": item_book,
    "spirit_shard": lambda: item_orb(VIOLET_LT, VIOLET_DK),
    "bone_chime": lambda: item_orb(BONE, BONE_DK),
    "copper_resonator": lambda: item_ingot(COPPER),
    "pulse_cell": lambda: item_orb(CYAN, TEAL_DK),
    "ritual_chalk": lambda: item_ingot(BONE),
    "echo_shard": lambda: item_orb(BONE_DK, TEAL),
    "attuned_echo": lambda: item_orb(TEAL, VIOLET),
    "bound_echo": lambda: item_orb(VIOLET, TEAL),
    "manifested_ingot": lambda: item_ingot(GOLD),
    "blank_seal": lambda: item_seal(BONE_DK),
    "earth_seal": lambda: item_seal(GREEN),
    "fire_seal": lambda: item_seal(ORANGE),
    "water_seal": lambda: item_seal(BLUE),
    "air_seal": lambda: item_seal(BONE),
    "spirit_seal": lambda: item_seal(VIOLET),
    "spiritgear_pickaxe": lambda: item_tool(VIOLET, BROWN),
    "spiritgear_axe": lambda: item_tool(TEAL, BROWN),
    "spiritgear_shovel": lambda: item_tool(GOLD, BROWN_DK),
    "spiritgear_blade": lambda: item_tool(CYAN, BROWN_DK),
}

BLOCK_ITEMS = list(BLOCKS.keys())


def write_json(path: Path, data) -> None:
    ensure(path.parent)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def gen_models() -> None:
    for name in BLOCKS:
        if name == "pulse_resonator":
            write_json(ASSETS / f"blockstates/{name}.json", {
                "variants": {
                    "lit=false": {"model": "tribalpower:block/pulse_resonator"},
                    "lit=true": {"model": "tribalpower:block/pulse_resonator_on"}
                }
            })
            write_json(ASSETS / "models/block/pulse_resonator_on.json", {
                "parent": "minecraft:block/cube_all",
                "textures": {"all": "tribalpower:block/pulse_resonator_on"}
            })
        else:
            write_json(ASSETS / f"blockstates/{name}.json", {
                "variants": {"": {"model": f"tribalpower:block/{name}"}}
            })
        write_json(ASSETS / f"models/block/{name}.json", {
            "parent": "minecraft:block/cube_all",
            "textures": {"all": f"tribalpower:block/{name}"}
        })
        write_json(ASSETS / f"models/item/{name}.json", {
            "parent": f"tribalpower:block/{name}"
        })

    for name in ITEMS:
        write_json(ASSETS / f"models/item/{name}.json", {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"tribalpower:item/{name}"}
        })


def gen_lang() -> None:
    lang = {
        "itemGroup.tribalpower": "Tribal Power",
        "item.tribalpower.spirit_codex": "Spirit Codex",
        "item.tribalpower.spirit_codex.desc": "Field guide to shamanic technomancy",
        "item.tribalpower.spirit_shard": "Spirit Shard",
        "item.tribalpower.bone_chime": "Bone Chime",
        "item.tribalpower.copper_resonator": "Copper Resonator",
        "item.tribalpower.pulse_cell": "Pulse Cell",
        "item.tribalpower.ritual_chalk": "Ritual Chalk",
        "item.tribalpower.ritual_chalk.desc": "Mark two Resonance Totems to seal a lattice link",
        "item.tribalpower.ritual_chalk.pending": "Pending mark: %s, %s, %s",
        "item.tribalpower.echo_shard": "Echo Shard",
        "item.tribalpower.attuned_echo": "Attuned Echo",
        "item.tribalpower.bound_echo": "Bound Echo",
        "item.tribalpower.manifested_ingot": "Manifested Ingot",
        "item.tribalpower.blank_seal": "Blank Seal",
        "item.tribalpower.earth_seal": "Earth Seal",
        "item.tribalpower.fire_seal": "Fire Seal",
        "item.tribalpower.water_seal": "Water Seal",
        "item.tribalpower.air_seal": "Air Seal",
        "item.tribalpower.spirit_seal": "Spirit Seal",
        "item.tribalpower.spiritgear_pickaxe": "Spiritgear Pickaxe",
        "item.tribalpower.spiritgear_blade": "Spiritgear Blade",
        "item.tribalpower.spiritgear.desc": "Consumes spirit strain while working",
        "block.tribalpower.drumheart": "Drumheart",
        "block.tribalpower.ley_collector": "Ley Collector",
        "block.tribalpower.pulse_resonator": "Pulse Resonator",
        "block.tribalpower.resonance_totem_earth": "Resonance Totem (Earth)",
        "block.tribalpower.resonance_totem_fire": "Resonance Totem (Fire)",
        "block.tribalpower.resonance_totem_water": "Resonance Totem (Water)",
        "block.tribalpower.resonance_totem_air": "Resonance Totem (Air)",
        "block.tribalpower.resonance_totem_spirit": "Resonance Totem (Spirit)",
        "block.tribalpower.song_bench": "Song Bench",
        "block.tribalpower.lattice_conductor": "Lattice Conductor",
        "block.tribalpower.echo_shatter": "Echo Shatter",
        "block.tribalpower.echo_attune": "Echo Attune",
        "block.tribalpower.echo_bind": "Echo Bind",
        "block.tribalpower.echo_manifest": "Echo Manifest",
        "block.tribalpower.ancestral_cache": "Ancestral Cache",
        "block.tribalpower.deep_cache": "Deep Cache",
        "block.tribalpower.rite_pedestal": "Rite Pedestal",
        "block.tribalpower.gate_drum": "Gate Drum",
        "block.tribalpower.spirit_door": "Spirit Door",
        "block.tribalpower.march_stone": "March Stone",
        "block.tribalpower.march_cobble": "March Cobble",
        "block.tribalpower.march_soil": "March Soil",
        "block.tribalpower.march_grass": "March Grass",
        "block.tribalpower.march_log": "March Log",
        "block.tribalpower.march_planks": "March Planks",
        "block.tribalpower.march_leaves": "March Leaves",
        "block.tribalpower.march_leaf": "March Leaf",
        "block.tribalpower.march_ore": "March Ore",
        "block.tribalpower.march_crystal": "March Crystal",
        "block.tribalpower.spirit_reed": "Spirit Reed",
        "block.tribalpower.echo_bloom": "Echo Bloom",
        "entity.tribalpower.spirit_wisp": "Spirit Wisp",
        "entity.tribalpower.march_walker": "March Walker",
        "attunement.tribalpower.earth": "Earth",
        "attunement.tribalpower.fire": "Fire",
        "attunement.tribalpower.water": "Water",
        "attunement.tribalpower.air": "Air",
        "attunement.tribalpower.spirit": "Spirit",
        "message.tribalpower.drumheart.beat": "Drumheart +%s Pulse (%s/%s)",
        "message.tribalpower.ley_collector.status": "Ley Collector Pulse %s/%s",
        "message.tribalpower.pulse_resonator.status": "Pulse Resonator %s/%s — burn %s, fuel %s",
        "message.tribalpower.pulse_resonator.need_fuel": "Pulse Resonator %s/%s — feed coal or charcoal",
        "message.tribalpower.pulse_resonator.fueled": "Resonator fueled (stock %s) — Pulse %s/%s",
        "message.tribalpower.pulse_resonator.full_fuel": "The Resonator already holds a full fuel stack",
        "message.tribalpower.pulse_resonator.removed_fuel": "Fuel lifted from the Pulse Resonator",
        "message.tribalpower.pulse_cell.charge_resonator": "Pulse Cell +%s (%s/%s) — Resonator %s",
        "message.tribalpower.totem.attunement": "Totem attunement: %s",
        "message.tribalpower.totem.links": "Lattice links: %s",
        "message.tribalpower.song_bench.start": "Song begun — %s totems answering",
        "message.tribalpower.song_bench.idle": "Song Bench silent — place grit and strike the song",
        "message.tribalpower.song_bench.need_item": "The bench waits for ore, grit, or echo",
        "message.tribalpower.song_bench.no_totems": "No Resonance Totems within song range",
        "message.tribalpower.song_bench.no_pulse": "The song stalls — no Pulse nearby",
        "message.tribalpower.song_bench.need_attunement": "Need a %s Resonance Totem nearby",
        "message.tribalpower.song_bench.working": "Echo %s — %s/%s beats (%s totems)",
        "message.tribalpower.song_bench.inserted": "Material laid on the Song Bench",
        "message.tribalpower.song_bench.full": "The Song Bench already holds a voice",
        "message.tribalpower.song_bench.removed": "Material lifted from the Song Bench",
        "message.tribalpower.chalk.mark": "Totem marked — chalk the partner totem",
        "message.tribalpower.chalk.clear": "Chalk mark cleared",
        "message.tribalpower.chalk.too_far": "Those totems are beyond lattice range",
        "message.tribalpower.chalk.link": "Lattice link sealed",
        "message.tribalpower.chalk.already": "Those totems are already linked",
        "message.tribalpower.chalk.fail": "The chalk finds no lasting mark",
        "message.tribalpower.rite.success": "The seal answers the pedestal",
        "message.tribalpower.rite.fail": "The rite finds no voice",
        "message.tribalpower.gate.travel": "The Gate Drum opens The March",
        "message.tribalpower.gate.fail": "The March cannot be reached",
        "message.tribalpower.gate.charge": "Gate Drum +%s Pulse (%s/%s)",
        "message.tribalpower.gate.full": "The Gate Drum is fully charged",
        "message.tribalpower.gate.cell_empty": "That Pulse Cell holds no charge",
        "message.tribalpower.gate.need_pulse": "Gate needs %s Pulse (has %s) — use a Pulse Cell or sneak-drum",
        "message.tribalpower.deep_cache.need_pulse": "Deep Cache needs a March link or Pulse in a cell",
        "message.tribalpower.deep_cache.pulse_link": "Pulse spent — temporary spirit link formed",
        "message.tribalpower.spiritgear.pulse": "Spiritgear draws a pulse of strain",
        "biome.tribalpower.march_steppe": "March Steppe",
    }
    # Merge rather than overwrite: later scripts (expand_content, generate_camp, generate_tribe_data, the 3.0
    # feature packages) add and refine keys in place, so an existing key keeps its current value and only
    # keys this bootstrap knows and the file lacks are added.
    path = ASSETS / "lang/en_us.json"
    if path.exists():
        existing = json.loads(path.read_text(encoding="utf-8"))
        for key, value in lang.items():
            existing.setdefault(key, value)
        lang = existing
    ensure(path.parent)
    path.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def gen_dimension() -> None:
    write_json(DATA / "dimension_type/the_march.json", {
        "ultrawarm": False,
        "natural": True,
        "coordinate_scale": 1.0,
        "has_skylight": True,
        "has_ceiling": False,
        "ambient_light": 0.15,
        "monster_spawn_light_level": {"type": "minecraft:uniform", "min_inclusive": 0, "max_inclusive": 7},
        "monster_spawn_block_light_limit": 0,
        "piglin_safe": False,
        "bed_works": True,
        "respawn_anchor_works": False,
        "has_raids": False,
        "logical_height": 384,
        "height": 384,
        "min_y": -64,
        "infiniburn": "#minecraft:infiniburn_overworld",
        "effects": "minecraft:overworld"
    })

    write_json(DATA / "worldgen/biome/march_steppe.json", {
        "has_precipitation": True,
        "temperature": 0.55,
        "downfall": 0.35,
        "effects": {
            "fog_color": 0x6AA8A8,
            "sky_color": 0x5B7FBF,
            "water_color": 0x3A7A9A,
            "water_fog_color": 0x1E4050,
            "foliage_color": 0x3D9A8C,
            "grass_color": 0x4A8F7A,
            "mood_sound": {
                "sound": "minecraft:ambient.cave",
                "tick_delay": 6000,
                "block_search_extent": 8,
                "offset": 2.0
            }
        },
        "spawners": {
            "monster": [],
            "creature": [
                {"type": "tribalpower:march_walker", "weight": 8, "minCount": 1, "maxCount": 2}
            ],
            "ambient": [
                {"type": "tribalpower:spirit_wisp", "weight": 6, "minCount": 1, "maxCount": 3}
            ],
            "axolotls": [],
            "underground_water_creature": [],
            "water_creature": [],
            "water_ambient": [],
            "misc": []
        },
        "spawn_costs": {},
        "carvers": {},
        "features": []
    })

    # Layered flat March with biome features (trees, ore, crystals, flora).
    write_json(DATA / "dimension/the_march.json", {
        "type": "tribalpower:the_march",
        "generator": {
            "type": "minecraft:flat",
            "settings": {
                "biome": "tribalpower:march_steppe",
                "lakes": False,
                "features": True,
                "layers": [
                    {"height": 1, "block": "minecraft:bedrock"},
                    {"height": 40, "block": "tribalpower:march_stone"},
                    {"height": 4, "block": "tribalpower:march_cobble"},
                    {"height": 3, "block": "tribalpower:march_soil"},
                    {"height": 1, "block": "tribalpower:march_grass"}
                ],
                "structure_overrides": []
            }
        }
    })


def gen_guide_json() -> None:
    chapters = [
        {"id": "intro", "title": "Shamanic Technomancy", "text": "Steward tribes of the Loom braid spirit and craft. Pulse, totem lattices, and Echo — not furnace magic."},
        {"id": "pulse", "title": "Spirit Pulse", "text": "Rhythmic beats stored by PulseHandlers. Drumheart for bursts; Ley Collector for ambient trickle."},
        {"id": "lattice", "title": "Totem Lattice", "text": "Resonance Totems near a Song Bench. Ritual Chalk seals lasting links within 16 blocks."},
        {"id": "echo", "title": "Echo Stages", "text": "Song Bench path: raw ore → Echo Shard (Earth) → Attuned Echo (Fire) → Bound Echo (Water) → Manifested Ingot (Spirit)."},
        {"id": "song", "title": "Song Bench", "text": "Insert grit, start the song, keep Pulse and the right attunement nearby. Shift-click to remove."},
        {"id": "storage", "title": "Caches", "text": "Ancestral Cache locally; Deep Cache via spirit link to The March."},
        {"id": "rites", "title": "Seals & Rites", "text": "Offer seals on a Rite Pedestal for spirit boons."},
        {"id": "march", "title": "The March", "text": "Otherworld dimension reached by Gate Drum."},
    ]
    write_json(DATA / "guide/spirit_codex.json", {"book": "Spirit Codex", "chapters": chapters})


def gen_loot() -> None:
    for name in BLOCKS:
        write_json(DATA / f"loot_table/blocks/{name}.json", {
            "type": "minecraft:block",
            "pools": [{
                "rolls": 1,
                "entries": [{"type": "minecraft:item", "name": f"tribalpower:{name}"}],
                "conditions": [{"condition": "minecraft:survives_explosion"}]
            }]
        })


def main() -> None:
    for name, factory in BLOCKS.items():
        save(factory(), ASSETS / f"textures/block/{name}.png")
    # Lit variant texture for Pulse Resonator (not a separate block id)
    save(block_tex(COPPER_LT, ORANGE, "grid"), ASSETS / "textures/block/pulse_resonator_on.png")
    for name, factory in ITEMS.items():
        save(factory(), ASSETS / f"textures/item/{name}.png")
    save(entity_skin(VIOLET_LT, CYAN), ASSETS / "textures/entity/spirit_wisp.png")
    save(entity_skin(TEAL, BONE), ASSETS / "textures/entity/march_walker.png")
    gen_models()
    gen_lang()
    gen_dimension()
    gen_guide_json()
    gen_loot()
    print(f"Generated assets under {ASSETS} and {DATA}")


if __name__ == "__main__":
    main()
