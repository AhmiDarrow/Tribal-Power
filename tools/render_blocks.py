"""Render the mod's block models with Blender the way the inventory shows them, into labelled contact sheets.

Parses blockstate -> model JSON (parents, native `elements` with from/to/faces/uv/rotation, `#slot` texture
references, `tribalpower:block/*` and `minecraft:block/*` textures - vanilla textures fall back to a flat colour),
builds one Blender mesh per model with the real PNG textures (Closest interpolation, cutout alpha), shades it like
the in-game isometric GUI view (up 1.0, north 0.8, west 0.6) and composes:

  art/blocks-3.0-contact-sheet.png   every block whose blockstate was added/changed since --since (default v2.3.0)
  art/items-3.0-contact-sheet.png    every item texture added since --since, at 4x nearest

Run: python3 tools/render_blocks.py [--since 4432c3e] [--all] [--only name,name] [--out-dir art]
Needs Blender as a Python module (`import bpy`, 4.2, CPU Cycles) and Pillow.
"""
from __future__ import annotations

import argparse
import json
import math
import subprocess
import sys
import tempfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets'
MODID = 'tribalpower'
FONT = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
SHADE = {'up': 1.0, 'down': 0.5, 'north': 0.8, 'south': 0.8, 'east': 0.6, 'west': 0.6}
# Faces with tintindex take this colour (spark tribe amber for the hearth embers).
TINT = (0xe0 / 255, 0xa3 / 255, 0x2d / 255)

# Minimal vanilla parents so `minecraft:block/*` chains resolve without the client jar.
FULL = {'from': [0, 0, 0], 'to': [16, 16, 16]}
BUILTIN = {
    'minecraft:block/block': {},
    'minecraft:block/cube': {'parent': 'minecraft:block/block', 'elements': [dict(FULL, faces={
        f: {'texture': '#' + f, 'cullface': f} for f in ('down', 'up', 'north', 'south', 'west', 'east')})]},
    'minecraft:block/cube_all': {'parent': 'minecraft:block/cube', 'textures': {
        'particle': '#all', 'down': '#all', 'up': '#all', 'north': '#all', 'south': '#all', 'west': '#all', 'east': '#all'}},
    'minecraft:block/cube_bottom_top': {'parent': 'minecraft:block/cube', 'textures': {
        'particle': '#side', 'down': '#bottom', 'up': '#top', 'north': '#side', 'south': '#side', 'west': '#side', 'east': '#side'}},
    'minecraft:block/cube_top': {'parent': 'minecraft:block/cube', 'textures': {
        'particle': '#side', 'down': '#side', 'up': '#top', 'north': '#side', 'south': '#side', 'west': '#side', 'east': '#side'}},
    'minecraft:block/cube_column': {'parent': 'minecraft:block/cube', 'textures': {
        'particle': '#side', 'down': '#end', 'up': '#end', 'north': '#side', 'south': '#side', 'west': '#side', 'east': '#side'}},
    'minecraft:block/orientable': {'parent': 'minecraft:block/cube', 'textures': {
        'particle': '#front', 'down': '#top', 'up': '#top', 'north': '#front', 'south': '#side', 'west': '#side', 'east': '#side'}},
    'minecraft:block/cross': {'parent': 'minecraft:block/block', 'textures': {'particle': '#cross'}, 'elements': [
        {'from': [0.8, 0, 8], 'to': [15.2, 16, 8], 'rotation': {'origin': [8, 8, 8], 'axis': 'y', 'angle': 45, 'rescale': True},
         'shade': False, 'faces': {'north': {'uv': [0, 0, 16, 16], 'texture': '#cross'}, 'south': {'uv': [0, 0, 16, 16], 'texture': '#cross'}}},
        {'from': [8, 0, 0.8], 'to': [8, 16, 15.2], 'rotation': {'origin': [8, 8, 8], 'axis': 'y', 'angle': 45, 'rescale': True},
         'shade': False, 'faces': {'west': {'uv': [0, 0, 16, 16], 'texture': '#cross'}, 'east': {'uv': [0, 0, 16, 16], 'texture': '#cross'}}}]},
    'minecraft:block/tinted_cross': {'parent': 'minecraft:block/cross'},
}
VANILLA_COLOURS = {'stone': (125, 125, 125), 'cobblestone': (110, 110, 110), 'oak_planks': (162, 130, 78), 'spruce_planks': (114, 84, 48),
                   'dirt': (134, 96, 67), 'glass': (200, 230, 240), 'copper_block': (192, 110, 80), 'iron_block': (220, 220, 220)}


