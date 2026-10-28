"""Rebuild the Amazon world from preserved HD sources and immutable geography.

    python tools/world/build_art.py [--refresh-palette]

Image generation is deliberately not part of this deterministic build.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
from types import SimpleNamespace

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools/pixelize'))
import pixelize as px
import preview_world
import split_landmarks

PALETTE_SOURCES = [
    'world_overview_concept_v1', 'world_camera_concept_v1', 'world_tree_broadleaf_v1',
    'world_maloca_v1', 'world_temple_v1', 'world_theater_v1', 'world_city_house_v1',
    'world_tile_grass_v1', 'world_tile_negro_v1', 'world_tile_solimoes_v1', 'world_tile_burned_v1',
]
PARTED = {'world_castanheira_trunk_v1', 'world_root_v1'}

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def export_overview():
    configured_java = next((line.split('=', 1)[1].strip() for line in
        (ROOT / 'gradle.properties').read_text().splitlines()
        if line.startswith('org.gradle.java.home=')), None)
    java_home = os.environ.get('JAVA_HOME', configured_java)
    javac = str(Path(java_home) / 'bin/javac.exe') if java_home else shutil.which('javac')
    java = str(Path(java_home) / 'bin/java.exe') if java_home else shutil.which('java')
    if not javac or not java:
        raise SystemExit('JDK 17 is required to rebuild the overview')
    classes = ROOT / 'build/world-preview'
    classes.mkdir(parents=True, exist_ok=True)
    subprocess.run([javac, '-encoding', 'UTF-8', '-d', str(classes),
        'app/src/main/java/com/example/amazonMages/map/WorldDefinition.java',
        'tools/world/WorldOverviewExporter.java', 'tools/world/WorldTopologyPreview.java'], cwd=ROOT, check=True)
    for tool in ['WorldOverviewExporter', 'WorldTopologyPreview']:
        subprocess.run([java, '-cp', str(classes), tool], cwd=ROOT, check=True)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--refresh-palette', action='store_true')
    parser.add_argument('--overview-only', action='store_true')
    args = parser.parse_args()
    if args.overview_only:
        export_overview()
        metadata = px.ART / 'world-v1/runtime-assets.json'
        report = json.loads(metadata.read_text(encoding='utf-8'))
        report['overview_sha256'] = sha(px.RES / 'world_overview_v1.png')
        report['world_definition_sha256'] = sha(ROOT / 'app/src/main/java/com/example/amazonMages/map/WorldDefinition.java')
        metadata.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
        return
    if args.refresh_palette:
        px.cmd_palette_extract(SimpleNamespace(
            images=[str(px.ART / 'hd' / (name + '.png')) for name in PALETTE_SOURCES],
            colors=160, name='amazon_world_v1'))
    manifest, assets = px.load_manifest()
    names = [name for name in assets if name.startswith('world_')]
    build_args = SimpleNamespace(names=names, prefix=None, palette=None, install=False)
    if px.cmd_build(build_args):
        raise SystemExit('World art build failed')
    report = {'palette_sha256': sha(px.ART / 'palettes/amazon_world_v1.json'), 'assets': {}}
    for name in names:
        spec = assets[name]
        output = px.ART / 'build' / (name + '.png')
        problems = px.verify(name, spec, output, spec.get('palette', manifest['palette']))
        if problems:
            raise ValueError(f'{name}: {problems}')
        runtime = name not in PARTED and 'concept' not in name
        if runtime:
            shutil.copyfile(output, px.RES / output.name)
        report['assets'][name] = {
            'hd_sha256': sha(px.ART / 'hd' / (name + '.png')),
            'output_sha256': sha(output), 'cell': spec['cell'],
            'runtime': runtime, 'split_into_parts': name in PARTED,
        }
    split_landmarks.main()
    preview_world.main()
    # The overview is generated offline, avoiding an expensive scan on the device.
    export_overview()
    report['overview_sha256'] = sha(px.RES / 'world_overview_v1.png')
    report['world_definition_sha256'] = sha(ROOT / 'app/src/main/java/com/example/amazonMages/map/WorldDefinition.java')
    (px.ART / 'world-v1/runtime-assets.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(f'[verified] {len(names)} HD sources; modular assets, landmark parts and overview installed')

if __name__ == '__main__':
    main()
