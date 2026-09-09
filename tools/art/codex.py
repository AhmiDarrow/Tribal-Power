"""Painter for the Spirit Codex 3.0 pages (design 3.0 §8): 128x128 RGBA art in textures/gui/codex/.

  tribe_<id>.png x9    heraldic crest: the tribe glyph on a shield in the tribe colour (pixel art, 64 px grid x2)
  ancestor_hall.png    Blender vignette: a hall corner with Lore Tablets, Loom-wood beams and an Ancestral Cache
  drum_circle.png      Blender vignette: the Silent Drum inside its ring of lantern pillars
  crystal_spire.png    Blender vignette: the March Crystal spire with the Loom-stitcher waystation at its foot
  3.0 teachings        loom_sixth_voice, rite_world_rites, rite_land_rites, camps_identity, ley_lens, pulse_logic,
                       codex_diagnostics, tribes_standing, tribes_kinship_totem and the shared lore_tablet_page
                       (Blender vignettes of the real block models; flat item quads stand in for seals and tablets)
The vignettes reuse tools/render_blocks.py (the block model renderer) so they always match the real models and
textures. Run: python3 tools/art/codex.py [project root] [--crests-only] [--only name,name]
"""
from pathlib import Path
import sys
import tempfile
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from glyphs import TRIBES, COLOURS, GLYPH_NAMES, draw_glyph  # noqa: E402
from lattice import *  # noqa: E402,F403

ARGS = [a for a in sys.argv[1:] if not a.startswith('--')]
ROOT = Path(ARGS[0]) if ARGS else Path(__file__).resolve().parents[2]
OUT = ROOT / 'src/main/resources/assets/tribalpower/textures/gui/codex'


def save(name, img):
    OUT.mkdir(parents=True, exist_ok=True)
    img.save(OUT / f'{name}.png')


# ---------------------------------------------------------------- crests

def shield_polygon(inset=0):
    """Heater shield on a 64 px canvas: flat top, straight shoulders, curving to a point at the bottom."""
    i = inset
    top, left, right, bottom = 4 + i, 8 + i, 55 - i, 60 - i
    pts = [(left, top), (right, top), (right, 30)]
    for t in range(1, 10):
        f = t / 10
        pts.append((round(right - (right - 31.5) * f * f), round(30 + (bottom - 30) * f)))
    pts.append((31, bottom)); pts.append((32, bottom))
    for t in range(9, 0, -1):
        f = t / 10
        pts.append((round(left + (31.5 - left) * f * f), round(30 + (bottom - 30) * f)))
    pts.append((left, 30))
    return pts


def crest(tribe):
    c = COLOURS[tribe] + (255,)
    dark, base, light = ramp(c, 0.3)
    deep = darken(c, 0.6)
    im = Image.new('RGBA', (64, 64), CLEAR); d = ImageDraw.Draw(im)
    d.polygon(shield_polygon(0), fill=INK)                     # outline
    d.polygon(shield_polygon(1), fill=BONE_SHADE)              # bone rim
    d.polygon(shield_polygon(2), fill=BONE_LIGHT)
    d.polygon(shield_polygon(3), fill=BONE)
    d.polygon(shield_polygon(5), fill=INK)
    d.polygon(shield_polygon(6), fill=base)                    # tribe field
    # dished shading: light along the top-left, dark down the right
    d.polygon([(14, 10), (49, 10), (49, 12), (16, 12), (16, 34), (14, 34)], fill=light)
    d.polygon([(47, 13), (49, 13), (49, 30), (41, 48), (39, 48), (47, 30)], fill=dark)
    # inner field (a smaller shield) carrying the glyph at 3 px cells (27 px), with a deep drop shadow
    d.polygon(shield_polygon(11), fill=INK)
    d.polygon(shield_polygon(12), fill=deep)
    draw_glyph(d, GLYPH_NAMES[tribe], 19, 21, BONE_LIGHT, scale=3, shadow=darken(c, 0.8))
    # copper chief with three rivets
    d.rectangle((14, 10, 49, 15), fill=COPPER); d.line((14, 10, 49, 10), fill=COPPER_LIGHT); d.line((14, 15, 49, 15), fill=COPPER_DARK)
    for x in (18, 32, 45):
        rivet(d, x, 13)
    return im.resize((128, 128), Image.NEAREST)


# ---------------------------------------------------------------- vignettes

SPINDLE = tuple(v / 255 for v in COLOURS['spindle'])
STONE_ID = 'tribalpower:block/march_stone'
COBBLE_ID = 'tribalpower:block/march_cobble'


def place(model, x, y, z, rot=0, tint=None):
    """One block in a vignette: model id, MC-axis block position, blockstate y rotation, tint for tintindex faces."""
    return dict(model=model, pos=(x, y, z), y=rot, tint=tint)