# ---------------------------------------------------------------- model resolution

def load_model(mid: str, cache: dict) -> dict:
    """Return the fully merged model (textures + elements + display) for a resource id like `tribalpower:block/x`."""
    if mid in cache:
        return cache[mid]
    if ':' not in mid:
        mid = 'minecraft:' + mid
    ns, path = mid.split(':', 1)
    file = ASSETS / ns / 'models' / (path + '.json')
    if file.exists():
        raw = json.loads(file.read_text(encoding='utf-8'))
    elif mid in BUILTIN:
        raw = BUILTIN[mid]
    else:
        print(f'  [warn] unknown model {mid}, treating as empty', file=sys.stderr)
        raw = {}
    merged = {'textures': {}, 'elements': [], 'display': {}}
    if raw.get('parent'):
        parent = load_model(raw['parent'], cache)
        merged['textures'] = dict(parent['textures'])
        merged['elements'] = list(parent['elements'])
        merged['display'] = dict(parent['display'])
    merged['textures'].update(raw.get('textures', {}))
    if 'elements' in raw:
        merged['elements'] = raw['elements']
    merged['display'].update(raw.get('display', {}))
    cache[mid] = merged
    return merged


def resolve_texture(ref: str, textures: dict) -> str | None:
    for _ in range(12):
        if not ref.startswith('#'):
            return ref
        ref = textures.get(ref[1:], '')
        if not ref:
            return None
    return None


