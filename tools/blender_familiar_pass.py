"""Render the bonded-familiar portraits for the Spirit Codex from art/creatures/tribal_bestiary.blend.

The three gentle animals are rendered with their bonded collar overlay (textures/entity/bonded_collar_<id>.png tinted
the way client/BondedCollarLayer draws it) composited onto their skin, using the same framing as
tools/blender_icon_pass.py. Outputs 128x128 RGBA in textures/gui/codex/:
  familiar_lantern_fox.png, familiar_mossback.png, familiar_dawn_stag.png   one bonded animal each
  familiar_mossback_stag.png                                                 the two mounts side by side
  familiars_bonding.png                                                      all three bonded animals
Run after tools/blender_bestiary.py: python3 tools/blender_familiar_pass.py
"""
import bpy, json
from pathlib import Path
from mathutils import Vector
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / 'src/main/resources/assets/tribalpower/textures/entity'
CODEX = ROOT / 'src/main/resources/assets/tribalpower/textures/gui/codex'
PREVIEW = ROOT / 'art/creatures/preview'
COLLAR_TINT = (0x7E, 0xFF, 0xCB)  # BondedCollarLayer.TINT
FAMILIARS = ['lantern_fox', 'mossback', 'dawn_stag']

bpy.ops.wm.open_mainfile(filepath=str(ROOT / 'art/creatures/tribal_bestiary.blend'))
rows = {r['id']: r for r in json.loads((ROOT / 'art/creatures/roster.json').read_text(encoding='utf-8'))}
scene = bpy.context.scene
scene.render.resolution_x = scene.render.resolution_y = 512  # downscaled after the crop, so the 128 page stays crisp
scene.render.film_transparent = True
scene.cycles.samples = 16
bpy.data.objects['Studio floor'].hide_render = True
for obj in bpy.data.objects:
    if obj.type == 'FONT':
        obj.hide_render = True
collections = [c for c in bpy.data.collections if not c.name.startswith('Label')]


def height(r):
    top = min(p['pivot'][1] + b['box'][1] for p in r['parts'] for b in p['boxes'])
    return (24 - top) / 16 * r.get('scale', 1)


def bonded_atlas(creature):
    """The creature skin with its tinted collar overlay composited on top, saved beside the other previews."""
    base = Image.open(TEXTURES / f'{creature}.png').convert('RGBA')
    collar = Image.open(TEXTURES / f'bonded_collar_{creature}.png').convert('RGBA')
    ch = collar.split()
    collar = Image.merge('RGBA', tuple(ch[i].point(lambda v, k=COLLAR_TINT[i]: v * k // 255) for i in range(3)) + (ch[3],))
    base.alpha_composite(collar)
    PREVIEW.mkdir(parents=True, exist_ok=True)
    out = PREVIEW / f'{creature}_bonded.png'
    base.save(out)
    return out


def swap_skin(creature, atlas):
    """Point the creature's base-colour image node at the bonded atlas."""
    mat = bpy.data.materials[rows[creature]['name']]
    for node in mat.node_tree.nodes:
        if node.type == 'TEX_IMAGE' and node.image and Path(node.image.filepath).name == f'{creature}.png':
            node.image = bpy.data.images.load(str(atlas))
            node.image.colorspace_settings.name = 'sRGB'
            node.interpolation = 'Closest'
            return
    raise SystemExit(f'no skin image node for {creature}')


def render(creature, path):
    r = rows[creature]
    origin = bpy.data.objects[creature]
    for c in collections:
        c.hide_render = origin.name not in c.objects
    loc = origin.location
    s = max(1.0, height(r) / 2.0)
    scene.camera.location = loc + Vector((1.8, 4, 1.9)) * s
    scene.camera.rotation_euler = (loc + Vector((0, 0, .9 * s)) - scene.camera.location).to_track_quat('-Z', 'Y').to_euler()
    scene.camera.data.ortho_scale = 2.6 * s
    scene.render.filepath = str(path)
    bpy.ops.render.render(write_still=True)
    # Codex pages are larger than bestiary icons: fill the page with the animal, keeping a small margin.
    im = Image.open(path).convert('RGBA')
    im = im.crop(im.getbbox())
    fit = 112 / max(im.size)
    im = im.resize((max(1, round(im.width * fit)), max(1, round(im.height * fit))), Image.LANCZOS)
    page = Image.new('RGBA', (128, 128), (0, 0, 0, 0))
    page.paste(im, ((128 - im.width) // 2, (128 - im.height) // 2), im)
    page.save(path)


def compose(names, out, size):
    """Several portraits shrunk and overlapped on one 128x128 page, later ones in front."""
    page = Image.new('RGBA', (128, 128), (0, 0, 0, 0))
    step = (128 - size) // max(1, len(names) - 1) if len(names) > 1 else 0
    for i, name in enumerate(names):
        im = Image.open(CODEX / f'familiar_{name}.png').convert('RGBA')
        im = im.crop(im.getbbox())
        im.thumbnail((size, size), Image.LANCZOS)
        page.alpha_composite(im, (i * step + (size - im.width) // 2, 128 - im.height - (len(names) - 1 - i) * 6))
    page.save(out)


CODEX.mkdir(parents=True, exist_ok=True)
for creature in FAMILIARS:
    swap_skin(creature, bonded_atlas(creature))
    render(creature, CODEX / f'familiar_{creature}.png')
    print('rendered bonded', creature)
compose(['dawn_stag', 'mossback'], CODEX / 'familiar_mossback_stag.png', 92)
compose(['dawn_stag', 'mossback', 'lantern_fox'], CODEX / 'familiars_bonding.png', 80)
print('Rendered', len(FAMILIARS), 'bonded portraits and two group pages')