def drum_circle():
    """The Silent Drum on its stone at the centre of a ring of lantern pillars."""
    p = []
    for x in range(-2, 3):
        for z in range(-2, 3):
            p.append(place('tribalpower:block/march_grass', x, 0, z))
    p.append(place(STONE_ID, 0, 0, 0))
    p.append(place('tribalpower:block/silent_drum', 0, 1, 0))
    for (x, z) in ((0, -2), (2, -2), (-2, 0), (2, 0), (-2, 2), (0, 2), (2, 2)):  # the north-west gap faces the reader
        p.append(place(COBBLE_ID, x, 1, z))
        p.append(place('tribalpower:block/spirit_lantern', x, 2, z))
    p.append(place(COBBLE_ID, -2, 1, -2))  # a seating stone in the gap
    return p


def ancestor_hall():
    """A corner of the sunken hall: the far (south and east) walls are seen from inside."""
    p = []
    for x in range(0, 3):
        for z in range(0, 3):
            p.append(place(STONE_ID if (x + z) % 3 else COBBLE_ID, x, 0, z))
    for y in (1, 2):
        for x in range(0, 4):  # south wall (its north face is seen)
            p.append(place(COBBLE_ID if (x * 7 + y * 3) % 5 == 0 else STONE_ID, x, y, 3))
        for z in range(0, 3):  # east wall
            p.append(place(COBBLE_ID if (z * 5 + y * 2) % 4 == 0 else STONE_ID, 3, y, z))
    for x in range(0, 4):  # Loom-wood beams along the wall heads
        p.append(place('tribalpower:block/march_log', x, 3, 3))
    for z in range(0, 3):
        p.append(place('tribalpower:block/march_log', 3, 3, z))
    p.append(place('tribalpower:block/lore_tablet', 0, 1, 2))          # facing north, on the south wall
    p.append(place('tribalpower:block/lore_tablet', 1, 2, 2))
    p.append(place('tribalpower:block/lore_tablet', 2, 1, 0, rot=270))   # facing west, on the east wall
    p.append(place('tribalpower:block/ancestral_cache', 2, 1, 2))
    p.append(place('tribalpower:block/spirit_lantern', 0, 1, 0))
    return p


def crystal_spire():
    """The March Crystal spire with the Loom-stitcher waystation (hearth, banner, totems) at its foot."""
    p = []
    for x in range(-2, 3):
        for z in range(-2, 3):
            p.append(place('tribalpower:block/march_grass' if max(abs(x), abs(z)) == 2 else STONE_ID, x, 0, z))
    crystal = 'tribalpower:block/march_crystal'
    for x in (-1, 0, 1):
        for z in (-1, 0, 1):
            if abs(x) + abs(z) < 2:
                p.append(place(crystal, x, 1, z))
    for y in range(2, 5):
        p.append(place(crystal, 0, y, 0))
    p.append(place(crystal, 1, 1, 1)); p.append(place(crystal, -1, 2, 0)); p.append(place(crystal, 0, 2, 1))
    p.append(place('tribalpower:block/tribe_hearth', 0, 1, -2, tint=SPINDLE))
    p.append(place('tribalpower:block/tribe_banner_spindle', -2, 1, 0))
    p.append(place('tribalpower:block/resonance_totem_loom', 2, 1, -2))
    p.append(place('tribalpower:block/kinship_totem_spindle', -2, 1, 2))
    return p



def item(texture, x, y, z, standing=False, rot=0):
    """A flat item quad (`minecraft:item/bread`, `tribalpower:item/water_seal`) lying on, or standing at, a block position."""
    return dict(model=f'synthetic:item/{texture}/{int(standing)}', pos=(x, y, z), y=rot)


def tile(colour, x, y, z):
    """A solid-colour tile one pixel above the ground (ley grid cells)."""
    return dict(model=f'synthetic:tile/{colour}', pos=(x, y, z))


def floor(block, radius=1, y=0):
    return [place(block, x, y, z) for x in range(-radius, radius + 1) for z in range(-radius, radius + 1)]


SOIL = tuple(v / 255 for v in COLOURS['soil'])


def loom_sixth_voice():
    """The Loom totem and Echo Unweave on March planks, thread and echoes between them."""
    p = floor('tribalpower:block/march_planks')
    p.append(place('tribalpower:block/resonance_totem_loom', -1, 1, 1))
    p.append(place('tribalpower:block/echo_unweave', 1, 1, -1))
    p.append(place('tribalpower:block/spirit_lantern', 1, 1, 1))
    p.append(item('tribalpower:item/loom_thread', 0, 1, 0))
    p.append(item('tribalpower:item/manifested_ingot', -1, 1, -1))
    p.append(item('tribalpower:item/echo_shard', 0, 1, -1))
    return p


