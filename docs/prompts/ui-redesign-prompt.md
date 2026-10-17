# Prompt v2 — AmazonMages interface (UI/HUD) redesign (HD → pixelization workflow)

> **Language rule: send this prompt to the image AI in English** (as written below). English gives more precise results. **Exception:** the in-game texts shown on the mockups (menu labels, titles, descriptions) must be written in **Brazilian Portuguese with correct accents**, exactly as quoted in this document, because that is the language of the game.
>
> **How to use.** Send the image AI: this document (sections 0 to 4) + the current screenshots + `sprite-generation-prompt.md` v2 (magenta background, outline, ramps, sizes rules). **Request one screen or one group of components at a time**, starting with 6.1 (component kit).
> Every component is generated in **flat HD on magenta `#FF00FF`** and then reduced by `tools/pixelize/pixelize.py` to the fixed palette. The **screen mockups** are only composition references (they do not ship in the game).

---

## 0. Role and goal

You are a game UI designer (16/32-bit RPG level) and you will redesign the **whole interface** of **AmazonMages**: a 2D action RPG for Android in **landscape**, 3/4 camera, **fantasy Amazon rainforest** setting, **Leaf** mage protagonist.

The current HUD components (dark wooden frame with metal corners, vines on the weapon bar, round mossy stone buttons, hearts and leaf-drops) **are good and are the style reference**. The job is to:

1. **Standardise** whatever still breaks the style (zoom button, menus, panels, text, performance monitor).
2. **Simplify** Status, Inventory, Map and Journal in the spirit of **Minecraft**: few elements, slot grids, large icons, short text.
3. Make everything **cohesive with the forest**: dark wood, vines, leaves, mossy stone, greens and gold.

Do not copy assets from Minecraft, Pokémon or any other game; only borrow the *idea* of the organisation. Every drawing is original.

## 1. Observed problems (fix all of them)

1. **"1×" zoom button** (right side, below the menu): a plain flat green rectangle, out of style.
2. **Panels (Status, Inventory, Map, Journal)**: flat green background, small loose text, almost invisible `+` buttons, a flat "FECHAR PAINEL (X)" button, clipped title, long lists.
3. **Text** in mixed sizes and fonts, UPPERCASE **without accents** ("DIARIO DE MISSOES", "Posicao"). It must be **one single family, with correct accents**.
4. **Main and pause menus**: generic dark-green buttons, no themed frame, long labels.
5. **Command chat and performance monitor**: opaque boxes covering the HUD; the monitor, stuck in the top-left corner, hides health, mana and hotbar.
6. The screen has a **black bar on the left (~52 px, camera cut-out) and a navigation bar at the bottom (~30 px)**. Nothing important may sit in those areas.

## 2. How the art becomes UI (technical rules)

### 2.1 Pipeline
- You deliver **flat HD** (each logical pixel = **8 × 8 px** of your image). The script reduces it, converts it to the fixed palette and outputs **native 1×**; the game code enlarges it by an integer factor (×2 to ×4) with nearest neighbour.
- Each component is a **horizontal strip of equal frames** (or a single image) whose **HD size = logical size × 8**, as in the table in 6.1.
- **Flat magenta `#FF00FF` background**, no magenta or pink inside the drawing, **no text inside components** (the game draws the text), no cast shadow outside the drawing.
- **Uniform dark outline of 8 HD px** (= 1 logical px), a dark tone of its own ramp, never pure black.
- **Flat cel shading:** at most 3 tones per material (+ a highlight). No gradient, noise, fine texture, glow, blur or dithering.
- **Nothing smaller than 8 HD px** (it disappears in the reduction). Icons: large simple shapes, 1–2 elements.
- Light from the top-left.

### 2.2 Reference screen: 1200 × 540 px, landscape
Safe area: **x from 56 to 1200, y from 0 to 510**; minimum inner margin 12 px. Minimum touch target **48 × 48 real px** for any button.

### 2.3 Interface palette (use these ramps)

