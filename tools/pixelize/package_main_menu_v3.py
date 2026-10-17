"""Package the title-screen assets and actual on-device evidence."""
import json
import shutil
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ART = ROOT / 'art'
OUT = ART / 'main-menu-v3'


def main():
    captures = {
        'main-menu-phone.png': ('capture-normal.png', 'preview-1200x540.png'),
        'main-menu-pressed.png': ('capture-pressed.png', 'preview-pressed-1200x540.png'),
        'main-menu-font-130.png': ('capture-font-130.png', None),
    }
    for source, (name, preview) in captures.items():
        path = ROOT / '.impeccable/review' / source
        shutil.copyfile(path, OUT / name)
        if preview:
            Image.open(path).resize((1200, 540), Image.Resampling.LANCZOS).save(OUT / preview)
    buttons = []
    for node in ET.parse(OUT / 'main-menu-layout.xml').iter('node'):
        if node.get('clickable') == 'true':
            buttons.append({'label': node.get('text'), 'bounds': node.get('bounds')})
    measurements = {
        'stage': '6.2', 'device': 'Moto G52', 'capture_size': [2400, 1080],
        'preview_size': [1200, 540], 'safe_area': [105, 0, 2400, 1020],
        'button_nine_slice_ltrb': [8, 6, 8, 6], 'button_frames': 3,
        'button_cell': [32, 20], 'button_touch_size_px': [602, 120],
        'font_scales_verified': [1.0, 1.3], 'buttons': buttons,
        'background': {'logical_size': [400, 180], 'hd_source_size': [1200, 540],
                       'downsample': 3, 'palette': 'menu_dusk', 'colors': 48,
                       'render': 'integer cover, nearest neighbor'},
        'hd_assets_preserved': ['ui_logo', 'portrait_player'],
        'assets': {p.name: list(Image.open(p).size) for p in (OUT / 'assets').glob('*.png')},
    }
    (OUT / 'measurements.json').write_text(json.dumps(measurements, indent=2, ensure_ascii=False)+'\n', encoding='utf-8')
    with zipfile.ZipFile(OUT / 'main-menu-v3.zip', 'w', zipfile.ZIP_DEFLATED) as package:
        for path in sorted((OUT / 'assets').glob('*.png')):
            package.write(path, path.relative_to(OUT))
        for name in ['README.md', 'sources.json', 'measurements.json',
                     'preview-1200x540.png', 'preview-pressed-1200x540.png']:
            package.write(OUT / name, name)
        package.write(ART / 'fonts/PixelifySans.ttf', 'font/PixelifySans.ttf')
        package.write(ART / 'fonts/PixelifySans-OFL.txt', 'font/OFL.txt')
        package.write(ART / 'ui-kit-v3/native/ui_button.png', 'ui_button.png')
        package.write(ART / 'palettes/menu_dusk.json', 'palette/menu_dusk.json')
    print('Packaged assets, actual screenshots and measured touch targets.')


if __name__ == '__main__':
    main()