def rite_world_rites():
    """A Ritual Brazier holding the Water seal, the Rain Calling tablet beside it and the Water totem behind."""
    p = floor(STONE_ID)
    p.append(place('tribalpower:block/ritual_brazier', 0, 1, 0))
    p.append(item('tribalpower:item/water_seal', 0, 2.05, 0))
    p.append(item('tribalpower:item/rite_rain_calling', -1, 1, 1, standing=True, rot=315))
    p.append(place('tribalpower:block/resonance_totem_water', 1, 1, -1))
    p.append(place('tribalpower:block/spirit_lantern', -1, 1, -1))
    return p


def rite_land_rites():
    """Green Blessing: a brazier with the Earth seal over a wheat plot."""
    p = []
    for x in range(-2, 3):
        for z in range(-2, 3):
            crop = max(abs(x), abs(z)) == 2 or (x, z) in ((-1, 0), (1, 0), (0, -1), (0, 1))
            p.append(place('minecraft:block/farmland_moist' if crop else STONE_ID, x, 0, z))
            if crop and (x, z) != (0, 0):
                p.append(place('minecraft:block/wheat_stage7' if (x * 3 + z) % 4 else 'minecraft:block/wheat_stage5', x, 1, z))
    p.append(place('tribalpower:block/ritual_brazier', 0, 1, 0))
    p.append(item('tribalpower:item/earth_seal', 0, 2.05, 0))
    p.append(place('tribalpower:block/resonance_totem_earth', -2, 1, -2))
    return p


def camps_identity():
    """A shared camp: Deep Cache and Wayanchor with the charter and satchel laid out."""
    p = floor('tribalpower:block/march_planks')
    p.append(place('tribalpower:block/deep_cache', 0, 1, 0))
    p.append(place('tribalpower:block/wayanchor', 1, 1, -1))
    p.append(place('tribalpower:block/offering_table', -1, 1, 1))
    p.append(item('tribalpower:item/camp_charter', -1, 1, -1, standing=True, rot=315))
    p.append(item('tribalpower:item/wayfarer_satchel', 1, 1, 1))
    return p


def ley_lens():
    """The lens's 9-by-9 reading shown as a 5-by-5 ring of quiet blue and singing gold tiles around a collector."""
    p = floor('tribalpower:block/march_grass', 2)
    ramp_colours = ['3a6eb5', '5a86a0', '9a9a70', 'c9a040', 'e0a32d']
    for x in range(-2, 3):
        for z in range(-2, 3):
            if (x, z) == (0, 0):
                continue
            strength = (7 + x * 3 + z * 2 + (x * z) % 3) % 5
            p.append(tile(ramp_colours[strength], x, 1, z))
    p.append(place('tribalpower:block/ley_collector', 0, 1, 0))
    p.append(place('tribalpower:block/ley_thistle', -2, 1, 2))
    p.append(item('tribalpower:item/ley_lens', 2, 1.1, -2, standing=True, rot=315))
    return p


def pulse_logic():
    """A charged Drumheart read by a Pulse Gauge and a Pulse Threshold that lights a redstone lamp."""
    p = floor(STONE_ID)
    p.append(place('tribalpower:block/drumheart', 0, 1, 0))
    p.append(place('tribalpower:block/pulse_gauge', -1, 1, 0, rot=90))
    p.append(place('tribalpower:block/pulse_threshold', 1, 1, 0, rot=270))
    p.append(place('minecraft:block/redstone_lamp_on', 1, 1, 1))
    p.append(place('tribalpower:block/pulse_resonator_on', -1, 1, -1))
    return p


def codex_diagnostics():
    """The Codex held over an Echo Shatter station with its Earth totem: what diagnostics listen to."""
    p = floor(STONE_ID)
    p.append(place('tribalpower:block/echo_shatter', 0, 1, 0))
    p.append(place('tribalpower:block/resonance_totem_earth', 1, 1, -1))
    p.append(place('tribalpower:block/drumheart', -1, 1, 1))
    p.append(item('tribalpower:item/spirit_codex', 0, 2.3, 0, standing=True, rot=315))
    return p


def tribes_standing():
    """A Pad-keepers' hearth with offerings laid before it and the banner behind."""
    p = floor('tribalpower:block/march_grass')
    p.append(place('tribalpower:block/tribe_hearth', 0, 1, 0, tint=SOIL))
    p.append(place('tribalpower:block/tribe_banner_soil', 1, 1, -1))
    p.append(place('tribalpower:block/offering_table', -1, 1, 1))
    p.append(item('minecraft:item/bread', -1, 1, 0))
    p.append(item('tribalpower:item/echo_shard', 0, 1, 1))
    p.append(item('tribalpower:item/pulse_cell', 1, 1, 1))
    p.append(place('minecraft:block/moss_block', -1, 0, -1))
    return p


