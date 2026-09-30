#!/usr/bin/env python3
"""Генератор geo-модели и текстур разведывательного дрона (superbwarfare:scout_drone).

Рисуем сами: из wrbdrones ничего копировать нельзя (ARR). Силуэт — «мавик»:
вытянутый корпус, четыре Г-образных луча, подвесная камера на носу, два лопастных
винта на каждом моторе.

UV — box-раскладка Bedrock (1 юнит модели = 1 пиксель текстуры), поэтому геометрия
и текстура генерируются вместе: раскладка укладывается shelf-упаковкой, а красится
по тем же прямоугольникам. Правки геометрии — здесь, не в json.

    python3 tools/gen_scout_drone.py
"""

import json
import math
import os
import random

from PIL import Image

random.seed(20260905)

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/superbwarfare")

TEX = 128  # размер PNG
UV = 64  # система координат UV в geo (texture_width/height)
SCALE = TEX // UV  # 1 юнит модели = SCALE пикселей текстуры

# Материалы: (базовый цвет, разброс шума)
MAT = {
    "shell": ((198, 202, 207), 5),  # серый пластик корпуса
    "shell_light": ((228, 231, 234), 4),  # белая верхняя крышка
    "dark": ((88, 93, 100), 5),  # мотор/шасси/аккумулятор
    "prop": ((58, 62, 68), 3),  # лопасти
    "lens": ((26, 30, 38), 2),  # объектив
    "led": ((196, 60, 52), 2),  # проблесковый маяк
}

# Затенение граней, чтобы кубы читались объёмными без ручной отрисовки.
FACE_SHADE = {
    "up": 1.0,
    "down": 0.55,
    "north": 0.86,
    "south": 0.78,
    "east": 0.70,
    "west": 0.70,
}


def mirror_x(origin, size):
    return [-(origin[0] + size[0]), origin[1], origin[2]], list(size)


def arm(rotor_x, rotor_z, long_z0, long_len, lat_len):
    """Г-образный луч: продольный сегмент от корпуса + поперечный к мотору + мотор."""
    return [
        ("dark", [1.15, 3.05, long_z0], [0.9, 0.65, long_len]),
        ("dark", [1.15, 3.05, rotor_z - 0.45], [lat_len, 0.65, 0.9]),
        ("dark", [rotor_x - 0.65, 2.95, rotor_z - 0.65], [1.3, 1.1, 1.3]),
    ]


def prop(rotor_x, rotor_z):
    return [
        ("dark", [rotor_x - 0.1, 3.95, rotor_z - 0.1], [0.2, 0.7, 0.2]),
        ("dark", [rotor_x - 0.35, 4.05, rotor_z - 0.35], [0.7, 0.45, 0.7]),
        ("prop", [rotor_x - 0.25, 4.15, rotor_z - 2.6], [0.5, 0.22, 2.4]),
        ("prop", [rotor_x - 0.25, 4.15, rotor_z + 0.2], [0.5, 0.22, 2.4]),
    ]


ROTOR_FRONT = (4.9, -3.0)
ROTOR_BACK = (4.2, 3.9)

BODY_CUBES = [
    ("shell", [-1.6, 2.4, -3.4], [3.2, 1.2, 6.6]),  # фюзеляж
    ("shell_light", [-1.3, 3.6, -2.8], [2.6, 0.9, 4.4]),  # верхняя крышка
    ("shell", [-1.0, 2.7, -4.4], [2.0, 0.9, 1.0]),  # нос
    ("dark", [-1.2, 2.6, 3.2], [2.4, 1.1, 1.5]),  # аккумулятор
    ("dark", [1.0, 1.3, -0.9], [0.5, 1.1, 2.2]),  # стойка шасси L
    ("dark", [-1.5, 1.3, -0.9], [0.5, 1.1, 2.2]),  # стойка шасси R
    ("shell", [0.85, 1.15, -1.0], [0.8, 0.25, 2.4]),  # пятка L
    ("shell", [-1.65, 1.15, -1.0], [0.8, 0.25, 2.4]),  # пятка R
    ("dark", [-0.15, 4.5, 2.2], [0.3, 1.2, 0.3]),  # антенна
    ("led", [-0.3, 2.25, 1.4], [0.6, 0.2, 0.6]),  # маяк
]

