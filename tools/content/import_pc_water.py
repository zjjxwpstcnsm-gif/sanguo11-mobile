#!/usr/bin/env python3
"""Reproduce original coarse water initialization and copy original WFTX pixels.

Requires Pillow and Unicorn. The installation is read only. Native visual RNG
has its own isolated default startup; Android gameplay never supplies a seed.
"""
import argparse
import gzip
import json
from pathlib import Path

from check_pc_water_mesh import check
from pc_resources import Archive, sha, wftx_levels

ROOT = Path(__file__).resolve().parents[2]


def convert(installation):
    reference = ROOT/'out/pc-visual/water-import-native-reference.bin'
    proof = ROOT/'out/pc-visual/water-import-native-check.json'
    check(installation, proof, reference)
    records = reference.read_bytes()
    runtime = ROOT/'core/src/main/resources/maps/pc-water.bin.gz'
    runtime.write_bytes(gzip.compress(records, compresslevel=9, mtime=0))
    archive = Archive(installation/'Media/san11pkres.bin')
    try:
        texture = archive.read(4844)
        offset, length = archive.entries[4844]
    finally:
        archive.close()
    frames = wftx_levels(texture)
    if len(frames) != 2 or any(len(f['levels']) != 1 or f['levels'][0].size != (256, 256) for f in frames):
        raise ValueError('Original water has two unmipped 256x256 sheets')
    outputs = [dict(path=str(runtime.relative_to(ROOT)), sha256=sha(runtime.read_bytes()),
                    decoded_sha256=sha(records), decoded_bytes=len(records))]
    for index, frame in enumerate(frames):
        path = ROOT/f'app/src/main/assets/3d/pc-map/water-{index}.png'
        image = frame['levels'][0].convert('RGBA')
        image.save(path, optimize=True)
        outputs.append(dict(path=str(path.relative_to(ROOT)), sha256=sha(path.read_bytes()),
                            rgba_sha256=sha(image.tobytes()), bytes=path.stat().st_size))
    alias=installation/'Media/stage/water.wft'
    overrides=[] if not alias.is_file() else [dict(path=str(alias),bytes=alias.stat().st_size,sha256=sha(alias.read_bytes()))]
    report = dict(schema=1, source_kind='user supplied MOD installation', goal_complete=False,
        source=str(installation), native_check=json.loads(proof.read_text()),
        resource=dict(id=4844, format='WFTX0010', sha256=sha(texture), offset=offset, bytes=length,
                      loose_alias='media/stage/water.wft',potential_overrides=overrides,
                      selection='Supplied archive; actual PC/MOD active branch remains unverified'),
        conversion='Native415e20/415d80 isolated default initialization; exact source RGBA, PNG lossless',
        outputs=outputs, runtime_binding='PcMap coarse visual field -> SceneMesh.sourceWater -> FilamentMapView',
        limits=['PC registry/MOD runtime overrides and matching PC video remain unverified',
                'Shader blending/depth and visibility clock require installed validation'])
    path = ROOT/'docs/pc-visual/water-source.json'
    path.write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    manifest = ROOT/'tools/content/map-release-manifest.json'
    data = json.loads(manifest.read_text())
    for item in outputs:
        source = item['path']
        apk = 'maps/pc-water.bin.gz' if source.startswith('core/') else source.replace('app/src/main/', '')
        entry = next((e for e in data['files'] if e['source_path'] == source), None)
        if entry is None:
            entry = dict(source_path=source, apk_path=apk)
            data['files'].append(entry)
        entry['sha256'] = item['sha256']
    manifest.write_text(json.dumps(data, ensure_ascii=False, indent=2)+'\n')
    print(json.dumps(dict(outputs=outputs), ensure_ascii=False))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('installation', type=Path)
    convert(parser.parse_args().installation)
