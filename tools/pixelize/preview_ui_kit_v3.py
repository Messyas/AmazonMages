"""Contact sheet and delivery package for the v3 component kit; no APK changes."""
import json
import shutil
import zipfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
ART = ROOT / 'art'
OUT = ART / 'ui-kit-v3'
FONT = ART / 'fonts/PixelifySans.ttf'
MARGINS = {
    'ui_panel': [10, 10, 10, 10], 'ui_panel_title': [8, 7, 8, 7],
    'ui_button': [8, 6, 8, 6], 'ui_tab': [8, 6, 8, 6],
    'ui_bar_frame': [4, 3, 4, 3], 'ui_perf_overlay': [4, 4, 4, 4],
}


def main():
    records = json.loads((OUT / 'sources.json').read_text(encoding='utf-8'))['assets']
    sizes = json.loads((OUT / 'measurements.json').read_text(encoding='utf-8'))
    native = OUT / 'native'
    native.mkdir(exist_ok=True)
    hd = OUT / 'hd'
    hd.mkdir(exist_ok=True)
    board = Image.new('RGB', (1200, 1200), '#0B1A12')
    draw = ImageDraw.Draw(board)
    title = ImageFont.truetype(str(FONT), 32)
    label = ImageFont.truetype(str(FONT), 19)
    small = ImageFont.truetype(str(FONT), 16)
    draw.text((28, 16), 'AmazonMages · Kit de componentes v3', font=title, fill='#F2D36B')
    draw.text((28, 57), 'Arte real reduzida · Ampliação inteira por vizinho mais próximo', font=small, fill='#B8C99A')
    for i, record in enumerate(records):
        name = record['name']
        img = Image.open(ART / 'build' / (name+'.png')).convert('RGBA')
        shutil.copyfile(ART / 'build' / (name+'.png'), native / (name+'.png'))
        # Final HD delivery is the cleaned palette result at exactly 8x, on magenta.
        # Raw generator originals and pre-reduction normalization remain available.
        clean_hd = Image.new('RGB', (img.width*8, img.height*8), '#FF00FF')
        enlarged = img.resize(clean_hd.size, Image.Resampling.NEAREST)
        clean_hd.paste(enlarged, (0, 0), enlarged)
        clean_hd.save(hd / (name+'.png'))
        if name == 'ui_icons':
            x, y, width, height, zoom = 28, 1030, 1144, 140, 3
        else:
            x, y, width, height = 28+(i%3)*390, 96+(i//3)*184, 366, 170
            # Icons occupy a full-width row below the other fourteen samples.
            if i > 11:
                x, y = 28+((i-12)%3)*390, 832
            zoom = min(3, max(1, min((width-16)//img.width, (height-58)//img.height)))
        draw.rectangle((x, y, x+width, y+height), fill='#17321F')
        draw.text((x+10, y+8), name, font=label, fill='#F4EBC8')
        px = img.resize((img.width*zoom, img.height*zoom), Image.Resampling.NEAREST)
        board.paste(px, (x+10, y+38), px)
        draw.text((x+10, y+height-24), f'{img.width}×{img.height} px · {sizes[name]["frames"]} quadros · {zoom}×',
                  font=small, fill='#B8C99A')
        sizes[name]['nine_slice_ltrb'] = MARGINS.get(name)
    board.save(OUT / 'contact-sheet.png')
    mockup = json.loads((OUT / 'mockup-source.json').read_text(encoding='utf-8'))
    shutil.copyfile(mockup['source'], OUT / 'mockup-original.png')
    Image.open(mockup['source']).resize((1200, 540), Image.Resampling.LANCZOS).save(OUT / 'mockup-1200x540.png')
    (OUT / 'measurements.json').write_text(json.dumps(sizes, indent=2)+'\n', encoding='utf-8')
    # Sheet previews preserve binary alpha and are never installed automatically.
    with zipfile.ZipFile(OUT / 'component-kit-v3.zip', 'w', zipfile.ZIP_DEFLATED) as archive:
        for folder in (native, hd):
            for file in sorted(folder.glob('*.png')):
                archive.write(file, file.relative_to(OUT))
        for filename in ('README.md', 'measurements.json', 'contact-sheet.png', 'mockup-1200x540.png'):
            file = OUT / filename
            if file.exists():
                archive.write(file, filename)
        archive.write(FONT, 'font/PixelifySans.ttf')
        archive.write(ART / 'fonts/PixelifySans-OFL.txt', 'font/OFL.txt')
    print('Saved contact-sheet.png, mockup-1200x540.png, native/ and component-kit-v3.zip')


if __name__ == '__main__':
    main()
