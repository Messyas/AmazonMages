# Prompt v2 — HD art for AmazonMages (HD → automatic pixelization workflow)

> **Language rule: send this prompt to the image AI in English** (the whole block below, as written). English gives noticeably more precise results. Talk to the user in Portuguese, but never translate this prompt. The only text that stays in Portuguese is in-game text that appears inside UI mockups.
>
> **How to use.** Always paste the **fixed block (sections 0 to 3)** + **one batch at a time (section 4)**, and attach the approved reference images (the character turnaround sheet, the palette). Request **one animation row (one direction) per request**, never the whole sheet.
> What you generate here is **not pixel art**: it is clean HD art. A script (`tools/pixelize/pixelize.py`) reduces and converts it to pixel art deterministically, which is why this prompt asks for different things than v1.
> Full workflow, commands and tuning: `art/README.md`.

---

## 0. Role and goal

You are a senior character and 2D game illustrator, expert in **clean anime / cel-shaded art**. You will produce the HD art for **AmazonMages**, a 2D action RPG for Android (landscape, phone) with a **top-down 3/4 camera** in the spirit of GBA-era handheld RPGs ("Pokémon Emerald"), real-time combat, elemental magic, parry and a skill tree.

Setting: **fantasy Amazon rainforest**. Protagonist: a young **Leaf / Nature** mage (cutting leaves, vines, roots, glowing spores).

Inspiration, not copying: use the visual language (perspective, proportions, readability on a small screen). No characters, tiles, logos or existing assets from Pokémon, Minecraft or any other game.

## 1. Why the rules below exist (read this)

Your image will be **downscaled 8×** to a tiny grid (a character ends up ~28 px tall) and **converted to a fixed palette**. The reduction picks, for each final pixel, the **most frequent colour** of the 8×8 px block of your image. Consequences:

- Anything **smaller than ~8 px (3% of the cell)** disappears or turns into noise: hair strands, fabric texture, tiny highlights, thin lines, text.
- **Gradients, noise, grain, glow and blur** become random blotches. **Flat areas with few colours** become clean pixel art.
- A **uniform dark outline** survives the reduction and gives the look of hand-made pixel art.
- The **flat magenta background** is removed automatically; anything that is not background becomes character.

So: **draw big, clean, flat, with strong shapes.** The closer it looks to an "anime sticker with 3 tones per material", the better the final result.

## 2. Image rules (mandatory, apply to everything)

### 2.1 Background
- **Pure magenta `#FF00FF`, flat, no gradient, no cast shadow, no ground, no frame, no text.**
- **No magenta, hot pink or neon purple anywhere inside the drawing** (flowers and magic use other colours).
- Exception: **ground tiles** and **menu backgrounds** are opaque and fill the whole cell.

### 2.2 Framing and size
- **Every frame is a square of fixed size** (table 2.3). All frames in one request have the **same size**, with no gap between them, no grid lines and no numbers.
- The character is **horizontally centred**, with the **feet resting on the line at 90% of the frame height** (for 256 px: y = 230), **in every frame and direction**. Keep at least 6% free margin on every side. Nothing touches the border.
- **Same scale in every frame and file**: the character's total height (feet to hat tip) is **≈ 215 px in a 256 frame**. Do not zoom in or out between animations.
- Deliver the image at the **exact requested size**; if the generator cannot, deliver the **largest possible size keeping all frames equal** (the script resizes it), never cropped.

### 2.3 Cell sizes (HD = logical × 8)

| Type | HD frame | Becomes (logical → final) |
|---|---|---|
| Character, ghost | **256 × 256** | 32 → 64 px |
| Leaf projectile, cast flash | **192 × 192** | 24 → 48 px |
| Shield, impact, parry, posture break, special warning | **256 × 256** | 32 → 64 px |
| Leaf storm | **512 × 512** | 64 → 128 px |
| Status icon (Rooted) | **128 × 128** | 16 → 32 px |
| Ground tile | **256 × 256** | 32 → 64 px |
| Bush, flower, rock, fence, sign | **256 × 256** | 32 → 64 px |
| Round tree | **512 × 512** | 64 → 128 px |
| Palm tree | **512 × 768** | 64×96 → 128×192 |
| Kapok (giant tree) | **768 × 768** | 96 → 192 px |
| Character portrait (anime version) | **1024 × 1024** | used as is (512 × 512) |