| Use | Colours |
|---|---|
| Dark wood (frame) | `#2B1A12` `#4A2E1F` `#6B4529` `#8F6238` |
| Mossy stone (buttons) | `#2E3B2A` `#4A5E3C` `#6F8A52` `#A8C27A` |
| Leaf (accent) | `#1E6436` `#3DC45E` `#9BF06A` |
| Gold (titles, selection) | `#7A5A1E` `#C99A2E` `#F2D36B` `#FFF1B0` |
| Panel background | `#10241A` `#17321F` `#1F4228` |
| Danger / close | `#5A1E1A` `#9C3A2E` `#E06A4E` |
| Text | `#F4EBC8` cream · `#B8C99A` secondary |
| Outline | `#0B1A12` `#14261C` |
| **Performance monitor text** | **`#5CC8FF` cyan-blue**, outline `#06233A`; warnings `#FFE066` and `#FF6A5E` |

### 2.4 Typography (the image generator does not draw the font)
- **Do not ask the image generator for a font.** Use a **real pixel font with a free licence** bundled in the app (for example *Pixelify Sans* or *Silkscreen*, both OFL on Google Fonts; **check accent support** á é í ó ú ã õ ç before adopting one).
- Fixed sizes, in **real** pixels on the 1200×540 screen:

| Role | Letter height | Colour |
|---|---:|---|
| Panel title | 28 px | gold `#F2D36B` with shadow `#1B1008` |
| Subtitle / item name | 20 px | cream |
| Body / description | 16 px | cream / secondary |
| Slot quantity | 14 px | white with dark outline |
| Button hint | 14 px | secondary |

- **Never below 14 px.** At most 40 characters per line and 3 lines per description. Titles in "Sentence case **with accents**" (Diário de Missões, Inventário).
- On the mockups, **use this same font** and write the texts correctly in Brazilian Portuguese.

## 3. Two art families

