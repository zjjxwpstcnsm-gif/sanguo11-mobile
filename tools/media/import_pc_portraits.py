#!/usr/bin/env python3
"""Strict FCE pixel extraction. Source face IDs and image groups are NOT officer identities."""
import argparse
import hashlib
import io
import json
from pathlib import Path
import struct
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'content'))
from pc_resources import wftx_levels


def sha(data):
    return hashlib.sha256(data).hexdigest()


def convert(installation, output):
    installation = installation.resolve()
    output = output.resolve()
    if output == installation or installation in output.parents:
        raise ValueError('Output must be outside read-only installation')
    if output.exists():
        raise ValueError('Use a fresh output for reproducibility')
    source = installation / 'Media/face/San11Face00.fce'
    data = source.read_bytes()
    magic, version, start, count, images = struct.unpack_from('<5I', data)
    if magic != 0x45434146 or version != 64 or count < 1 or start+count > 2400 or images != count*3:
        raise ValueError('Unexamined FCE header')
    table = 20+count*4
    end = table+images*8
    entries = list(struct.iter_unpack('<II', data[table:end]))
    if len(entries) != images:
        raise ValueError('Truncated FCE table')
    output.mkdir(parents=True)
    rows = []
    for index, (offset, size) in enumerate(entries):
        if offset != end or offset+size > len(data):
            raise ValueError('Noncontiguous/out-of-bounds FCE entry')
        end = offset+size
        # Original46e41b..46e45a loads count large entries then count*2
        # interleaved small entries at registry+2400*8+start*16.
        group = 0 if index < count else 1+(index-count) % 2
        face = start+index if index < count else start+(index-count)//2
        registry = face if group == 0 else 2400+face*2+group-1
        row = dict(resourceIndex=index, sourceRegistryIndex=registry, faceId=face, imageGroup=group, offset=offset, bytes=size,
                   officerId=None, nativeId=None, sourceVariant=None, runtimeRole='UNVERIFIED')
        rows.append(row)
        if not size:
            row['status'] = 'SOURCE_EMPTY'
            continue
        raw = data[offset:end]
        decoded = wftx_levels(raw)
        if len(decoded) != 1:
            raise ValueError('Unexamined multi-frame FCE entry')
        frame = decoded[0]
        image = frame['levels'][0].convert('RGBA')
        png = io.BytesIO()
        image.save(png, format='PNG', compress_level=9, optimize=False)
        rel = f'group-{group}/face-{face:04d}.png'
        target = output/rel
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(png.getvalue())
        rawpath = output/'original'/f'{index:04d}.wftx'
        rawpath.parent.mkdir(exist_ok=True)
        rawpath.write_bytes(raw)
        alpha = image.getchannel('A')
        row.update(status='SOURCE_PIXELS_DECODED_IDENTITY_UNBOUND', width=image.width, height=image.height,
                   sourceBits=frame['bits'], sourceMips=frame['source_levels'], sourcePalette=frame['source_palette'],
                   alphaMin=alpha.getextrema()[0], alphaMax=alpha.getextrema()[1],
                   nonOpaquePixels=image.width*image.height-alpha.histogram()[255],
                   sourceSha256=sha(raw), rgbaSha256=sha(image.tobytes()), pngSha256=sha(png.getvalue()),
                   asset=rel, original=rawpath.relative_to(output).as_posix(), decoder='pc_resources.wftx_levels: original base pixels and source mip/palette')
    if end != len(data):
        raise ValueError('Trailing FCE bytes')
    manifest = dict(schema=1, sourcePolicy='READ_ONLY_NO_WINE', sourceFile=source.relative_to(installation).as_posix(),
                    sourceSha256=sha(data), descriptorSha256=sha(data[20:table]),
                    sourceFaceCount=count, sourceImageCount=images,
                    decodedImages=sum(bool(r['bytes']) for r in rows), effectiveOfficerCoverage=0,
                    indexLayout='Original46e41b..46e45a: large[count], then (small1,small2)[count]; registry large=face, small=2400+2*face+form-1',
                    status='PIXELS_ONLY_NOT_RUNTIME_OR_ALL_OFFICER_ACCEPTANCE', entries=rows,
                    limits=['Three source image groups retained without guessing dialogue/duel usage.',
                            'Empty entries are not aliases or invented fallback pixels.',
                            'MOD/backup live precedence and session1 identity mapping still required.'])
    (output/'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    print(json.dumps({k: manifest[k] for k in ['sourceFaceCount', 'sourceImageCount', 'decodedImages', 'effectiveOfficerCoverage']}))


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('installation', type=Path)
    p.add_argument('--output', type=Path, required=True)
    a = p.parse_args()
    convert(a.installation, a.output)
