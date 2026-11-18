"""Generates a cohesive GBA Pokemon-Emerald-inspired 2x pixel-art asset suite for Amazon Mages.

All runtime sprites use crisp NEAREST 2x scaling (1 logical pixel = 2x2 real pixels)
and derive colors from the unified master palette in art/palettes/amazon.json plus
complementary Hoenn-style stone, water, and colonial architecture ramps.

Run:
    python tools/generate_emerald_assets.py
"""
from __future__ import annotations

import math
import random
from pathlib import Path
from PIL import Image, ImageDraw

OUT = Path("app/src/main/res/drawable-nodpi")
OUT.mkdir(parents=True, exist_ok=True)


def C(hexv: str, alpha: int = 255) -> tuple[int, int, int, int]:
    hexv = hexv.lstrip("#")
    return tuple(int(hexv[i : i + 2], 16) for i in (0, 2, 4)) + (alpha,)


# Master palette aligned with art/palettes/amazon.json + Hoenn GBA harmony
INK = C("#0B1A12")
INK_WARM = C("#1B1008")
INK_COOL = C("#14261C")
SHADOW = C("#0B1A12", 115)

GRASS = [C("#1D5A2C"), C("#2C8A3A"), C("#3CA342"), C("#5BC04D"), C("#8BE065"), C("#B4EC78")]
FOLIAGE = [C("#123F2A"), C("#1E6436"), C("#2F8B3E"), C("#55B24A"), C("#94D768")]
DIRT = [C("#5A3A24"), C("#86593A"), C("#B38455"), C("#D6AF78"), C("#EED7A4")]
WOOD = [C("#2B1A12"), C("#4A2E1F"), C("#6B4529"), C("#8F6238"), C("#BD8550")]
STONE = [C("#2E3B2A"), C("#4A5E3C"), C("#6F8A52"), C("#A8C27A"), C("#D5E1BA")]
ROCK = [C("#3A3832"), C("#5C584D"), C("#837D6E"), C("#ABA391"), C("#D4CCB8")]
NEGRO = [C("#0E203E"), C("#163364"), C("#224B8A"), C("#376DB5"), C("#72A9E2")]
SOLIMOES = [C("#284B47"), C("#3E6E64"), C("#5C8E75"), C("#89AD7B"), C("#C5D69E")]
SWAMP_GROUND = [C("#1D221D"), C("#2F352E"), C("#444B41"), C("#5E6658")]
ACID = [C("#4A6E12"), C("#8FD12A"), C("#C6F032"), C("#5A2A8A"), C("#8A3FD0"), C("#B98AF0")]
GOLD = [C("#7A5A1E"), C("#C99A2E"), C("#F2D36B"), C("#FFF1B0")]
CREAM = [C("#B8C99A"), C("#E4DFC0"), C("#F4EBC8"), C("#FFF9E6")]
TERRACOTTA = [C("#5A1E1A"), C("#9C3A2E"), C("#CC5844"), C("#E67A5E"), C("#F4AE8A")]
TEAL = [C("#153A42"), C("#1C4A52"), C("#2D7C80"), C("#4FB0B2"), C("#9AE2DF")]
CRIMSON = [C("#3B0D1D"), C("#6B172F"), C("#9A2438"), C("#D43E48"), C("#F07A6A")]
FIRE = [C("#7A2418"), C("#C4452A"), C("#F08A38"), C("#FFD05A"), C("#FFF3AA")]
ARCANE = [C("#1A3E7A"), C("#2861B0"), C("#3E8FD6"), C("#7CC4F0"), C("#D2F2FF")]
LEAF_MAGIC = [C("#0F4A2E"), C("#1E8A4A"), C("#3DC45E"), C("#9BF06A"), C("#F4F7A0")]
WHITE = C("#FFFFFF")

# Direction enum order: SOUTH, SOUTH_EAST, EAST, NORTH_EAST, NORTH, NORTH_WEST, WEST, SOUTH_WEST
DIRECTIONS = (
    (0, 1), (1, 1), (1, 0), (1, -1),
    (0, -1), (-1, -1), (-1, 0), (-1, 1),
)


def new_img(w: int, h: int, bg=(0, 0, 0, 0)) -> Image.Image:
    return Image.new("RGBA", (w, h), bg)


def save_2x(name: str, logical_img: Image.Image) -> None:
    scaled = logical_img.resize(
        (logical_img.width * 2, logical_img.height * 2), Image.Resampling.NEAREST
    )
    scaled.save(OUT / f"{name}.png", optimize=True)


def save_strip_2x(name: str, cols: int, cw: int, ch: int, draw_fn) -> None:
    sheet = new_img(cols * cw, ch)
    for i in range(cols):
        cell = new_img(cw, ch)
        draw_fn(ImageDraw.Draw(cell), cell, i, cw, ch)
        sheet.alpha_composite(cell, (i * cw, 0))
    save_2x(name, sheet)


def px(img: Image.Image, x: int, y: int, color: tuple[int, int, int, int]) -> None:
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), color)


def paint_grass_base(img: Image.Image, ox: int, oy: int, seed: int, rich: bool = False) -> None:
    d = ImageDraw.Draw(img)
    d.rectangle((ox, oy, ox + 31, oy + 31), fill=GRASS[2])
    rng = random.Random(seed)
    # Subtle Hoenn diamond/tuft weave without noisy speckles
    for ty in range(2, 31, 6):
        for tx in range(2 + (ty % 4), 31, 7):
            x = ox + (tx + rng.randint(-1, 1)) % 30
            y = oy + (ty + rng.randint(-1, 1)) % 30
            c_idx = 3 if rng.random() < 0.65 else 1
            px(img, x, y, GRASS[c_idx])
            px(img, x + 1, y - 1, GRASS[3 if c_idx == 1 else 4])
            if rng.random() < 0.35:
                px(img, x - 1, y - 1, GRASS[1])
    if rich:
        for _ in range(3):
            fx = ox + rng.randint(4, 26)
            fy = oy + rng.randint(4, 26)
            petal = rng.choice([GOLD[2], CREAM[2], TERRACOTTA[3], ARCANE[3]])
            px(img, fx, fy, GRASS[0])
            px(img, fx - 1, fy, petal)
            px(img, fx + 1, fy, petal)
            px(img, fx, fy - 1, petal)
            px(img, fx, fy + 1, GOLD[2] if petal != GOLD[2] else CREAM[3])


def generate_grass_tiles() -> None:
    sheet = new_img(8 * 32, 32)
    for n in range(8):
        paint_grass_base(sheet, n * 32, 0, 300 + n * 41, rich=(n >= 4))
        if n in (4, 5):
            # Clover cluster
            rng = random.Random(900 + n)
            for _ in range(2):
                cx = n * 32 + rng.randint(6, 25)
                cy = rng.randint(6, 25)
                for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                    px(sheet, cx + dx, cy + dy, GRASS[4])
                px(sheet, cx, cy, GRASS[0])
    save_2x("emerald_tiles_grass", sheet)


