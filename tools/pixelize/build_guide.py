"""Build the original guide's sprites and derive her optional blue outfit."""

import argparse
import shutil

import numpy as np
from PIL import Image

from pixelize import ART, RES, SUPER, build_asset, hex_to_rgb, load_manifest, place_on_canvas, source_cells, verify


BLUE_OUTFIT = {
    "#6A8994": "#2F558B",
    "#94AEB7": "#3A73AC",
    "#C5D9E0": "#508DC2",
    "#E2EEF0": "#6BA6D8",
    "#F7FAF5": "#99C6E6",
}
SPEAKING_POSES = ("guide_greet", "guide_explain", "guide_warn", "guide_think")


def preserve_white_fabric(atlas, spec):
    """Keep thin white straps and fabric from disappearing in the palette vote."""
    cells = source_cells("guide_walk", spec)
    cw, ch = spec["cell"]
    scale = spec["scale"]
    bounds = [cell.getchannel("A").getbbox() for cell in cells.values()]
    shared_scale = min(
        cw * SUPER * spec["fill"] / max(box[2] - box[0] for box in bounds),
        ch * SUPER * spec["fill"] / max(box[3] - box[1] for box in bounds),
    )
    logical = np.array(atlas)[::scale, ::scale].copy()
    for (row, col), cell in cells.items():
        placed = np.asarray(place_on_canvas(cell, spec, shared_scale))
        red, green, blue, alpha = placed.astype(np.int16).transpose(2, 0, 1)
        cloth = (alpha >= 128) & (green >= 128) & (blue >= red - 8) & (green >= red - 6)
        coverage = cloth.reshape(ch, SUPER, cw, SUPER).mean(axis=(1, 3))
        target = logical[row * ch:(row + 1) * ch, col * cw:(col + 1) * cw]
        below_face = np.arange(ch)[:, None] >= round(ch * 0.31)
        edge = (coverage >= .16) & below_face & (target[..., 3] == 255)
        target[edge, :3] = hex_to_rgb("#C5D9E0")
        target[edge & (coverage >= .4), :3] = hex_to_rgb("#F7FAF5")
    return Image.fromarray(logical.repeat(scale, 0).repeat(scale, 1), "RGBA")


def blue_walk(atlas, cell_height):
    pixels = np.array(atlas)
    after_face = (np.arange(pixels.shape[0]) % cell_height >= round(cell_height * 0.64))[:, None]
    visible_outfit = (pixels[..., 3] > 0) & after_face
    original = pixels[..., :3].copy()
    for light, blue in BLUE_OUTFIT.items():
        selected = visible_outfit & np.all(original == hex_to_rgb(light), axis=-1)
        pixels[selected, :3] = hex_to_rgb(blue)
    return Image.fromarray(pixels, "RGBA")


def blue_portrait(portrait):
    pixels = np.array(portrait)
    red, green, blue = pixels[..., :3].astype(np.int16).transpose(2, 0, 1)
    visible_outfit = ((pixels[..., 3] > 0) & (green > 120) & (blue >= red - 8)
                      & (green >= red - 6) & (np.arange(pixels.shape[0])[:, None] >= pixels.shape[0] * .28))
    shade = green[visible_outfit] / 255
    pixels[visible_outfit, :3] = np.column_stack((15 + 121 * shade, 45 + 145 * shade, 78 + 154 * shade))
    return Image.fromarray(pixels, "RGBA")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--install", action="store_true")
    args = parser.parse_args()
    _, assets = load_manifest()
    build_dir = ART / "build"
    build_dir.mkdir(parents=True, exist_ok=True)
    if args.install:
        RES.mkdir(parents=True, exist_ok=True)
    for name in ("guide_walk", "guide_portrait", *SPEAKING_POSES):
        spec = assets[name]
        palette = spec.get("palette", "amazon")
        original = build_asset(name, spec, palette, print)
        if name == "guide_walk":
            original = preserve_white_fabric(original, spec)
            variant = blue_walk(original, spec["cell"][1] * spec["scale"])
        else:
            variant = blue_portrait(original)
        base = build_dir / f"{name}.png"
        original.save(base, optimize=True)
        problems = verify(name, spec, base, palette)
        if problems:
            raise SystemExit(f"{name}: {', '.join(problems)}")
        alt = build_dir / f"{name}_blue.png"
        variant.save(alt, optimize=True)
        for path in (base, alt):
            if args.install:
                shutil.copyfile(path, RES / path.name)
            print(f"[ok] {path.name} {original.width}x{original.height}")


if __name__ == "__main__":
    main()