for _x, _z, _z0, _len, _lat in (
    (ROTOR_FRONT[0], ROTOR_FRONT[1], -3.45, 2.3, 3.5),
    (ROTOR_BACK[0], ROTOR_BACK[1], 1.3, 3.05, 3.0),
):
    for mat, origin, size in arm(_x, _z, _z0, _len, _lat):
        BODY_CUBES.append((mat, origin, size))
        BODY_CUBES.append((mat,) + tuple(mirror_x(origin, size)))

CAMERA_CUBES = [
    ("dark", [-0.9, 2.2, -4.6], [1.8, 0.6, 0.9]),
    ("dark", [-0.85, 1.1, -4.75], [1.7, 1.2, 1.5]),
    ("lens", [-0.6, 1.35, -5.05], [1.2, 0.8, 0.35]),
]

BONES = [
    {"name": "bone", "pivot": [0, 0, 0], "cubes": []},
    {"name": "0", "parent": "bone", "pivot": [0, 2.2, 0], "cubes": []},
    {"name": "body", "parent": "0", "pivot": [0, 3, 0], "cubes": BODY_CUBES},
    {
        "name": "camera",
        "parent": "body",
        "pivot": [0, 2.2, -4.2],
        "cubes": CAMERA_CUBES,
    },
]

# Имена winglet-костей обязаны совпадать с DroneModel.collectTransform — оттуда идёт
# вращение пропеллеров, общее с обычным дроном.
for name, (rx, rz) in (
    ("wingFL", (ROTOR_FRONT[0], ROTOR_FRONT[1])),
    ("wingFR", (-ROTOR_FRONT[0], ROTOR_FRONT[1])),
    ("wingBL", (ROTOR_BACK[0], ROTOR_BACK[1])),
    ("wingBR", (-ROTOR_BACK[0], ROTOR_BACK[1])),
):
    BONES.append(
        {"name": name, "parent": "0", "pivot": [rx, 4.05, rz], "cubes": prop(rx, rz)}
    )


def box_uv_size(size):
    sx, sy, sz = size
    return 2 * (sx + sz), sy + sz


def face_rects(u, v, size):
    sx, sy, sz = size
    return {
        "up": (u + sz, v, sx, sz),
        "down": (u + sz + sx, v, sx, sz),
        "east": (u, v + sz, sz, sy),
        "north": (u + sz, v + sz, sx, sy),
        "west": (u + sz + sx, v + sz, sz, sy),
        "south": (u + 2 * sz + sx, v + sz, sx, sy),
    }


def pack(cubes):
    """Shelf-упаковка box-UV в квадрат TEX x TEX."""
    placed = []
    x = y = shelf_h = 0
    for mat, origin, size in cubes:
        w, h = box_uv_size(size)
        cw, ch = math.ceil(w) + 1, math.ceil(h) + 1
        if x + cw > UV:
            x, y, shelf_h = 0, y + shelf_h, 0
        if y + ch > UV:
            raise SystemExit("UV не влезает в %dx%d" % (UV, UV))
        placed.append((mat, origin, size, x, y))
        x += cw
        shelf_h = max(shelf_h, ch)
    return placed