def draw_autotile_mask(
    draw: ImageDraw.ImageDraw,
    img: Image.Image,
    mask: int,
    palette: list[tuple[int, int, int, int]],
    edge_palette: list[tuple[int, int, int, int]],
    seed: int,
    rounded: int = 7,
    lip_top: int = 3,
    is_stone: bool = False,
) -> None:
    """Draws one 32x32 autotile cell for cardinal mask (N=1, E=2, S=4, W=8) where bit=1 means outer border."""
    n = bool(mask & 1)
    e = bool(mask & 2)
    s = bool(mask & 4)
    w = bool(mask & 8)

    left = 4 if w else 0
    top = 4 if n else 0
    right = 27 if e else 31
    bottom = 27 if s else 31

    # Inner fill rectangle
    draw.rectangle((left, top, right, bottom), fill=palette[2])
    rng = random.Random(seed + mask * 13)
    if is_stone:
        # Carved flagstone grid
        for gy in (0, 8, 16, 24):
            if top <= gy <= bottom:
                draw.line((left, gy, right, gy), fill=palette[1], width=1)
        for row_i, gy in enumerate((0, 8, 16, 24)):
            offset = (row_i % 2) * 8
            for gx in range(offset, 32, 16):
                if left <= gx <= right and top <= gy <= bottom - 2:
                    y2 = min(bottom, gy + 7)
                    draw.line((gx, gy + 1, gx, y2), fill=palette[1], width=1)
                    if gx + 2 <= right and gy + 2 <= bottom:
                        px(img, gx + 2, gy + 2, palette[3])
    else:
        for _ in range(18):
            tx = rng.randint(left + 1, max(left + 1, right - 1))
            ty = rng.randint(top + 1, max(top + 1, bottom - 1))
            px(img, tx, ty, palette[3] if rng.random() < 0.45 else palette[1])

    # Crisp organic GBA border steps where bit=1
    if n:
        draw.line((left, top, right, top), fill=edge_palette[0], width=1)
        for hx in range(left + 1, right):
            if (hx + seed) % 5 == 0:
                px(img, hx, top - 1, edge_palette[1])
                px(img, hx, top + 1, edge_palette[0])
            elif hx % 3 == 0:
                px(img, hx, top + 1, palette[3])
        if lip_top > 0:
            draw.line((left + 1, top - 1, right - 1, top - 1), fill=edge_palette[1], width=1)
    if s:
        draw.line((left, bottom, right, bottom), fill=edge_palette[0], width=1)
        draw.line((left + 1, bottom - 1, right - 1, bottom - 1), fill=palette[1], width=1)
        for hx in range(left + 2, right - 1, 5):
            px(img, hx, bottom + 1, edge_palette[1])
    if w:
        draw.line((left, top, left, bottom), fill=edge_palette[0], width=1)
        for hy in range(top + 2, bottom - 1, 4):
            px(img, left + 1, hy, palette[3])
            if (hy + seed) % 6 == 0:
                px(img, left - 1, hy, edge_palette[1])
    if e:
        draw.line((right, top, right, bottom), fill=edge_palette[0], width=1)
        draw.line((right - 1, top + 1, right - 1, bottom - 1), fill=palette[1], width=1)
        for hy in range(top + 2, bottom - 1, 5):
            px(img, right + 1, hy, edge_palette[1])

    # Stepped convex outer corners so roads/plazas curve smoothly into grass
    def cut_corner(cx: int, cy: int, sx: int, sy: int) -> None:
        for dy in range(rounded):
            for dx in range(rounded):
                if dx + dy < rounded:
                    px(img, cx + dx * sx, cy + dy * sy, (0, 0, 0, 0))
                elif dx + dy == rounded:
                    px(img, cx + dx * sx, cy + dy * sy, edge_palette[0])
                elif dx + dy == rounded + 1:
                    px(img, cx + dx * sx, cy + dy * sy, palette[3] if sy > 0 else palette[1])

    if n and w:
        cut_corner(left, top, 1, 1)
    if n and e:
        cut_corner(right, top, -1, 1)
    if s and e:
        cut_corner(right, bottom, -1, -1)
    if s and w:
        cut_corner(left, bottom, 1, -1)


def draw_inner_corner_cutout(
    img: Image.Image, corner_idx: int, edge_color: tuple[int, int, int, int], grass_shade: tuple[int, int, int, int]
) -> None:
    """Frames 16..19: transparent corner grass patches for diagonal inner corners (NW, NE, SE, SW)."""
    cx, cy, sx, sy = [(0, 0, 1, 1), (31, 0, -1, 1), (31, 31, -1, -1), (0, 31, 1, -1)][corner_idx]
    for dy in range(6):
        for dx in range(6):
            x, y = cx + dx * sx, cy + dy * sy
            if dx + dy < 4:
                px(img, x, y, grass_shade)
            elif dx + dy == 4:
                px(img, x, y, edge_color)
            elif dx + dy == 5:
                px(img, x, y, GRASS[3])


def generate_terrain_autotiles() -> None:
    # 1. Path (20 frames: 0..15 cardinal masks + 16..19 inner grass corner patches)
    def draw_path(d: ImageDraw.ImageDraw, cell: Image.Image, i: int, w: int, h: int) -> None:
        if i < 16:
            draw_autotile_mask(d, cell, i, DIRT, [GRASS[0], GRASS[1]], 101, rounded=6, lip_top=1)
        else:
            draw_inner_corner_cutout(cell, i - 16, DIRT[0], GRASS[2])

    save_strip_2x("emerald_tiles_path", 20, 32, 32, draw_path)

    # 2. Stone Paving (20 frames)
    def draw_paving(d: ImageDraw.ImageDraw, cell: Image.Image, i: int, w: int, h: int) -> None:
        if i < 16:
            draw_autotile_mask(d, cell, i, ROCK, [INK_COOL, STONE[1]], 202, rounded=5, lip_top=1, is_stone=True)
        else:
            draw_inner_corner_cutout(cell, i - 16, ROCK[0], GRASS[2])

    save_strip_2x("emerald_tiles_paving", 20, 32, 32, draw_paving)

    # 3. Swamp / Burned Soil (20 frames)
    def draw_swamp(d: ImageDraw.ImageDraw, cell: Image.Image, i: int, w: int, h: int) -> None:
        if i < 16:
            draw_autotile_mask(d, cell, i, SWAMP_GROUND, [INK, GRASS[0]], 303, rounded=6, lip_top=1)
            rng = random.Random(303 + i * 29)
            if i == 0 or rng.random() < 0.55:
                bx, by = rng.randint(8, 22), rng.randint(8, 22)
                d.ellipse((bx - 4, by - 2, bx + 4, by + 2), fill=ACID[0], outline=INK)
                d.ellipse((bx - 2, by - 1, bx + 2, by + 1), fill=ACID[1])
                px(cell, bx - 1, by - 1, ACID[2])
            if i % 3 == 0:
                px(cell, rng.randint(7, 24), rng.randint(7, 24), ACID[4])
        else:
            draw_inner_corner_cutout(cell, i - 16, SWAMP_GROUND[0], GRASS[1])

    save_strip_2x("emerald_tiles_swamp", 20, 32, 32, draw_swamp)

    # 4. Water autotiles (20 frames: 0..15 bank masks + 16..19 animated wave shimmer overlays)
    for name, pal, seed in (
        ("emerald_tiles_water_negro", NEGRO, 404),
        ("emerald_tiles_water_solimoes", SOLIMOES, 505),
    ):
        def draw_water(d: ImageDraw.ImageDraw, cell: Image.Image, i: int, w: int, h: int, pal=pal, seed=seed) -> None:
            if i < 16:
                d.rectangle((0, 0, 31, 31), fill=pal[1])
                rng = random.Random(seed + i * 19)
                for _ in range(16):
                    px(cell, rng.randint(0, 31), rng.randint(0, 31), pal[2] if rng.random() < 0.6 else pal[0])
                n, e, s, w_bit = bool(i & 1), bool(i & 2), bool(i & 4), bool(i & 8)
                if n:
                    # Raised GBA cliff/earth riverbank along north shore
                    d.rectangle((0, 0, 31, 4), fill=DIRT[1])
                    d.line((0, 0, 31, 0), fill=GRASS[1], width=1)
                    d.line((0, 5, 31, 5), fill=INK, width=1)
                    d.line((0, 6, 31, 6), fill=pal[3], width=1)
                if s:
                    d.rectangle((0, 28, 31, 31), fill=DIRT[2])
                    d.line((0, 27, 31, 27), fill=INK, width=1)
                    d.line((0, 26, 31, 26), fill=pal[3], width=1)
                if w_bit:
                    d.rectangle((0, 0, 3, 31), fill=DIRT[2])
                    d.line((4, 0, 4, 31), fill=INK, width=1)
                    d.line((5, 0, 5, 31), fill=pal[3], width=1)
                if e:
                    d.rectangle((28, 0, 31, 31), fill=DIRT[2])
                    d.line((27, 0, 27, 31), fill=INK, width=1)
                    d.line((26, 0, 26, 31), fill=pal[3], width=1)
            else:
                phase = i - 16
                for k in range(3):
                    wx = (4 + k * 10 + phase * 2) % 24 + 2
                    wy = 7 + k * 9 + (phase % 2)
                    d.line((wx, wy, wx + 4, wy), fill=pal[3], width=1)
                    if (k + phase) % 2 == 0:
                        px(cell, wx + 2, wy - 1, pal[4])

        save_strip_2x(name, 20, 32, 32, draw_water)

    # 5. Modular Bridge (6 frames: 0=plank deck, 1=north rail, 2=south rail+pilings, 3=west rail, 4=east rail, 5=corner posts)
    def draw_bridge(d: ImageDraw.ImageDraw, cell: Image.Image, i: int, w: int, h: int) -> None:
        if i == 0:
            d.rectangle((0, 0, 31, 31), fill=WOOD[3])
            for x in range(0, 32, 4):
                d.line((x, 0, x, 31), fill=WOOD[1], width=1)
                d.line((x + 1, 0, x + 1, 31), fill=WOOD[4], width=1)
                px(cell, x + 2, 5, WOOD[0])
                px(cell, x + 2, 26, WOOD[0])
        elif i == 1:
            d.rectangle((0, 0, 31, 3), fill=WOOD[0])
            d.rectangle((0, 1, 31, 2), fill=WOOD[4])
            for x in (2, 16, 29):
                d.rectangle((x - 1, 0, x + 1, 4), fill=WOOD[0])
                d.rectangle((x, 1, x, 3), fill=GOLD[2])
        elif i == 2:
            d.rectangle((0, 26, 31, 29), fill=WOOD[0])
            d.rectangle((0, 27, 31, 28), fill=WOOD[3])
            for x in (2, 16, 29):
                d.rectangle((x - 1, 25, x + 1, 31), fill=WOOD[0])
                d.rectangle((x, 26, x, 30), fill=WOOD[2])
                px(cell, x, 26, GOLD[1])
        elif i == 3:
            d.rectangle((0, 0, 3, 31), fill=WOOD[0])
            d.rectangle((1, 0, 2, 31), fill=WOOD[3])
            for y in (2, 16, 29):
                d.rectangle((0, y - 1, 3, y + 1), fill=GOLD[1])
        elif i == 4:
            d.rectangle((28, 0, 31, 31), fill=WOOD[0])
            d.rectangle((29, 0, 30, 31), fill=WOOD[3])
            for y in (2, 16, 29):
                d.rectangle((28, y - 1, 31, y + 1), fill=GOLD[1])
        else:
            d.rectangle((0, 12, 31, 19), fill=WOOD[2])
            d.line((0, 12, 31, 12), fill=WOOD[4], width=1)

    save_strip_2x("emerald_tiles_bridge", 6, 32, 32, draw_bridge)


