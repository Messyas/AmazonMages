#!/usr/bin/env python3
"""pixelize: deterministic HD -> pixel-art atlas converter (for AI-generated HD art).

Flow:  art/hd/<name>.png  ->  grid slicing  ->  chroma key (magenta background)  ->
       supersampled reduction + master palette + per-block mode  ->  cleanup  ->  alignment
       ->  atlas in art/build/<name>.png  ->  (optional) --install into drawable-nodpi.

Same input + same palette + same manifest = same output, byte for byte.
Usage:  python tools/pixelize/pixelize.py --help
"""
import argparse, json, shutil, sys
from collections import Counter
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter

ROOT = Path(__file__).resolve().parents[2]
ART = ROOT / "art"
RES = ROOT / "app/src/main/res/drawable-nodpi"
SUPER = 4  # each logical pixel is decided by a SUPER x SUPER block of the downscaled HD image
SOFT_LEVELS = [(0.75, 255), (0.45, 160), (0.20, 80)]  # alpha levels allowed in "soft_alpha" VFX


# ----------------------------------------------------------------------------- colour / palette
def srgb_to_oklab(rgb):
    c = np.asarray(rgb, dtype=np.float64) / 255.0
    lin = np.where(c <= 0.04045, c / 12.92, ((c + 0.055) / 1.055) ** 2.4)
    r, g, b = lin[..., 0], lin[..., 1], lin[..., 2]
    l = np.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
    m = np.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
    s = np.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
    return np.stack([0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
                     1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
                     0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s], -1)


def hex_to_rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


class Palette:
    def __init__(self, hexes):
        self.hex = list(dict.fromkeys("#" + h.lstrip("#").upper() for h in hexes))
        self.rgb = np.array([hex_to_rgb(h) for h in self.hex], np.uint8)
        self.lab = srgb_to_oklab(self.rgb)
        self.ink = self.lab[:, 0] < 0.34  # dark tones get extra weight so outlines survive

    @classmethod
    def load(cls, name, ramps=None):
        data = json.loads((ART / "palettes" / f"{name}.json").read_text(encoding="utf-8"))
        always = set(data.get("always", []))
        hexes = []
        for ramp, colors in data["ramps"].items():
            if ramps is None or ramp in ramps or ramp in always:
                hexes += colors
        if not hexes:
            raise SystemExit(f"palette '{name}' is empty for ramps={ramps}")
        return cls(hexes)

    def quantize(self, rgb):
        """(...,3) uint8 -> index of the nearest palette colour (OKLab). Caches unique colours."""
        flat = rgb.reshape(-1, 3)
        packed = (flat[:, 0].astype(np.uint32) << 16) | (flat[:, 1].astype(np.uint32) << 8) | flat[:, 2]
        uniq, inv = np.unique(packed, return_inverse=True)
        u = np.stack([(uniq >> 16) & 255, (uniq >> 8) & 255, uniq & 255], -1)
        d = ((srgb_to_oklab(u)[:, None, :] - self.lab[None, :, :]) ** 2).sum(-1)
        return d.argmin(1)[inv].reshape(rgb.shape[:-1]).astype(np.int32)


# ----------------------------------------------------------------------------- HD source
def key_out(img, key, tol):
    """Remove the flat-colour background (default magenta). Images that already have transparency are kept as they are."""
    img = img.convert("RGBA")
    a = np.asarray(img).copy()
    if a[..., 3].min() < 250 or key is None:
        return Image.fromarray(a)
    d = np.linalg.norm(a[..., :3].astype(np.float64) - np.array(hex_to_rgb(key), np.float64), axis=-1)
    mask = Image.fromarray(((d > tol) * 255).astype(np.uint8))
    mask = mask.filter(ImageFilter.MinFilter(3))  # eats the pink fringe on the edge
    a[..., 3] = np.asarray(mask)
    return Image.fromarray(a)