def paint(img, placed):
    px = img.load()
    for mat, _origin, size, u, v in placed:
        base, noise = MAT[mat]
        for face, (fx, fy, fw, fh) in face_rects(u, v, size).items():
            shade = FACE_SHADE[face]
            x0, y0 = int(round(fx * SCALE)), int(round(fy * SCALE))
            x1, y1 = int(round((fx + fw) * SCALE)), int(round((fy + fh) * SCALE))
            for yy in range(y0, max(y1, y0 + 1)):
                for xx in range(x0, max(x1, x0 + 1)):
                    if not (0 <= xx < TEX and 0 <= yy < TEX):
                        continue
                    # Тёмная окантовка по краю грани: даёт панельные швы.
                    edge = 0.82 if (xx in (x0, x1 - 1) or yy in (y0, y1 - 1)) else 1.0
                    j = random.randint(-noise, noise)
                    px[xx, yy] = tuple(
                        max(0, min(255, int(c * shade * edge) + j)) for c in base
                    ) + (255,)


def write_geo(placed):
    by_bone = {}
    for (mat, origin, size, u, v), (bone_name, cube_index) in zip(placed, CUBE_INDEX):
        by_bone.setdefault(bone_name, []).append(
            {
                "origin": [round(c, 4) for c in origin],
                "size": [round(c, 4) for c in size],
                "uv": [round(u, 2), round(v, 2)],
            }
        )

    bones = []
    for bone in BONES:
        entry = {"name": bone["name"], "pivot": bone["pivot"]}
        if "parent" in bone:
            entry["parent"] = bone["parent"]
        cubes = by_bone.get(bone["name"])
        if cubes:
            entry["cubes"] = cubes
        bones.append(entry)

    geo = {
        "format_version": "1.12.0",
        "minecraft:geometry": [
            {
                "description": {
                    "identifier": "geometry.scout_drone",
                    "texture_width": UV,
                    "texture_height": UV,
                    "visible_bounds_width": 2,
                    "visible_bounds_height": 1.5,
                    "visible_bounds_offset": [0, 0.25, 0],
                },
                "bones": bones,
            }
        ],
    }
    path = os.path.join(ASSETS, "geo/scout_drone.geo.json")
    with open(path, "w") as f:
        json.dump(geo, f, indent=2)
        f.write("\n")
    return path


def gen_item_icon():
    """16x16 иконка: вид сверху на квадрокоптер."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()

    def rect(x0, y0, x1, y1, color):
        for y in range(y0, y1):
            for x in range(x0, x1):
                if 0 <= x < 16 and 0 <= y < 16:
                    px[x, y] = color + (255,)

    shell = (206, 210, 215)
    shell_hi = (234, 236, 239)
    dark = (86, 91, 98)
    prop = (64, 68, 74)

    # Лучи по диагонали.
    for i in range(5):
        for dx, dy in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
            x, y = 8 + dx * (2 + i), 8 + dy * (2 + i)
            rect(x - 1, y - 1, x + 1, y + 1, dark)
    # Моторы + винты.
    for dx, dy in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        cx, cy = 8 + dx * 6, 8 + dy * 6
        rect(cx - 1, cy - 1, cx + 1, cy + 1, dark)
        rect(cx - 3, cy, cx + 3, cy + 1, prop)
        rect(cx, cy - 3, cx + 1, cy + 3, prop)
    # Корпус.
    rect(6, 4, 10, 12, shell)
    rect(7, 5, 9, 10, shell_hi)
    # Камера на носу.
    rect(7, 3, 9, 5, dark)
    rect(7, 3, 9, 4, (30, 34, 42))

    path = os.path.join(ASSETS, "textures/item/scout_drone.png")
    img.save(path)
    return path


CUBE_INDEX = []
ALL_CUBES = []
for bone in BONES:
    for i, cube in enumerate(bone["cubes"]):
        ALL_CUBES.append(cube)
        CUBE_INDEX.append((bone["name"], i))

if __name__ == "__main__":
    placed = pack(ALL_CUBES)
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    paint(img, placed)
    tex_path = os.path.join(ASSETS, "textures/entity/scout_drone.png")
    img.save(tex_path)
    geo_path = write_geo(placed)
    icon_path = gen_item_icon()
    print("bones: %d, cubes: %d" % (len(BONES), len(ALL_CUBES)))
    for p in (geo_path, tex_path, icon_path):
        print("wrote", os.path.relpath(p, ROOT))