- **UI components (pixel, through the pipeline):** panels, buttons, slots, icons, bars, cursor. Flat HD → 1× pixel.
- **Portraits and scenes (anime HD, no pixelization):** `portrait_player` (the mage's portrait, 1024×1024 → used at 512×512), `bg_menu` (forest scene at dusk, 1200×540, opaque) and `ui_logo` (the "AMAZON MAGES" logo, 840×180). Soft shading allowed; magenta background on the portrait and logo; **no text** other than the logo itself.

## 4. Principles ("Minecraft" style, forest edition)

1. **Slot grids before lists.** Items are icons in slots, not text rows; name and description appear in a **detail panel** only for the selected item.
2. **Fewer screens.** Status + Inventory become a single one: the **"Mochila"** (Backpack) with 2 tabs. Map and Journal stay, with the same skeleton.
3. **Same skeleton in every panel**: wooden frame, centred title on a ribbon, round **X** button in the top-right corner, content. No spelled-out "Fechar".
4. **One focus point per screen**, with ≥ 16 px of breathing room between groups.
5. **Visible feedback**: selected slot with a gold border pulsing over 2 frames; a pressed button drops 1 logical px; a locked item shows a vine padlock.
6. **Forest coherence**: small leaves on the corners, vines only on the sides (never behind text), mossy stone on buttons, no flat rectangles or grey.

## 5. Gameplay HUD placement (1200 × 540 screen)

Coordinates in real pixels, origin at the top-left corner; everything inside the safe area.

| Element | Position / size | Note |
|---|---|---|
| Health (hearts) | x 64, y 12; 9×9 art ×3 | one row, at the top |
| Mana (leaf-drops) | x 64, y 52 | same scale |
| XP bar + level badge | x 64, y 90, width ≈ 220 | gold badge on the right |
| Weapon hotbar (vertical, 4 slots) | glued to the left, x 64, centred at y ≈ 270; 24-px art slot ×3 | keeps wood + vine |
| Movement joystick | centre ≈ (260, 400), radius ≈ 70 | already exists |
| Parry button | centre ≈ (770, 465) | stone |
| Special button | centre ≈ (925, 465) | stone |
| **Aim joystick** | centre ≈ (1080, 420), radius ≈ 55 | mossy stone ring; the attack button (leaf blade) is the knob. While dragging, a green-gold arrow / dotted line appears in the world |
| Weapon swap button | centre ≈ (1055, 320) | above the aim joystick |
| Top icon bar | top-right, centred at x ≈ 900; 5 slots 48×48, 12 gap | chat, **backpack**, map, journal, plus one free |
| Menu button | x 1150, y 48, circle ≈ 56 | already exists |
| **Zoom button** | x 1150, y 118, circle ≈ 56 | mossy stone; **magnifier** + factor **1× / 2× / 3×** in 16-px gold |
| **Performance monitor** | see 6.8 | translucent, overlaid, blue |

## 6. What to generate (one per request)

For each request: **(a)** the HD components (magenta background), **(b)** a **1200×540 mockup** of the screen assembled over the attached gameplay screenshot (reference), **(c)** the list of measurements (9-slice margins, size of each frame). Save in `art/hd/<name>.png`.

### 6.1 Component kit (approve before anything else)

| Asset (`art/hd/…`) | Frames | Logical size → **HD** | Content |
|---|---|---|---|
| `ui_panel` | 1 | 32×32 → **256×256** | 9-slice panel: dark wooden frame with small leaves and golden corners; **flat interior** in `#10241A`. Corners ≈ 80 HD px (10 logical) and **identical** borders all around |
| `ui_panel_title` | 1 | 48×20 → **384×160** | title ribbon (wood with vine), horizontal 9-slice |
| `ui_button` | 3 | 32×20 → **256×160** each | normal, pressed (1 logical px lower), disabled; mossy stone; 9-slice |
| `ui_button_round` | 3 | 32×32 → **256×256** each | normal, pressed, disabled (zoom, menu, X) |
| `ui_close` | 2 | 20×20 → **160×160** each | round red-earth **X** with a leaf; normal and pressed |
| `ui_slot` | 4 | 24×24 → **192×192** each | normal, selected (gold border), selected-2 (lighter gold), locked (vine padlock) |
| `ui_tab` | 2 | 40×16 → **320×128** each | active tab (gold, 2 logical px higher) and inactive; 9-slice |
| `ui_divider_h` / `ui_divider_v` | 1 | 64×6 → **512×48** / 6×64 → **48×512** | divider with a leaf in the middle |
| `ui_bar_frame` | 1 | 64×12 → **512×96** | bar frame (XP, posture), 4-logical-px caps |
| `ui_bar_fill` | 3 | 16×6 → **128×48** each | green, gold, amber fill (flat, with 1 light stripe) |
| `ui_stepper` | 3 | 20×20 → **160×160** each | very visible round **+** button: normal, pressed, disabled |
| `ui_icons` | 18 | 16×16 → **128×128** each | magnifier, backpack, attributes (leaf star), map, journal (scroll), chat (bubble), menu (three leaves), pause, sound, graphics (monitor), save, back (vine arrow), heart, leaf-drop, shield, leaf sword, hand, padlock — **in this order**, **single large shape, no fine detail** |
| `ui_cursor_select` | 2 | 28×28 → **224×224** each | pulsing selection frame (2 frames) |
| `ui_perf_overlay` | 1 | 16×16 → **128×128** | 9-slice background of the monitor (dark rounded rectangle, 4-logical-px corners) |

> The existing HUD icons (`hud_*`) stay valid; if you want to redo them, the same method applies (`hud_icons` 12 frames 16×16, `hud_slot` 4 frames 24×24, etc., see `art/manifest.json`).

### 6.2 Screen: Main menu
- Background: `bg_menu` (forest scene at dusk, **no text**).
- `ui_logo` centred at x 600, y 70–160 (≈ 420×90 real px). Subtitle "Sua aventura na floresta começa aqui" at 16 px.
- **3 buttons** `ui_button` 320×56 real px, stacked and centred: **"Jogar"** (y 230), **"Opções"** (y 300), **"Sair"** (y 370). 20-px labels. Mockup also showing one pressed button.

### 6.3 Screen: Save-slot selection (3 slots)
- Title "Suas Jogatinas" (28-px gold); **no** long sentence.
- **3 cards side by side**, ≈ 340 × 300 real px, x 90 / 430 / 770, y 130.
- Saved card: cropped **`portrait_player`** (bust), "Jogatina 1", "Nível 1", short date `17/10/26 12:24`, a **"Continuar"** button and a small **trash** button (red-earth, icon).
- New card: dashed mage silhouette, "Nova aventura", **"Começar"** button.
- Back button (vine arrow) in the bottom-left corner.

### 6.4 Screen: Pause
- Centred panel, 440 × 400 real px, title "Pausado", **4 buttons** 360 × 48 with 16-px gaps: **"Continuar"**, **"Opções"**, **"Salvar"**, **"Menu principal"**. Graphics and audio live inside **Opções**. **X** in the corner = resume.
- Game background darkened (black 45%), **no stacked panels** behind it.

### 6.5 Screen: Options (audio, graphics, controls)
- Panel 640 × 420, **3 tabs**: **"Áudio"**, **"Gráficos"**, **"Controles"**.
- Audio: 3 sliders ("Música", "Efeitos", "Geral") with a wooden track, leaf knob and the percentage on the right.
- Graphics: `‹ value ›` selectors for **"Resolução"** (100, 85, 75, 60, 50%) and **"Taxa de FPS"** (60, 30); 14-px hint ("Menos resolução = mais bateria").
- Controls: button and aim size (P / M / G) and default zoom (1×, 2×, 3×). A **"Voltar"** button at the bottom.

### 6.6 Screen: Mochila (merges Status + Inventory)
**960 × 440** panel, centred; title "Mochila"; **X** in the corner; **2 tabs**: **"Itens"** and **"Atributos"**.

**Items tab:**
- **Left** (x 80–540): **6 × 4** slot grid, 72-px real slots, 4-px gaps; quantity in the bottom-right corner; selection with `ui_cursor_select`.
- **Right** (x 580–900): detail panel: large 64×64 icon, name (20 px), type tag ("Consumível", "Arma", "Catalisador"), up to 3 description lines (16 px), **"Usar" / "Equipar"** button (320×48).
- **Bottom**: row of **4 shortcut slots** (the hotbar), numbered 1–4, and an **"Atribuir"** button.

**Attributes tab:**
- Header: "Nível 1", XP bar, gold tag "Pontos: 0". On the left, the **`portrait_player`** (anime portrait) about 240 px wide.
- On the right, a **2 × 4 grid of cards** ≈ 200 × 64: 32×32 icon, name (20 px), value (28 px) and a `ui_stepper` (+) very visible, **active only when there are points**. Attributes: Vigor (heart), Mente (leaf brain), Resistência (shield), Inteligência (book), Destreza (feather), Arcano (crystal), Força (fist).
- 14-px hint line at the bottom ("Vigor: +2 de vida por ponto").

### 6.7 Screens: Map and Journal
**Map** (960 × 440, "Mapa da Floresta"): **large, centred** pixel-art map (≈ 560 × 392) with a parchment frame; pulsing player marker; **icon legend** on the right (Mata, Água, Ácido, Árvores; 16 px); map zoom `+` / `−` buttons. No stray "Posição atual".

**Journal** (960 × 440, "Diário de Missões"): list of cards (max 4 visible, scrolling with a vine bar), each 880 × 80: seal icon (active = gold, locked = padlock, done = green leaf), title 20 px, 1 objective line 16 px and a thin progress bar. No "Bloqueada" tag.

### 6.8 Performance monitor (translucent overlay)
- **Position:** strip at the **top centre, x 340 to 770, y 8 to 120**, so it does not cover health, mana, hotbar or joysticks. (Alternative: bottom centre, x 400–800, y 470–505.)
- **Background:** `ui_perf_overlay` at **black 35% opacity** (applied by the game, not baked into the art), 1-px cyan-blue border at 50%. The game stays visible behind it.
- **Text:** **cyan-blue `#5CC8FF`** with a 1-px `#06233A` outline, 16 px. **Yellow `#FFE066`** (FPS < 50) and **red `#FF6A5E`** (FPS < 30 or high memory).
- **5 short lines**, no long labels:
  1. `FPS 59.6   UPS 59.6`
  2. `Update 0.51 ms   Draw 2.32 ms`
  3. `Inimigos 1   Projéteis 0   Magias 0`
  4. `Heap 5.9/256 MB   Nativa 116 MB`
  5. `Bateria 86% · 36 °C · Térmico normal`
- Optional: frame-time mini graph (60 bars of 2 px, blue, dashed 16.6 ms line). **Does not intercept touches.**

### 6.9 Command chat (developer mode)
- **Lower, translucent** panel, 720 × 280, anchored at the **bottom centre** (x 240–960, y 220–500), so it does not hide the character.
- 3-line log (14 px), 1-line input field and a **"Enviar"** button on the right (96 × 40). The **FPS** button becomes a monitor icon in the title bar that toggles the monitor from 6.8.

## 7. How to build each request (template to paste)

```
[Sections 0 to 4 of this document + the rules of sprite-generation-prompt.md v2]
[Attachments: current screenshots, approved component kit]

REQUEST
Asset: ui_slot
Frames: 4 side by side · final image 768×192 (each frame 192×192) · flat magenta #FF00FF background
Content: [row from table 6.1]
Deliver only the image, then answer the checklist in section 8.
```

## 8. Delivery checklist (answer item by item)

- [ ] Components in flat HD on **magenta `#FF00FF`**, at the HD size from the table (logical × 8), equal frames with no gap between them.
- [ ] **No text** inside components; no magenta in the drawing.
- [ ] Uniform dark 8-HD-px outline; at most 3 tones per material; no gradient / noise / glow / blur; no detail < 8 px.
- [ ] 9-slice panels and buttons with **identical** borders on all 4 sides and the margins reported.
- [ ] Ramps from section 2.3; no flat green and no grey.
- [ ] Round zoom button with magnifier and 1×/2×/3× factor (text drawn by the game).
- [ ] A single Mochila with 2 tabs; slot grid instead of a list; anime portrait on the Atributos tab.
- [ ] Same skeleton (frame, title ribbon, round X) in Mochila, Map, Journal, Pause and Options.
- [ ] Performance monitor: translucent, cyan-blue `#5CC8FF`, 5 lines, clear of health / mana / hotbar / joysticks.
- [ ] Aim joystick and swap button placed as in the section 5 table.
- [ ] Touch targets ≥ 48 real px; nothing in the forbidden area (x < 56, y > 510).
- [ ] 1200×540 mockups of **every** screen, with Brazilian Portuguese texts, accented, in the single pixel font.
- [ ] Nothing copied from an existing game.

---

### Notes for the integrator (do not send to the generator)

- UI assets are in `art/manifest.json` (`kind: ui`, scale 1) and are produced by `python tools/pixelize/pixelize.py build --prefix ui_ [--install]`. Portraits, logo and background are `kind: smooth` (magenta cut-out and resize only).
- The HUD is drawn by `ForestHud.java` (integer scale 2–4); menus, panels and chat are built in code in `MainActivity.java` (plain Views, no sprites). To apply this art, the panels must migrate to `NinePatch` / `Canvas` with the new PNGs and the chosen pixel font.
- The aim joystick and the swap button are already in `Joystick.layoutAim` and `ForestHud.drawActions`; adjust the section 5 coordinates if the art changes their size.
- The performance monitor is currently `drawDebugOverlay` in `Game.java`; change background, colour and position as in 6.8.
- Status and Inventory become one screen; the top bar goes from 6 to 5 icons.
