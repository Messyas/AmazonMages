"""pixelize tests using synthetic HD art (no real images needed).
Run:  python -m unittest tools/pixelize/test_pixelize.py -v   (or python tools/pixelize/test_pixelize.py)
"""
import json, random, shutil, sys, tempfile, unittest
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
import pixelize as px  # noqa: E402

MAGENTA = (255, 0, 255, 255)


def blob(size, jitter=(0, 0), color=(212, 62, 72), w=0.5, h=0.8):
    """Synthetic doll: body + head with anti-aliased edges on a magenta background."""
    S = 4
    im = Image.new("RGBA", (size * S, size * S), MAGENTA)
    d = ImageDraw.Draw(im)
    cx, base = size * S // 2 + jitter[0] * S, int(size * S * 0.9) + jitter[1] * S
    bw, bh = int(size * S * w / 2), int(size * S * h)
    d.rectangle([cx - bw - 6 * S, base - bh - 6 * S, cx + bw + 6 * S, base], fill=(20, 30, 24, 255))      # dark outline
    d.rectangle([cx - bw, base - bh, cx + bw, base - 6 * S], fill=color + (255,))                          # body
    d.ellipse([cx - bw * 0.7, base - bh - bw, cx + bw * 0.7, base - bh + bw * 0.4], fill=(240, 190, 148, 255))
    return im.resize((size, size), Image.LANCZOS)