def tribes_kinship_totem():
    """All nine Kinship Totems on March stone, one per tribe."""
    p = floor(STONE_ID)
    for i, tribe in enumerate(TRIBES):
        p.append(place(f'tribalpower:block/kinship_totem_{tribe}', i % 3 - 1, 1, i // 3 - 1))
    return p


def lore_tablet_page():
    """An etched tablet set into a March stone wall, lit by a Spirit Lantern: the shared picture for every tablet page."""
    p = []
    for x in range(-1, 2):
        for y in range(0, 2):
            p.append(place(COBBLE_ID if (x * 5 + y * 3) % 4 == 0 else STONE_ID, x, y, 1))
        p.append(place(STONE_ID if x else COBBLE_ID, x, -1, 0))
        p.append(place('tribalpower:block/march_log', x, 2, 1))
    p.append(place('tribalpower:block/lore_tablet', 0, 1, 0))
    p.append(place('tribalpower:block/spirit_lantern', 1, 0, 0))
    return p


SCENES = {'drum_circle': drum_circle, 'ancestor_hall': ancestor_hall, 'crystal_spire': crystal_spire,
          'loom_sixth_voice': loom_sixth_voice, 'rite_world_rites': rite_world_rites, 'rite_land_rites': rite_land_rites,
          'camps_identity': camps_identity, 'ley_lens': ley_lens, 'pulse_logic': pulse_logic, 'codex_diagnostics': codex_diagnostics,
          'tribes_standing': tribes_standing, 'tribes_kinship_totem': tribes_kinship_totem, 'lore_tablet_page': lore_tablet_page}
PITCH = {'lore_tablet_page': 18, 'ley_lens': 42}
YAW = {'lore_tablet_page': -20}


def synthetic_models(cache, placements, rb):
    """Register the flat item quads and colour tiles a scene uses so render_scene finds them like real models."""
    for pl in placements:
        mid = pl['model']
        if mid in cache or not mid.startswith('synthetic:'):
            continue
        kind, rest = mid.split(':', 1)[1].split('/', 1)
        if kind == 'item':
            texture, standing = rest.rsplit('/', 1)
            cache[mid] = rb.item_model(texture, standing == '1')
        else:
            cache[mid] = rb.tile_model(rest)


def rain_streaks(page):
    """A few pale rain streaks over the world-rites scene."""
    d = ImageDraw.Draw(page)
    for i, (x, y) in enumerate(((14, 8), (30, 16), (48, 6), (70, 12), (96, 9), (112, 20), (22, 40), (104, 44))):
        d.line((x, y, x - 2, y + 7 + i % 3), fill=(150, 200, 255, 200), width=1)
    return page


def green_sparkles(page):
    d = ImageDraw.Draw(page)
    for i, (x, y) in enumerate(((16, 30), (38, 18), (62, 12), (88, 20), (108, 34), (24, 70), (102, 72), (56, 46), (78, 58))):
        d.rectangle((x, y, x + 1, y + 1), fill=(168, 240, 160, 230))
        if i % 2:
            d.point((x + 3, y - 2), fill=(120, 200, 110, 200))
    return page


POST = {'rite_world_rites': rain_streaks, 'rite_land_rites': green_sparkles}


def render_vignettes(only=None):
    import render_blocks as rb
    bpy, sc = rb.blender_scene()
    sc.cycles.samples = 24
    sc.render.filter_size = 1.2
    scratch = Path(tempfile.mkdtemp(prefix='codex_'))
    cache, mats = {}, {}
    for name, builder in SCENES.items():
        if only and name not in only:
            continue
        placements = builder()
        synthetic_models(cache, placements, rb)
        out = scratch / f'{name}.png'
        rb.render_scene(bpy, sc, placements, out, scratch, size=512, pitch_deg=PITCH.get(name, 36), yaw_deg=YAW.get(name, -45), cache=cache, mat_cache=mats)
        im = Image.open(out).convert('RGBA')
        im = im.crop(im.getbbox())  # fill the page: crop to the drawn pixels, fit into 128 with a small margin
        fit = 120 / max(im.size)
        im = im.resize((max(1, round(im.width * fit)), max(1, round(im.height * fit))), Image.LANCZOS)
        page = Image.new('RGBA', (128, 128), CLEAR)
        page.paste(im, ((128 - im.width) // 2, (128 - im.height) // 2), im)
        if name in POST:
            page = POST[name](page)
        save(name, page)
        print('codex vignette', name)


def main():
    for tribe in TRIBES:
        save(f'tribe_{tribe}', crest(tribe))
    print('codex crests written to', OUT)
    only = [a.split('=', 1)[1].split(',') for a in sys.argv if a.startswith('--only=')]
    if '--crests-only' not in sys.argv:
        render_vignettes(only[0] if only else None)


if __name__ == '__main__':
    main()
