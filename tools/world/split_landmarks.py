"""Split generated landmark rasters into bounded runtime parts, preserving exact pixels."""
import hashlib
import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / 'app/src/main/res/drawable-nodpi'

def main():
    report = {}
    for name in ['world_castanheira_trunk_v1', 'world_root_v1']:
        source = Image.open(ROOT / 'art/build' / (name + '.png')).convert('RGBA')
        parts = []
        reconstructed = Image.new('RGBA', source.size)
        columns = (source.width + 511) // 512
        for row in range((source.height + 511) // 512):
            for col in range(columns):
                left, top = col * 512, row * 512
                piece = source.crop((left, top, min(left + 512, source.width), min(top + 512, source.height)))
                filename = name + '_p' + str(row * columns + col) + '.png'
                piece.save(RES / filename, optimize=True)
                reconstructed.paste(piece, (left, top))
                parts.append({'file': filename, 'origin': [left, top], 'size': list(piece.size),
                              'sha256': hashlib.sha256((RES / filename).read_bytes()).hexdigest()})
        if source.tobytes() != reconstructed.tobytes():
            raise AssertionError('Split changed pixels: ' + name)
        report[name] = {'size': list(source.size), 'parts': parts, 'lossless_reconstruction': True}
    (ROOT / 'art/world-v1/landmark-parts.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print('Landmark parts verified and installed')

if __name__ == '__main__':
    main()