def texture_file(tex: str, scratch: Path) -> Path:
    """PNG for a texture id; animated strips are cropped to their first frame, vanilla ids get a flat colour."""
    if ':' not in tex:
        tex = 'minecraft:' + tex
    ns, path = tex.split(':', 1)
    file = ASSETS / ns / 'textures' / (path + '.png')
    out = scratch / (ns + '_' + path.replace('/', '_') + '.png')
    if out.exists():
        return out
    if file.exists():
        im = Image.open(file).convert('RGBA')
        if im.height > im.width:  # animation strip: first frame
            im = im.crop((0, 0, im.width, im.width))
        im.save(out)
    else:
        name = path.rsplit('/', 1)[-1]
        colour = VANILLA_COLOURS.get(name)
        if colour is None:
            h = sum(ord(c) * (i + 3) for i, c in enumerate(name))
            colour = (90 + h % 90, 90 + (h // 7) % 90, 90 + (h // 49) % 90)
        im = Image.new('RGBA', (16, 16), colour + (255,))
        ImageDraw.Draw(im).rectangle((0, 0, 15, 15), outline=tuple(max(0, c - 40) for c in colour) + (255,))
        im.save(out)
    return out


# ---------------------------------------------------------------- geometry

def default_uv(face: str, f, t):
    fx, fy, fz = f; tx, ty, tz = t
    return {
        'down': [fx, 16 - tz, tx, 16 - fz], 'up': [fx, fz, tx, tz],
        'north': [16 - tx, 16 - ty, 16 - fx, 16 - fy], 'south': [fx, 16 - ty, tx, 16 - fy],
        'west': [fz, 16 - ty, tz, 16 - fy], 'east': [16 - tz, 16 - ty, 16 - fz, 16 - fy],
    }[face]


def face_corners(face: str, f, t):
    """Four MC-space corners in the order top-left, top-right, bottom-right, bottom-left as seen from outside."""
    fx, fy, fz = f; tx, ty, tz = t
    return {
        'north': [(tx, ty, fz), (fx, ty, fz), (fx, fy, fz), (tx, fy, fz)],
        'south': [(fx, ty, tz), (tx, ty, tz), (tx, fy, tz), (fx, fy, tz)],
        'west': [(fx, ty, fz), (fx, ty, tz), (fx, fy, tz), (fx, fy, fz)],
        'east': [(tx, ty, tz), (tx, ty, fz), (tx, fy, fz), (tx, fy, tz)],
        'up': [(fx, ty, fz), (tx, ty, fz), (tx, ty, tz), (fx, ty, tz)],
        'down': [(fx, fy, tz), (tx, fy, tz), (tx, fy, fz), (fx, fy, fz)],
    }[face]


def rotate_point(p, origin, axis, angle_deg, rescale):
    a = math.radians(angle_deg)
    c, s = math.cos(a), math.sin(a)
    x, y, z = (p[i] - origin[i] for i in range(3))
    if axis == 'x':
        y, z = y * c - z * s, y * s + z * c
    elif axis == 'y':
        x, z = x * c + z * s, -x * s + z * c
    else:
        x, y = x * c - y * s, x * s + y * c
    if rescale and abs(angle_deg) in (22.5, 45):
        k = 1 / math.cos(a)
        if axis == 'x':
            y, z = y * k, z * k
        elif axis == 'y':
            x, z = x * k, z * k
        else:
            x, y = x * k, y * k
    return (x + origin[0], y + origin[1], z + origin[2])


def rotate_about_centre(p, rx, ry):
    """Blockstate x/y rotation (degrees, clockwise as in vanilla) around the block centre."""
    q = p
    if rx:
        q = rotate_point(q, (8, 8, 8), 'x', -rx, False)
    if ry:
        q = rotate_point(q, (8, 8, 8), 'y', -ry, False)
    return q


def mc_to_blender(p, offset=(0, 0, 0)):
    return (p[0] / 16.0 + offset[0], 1 - p[2] / 16.0 - offset[2], p[1] / 16.0 + offset[1])


def model_faces(model: dict, rx=0, ry=0, offset=(0, 0, 0)):
    """Yield (texture_id, corners_blender[4], uvs[4], shade, tint) for every face of the model, `offset` in blocks."""
    textures = model['textures']
    for el in model['elements']:
        f, t = el['from'], el['to']
        rot = el.get('rotation')
        for face, spec in el.get('faces', {}).items():
            tex = resolve_texture(spec.get('texture', ''), textures)
            if not tex:
                continue
            uv = spec.get('uv') or default_uv(face, f, t)
            u1, v1, u2, v2 = uv
            uvs = [(u1, v1), (u2, v1), (u2, v2), (u1, v2)]
            r = int(spec.get('rotation', 0)) // 90 % 4
            uvs = uvs[r:] + uvs[:r]
            corners = face_corners(face, f, t)
            if rot:
                corners = [rotate_point(c, rot['origin'], rot['axis'], rot['angle'], rot.get('rescale', False)) for c in corners]
            corners = [rotate_about_centre(c, rx, ry) for c in corners]
            shade = SHADE[face] if el.get('shade', True) else 1.0
            if rx or ry:  # re-derive the lit direction from the rotated normal
                shade = shade_for(corners)
            yield tex, [mc_to_blender(c, offset) for c in corners], [(u / 16.0, 1 - v / 16.0) for u, v in uvs], shade, 'tintindex' in spec


def shade_for(corners):
    ax, ay, az = corners[0]; bx, by, bz = corners[1]; cx, cy, cz = corners[3]
    ux, uy, uz = bx - ax, by - ay, bz - az
    vx, vy, vz = cx - ax, cy - ay, cz - az
    nx, ny, nz = uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx  # MC-space normal (y up)
    n = max(1e-6, math.sqrt(nx * nx + ny * ny + nz * nz))
    nx, ny, nz = nx / n, ny / n, nz / n
    # corners are TL, TR, BR, BL from outside: TL->TR cross TL->BL points inward, flip
    nx, ny, nz = -nx, -ny, -nz
    if abs(ny) > 0.7:
        return 1.0 if ny > 0 else 0.5
    return 0.8 if abs(nz) >= abs(nx) else 0.6


# ---------------------------------------------------------------- blender

def aim_camera(sc, centre=(0.5, 0.5, 0.5), ortho_scale=1.62, yaw_deg=-45, pitch_deg=30):
    """Isometric GUI view: from the north-west (yaw -45), 30 degrees above the horizon, looking at `centre`."""
    from mathutils import Vector
    cam = sc.camera
    cam.data.ortho_scale = ortho_scale
    d = 20.0
    yaw, pitch = math.radians(yaw_deg), math.radians(pitch_deg)
    cam.location = (centre[0] + d * math.cos(pitch) * math.sin(yaw), centre[1] + d * math.cos(pitch) * math.cos(yaw), centre[2] + d * math.sin(pitch))
    cam.rotation_euler = (Vector(centre) - Vector(cam.location)).to_track_quat('-Z', 'Y').to_euler()
    cam.data.clip_end = 100


def render_scene(bpy, sc, placements, out: Path, scratch: Path, size=128, pad=1.15, yaw_deg=-45, pitch_deg=30, cache=None, mat_cache=None):
    """Render several block models placed in one scene (a Codex vignette).

    placements: iterable of dicts {model: 'tribalpower:block/x', pos: (x, y, z) in blocks (MC axes), x/y: blockstate
    rotation, tint: (r, g, b) for tintindex faces}. The camera frames the union of the placed blocks."""
    cache = {} if cache is None else cache
    mat_cache = {} if mat_cache is None else mat_cache
    objs = []
    lo = [1e9] * 3; hi = [-1e9] * 3
    for i, pl in enumerate(placements):
        model = load_model(pl['model'], cache)
        faces = list(model_faces(model, pl.get('x', 0), pl.get('y', 0), pl['pos']))
        if not faces:
            continue
        objs.append(build_object(bpy, sc, f'scene_{i}', faces, scratch, mat_cache, tuple(pl['tint']) if pl.get('tint') else TINT))
        for _, corners, _, _, _ in faces:
            for c in corners:
                for k in range(3):
                    lo[k] = min(lo[k], c[k]); hi[k] = max(hi[k], c[k])
    centre = tuple((lo[k] + hi[k]) / 2 for k in range(3))
    span = max(hi[k] - lo[k] for k in range(3))
    # projected footprint of an iso cube is about 1.4-1.7x its edge
    aim_camera(sc, centre, ortho_scale=span * 1.45 * pad, yaw_deg=yaw_deg, pitch_deg=pitch_deg)
    sc.render.resolution_x = sc.render.resolution_y = size
    sc.render.filepath = str(out)
    bpy.ops.render.render(write_still=True)
    for o in objs:
        bpy.data.objects.remove(o, do_unlink=True)
    return out


def blender_scene():
    import bpy
    bpy.ops.wm.read_factory_settings(use_empty=True)
    sc = bpy.context.scene
    sc.render.engine = 'CYCLES'
    sc.cycles.samples = 8
    sc.cycles.use_denoising = False
    sc.cycles.device = 'CPU'
    sc.render.film_transparent = True
    sc.render.resolution_x = sc.render.resolution_y = 160
    sc.render.resolution_percentage = 100
    sc.render.image_settings.color_mode = 'RGBA'
    sc.render.dither_intensity = 0
    sc.view_settings.view_transform = 'Standard'
    sc.render.filter_size = 0.01  # crisp texels
    cam_data = bpy.data.cameras.new('cam')
    cam_data.type = 'ORTHO'
    cam = bpy.data.objects.new('cam', cam_data)
    sc.collection.objects.link(cam)
    sc.camera = cam
    aim_camera(sc)
    return bpy, sc


def material_for(bpy, tex_path: Path, shade: float, tint, cache: dict):
    key = (str(tex_path), shade, tint)
    if key in cache:
        return cache[key]
    mat = bpy.data.materials.new(f'{tex_path.stem}_{shade}_{int(bool(tint))}')
    mat.use_nodes = True
    nt = mat.node_tree
    for n in list(nt.nodes):
        nt.nodes.remove(n)
    out = nt.nodes.new('ShaderNodeOutputMaterial')
    img = nt.nodes.new('ShaderNodeTexImage')
    images = cache.setdefault('__images__', {})
    if str(tex_path) not in images:
        image = bpy.data.images.load(str(tex_path))
        image.colorspace_settings.name = 'sRGB'
        images[str(tex_path)] = image
    img.image = images[str(tex_path)]
    img.interpolation = 'Closest'
    img.extension = 'REPEAT'
    mul = nt.nodes.new('ShaderNodeMixRGB')
    mul.blend_type = 'MULTIPLY'
    mul.inputs['Fac'].default_value = 1.0
    colour = tuple(c * shade for c in (tint or (1, 1, 1)))
    mul.inputs['Color2'].default_value = (*colour, 1)
    emit = nt.nodes.new('ShaderNodeEmission')
    emit.inputs['Strength'].default_value = 1.0
    trans = nt.nodes.new('ShaderNodeBsdfTransparent')
    step = nt.nodes.new('ShaderNodeMath')
    step.operation = 'GREATER_THAN'
    step.inputs[1].default_value = 0.5
    mix = nt.nodes.new('ShaderNodeMixShader')
    nt.links.new(img.outputs['Color'], mul.inputs['Color1'])
    nt.links.new(mul.outputs['Color'], emit.inputs['Color'])
    nt.links.new(img.outputs['Alpha'], step.inputs[0])
    nt.links.new(step.outputs[0], mix.inputs['Fac'])
    nt.links.new(trans.outputs[0], mix.inputs[1])
    nt.links.new(emit.outputs[0], mix.inputs[2])
    nt.links.new(mix.outputs[0], out.inputs['Surface'])
    mat.blend_method = 'CLIP'
    mat.use_backface_culling = False
    cache[key] = mat
    return mat


def build_object(bpy, sc, name: str, faces, scratch: Path, mat_cache: dict, tint=TINT):
    verts, polys, uv_data, mat_index = [], [], [], []
    mats = []
    for tex, corners, uvs, shade, tinted in faces:
        mat = material_for(bpy, texture_file(tex, scratch), shade, tint if tinted else None, mat_cache)
        if mat not in mats:
            mats.append(mat)
        base = len(verts)
        verts.extend(corners)
        polys.append((base, base + 1, base + 2, base + 3))
        uv_data.extend(uvs)
        mat_index.append(mats.index(mat))
    mesh = bpy.data.meshes.new(name)
    mesh.from_pydata(verts, [], polys)
    uv_layer = mesh.uv_layers.new(name='UVMap')
    for i, uv in enumerate(uv_data):
        uv_layer.data[i].uv = uv
    for m in mats:
        mesh.materials.append(m)
    for p, mi in zip(mesh.polygons, mat_index):
        p.material_index = mi
    obj = bpy.data.objects.new(name, mesh)
    sc.collection.objects.link(obj)
    return obj


# ---------------------------------------------------------------- selection

def changed_files(since: str, sub: str) -> list[str]:
    try:
        out = subprocess.run(['git', 'diff', '--name-only', since, 'HEAD', '--', f'src/main/resources/assets/{MODID}/{sub}'],
                             cwd=ROOT, capture_output=True, text=True, check=True).stdout.split()
        untracked = subprocess.run(['git', 'ls-files', '--others', '--exclude-standard', f'src/main/resources/assets/{MODID}/{sub}'],
                                   cwd=ROOT, capture_output=True, text=True, check=True).stdout.split()
    except subprocess.CalledProcessError as e:
        sys.exit(f'git failed: {e.stderr}')
    names = [Path(p).stem for p in out + untracked if p.endswith('.png') or p.endswith('.json')]
    return sorted(dict.fromkeys(names))


def blockstate_models(name: str) -> list[tuple[str, str, int, int]]:
    """(label, model id, x, y) for every distinct model a blockstate references."""
    bs = json.loads((ASSETS / MODID / 'blockstates' / (name + '.json')).read_text(encoding='utf-8'))
    seen, result = set(), []
    entries = []
    if 'variants' in bs:
        for key, v in bs['variants'].items():
            entries.append((key, v))
    else:
        for part in bs.get('multipart', []):
            entries.append(('multipart', part['apply']))
    for key, v in entries:
        if isinstance(v, list):
            v = v[0]
        mid = v['model']
        if mid in seen:
            continue
        seen.add(mid)
        short = mid.split('/')[-1]
        label = name if short == name else short.replace(name + '_', name + ':')
        result.append((label, mid, 0, 0))
    return result


# ---------------------------------------------------------------- sheets

def compose_sheet(cells: list[tuple[str, Image.Image]], out: Path, cell=(160, 160), cols=8, title=''):
    font = ImageFont.truetype(FONT, 11)
    tfont = ImageFont.truetype(FONT, 14)
    cw, ch = cell[0] + 8, cell[1] + 26
    rows = max(1, math.ceil(len(cells) / cols))
    top = 28 if title else 6
    sheet = Image.new('RGBA', (cols * cw + 8, rows * ch + top + 6), (28, 32, 38, 255))
    d = ImageDraw.Draw(sheet)
    if title:
        d.text((8, 6), title, fill=(200, 210, 220), font=tfont)
    for i, (label, im) in enumerate(cells):
        x = 8 + (i % cols) * cw
        y = top + (i // cols) * ch
        d.rectangle((x, y, x + cell[0] - 1, y + cell[1] - 1), fill=(38, 44, 52), outline=(52, 60, 70))
        im = im.convert('RGBA')
        if im.size != cell:
            im.thumbnail(cell, Image.NEAREST)
        sheet.paste(im, (x + (cell[0] - im.width) // 2, y + (cell[1] - im.height) // 2), im)
        d.text((x + 2, y + cell[1] + 2), label[:26], fill=(220, 224, 230), font=font)
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    print('wrote', out)


def render_blocks(names: list[str], out: Path, scratch: Path, title: str):
    bpy, sc = blender_scene()
    cache, mat_cache = {}, {}
    cells = []
    for name in names:
        for label, mid, rx, ry in blockstate_models(name):
            model = load_model(mid, cache)
            faces = list(model_faces(model, rx, ry))
            if not faces:
                print(f'  {label}: no faces (invisible block), skipped')
                continue
            obj = build_object(bpy, sc, label, faces, scratch, mat_cache)
            sc.render.filepath = str(scratch / f'render_{label.replace(":", "_")}.png')
            bpy.ops.render.render(write_still=True)
            bpy.data.objects.remove(obj, do_unlink=True)
            cells.append((label, Image.open(sc.render.filepath)))
            print(f'  rendered {label} ({len(faces)} faces)')
    compose_sheet(cells, out, title=title)


def render_items(names: list[str], out: Path, title: str):
    cells = []
    for name in names:
        file = ASSETS / MODID / 'textures/item' / (name + '.png')
        if not file.exists():
            continue
        im = Image.open(file).convert('RGBA')
        if im.height > im.width:
            im = im.crop((0, 0, im.width, im.width))
        model = ASSETS / MODID / 'models/item' / (name + '.json')
        if model.exists():  # layered item models: composite the extra layers (layer1+ tinted like the hearth embers)
            layers = json.loads(model.read_text(encoding='utf-8')).get('textures', {})
            for i in range(1, 4):
                ref = layers.get(f'layer{i}', '')
                if ref.startswith(MODID + ':'):
                    layer = Image.open(ASSETS / MODID / 'textures' / (ref.split(':', 1)[1] + '.png')).convert('RGBA')
                    from PIL import ImageChops
                    tint = Image.new('RGB', layer.size, tuple(int(c * 255) for c in TINT))
                    layer = Image.merge('RGBA', (*ImageChops.multiply(layer.convert('RGB'), tint).split(), layer.split()[3]))
                    im.alpha_composite(layer)
        im = im.resize((im.width * 4, im.height * 4), Image.NEAREST)
        cells.append((name, im))
    compose_sheet(cells, out, cell=(136, 136), cols=9, title=title)


def main():
    ap = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    ap.add_argument('--since', default='4432c3e', help='git revision the sheets are diffed against (default: v2.3.0)')
    ap.add_argument('--all', action='store_true', help='render every blockstate / item texture, not only the new ones')
    ap.add_argument('--only', default='', help='comma-separated blockstate names to render')
    ap.add_argument('--out-dir', default=str(ROOT / 'art'))
    ap.add_argument('--tag', default='3.0', help='name fragment of the output files')
    ap.add_argument('--no-items', action='store_true')
    args = ap.parse_args()
    out_dir = Path(args.out_dir)
    scratch = Path(tempfile.mkdtemp(prefix='render_blocks_'))
    if args.only:
        blocks = args.only.split(',')
    elif args.all:
        blocks = sorted(p.stem for p in (ASSETS / MODID / 'blockstates').glob('*.json'))
    else:
        blocks = changed_files(args.since, 'blockstates')
    print(f'blocks: {len(blocks)}')
    render_blocks(blocks, out_dir / f'blocks-{args.tag}-contact-sheet.png', scratch, f'Tribal Power {args.tag} blocks (GUI view)')
    if not args.no_items:
        if args.all:
            items = sorted(p.stem for p in (ASSETS / MODID / 'textures/item').glob('*.png'))
        else:
            items = [n for n in changed_files(args.since, 'textures/item')]
        print(f'items: {len(items)}')
        render_items(items, out_dir / f'items-{args.tag}-contact-sheet.png', f'Tribal Power {args.tag} items (4x)')


if __name__ == '__main__':
    main()