def draw_leaf_cluster(
    d: ImageDraw.ImageDraw, cx: int, cy: int, rx: int, ry: int, pal=FOLIAGE
) -> None:
    d.ellipse((cx - rx - 1, cy - ry - 1, cx + rx + 1, cy + ry + 1), fill=INK)
    # Stepped Hoenn Petalburg-style sub-lobes around the rim for organic pixel silhouette
    for sx, sy in ((-rx + 3, 0), (rx - 3, 0), (0, -ry + 2), (-rx // 2, -ry // 2), (rx // 2, -ry // 2)):
        sr = max(4, min(rx, ry) // 3)
        d.ellipse((cx + sx - sr - 1, cy + sy - sr - 1, cx + sx + sr + 1, cy + sy + sr + 1), fill=INK)
    d.ellipse((cx - rx, cy - ry, cx + rx, cy + ry), fill=pal[1])
    for sx, sy in ((-rx + 3, 0), (rx - 3, 0), (0, -ry + 2), (-rx // 2, -ry // 2), (rx // 2, -ry // 2)):
        sr = max(4, min(rx, ry) // 3)
        d.ellipse((cx + sx - sr, cy + sy - sr, cx + sx + sr, cy + sy + sr), fill=pal[1])
    d.ellipse((cx - rx + 1, cy - ry + 1, cx + rx - 1, cy + ry - 2), fill=pal[2])
    d.ellipse((cx - rx + 3, cy - ry + 1, cx + rx - 3, cy + ry - 5), fill=pal[3])
    # Dappled GBA foliage highlights
    for hx, hy in ((-rx // 2, -ry // 3), (0, -ry // 2), (rx // 3, -ry // 4), (-rx // 4, 0)):
        d.ellipse((cx + hx - 2, cy + hy - 1, cx + hx + 2, cy + hy + 1), fill=pal[4])
    d.arc((cx - rx + 4, cy - ry + 2, cx + rx - 4, cy + ry - 4), 205, 325, fill=pal[4], width=2)


def generate_scenery_trees() -> None:
    # 4 x (64x80 logical -> 128x160 px)
    def draw_tree(d: ImageDraw.ImageDraw, cell: Image.Image, idx: int, w: int, h: int) -> None:
        cx = w // 2
        d.ellipse((cx - 21, h - 12, cx + 21, h - 2), fill=SHADOW)
        if idx in (0, 1):
            # Hoenn broadleaf tree
            d.polygon(
                [(cx - 6, h - 24), (cx + 6, h - 24), (cx + 12, h - 5), (cx + 5, h - 7),
                 (cx, h - 4), (cx - 5, h - 7), (cx - 12, h - 5)],
                fill=WOOD[0],
            )
            d.rectangle((cx - 5, h - 28, cx + 5, h - 7), fill=WOOD[2])
            d.line((cx - 3, h - 26, cx - 3, h - 9), fill=WOOD[3], width=2)
            d.line((cx + 2, h - 24, cx + 3, h - 9), fill=WOOD[1], width=1)
            lobes = [
                (cx, 20, 21, 15), (cx - 15, 29, 14, 12), (cx + 15, 29, 14, 12),
                (cx - 11, 41, 15, 11), (cx + 11, 41, 15, 11), (cx, 37, 17, 13),
            ]
            for lx, ly, rx, ry in lobes:
                draw_leaf_cluster(d, lx, ly, rx, ry)
            if idx == 1:
                for fx, fy in ((cx - 8, 22), (cx + 9, 25), (cx - 2, 34), (cx + 6, 39)):
                    px(cell, fx, fy, GOLD[2])
                    px(cell, fx + 1, fy, GOLD[3])
        elif idx == 2:
            # Amazon Acai/Buriti Palm
            d.polygon([(cx - 4, h - 6), (cx + 4, h - 6), (cx + 3, 24), (cx - 2, 24)], fill=WOOD[1])
            d.line((cx, 25, cx + 1, h - 8), fill=WOOD[3], width=2)
            for ry in range(28, h - 8, 5):
                d.line((cx - 3, ry, cx + 3, ry), fill=WOOD[0], width=1)
            for i in range(7):
                ang = -2.8 + i * 0.48
                dx, dy = math.cos(ang), math.sin(ang)
                ex, ey = cx + dx * 24, 22 + dy * 17
                d.line((cx, 22, round(ex), round(ey)), fill=FOLIAGE[0], width=2)
                for step in range(2, 9):
                    fx, fy = cx + dx * step * 2.7, 22 + dy * step * 1.9
                    side = -1 if step % 2 else 1
                    d.line(
                        (round(fx), round(fy), round(fx - dy * side * 5), round(fy + dx * side * 5)),
                        fill=FOLIAGE[2 + (i + step) % 3], width=2,
                    )
            for bx, by in ((cx - 2, 26), (cx + 2, 27), (cx, 30)):
                d.ellipse((bx - 2, by - 2, bx + 2, by + 2), fill=ACID[3], outline=INK)
        else:
            # Withered Mata Morta tree
            d.polygon(
                [(cx - 7, h - 22), (cx + 7, h - 22), (cx + 13, h - 5), (cx, h - 4), (cx - 13, h - 5)],
                fill=INK,
            )
            d.polygon(
                [(cx - 5, h - 28), (cx + 5, h - 28), (cx + 7, h - 7), (cx - 7, h - 7)],
                fill=ROCK[1],
            )
            for bx, by in ((cx - 18, 22), (cx + 19, 25), (cx - 11, 14), (cx + 10, 12)):
                d.line((cx, h - 28, bx, by), fill=INK, width=4)
                d.line((cx, h - 28, bx, by), fill=ROCK[2], width=2)
            for mx, my in ((cx - 12, 26), (cx + 9, 28), (cx, 44)):
                d.ellipse((mx - 3, my - 2, mx + 3, my + 2), fill=ACID[1], outline=INK)

    save_strip_2x("emerald_scenery_trees", 4, 64, 80, draw_tree)


def generate_castanheira() -> None:
    # 192 x 224 logical -> 384 x 448 px
    w, h = 192, 224
    im = new_img(w, h)
    d = ImageDraw.Draw(im)
    cx = w // 2
    # Grass mound and deep ground shadow
    d.ellipse((cx - 82, h - 46, cx + 82, h - 6), fill=GRASS[1], outline=INK)
    d.ellipse((cx - 76, h - 42, cx + 76, h - 10), fill=GRASS[2])
    d.ellipse((cx - 64, h - 38, cx + 64, h - 10), fill=SHADOW)

    # Spreading buttress roots
    for pts in (
        [(cx - 26, h - 58), (cx - 66, h - 18), (cx - 52, h - 12), (cx - 16, h - 34)],
        [(cx + 26, h - 58), (cx + 66, h - 18), (cx + 52, h - 12), (cx + 16, h - 34)],
        [(cx - 18, h - 46), (cx - 42, h - 8), (cx - 26, h - 7), (cx - 8, h - 28)],
        [(cx + 18, h - 46), (cx + 42, h - 8), (cx + 26, h - 7), (cx + 8, h - 28)],
    ):
        d.polygon(pts, fill=WOOD[2], outline=INK_WARM)

    # Colossal trunk
    d.polygon(
        [(cx - 28, 74), (cx + 28, 74), (cx + 34, h - 24), (cx - 34, h - 24)],
        fill=WOOD[2], outline=INK_WARM,
    )
    d.rectangle((cx - 20, 78, cx - 6, h - 26), fill=WOOD[3])
    d.rectangle((cx + 10, 78, cx + 26, h - 26), fill=WOOD[1])
    for vx in range(cx - 22, cx + 24, 7):
        d.line((vx, 82, vx + (1 if vx < cx else -1), h - 28), fill=WOOD[0], width=1)

    # Arcane glowing sanctuary hollow at front base
    d.ellipse((cx - 12, h - 52, cx + 12, h - 24), fill=INK_WARM, outline=WOOD[4])
    d.ellipse((cx - 8, h - 48, cx + 8, h - 26), fill=ARCANE[1])
    d.ellipse((cx - 5, h - 44, cx + 5, h - 28), fill=ARCANE[3])
    d.point((cx, h - 38), fill=ARCANE[4])
    for rx, ry in ((cx - 16, h - 40), (cx + 16, h - 40), (cx, h - 58)):
        d.rectangle((rx - 1, ry - 1, rx + 1, ry + 1), fill=LEAF_MAGIC[3])

    # Lush emerald canopy tiers
    clusters = [
        (cx - 54, 78, 30, 22), (cx + 54, 78, 30, 22), (cx, 84, 38, 24),
        (cx - 64, 56, 28, 22), (cx + 64, 56, 28, 22), (cx - 30, 62, 34, 24),
        (cx + 30, 62, 34, 24), (cx, 54, 42, 26), (cx - 36, 36, 30, 22),
        (cx + 36, 36, 30, 22), (cx, 28, 36, 22),
    ]
    for lx, ly, rx, ry in clusters:
        draw_leaf_cluster(d, lx, ly, rx, ry)
    # Hanging vines and golden blossoms
    for vx in (cx - 52, cx - 28, cx + 28, cx + 52):
        d.line((vx, 92, vx, 114), fill=FOLIAGE[2], width=2)
        px(im, vx, 115, LEAF_MAGIC[3])
    for fx, fy in ((cx - 32, 32), (cx + 14, 24), (cx - 16, 52), (cx + 38, 56), (cx, 40)):
        d.rectangle((fx - 1, fy - 1, fx + 1, fy + 1), fill=GOLD[2])
        px(im, fx, fy, GOLD[3])
    save_2x("emerald_castanheira", im)


def generate_buildings() -> None:
    # 1. Maloca (112x88 logical -> 224x176 px)
    im = new_img(112, 88)
    d = ImageDraw.Draw(im)
    d.ellipse((6, 68, 106, 86), fill=SHADOW)
    # Bamboo wall base
    d.rectangle((14, 46, 98, 76), fill=WOOD[2], outline=INK_WARM)
    for x in range(18, 96, 6):
        d.line((x, 48, x, 75), fill=WOOD[1], width=1)
    # Entrance porch & dirt doorstep
    d.rectangle((42, 70, 70, 82), fill=DIRT[3], outline=DIRT[1])
    d.rectangle((46, 52, 66, 75), fill=INK_WARM, outline=WOOD[4])
    d.rectangle((49, 55, 63, 74), fill=WOOD[0])
    # Clay pots & torch flames
    for px_x in (28, 35, 77, 84):
        d.ellipse((px_x - 3, 68, px_x + 3, 76), fill=TERRACOTTA[2], outline=INK_WARM)
        px(im, px_x - 1, 70, TERRACOTTA[4])
    # Domed palm-thatch roof
    d.polygon([(4, 52), (24, 16), (56, 8), (88, 16), (108, 52)], fill=DIRT[2], outline=INK_WARM)
    d.polygon([(12, 46), (28, 18), (56, 11), (84, 18), (98, 46)], fill=DIRT[3])
    d.polygon([(26, 28), (56, 13), (80, 24), (56, 32)], fill=DIRT[4])
    for y in range(22, 52, 7):
        d.line((10 + (52 - y) // 2, y, 102 - (52 - y) // 2, y), fill=DIRT[1], width=1)
    d.rectangle((50, 5, 62, 11), fill=GOLD[1], outline=INK_WARM)
    save_2x("emerald_struct_maloca", im)

    # 2. Stepped Temple (128x112 logical -> 256x224 px)
    im = new_img(128, 112)
    d = ImageDraw.Draw(im)
    d.rectangle((9, 98, 119, 101), fill=SHADOW)
    tiers = [(10, 62, 118, 98), (22, 38, 106, 66), (34, 18, 94, 42)]
    for x1, y1, x2, y2 in tiers:
        d.rectangle((x1, y1, x2, y2), fill=ROCK[2], outline=INK_COOL)
        d.rectangle((x1 + 2, y1 + 2, x2 - 2, y1 + 7), fill=ROCK[3])
        d.line((x1 + 2, y2 - 2, x2 - 2, y2 - 2), fill=ROCK[1], width=2)
        for bx in range(x1 + 10, x2 - 6, 12):
            d.line((bx, y1 + 7, bx, y2 - 3), fill=ROCK[1], width=1)
            px(im, bx + 2, y1 + 11, ROCK[3])
    # Central stairway
    d.rectangle((50, 42, 78, 100), fill=ROCK[3], outline=INK_COOL)
    d.rectangle((46, 42, 50, 100), fill=ROCK[2], outline=INK_COOL)
    d.rectangle((78, 42, 82, 100), fill=ROCK[2], outline=INK_COOL)
    for sy in range(46, 100, 6):
        d.line((52, sy, 76, sy), fill=ROCK[1], width=2)
        d.line((52, sy + 2, 76, sy + 2), fill=ROCK[4], width=1)
    # Sanctuary portal
    d.rectangle((54, 22, 74, 42), fill=INK_COOL, outline=GOLD[1])
    d.rectangle((57, 25, 71, 41), fill=ARCANE[0])
    d.ellipse((61, 29, 67, 35), fill=ARCANE[2])
    px(im, 64, 31, LEAF_MAGIC[3])
    # Moss & hanging vines
    for mx, my, r in ((18, 66, 7), (104, 68, 8), (30, 40, 6), (96, 40, 6), (12, 97, 5), (116, 97, 5)):
        draw_leaf_cluster(d, mx, my, r, max(3, r - 2))
    for vx in (18, 30, 96, 108):
        d.line((vx, 68, vx, 84), fill=FOLIAGE[2], width=2)
    save_2x("emerald_struct_temple", im)

    # 3. Ancient Ruin (80x64 logical -> 160x128 px)
    im = new_img(80, 64)
    d = ImageDraw.Draw(im)
    d.rectangle((9, 52, 71, 55), fill=SHADOW)
    d.rectangle((10, 26, 70, 52), fill=ROCK[2], outline=INK_COOL)
    d.rectangle((14, 16, 28, 52), fill=ROCK[3], outline=INK_COOL)
    d.rectangle((52, 20, 66, 52), fill=ROCK[3], outline=INK_COOL)
    d.rectangle((26, 30, 54, 52), fill=INK_COOL)
    for ry in (32, 42):
        d.line((15, ry, 27, ry), fill=ROCK[1], width=1)
        d.line((53, ry, 65, ry), fill=ROCK[1], width=1)
    draw_leaf_cluster(d, 22, 16, 10, 7)
    draw_leaf_cluster(d, 58, 22, 9, 6)
    draw_leaf_cluster(d, 11, 51, 4, 3)
    draw_leaf_cluster(d, 69, 51, 4, 3)
    px(im, 20, 38, LEAF_MAGIC[3])
    px(im, 60, 40, LEAF_MAGIC[3])
    save_2x("emerald_struct_ruin", im)

    # 4. Teatro Amazonas (144x112 logical -> 288x224 px)
    im = new_img(144, 112)
    d = ImageDraw.Draw(im)
    d.rectangle((9, 98, 135, 101), fill=SHADOW)
    # Stone plinth & grand staircase
    d.rectangle((10, 90, 134, 98), fill=ROCK[3], outline=INK_COOL)
    # Salmon-rose facade + shaded recesses
    d.rectangle((12, 44, 132, 90), fill=TERRACOTTA[2], outline=INK_WARM)
    d.rectangle((14, 48, 130, 88), fill=TERRACOTTA[3])
    d.rectangle((12, 42, 132, 49), fill=CREAM[2], outline=INK_WARM)
    d.line((14, 44, 130, 44), fill=CREAM[3], width=1)
    # Pediment
    d.polygon([(36, 42), (72, 28), (108, 42)], fill=CREAM[2], outline=INK_WARM)
    d.polygon([(44, 41), (72, 31), (100, 41)], fill=TERRACOTTA[2])
    # Columns & arched windows/doors
    for cx_col in (22, 40, 58, 86, 104, 122):
        d.rectangle((cx_col - 3, 49, cx_col + 3, 90), fill=CREAM[2], outline=INK_WARM)
        d.line((cx_col - 1, 50, cx_col - 1, 89), fill=CREAM[3], width=1)
    for wx in (31, 113):
        d.rectangle((wx - 4, 56, wx + 4, 72), fill=TEAL[1], outline=CREAM[2])
        d.rectangle((wx - 2, 58, wx, 64), fill=ARCANE[3])
    for door_x in (49, 72, 95):
        d.rectangle((door_x - 5, 62, door_x + 5, 90), fill=TEAL[1], outline=INK_COOL)
        d.ellipse((door_x - 5, 57, door_x + 5, 67), fill=TEAL[2], outline=INK_COOL)
    d.rectangle((60, 88, 84, 100), fill=ROCK[3], outline=INK_COOL)
    d.line((62, 93, 82, 93), fill=ROCK[2], width=1)
    # Foundation flower bushes at corners
    draw_leaf_cluster(d, 14, 95, 5, 3)
    draw_leaf_cluster(d, 130, 95, 5, 3)
    # Iconic green & gold mosaic cupola dome
    d.ellipse((46, 10, 98, 40), fill=FOLIAGE[2], outline=INK_COOL)
    d.ellipse((51, 12, 93, 35), fill=GOLD[2])
    for rib_x in (58, 66, 72, 78, 86):
        d.line((72, 11, rib_x, 36), fill=FOLIAGE[2], width=2)
    for dy in range(16, 36, 5):
        for dx in range(54, 90, 6):
            if (dx + dy) % 4 == 0:
                px(im, dx, dy, GOLD[3])
    d.rectangle((70, 4, 74, 12), fill=GOLD[3], outline=INK_WARM)
    save_2x("emerald_struct_theater", im)

    # 5. Riverfront City House (80x72 logical -> 160x144 px)
    im = new_img(80, 72)
    d = ImageDraw.Draw(im)
    d.rectangle((11, 62, 69, 65), fill=SHADOW)
    d.rectangle((12, 34, 68, 62), fill=CREAM[2], outline=INK_WARM)
    d.line((13, 35, 67, 35), fill=CREAM[1], width=2)
    d.polygon([(6, 36), (18, 12), (62, 12), (74, 36)], fill=TERRACOTTA[2], outline=INK_WARM)
    d.polygon([(11, 33), (20, 15), (60, 15), (69, 33)], fill=TERRACOTTA[3])
    for ry in range(19, 34, 5):
        inset = round((36 - ry) * 0.5)
        d.line((6 + inset + 2, ry, 74 - inset - 2, ry), fill=TERRACOTTA[1], width=1)
    # Door & windows + stone doorstep + corner foundation shrubs
    d.rectangle((34, 42, 46, 62), fill=WOOD[2], outline=INK_WARM)
    d.rectangle((32, 61, 48, 65), fill=ROCK[3], outline=INK_COOL)
    px(im, 43, 52, GOLD[2])
    for wx in (18, 52):
        d.rectangle((wx, 40, wx + 10, 50), fill=TEAL[2], outline=INK_COOL)
        d.rectangle((wx + 1, 41, wx + 5, 45), fill=ARCANE[3])
        d.rectangle((wx - 1, 50, wx + 11, 53), fill=FOLIAGE[2], outline=INK)
        px(im, wx + 3, 50, TERRACOTTA[3])
        px(im, wx + 7, 50, GOLD[2])
    draw_leaf_cluster(d, 13, 61, 4, 3)
    draw_leaf_cluster(d, 67, 61, 4, 3)
    save_2x("emerald_struct_house", im)

    # 6. Cursed Burned Tree Landmark (80x96 logical -> 160x192 px)
    im = new_img(80, 96)
    d = ImageDraw.Draw(im)
    d.ellipse((10, 74, 70, 92), fill=SHADOW)
    d.polygon([(26, 42), (54, 42), (62, 84), (40, 78), (18, 84)], fill=SWAMP_GROUND[1], outline=INK)
    d.polygon([(30, 34), (50, 34), (52, 78), (28, 78)], fill=SWAMP_GROUND[2])
    for bx, by in ((10, 18), (68, 16), (22, 8), (58, 10), (40, 4)):
        d.line((40, 38, bx, by), fill=INK, width=5)
        d.line((40, 38, bx, by), fill=SWAMP_GROUND[3], width=2)
    # Glowing acid scar
    d.polygon([(36, 48), (44, 48), (42, 68), (38, 68)], fill=ACID[3], outline=INK)
    d.line((40, 50, 40, 65), fill=ACID[1], width=2)
    save_2x("emerald_struct_burned_tree", im)


def draw_shaded_blob(d: ImageDraw.ImageDraw, cx: int, cy: int, rx: int, ry: int, pal) -> None:
    d.ellipse((cx - rx - 1, cy - ry - 1, cx + rx + 1, cy + ry + 1), fill=INK)
    d.ellipse((cx - rx, cy - ry, cx + rx, cy + ry), fill=pal[1])
    d.ellipse((cx - rx + 1, cy - ry + 1, cx + rx - 2, cy + ry - 2), fill=pal[2])
    d.ellipse((cx - rx + 2, cy - ry + 1, cx + rx // 2, cy), fill=pal[3])


def generate_enemy_atlases() -> None:
    # Helper builds 4 cols x 8 direction rows
    def build_atlas(name: str, cw: int, ch: int, draw_actor) -> None:
        sheet = new_img(4 * cw, 8 * ch)
        for row, (dx, dy) in enumerate(DIRECTIONS):
            for col in range(4):
                cell = new_img(cw, ch)
                draw_actor(ImageDraw.Draw(cell), cell, col, dx, dy, cw, ch)
                sheet.alpha_composite(cell, (col * cw, row * ch))
        save_2x(name, sheet)

    # 1. Swarm Bat (32x32 logical -> 64x64)
    def draw_bat(d, cell, col, dx, dy, w, h):
        cx, cy = w // 2, h // 2
        flap = (-5, -1, 4, 0)[col]
        bob = (0, 1, 2, 1)[col]
        d.ellipse((cx - 8, h - 6, cx + 8, h - 2), fill=SHADOW)
        # Wings
        wing_span = 11 if col % 2 == 0 else 9
        d.polygon([(cx - 3, cy + bob), (cx - wing_span, cy - 4 + flap), (cx - wing_span + 2, cy + 5 + flap)], fill=CRIMSON[1], outline=INK)
        d.polygon([(cx + 3, cy + bob), (cx + wing_span, cy - 4 + flap), (cx + wing_span - 2, cy + 5 + flap)], fill=CRIMSON[1], outline=INK)
        draw_shaded_blob(d, cx, cy + bob, 6, 5, CRIMSON)
        # Pointed ears
        d.polygon([(cx - 5, cy - 3 + bob), (cx - 4, cy - 9 + bob), (cx - 1, cy - 4 + bob)], fill=CRIMSON[2], outline=INK)
        d.polygon([(cx + 5, cy - 3 + bob), (cx + 4, cy - 9 + bob), (cx + 1, cy - 4 + bob)], fill=CRIMSON[2], outline=INK)
        if dy >= 0:
            ex = cx + dx * 2
            px(cell, ex - 2, cy + bob, GOLD[3])
            px(cell, ex + 2, cy + bob, GOLD[3])

    build_atlas("enemy_bat_atlas", 32, 32, draw_bat)

    # 2. Curupira Imp (32x32 logical -> 64x64)
    def draw_imp(d, cell, col, dx, dy, w, h):
        cx, cy = w // 2, h // 2 + 2
        stride = (-2, 0, 2, 0)[col]
        bob = -abs(stride) // 2
        d.ellipse((cx - 8, h - 6, cx + 8, h - 2), fill=SHADOW)
        # Feet
        d.rectangle((cx - 5 + stride, h - 8, cx - 2 + stride, h - 4), fill=WOOD[1], outline=INK)
        d.rectangle((cx + 2 - stride, h - 8, cx + 5 - stride, h - 4), fill=WOOD[1], outline=INK)
        # Leaf tunic & fiery hair
        draw_shaded_blob(d, cx, cy + 3 + bob, 6, 5, FOLIAGE)
        draw_shaded_blob(d, cx, cy - 4 + bob, 6, 5, DIRT)
        for fx in (-4, -1, 2, 4):
            d.polygon([(cx + fx - 2, cy - 7 + bob), (cx + fx, cy - 13 + bob - (col % 2)), (cx + fx + 2, cy - 7 + bob)], fill=FIRE[2], outline=INK)
        # Wooden spear
        sx = cx + (8 if dx >= 0 else -8)
        d.line((sx, cy - 6 + bob, sx, cy + 8 + bob), fill=WOOD[3], width=2)
        d.polygon([(sx - 2, cy - 6 + bob), (sx, cy - 11 + bob), (sx + 2, cy - 6 + bob)], fill=LEAF_MAGIC[3], outline=INK)
        if dy >= 0:
            ex = cx + dx * 2
            px(cell, ex - 2, cy - 4 + bob, GOLD[3])
            px(cell, ex + 2, cy - 4 + bob, GOLD[3])

    build_atlas("enemy_imp_atlas", 32, 32, draw_imp)

    # 3. Moss Golem (48x48 logical -> 96x96)
    def draw_golem(d, cell, col, dx, dy, w, h):
        cx, cy = w // 2, h // 2 + 2
        stomp = (-2, 0, 2, 0)[col]
        bob = abs(stomp) // 2
        d.ellipse((cx - 16, h - 8, cx + 16, h - 2), fill=SHADOW)
        # Heavy stone legs
        d.rectangle((cx - 11, h - 14 + stomp, cx - 3, h - 4), fill=ROCK[1], outline=INK)
        d.rectangle((cx + 3, h - 14 - stomp, cx + 11, h - 4), fill=ROCK[1], outline=INK)
        # Broad boulder torso & moss shoulder pads
        draw_shaded_blob(d, cx, cy + bob, 14, 12, ROCK)
        draw_leaf_cluster(d, cx - 10, cy - 8 + bob, 7, 5)
        draw_leaf_cluster(d, cx + 10, cy - 8 + bob, 7, 5)
        # Arms
        d.rectangle((cx - 19, cy - 4 + bob - stomp, cx - 13, cy + 10 + bob - stomp), fill=ROCK[2], outline=INK)
        d.rectangle((cx + 13, cy - 4 + bob + stomp, cx + 19, cy + 10 + bob + stomp), fill=ROCK[2], outline=INK)
        # Glowing emerald core & eyes
        if dy >= 0:
            ex = cx + dx * 3
            d.rectangle((ex - 5, cy - 5 + bob, ex - 2, cy - 3 + bob), fill=LEAF_MAGIC[3])
            d.rectangle((ex + 2, cy - 5 + bob, ex + 5, cy - 3 + bob), fill=LEAF_MAGIC[3])
            d.ellipse((cx - 3, cy + 1 + bob, cx + 3, cy + 7 + bob), fill=LEAF_MAGIC[2], outline=INK)

    build_atlas("enemy_golem_atlas", 48, 48, draw_golem)

    # 4. Vine Shaman (32x32 logical -> 64x64)
    def draw_shaman(d, cell, col, dx, dy, w, h):
        cx, cy = w // 2, h // 2 + 2
        bob = (0, -1, 0, 1)[col]
        d.ellipse((cx - 9, h - 6, cx + 9, h - 2), fill=SHADOW)
        # Feathered indigo cloak
        d.polygon([(cx - 8, h - 4), (cx - 6, cy - 2 + bob), (cx + 6, cy - 2 + bob), (cx + 8, h - 4)], fill=TEAL[1], outline=INK)
        draw_shaded_blob(d, cx, cy - 5 + bob, 6, 6, ACID[3:] + [ACID[5]])
        # Bone mask & feathered crown
        for fx in (-5, 0, 5):
            d.line((cx + fx, cy - 10 + bob, cx + fx, cy - 14 + bob), fill=GOLD[2], width=2)
        if dy >= 0:
            ex = cx + dx * 2
            d.rectangle((ex - 4, cy - 7 + bob, ex + 4, cy - 2 + bob), fill=CREAM[2], outline=INK)
            px(cell, ex - 2, cy - 5 + bob, ACID[1])
            px(cell, ex + 2, cy - 5 + bob, ACID[1])
        # Staff
        sx = cx + (9 if dx >= 0 else -9)
        d.line((sx, cy - 9 + bob, sx, h - 4), fill=WOOD[2], width=2)
        d.ellipse((sx - 2, cy - 12 + bob, sx + 2, cy - 8 + bob), fill=ACID[1], outline=INK)

    build_atlas("enemy_shaman_atlas", 32, 32, draw_shaman)

    # 5. Swamp Sentinel Mini-Boss (48x48 logical -> 96x96)
    def draw_sentinel(d, cell, col, dx, dy, w, h):
        cx, cy = w // 2, h // 2 + 2
        bob = (-1, 0, 1, 0)[col]
        d.ellipse((cx - 17, h - 8, cx + 17, h - 2), fill=SHADOW)
        d.polygon([(cx - 15, h - 4), (cx - 12, cy - 6 + bob), (cx + 12, cy - 6 + bob), (cx + 15, h - 4)], fill=WOOD[1], outline=INK)
        draw_shaded_blob(d, cx, cy - 3 + bob, 14, 13, WOOD)
        draw_leaf_cluster(d, cx, cy - 15 + bob, 13, 7)
        # Acid lanterns hanging from branch arms
        for side in (-1, 1):
            ax = cx + side * 17
            d.line((cx + side * 10, cy - 4 + bob, ax, cy + 4 + bob), fill=WOOD[2], width=3)
            d.ellipse((ax - 3, cy + 4 + bob, ax + 3, cy + 11 + bob), fill=ACID[1], outline=INK)
        if dy >= 0:
            ex = cx + dx * 3
            d.rectangle((ex - 5, cy - 7 + bob, ex - 2, cy - 4 + bob), fill=ACID[2])
            d.rectangle((ex + 2, cy - 7 + bob, ex + 5, cy - 4 + bob), fill=ACID[2])

    build_atlas("enemy_sentinel_atlas", 48, 48, draw_sentinel)

    # 6. Ancient Wraith Boss (48x48 logical -> 96x96)
    def draw_wraith(d, cell, col, dx, dy, w, h):
        cx, cy = w // 2, h // 2
        hover = (-2, -1, 1, 0)[col]
        d.ellipse((cx - 14, h - 7, cx + 14, h - 2), fill=SHADOW)
        # Tattered crimson-violet spectral cloak
        d.polygon(
            [(cx - 14, h - 7 + hover), (cx - 10, cy - 6 + hover), (cx + 10, cy - 6 + hover),
             (cx + 14, h - 7 + hover), (cx + 6, h - 11 + hover), (cx, h - 5 + hover), (cx - 6, h - 11 + hover)],
            fill=CRIMSON[1], outline=INK,
        )
        draw_shaded_blob(d, cx, cy - 7 + hover, 10, 10, [CRIMSON[0], ACID[3], ACID[4], ACID[5]])
        # Golden horned crown
        d.polygon([(cx - 10, cy - 12 + hover), (cx - 13, cy - 21 + hover), (cx - 4, cy - 15 + hover),
                   (cx, cy - 22 + hover), (cx + 4, cy - 15 + hover), (cx + 13, cy - 21 + hover), (cx + 10, cy - 12 + hover)],
                  fill=GOLD[2], outline=INK)
        # Orbiting soul crystal
        ox = cx + round(math.cos(col * math.pi / 2) * 18)
        oy = cy + hover + round(math.sin(col * math.pi / 2) * 5)
        d.polygon([(ox, oy - 4), (ox + 3, oy), (ox, oy + 4), (ox - 3, oy)], fill=ARCANE[3], outline=INK)
        if dy >= 0:
            ex = cx + dx * 2
            d.rectangle((ex - 5, cy - 8 + hover, ex - 2, cy - 6 + hover), fill=GOLD[3])
            d.rectangle((ex + 2, cy - 8 + hover, ex + 5, cy - 6 + hover), fill=GOLD[3])

    build_atlas("enemy_wraith_atlas", 48, 48, draw_wraith)


def generate_pickups_and_vfx() -> None:
    # emerald_pickups.png: 8 x 16x16 logical -> 256 x 32 px
    def draw_pickup(d, cell, i, w, h):
        cx, cy = 8, 8
        d.ellipse((cx - 5, h - 4, cx + 5, h - 1), fill=SHADOW)
        if i in (0, 1, 2):
            pal = (ARCANE, LEAF_MAGIC, GOLD)[i]
            pts = [(cx, 2), (cx + 5, 7), (cx, 13), (cx - 5, 7)]
            d.polygon(pts, fill=pal[2], outline=INK)
            d.polygon([(cx, 3), (cx + 3, 7), (cx, 11)], fill=pal[3])
            px(cell, cx - 1, 6, WHITE)
        elif i == 3:  # erva
            for ang in (-0.5, 0.0, 0.5):
                d.ellipse((cx - 4 + round(ang * 4), 4, cx + 2 + round(ang * 4), 12), fill=FOLIAGE[3], outline=INK)
            px(cell, cx, 7, FOLIAGE[4])
        elif i == 4:  # mana flask
            d.rectangle((6, 2, 10, 4), fill=GOLD[2], outline=INK)
            d.ellipse((4, 5, 12, 13), fill=ARCANE[2], outline=INK)
            d.ellipse((6, 6, 10, 10), fill=ARCANE[4])
        elif i == 5:  # bencao
            d.ellipse((3, 3, 13, 13), fill=GOLD[2], outline=INK)
            d.polygon([(cx, 5), (cx + 3, 8), (cx, 11), (cx - 3, 8)], fill=LEAF_MAGIC[3])
        elif i == 6:  # cajado
            d.line((4, 13, 12, 3), fill=WOOD[3], width=2)
            d.ellipse((9, 1, 14, 6), fill=LEAF_MAGIC[3], outline=INK)
        else:  # coco
            d.ellipse((3, 4, 13, 13), fill=WOOD[3], outline=INK_WARM)
            px(cell, 7, 9, INK_WARM); px(cell, 9, 9, INK_WARM); px(cell, 8, 11, INK_WARM)
            d.polygon([(10, 4), (14, 2), (13, 6)], fill=FOLIAGE[3])

    save_strip_2x("emerald_pickups", 8, 16, 16, draw_pickup)

    # emerald_vfx_orbs.png: 4 x 24x24 logical -> 192 x 48 px
    def draw_orb(d, cell, i, w, h):
        cx, cy = 12, 12
        d.ellipse((cx - 8, cy - 8, cx + 8, cy + 8), fill=ARCANE[1], outline=INK_COOL)
        d.ellipse((cx - 6, cy - 6, cx + 6, cy + 6), fill=ARCANE[2])
        d.ellipse((cx - 4, cy - 4, cx + 4, cy + 4), fill=ARCANE[3])
        ang = i * math.pi / 2
        for k in range(3):
            a = ang + k * 2.1
            sx, sy = round(cx + math.cos(a) * 6), round(cy + math.sin(a) * 6)
            d.rectangle((sx - 1, sy - 1, sx + 1, sy + 1), fill=ARCANE[4] if k == 0 else LEAF_MAGIC[3])
        px(cell, cx - 2, cy - 2, WHITE)

    save_strip_2x("emerald_vfx_orbs", 4, 24, 24, draw_orb)

    # emerald_vfx_lightning.png: 4 x 32x64 logical -> 256 x 128 px
    def draw_lightning(d, cell, i, w, h):
        cx = 16
        r = 6 + i * 3
        d.ellipse((cx - r, h - 10 - r // 3, cx + r, h - 10 + r // 3), outline=GOLD[2], width=2)
        if i < 3:
            pts = [(cx + ((i * 3 + k) % 5) - 2, k * 10) for k in range(6)]
            d.line(pts, fill=LEAF_MAGIC[3], width=4)
            d.line(pts, fill=GOLD[3], width=2)
            d.line(pts, fill=WHITE, width=1)

    save_strip_2x("emerald_vfx_lightning", 4, 32, 64, draw_lightning)

    # emerald_parry_nova.png: 5 x 64x64 logical -> 640 x 128 px
    def draw_nova(d, cell, i, w, h):
        cx, cy = 32, 32
        r = 10 + i * 5
        d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=INK, width=4)
        d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=LEAF_MAGIC[3] if i % 2 == 0 else GOLD[2], width=2)
        for k in range(8):
            a = k * math.pi / 4 + i * 0.2
            x1, y1 = round(cx + math.cos(a) * (r - 3)), round(cy + math.sin(a) * (r - 3))
            x2, y2 = round(cx + math.cos(a) * (r + 3)), round(cy + math.sin(a) * (r + 3))
            d.line((x1, y1, x2, y2), fill=GOLD[3], width=1)

    save_strip_2x("emerald_parry_nova", 5, 64, 64, draw_nova)


def generate_combat_evolution_vfx() -> None:
    """Cohesive 2x GBA pixel-art strips for Staff Vortex, Curupira Dash embers, and Living Bark Aegis."""
    # 1. emerald_vfx_vortex.png: 4 x 32x32 logical -> 256 x 64 px (Encontro das Aguas whirlpool)
    def draw_vortex(d, cell, frame, w, h):
        cx, cy = 16, 16
        d.ellipse((1, 1, 30, 30), outline=INK_COOL, width=2)
        d.ellipse((3, 3, 28, 28), fill=(NEGRO[0][0], NEGRO[0][1], NEGRO[0][2], 175), outline=NEGRO[2], width=1)
        spin = frame * (math.pi / 2)
        for arm in range(4):
            pal = NEGRO if arm % 2 == 0 else SOLIMOES
            base_a = spin + arm * (math.pi / 2)
            for step in range(9):
                t = step / 8.0
                r = 3.0 + t * 11.5
                a = base_a + t * 1.65
                sx = round(cx + math.cos(a) * r)
                sy = round(cy + math.sin(a) * r)
                c = pal[4] if t > 0.72 else pal[3] if t > 0.35 else CREAM[2]
                d.rectangle((sx - 1, sy - 1, sx + 1, sy + 1), fill=c)
                if step % 3 == 0:
                    px(cell, sx + 1, sy, WHITE if t > 0.6 else CREAM[1])
        d.ellipse((cx - 4, cy - 4, cx + 4, cy + 4), fill=ARCANE[3], outline=INK_COOL)
        px(cell, cx - 1, cy - 1, WHITE)

    save_strip_2x("emerald_vfx_vortex", 4, 32, 32, draw_vortex)

    # 2. emerald_vfx_dash.png: 4 x 32x32 logical -> 256 x 64 px (Passo de Curupira ember/vine trail)
    def draw_dash(d, cell, frame, w, h):
        cx, cy = 16, 16
        rng = random.Random(700 + frame * 19)
        r = 12 - frame * 2
        d.ellipse((cx - r - 1, cy - r + 2, cx + r + 1, cy + r - 1), outline=INK, width=2)
        d.ellipse((cx - r, cy - r + 3, cx + r, cy + r - 2), fill=(FIRE[1][0], FIRE[1][1], FIRE[1][2], 165))
        for k in range(7):
            a = k * (math.pi * 2 / 7) + frame * 0.45
            dist = 3 + (frame * 2 + k * 2) % 10
            ex = round(cx + math.cos(a) * dist)
            ey = round(cy + math.sin(a) * dist)
            col = FIRE[3] if (k + frame) % 2 == 0 else LEAF_MAGIC[3]
            d.rectangle((ex - 1, ey - 1, ex + 1, ey + 1), fill=col, outline=INK)
        for _ in range(5):
            fx = rng.randint(6, 25)
            fy = rng.randint(6, 25)
            px(cell, fx, fy, FIRE[4])

    save_strip_2x("emerald_vfx_dash", 4, 32, 32, draw_dash)

    # 3. emerald_aegis_ring.png: 4 x 32x32 logical -> 256 x 64 px (Manto da Castanheira living bark shield)
    def draw_aegis(d, cell, frame, w, h):
        cx, cy = 16, 16
        d.ellipse((2, 2, 29, 29), outline=INK, width=2)
        d.ellipse((3, 3, 28, 28), outline=GOLD[2] if frame % 2 == 0 else FOLIAGE[3], width=1)
        for k in range(3):
            a = frame * (math.pi / 2) + k * (math.pi * 2 / 3)
            lx = round(cx + math.cos(a) * 12)
            ly = round(cy + math.sin(a) * 12)
            d.ellipse((lx - 3, ly - 2, lx + 3, ly + 2), fill=FOLIAGE[3], outline=INK)
            px(cell, lx - 1, ly - 1, GOLD[3])
        px(cell, cx - 12, cy, GOLD[2])
        px(cell, cx + 12, cy, GOLD[2])

    save_strip_2x("emerald_aegis_ring", 4, 32, 32, draw_aegis)


def generate_hud_joystick() -> None:
    # Matches exact 72x48 sprite contract read by Joystick.java (0..48 base, 48..72 knob)
    im = new_img(72, 48)
    d = ImageDraw.Draw(im)
    # Base ring
    d.ellipse((2, 2, 45, 45), fill=(16, 36, 26, 150), outline=GOLD[1], width=2)
    d.ellipse((5, 5, 42, 42), outline=GRASS[3], width=1)
    for x, y in ((24, 7), (24, 40), (7, 24), (40, 24)):
        d.rectangle((x - 1, y - 1, x + 1, y + 1), fill=GOLD[2])
    # Knob
    d.ellipse((49, 1, 70, 22), fill=INK_COOL, outline=GOLD[2], width=2)
    d.ellipse((51, 3, 68, 20), fill=FOLIAGE[2], outline=LEAF_MAGIC[3], width=1)
    px(im, 56, 7, GOLD[3])
    im.save(OUT / "hud_joystick.png", optimize=True)


def main() -> None:
    generate_grass_tiles()
    generate_terrain_autotiles()
    generate_scenery_trees()
    generate_castanheira()
    generate_buildings()
    generate_enemy_atlases()
    generate_pickups_and_vfx()
    generate_combat_evolution_vfx()
    generate_hud_joystick()
    print("Generated cohesive Pokemon-Emerald-style 2x asset suite in", OUT)


if __name__ == "__main__":
    main()
