"""Wind Harp Blender rig (design 3.1 §15).

Builds the folk harp from the same cuboids as tools/generate_3_1_assets.py, parents five
string empties as a pluck rig, saves art/blocks/wind_harp.blend and a studio still.
Does not overwrite the in-game JSON cuboid — that generator stays the shipped model.

Run with Blender on PATH, or pass --blender:

    blender --background --python tools/blender_wind_harp.py
    python tools/blender_wind_harp.py
"""
from __future__ import annotations

import json
import math
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BLEND = ROOT / "art" / "blocks" / "wind_harp.blend"
STILL = ROOT / "art" / "blocks" / "wind_harp.png"
EXPORT = ROOT / "art" / "blocks" / "wind_harp-export.json"

# Minecraft from/to in 0–16 space, matching generate_3_1_assets.cuboid("wind_harp").
CUBOIDS = [
    ("plinth", [2, 0, 5], [14, 2, 11], (0.42, 0.40, 0.36, 1)),
    ("soundbox", [10, 2, 5], [14, 14, 11], (0.62, 0.38, 0.22, 1)),
    ("soundbox_crown", [10, 14, 5], [14, 15, 11], (0.78, 0.55, 0.28, 1)),
    ("forepillar", [2, 2, 7], [4, 15, 9], (0.55, 0.36, 0.20, 1)),
    ("neck", [2, 15, 7], [12, 16, 9], (0.55, 0.36, 0.20, 1)),
    ("rail", [4, 2, 7], [10, 3.5, 9], (0.78, 0.55, 0.28, 1)),
    ("back", [5, 4, 9], [10, 13, 10.5], (0.50, 0.32, 0.18, 1)),
]
STRINGS = [("string_%d" % (i + 1), x, 3.6 + i * 0.5, 15.2) for i, x in enumerate((4.6, 5.8, 7.0, 8.2, 9.4))]


def blender_exe() -> str:
    from shutil import which

    found = which("blender")
    if found:
        return found
    candidates = [
        ROOT / "build/tooling/blender-4.5.10-windows-x64/blender.exe",
        Path(r"C:\Program Files\Blender Foundation\Blender 4.5\blender.exe"),
        Path(r"C:\Program Files\Blender Foundation\Blender 4.2\blender.exe"),
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

    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)
    for block in (bpy.data.meshes, bpy.data.materials, bpy.data.collections, bpy.data.images):
        for item in list(block):
            block.remove(item)

    scene = bpy.context.scene
    scene.render.engine = "CYCLES"
    scene.cycles.samples = 32
    scene.render.resolution_x = 1024
    scene.render.resolution_y = 1024
    scene.render.film_transparent = True
    scene.world.color = (0.08, 0.09, 0.12)
    scene.view_settings.view_transform = "Standard"
    scene.render.image_settings.file_format = "PNG"
    scene.frame_start = 1
    scene.frame_end = 24

    col = bpy.data.collections.new("Wind Harp")
    scene.collection.children.link(col)

    def mat(name, colour):
        m = bpy.data.materials.new(name)
        m.use_nodes = True
        bsdf = m.node_tree.nodes.get("Principled BSDF")
        bsdf.inputs["Base Color"].default_value = colour
        bsdf.inputs["Roughness"].default_value = 0.55
        return m

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

    export = []
    for name, a, b, colour in CUBOIDS:
        add_box(name, a, b, mat(name, colour))
        export.append({"part": name, "from": a, "to": b})

    string_mat = mat("string", (0.85, 0.92, 0.95, 1))
    bsdf = string_mat.node_tree.nodes.get("Principled BSDF")
    if "Emission Color" in bsdf.inputs:
        bsdf.inputs["Emission Color"].default_value = (0.7, 0.9, 1.0, 1)
    elif "Emission" in bsdf.inputs:
        bsdf.inputs["Emission"].default_value = (0.7, 0.9, 1.0, 1)
    bsdf.inputs["Emission Strength"].default_value = 0.35
    for i, (name, x, y0, y1) in enumerate(STRINGS):
        a = [x, y0, 7.7]
        b = [x + 0.35, y1, 8.15]
        obj = add_box(name, a, b, string_mat)
        rest = tuple(obj.scale)
        empty = bpy.data.objects.new(name + "_pluck", None)
        col.objects.link(empty)
        empty.parent = obj
        empty.empty_display_size = 0.04
        for frame, zmul in ((1, 1.0), (1 + i * 2, 1.0), (4 + i * 2, 1.12), (10 + i * 2, 1.0), (24, 1.0)):
            obj.scale = (rest[0], rest[1], rest[2] * zmul)
            obj.keyframe_insert("scale", frame=frame)
        export.append({"part": name, "from": a, "to": b, "rig": name + "_pluck"})

    bpy.ops.object.light_add(type="AREA", location=(1.6, -1.8, 1.8))
    key = bpy.context.object
    key.data.energy = 40
    key.data.size = 1.4
    bpy.ops.object.light_add(type="AREA", location=(-1.1, 1.2, 1.1))
    fill = bpy.context.object
    fill.data.energy = 12
    fill.data.size = 2.0

    bpy.ops.object.camera_add()
    cam = bpy.context.object
    cam.name = "Wind Harp camera"
    target = Vector((0.5, 0.5, 0.5))
    cam.location = target + Vector((1.35, -1.85, 0.95))
    cam.rotation_euler = (target - cam.location).to_track_quat("-Z", "Y").to_euler()
    cam.data.lens = 85
    scene.camera = cam
    scene.frame_set(7)

    BLEND.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(BLEND))
    scene.render.filepath = str(STILL)
    bpy.ops.render.render(write_still=True)
    EXPORT.write_text(json.dumps({"id": "wind_harp", "cubes": export}, indent=2) + "\n", encoding="utf-8")
    print("wrote", BLEND, "and", STILL)


if __name__ == "__main__":
    if "bpy" in sys.modules:
        build_in_blender()
    else:
        blender = blender_exe()
        cmd = [blender, "--background", "--python", str(Path(__file__).resolve())]
        print("running", *cmd)
        raise SystemExit(subprocess.call(cmd))
