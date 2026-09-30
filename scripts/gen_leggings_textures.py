#!/usr/bin/env python3
"""Свои текстуры поножей ru_leggings / us_leggings (ассеты fracturepoint копировать нельзя).

Текстура брони 128x128 разбита на три плоских региона, на которые geo-модель ссылается
по-фейсово (per-face uv), поэтому раскладка не зависит от размеров кубов:
    (0,0)-(63,63)    ткань штанины
    (64,0)-(127,63)  накладка наколенника
    (0,64)-(63,127)  набедренный карман
Ещё генерируется иконка предмета 16x16.

Запуск: python3 scripts/gen_leggings_textures.py
"""

import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ARMOR_DIR = os.path.join(
    ROOT, "src/main/resources/assets/superbwarfare/textures/bedrock/armor"
)
ITEM_DIR = os.path.join(ROOT, "src/main/resources/assets/superbwarfare/textures/item")

PALETTES = {
    # РФ: оливковая «флора», крупные мягкие пятна.
    "ru_leggings": {
        "seed": 6243,
        "cloth": (86, 96, 60),
        "camo": [(70, 80, 48), (100, 108, 70), (82, 76, 50)],
        "blob": (4, 9),
        "density": 90,
        "pad": (48, 52, 40),
        "pad_camo": [(38, 42, 32), (60, 64, 50)],
        "pocket": (74, 84, 52),
        "strap": (52, 44, 30),
    },
    # США: койот/мультикам, мелкий «пиксель».
    "us_leggings": {
        "seed": 1077,
        "cloth": (146, 132, 94),
        "camo": [(97, 100, 60), (88, 71, 48), (172, 158, 118), (118, 112, 74)],
        "blob": (3, 6),
        "density": 140,
        "pad": (92, 82, 60),
        "pad_camo": [(74, 66, 48), (112, 100, 76)],
        "pocket": (124, 113, 82),
        "strap": (86, 75, 52),
    },
}

REGIONS = {"cloth": (0, 0), "pad": (64, 0), "pocket": (0, 64)}
SIZE = 64


def fill_region(draw, rng, origin, base, spots, blob, density):
    x0, y0 = origin
    draw.rectangle([x0, y0, x0 + SIZE - 1, y0 + SIZE - 1], fill=base)
    for _ in range(density):
        w = rng.randint(*blob)
        h = rng.randint(*blob)
        x = x0 + rng.randint(0, SIZE - w)
        y = y0 + rng.randint(0, SIZE - h)
        draw.ellipse([x, y, x + w, y + h], fill=rng.choice(spots))


def build_armor(name, p):
    rng = random.Random(p["seed"])
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    fill_region(
        draw, rng, REGIONS["cloth"], p["cloth"], p["camo"], p["blob"], p["density"]
    )
    fill_region(draw, rng, REGIONS["pad"], p["pad"], p["pad_camo"], (5, 11), 40)
    fill_region(
        draw, rng, REGIONS["pocket"], p["pocket"], p["camo"], p["blob"], p["density"]
    )

    # Наколенник: горизонтальные рёбра накладки.
    px, py = REGIONS["pad"]
    for y in range(py + 8, py + SIZE - 6, 11):
        draw.rectangle([px + 6, y, px + SIZE - 7, y + 3], fill=p["strap"])

    # Карман: клапан сверху и шов по низу.
    ox, oy = REGIONS["pocket"]
    draw.rectangle([ox + 5, oy + 6, ox + SIZE - 6, oy + 20], fill=p["strap"])
    draw.rectangle(
        [ox + 5, oy + SIZE - 12, ox + SIZE - 6, oy + SIZE - 10], fill=p["strap"]
    )

    img.save(os.path.join(ARMOR_DIR, name + ".png"))


def build_icon(name, p):
    rng = random.Random(p["seed"] + 1)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    # Силуэт штанов: пояс и две штанины.
    draw.rectangle([3, 2, 12, 5], fill=p["strap"])
    draw.rectangle([3, 6, 6, 14], fill=p["cloth"])
    draw.rectangle([9, 6, 12, 14], fill=p["cloth"])
    draw.rectangle([3, 6, 12, 7], fill=p["cloth"])
    for _ in range(22):
        x = rng.randint(3, 12)
        y = rng.randint(6, 13)
        if 6 < x < 9 and y > 7:
            continue
        draw.point((x, y), fill=rng.choice(p["camo"]))
    # Наколенники.
    draw.rectangle([3, 11, 6, 12], fill=p["pad"])
    draw.rectangle([9, 11, 12, 12], fill=p["pad"])
    img.save(os.path.join(ITEM_DIR, name + ".png"))


if __name__ == "__main__":
    for name, palette in PALETTES.items():
        build_armor(name, palette)
        build_icon(name, palette)
        print("written", name)
