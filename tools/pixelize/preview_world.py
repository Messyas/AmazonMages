"""Build reproducible art direction previews; these are not playable screenshots.

Run with the same Pillow/NumPy environment as pixelize.py:
    python tools/pixelize/preview_world.py
"""
import hashlib
import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

import pixelize as px


def main():
    manifest, assets = px.load_manifest()
    output = px.ART / "world-v1"
    output.mkdir(parents=True, exist_ok=True)
    names = ["world_overview_concept_v1", "world_camera_concept_v1", "world_tree_broadleaf_v1"]
    report = {"status": "Art direction references; runtime integration and Android evidence are documented in art/world-v1/README.md", "assets": {}}
    for name in names:
        spec = assets[name]
        palette = spec.get("palette", manifest["palette"])
        result = px.build_asset(name, spec, palette, print)
        path = output / f"{name}_reduced.png"
        result.save(path, optimize=True)
        issues = px.verify(name, spec, path, palette)
        if issues:
            raise ValueError(f"{name}: {issues}")
        # Rebuild the same source to verify determinism on the actual generated art.
        rebuilt = px.build_asset(name, spec, palette, print)
        if result.tobytes() != rebuilt.tobytes():
            raise AssertionError(f"Non-deterministic output: {name}")
        source = px.ART / "hd" / f"{name}.png"
        report["assets"][name] = {
            "source": str(source.relative_to(px.ROOT)).replace("\\", "/"),
            "source_sha256": hashlib.sha256(source.read_bytes()).hexdigest(),
            "source_size": list(Image.open(source).size),
            "final_size": list(result.size),
            "palette": palette,
            "colors_used": len(np.unique(np.asarray(result)[..., :3].reshape(-1, 3), axis=0)),
            "opaque": result.getchannel("A").getextrema() == (255, 255),
            "alpha_values": np.unique(np.asarray(result)[..., 3]).tolist(),
            "deterministic": True,
            "output_sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
        }

    name = names[1]
    spec = assets[name]
    palette = spec["palette"]
    native = Image.open(output / f"{name}_reduced.png").convert("RGBA")
    legacy = dict(spec, cell=[208, 117], scale=2)
    old = px.build_asset(name, legacy, palette, print)
    width, height = native.width * 2, native.height * 2
    # The HD panel uses identical cover framing without palette quantization.
    hd = px.place_on_canvas(Image.open(px.ART / "hd" / f"{name}.png").convert("RGBA"), spec, None)
    panels = [hd.resize((width, height), Image.Resampling.LANCZOS),
              native.resize((width, height), Image.Resampling.NEAREST),
              old.resize((width, height), Image.Resampling.NEAREST)]
    margin, heading, footer = 16, 46, 38
    sheet = Image.new("RGB", (width * 3 + margin * 4, height + heading + footer), "#15251E")
    draw = ImageDraw.Draw(sheet)
    font = ImageFont.load_default(size=18)
    labels = ["HD source / same framing", "New detail profile: 416 x 234", "Legacy detail profile: 208 x 117, enlarged"]
    for i, (panel, label) in enumerate(zip(panels, labels)):
        left = margin + i * (width + margin)
        draw.text((left, 14), label, fill="#F4F1D2", font=font)
        sheet.paste(panel.convert("RGB"), (left, heading))
    draw.text((margin, height + heading + 10),
              "Environment style comparison. Generated character is a scale cue; this is not an Android screenshot.",
              fill="#B8C99A", font=font)
    sheet.save(output / "world_camera_comparison.png", optimize=True)
    (output / "verification.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(f"[verified] {output.relative_to(px.ROOT)}")


if __name__ == "__main__":
    main()
