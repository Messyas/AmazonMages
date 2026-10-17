"""Prepare the pixel-art menu background, retaining the HD logo and portrait."""
import argparse
import shutil
from pathlib import Path

import numpy as np
from PIL import Image, ImageOps
from pixelize import build_asset, load_manifest

ROOT = Path(__file__).resolve().parents[2]
ART = ROOT / 'art'


def chroma_key(image):
    a = np.asarray(image.convert('RGBA')).copy()
    rgb = a[:, :, :3].astype(np.int32)
    mask = np.linalg.norm(rgb - [255, 0, 255], axis=2) < 100
    a[mask] = [0, 0, 0, 0]
    return Image.fromarray(a)


def main(background_only=False):
    destination = ART / 'main-menu-v3/assets'
    destination.mkdir(parents=True, exist_ok=True)
    names = ('bg_menu',) if background_only else ('bg_menu', 'ui_logo', 'portrait_player')
    for name in names:
        raw = Image.open(ART / 'source/main-menu-v3' / (name+'.png')).convert('RGBA')
        if name == 'bg_menu':
            # 1200x540 HD -> 400x180 logical: exactly 3x downsampling on both axes.
            # The opaque scene uses a fixed 48-colour palette extracted from its approved source.
            _, assets = load_manifest()
            source = ImageOps.fit(raw, tuple(assets[name]['source_cell']), Image.Resampling.LANCZOS)
            source.save(ART / 'hd' / 'bg_menu.png')
            final = build_asset(name, assets[name], 'menu_dusk', print)
        else:
            keyed = chroma_key(raw)
            if name == 'ui_logo':
                keyed = keyed.crop(keyed.getbbox())
                size, inner = (840, 180), (824, 172)
            else:
                size, inner = (1024, 1024), (1024, 1024)
            keyed = ImageOps.contain(keyed, inner, Image.Resampling.LANCZOS)
            final = Image.new('RGBA', size)
            final.alpha_composite(keyed, ((size[0]-keyed.width)//2, (size[1]-keyed.height)//2))
        final.save(destination / (name+'.png'))
        if name != 'bg_menu':
            shutil.copyfile(destination / (name+'.png'), ART / 'hd' / (name+'.png'))
        shutil.copyfile(destination / (name+'.png'), ROOT / 'app/src/main/res/drawable-nodpi' / (name+'.png'))
        print(f'{name}: {final.width}x{final.height}')
    if background_only:
        return
    shutil.copyfile(ART / 'ui-kit-v3/native/ui_button.png', ROOT / 'app/src/main/res/drawable-nodpi/ui_button.png')
    fonts = ROOT / 'app/src/main/res/font'
    fonts.mkdir(exist_ok=True)
    shutil.copyfile(ART / 'fonts/PixelifySans.ttf', fonts / 'pixelify_sans.ttf')
    licenses = ROOT / 'app/src/main/assets/licenses'
    licenses.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(ART / 'fonts/PixelifySans-OFL.txt', licenses / 'PixelifySans-OFL.txt')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--background-only', action='store_true')
    main(parser.parse_args().background_only)
