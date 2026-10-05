#!/usr/bin/env python3
"""Convert source OBJS forest/wetland meshes; preserve source LODs and alpha.

Requires numpy/Pillow. Model bindings are derived from this installation's EXE,
not guessed from resource order. See docs/pc-visual/FORMAT_NOTES.md.
"""
import argparse
import gzip
import json
from pathlib import Path
import struct
import numpy as np
from PIL import Image
from pc_scene_coordinates import WORLD_AXES, contract
from pc_resources import Archive, objects, wkmd_geometry, wftx, sha

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'app/src/main/assets/3d/pc-scenery'


def convert(installation,climate_reference):
    from pc_region_climate import RegionClimate
    exe = (installation / 'san11pk.exe').read_bytes()
    archive = Archive(installation / 'Media/san11pkres.bin')
    # The inspected executable reads this table at VA 0x763740 in 0x4118c9.
    # Refuse another executable rather than silently assigning incorrect models.
    table = struct.unpack_from('<320I', exe, 0x363740)
    if table[288:320] != tuple(range(4808, 4840)):
        raise ValueError('Source executable model table changed; inspect binding again')
    source = archive.read(4805)
    rows = [r for r in objects(source) if r['model'] in (46, 47, 48)]
    if any(r['tail'] for r in rows):
        raise ValueError('Source object state bytes changed; inspect resolver before conversion')
    climate=RegionClimate(exe,archive.read(4791),climate_reference)
    climate_rows=[]
    payload = bytearray(b'PCSCN003' + struct.pack('<II', len(rows), 26))
    for r in rows:
        resolved=climate.resolve(r);climate_rows.append(resolved)
        payload += struct.pack('<HHHHfH', r['model'], r['x'], r['z'], r['height'], r['yaw'],resolved['climate'])
    evidence = dict(schema=1, source_archive='Media/san11pkres.bin',
        executable_sha256=sha(exe), binding_table_offset=0x363740,
        binding_table_sha256=sha(exe[0x363740:0x363740+320*4]),
        objects_resource=4805, objects_sha256=sha(source), placements=len(rows), models=[],
        **climate.evidence(),
        climate_resolution=climate_rows)
    for resource in range(4808, 4834):
        data = archive.read(resource)
        if struct.unpack_from('<8I', data, 48)[4:] != (1, 1, 0, 1):
            raise ValueError('Scenery mesh has unsupported draw groups')
        mesh, = wkmd_geometry(data)
        draw = struct.unpack_from('<I', data, 160)[0]
        flags, texture = struct.unpack_from('<IH', data, draw)
        primitive, first, nv, first_index, primitives = struct.unpack_from('<5I', data, draw+24)
        if texture != 65535 or primitive != 5 or first != 0 or nv != len(mesh['vertices']) or first_index != 0 or primitives != len(mesh['indices'])-2:
            raise ValueError('Scenery draw contract changed')
        strip = mesh['indices']
        triangles = []
        for i in range(len(strip)-2):
            tri = strip[i:i+3].tolist()
            if len(set(tri)) != 3:
                continue
            if i & 1:
                tri[0], tri[1] = tri[1], tri[0]
            triangles.extend(tri)
        v = mesh['vertices'].astype('<f4')
        # Uniform .05 XYZ, based on EXE415920/41c356 arithmetic.
        v[:, :3] *= WORLD_AXES
        normals = v[:, 3:6] / WORLD_AXES
        norms = np.linalg.norm(normals, axis=1)
        if np.any(norms < .9):
            raise ValueError('Source normal missing')
        v[:, 3:6] = normals / norms[:, None]
        colors = mesh['colors'][:, [2, 1, 0, 3]].astype('<f4') / 255
        attributes = np.concatenate([v, colors], axis=1).astype('<f4')
        indices = np.asarray(triangles, dtype='<u4')
        payload += struct.pack('<III', resource, len(v), len(indices))
        payload += attributes.tobytes() + indices.tobytes()
        evidence['models'].append(dict(id=resource, sha256=sha(data), vertices=len(v), triangles=len(indices)//3,
            draw_flags=hex(flags), source_lod='near' if resource%2==0 else 'far',
            conversion='source float position/normal/UV, BGRA color, D3D triangle strip to triangles'))
    OUT.mkdir(parents=True, exist_ok=True)
    target = OUT / 'scenery.pcz'
    target.write_bytes(gzip.compress(payload, mtime=0))
    images = [wftx(archive.read(n))[0] for n in range(4840, 4844)]
    w, h = images[0].size
    if any(i.size != (w,h) for i in images):
        raise ValueError('Season atlas size mismatch')
    # Independent full source sheets retain alpha, with no repaint or AI generation.
    atlas = Image.new('RGBA', (w*4,h))
    for season, im in enumerate(images):
        atlas.paste(im, (season*w,0))
    atlas.save(OUT / 'atlas.png')
    archive.close()
    evidence['textures'] = [dict(id=n, sha256=sha(ArchiveRead(installation,n))) for n in range(4840,4844)]
    evidence['outputs'] = [dict(path=p.relative_to(ROOT).as_posix(), bytes=p.stat().st_size, sha256=sha(p.read_bytes())) for p in (target, OUT/'atlas.png')]
    evidence['uv_policy'] = 'Source UV preserved in pcz; runtime clamps to source sheet texel centres before seasonal packing. Original PC sampler state remains unverified.'
    evidence['model_quarter_order']='spring0/summer1/autumn2/winter3; EXE VA0x41bae0; separate from atlas4840 autumn/4841 spring/4842 summer/4843 winter'
    evidence['runtime_binding'] = 'PcScenery.buildWindow / FilamentMapView pcSceneryMaterial'
    evidence['validation'] = 'source binary crosschecks; installed Android status is separately recorded in docs/pc-visual/validation.json; PC comparison pending'
    evidence['limits'] = ['Climate snapshot from actual207 autumn source opening; source TOD initialization and other MOD launch state not independently decoded', 'Uniform source XYZ .05 and height-byte .025; source machine arithmetic and camera verified, PC raster comparison still has material/outline/fog/LOD differences', 'Only OBJS kinds46/47/48 in this asset; other source scopes have separate binders', 'Damaged/burning forest state transitions not yet bound', 'Source per-object LOD and draw ordering remain unverified']
    evidence['coordinate_contract']=contract('PCSCN003')
    (ROOT/'docs/pc-visual/scenery-source.json').write_text(json.dumps(evidence, ensure_ascii=False, indent=2)+'\n')
    inventory_path=ROOT/'docs/pc-visual/inventory.json'
    if inventory_path.exists():
        inventory=json.loads(inventory_path.read_text())
        for package in inventory['archives']:
            if package['path'].lower()!='media/san11pkres.bin':
                continue
            for entry in package['entries']:
                if entry['id']==4805 or 4808<=entry['id']<=4833 or 4840<=entry['id']<=4843:
                    entry['conversion']='tools/content/import_pc_scenery.py'
                    entry['android_output']='app/src/main/assets/3d/pc-scenery/atlas.png' if entry['id']>=4840 else 'app/src/main/assets/3d/pc-scenery/scenery.pcz'
                    entry['runtime_binding']='PcScenery / FilamentMapView'
                    entry['validation']='source-crosschecked; installed verification in docs/pc-visual/validation.json'
                    if entry['id']==4805:
                        entry['conversion_scope']='943 enabled kinds46/47/48 of 1195 enabled source objects'
        inventory_path.write_text(json.dumps(inventory,ensure_ascii=False,indent=2)+'\n')
    manifest_path=ROOT/'tools/content/map-release-manifest.json'
    manifest=json.loads(manifest_path.read_text())
    for output in evidence['outputs']:
        path=output['path'];entry=dict(apk_path='assets/'+str(Path(path).relative_to('app/src/main/assets')),source_path=path,sha256=output['sha256'])
        existing=next((r for r in manifest['files'] if r['source_path']==path),None)
        if existing is None:manifest['files'].append(entry)
        else:existing.update(entry)
    manifest_path.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(dict(placements=len(rows), models=26, compressed_bytes=target.stat().st_size, atlas_size=atlas.size)))


def ArchiveRead(installation, n):
    archive = Archive(installation / 'Media/san11pkres.bin')
    try:
        return archive.read(n)
    finally:
        archive.close()


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('installation', type=Path)
    parser.add_argument('--climate-reference',type=Path,default=ROOT/'docs/pc-visual/region-climate-live-source.json')
    args=parser.parse_args();convert(args.installation,args.climate_reference)
