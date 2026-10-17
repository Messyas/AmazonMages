# AmazonMages art pipeline (HD → pixel art)

> **Language rule:** everything in this pipeline (prompts sent to the image AI, file and asset names, manifest, palette, tool output) is in **English**. Image generators follow English prompts more precisely. Only the **in-game text** drawn on mockups stays in Brazilian Portuguese (with correct accents).

The image AI generates everything in **HD, clean anime / cel-shaded style**. The game uses the **deterministically pixelized version**, produced by `tools/pixelize/pixelize.py`. This way the AI is more accurate (crisp drawing) and the typical AI "pixel hallucinations" (a square dissolving at the hilt of a sword, stray pixels, off-palette colours) disappear in the reduction.

```
art/
  hd/            approved HD sources (commit them). <name>.png  OR  <name>/r{R}c{C}.png  OR  <name>/r{R}.png (row strip)
  palettes/      master palette (amazon.json). Every output uses ONLY these colours
  manifest.json  what each asset is: grid (cols x rows), logical cell, scale, ramps, etc.
  build/         generated final PNGs (do not commit)
  preview/       HD vs pixel comparisons (do not commit)
tools/pixelize/  tool + tests
docs/prompts/    the prompts to send to the image AI (English)
```

## Workflow for one asset
1. Generate the HD image with the prompts in `docs/prompts/` (**flat magenta `#FF00FF` background** for cutouts). **Write the request to the image AI in English.** New UI components use **4× downsampling**, the menu background uses **3×**, and the existing sprite/legacy-kit contracts keep their recorded sizes.
2. Save it as `art/hd/<name>.png` (the name comes from `art/manifest.json`).
3. `python tools/pixelize/pixelize.py build <name>` → `art/build/<name>.png` (it already validates size, palette, alpha and empty frames).
4. `python tools/pixelize/pixelize.py preview <name>` → `art/preview/<name>.png` (HD | pixel at 3×; tiles are also shown tiled 3×3).
5. Approved? `python tools/pixelize/pixelize.py build <name> --install` copies it into `app/src/main/res/drawable-nodpi/`.

Other commands: `status` (what is missing), `check [--installed]`, `palette-extract img1.png img2.png --colors 48` (lock the palette to approved art).
Batch: `build --prefix player_`, or plain `build` for everything that has an HD source.

## What the tool does (in order)
1. Slices the sheet into an even grid (or reads separate frames / row strips).
2. Removes the magenta background (+ 1 px erosion of the fringe). If the PNG already has transparency, it is respected.
3. Fits each frame to the cell with **one single scale for the whole frame** (keeps the character size across frames).
4. Reduces with 4× supersampling, snaps to the master palette (distance in OKLab) and picks the **most frequent colour of each block**, with extra weight for dark tones (preserves outlines).
5. Cleanup: removes isolated "odd-colour" pixels and tiny islands; optional automatic outline (`"outline":"auto"`).
6. Aligns the feet of all frames (same baseline / centre, up to 3 px) and **mirrors 5 → 8 directions** (`"directions":5`).
7. Binary alpha (0/255); VFX with `soft_alpha` use only 255/160/80. Enlarges with nearest neighbour (`scale`).

## Scales (HD → logical → final)
| Type | logical cell | HD source | `scale` | final |
|---|---|---|---|---|
| character / ghost | 32×32 | 256×256 | 2 | 64×64 |
| VFX small / medium / large | 24 / 32 / 64 | 192 / 256 / 512 | 2 | 48 / 64 / 128 |
| tile | 32×32 | 256×256 | 2 | 64×64 |
| object (round tree) | 64×64 | 512×512 | 2 | 128×128 |
| new UI | 64×40 example | 256×160 (÷4) | 1 | 64×40; the renderer uses its actual cell and margins |
| approved legacy kit 6.1 | existing native sizes | native ×8 | 1 | existing PNG contracts |
| menu background | 400×180 | 1200×540 (÷3) | 1 | integer nearest-neighbor cover in the game |
| portrait, logo | — | final HD size | — | `"smooth": true` (no palette) |

For new UI, one logical pixel is **4 HD px**: outline and minimum detail ~4 HD px, without glow, gradients or fine noise. Existing sprite contracts retain their documented 8-HD-px grid.

Declare the **HD size of one frame** as `source_cell`; the converter derives the native cell from `source_cell / downsample`. The `ui` kind defaults to 4. Example: `{"kind":"ui","source_cell":[256,160],"cols":3,"rows":1}` produces three 64×40 frames (192×40 atlas). This preserves twice as many pixels per axis as reducing the same source by 8. Keep 9-slice margins in native pixels and adapt consuming renderers when integrating new assets. An explicitly declared `cell` must match the division. Legacy assets without `source_cell` retain their native `cell` unchanged.

## Per-asset tuning (`art/manifest.json`)
`ramps` (restricts the palette: less colour drift), `outline_bias` (weight of dark tones), `alpha_thr` (0.5), `despeckle`, `min_island`,
`fit` (`cell` keeps the HD framing; `trim` crops and centres, good for icons), `stabilize: "feet"`, `max_shift`, `key` / `key_tol` (background other than magenta; `null` = no keying).

## Tests
`python -m unittest tools/pixelize/test_pixelize.py -v` (synthetic art: size, palette, alpha, feet, mirroring, determinism, noise).
Dependencies: `pip install pillow numpy`.
