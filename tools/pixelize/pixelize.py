#!/usr/bin/env python3
"""pixelize: converte arte HD (gerada por IA) em atlas pixel art de forma 100% determinística.

Fluxo:  art/hd/<nome>.png  ->  recorte em grade  ->  chave de cor (fundo magenta)  ->
        redução por supersample + paleta mestra + moda por bloco  ->  limpeza  ->  alinhamento
        ->  atlas em art/build/<nome>.png  ->  (opcional) --install em drawable-nodpi.

Mesma entrada + mesma paleta + mesmo manifesto = mesma saída, byte a byte.
Uso:  python tools/pixelize/pixelize.py --help
"""
import argparse, json, shutil, sys
from collections import Counter
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter

ROOT = Path(__file__).resolve().parents[2]
ART = ROOT / "art"
RES = ROOT / "app/src/main/res/drawable-nodpi"
SUPER = 4  # cada pixel lógico é decidido por um bloco SUPER x SUPER do HD reduzido
SOFT_LEVELS = [(0.75, 255), (0.45, 160), (0.20, 80)]  # alfa permitido em VFX "soft_alpha"


# ----------------------------------------------------------------------------- cor / paleta
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
        self.ink = self.lab[:, 0] < 0.34  # tons escuros: ganham peso para preservar contornos

    @classmethod
    def load(cls, name, ramps=None):
        data = json.loads((ART / "palettes" / f"{name}.json").read_text(encoding="utf-8"))
        always = set(data.get("always", []))
        hexes = []
        for ramp, colors in data["ramps"].items():
            if ramps is None or ramp in ramps or ramp in always:
                hexes += colors
        if not hexes:
            raise SystemExit(f"paleta '{name}' vazia para ramps={ramps}")
        return cls(hexes)

    def quantize(self, rgb):
        """(...,3) uint8 -> índices da cor mais próxima (OKLab). Cacheia cores únicas."""
        flat = rgb.reshape(-1, 3)
        packed = (flat[:, 0].astype(np.uint32) << 16) | (flat[:, 1].astype(np.uint32) << 8) | flat[:, 2]
        uniq, inv = np.unique(packed, return_inverse=True)
        u = np.stack([(uniq >> 16) & 255, (uniq >> 8) & 255, uniq & 255], -1)
        d = ((srgb_to_oklab(u)[:, None, :] - self.lab[None, :, :]) ** 2).sum(-1)
        return d.argmin(1)[inv].reshape(rgb.shape[:-1]).astype(np.int32)


# ----------------------------------------------------------------------------- fonte HD
def key_out(img, key, tol):
    """Remove o fundo de cor chapada (padrão magenta). Se a imagem já tem transparência, respeita."""
    img = img.convert("RGBA")
    a = np.asarray(img).copy()
    if a[..., 3].min() < 250 or key is None:
        return Image.fromarray(a)
    d = np.linalg.norm(a[..., :3].astype(np.float64) - np.array(hex_to_rgb(key), np.float64), axis=-1)
    mask = Image.fromarray(((d > tol) * 255).astype(np.uint8))
    mask = mask.filter(ImageFilter.MinFilter(3))  # come a franja rosada da borda
    a[..., 3] = np.asarray(mask)
    return Image.fromarray(a)


def source_cells(name, spec):
    """Devolve {(linha, coluna): Image RGBA} do HD, de folha única ou de pasta r{R}c{C}.png."""
    cols, rows = spec["cols"], spec["rows_src"]
    key, tol = spec.get("key", "#FF00FF"), spec.get("key_tol", 90)
    folder, sheet = ART / "hd" / name, ART / "hd" / f"{name}.png"
    cells = {}
    if folder.is_dir():
        for r in range(rows):
            for c in range(cols):
                f = folder / f"r{r}c{c}.png"
                if not f.exists():
                    raise FileNotFoundError(f"falta {f.relative_to(ROOT)}")
                cells[(r, c)] = key_out(Image.open(f), key, tol)
        return cells
    if not sheet.exists():
        raise FileNotFoundError(f"falta {sheet.relative_to(ROOT)} (ou a pasta {folder.relative_to(ROOT)}/)")
    full = key_out(Image.open(sheet), key, tol)
    W, H = full.size
    for r in range(rows):
        for c in range(cols):
            box = (round(c * W / cols), round(r * H / rows), round((c + 1) * W / cols), round((r + 1) * H / rows))
            cells[(r, c)] = full.crop(box)
    return cells


