"""Processes AI-generated Sanctuary bastion and courtyard sigil sprites into crisp
GBA 2x pixel art with pure transparent backgrounds.
"""
from pathlib import Path
import numpy as np
from PIL import Image

BRAIN = Path(r"C:\Users\User\.gemini\antigravity\brain\cb175224-d598-4af5-828d-dfc3ae41a442")
OUT = Path(r"d:\android\AmazonMages\app\src\main\res\drawable-nodpi")
HD_DIR = Path(r"d:\android\AmazonMages\art\hd")
OUT.mkdir(parents=True, exist_ok=True)
HD_DIR.mkdir(parents=True, exist_ok=True)

BASTIONS_SRC = BRAIN / "sanctuary_bastions_2x2_v3_1795001994265.jpg"
SIGIL_SRC = BRAIN / "sanctuary_floor_sigil_1795001935580.jpg"

def is_magenta(rgb, tol=90.0):
    diff = np.sqrt(((rgb.astype(np.float32) - np.array([255, 0, 255], dtype=np.float32)) ** 2).sum(axis=-1))
    return diff <= tol

def process_bastions():
    im = Image.open(BASTIONS_SRC).convert("RGBA")
    w, h = im.size
    cols, rows = 2, 2
    cell_w, cell_h = w // cols, h // rows
    names = ["sanctuary_nw", "sanctuary_ne", "sanctuary_sw", "sanctuary_se"]

    for idx, name in enumerate(names):
        r, c = idx // cols, idx % cols
        cell = im.crop((c * cell_w, r * cell_h, (c + 1) * cell_w, (r + 1) * cell_h))
        rgb = np.asarray(cell.convert("RGB"))
        alpha = np.ones((cell_h, cell_w), dtype=np.uint8) * 255
        mag_mask = is_magenta(rgb, tol=110.0)
        alpha[mag_mask] = 0

        # Find bounding box of foreground
        ys, xs = np.where(alpha > 0)
        if len(xs) == 0:
            continue
        min_x, max_x = int(xs.min()), int(xs.max()) + 1
        min_y, max_y = int(ys.min()), int(ys.max()) + 1

        cropped_rgba = np.dstack([rgb, alpha])[min_y:max_y, min_x:max_x]
        cropped_img = Image.fromarray(cropped_rgba, "RGBA")

        # Logical target size: 80x88 logical -> 160x176 at 2x
        # First downscale with lanczos/bilinear to logical, then scale 2x nearest for crisp GBA pixel feel
        target_logical_w = 84
        target_logical_h = int(round(cropped_img.height * (target_logical_w / cropped_img.width)))
        logical = cropped_img.resize((target_logical_w, target_logical_h), Image.Resampling.LANCZOS)

        # Clean soft alpha into crisp binary mask (>= 128)
        logical_arr = np.asarray(logical).copy()
        logical_arr[logical_arr[:, :, 3] < 128, 3] = 0
        logical_arr[logical_arr[:, :, 3] >= 128, 3] = 255
        clean_logical = Image.fromarray(logical_arr, "RGBA")

        # 2x nearest upscale for GBA runtime
        runtime_2x = clean_logical.resize((target_logical_w * 2, target_logical_h * 2), Image.Resampling.NEAREST)
        out_path = OUT / f"emerald_struct_{name}.png"
        runtime_2x.save(out_path, optimize=True)
        print(f"Saved {out_path} ({runtime_2x.size})")

def process_sigil():
    im = Image.open(SIGIL_SRC).convert("RGBA")
    w, h = im.size
    rgb = np.asarray(im.convert("RGB"))
    alpha = np.ones((h, w), dtype=np.uint8) * 255
    mag_mask = is_magenta(rgb, tol=110.0)
    alpha[mag_mask] = 0

    ys, xs = np.where(alpha > 0)
    min_x, max_x = int(xs.min()), int(xs.max()) + 1
    min_y, max_y = int(ys.min()), int(ys.max()) + 1
    cropped = Image.fromarray(np.dstack([rgb, alpha])[min_y:max_y, min_x:max_x], "RGBA")

    # Target: 128x128 logical (256x256 runtime = 4x4 tiles of 64px)
    logical = cropped.resize((128, 128), Image.Resampling.LANCZOS)
    arr = np.asarray(logical).copy()
    arr[arr[:, :, 3] < 128, 3] = 0
    arr[arr[:, :, 3] >= 128, 3] = 255
    clean_logical = Image.fromarray(arr, "RGBA")
    runtime_2x = clean_logical.resize((256, 256), Image.Resampling.NEAREST)
    out_path = OUT / "emerald_sanctuary_sigil.png"
    runtime_2x.save(out_path, optimize=True)
    print(f"Saved {out_path} ({runtime_2x.size})")

if __name__ == "__main__":
    process_bastions()
    process_sigil()
