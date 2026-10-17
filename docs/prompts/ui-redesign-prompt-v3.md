# Prompt v3 — AmazonMages interface (UI/HUD) redesign

> **Send this whole prompt to the image AI in English, together with the 10 attached screenshots.** Self-contained: no other file is needed. The in-game texts shown on mockups stay in **Brazilian Portuguese with correct accents**, exactly as quoted.
> **Workflow:** the AI must do **only what each message asks** (one batch at a time) and start with the component kit (6.1).
> Technical background: the art is generated as **flat HD on magenta `#FF00FF`**, then reduced 4× by a script to a fixed pixel palette. The screen **mockups** are composition references only.

---

## 0. Role and goal

You are a game UI designer (16/32-bit RPG level). Redesign the **whole interface** of **AmazonMages**: a 2D action RPG for Android in **landscape**, top-down 3/4 camera (Pokémon Emerald-like perspective), **fantasy Amazon rainforest** setting, **Leaf** mage protagonist.

The attached screenshots, in order: (1) main menu, (2) gameplay HUD, (3) developer command chat, (4) save-slot selection, (5) Status & Attributes panel, (6) pause menu, (7) gameplay with the performance monitor open, (8) Inventory, (9) Map, (10) Quest Journal.

The current **gameplay HUD components are good and are the style reference**: dark wooden frame with metal corners, vines on the weapon bar, round mossy stone buttons, hearts and leaf-drops. The job is to:

1. **Standardise** whatever breaks that style (zoom button, menus, panels, text, performance monitor).
2. **Simplify** Status, Inventory, Map and Journal in the spirit of **Minecraft**: few elements, slot grids, large icons, short text.
3. Make everything **cohesive with the forest**: dark wood, vines, leaves, mossy stone, greens and gold.

Do not copy assets from Minecraft, Pokémon or any other game; only borrow the *idea* of the organisation. Every drawing is original.

**Reply "ready" and wait for my first request.**

## 1. Problems seen in the screenshots (fix all)

1. **"1×" zoom button** (right side, under the menu button): flat green rectangle, out of style.
2. **Panels (Status, Inventory, Map, Journal)**: flat green background, small loose text, almost invisible `+` buttons, flat "FECHAR PAINEL (X)" button, clipped title, long lists, hard to read.
3. **Text** in mixed sizes and fonts, UPPERCASE **without accents** ("DIARIO DE MISSOES", "Posicao"). It must be **one single family with correct accents**.
4. **Main and pause menus**: generic dark-green buttons, no themed frame, long labels ("Gráficos (resolução e FPS)").
5. **Command chat and performance monitor**: opaque boxes covering the HUD; the monitor, stuck in the top-left corner, hides health, mana and the weapon bar.
6. The screen has a **black bar on the left (~52 px, camera cut-out) and a system navigation bar at the bottom (~30 px)**. Nothing important may sit there.

## 2. Technical rules (mandatory)

### 2.1 How the art is used
- You deliver **flat HD**: each *logical* pixel of the final game = **4 × 4 px** of your image. A script downsizes it 4×, snaps it to a fixed palette and picks the **most frequent colour of each 4×4 block**. The game then enlarges the result by an integer factor with nearest neighbour.
- Consequences: **anything smaller than 4 px disappears or becomes noise** (thin lines, fine texture, tiny highlights, text); **gradients, noise, glow and blur become blotches**; **flat areas with few colours and a uniform dark outline become clean pixel art.**
- Each component is a **horizontal strip of equal frames** (or one image) whose **HD size = logical size × 4** (table 6.1). If the generator cannot hit the exact size, deliver the **largest size that keeps all frames equal**, never cropped.
- **Background: flat magenta `#FF00FF`**, no gradient, no cast shadow, no frame. **No magenta, hot pink or neon purple inside any drawing.**
- **No text inside components** (the game draws all text). No watermarks, no signatures.
- **Outline:** continuous, uniform, **4 HD px** (= 1 logical px), in the darkest tone of its own ramp, never pure black.
- **Flat cel shading:** at most **3 tones per material** (+ 1 highlight). No gradient, no noise, no fine texture, no dithering, no glow, no blur, no partial transparency.
- **Minimum detail size 4 HD px.** Icons: one or two large simple shapes.
- Light from the **top-left**.

### 2.2 Reference screen: 1200 × 540 px, landscape
Safe area: **x from 56 to 1200, y from 0 to 510**; inner margin ≥ 12 px. Minimum touch target **48 × 48 real px**.

### 2.3 Palette (use these ramps, shadow → light)

