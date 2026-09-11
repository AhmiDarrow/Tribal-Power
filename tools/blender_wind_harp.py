"""Wind Harp Blender rig (design 3.1 §15).

The shipped block is the Living Lattice cuboid: plinth, forepillar, neck, soundbox, five strings,
textured with the 32px loom atlas. This file is the source of those boxes. Running it writes the
in-game JSON, then (if Blender is present) a .blend whose cubes wear the same pixel textures.

    python tools/blender_wind_harp.py
    blender --background --python tools/blender_wind_harp.py
"""
from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/tribalpower"
BLEND = ROOT / "art" / "blocks" / "wind_harp.blend"
STILL = ROOT / "art" / "blocks" / "wind_harp.png"
EXPORT = ROOT / "art" / "blocks" / "wind_harp-export.json"
NS = "tribalpower"

# Minecraft from/to in 0–16 space. Texture ids match generate_3_1_assets cuboid slots.
# (name, from, to, texture, up_texture_or_None)
CUBOIDS = [
    ("plinth", [2, 0, 5], [14, 2, 11], "stone", None),
    ("soundbox", [10, 2, 5], [14, 14, 11], "side", None),
    ("soundbox_crown", [10, 14, 5], [14, 15, 11], "trim", "top"),
    ("forepillar", [2, 2, 7], [4, 15, 9], "wood", None),
    ("neck", [2, 15, 7], [12, 16, 9], "wood", None),
    ("rail", [4, 2, 7], [10, 3.5, 9], "trim", None),
    ("back", [5, 4, 9], [10, 13, 10.5], "wood", None),
]
STRINGS = [("string_%d" % (i + 1), x, 3.6 + i * 0.5, 15.2) for i, x in enumerate((4.6, 5.8, 7.0, 8.2, 9.4))]
TEX_FILES = {
    "side": "textures/block/wind_harp.png",
    "top": "textures/block/wind_harp_top.png",
    "wood": "textures/block/loom_wood.png",
    "stone": "textures/block/loom_stone.png",
    "trim": "textures/block/loom_copper.png",
    "light": "textures/block/loom_light.png",
}


def _num(n):
    v = round(float(n), 2)
    return int(v) if v == int(v) else v


def minecraft_model(lit=True):
    """The shipped vanilla cuboid. Identical boxes whether Blender is installed or not."""
    textures = {
        "particle": f"{NS}:block/wind_harp",
        "side": f"{NS}:block/wind_harp",
        "top": f"{NS}:block/wind_harp_top",
        "wood": f"{NS}:block/loom_wood",
        "stone": f"{NS}:block/loom_stone",
        "trim": f"{NS}:block/loom_copper",
        "light": f"{NS}:block/loom_light" if lit else f"{NS}:block/loom_stone",
    }
    parts = []

    def faces(tex, up=None):
        out = {f: {"texture": "#" + tex} for f in ("north", "south", "east", "west", "down")}
        out["up"] = {"texture": "#" + (up or tex)}
        return out

    for _name, a, b, tex, up in CUBOIDS:
        parts.append({"from": [_num(n) for n in a], "to": [_num(n) for n in b], "faces": faces(tex, up)})
    for _name, x, y0, _y1 in STRINGS:
        a = [x, y0, 7.7]
        b = [x + 0.35, 15.2, 8.15]
        parts.append({"from": [_num(n) for n in a], "to": [_num(n) for n in b], "faces": faces("light")})
    return {"parent": "minecraft:block/block", "textures": textures, "elements": parts}


def write_ingame_models():
    models = ASSETS / "models" / "block"
    models.mkdir(parents=True, exist_ok=True)
    (models / "wind_harp.json").write_text(json.dumps(minecraft_model(True), indent=2) + "\n", encoding="utf-8")
    (models / "wind_harp_off.json").write_text(json.dumps(minecraft_model(False), indent=2) + "\n", encoding="utf-8")
    cubes = [{"part": n, "from": a, "to": b, "texture": tex} for n, a, b, tex, _up in CUBOIDS]
    for name, x, y0, y1 in STRINGS:
        cubes.append({"part": name, "from": [x, y0, 7.7], "to": [x + 0.35, y1, 8.15], "texture": "light",
                      "rig": name + "_pluck"})
    EXPORT.parent.mkdir(parents=True, exist_ok=True)
    EXPORT.write_text(json.dumps({"id": "wind_harp", "cubes": cubes}, indent=2) + "\n", encoding="utf-8")
    print("wrote in-game Wind Harp cuboid")


def blender_exe() -> str:
    from shutil import which

    found = which("blender")
    if found:
        return found
    candidates = [
        ROOT / "build/tooling/blender-4.5.10-windows-x64/blender.exe",
        Path(r"C:\Program Files\Blender Foundation\Blender 4.5\blender.exe"),
        Path.home() / "scoop/apps/blender/current/blender.exe",
    ]
    for path in candidates:
        if path.is_file():
            return str(path)
    raise SystemExit("Blender is not on PATH and was not found under build/tooling or scoop.")


def mc_center_size(a, b):
    cx = (a[0] + b[0]) / 2 / 16
    cy = (a[2] + b[2]) / 2 / 16
    cz = (a[1] + b[1]) / 2 / 16
    sx = abs(b[0] - a[0]) / 16
    sy = abs(b[2] - a[2]) / 16
    sz = abs(b[1] - a[1]) / 16
    return (cx, cy, cz), (sx, sy, sz)