### 2.4 Line and shading (the heart of the method)
- **Outline**: continuous, **uniform, ≈ 8 px in a 256 frame** (3%), in a **dark coloured tone** (the darkest tone of its own ramp, never pure black). On large objects scale it proportionally (512 tree → 16 px).
- **Cel shading**: at most **3 tones per material** (shadow, base, light) **+ 1 optional highlight**. **No gradients, no noise, no cloth/skin/paper texture, no dithering, no hatching.**
- Light always comes from the **top-left**. Contact shadow on the ground under characters and objects: **a flat ellipse of one dark tone, solid** (the shadow is part of the drawing, not of the background).
- **Minimum detail size 8 px**: every eye, button, hat star and finger must be a readable shape of at least 8×8 px. Prefer a few large details to many small ones.
- **No partial transparency, no glow, no blur, no motion blur, no tiny particles, no lens flare.** Magic glow = **solid concentric bands** of lighter colour (up to 3).
- 3/4 perspective: tops of objects visible, front face slightly visible; cliffs show the vertical face; trees show the canopy from above and the trunk below.

### 2.5 Palette and ramps
Use **these ramps** (shadow → light). The script converts everything to them; the closer you stay, the better.

| Ramp | Colours |
|---|---|
| Grass | `#1D5A2C` `#2C8A3A` `#46B144` `#74D25A` `#B4EC78` |
| Foliage | `#123F2A` `#1E6436` `#2F8B3E` `#55B24A` `#94D768` |
| Dirt / trail | `#5A3A24` `#86593A` `#B38455` `#D6AF78` `#EED7A4` |
| Cliff | `#4A2E2A` `#7A4A3C` `#A8705A` `#C99A7C` |
| Water | `#1A3E7A` `#2861B0` `#3E8FD6` `#7CC4F0` `#D2F2FF` |
| **Leaf magic** | `#0F4A2E` `#1E8A4A` `#3DC45E` `#9BF06A` core `#F4F7A0` |
| Acid (enemy) | `#4A6E12` `#8FD12A` `#C6F032` + purple `#5A2A8A` `#8A3FD0` `#B98AF0` |
| Lava | `#3A1210` `#7A2418` `#C4452A` `#F08A38` `#FFD05A` |
| Hero: outfit | `#5A1226` `#9A2438` `#D43E48` `#F07A6A` |
| Hero: skin | `#8A4F3A` `#C98660` `#F0BE94` |
| Hero: hair | `#3A2216` `#6B4328` |
| Hero: shirt | `#1C4A52` `#2D7C80` |
| Outline | `#0B1A12` `#14261C` |
| Gold | `#7A5A1E` `#C99A2E` `#F2D36B` `#FFF1B0` |

The player's Leaf magic **must not be confused** with the enemy's acid: **emerald / forest green with a golden core, shaped like a sharp leaf** (points, central vein), never a round blob. The acid is amorphous and bubbling, lime green with purple eyes.

### 2.6 Directions (code contract)
Draw **5 views**, in this order, top to bottom: **S** (facing the camera), **SE**, **E** (profile facing right), **NE**, **N** (back view). The script mirrors E, NE, SE to produce W, NW, SW. **Therefore the character must not carry an asymmetric item that changes meaning when mirrored** (the staff is always in his left hand, etc.). If asymmetry is needed, say so: the asset then requires all 8 directions.
- Columns = animation frames, left to right.

### 2.7 Forbidden
Text, numbers, signatures, watermarks, frames, visible grids, any background that is not flat magenta, scenery behind the character, a different camera per frame, a character cropped by the border, extra hands/fingers, hair as individual strands, photographic textures, characters/logos from existing games.

## 3. Two deliverables per character

Every character has **two versions of the same design**:

**A. Anime version (portrait).** Full-body or bust HD illustration, normal anime proportions, soft shading **allowed**, rich detail. **It does not go through the reduction**: it is used as is in dialogues, the save screen, the Backpack and the menu. Flat magenta background (the script cuts it out). 1024 × 1024.

**B. Mini version (sprite).** The same character in **chibi** form, to become the sprite: head + hat ≈ 40–45% of the height, compact body, large simple hands and feet, **flat cel shading and thick outline** (section 2.4). This is the only one that feeds the pixelization. It must be the same character as version A (colours, hat, bracelet, leaf on the ribbon).

### Protagonist identity
- Young mage, messy brown hair, aqua-green eyes.
- **Large wizard hat with a wide brim and a drooping tip**, red with a few large golden stars, and **a leaf / sprout tucked into the ribbon**.
- Long open red robe, teal shirt underneath, dark boots.
- **Vine bracelet** on the casting forearm; hands glow green when charging magic.
- Red on the body + green magic contrasts with the grass; **do not make the whole character green.**

## 4. Production batches

> One batch per request. File name = asset name in `art/manifest.json`. Save in `art/hd/`. **Row** = one direction, with all animation frames side by side.