| Use | Colours |
|---|---|
| Dark wood (frames) | `#2B1A12` `#4A2E1F` `#6B4529` `#8F6238` |
| Mossy stone (buttons) | `#2E3B2A` `#4A5E3C` `#6F8A52` `#A8C27A` |
| Leaf (accent) | `#1E6436` `#3DC45E` `#9BF06A` |
| Gold (titles, selection) | `#7A5A1E` `#C99A2E` `#F2D36B` `#FFF1B0` |
| Panel background | `#10241A` `#17321F` `#1F4228` |
| Danger / close | `#5A1E1A` `#9C3A2E` `#E06A4E` |
| Text | `#F4EBC8` cream · `#B8C99A` secondary |
| Outline | `#0B1A12` `#14261C` |
| **Performance monitor text** | **`#5CC8FF` cyan-blue**, outline `#06233A`; warnings `#FFE066` and `#FF6A5E` |

### 2.4 Typography
- **Do not draw a font.** The game will use a real pixel font with a free licence (e.g. Pixelify Sans or Silkscreen). On mockups, imitate **one clean pixel font with full Portuguese accent support** (á é í ó ú ã õ ç), the same in every screen.
- Sizes in **real** pixels on the 1200×540 screen:

| Role | Letter height | Colour |
|---|---:|---|
| Panel title | 28 px | gold `#F2D36B` with shadow `#1B1008` |
| Subtitle / item name | 20 px | cream |
| Body / description | 16 px | cream / secondary |
| Slot quantity | 14 px | white with dark outline |
| Button hint | 14 px | secondary |

- **Never below 14 px.** At most 40 characters per line, 3 lines per description. Titles in sentence case **with accents** ("Diário de Missões", "Inventário").

## 3. Two art families

**Current user preference (2026-10-17):** downsample `bg_menu` by **3×** (1200×540 → 400×180), and **new UI components by 4×**. Keep `portrait_player` and `ui_logo` in HD. The approved legacy kit 6.1 remains recorded at its existing 8× sizes; the component table below specifies new deliveries. HD source frame sizes stay the same, so new 4× PNGs contain twice as many pixels per axis as the old 8× versions. Update consuming frame sizes and 9-slice margins when integrating them.
- **UI components (pixel, reduced by the script):** panels, buttons, slots, icons, bars, cursor.
- **Portrait and logo (anime HD, not reduced):** `portrait_player` (the Leaf mage: messy brown hair, aqua-green eyes, large red wizard hat with a leaf on the ribbon, red robe, teal shirt; 1024×1024, magenta background) and `ui_logo` ("AMAZON MAGES" logo with leaves, 840×180, magenta background). Soft shading allowed here.
- **Menu scene:** `bg_menu`, forest at dusk, 1200×540 HD source, opaque, no text; downsample 3× to 400×180 and enlarge with integer nearest neighbor.

## 4. Design principles ("Minecraft" style, forest edition)
1. **Slot grids before lists.** Items are icons in slots; name and description appear in a **detail panel** only for the selected item.
2. **Fewer screens.** Status + Inventory become one: **"Mochila"** (Backpack) with 2 tabs. Map and Journal stay, with the same skeleton.
3. **Same skeleton in every panel**: wooden frame, centred title on a ribbon, round **X** button at the top-right, content. No spelled-out "Fechar".
4. **One focus per screen**, ≥ 16 px of breathing room between groups.
5. **Visible feedback**: selected slot with a gold border pulsing over 2 frames; pressed button drops 1 logical px; locked item shows a vine padlock.
6. **Forest coherence**: small leaves on corners, vines only on the sides (never behind text), mossy stone on buttons, no flat rectangles, no grey.

## 5. Gameplay HUD placement (1200 × 540)
Real pixels, origin top-left; everything inside the safe area.

| Element | Position / size | Note |
|---|---|---|
| Health (hearts) | x 64, y 12; art 9×9 ×3 | one row, top |
| Mana (leaf-drops) | x 64, y 52 | same scale |
| XP bar + level badge | x 64, y 90, width ≈ 220 | gold badge on the right |
| Weapon hotbar (vertical, 4 slots) | glued to the left, x 64, centred at y ≈ 270; art slot 24 ×3 | keeps wood + vine |
| Movement joystick | centre ≈ (260, 400), radius ≈ 70 | exists |
| Parry button | centre ≈ (770, 465) | stone |
| Special button | centre ≈ (925, 465) | stone |
| **Aim joystick (new)** | centre ≈ (1080, 420), radius ≈ 55 | mossy stone ring; the attack button (leaf blade icon) is the knob. While dragging, a green-gold dotted line with an arrow appears in the world |
| Weapon swap button | centre ≈ (1055, 320) | above the aim joystick |
| Top icon bar | top-right, centred at x ≈ 900; 5 slots 48×48, gap 12 | chat, **backpack**, map, journal, one free |
| Menu button | x 1150, y 48, circle ≈ 56 | exists |
| **Zoom button (redesign)** | x 1150, y 118, circle ≈ 56 | mossy stone; **magnifier icon** + factor **1× / 2× / 3×** in 16-px gold |
| **Performance monitor** | see 6.8 | translucent, overlaid, blue |

