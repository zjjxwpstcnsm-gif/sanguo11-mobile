#!/usr/bin/env python3
"""Copy all original effect library124 images losslessly; source remains readonly.

This supplies pixels for source worker image indices, not PC raster acceptance.
Original extra-mip count is zero. No resizing, atlas or generated mip levels.
"""
import argparse
import json
from pathlib import Path
from pc_resources import Archive, sha, wftx_levels

ROOT = Path(__file__).resolve().parents[2]


def convert(installation):
    archive = Archive(installation/'Media/san11pkres.bin')
    try:
        raw = archive.read(124)
        offset, size = archive.entries[124]
    finally:
        archive.close()
    frames = wftx_levels(raw)
    if len(frames) != 33 or any(len(f['levels']) != 1 for f in frames):
        raise ValueError('Reinspect original effect library33 unmipped images')
    output = ROOT/'app/src/main/assets/3d/pc-effects'
    output.mkdir(parents=True, exist_ok=True)
    entries = []
    for index, frame in enumerate(frames):
        image = frame['levels'][0].convert('RGBA')
        path = output/f'image-{index:02d}.png'
        image.save(path, optimize=True)
        entries.append(dict(image_index=index, width=image.width, height=image.height,
            source_resource=124, source_image_metadata={k:v for k,v in frame.items() if k!='levels'},
            rgba_sha256=sha(image.tobytes()), source_path=str(path.relative_to(ROOT)),
            apk_path='assets/3d/pc-effects/'+path.name, bytes=path.stat().st_size,
            sha256=sha(path.read_bytes()), runtime_binding='PcEffectProcess packet texture_index -> PcMapEffects.loadTexture original image; normal production map, PC acceptance pending'))
    report = dict(schema=1, goal_complete=False, status='ORIGINAL_PIXELS_CONVERTED_MAP_GPU_BOUND_PC_PENDING',
        source_archive=str(installation/'Media/san11pkres.bin'), source_resource=124,
        source_sha256=sha(raw), source_offset=offset, source_bytes=size, images=entries,
        conversion='WFTX0010 source base RGBA -> lossless PNG; no resize or new mip levels',
        runtime_effects_added=0,
        limits=['Actual PC/MOD active resource resolution unverified',
            'Sampler addressing, original shader and matching PC images still pending'])
    (ROOT/'docs/pc-visual/effect-textures-source.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    manifest = ROOT/'tools/content/map-release-manifest.json'
    data = json.loads(manifest.read_text())
    for item in entries:
        found = [e for e in data['files'] if e['source_path'] == item['source_path']]
        if len(found) > 1: raise ValueError('Duplicate effect image pin')
        if found: found[0]['sha256'] = item['sha256']
        else: data['files'].append({k:item[k] for k in ('source_path','apk_path','sha256')})
    manifest.write_text(json.dumps(data, ensure_ascii=False, indent=2)+'\n')
    print(json.dumps(dict(images=len(entries), png_bytes=sum(e['bytes'] for e in entries), source_sha256=sha(raw))))


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('installation', type=Path)
    convert(p.parse_args().installation.resolve())
