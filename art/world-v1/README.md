# Amazon world v1 — integrated environment

The normal Android game now uses this environment: a colossal central Brazil nut tree, northwest malocas, northeast stepped lost city, southwest burned forest and southeast theater city. Rivers, buildings and routes are authored; forest detail varies deterministically with the seed.

## Review

- [Runtime overview](world_runtime_overview.png): exported from the actual Java geography and reduced artwork; also used by the in-game exploration panel.
- [Android renderer captures](../../docs/validation/world-renderer/): production Canvas rendering of landmarks, rivers, bridges and four empty boss arenas. These images omit the HUD.
- [Validation report](../../docs/validation/2026-10-27-world.md): automated results, emulator measurements and limits.
- [Detail comparison](world_camera_comparison.png): HD scene, new 416 × 234 reduction and previous 208 × 117 profile at matching framing. This is an art reference, not an Android screenshot.
- [HD composition](../hd/world_overview_concept_v1.png) and [HD camera reference](../hd/world_camera_concept_v1.png): generated direction references, not playable scenes. The generated wizard is only a scale cue; runtime uses the existing character atlas.
- [Topology diagnostic](world_topology.png): geometry and collision visualization; diagnostic colors are not the environment artwork.

## Sources and runtime assets

Twenty original generated HD PNGs are preserved in `art/hd/world_*_v1.png`. Exact English prompts are in `docs/prompts/world-*-v1.md`. Image generation produces drawn artwork; deterministic reduction, palette quantization and alpha normalization happen afterward. The environment has its own 160-color palette; character and UI palettes remain unchanged.

Runtime uses sixteen modular images: broadleaf tree, maloca, stepped temple, theater, city house, ruin, burned tree, canopy and eight terrain materials. The colossal trunk and root are split into nine losslessly reconstructable parts, each at most 512 × 512 px. Only visible parts are requested from the bitmap cache. The offline overview is 768 × 768 px. Whole HD compositions and whole giant-tree sprites are not used as the world background.

`runtime-assets.json` records source/output hashes, dimensions, palette hash and matching `WorldDefinition.java` hash. `landmark-parts.json` records part coordinates and hashes. `verification.json` verifies reference dimensions, alpha, palette membership and deterministic output. Source resolutions vary; declared final cell sizes control reduction rather than trusting dimensions requested from the generator.

## Rebuild

Requires Python with Pillow and NumPy, plus JDK 17. The workspace environment is `build/world-art-python`.

```powershell
python tools/world/build_art.py
python -m unittest tools/pixelize/test_pixelize.py -v
```

The build reduces all twenty HD sources, checks them, installs modular runtime assets, splits and verifies the trunk/root, generates previews and exports the real overview/topology from Java. It does not call image generation.

After intentional palette changes, use `python tools/world/build_art.py --refresh-palette`. After geometry-only edits, use `python tools/world/build_art.py --overview-only`, then rebuild the APK. The legacy `tools/generate_world_tiles.py` does not produce or replace these `world_*` assets; this manifest-based build is their source of truth.

Four future boss arenas are reserved. Boss characters, interiors, boats and new NPCs are outside this environment delivery. Physical-device performance and further vegetation variants remain future work.
