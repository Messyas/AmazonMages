"""Prepare and normalize v4 UI source sheets (skill medallions, skill icons, craft icons,
and cauldron states) into 8x HD magenta-keyed strips for deterministic downsampling via
tools/pixelize/pixelize.py.

Run from repo root: python tools/pixelize/prepare_ui_v4.py
"""
from pathlib import Path
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "art" / "source" / "ui-v4"
HD = ROOT / "art" / "hd"


def foreground_mask(img: Image.Image, tol: float = 95.0) -> np.ndarray:
    """True for artwork pixels, False for #FF00FF magenta background."""
    rgb = np.asarray(img.convert("RGB"), dtype=np.int32)
    dist = np.sqrt(((rgb - np.array([255, 0, 255])) ** 2).sum(axis=-1))
    return dist > tol


def bbox(mask: np.ndarray):
    ys, xs = np.where(mask)
    if len(xs) == 0:
        raise ValueError("Empty cell in source sprite sheet")
    return int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1


def clean_magenta(strip: Image.Image, tol: float = 95.0) -> Image.Image:
    rgb = np.asarray(strip.convert("RGB")).copy()
    mask = foreground_mask(strip, tol=tol)
    rgb[~mask] = [255, 0, 255]
    return Image.fromarray(rgb, "RGB")


def split_grid(img: Image.Image, cols: int, rows: int):
    cells = []
    w, h = img.size
    for r in range(rows):
        for c in range(cols):
            box = (
                round(c * w / cols),
                round(r * h / rows),
                round((c + 1) * w / cols),
                round((r + 1) * h / rows),
            )
            cells.append(img.crop(box))
    return cells


def pack_grid_strip(
    source_name: str,
    out_name: str,
    cols_src: int,
    rows_src: int,
    cell_w_hd: int,
    cell_h_hd: int,
    margin_hd: int = 8,
    shared_box: bool = False,
):
    raw = Image.open(SRC / source_name).convert("RGB")
    cells = split_grid(raw, cols_src, rows_src)
    total = len(cells)
    strip = Image.new("RGB", (cell_w_hd * total, cell_h_hd), (255, 0, 255))

    if shared_box:
        masks = [foreground_mask(c) for c in cells]
        boxes = [bbox(m) for m in masks]
        ub = (
            min(b[0] for b in boxes),
            min(b[1] for b in boxes),
            max(b[2] for b in boxes),
            max(b[3] for b in boxes),
        )
        crops = [c.crop(ub) for c in cells]
    else:
        crops = [c.crop(bbox(foreground_mask(c))) for c in cells]

    max_w = cell_w_hd - margin_hd * 2
    max_h = cell_h_hd - margin_hd * 2
    for idx, crop in enumerate(crops):
        cw, ch = crop.size
        scale = min(max_w / cw, max_h / ch)
        nw = max(1, round(cw * scale))
        nh = max(1, round(ch * scale))
        resized = crop.resize((nw, nh), Image.Resampling.LANCZOS)
        ox = idx * cell_w_hd + (cell_w_hd - nw) // 2
        oy = (cell_h_hd - nh) // 2
        strip.paste(resized, (ox, oy))

    cleaned = clean_magenta(strip)
    cleaned.save(HD / f"{out_name}.png")
    print(f"{out_name}: {raw.size} -> {cleaned.size} ({total} frames)")


def pack_paired_cauldron():
    idle = Image.open(SRC / "cauldron_idle.png").convert("RGB")
    active = Image.open(SRC / "cauldron_active.png").convert("RGB")
    if idle.size != active.size:
        active = active.resize(idle.size, Image.Resampling.LANCZOS)

    b0 = bbox(foreground_mask(idle))
    b1 = bbox(foreground_mask(active))
    ub = (
        min(b0[0], b1[0]),
        min(b0[1], b1[1]),
        max(b0[2], b1[2]),
        max(b0[3], b1[3]),
    )
    cell_w, cell_h = 48 * 8, 48 * 8  # 384x384 HD -> 48x48 logical
    margin = 16
    cw, ch = ub[2] - ub[0], ub[3] - ub[1]
    scale = min((cell_w - margin * 2) / cw, (cell_h - margin * 2) / ch)
    nw, nh = max(1, round(cw * scale)), max(1, round(ch * scale))
    ox = (cell_w - nw) // 2
    oy = cell_h - margin - nh  # bottom-anchor so pot logs sit firmly on ground

    strip = Image.new("RGB", (cell_w * 2, cell_h), (255, 0, 255))
    for i, img in enumerate((idle, active)):
        cropped = img.crop(ub).resize((nw, nh), Image.Resampling.LANCZOS)
        strip.paste(cropped, (i * cell_w + ox, oy))

    cleaned = clean_magenta(strip)
    cleaned.save(HD / "ui_craft_cauldron.png")
    print(f"ui_craft_cauldron: {idle.size} -> {cleaned.size} (2 shared-box frames)")


def main():
    HD.mkdir(parents=True, exist_ok=True)
    # 2x2 medallions -> 4x1 strip of 224x224 HD (28x28 logical)
    pack_grid_strip(
        "skill_medallions_2x2.png", "ui_skill_medallion",
        cols_src=2, rows_src=2, cell_w_hd=28 * 8, cell_h_hd=28 * 8,
        margin_hd=8, shared_box=False,
    )
    # 3x3 skill icons -> 9x1 strip of 160x160 HD (20x20 logical)
    pack_grid_strip(
        "skill_icons_3x3.png", "ui_skill_icons",
        cols_src=3, rows_src=3, cell_w_hd=20 * 8, cell_h_hd=20 * 8,
        margin_hd=8, shared_box=False,
    )
    # 3x2 craft icons -> 6x1 strip of 160x160 HD (20x20 logical)
    pack_grid_strip(
        "craft_icons_3x2.png", "ui_craft_icons",
        cols_src=3, rows_src=2, cell_w_hd=20 * 8, cell_h_hd=20 * 8,
        margin_hd=8, shared_box=False,
    )
    # 2 paired cauldron states -> 2x1 strip of 384x384 HD (48x48 logical)
    pack_paired_cauldron()


if __name__ == "__main__":
    main()

