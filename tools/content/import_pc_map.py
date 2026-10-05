#!/usr/bin/env python3
"""Import the user's local SAN11 LINK archive; never execute the PC binaries.

Requires Pillow/numpy. All resource IDs are zero-based. Source bytes and hashes
are recorded so a modded installation cannot be misrepresented as retail data.
"""
import argparse
import collections
import gzip
import hashlib
import io
import json
import re
from pathlib import Path
import struct

import numpy as np
from PIL import Image
from pc_ground_palette import pack_palette

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'app/src/main/assets/3d/pc-map'
MAP = ROOT / 'core/src/main/resources/maps/national-map-v056.properties'
# Existing rule vocabulary collapses dirt/wasteland into PLAIN and banks into
# blocked mountain. Exact original IDs remain in pc-map.bin.gz for inspection.
CODES = ['P','P','A','Z','X','F','Q','W','O','P','R','B','P','S','M','M','P','P','R','D']


class LinkArchive:
    def __init__(self, path):
        self.path = Path(path)
        self.file = self.path.open('rb')
        header = self.file.read(16)
        if header[:4] != b'LINK':
            raise ValueError('Not a LINK archive')
        count, version = struct.unpack_from('<II', header, 4)
        if not 4806 <= count <= 10000 or version != 1:
            raise ValueError('Unsupported LINK table')
        self.entries = [struct.unpack('<II', self.file.read(8)) for _ in range(count)]
        end = 16 + count * 8
        for offset, size in self.entries:
            if offset < end or offset + size > self.path.stat().st_size:
                raise ValueError('Invalid/overlapping LINK entry')
            end = offset + size

    def read(self, index, magic):
        offset, size = self.entries[index]
        self.file.seek(offset)
        data = self.file.read(size)
        if not data.startswith(magic) or len(data) != size:
            raise ValueError(f'Resource {index}: wrong format')
        return data