# ----------------------------------------------------------------------------- redução
def place_on_canvas(img, spec, shared_scale):
    """Coloca o HD num canvas (cw*S, ch*S) já na escala final de supersample."""
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
    # recorta o que sair do canvas (nunca deveria, mas evita exceção por arredondamento)
    sx, sy = max(0, -x), max(0, -y)
    scaled = scaled.crop((sx, sy, min(tw, cw * S - x), min(th, ch * S - y)))
    canvas.alpha_composite(scaled, (max(0, x), max(0, y)))
    return canvas


def reduce_canvas(canvas, spec, pal):
    """Canvas supersample -> (idx, alpha8) na grade lógica, por moda ponderada dentro de cada bloco."""
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


# ----------------------------------------------------------------------------- limpeza
NEI8 = [(-1, -1), (-1, 0), (-1, 1), (0, -1), (0, 1), (1, -1), (1, 0), (1, 1)]


def shifted(arr, dy, dx, fill):
    out = np.full_like(arr, fill)
    H, W = arr.shape
    ys, yd = (slice(max(0, -dy), H - max(0, dy)), slice(max(0, dy), H - max(0, -dy)))
    xs, xd = (slice(max(0, -dx), W - max(0, dx)), slice(max(0, dx), W - max(0, -dx)))
    out[yd, xd] = arr[ys, xs]
    return out


def despeckle(idx, alpha):
    """Pixel opaco cuja cor não aparece em nenhum dos 8 vizinhos opacos vira a moda dos vizinhos."""
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
    """Remove ilhas opacas (4-conexas) menores que min_size pixels."""
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
    """Contorno de 1 px, na cor do vizinho escurecida e ajustada à paleta, onde a silhueta não tem contorno escuro."""
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
    """Alinha os pés de todos os quadros: mesma linha de base e mesmo centro (limitado a max_shift)."""
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
            warn(f"quadro {k}: pés fora do pivô ({dy:+d},{dx:+d}px); corrigido só até {lim}px")
        dy, dx = max(-lim, min(lim, dy)), max(-lim, min(lim, dx))
        out[k] = (shifted(idx, dy, dx, -1), shifted(alpha, dy, dx, 0))
    return out


# ----------------------------------------------------------------------------- montagem
def build_asset(name, spec, palette_name, log):
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
            sub = stabilize_feet({k: v for k, v in frames.items() if k[0] == r}, spec, lambda m, r=r: warns.append(f"linha {r}: {m}"))
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
        log(f"  aviso: {w}")
    return Image.fromarray(sheet, "RGBA")


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


# ----------------------------------------------------------------------------- verificação
def verify(name, spec, png, palette_name):
    """Devolve lista de problemas do PNG já gerado (vazia = ok)."""
    pal = Palette.load(palette_name, spec.get("ramps"))
    img = Image.open(png).convert("RGBA")
    a = np.asarray(img)
    s = spec.get("scale", 1)
    problems = []
    if "parts" in spec:
        exp = (spec["size"][0] * s, spec["size"][1] * s)
    else:
        exp = (spec["cols"] * spec["cell"][0] * s, spec.get("rows", spec["rows_src"]) * spec["cell"][1] * s)
    if img.size != exp:
        problems.append(f"tamanho {img.size}, esperado {exp}")
    allowed = {0, 255} | ({v for _, v in SOFT_LEVELS} if spec.get("soft_alpha") else set())
    bad_alpha = set(np.unique(a[..., 3]).tolist()) - allowed
    if bad_alpha:
        problems.append(f"alfa fora do permitido: {sorted(bad_alpha)}")
    op = a[a[..., 3] > 0][:, :3]
    if len(op):
        pk = {tuple(c) for c in pal.rgb.tolist()}
        off = {tuple(c) for c in np.unique(op, axis=0).tolist()} - pk
        if off:
            problems.append(f"{len(off)} cor(es) fora da paleta")
    blocks = spec["cell"][0] * s, spec["cell"][1] * s
    if "parts" not in spec:
        rows_out = spec.get("rows", spec["rows_src"])
        empty = [(r, c) for r in range(rows_out) for c in range(spec["cols"])
                 if not a[r * blocks[1]:(r + 1) * blocks[1], c * blocks[0]:(c + 1) * blocks[0], 3].any()]
        if empty and spec.get("kind") != "tile" and not spec.get("allow_empty"):
            problems.append(f"{len(empty)} quadro(s) vazio(s), ex.: {empty[:3]}")
    return problems