def source_cells(name, spec):
    """Returns {(row, col): RGBA Image} from the HD source: a single sheet, a folder of r{R}c{C}.png frames, or r{R}.png row strips."""
    cols, rows = spec["cols"], spec["rows_src"]
    key, tol = spec.get("key", "#FF00FF"), spec.get("key_tol", 90)
    folder, sheet = ART / "hd" / name, ART / "hd" / f"{name}.png"
    cells = {}
    if folder.is_dir():
        for r in range(rows):
            strip = folder / f"r{r}.png"  # a whole row (all frames of one direction) in a single image
            if strip.exists():
                row_img = key_out(Image.open(strip), key, tol)
                W, H = row_img.size
                for c in range(cols):
                    cells[(r, c)] = row_img.crop((round(c * W / cols), 0, round((c + 1) * W / cols), H))
                continue
            for c in range(cols):
                f = folder / f"r{r}c{c}.png"
                if not f.exists():
                    raise FileNotFoundError(f"missing {f.relative_to(ROOT)}")
                cells[(r, c)] = key_out(Image.open(f), key, tol)
        return cells
    if not sheet.exists():
        raise FileNotFoundError(f"missing {sheet.relative_to(ROOT)} (or the folder {folder.relative_to(ROOT)}/)")
    full = key_out(Image.open(sheet), key, tol)
    W, H = full.size
    for r in range(rows):
        for c in range(cols):
            box = (round(c * W / cols), round(r * H / rows), round((c + 1) * W / cols), round((r + 1) * H / rows))
            cells[(r, c)] = full.crop(box)
    return cells


