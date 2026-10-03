#!/usr/bin/env python3
"""Read-only inventory and strict decoders for the supplied modded PC installation.

IDs are zero based. Never launch, extract into, or rewrite the installation.
Unknown formats and meanings stay unknown; auxiliary patches are not assumed active.
"""
import argparse
import collections
import hashlib
import json
import math
from pathlib import Path
import struct

ROOT = Path(__file__).resolve().parents[2]


def sha(data):
    return hashlib.sha256(data).hexdigest()


def file_sha(file):
    digest = hashlib.sha256()
    for block in iter(lambda: file.read(1024 * 1024), b''):
        digest.update(block)
    return digest.hexdigest()


class Archive:
    def __init__(self, path):
        self.path = Path(path)
        self.file = self.path.open('rb')
        header = self.file.read(16)
        if len(header) != 16 or header[:4] != b'LINK':
            raise ValueError('LINK header')
        count, version = struct.unpack_from('<II', header, 4)
        if version != 1 or not 0 < count < 100000:
            raise ValueError('LINK version/count')
        table = self.file.read(count * 8)
        if len(table) != count * 8:
            raise ValueError('LINK truncated table')
        self.entries = list(struct.iter_unpack('<II', table))
        end = 16 + count * 8
        size = self.path.stat().st_size
        for offset, length in self.entries:
            if offset < end or offset + length > size:
                raise ValueError('LINK overlapping/out-of-bounds entry')
            end = offset + length

    def read(self, index):
        offset, length = self.entries[index]
        self.file.seek(offset)
        data = self.file.read(length)
        if len(data) != length:
            raise ValueError('LINK resource truncated')
        return data

    def close(self):
        self.file.close()


def objects(data):
    if data[:8] != b'OBJS0004' or len(data) != 8 + 65535 * 14:
        raise ValueError('OBJS0004 layout')
    result = []
    for index, row in enumerate(struct.iter_unpack('<BHHHBfH', data[8:])):
        enabled, model, x, z, height, yaw, tail = row
        if enabled not in (0, 1) or not math.isfinite(yaw):
            raise ValueError('OBJS flags/rotation')
        if enabled:
            result.append(dict(slot=index, model=model, x=x, z=z, height=height,
                               yaw=yaw, tail=tail))
    return result


def effects(data):
    if data[:8] != b'SEFF0001' or len(data) < 12:
        raise ValueError('SEFF0001 header')
    count = struct.unpack_from('<I', data, 8)[0]
    if len(data) != 12 + count * 18:
        raise ValueError('SEFF0001 length')
    result = []
    for i, row in enumerate(struct.iter_unpack('<H4f', data[12:])):
        kind, x, y, z, yaw = row
        if not all(math.isfinite(v) for v in row[1:]):
            raise ValueError('SEFF nonfinite transform')
        result.append(dict(slot=i, effect=kind, x=x, y=y, z=z, yaw=yaw))
    return result


def wftx(data):
    """Return original base images, retaining alpha and consuming all source mips."""
    return [frame['levels'][0] for frame in wftx_levels(data)]


