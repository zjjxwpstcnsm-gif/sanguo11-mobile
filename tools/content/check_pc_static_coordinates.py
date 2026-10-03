#!/usr/bin/env python3
"""Cross-check serialized static meshes against supplied WKMD source geometry.

Checks all converted scenery, city and facility vertices/normal directions/UV/
RGBA/triangles. Uses raw archive resources and executable binding tables, not
the converter's generated source report. PC rendered lighting is separate.
"""
import argparse
import gzip
import json
from pathlib import Path
import struct
import numpy as np
from pc_resources import Archive, wkmd_geometry, sha
from inspect_pc_effect_bindings import EXE_SHA

ROOT = Path(__file__).resolve().parents[2]


def check(installation, output):
    exe = (installation/'san11pk.exe').read_bytes()
    if sha(exe) != EXE_SHA:
        raise ValueError('Reinspect original coordinate build')
    resource_table = struct.unpack_from('<388I', exe, 0x363740)
    archive = Archive(installation/'Media/san11pkres.bin')
    result = []
    try:
        for kind in ('scenery', 'sites', 'facilities'):
            path = ROOT/f'app/src/main/assets/3d/pc-{kind}/{kind}.pcz'
            data = gzip.decompress(path.read_bytes())
            if data[:8] != {'scenery':b'PCSCN003','sites':b'PCSIT004','facilities':b'PCFAC002'}[kind]:
                raise ValueError('Refuse previous flattened coordinate assets')
            a, n, t = struct.unpack_from('<III', data, 8)
            if kind == 'scenery':
                # Two header counts only; first placement begins at16.
                n = struct.unpack_from('<I', data, 12)[0]
                offset = 16+a*14
            elif kind == 'sites':
                offset = 20+8*4*4+t*4+a*18
            else:
                offset = 20+a*20+t*12
            vertices = triangles = 0
            source_rows = []
            for _ in range(n):
                if kind == 'scenery':
                    index, nv, ni = struct.unpack_from('<III', data, offset)
                    offset += 12
                    resource = index
                else:
                    index, texture, nv, ni = struct.unpack_from('<IIII', data, offset)
                    offset += 16
                    resource = resource_table[index]
                actual = np.frombuffer(data, dtype='<f4', count=nv*12, offset=offset).reshape(nv, 12)
                offset += nv*48
                actual_indices = np.frombuffer(data, dtype='<u4', count=ni, offset=offset)
                offset += ni*4
                raw = archive.read(resource)
                source, = wkmd_geometry(raw)
                positions = (source['vertices'][:, :3].astype('float64')*.05).astype('<f4')
                normals = source['vertices'][:, 3:6].astype('float64')/.05
                normals = (normals/np.linalg.norm(normals, axis=1)[:, None]).astype('<f4')
                colors = source['colors'][:, [2, 1, 0, 3]].astype('<f4')/255
                expected = np.concatenate((positions, normals, source['vertices'][:, 6:8], colors), axis=1).astype('<f4')
                if actual.tobytes() != expected.tobytes():
                    field = np.argwhere(actual != expected)[0].tolist()
                    raise AssertionError((kind, resource, 'original attribute mismatch', field))
                strip = source['indices'].tolist()
                expected_indices = []
                for i in range(len(strip)-2):
                    tri = strip[i:i+3]
                    if len(set(tri)) != 3:
                        continue
                    if i & 1:
                        tri[0], tri[1] = tri[1], tri[0]
                    expected_indices.extend(tri)
                if actual_indices.tobytes() != np.asarray(expected_indices, dtype='<u4').tobytes():
                    raise AssertionError((kind, resource, 'original triangles/winding mismatch'))
                vertices += nv
                triangles += ni//3
                source_rows.append(dict(resource_id=resource, sha256=sha(raw), vertices=nv, triangles=ni//3))
            if offset != len(data):
                raise AssertionError((kind, 'trailing data'))
            result.append(dict(kind=kind, models=n, vertices=vertices, triangles=triangles,
                               android_output=str(path.relative_to(ROOT)), output_sha256=sha(path.read_bytes()),
                               source_rows=source_rows))
    finally:
        archive.close()
    record = dict(schema=1, status='PASS', executable_sha256=EXE_SHA, uniform_source_scale=.05,
                  limits=['Raw geometry/normal directions/UV/RGBA/triangles only; not PC raster/timing acceptance',
                          'Normal magnitudes, source wall shear lighting, alpha sampling/order and contact still need rendered comparison'],
                  catalogs=result)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(record, indent=2)+'\n')
    print('PASS original static uniform coordinates: '+', '.join(f"{r['kind']} {r['models']} models/{r['vertices']} vertices" for r in result))


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('installation', type=Path)
    p.add_argument('--output', type=Path, default=ROOT/'out/pc-visual/v145-static-coordinate-source.json')
    a = p.parse_args()
    check(a.installation, a.output)