# ----------------------------------------------------------------------------- reduction
def place_on_canvas(img, spec, shared_scale):
    """Places the HD cell on a (cw*S, ch*S) canvas already at the supersample scale."""
    cw, ch = spec["cell"]
    S = SUPER
    canvas = Image.new("RGBA", (cw * S, ch * S), (0, 0, 0, 0))
    fit = spec.get("fit", "cell")
    if fit == "cell":
        w, h = img.size
        s = min(cw * S / w, ch * S / h)
        box = None
    else:
        alpha = img.getchannel("A").point(lambda v: 255 if v >= 128 else 0)
        box = alpha.getbbox()
        if box is None:
            return canvas
        bw, bh = box[2] - box[0], box[3] - box[1]
        s = shared_scale if shared_scale else min(cw * S * spec.get("fill", 0.9) / bw, ch * S * spec.get("fill", 0.9) / bh)
        img = img.crop(box)
        w, h = bw, bh
    tw, th = max(1, round(w * s)), max(1, round(h * s))
    scaled = img.resize((tw, th), Image.LANCZOS)
    x = ((cw * S - tw) // 2 // S) * S
    anchor = spec.get("anchor", "center") if fit != "cell" else "center"
    if anchor in ("feet", "bottom"):
        base = spec.get("baseline", ch - max(1, round(ch * 0.09)) if anchor == "feet" else ch)
        y = (round(base * S - th) // S) * S
    else:
        y = ((ch * S - th) // 2 // S) * S
    # crop anything that falls outside the canvas (should not happen; guards against rounding)
    sx, sy = max(0, -x), max(0, -y)
    scaled = scaled.crop((sx, sy, min(tw, cw * S - x), min(th, ch * S - y)))
    canvas.alpha_composite(scaled, (max(0, x), max(0, y)))
    return canvas


def reduce_canvas(canvas, spec, pal):
    """Supersampled canvas -> (idx, alpha8) on the logical grid, by weighted mode inside each block."""
    S = SUPER
    a = np.asarray(canvas)
    H, W = a.shape[0] // S, a.shape[1] // S
    a8 = a[..., 3].astype(np.float64)
    idx = pal.quantize(a[..., :3])
    soft = spec.get("soft_alpha", False)
    cover = a8.reshape(H, S, W, S).mean(axis=(1, 3)) / 255.0
    cut = 64 if soft else 128
    blk = lambda x: x.reshape(H, S, W, S).transpose(0, 2, 1, 3).reshape(H, W, S * S)
    bi, bo = blk(idx), blk(a8 >= cut)
    weights = np.where(pal.ink, 1.0 + spec.get("outline_bias", 0.35), 1.0)
    counts = np.stack([((bi == p) & bo).sum(-1) * weights[p] for p in range(len(pal.hex))], -1)
    out_idx = counts.argmax(-1).astype(np.int32)
    if soft:
        alpha = np.zeros((H, W), np.uint8)
        for thr, val in SOFT_LEVELS:
            alpha = np.where((alpha == 0) & (cover >= thr), val, alpha)
    else:
        alpha = np.where(cover >= spec.get("alpha_thr", 0.5), 255, 0).astype(np.uint8)
    out_idx = np.where(alpha > 0, out_idx, -1)
    return out_idx, alpha


# ----------------------------------------------------------------------------- cleanup
NEI8 = [(-1, -1), (-1, 0), (-1, 1), (0, -1), (0, 1), (1, -1), (1, 0), (1, 1)]


def shifted(arr, dy, dx, fill):
    out = np.full_like(arr, fill)
    H, W = arr.shape
    ys, yd = (slice(max(0, -dy), H - max(0, dy)), slice(max(0, dy), H - max(0, -dy)))
    xs, xd = (slice(max(0, -dx), W - max(0, dx)), slice(max(0, dx), W - max(0, -dx)))
    out[yd, xd] = arr[ys, xs]
    return out


def despeckle(idx, alpha):
    """An opaque pixel whose colour appears in none of its 8 opaque neighbours becomes the neighbours' mode."""
    out = idx.copy()
    H, W = idx.shape
    for y in range(H):
        for x in range(W):
            if idx[y, x] < 0:
                continue
            ns = [idx[y + dy, x + dx] for dy, dx in NEI8 if 0 <= y + dy < H and 0 <= x + dx < W and idx[y + dy, x + dx] >= 0]
            if len(ns) >= 5 and idx[y, x] not in ns:
                out[y, x] = Counter(ns).most_common(1)[0][0]
    return out


def drop_islands(idx, alpha, min_size):
    """Removes opaque islands (4-connected) smaller than min_size pixels."""
    H, W = idx.shape
    seen = np.zeros((H, W), bool)
    for y in range(H):
        for x in range(W):
            if idx[y, x] < 0 or seen[y, x]:
                continue
            stack, comp = [(y, x)], []
            seen[y, x] = True
            while stack:
                cy, cx = stack.pop()
                comp.append((cy, cx))
                for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    ny, nx = cy + dy, cx + dx
                    if 0 <= ny < H and 0 <= nx < W and idx[ny, nx] >= 0 and not seen[ny, nx]:
                        seen[ny, nx] = True
                        stack.append((ny, nx))
            if len(comp) < min_size:
                for cy, cx in comp:
                    idx[cy, cx] = -1
                    alpha[cy, cx] = 0
    return idx, alpha


def auto_outline(idx, alpha, pal):
    """1 px outline in the neighbour colour, darkened and snapped to the palette, where the silhouette has no dark outline."""
    H, W = idx.shape
    out_idx, out_a = idx.copy(), alpha.copy()
    for y in range(H):
        for x in range(W):
            if idx[y, x] >= 0:
                continue
            cand = [idx[y + dy, x + dx] for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1))
                    if 0 <= y + dy < H and 0 <= x + dx < W and idx[y + dy, x + dx] >= 0 and not pal.ink[idx[y + dy, x + dx]]]
            if cand:
                n = Counter(cand).most_common(1)[0][0]
                lab = pal.lab[n].copy()
                lab[0] *= 0.45
                out_idx[y, x] = ((pal.lab - lab) ** 2).sum(-1).argmin()
                out_a[y, x] = 255
    return out_idx, out_a


def stabilize_feet(frames, spec, warn):
    """Aligns the feet of all frames: same baseline and same centre (limited to max_shift)."""
    bottoms, cxs = {}, {}
    for k, (idx, alpha) in frames.items():
        ys, xs = np.nonzero(alpha > 0)
        if len(ys) == 0:
            continue
        bottoms[k] = ys.max()
        low = xs[ys >= ys.max() - 3]
        cxs[k] = low.mean()
    if not bottoms:
        return frames
    tb = int(np.median(list(bottoms.values())))
    tx = float(np.median(list(cxs.values())))
    lim = spec.get("max_shift", 3)
    out = {}
    for k, (idx, alpha) in frames.items():
        if k not in bottoms:
            out[k] = (idx, alpha)
            continue
        dy, dx = tb - bottoms[k], int(round(tx - cxs[k]))
        if abs(dy) > lim or abs(dx) > lim:
            warn(f"frame {k}: feet off the pivot ({dy:+d},{dx:+d}px); corrected only up to {lim}px")
        dy, dx = max(-lim, min(lim, dy)), max(-lim, min(lim, dx))
        out[k] = (shifted(idx, dy, dx, -1), shifted(alpha, dy, dx, 0))
    return out


# ----------------------------------------------------------------------------- assembly
def build_asset(name, spec, palette_name, log):
    if spec.get("smooth"):  # portraits, backgrounds, logos: only remove the background and resize, no palette
        return build_smooth(name, spec, log)
    pal = Palette.load(palette_name, spec.get("ramps"))
    warns = []
    if "parts" in spec:
        return build_parts(name, spec, palette_name, log)
    cw, ch = spec["cell"]
    cols, rows_src = spec["cols"], spec["rows_src"]
    rows_out = spec.get("rows", rows_src)
    cells = source_cells(name, spec)
    shared = None
    if spec.get("fit", "cell") == "trim" and spec.get("shared", True):
        mx = my = 1
        for img in cells.values():
            b = img.getchannel("A").point(lambda v: 255 if v >= 128 else 0).getbbox()
            if b:
                mx, my = max(mx, b[2] - b[0]), max(my, b[3] - b[1])
        fill = spec.get("fill", 0.9)
        shared = min(cw * SUPER * fill / mx, ch * SUPER * fill / my)
    frames = {}
    for (r, c), img in cells.items():
        canvas = place_on_canvas(img, spec, shared)
        idx, alpha = reduce_canvas(canvas, spec, pal)
        if spec.get("despeckle", True):
            idx = despeckle(idx, alpha)
        if spec.get("min_island", 0):
            idx, alpha = drop_islands(idx, alpha, spec["min_island"])
        if spec.get("outline") == "auto":
            idx, alpha = auto_outline(idx, alpha, pal)
        frames[(r, c)] = (idx, alpha)
    if spec.get("stabilize") == "feet":
        for r in range(rows_src):
            sub = stabilize_feet({k: v for k, v in frames.items() if k[0] == r}, spec, lambda m, r=r: warns.append(f"row {r}: {m}"))
            frames.update(sub)
    if spec.get("directions") == 5 and rows_out == 8:  # S SE E NE N -> + NW W SW por espelho
        for dst, src in ((5, 3), (6, 2), (7, 1)):
            for c in range(cols):
                i, a = frames[(src, c)]
                frames[(dst, c)] = (np.fliplr(i).copy(), np.fliplr(a).copy())
    H, W = rows_out * ch, cols * cw
    sheet = np.zeros((H, W, 4), np.uint8)
    for (r, c), (idx, alpha) in frames.items():
        rgb = np.where((idx >= 0)[..., None], pal.rgb[np.clip(idx, 0, None)], 0)
        sheet[r * ch:(r + 1) * ch, c * cw:(c + 1) * cw, :3] = rgb
        sheet[r * ch:(r + 1) * ch, c * cw:(c + 1) * cw, 3] = alpha
    s = spec.get("scale", 1)
    if s > 1:
        sheet = sheet.repeat(s, 0).repeat(s, 1)
    for w in warns:
        log(f"  warning: {w}")
    return Image.fromarray(sheet, "RGBA")


def build_smooth(name, spec, log):
    src = ART / "hd" / f"{name}.png"
    if not src.exists():
        raise FileNotFoundError(f"missing {src.relative_to(ROOT)}")
    key = spec.get("key", "#FF00FF")
    img = key_out(Image.open(src), key, spec.get("key_tol", 90)) if key else Image.open(src).convert("RGBA")
    w, h = spec["size"]
    if abs(img.width / img.height - w / h) > 0.03:
        log(f"  warning: {name}: source aspect {img.width}x{img.height} differs from {w}x{h}; image will be stretched")
    return img.resize((w, h), Image.LANCZOS)


def build_parts(name, spec, palette_name, log):
    W, H = spec["size"]
    s = spec.get("scale", 1)
    out = Image.new("RGBA", (W * s, H * s), (0, 0, 0, 0))
    for part in spec["parts"]:
        sub = dict(spec)
        sub.pop("parts")
        sub.update(cols=1, rows_src=1, cell=part["cell"], fit=part.get("fit", spec.get("fit", "trim")))
        img = build_asset(part["name"], sub, palette_name, log)
        out.alpha_composite(img, (part["at"][0] * s, part["at"][1] * s))
    return out


# ----------------------------------------------------------------------------- verification
def verify(name, spec, png, palette_name):
    """Returns the list of problems of an already built PNG (empty = ok)."""
    img = Image.open(png).convert("RGBA")
    if spec.get("smooth"):
        return [] if img.size == tuple(spec["size"]) else [f"size {img.size}, expected {tuple(spec['size'])}"]
    pal = Palette.load(palette_name, spec.get("ramps"))
    a = np.asarray(img)
    s = spec.get("scale", 1)
    problems = []
    if "parts" in spec:
        exp = (spec["size"][0] * s, spec["size"][1] * s)
    else:
        exp = (spec["cols"] * spec["cell"][0] * s, spec.get("rows", spec["rows_src"]) * spec["cell"][1] * s)
    if img.size != exp:
        problems.append(f"size {img.size}, expected {exp}")
    allowed = {0, 255} | ({v for _, v in SOFT_LEVELS} if spec.get("soft_alpha") else set())
    bad_alpha = set(np.unique(a[..., 3]).tolist()) - allowed
    if bad_alpha:
        problems.append(f"alpha outside the allowed set: {sorted(bad_alpha)}")
    op = a[a[..., 3] > 0][:, :3]
    if len(op):
        pk = {tuple(c) for c in pal.rgb.tolist()}
        off = {tuple(c) for c in np.unique(op, axis=0).tolist()} - pk
        if off:
            problems.append(f"{len(off)} colour(s) outside the palette")
    blocks = spec["cell"][0] * s, spec["cell"][1] * s
    if "parts" not in spec:
        rows_out = spec.get("rows", spec["rows_src"])
        empty = [(r, c) for r in range(rows_out) for c in range(spec["cols"])
                 if not a[r * blocks[1]:(r + 1) * blocks[1], c * blocks[0]:(c + 1) * blocks[0], 3].any()]
        if empty and spec.get("kind") != "tile" and not spec.get("allow_empty"):
            problems.append(f"{len(empty)} empty frame(s), e.g. {empty[:3]}")
    return problems


# ----------------------------------------------------------------------------- manifest / CLI
def load_manifest():
    m = json.loads((ART / "manifest.json").read_text(encoding="utf-8"))
    assets = {}
    for name, spec in m["assets"].items():
        merged = dict(m["kinds"].get(spec.get("kind"), {}))
        merged.update(spec)
        if "source_cell" in merged:
            factor = merged.get("downsample")
            source = merged["source_cell"]
            if (not isinstance(factor, int) or isinstance(factor, bool) or factor < 1
                    or not isinstance(source, list) or len(source) != 2
                    or any(not isinstance(v, int) or isinstance(v, bool) or v < 1 or v % factor for v in source)):
                raise ValueError(f"{name}: source_cell needs two positive dimensions divisible by a positive integer downsample")
            cell = [v // factor for v in source]
            if "cell" in spec and spec["cell"] != cell:
                raise ValueError(f"{name}: cell must match source_cell / downsample ({cell})")
            merged["cell"] = cell
        merged.setdefault("rows_src", 5 if merged.get("directions") == 5 else merged.get("rows", 1))
        assets[name] = merged
    return m, assets


def select(assets, names, prefix):
    if names:
        missing = [n for n in names if n not in assets]
        if missing:
            raise SystemExit("not in the manifest: " + ", ".join(missing))
        return names
    return [n for n in assets if not prefix or n.startswith(prefix)]


def has_source(name, spec):
    if "parts" in spec:
        return all(has_source(p["name"], {}) for p in spec["parts"])
    return (ART / "hd" / f"{name}.png").exists() or (ART / "hd" / name).is_dir()


def cmd_build(args):
    m, assets = load_manifest()
    pal = args.palette or m.get("palette", "amazon")
    todo = select(assets, args.names, args.prefix)
    out_dir = ART / "build"
    out_dir.mkdir(parents=True, exist_ok=True)
    ok = skipped = failed = 0
    for name in todo:
        spec = assets[name]
        if not has_source(name, spec):
            skipped += 1
            continue
        try:
            img = build_asset(name, spec, pal, print)
            dst = out_dir / f"{name}.png"
            img.save(dst, optimize=True)
            problems = verify(name, spec, dst, pal)
        except Exception as e:  # noqa: BLE001
            print(f"[ERROR] {name}: {e}")
            failed += 1
            continue
        if problems:
            failed += 1
            print(f"[FAILED] {name}: " + "; ".join(problems))
            continue
        ok += 1
        print(f"[ok] {name}  {img.size[0]}x{img.size[1]}")
        if args.install:
            RES.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(dst, RES / f"{name}.png")
    print(f"\n{ok} built, {skipped} without HD source, {failed} with problems" + (" (installed into drawable-nodpi)" if args.install and ok else ""))
    return 1 if failed else 0


def cmd_check(args):
    m, assets = load_manifest()
    pal = args.palette or m.get("palette", "amazon")
    bad = 0
    for name in select(assets, args.names, args.prefix):
        f = (RES if args.installed else ART / "build") / f"{name}.png"
        if not f.exists():
            continue
        p = verify(name, assets[name], f, pal)
        print(("[ok] " if not p else "[FAILED] ") + name + ("" if not p else ": " + "; ".join(p)))
        bad += bool(p)
    return 1 if bad else 0


def cmd_status(args):
    m, assets = load_manifest()
    rows = []
    for name, spec in assets.items():
        hd = has_source(name, spec)
        built = (ART / "build" / f"{name}.png").exists()
        rows.append((name, "HD ok" if hd else "no HD", "built" if built else "-"))
    w = max(len(r[0]) for r in rows)
    for r in rows:
        print(f"{r[0]:<{w}}  {r[1]:<8}  {r[2]}")
    print(f"\n{sum(r[1] == 'HD ok' for r in rows)}/{len(rows)} with HD source")
    return 0


def cmd_preview(args):
    """Side by side: downscaled HD | result enlarged 3x (and a 3x3 repetition for tiles)."""
    m, assets = load_manifest()
    pal = args.palette or m.get("palette", "amazon")
    (ART / "preview").mkdir(parents=True, exist_ok=True)
    for name in select(assets, args.names, args.prefix):
        spec = assets[name]
        f = ART / "build" / f"{name}.png"
        src = ART / "hd" / f"{name}.png"
        if not f.exists() or not src.exists():
            continue
        res = Image.open(f).convert("RGBA")
        hd = Image.open(src).convert("RGBA")
        z = 3 // spec.get("scale", 1) or 1
        res = res.resize((res.width * z, res.height * z), Image.NEAREST)
        hd = hd.resize((max(1, round(hd.width * res.height / hd.height)), res.height), Image.LANCZOS)
        bg = Image.new("RGBA", (hd.width + res.width + 16, res.height), (40, 40, 48, 255))
        bg.alpha_composite(hd, (0, 0))
        bg.alpha_composite(res, (hd.width + 16, 0))
        if spec.get("mode") == "tile" or spec.get("kind") == "tile":
            cell = res.crop((0, 0, spec["cell"][0] * spec.get("scale", 1) * z, spec["cell"][1] * spec.get("scale", 1) * z))
            rep = Image.new("RGBA", (cell.width * 3, cell.height * 3))
            for i in range(3):
                for j in range(3):
                    rep.paste(cell, (i * cell.width, j * cell.height))
            bg2 = Image.new("RGBA", (bg.width, bg.height + rep.height + 8), (40, 40, 48, 255))
            bg2.paste(bg, (0, 0))
            bg2.paste(rep, (0, bg.height + 8))
            bg = bg2
        bg.save(ART / "preview" / f"{name}.png")
        print(f"[preview] art/preview/{name}.png")
    return 0


def cmd_palette_extract(args):
    """Extracts N colours from approved HD images (median cut) and writes a new palette."""
    imgs = []
    for p in args.images:
        im = Image.open(p).convert("RGBA")
        im.thumbnail((256, 256))
        a = np.asarray(im)
        imgs.append(a[a[..., 3] > 200][:, :3])
    px = np.concatenate(imgs)
    side = int(np.ceil(np.sqrt(len(px))))
    px = np.concatenate([px, px[: side * side - len(px)]])[: side * side].reshape(side, side, 3)
    q = Image.fromarray(px.astype(np.uint8)).quantize(colors=args.colors, method=Image.MEDIANCUT)
    pal = np.array(q.getpalette()[: args.colors * 3]).reshape(-1, 3)
    pal = sorted((tuple(c) for c in pal), key=lambda c: srgb_to_oklab(np.array(c))[0])
    hexes = ["#%02X%02X%02X" % c for c in pal]
    out = ART / "palettes" / f"{args.name}.json"
    out.write_text(json.dumps({"always": ["extracted"], "ramps": {"extracted": hexes}}, indent=1), encoding="utf-8")
    print(f"{len(hexes)} colours -> {out.relative_to(ROOT)}")
    return 0


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)

    def common(p):
        p.add_argument("names", nargs="*", help="manifest assets (empty = all that have an HD source)")
        p.add_argument("--prefix", help="only assets whose name starts with this prefix, e.g. player_")
        p.add_argument("--palette", help="palette in art/palettes (default: the manifest one)")

    b = sub.add_parser("build", help="builds art/build/<name>.png")
    common(b)
    b.add_argument("--install", action="store_true", help="copies into app/src/main/res/drawable-nodpi")
    c = sub.add_parser("check", help="validates built PNGs (size, palette, alpha, empty frames)")
    common(c)
    c.add_argument("--installed", action="store_true", help="validates the ones installed in drawable-nodpi")
    common(sub.add_parser("status", help="shows which assets have an HD source and which are built"))
    common(sub.add_parser("preview", help="writes HD vs pixel comparisons to art/preview"))
    pe = sub.add_parser("palette-extract", help="creates a palette from approved HD images")
    pe.add_argument("images", nargs="+")
    pe.add_argument("--colors", type=int, default=48)
    pe.add_argument("--name", default="extracted")
    args = ap.parse_args(argv)
    return {"build": cmd_build, "check": cmd_check, "status": cmd_status, "preview": cmd_preview,
            "palette-extract": cmd_palette_extract}[args.cmd](args)


if __name__ == "__main__":
    sys.exit(main())