def wftx_levels(data):
    """WFTX0010 header8 = width/height/u8 depth/palette/extra-mips/reserved.

    EXE452700 reads those eight bytes; 450fe0 totals four-byte-aligned rows
    for the base image and every extra mip. A single 256-entry BGRA palette
    follows the pixel levels for the examined 8-bit/palette-count1 variant.
    Every source mip is decoded, never regenerated or mistaken for an image.
    """
    from PIL import Image
    if data[:8] != b'WFTX0010' or len(data) < 16:
        raise ValueError('WFTX0010 header')
    declared, count = struct.unpack_from('<II', data, 8)
    if declared != len(data) or not 0 < count <= 4096:
        raise ValueError('WFTX length/count')
    offset, frames = 16, []
    for i in range(count):
        if offset + 8 > len(data):
            raise ValueError('WFTX image header')
        header = offset
        w, h, bits, palette, extra_mips, reserved = struct.unpack_from('<HH4B', data, offset)
        offset += 8
        if not 0 < w <= 4096 or not 0 < h <= 4096 or (bits, palette) not in ((24, 0), (32, 0), (8, 1)) or reserved or extra_mips > 12:
            raise ValueError('WFTX image format')
        levels, metadata = [], []
        for level in range(extra_mips + 1):
            width, height = w >> level, h >> level
            if not width or not height:
                raise ValueError('WFTX source mip dimension becomes zero')
            pitch = (width * (bits // 8) + 3) & ~3
            size = pitch * height
            if offset + size > len(data):
                raise ValueError('WFTX pixels truncated')
            raw = data[offset:offset + size]
            mode, layout = ('P', 'P') if bits == 8 else ('RGB', 'BGR') if bits == 24 else ('RGBA', 'BGRA')
            levels.append(Image.frombytes(mode, (width, height), raw, 'raw', layout, pitch))
            metadata.append(dict(level=level, offset=offset, bytes=size, pitch=pitch, sha256=sha(raw)))
            offset += size
        palette_metadata = None
        if palette:
            if offset + 1024 > len(data):
                raise ValueError('WFTX palette truncated')
            raw = data[offset:offset+1024]
            rgba = bytearray(1024)
            for color in range(256):
                b, g, r, a = raw[color*4:color*4+4]
                rgba[color*4:color*4+4] = bytes((r, g, b, a))
            for image in levels:
                image.putpalette(bytes(rgba), rawmode='RGBA')
            levels = [image.convert('RGBA') for image in levels]
            palette_metadata = dict(offset=offset, bytes=1024, sha256=sha(raw))
            offset += 1024
        frames.append(dict(image=i, header_offset=header, width=w, height=h, bits=bits,
                           palette_raw=palette, extra_mips=extra_mips, reserved_raw=reserved,
                           levels=levels, source_levels=metadata, source_palette=palette_metadata))
    if offset != len(data):
        raise ValueError('WFTX trailing bytes')
    return frames


def wkmd_geometry(data):
    """Decode unskinned position/normal/UV/color streams for geometry investigation.

    Does not claim material, skin, hierarchy or draw-state reconstruction. Exported
    previews cannot serve as evidence of final in-game fidelity.
    """
    import numpy as np
    if data[:8] != b'WKMD0010' or len(data) < 160:
        raise ValueError('WKMD0010 header')
    if struct.unpack_from('<I', data, 8)[0] != len(data):
        raise ValueError('WKMD length')
    count = struct.unpack_from('<I', data, 68)[0]
    if not 0 < count <= 128 or 80 + count * 80 > len(data):
        raise ValueError('WKMD stream count')
    result = []
    for i in range(count):
        code, stride, nv, vp, cp, _, _, _, indexcode, ni, ip = struct.unpack_from('<11I', data, 80 + i * 80)
        if code != 0x112 or stride != 32 or indexcode != 0x65:
            raise ValueError(f'Unsupported WKMD vertex layout {code:x}/{stride}/{indexcode:x}')
        if not 0 < nv <= 65535 or not 3 <= ni <= 1000000:
            raise ValueError('WKMD vertex/index count')
        if vp + nv * stride > len(data) or cp + nv * 4 > len(data) or ip + ni * 2 > len(data):
            raise ValueError('WKMD stream bounds')
        vertices = np.frombuffer(data, '<f4', count=nv * 8, offset=vp).reshape(nv, 8).copy()
        colors = np.frombuffer(data, np.uint8, count=nv * 4, offset=cp).reshape(nv, 4).copy()
        indices = np.frombuffer(data, '<u2', count=ni, offset=ip).copy()
        if not np.isfinite(vertices).all() or indices.max() >= nv:
            raise ValueError('WKMD nonfinite/index bounds')
        result.append(dict(vertices=vertices, colors=colors, indices=indices))
    return result


def inventory(installation, destination):
    installation = installation.resolve()
    destination.mkdir(parents=True, exist_ok=True)
    files, archives = [], []
    for path in sorted(installation.rglob('*')):
        if not path.is_file() or path.name == '.DS_Store':
            continue
        with path.open('rb') as f:
            magic = f.read(8)
            f.seek(0)
            digest = file_sha(f)
        rel = path.relative_to(installation).as_posix()
        role = 'active-installation' if rel.startswith('Media/') or rel == 'san11pk.exe' else 'auxiliary-not-proven-active'
        record = dict(path=rel, bytes=path.stat().st_size, sha256=digest, head_hex=magic.hex(), role=role)
        files.append(record)
        if magic[:4] != b'LINK':
            continue
        try:
            archive = Archive(path)
        except ValueError as error:
            record['archive_error'] = str(error)
            continue
        entries = []
        for i, (offset, length) in enumerate(archive.entries):
            data = archive.read(i)
            head = data[:8]
            fmt = head.decode('ascii') if all(32 <= c < 127 for c in head) else 'unknown'
            entries.append(dict(id=i, offset=offset, bytes=length, format=fmt,
                                head_hex=head.hex(), sha256=sha(data),
                                conversion='not-converted', android_output=None,
                                runtime_binding=None, validation='unverified'))
        archive.close()
        archives.append(dict(path=rel, sha256=digest, count=len(entries), entries=entries))
    report = dict(schema=1, installation=str(installation), source_kind='user-provided modded installation',
                  source_policy='read-only', files=files, archives=archives,
                  override_policy='Media bytes are authoritative; Backup, tools, patch archives and SIRE configs are auxiliary until runtime evidence proves activation; no clean retail baseline supplied')
    (destination / 'inventory.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    main = next(a for a in archives if a['path'].lower() == 'media/san11pkres.bin')
    archive = Archive(installation / main['path'])
    decoded = dict(objects=objects(archive.read(4805)), effects=effects(archive.read(4792)),
                   coordinate_status='raw source coordinates; world conversion/model ID binding still requires crosschecks')
    archive.close()
    (destination / 'placements.json').write_text(json.dumps(decoded, ensure_ascii=False, indent=2) + '\n')
    summary = dict(files=len(files), archives=[dict(path=a['path'], count=a['count']) for a in archives],
                   formats=dict(collections.Counter(e['format'] if e['format'] != 'unknown' else e['head_hex'][:8] for e in main['entries'])),
                   enabled_objects=len(decoded['objects']), map_effects=len(decoded['effects']))
    (destination / 'inventory-summary.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(summary, ensure_ascii=False))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('installation', type=Path)
    parser.add_argument('--output', type=Path, default=ROOT / 'docs/pc-visual')
    args = parser.parse_args()
    inventory(args.installation, args.output)
