#!/usr/bin/env python3
"""Execute supplied EXE face-water extraction/height arithmetic, read-only.

No gameplay or emitter RNG is invoked. This proves the packed field and plane
height, not the original water shader, coarse topology or rasterization.
"""
import argparse
import collections
import gzip
import json
from pathlib import Path
import struct

from unicorn import Uc, UC_ARCH_X86, UC_MODE_32
from unicorn.x86_const import UC_X86_REG_ECX, UC_X86_REG_ESP, UC_X86_REG_EIP
from pc_resources import Archive, sha
from inspect_pc_effect_bindings import EXE_SHA

ROOT = Path(__file__).resolve().parents[2]


def check(installation, output, reference, converted):
    exe = (installation / 'san11pk.exe').read_bytes()
    if sha(exe) != EXE_SHA:
        raise ValueError('Reinspect water EXE')
    bias = struct.unpack_from('<f', exe, 0x779bb0 - 0x400000)[0]
    if bias != struct.unpack('<f', struct.pack('<f', .0025))[0]:
        raise ValueError('Source water bias changed')
    archive = Archive(installation / 'Media/san11pkres.bin')
    try:
        terrain = archive.read(4793)
    finally:
        archive.close()
    if terrain[:8] != b'K3ST0006' or len(terrain) != 8 + 1025**2*8 + 1024**2*8:
        raise ValueError('Terrain source bounds')
    face_start = 8 + 1025**2*8
    # Independent integer extraction; source routine 415bb0 is checked below
    # with every possible byte and both wet/dry terrain comparisons.
    water = bytes((struct.unpack_from('<Q', terrain, face_start + i*8)[0] >> 44) & 255
                  for i in range(1024**2))
    xfast_water = bytes(water[x*1024+y] for y in range(1024) for x in range(1024))
    old_nibbles = bytes(v & 15 for v in water)
    u = Uc(UC_ARCH_X86, UC_MODE_32)
    u.mem_map(0x400000, 0x500000)
    u.mem_write(0x400000, exe[:0x500000])
    heap, stack, stop = 0x10000000, 0x20000000, 0x30000000
    u.mem_map(heap, 0x1400000)
    u.mem_map(stack, 0x10000)
    u.mem_map(stop, 4096)
    caller, destination = heap+0x1000, heap+0x2000
    u.mem_write(caller, b'\xd9\x1d' + struct.pack('<I', destination)
                + b'\xe9' + struct.pack('<i', stop-(caller+11)))
    plane_heights, cases = [], 0
    for value in range(256):
        for land in sorted({0, max(0, value-1), value, min(255, value+1)}):
            # Unrelated layer/flag bits are deliberately nonzero. The real
            # consumer must ignore them when reading bits44..51.
            word = (0xabc << 52) | (value << 44) | 0x76543210abcd
            word = (word & ~(255 << 44)) | (value << 44)
            u.mem_write(heap+8, struct.pack('<Q', word))
            u.mem_write(heap+0x800008, bytes([land]))
            u.mem_write(stack+0x8000, struct.pack('<3I', caller, 0, 0))
            u.reg_write(UC_X86_REG_ECX, heap)
            u.reg_write(UC_X86_REG_ESP, stack+0x8000)
            u.emu_start(0x415bb0, stop, count=1000)
            actual = bytes(u.mem_read(destination, 4))
            expected = struct.pack('<f', max(land*.5, (value+bias)*.5 if value else 0))
            if actual != expected or u.reg_read(UC_X86_REG_EIP) != stop:
                raise AssertionError(('Source water height', value, land, actual.hex(), expected.hex()))
            if land == 0:
                plane_heights.append(actual)
            cases += 1
    reference.parent.mkdir(parents=True, exist_ok=True)
    reference.write_bytes(b'PCWTR001' + b''.join(plane_heights) + xfast_water)
    converted_status = 'NOT_CHECKED'
    if converted:
        payload = gzip.decompress(converted.read_bytes())
        expected = (b'PCMAP002' + bytes(terrain[8+(x*1025+y)*8]
                    for y in range(1025) for x in range(1025)) + xfast_water)
        if payload[:len(expected)] != expected:
            raise AssertionError('Converted terrain/water differs from source')
        converted_status = 'BYTE_EXACT'
    result = dict(schema=1, status='PASS', goal_complete=False,
        executable_sha256=EXE_SHA, source_resource=dict(id=4793, sha256=sha(terrain)),
        consumer=dict(start='415bb0', bytes=0x415ca1-0x415bb0,
            sha256=sha(exe[0x15bb0:0x15ca1]), shift_helper='707de0',
            water_bits=[44, 52], PC_plane='(nonzero byte + float32(.0025)) * .5',
            scene_scale=.05), machine_cases=cases, planes=256,
        source_faces=len(water), source_water_histogram=dict(sorted(collections.Counter(water).items())),
        previous_nibble_wrong_faces=sum(a!=b for a,b in zip(water,old_nibbles)),
        previous_nibble_omitted_faces=sum(a>0 and b==0 for a,b in zip(water,old_nibbles)),
        reference=dict(path=str(reference.relative_to(ROOT)), bytes=reference.stat().st_size,
            sha256=sha(reference.read_bytes())), converted=converted_status,
        limits=['No PC image comparison, original water material/animation or coarse topology acceptance',
            'Source 415e20 also chooses a first nonzero face within4x4 for coarse records; Android per-face topology is still provisional',
            'No mutable gameplay state or random source used'])
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, indent=2)+'\n')
    print(json.dumps({k:result[k] for k in ('status','machine_cases','planes','source_faces',
        'previous_nibble_wrong_faces','previous_nibble_omitted_faces','converted')}))


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('installation', type=Path)
    p.add_argument('--output', type=Path, default=ROOT/'out/pc-visual/v146-water-source-check.json')
    p.add_argument('--reference', type=Path, default=ROOT/'out/pc-visual/v146-water-source-reference.bin')
    p.add_argument('--converted', type=Path)
    a = p.parse_args()
    check(a.installation, a.output, a.reference, a.converted)