### Batch 1 — Portrait and model sheet (approve before animating)

| Deliverable | File | Size | Content |
|---|---|---|---|
| Anime portrait | `art/hd/portrait_player.png` | 1024×1024 | bust or full body, neutral-determined expression, magenta background |
| Mini sheet (8 directions) | `art/hd/player_turnaround.png` | **2048×256** (8 frames of 256) | order **S, SE, E, NE, N, NW, W, SW**, neutral standing pose, feet on the 90% line |
| Extra reference (optional, not shipped) | `art/hd/_ref/player_closeup.png` | free | hat, bracelet and boots details |

**Stop here and approve.** Then run `python tools/pixelize/pixelize.py build player_turnaround` and `preview`: if the character is not readable at ~28 px, go back and simplify (fewer details, bigger shapes). Attach the approved sheet to **every** following request.

### Batch 2 — Player animations (mini version, 1 row per request)

For each animation, request **5 separate rows (S, SE, E, NE, N)**. Each row is one image **N × 256 wide by 256 tall**, saved as `art/hd/<asset>/r0.png` (S), `r1.png` (SE), `r2.png` (E), `r3.png` (NE), `r4.png` (N). Attach the approved turnaround and the approved S row to keep consistency.

| Asset | Frames (N) | Ticks/frame (60 UPS) | Loop | Content |
|---|---:|---:|---|---|
| `player_idle` | 4 | 14 | yes | breathing (1 logical px = 8 HD px), hat tip swaying, robe hem moving, blink on frame 3, a small leaf floating near the hand |
| `player_move` | 6 | 6 | yes | contact–passing–contact, head bobs 8 px, arms opposite to legs, robe and hat lag 1 frame |
| `player_attack` | 6 | 5 | no | 1 anticipation → 2 charge → **3 release** (arm extended, green flash; the projectile spawns here) → 4 follow-through → 5–6 recovery |
| `player_hurt` | 3 | 6 | no | knocked back, eyes shut, hat crooked |
| `player_death` | 8 | 8 | no | kneels, falls, hat slides off, leaves wither |
| `player_special` | 8 | 6 | no | (future) hands up, whirlwind of leaves, glow on the ground |
| `player_parry` | 4 | 4 | no | (future) quick arm swing, shield of leaves opens in front |
| `player_dodge` | 5 | 4 | no | (future) roll / side step with a leaf trail |

Animation rules: **the feet do not leave the baseline** (except dodge and death); keep the size constant; anticipation and follow-through with **large** shapes (the difference between frames must be at least 8 HD px to show up).

### Batch 3 — Leaf magic VFX (magenta background, no partial transparency)

| Asset | Frames | HD frame | Directions | Description |
|---|---|---|---|---|
| `leaf_projectile` | 4 per direction | 192 | 5 rows (S, SE, E, NE, N) | emerald leaf blade with a light vein and golden core, spinning around its axis; 2 **large** trailing leaflets |
| `leaf_cast_flash` | 5 | 192 | — | ring of leaves that opens and fades, at the hand |
| `leaf_impact` | 6 | 256 | — | leaf fragments + green-gold sparks (large) + flat ground ring in 3/4 |
| `leaf_storm` | 10 | **512** | — | ascending spiral vortex, solid elliptical shadow on the ground; 1–2 forming, 3–8 loop, 9–10 dissipating |
| `leaf_storm_warning` | 4 | 256 | — | ground circle of sprouts / roots that pulses |
| `leaf_shield` | 4 per direction | 256 | 5 rows | **frontal** shield (≈ 100° arc) of overlapping leaves, never a 360° bubble |
| `parry_flash` | 5 | 256 | — | green-white 4-point star + large petals |
| `posture_break` | 6 | 256 | — | crack / ring breaking over the target |
| `status_rooted` | 4 | 128 | — | roots-binding icon, simple and large |

### Batch 4 — World tiles (opaque, fill the whole 256×256 frame)

**Must tile seamlessly**: the left/right and top/bottom edges match each other. **No vignette, no frame, no darkened border.** Each file is a horizontal strip of frames.

- `tile_grass` — 8 frames: 4 base grass variants (combinable in any order) + 4 overlays (clover, small yellow flower, pebbles, fallen leaves) **on magenta** (overlays have a magenta background).
- `tile_tall_grass` — 5 frames: 2 variants + 3 sway frames; magenta behind the grass.
- `tile_path` — 13 frames (autotile): centre, 4 edges, 4 outer corners, 4 inner corners. **Wavy** edges, grass tufts creeping in.
- `tile_water` — 17 frames: 4 animated water + a 13-piece bank autotile.
- `tile_ledge` — 4 frames: left cap, middle ×2, right cap.
- `tile_cliff` — 13 frames (autotile).
- `tile_ground` — 3 packed-earth variants.
- `tile_lava` and `tile_acid_swamp` — 4 frames each.

