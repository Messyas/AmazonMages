"""Build the Boitata atlases and derive the ember phase from the shared poses."""

import argparse
import shutil

import numpy as np
from PIL import Image

from pixelize import ART, RES, build_asset, hex_to_rgb, load_manifest, verify


EMBER_COLORS = {
    "#111F51": "#1B221D",
    "#172D70": "#292B2E",
    "#23408B": "#29352A",
    "#2259B4": "#3F5034",
    "#2974E3": "#52633A",
    "#428BEC": "#81462A",
    "#5489BA": "#B66332",
    "#08489F": "#29352A",
    "#0967DB": "#3F5034",
    "#168FFF": "#52633A",
    "#35B8FF": "#DB873D",
    "#56E2FF": "#FF891F",
    "#8FEDFC": "#FFC043",
    "#B9F0FF": "#FFF0A4",
    "#DDF9FF": "#FFFDF4",
}

FIREBALL_EMBER_COLORS = {
    **EMBER_COLORS,
    "#08489F": "#AF321D",
    "#0967DB": "#E95120",
    "#168FFF": "#FF891F",
    "#56E2FF": "#FFC043",
}

EMBER_VARIANTS = (
    "boitata_orbit",
    "boitata_bite",
    "boitata_cast",
    "boitata_encircle",
    "boitata_recover",
    "boitata_defeat",
    "boitata_fireball",
)


def to_ember(image, colors):
    pixels = np.asarray(image)
    ember = pixels.copy()
    for cold, warm in colors.items():
        selected = (pixels[..., 3] > 0) & np.all(pixels[..., :3] == hex_to_rgb(cold), axis=-1)
        ember[selected, :3] = hex_to_rgb(warm)
    return Image.fromarray(ember, "RGBA")


def save_atlas(name, spec, atlas, install):
    path = ART / "build" / f"{name}.png"
    atlas.save(path, optimize=True)
    problems = verify(name, spec, path, spec["palette"])
    if problems:
        raise ValueError(f"{name}: {'; '.join(problems)}")
    if install:
        shutil.copyfile(path, RES / path.name)
    print(f"[ok] {name} {atlas.width}x{atlas.height}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--install", action="store_true", help="copy validated atlases to drawable-nodpi")
    args = parser.parse_args()
    (ART / "build").mkdir(parents=True, exist_ok=True)
    if args.install:
        RES.mkdir(parents=True, exist_ok=True)

    _, assets = load_manifest()
    for name, spec in assets.items():
        if not name.startswith("boitata_"):
            continue
        atlas = build_asset(name, spec, spec["palette"], print)
        save_atlas(name, spec, atlas, args.install)
        if name in EMBER_VARIANTS:
            ember_spec = {**spec, "ramps": ["stone", "moss", "ember", "ivory"]}
            colors = FIREBALL_EMBER_COLORS if name == "boitata_fireball" else EMBER_COLORS
            save_atlas(f"{name}_ember", ember_spec, to_ember(atlas, colors), args.install)


if __name__ == "__main__":
    main()