## 6. What to generate (only what I ask for in each message)

For each request give: **(a)** the HD components (magenta background), **(b)** a **1200×540 mockup** of the screen over the matching attached screenshot (reference only), **(c)** the measurements (9-slice margins, frame sizes).

### 6.1 Component kit (first request)

| Asset | Frames | Logical → **HD size** | Content |
|---|---|---|---|
| `ui_panel` | 1 | 64×64 → **256×256** | 9-slice panel: dark wooden frame with small leaves and golden corners; **flat interior** `#10241A`. Corners ≈ 80 HD px, **identical** borders on all 4 sides |
| `ui_panel_title` | 1 | 96×40 → **384×160** | title ribbon (wood with vine), horizontal 9-slice |
| `ui_button` | 3 | 64×40 → **256×160** each | normal, pressed (1 logical px lower), disabled; mossy stone; 9-slice |
| `ui_button_round` | 3 | 64×64 → **256×256** each | normal, pressed, disabled (zoom, menu, X) |
| `ui_close` | 2 | 40×40 → **160×160** each | round red-earth **X** with a leaf; normal, pressed |
| `ui_slot` | 4 | 48×48 → **192×192** each | normal, selected (gold border), selected-2 (lighter gold), locked (vine padlock) |
| `ui_tab` | 2 | 80×32 → **320×128** each | active (gold, 2 logical px higher) and inactive; 9-slice |
| `ui_divider_h` / `ui_divider_v` | 1 | 128×12 → **512×48** / 12×128 → **48×512** | divider with a leaf in the middle |
| `ui_bar_frame` | 1 | 128×24 → **512×96** | bar frame (XP, posture), 4-logical-px caps |
| `ui_bar_fill` | 3 | 32×12 → **128×48** each | green, gold, amber fill (flat, one light stripe) |
| `ui_stepper` | 3 | 40×40 → **160×160** each | very visible round **+** button: normal, pressed, disabled |
| `ui_icons` | 18 | 32×32 → **128×128** each | in this order: magnifier, backpack, attributes (leaf star), map, journal (scroll), chat (bubble), menu (three leaves), pause, sound, graphics (monitor), save, back (vine arrow), heart, leaf-drop, shield, leaf sword, hand, padlock. Single large shape each |
| `ui_cursor_select` | 2 | 56×56 → **224×224** each | pulsing selection frame (2 frames) |
| `ui_perf_overlay` | 1 | 32×32 → **128×128** | 9-slice monitor background (dark rounded rectangle, 4-logical-px corners) |

Also deliver `portrait_player`, `ui_logo` and `bg_menu` when I ask for the main menu.

### 6.2 Screen: Main menu
- Background `bg_menu`. `ui_logo` centred at x 600, y 70–160 (≈ 420×90 real px). Subtitle "Sua aventura na floresta começa aqui" at 16 px.
- **3 buttons** 320×56 real px, stacked and centred: **"Jogar"** (y 230), **"Opções"** (y 300), **"Sair"** (y 370), 20-px labels. Also show one pressed button.

### 6.3 Screen: Save-slot selection
- Title "Suas Jogatinas" (28-px gold); no long sentence.
- **3 cards side by side**, ≈ 340 × 300, x 90 / 430 / 770, y 130.
- Saved card: cropped `portrait_player` bust, "Jogatina 1", "Nível 1", short date `17/10/26 12:24`, a **"Continuar"** button and a small **trash** button (red-earth, icon).
- New card: dashed mage silhouette, "Nova aventura", **"Começar"** button.
- Back button (vine arrow) bottom-left.

### 6.4 Screen: Pause
- Centred panel 440 × 400, title "Pausado", **4 buttons** 360 × 48 with 16-px gaps: **"Continuar"**, **"Opções"**, **"Salvar"**, **"Menu principal"**. Round **X** in the corner = resume.
- Game darkened (black 45%), **no stacked panels** behind it.

### 6.5 Screen: Options
- Panel 640 × 420 with **3 tabs**: **"Áudio"**, **"Gráficos"**, **"Controles"**.
- Audio: 3 sliders ("Música", "Efeitos", "Geral"): wooden track, leaf knob, percentage on the right.
- Graphics: `‹ value ›` selectors for **"Resolução"** (100, 85, 75, 60, 50%) and **"Taxa de FPS"** (60, 30); 14-px hint "Menos resolução = mais bateria".
- Controls: size of buttons and aim stick (P / M / G) and default zoom (1×, 2×, 3×). **"Voltar"** button at the bottom.

### 6.6 Screen: Mochila (Status + Inventory merged)
Centred **960 × 440** panel; title "Mochila"; **X** in the corner; **2 tabs**: **"Itens"** and **"Atributos"**.