Contrast rule: **~50% of every ground tile must be smooth base colour** (the player needs a place to "rest" the eye).

### Batch 5 — Scenery objects (magenta background)

- `obj_tree_round` — 3 variants **512×512** (irregular lobed canopy, visible trunk below, roots, solid shadow).
- `obj_tree_palm` — 2 variants **512×768** (açaí / buriti with fruit bunches).
- `obj_tree_giant` — 1 kapok **768×768** (buttress roots).
- `obj_bush` (4), `obj_flowers` (6), `obj_rock` (4, mossy): **256×256** each, different silhouettes.
- `obj_fence` (3 pieces: left, middle, right), `obj_sign` (1): **256×256**.

### Batch 6 — HUD and interface

See `ui-redesign-prompt.md` (same workflow).

## 5. How to build each request (template to paste)

```
[FIXED BLOCK: sections 0 to 3]
[ATTACHED REFERENCES: approved turnaround, approved S row, etc.]

REQUEST
Asset: player_attack
Row: SE (r1) · 6 frames side by side · final image 1536×256 (each frame 256×256)
Background: flat magenta #FF00FF. Feet on the line y=230 of each frame. Character height ≈ 215 px.
Frame content: [copy the row from the Batch 2 table]
Deliver only the image. Then answer the checklist in section 7.
```

## 6. Workflow after you deliver (for the human)

1. The user saves the image in `art/hd/…` and runs `build` + `preview`.
2. If something looks off, the fix is **in the prompt** (you) **or in the manifest** (script parameters). Quick table:

| Symptom in the preview | Likely cause | Who fixes it |
|---|---|---|
| A detail vanished (hat star, eye) | it was smaller than 8 px | you: make the detail bigger |
| Outline thin, broken or uneven | outline < 8 px or light colour | you: 8 px dark-toned outline |
| Colour blotch inside the sprite | gradient or texture | you: 3 flat tones |
| Character "jumps" between frames | feet off the 90% line | you: re-anchor; the script fixes up to 3 px |
| Odd colour (green where it should be blue) | colour outside the ramps | you: use table 2.5; user: `ramps` in the manifest |
| Pink fringe on the edge | anti-aliased magenta | user: larger `key_tol` |
| Sprite too big / too small | different frame scale | you: height ≈ 215/256 |
| Tile with a visible seam | not tileable | you: redo with matching edges |

## 7. Delivery checklist (answer item by item)

- [ ] **Flat magenta #FF00FF** background, no gradient / shadow / ground; no magenta or hot pink inside the drawing.
- [ ] Frames at the exact table 2.3 size (or proportional), no gaps, no grid, no text.
- [ ] Feet on the **90%** line in every frame; character height ≈ **84% of the frame** (215/256); nothing cropped at the border.
- [ ] Dark coloured outline of ≈ 3% of the cell, continuous and uniform.
- [ ] At most 3 tones per material (+1 highlight); **no** gradient, noise, texture, dithering, glow or blur.
- [ ] No detail smaller than 3% of the cell.
- [ ] Light from the top-left; solid contact shadow under characters / objects.
- [ ] Colours within the section 2.5 ramps; Leaf magic emerald + gold, leaf-shaped (≠ acid).
- [ ] Row order: S, SE, E, NE, N (5 views); nothing asymmetric that breaks when mirrored.
- [ ] Tiles seamless; ≥ 50% smooth base colour.
- [ ] Same character as the approved anime version and model sheet.
- [ ] Nothing copied from an existing game.

---

### Notes for the integrator (do not send to the generator)

- Tool: `tools/pixelize/pixelize.py` (`build`, `check`, `preview`, `status`, `palette-extract`). Tests: `python -m unittest tools/pixelize/test_pixelize.py`.
- The current assets in `drawable-nodpi` were produced by procedural scripts (`tools/generate_*.py`) and stay valid until the matching HD art is approved and installed with `--install` (same names and sizes, nothing changes in the code).
- `GhostAtlasRenderer` uses `Paint.FILTER_BITMAP_FLAG`, which blurs pixel art when scaling; draw the new assets without filtering and with integer scaling.
- Acid ghosts (`acid_ghost_*`) are already in the manifest with `ramps: acid, foliage`; use this same prompt, swapping the character identity.
- After approving the first batch, run `palette-extract` on the approved images if you want to lock the palette to the real art instead of the suggested ramps.