# ----------------------------------------------------------------------------- manifesto / CLI
def load_manifest():
    m = json.loads((ART / "manifest.json").read_text(encoding="utf-8"))
    assets = {}
    for name, spec in m["assets"].items():
        merged = dict(m["kinds"].get(spec.get("kind"), {}))
        merged.update(spec)
        merged.setdefault("rows_src", 5 if merged.get("directions") == 5 else merged.get("rows", 1))
        assets[name] = merged
    return m, assets


def select(assets, names, prefix):
    if names:
        missing = [n for n in names if n not in assets]
        if missing:
            raise SystemExit("não está no manifesto: " + ", ".join(missing))
        return names
    return [n for n in assets if not prefix or n.startswith(prefix)]


def has_source(name, spec):
    if "parts" in spec:
        return all(has_source(p["name"], spec) for p in spec["parts"])
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
            print(f"[ERRO] {name}: {e}")
            failed += 1
            continue
        if problems:
            failed += 1
            print(f"[FALHOU] {name}: " + "; ".join(problems))
            continue
        ok += 1
        print(f"[ok] {name}  {img.size[0]}x{img.size[1]}")
        if args.install:
            RES.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(dst, RES / f"{name}.png")
    print(f"\n{ok} gerado(s), {skipped} sem fonte HD, {failed} com problema" + (" (instalados em drawable-nodpi)" if args.install and ok else ""))
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
        print(("[ok] " if not p else "[FALHOU] ") + name + ("" if not p else ": " + "; ".join(p)))
        bad += bool(p)
    return 1 if bad else 0


def cmd_status(args):
    m, assets = load_manifest()
    rows = []
    for name, spec in assets.items():
        hd = has_source(name, spec)
        built = (ART / "build" / f"{name}.png").exists()
        rows.append((name, "HD ok" if hd else "falta HD", "gerado" if built else "-"))
    w = max(len(r[0]) for r in rows)
    for r in rows:
        print(f"{r[0]:<{w}}  {r[1]:<8}  {r[2]}")
    print(f"\n{sum(r[1] == 'HD ok' for r in rows)}/{len(rows)} com fonte HD")
    return 0


def cmd_preview(args):
    """Lado a lado: HD reduzido | resultado ampliado 3x (e repetição 3x3 para tiles)."""
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
    """Extrai N cores de imagens HD aprovadas (mediana de corte) e escreve uma paleta nova."""
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
    out.write_text(json.dumps({"always": ["extraida"], "ramps": {"extraida": hexes}}, indent=1), encoding="utf-8")
    print(f"{len(hexes)} cores -> {out.relative_to(ROOT)}")
    return 0


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)

    def common(p):
        p.add_argument("names", nargs="*", help="assets do manifesto (vazio = todos com fonte HD)")
        p.add_argument("--prefix", help="só assets que começam com este prefixo, ex.: player_")
        p.add_argument("--palette", help="paleta em art/palettes (padrão: a do manifesto)")

    b = sub.add_parser("build", help="gera art/build/<nome>.png")
    common(b)
    b.add_argument("--install", action="store_true", help="copia para app/src/main/res/drawable-nodpi")
    c = sub.add_parser("check", help="valida PNGs gerados (tamanho, paleta, alfa, quadros vazios)")
    common(c)
    c.add_argument("--installed", action="store_true", help="valida os que estão em drawable-nodpi")
    common(sub.add_parser("status", help="mostra o que tem fonte HD e o que já foi gerado"))
    common(sub.add_parser("preview", help="gera comparação HD x pixel em art/preview"))
    pe = sub.add_parser("palette-extract", help="cria paleta a partir de imagens HD aprovadas")
    pe.add_argument("images", nargs="+")
    pe.add_argument("--colors", type=int, default=48)
    pe.add_argument("--name", default="extraida")
    args = ap.parse_args(argv)
    return {"build": cmd_build, "check": cmd_check, "status": cmd_status, "preview": cmd_preview,
            "palette-extract": cmd_palette_extract}[args.cmd](args)


if __name__ == "__main__":
    sys.exit(main())