**Itens tab**
- **Left** (x 80–540): **6 × 4** slot grid, 72-px slots, 4-px gaps; quantity bottom-right; selection with `ui_cursor_select`.
- **Right** (x 580–900): detail panel: large 64×64 icon, name (20 px), type tag ("Consumível", "Arma", "Catalisador"), up to 3 description lines (16 px), **"Usar" / "Equipar"** button (320×48).
- **Bottom**: row of **4 shortcut slots** (the hotbar), numbered 1–4, and an **"Atribuir"** button.

**Atributos tab**
- Header: "Nível 1", XP bar, gold tag "Pontos: 0". On the left, `portrait_player` about 240 px wide.
- On the right, a **2 × 4 grid of cards** ≈ 200 × 64: 32×32 icon, name (20 px), value (28 px) and a very visible `ui_stepper` (+), **active only when points are available**. Attributes: Vigor (heart), Mente (leaf brain), Resistência (shield), Inteligência (book), Destreza (feather), Arcano (crystal), Força (fist).
- 14-px hint line at the bottom ("Vigor: +2 de vida por ponto").

### 6.7 Screens: Map and Journal
**Map** (960 × 440, "Mapa da Floresta"): **large centred** pixel-art map (≈ 560 × 392) in a parchment frame; pulsing player marker; **icon legend** on the right (Mata, Água, Ácido, Árvores; 16 px); `+` / `−` map zoom buttons. No stray "Posição atual".

**Journal** (960 × 440, "Diário de Missões"): list of cards (max 4 visible, scrolling with a vine bar), each 880 × 80: seal icon (active = gold, locked = padlock, done = green leaf), title 20 px, one objective line 16 px, thin progress bar. No "Bloqueada" tag.

### 6.8 Performance monitor (translucent overlay)
- **Position:** strip at the **top centre, x 340–770, y 8–120**, clear of health, mana, hotbar and joysticks. (Alternative: bottom centre, x 400–800, y 470–505.)
- **Background:** `ui_perf_overlay`, rendered by the game at **black 35% opacity**, 1-px cyan-blue border at 50%. The game stays visible behind it. **Overlaps the world; does not intercept touches.**
- **Text:** **cyan-blue `#5CC8FF`**, 1-px `#06233A` outline, 16 px. **Yellow `#FFE066`** when FPS < 50, **red `#FF6A5E`** when FPS < 30 or memory is high.
- **5 short lines**, no long labels:
  1. `FPS 59.6   UPS 59.6`
  2. `Update 0.51 ms   Draw 2.32 ms`
  3. `Inimigos 1   Projéteis 0   Magias 0`
  4. `Heap 5.9/256 MB   Nativa 116 MB`
  5. `Bateria 86% · 36 °C · Térmico normal`
- Optional: frame-time mini graph (60 bars of 2 px, blue, dashed 16.6 ms line).

### 6.9 Command chat (developer mode)
- **Low, translucent** panel 720 × 280, anchored at the **bottom centre** (x 240–960, y 220–500), so the character stays visible.
- 3-line log (14 px), one-line input and an **"Enviar"** button on the right (96 × 40). The **FPS** button becomes a monitor icon in the title bar that toggles the monitor from 6.8.

## 7. Request template (what I will send in each message)

REQUEST
Asset: ui_slot
Frames: 4 side by side · final image 768×192 (each frame 192×192) · flat magenta #FF00FF background
Content: [row from table 6.1]
Deliver only the image, then answer the checklist in section 8.

## 8. Delivery checklist (answer item by item)

- [ ] Flat HD on **magenta `#FF00FF`**, at the HD size from the table (logical × 4), equal frames, no gaps.
- [ ] **No text** inside components; no magenta/pink in the drawing.
- [ ] Uniform dark 4-HD-px outline; ≤ 3 tones per material; no gradient / noise / glow / blur; no detail < 4 px.
- [ ] 9-slice panels and buttons with **identical** borders on all sides, margins reported.
- [ ] Palette ramps from 2.3; no flat green, no grey.
- [ ] Round zoom button with magnifier and 1× / 2× / 3× (text drawn by the game).
- [ ] One Mochila with 2 tabs; slot grid instead of lists; anime portrait on Atributos.
- [ ] Same skeleton (frame, title ribbon, round X) in Mochila, Map, Journal, Pause, Options.
- [ ] Performance monitor translucent, `#5CC8FF`, 5 lines, clear of health / mana / hotbar / joysticks.
- [ ] Aim joystick and swap button placed as in section 5.
- [ ] Touch targets ≥ 48 real px; nothing in x < 56 or y > 510.
- [ ] Mockups at 1200×540 with accented Brazilian Portuguese text in one pixel font.
- [ ] Nothing copied from an existing game.