def textures(data):
    count = struct.unpack_from('<I', data, 12)[0]
    pos, result = 16, []
    if not 1 <= count <= 64:
        raise ValueError('WFTX image count')
    for _ in range(count):
        w, h, bits = struct.unpack_from('<HHI', data, pos)
        pos += 8
        if not 1 <= w <= 4096 or not 1 <= h <= 4096 or bits not in (24, 32):
            raise ValueError('Unsupported WFTX image')
        size = w * h * (bits // 8)
        result.append(Image.frombytes('RGB' if bits == 24 else 'RGBA', (w, h),
            data[pos:pos + size], 'raw', 'BGR' if bits == 24 else 'BGRA').convert('RGB'))
        pos += size
    if pos != len(data):
        raise ValueError('WFTX trailing/truncated data')
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('installation', type=Path)
    args = parser.parse_args()
    archive = LinkArchive(args.installation / 'Media/san11pkres.bin')
    evidence = {'source': str(archive.path), 'source_kind': 'user-provided modded PC installation',
        'archive_sha256': hashlib.file_digest(archive.path.open('rb'), 'sha256').hexdigest(),
        'resources': [], 'coordinate_contract': 'column-major; vx=114+4*x, vy=114+4*y+2*(x&1)',
        'height_scale': 'source byte *0.5 PC world units *0.05 uniform scene scale = byte*0.025; EXE415920/41c356',
        'palette_seasons': ['autumn', 'spring', 'summer', 'winter'],
        'limits': ['Original GPU lighting and texture UV scale are not yet captured from the PC executable.',
            'Near face IDs/corner masks and active overlay counts are restored; remaining low flag semantics, far material and special animated texture IDs are not yet reproduced.',
            'Rule vocabulary merges dirt/wasteland; banks remain impassable.',
            'Development ownership uses the existing administrative parent table.',
            'Existing mobile city/unit models remain; source scenery progress is separately recorded in docs/pc-visual/.']}

    def read(index, magic):
        data = archive.read(index, magic)
        evidence['resources'].append({'id': index, 'magic': magic.decode(), 'bytes': len(data),
            'offset': archive.entries[index][0], 'sha256': hashlib.sha256(data).hexdigest()})
        return data

    shex = read(4791, b'SHEX0008')
    if len(shex) != 8 + 200 * 200 * 11:
        raise ValueError('SHEX size')
    cells = np.frombuffer(shex[8:], np.uint8).reshape(200, 200, 11)
    if cells[:, :, 0].max() >= len(CODES) or np.count_nonzero(cells[:, :, 5]) != 591:
        raise ValueError('Unsupported geography or development count')
    mesh = read(4793, b'K3ST0006')
    if len(mesh) != 8 + 1025**2 * 8 + 1024**2 * 8:
        raise ValueError('K3ST size')
    vertices = np.frombuffer(mesh[8:8 + 1025**2 * 8], np.uint8).reshape(1025, 1025, 8)
    faces = np.frombuffer(mesh[8 + 1025**2 * 8:], np.uint8).reshape(1024, 1024, 8)
    if vertices[:, :, 7].max() > 35:
        raise ValueError('Unsupported terrain palette')
    words=faces.copy().view('<u8').reshape(1024,1024)
    overlays=(words>>2)&3
    layers=[]
    for n in range(4):
        ids=(words>>(4+10*n))&63
        masks=np.where(overlays>=n,(words>>(10+10*n))&15,0)
        if np.any((masks>0)&(ids>35)&(ids!=60)):
            raise ValueError('Unsupported near face texture ID')
        layers.append({'layer':n,'active_faces':int(np.count_nonzero(masks)),
            'animated_id60_faces':int(np.count_nonzero((masks>0)&(ids==60)))})
    if np.any(((words>>10)&15)!=15):
        raise ValueError('Unsupported near base corner mask')
    evidence['near_face_contract']='64-bit little endian: flags[0:2], active overlay count[2:4], four (palette ID:6, corner mask:4) layers[4:44], water byte[44:52], coarse water flags[52:54], far palette[54:60]/mask[60:64]; mask bits BR,BL,TR,TL. Ignore inactive layer bytes.'
    water=((words>>44)&255).astype(np.uint8)
    evidence['water_contract']={'consumer':'supplied EXE415bb0 and415e20, shift helper707de0',
        'machine_check':'tools/content/check_pc_water.py',
        'height':'nonzero byte + float32(.0025), multiplied by .5 PC units and .05 scene scale',
        'minimum_nonzero':int(water[water>0].min()),'maximum':int(water.max()),
        'previous_nibble_wrong_faces':int(np.count_nonzero(water!=(water&15))),
        'previous_nibble_omitted_faces':int(np.count_nonzero((water>0)&((water&15)==0))),
        'limits':'Full water byte restored; Android per-face clipping and legacy water shader remain provisional against native coarse4x4 topology/material.'}
    evidence['near_layers']=layers
    indices=vertices[:,:,7]
    corners=[indices[:-1,:-1],indices[1:,:-1],indices[:-1,1:],indices[1:,1:]] # TL, TR, BL, BR
    mixed=(corners[0]!=corners[1])|(corners[0]!=corners[2])|(corners[0]!=corners[3])
    authored=words
    resolved=np.repeat(((authored>>4)&63)[None,:,:],4,axis=0)
    for n in range(1,4):
        ids=(authored>>(4+10*n))&63;masks=(authored>>(10+10*n))&15
        for corner in range(4):
            resolved[corner]=np.where(((masks&(1<<corner))!=0)&(overlays>=n),ids,resolved[corner])
    matches=int(np.count_nonzero(resolved[[3,2,1,0]]==np.asarray(corners)))
    if matches!=4*1024*1024:
        raise ValueError('Authored active face layers disagree with vertex IDs')
    evidence['near_corner_crosscheck']={'mixed_faces':int(np.count_nonzero(mixed)),'matching_corners':matches,
        'total_corners':4*1024*1024,'note':'All face corners match source vertex IDs exactly after ignoring inactive layers.'}
    ASSETS.mkdir(parents=True, exist_ok=True)
    # Runtime x-fast order; preserve every authored vertex and original cell flag.
    payload = b'PCMAP002' + vertices[:, :, 0].T.tobytes() + water.T.tobytes() + shex[8:]
    output = ROOT / 'core/src/main/resources/maps/pc-map.bin.gz'
    output.write_bytes(gzip.compress(payload, mtime=0))
    # RGB avoids platform grayscale/color conversion of numeric palette IDs.
    indices=vertices[:, :, 7].T
    Image.fromarray(np.repeat(indices[:, :, None],3,axis=2)).save(ASSETS / 'texture-indices.png')
    # Four 10-bit (6-bit palette ID, 4-bit corner mask) layers follow the
    # low four flags. Preserve the first 48 bits in two RGB images: alpha data
    # would be premultiplied by Android bitmap decoding / texture upload.
    Image.fromarray(faces[:, :, :3].transpose(1,0,2)).save(ASSETS / 'face-low.png')
    Image.fromarray(faces[:, :, 3:6].transpose(1,0,2)).save(ASSETS / 'face-high.png')
    palette_sizes=[]
    for season, index in enumerate(range(4787, 4791)):
        color = read(index, b'GCOL0001')
        if len(color) != 8 + 1025**2 * 3:
            raise ValueError('GCOL size')
        # GCOL stores RGB; columns first, not BGR WFTX pixels.
        rgb = np.frombuffer(color[8:], np.uint8).reshape(1025, 1025, 3).transpose(1, 0, 2)
        Image.fromarray(rgb).save(ASSETS / f'color-{season}.png')
        images = textures(read(4800 + season, b'WFTX0010'))
        if len(images) != 36:
            raise ValueError('Expected 36 terrain textures')
        atlas,dimensions=pack_palette(images)
        palette_sizes.append(dimensions)
        atlas.save(ASSETS / f'palette-{season}.png')
    Image.fromarray(np.asarray(palette_sizes,dtype=np.uint8)).save(ASSETS/'palette-sizes.png')
    evidence['palette_contract']='Original64/128/256 RGB texels retained;260cell/two wrap gutters; numeric36x4 width/32,height/32 size lookup; source fine-vertex UV*32/dimension'

    old = (ROOT / 'data/map/reference-pc/national-map-v063.properties').read_text().splitlines()
    props = dict(line.split('=', 1) for line in old if '=' in line and not line.startswith('#'))
    sites = {int(k[5:]): tuple(map(int, v.split(','))) for k, v in props.items() if k.startswith('site.')}
    ports, moved = {tuple(map(int, xy)) for xy in np.argwhere(cells[:, :, 0] == 17)}, []
    for sid in range(20052, 20087):
        previous = sites[sid]
        if previous in ports:
            ports.remove(previous)
    for sid in range(20052, 20087):
        previous = sites[sid]
        if cells[previous[0], previous[1], 0] == 17:
            continue
        candidates = [p for p in ports if max(abs(p[0] - previous[0]), abs(p[1] - previous[1])) <= 2]
        if len(candidates) != 1:
            raise ValueError(f'Ambiguous PC port {sid}: {candidates}')
        sites[sid] = candidates[0]
        ports.remove(candidates[0])
        moved.append({'id': sid, 'before': previous, 'after': sites[sid]})
    if ports:
        raise ValueError('Unmapped PC ports')
    for sid in range(20000, 20052):
        x, y = sites[sid]
        if cells[x, y, 0] != (16 if sid < 20042 else 18):
            raise ValueError(f'PC site mismatch {sid}')
    terrain = np.asarray(CODES)[cells[:, :, 0]]
    # Keep the established administrative site-parent table. SHEX region IDs
    # include ports; development ownership belongs to their parent cities.
    affiliation = (ROOT / 'core/src/main/java/game/sanguo/core/SiteAffiliation.java').read_text()
    raw_parents = list(map(int, re.search(r'NATIONAL_PARENTS = \{(.*?)\}', affiliation, re.S).group(1).replace('\n', '').split(',')))
    parents = {20042 + i: 20000 + value for i, value in enumerate(raw_parents)}
    plots = collections.defaultdict(list)
    for x, y in np.argwhere(cells[:, :, 5] == 1):
        region = int(cells[x, y, 1]) + 20000
        owner = region if region < 20042 else parents[region]
        plots[owner].append((int(x), int(y)))
    evidence['terrain_counts'] = dict(collections.Counter(map(int, cells[:, :, 0].ravel())))
    evidence['moved_ports'] = moved
    evidence['changed_cells'] = sum(props[f'terrain.{y}'][x] != terrain[x, y] for x in range(200) for y in range(200))
    updated = ['# PC geography imported from user-provided LINK/SHEX0008; provenance: docs/pc-map/source.json.']
    for line in old:
        if line.startswith('#') or line.startswith('blocked.'):
            continue
        if line.startswith('revision='):
            line = 'revision=65'
        elif line.startswith('terrain.'):
            y = int(line.split('=', 1)[0][8:])
            line = f'terrain.{y}=' + ''.join(terrain[:, y])
        elif line.startswith('site.'):
            sid = int(line.split('=', 1)[0][5:])
            line = f'site.{sid}={sites[sid][0]},{sites[sid][1]}'
        elif line.startswith('plots.'):
            sid = int(line.split('=', 1)[0][6:])
            line = f'plots.{sid}=' + ';'.join(f'{x},{y}' for x, y in plots[sid])
        updated.append(line)
    MAP.write_text('\n'.join(updated).rstrip() + '\n')
    evidence['runtime_sha256'] = hashlib.sha256(output.read_bytes()).hexdigest()
    evidence['map_sha256'] = hashlib.sha256(MAP.read_bytes()).hexdigest()
    evidence['runtime_bytes'] = output.stat().st_size
    evidence['opening_revision'] = 65
    evidence['opening_dams'] = 'docs/pc-visual/dams-source.json'
    evidence['opening_note'] = 'Four source OBJS dams are materialized only for new openings; source wetland cells remain unchanged; revision64 saves retain their own entities.'
    target = ROOT / 'docs/pc-map/source.json'
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(evidence, ensure_ascii=False, indent=2) + '\n')
    # Keep the two existing release integrity gates in sync with imported bytes.
    java=ROOT / 'core/src/main/java/game/sanguo/core/NationalMap.java'
    java.write_text(re.sub(r'SHA256="[a-f0-9]{64}"', 'SHA256="'+evidence['map_sha256']+'"', java.read_text()))
    manifest=ROOT / 'tools/content/map-release-manifest.json'
    registered=json.loads(manifest.read_text())
    for entry in registered['files']:
        if '/3d/pc-map/' in entry['source_path'] or entry['source_path'] in (
                str(MAP.relative_to(ROOT)), str(output.relative_to(ROOT))):
            entry['sha256']=hashlib.sha256((ROOT / entry['source_path']).read_bytes()).hexdigest()
    manifest.write_text(json.dumps(registered, ensure_ascii=False, indent=2)+'\n')
    print(json.dumps({k: evidence[k] for k in ('changed_cells', 'moved_ports', 'runtime_sha256', 'map_sha256')}, ensure_ascii=False))


if __name__ == '__main__':
    main()