class PixelizeTest(unittest.TestCase):
    def test_declared_source_cells_use_kind_default_or_asset_downsample(self):
        manifest = {'kinds': {'ui': {'downsample': 4, 'scale': 1, 'despeckle': False}},
                    'assets': {
                        'new_button': {'kind': 'ui', 'source_cell': [256, 160], 'cols': 1, 'rows': 1},
                        'scene': {'kind': 'ui', 'source_cell': [1200, 540], 'downsample': 3, 'cols': 1, 'rows': 1},
                        'legacy': {'kind': 'ui', 'cell': [32, 20], 'downsample': 8}}}
        (self.tmp / 'manifest.json').write_text(json.dumps(manifest), encoding='utf-8')
        _, assets = px.load_manifest()
        self.assertEqual(assets['new_button']['cell'], [64, 40])
        self.assertEqual(assets['scene']['cell'], [400, 180])
        self.assertEqual(assets['legacy']['cell'], [32, 20])
        Image.new('RGBA', (256, 160), (46, 59, 42, 255)).save(self.tmp / 'hd/new_button.png')
        result, _ = self.build('new_button', assets['new_button'])
        self.assertEqual(result.size, (64, 40))

    def test_invalid_downsample_dimensions_are_rejected(self):
        for source, factor, cell in [([255, 160], 4, None), ([256, 160], 0, None),
                                     ([256, 160], 4, [32, 20])]:
            with self.subTest(source=source, factor=factor, cell=cell):
                spec = {'source_cell': source, 'downsample': factor}
                if cell is not None:
                    spec['cell'] = cell
                (self.tmp / 'manifest.json').write_text(
                    json.dumps({'kinds': {}, 'assets': {'bad': spec}}), encoding='utf-8')
                with self.assertRaises(ValueError):
                    px.load_manifest()

    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        (self.tmp / "hd").mkdir(parents=True)
        (self.tmp / "palettes").mkdir()
        shutil.copy(px.ART / "palettes" / "amazon.json", self.tmp / "palettes" / "amazon.json")
        self.old = px.ART
        px.ART = self.tmp

    def tearDown(self):
        px.ART = self.old
        shutil.rmtree(self.tmp, ignore_errors=True)

    def spec(self, **kw):
        base = {"scale": 2, "fit": "cell", "despeckle": True, "min_island": 3, "alpha_thr": 0.5, "rows_src": 1}
        base.update(kw)
        return base

    def build(self, name, spec):
        img = px.build_asset(name, spec, "amazon", lambda m: None)
        out = self.tmp / f"{name}.png"
        img.save(out)
        return img, out

    def test_anim_sheet_size_palette_alpha_pivot_and_mirror(self):
        rnd = random.Random(7)
        cols, rows, HD = 4, 5, 256
        sheet = Image.new("RGBA", (cols * HD, rows * HD), MAGENTA)
        for r in range(rows):
            for c in range(cols):
                sheet.paste(blob(HD, (rnd.randint(-8, 8), rnd.randint(-8, 8))), (c * HD, r * HD))
        sheet.save(self.tmp / "hd" / "hero_idle.png")
        spec = self.spec(cols=cols, rows=8, rows_src=5, directions=5, cell=[32, 32], stabilize="feet",
                         ramps=["hero_outfit", "hero_skin", "grass"])
        img, out = self.build("hero_idle", spec)
        self.assertEqual(img.size, (cols * 64, 8 * 64))
        self.assertEqual(px.verify("hero_idle", spec, out, "amazon"), [])
        a = np.asarray(img)
        self.assertEqual(set(np.unique(a[..., 3]).tolist()), {0, 255})
        # feet aligned: last opaque row of every frame falls on the same line after stabilizing
        bottoms = set()
        for r in range(rows):
            for c in range(cols):
                cell = a[r * 64:(r + 1) * 64, c * 64:(c + 1) * 64, 3]
                bottoms.add(int(np.nonzero(cell.any(1))[0].max()))
        self.assertLessEqual(len(bottoms), 2, f"feet misaligned: {bottoms}")
        # rows 5,6,7 = mirror of rows 3,2,1
        for dst, src in ((5, 3), (6, 2), (7, 1)):
            np.testing.assert_array_equal(a[dst * 64:(dst + 1) * 64, :64], a[src * 64:(src + 1) * 64, :64][:, ::-1])

    def test_no_magenta_fringe_and_deterministic(self):
        blob(256).save(self.tmp / "hd" / "thing.png")
        spec = self.spec(cols=1, cell=[32, 32], ramps=["hero_outfit", "hero_skin"])
        i1, _ = self.build("thing", spec)
        i2, _ = self.build("thing", spec)
        self.assertEqual(i1.tobytes(), i2.tobytes())
        rgb = np.asarray(i1)
        op = rgb[rgb[..., 3] > 0][:, :3]
        pink = (op[:, 0] > 200) & (op[:, 2] > 200) & (op[:, 1] < 120)
        self.assertFalse(pink.any(), "magenta fringe left over")

    def test_ai_noise_inside_sprite_is_cleaned(self):
        im = blob(256)
        d = ImageDraw.Draw(im)
        d.rectangle([120, 120, 124, 124], fill=(30, 200, 90, 255))  # isolated 1-logical-pixel "AI hallucination"
        im.save(self.tmp / "hd" / "noisy.png")
        spec = self.spec(cols=1, cell=[32, 32], ramps=["hero_outfit", "hero_skin", "grass"])
        img, out = self.build("noisy", spec)
        a = np.asarray(img)
        ramps = json.loads((self.tmp / "palettes" / "amazon.json").read_text(encoding="utf-8"))["ramps"]
        greens = {px.hex_to_rgb(h) for h in ramps["grass"]}
        found = [(y, x) for y in range(64) for x in range(64) if a[y, x, 3] and tuple(a[y, x, :3]) in greens]
        # ramps include grass, but an isolated green pixel must be absorbed by its neighbours
        self.assertLessEqual(len(found), 4)

    def test_tile_is_opaque_and_exact_size(self):
        rnd = np.random.default_rng(1)
        base = np.zeros((512, 1024, 3), np.uint8)
        base[:] = (70, 177, 68)
        base += rnd.integers(-12, 12, base.shape, dtype=np.int16).clip(-12, 12).astype(np.uint8) * 0
        Image.fromarray(base).save(self.tmp / "hd" / "tile_x.png")
        spec = self.spec(cols=2, cell=[32, 32], key=None, ramps=["grass"], despeckle=False)
        img, out = self.build("tile_x", spec)
        self.assertEqual(img.size, (128, 64))
        self.assertEqual(set(np.unique(np.asarray(img)[..., 3]).tolist()), {255})
        self.assertEqual(px.verify("tile_x", dict(spec, kind="tile"), out, "amazon"), [])

    def test_hud_icons_trim_each(self):
        sheet = Image.new("RGBA", (3 * 256, 256), MAGENTA)
        for c in range(3):
            sheet.paste(blob(256, w=0.3 + 0.2 * c, h=0.4 + 0.15 * c), (c * 256, 0))
        sheet.save(self.tmp / "hd" / "icons.png")
        spec = self.spec(scale=1, cols=3, cell=[16, 16], fit="trim", shared=False, fill=0.9, despeckle=False,
                         ramps=["hero_outfit", "hero_skin"])
        img, out = self.build("icons", spec)
        self.assertEqual(img.size, (48, 16))
        self.assertEqual(px.verify("icons", spec, out, "amazon"), [])

    def test_parts_joystick(self):
        blob(256, w=0.9, h=0.9).save(self.tmp / "hd" / "stick_base.png")
        blob(128, w=0.9, h=0.9).save(self.tmp / "hd" / "stick_knob.png")
        spec = self.spec(scale=1, size=[72, 48], fit="trim", shared=False, fill=0.9, despeckle=False,
                         ramps=["hero_outfit", "hero_skin"],
                         parts=[{"name": "stick_base", "at": [0, 0], "cell": [48, 48]},
                                {"name": "stick_knob", "at": [48, 0], "cell": [24, 24]}])
        img, _ = self.build("stick", spec)
        self.assertEqual(img.size, (72, 48))

    def test_row_strips_and_smooth_portrait(self):
        (self.tmp / "hd" / "walk").mkdir()
        for r in range(2):
            strip = Image.new("RGBA", (3 * 256, 256), MAGENTA)
            for c in range(3):
                strip.paste(blob(256, (c * 3, 0)), (c * 256, 0))
            strip.save(self.tmp / "hd" / "walk" / f"r{r}.png")
        spec = self.spec(cols=3, rows=2, rows_src=2, cell=[32, 32], ramps=["hero_outfit", "hero_skin"])
        img, out = self.build("walk", spec)
        self.assertEqual(img.size, (192, 128))
        blob(512).save(self.tmp / "hd" / "portrait.png")
        pimg = px.build_asset("portrait", {"smooth": True, "size": [128, 128]}, "amazon", lambda m: None)
        self.assertEqual(pimg.size, (128, 128))
        self.assertEqual(pimg.getpixel((0, 0))[3], 0)  # magenta background became transparent

    def test_verify_catches_bad_png(self):
        bad = Image.new("RGBA", (10, 10), (1, 2, 3, 128))
        bad.save(self.tmp / "bad.png")
        problems = px.verify("bad", self.spec(cols=1, cell=[32, 32], ramps=["grass"]), self.tmp / "bad.png", "amazon")
        self.assertTrue(any("size" in p for p in problems))
        self.assertTrue(any("alpha" in p for p in problems))
        self.assertTrue(any("palette" in p for p in problems))


if __name__ == "__main__":
    unittest.main(verbosity=2)