def build_in_blender():
    import bpy
    from mathutils import Vector

    write_ingame_models()

    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)
    for block in (bpy.data.meshes, bpy.data.materials, bpy.data.collections, bpy.data.images):
        for item in list(block):
            block.remove(item)

    scene = bpy.context.scene
    # EEVEE plus unshaded emission: the still has to read as the in-game cuboid, not a studio mesh.
    for engine in ("BLENDER_EEVEE_NEXT", "BLENDER_EEVEE"):
        try:
            scene.render.engine = engine
            break
        except TypeError:
            continue
    scene.render.resolution_x = 1024
    scene.render.resolution_y = 1024
    scene.render.film_transparent = True
    scene.view_settings.view_transform = "Standard"
    scene.view_settings.look = "None"
    scene.render.image_settings.file_format = "PNG"
    scene.frame_start = 1
    scene.frame_end = 24
    if hasattr(scene, "eevee"):
        if hasattr(scene.eevee, "use_bloom"):
            scene.eevee.use_bloom = False
        if hasattr(scene.eevee, "use_gtao"):
            scene.eevee.use_gtao = False
    world = scene.world
    world.use_nodes = True
    bg = world.node_tree.nodes.get("Background")
    if bg is not None:
        # Living Lattice ink, same ground the 32px tiles sit on.
        bg.inputs["Color"].default_value = (17 / 255, 26 / 255, 34 / 255, 1.0)
        bg.inputs["Strength"].default_value = 1.0

    col = bpy.data.collections.new("Wind Harp")
    scene.collection.children.link(col)

    def pixel_mat(name, rel):
        path = ASSETS / rel
        m = bpy.data.materials.new(name)
        m.use_nodes = True
        nodes = m.node_tree.nodes
        links = m.node_tree.links
        for node in list(nodes):
            if node.type == "BSDF_PRINCIPLED":
                nodes.remove(node)
        tex = nodes.new("ShaderNodeTexImage")
        tex.image = bpy.data.images.load(str(path))
        tex.image.pack()
        tex.interpolation = "Closest"
        tex.extension = "REPEAT"
        tex.location = (-420, 300)
        emit = nodes.new("ShaderNodeEmission")
        emit.inputs["Strength"].default_value = 1.0
        emit.location = (-140, 300)
        out = nodes.get("Material Output")
        links.new(tex.outputs["Color"], emit.inputs["Color"])
        links.new(emit.outputs["Emission"], out.inputs["Surface"])
        if hasattr(m, "blend_method"):
            m.blend_method = "CLIP"
        if hasattr(m, "shadow_method"):
            m.shadow_method = "NONE"
        return m

    mats = {key: pixel_mat(key, rel) for key, rel in TEX_FILES.items()}
    origin = bpy.data.objects.new("wind_harp", None)
    col.objects.link(origin)
    origin.location = (0, 0, 0)

    def add_box(name, a, b, material):
        center, size = mc_center_size(a, b)
        bpy.ops.mesh.primitive_cube_add(size=1, location=center)
        obj = bpy.context.object
        obj.name = name
        obj.scale = size
        bpy.context.view_layer.update()
        obj.data.materials.append(material)
        obj.parent = origin
        obj.matrix_parent_inverse = origin.matrix_world.inverted()
        for c in list(obj.users_collection):
            c.objects.unlink(obj)
        col.objects.link(obj)
        return obj

    for name, a, b, tex, _up in CUBOIDS:
        add_box(name, a, b, mats[tex if tex != "side" else "side"])

    for i, (name, x, y0, y1) in enumerate(STRINGS):
        a = [x, y0, 7.7]
        b = [x + 0.35, y1, 8.15]
        obj = add_box(name, a, b, mats["light"])
        rest = tuple(obj.scale)
        empty = bpy.data.objects.new(name + "_pluck", None)
        col.objects.link(empty)
        empty.parent = obj
        empty.empty_display_size = 0.04
        for frame, zmul in ((1, 1.0), (1 + i * 2, 1.0), (4 + i * 2, 1.08), (10 + i * 2, 1.0), (24, 1.0)):
            obj.scale = (rest[0], rest[1], rest[2] * zmul)
            obj.keyframe_insert("scale", frame=frame)

    bpy.ops.object.camera_add()
    cam = bpy.context.object
    cam.name = "Wind Harp camera"
    target = Vector((0.5, 0.5, 0.5))
    cam.location = target + Vector((1.55, -1.55, 1.25))
    cam.rotation_euler = (target - cam.location).to_track_quat("-Z", "Y").to_euler()
    cam.data.type = "ORTHO"
    cam.data.ortho_scale = 1.45
    scene.camera = cam
    scene.frame_set(7)

    BLEND.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(BLEND))
    scene.render.filepath = str(STILL)
    bpy.ops.render.render(write_still=True)
    print("wrote", BLEND, "and", STILL)


if __name__ == "__main__":
    if "bpy" in sys.modules:
        build_in_blender()
    else:
        write_ingame_models()
        blender = blender_exe()
        cmd = [blender, "--background", "--python", str(Path(__file__).resolve())]
        print("running", *cmd)
        raise SystemExit(subprocess.call(cmd))
