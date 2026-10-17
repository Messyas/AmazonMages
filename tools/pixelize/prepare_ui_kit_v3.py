"""Normalize generated UI strips without clipping drawings; retain original sources.

The brief explicitly permits script-based chroma keying, framing and reduction.
This preparation removes only magenta padding, preserving shared state alignment.
Run from any directory: python tools/pixelize/prepare_ui_kit_v3.py
"""
import json
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ART = ROOT / 'art'


def foreground(image):
    a = np.asarray(image.convert('RGB')).astype(np.int32)
    return np.sqrt(((a - [255, 0, 255]) ** 2).clip(0).sum(-1)) > 90


def bounds(mask):
    y, x = np.where(mask)
    if not len(x):
        raise ValueError('Empty component frame')
    return int(x.min()), int(y.min()), int(x.max()) + 1, int(y.max()) + 1


def prepare():
    manifest = json.loads((ART / 'manifest.json').read_text(encoding='utf-8'))
    records = json.loads((ART / 'ui-kit-v3/sources.json').read_text(encoding='utf-8'))
    target = ART / 'hd'
    target.mkdir(exist_ok=True)
    metadata = {}
    for record in records['assets']:
        name = record['name']
        spec = manifest['assets'][name]
        cols = spec['cols']
        w, h = (v * 8 for v in spec['cell'])
        raw = Image.open(ART / 'source/ui-kit-v3' / (name + '.png')).convert('RGB')
        boxes = [(round(c * raw.width / cols), 0,
                  round((c + 1) * raw.width / cols), raw.height) for c in range(cols)]
        if name == 'ui_icons':
            occupied = foreground(raw).any(axis=0)
            starts = np.where(np.diff(np.r_[False, occupied, False].astype(int)) == 1)[0]
            ends = np.where(np.diff(np.r_[False, occupied, False].astype(int)) == -1)[0]
            runs = [(int(a), 0, int(b), raw.height) for a, b in zip(starts, ends) if b-a > 8]
            # The pause glyph is two intentionally disconnected bars (icon 8).
            if len(runs) == cols + 1:
                a, b = runs[7:9]
                typical = float(np.median([r[2]-r[0] for r in runs]))
                if a[2]-a[0] < typical*.6 and b[2]-b[0] < typical*.6:
                    runs[7:9] = [(a[0], 0, b[2], raw.height)]
            if len(runs) != cols:
                raise ValueError(f'Expected {cols} distinct icons, found {len(runs)}')
            boxes = runs
        frames = [raw.crop(box) for box in boxes]
        crops = [bounds(foreground(frame)) for frame in frames]
        strip = Image.new('RGB', (w * cols, h), '#FF00FF')
        # State frames share vertical bounds; this retains deliberate pressed offsets.
        top, bottom = min(b[1] for b in crops), max(b[3] for b in crops)
        for i, (frame, box) in enumerate(zip(frames, crops)):
            left, _, right, _ = box
            if name != 'ui_icons':
                left = min(b[0] for b in crops)
                right = max(b[2] for b in crops)
                box = (left, top, min(right, frame.width), bottom)
            cropped = frame.crop(box)
            if name == 'ui_icons':
                cropped.thumbnail((w - 16, h - 16), Image.Resampling.LANCZOS)
                strip.paste(cropped, (i*w + (w-cropped.width)//2, (h-cropped.height)//2))
            else:
                strip.paste(cropped.resize((w, h), Image.Resampling.LANCZOS), (i*w, 0))
        if name in {'ui_button', 'ui_button_round', 'ui_close', 'ui_stepper'}:
            # Normalize one shared silhouette before generating the exact 1-pixel press.
            # The remaining generated disabled frame keeps its muted artwork.
            normalized = Image.new('RGB', strip.size, '#FF00FF')
            for i in range(cols):
                frame = strip.crop((i*w, 0, (i+1)*w, h))
                frame = frame.resize((w-16, h-24), Image.Resampling.LANCZOS)
                normalized.paste(frame, (i*w+8, 8))
            normal = normalized.crop((0, 0, w, h))
            normalized.paste(Image.new('RGB', (w, h), '#FF00FF'), (w, 0))
            normalized.paste(normal.crop((0, 0, w, h-8)), (w, 8))
            strip = normalized
        # Chroma areas become exactly magenta, including compression-like fringe drift.
        a = np.asarray(strip).copy()
        a[~foreground(strip)] = [255, 0, 255]
        Image.fromarray(a).save(target / (name + '.png'))
        metadata[name] = {'raw_size': list(raw.size), 'hd_size': [w*cols, h],
                          'frames': cols, 'logical_cell': spec['cell'],
                          'source_boxes': [list(b) for b in boxes]}
        print(f'{name}: {raw.size} -> {w*cols}x{h}, {cols} frames')
    (ART / 'ui-kit-v3/measurements.json').write_text(
        json.dumps(metadata, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    prepare()
