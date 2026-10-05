#!/usr/bin/env python3
"""Execute original coarse water setup and exact 96-byte water vertex writes.

Keeps source default visual RNG isolated. Does not execute PC GPU or change the
Android fine-face water implementation; source/reference acceptance is separate.
"""
import argparse
import collections
import json
from pathlib import Path
import struct

from unicorn import Uc, UC_ARCH_X86, UC_MODE_32
from unicorn.x86_const import UC_X86_REG_ECX, UC_X86_REG_ESP, UC_X86_REG_EIP
from pc_resources import Archive, sha
from inspect_pc_effect_bindings import EXE_SHA

ROOT = Path(__file__).resolve().parents[2]


def check(installation, output, reference):
    exe = (installation/'san11pk.exe').read_bytes()
    if sha(exe) != EXE_SHA:
        raise ValueError('Source EXE changed')
    archive = Archive(installation/'Media/san11pkres.bin')
    try:
        terrain, water_texture = archive.read(4793), archive.read(4844)
    finally:
        archive.close()
    start = 8+1025**2*8
    if terrain[:8] != b'K3ST0006' or len(terrain) != start+1024**2*8:
        raise ValueError('Source terrain bounds')
    # Original named configuration getter supplies WaterAnimTime=10000 ms.
    if exe[0x3e917:0x3e921] != b'\x68\x10\x27\0\0\x68\x34\x2c\x79\0':
        raise ValueError('Original WaterAnimTime default changed')
    if struct.unpack_from('<3I', exe, 0x4a5b68) != (625, 0, 0x9908b0df):
        raise ValueError('Native visual RNG static initialization changed')
    u = Uc(UC_ARCH_X86, UC_MODE_32)
    u.mem_map(0x400000, 0x500000)
    u.mem_write(0x400000, exe[:0x500000])
    u.mem_map(0x6ed0000, 0x10000)
    heap, stack, stop = 0x10000000, 0x20000000, 0x30000000
    u.mem_map(heap, 0x1400000)
    u.mem_map(stack, 0x10000)
    u.mem_map(stop, 4096)
    u.mem_write(heap+8, terrain[start:])
    native_vertices = bytearray(1025**2*10)
    native_vertices[::10] = terrain[8:start:8]
    u.mem_write(heap+0x800008, bytes(native_vertices))
    u.mem_write(0x8a5a78, struct.pack('<I', 10000))
    base = heap+0x1285012
    counts, covered_empty_faces, verified = collections.Counter(), 0, 0
    for x in range(256):
        for y in range(256):
            expected = None
            for dx in range(4):
                for dy in range(4):
                    word = struct.unpack_from('<Q', terrain, start+((x*4+dx)*1024+y*4+dy)*8)[0]
                    if (word >> 44) & 255:
                        expected = ((word >> 44) & 255, (word >> 52) & 3)
                        break
                if expected is not None:
                    break
            record = base+(x*256+y)*10
            u.mem_write(stack+0x8000, struct.pack('<3I', stop, x, y))
            u.reg_write(UC_X86_REG_ECX, heap)
            u.reg_write(UC_X86_REG_ESP, stack+0x8000)
            u.emu_start(0x415e20, stop, count=30000)
            if u.reg_read(UC_X86_REG_EIP) != stop:
                raise AssertionError('Coarse setup exceeded instruction budget')
            phase, period, _, unused, height, mask, flags = struct.unpack('<3H4B', u.mem_read(record, 10))
            value = expected[0] if expected else 0
            alpha_mask = sum((terrain[8+((x*4+dx)*1025+y*4+dy)*8] < value) << bit
                             for bit, (dx, dy) in enumerate(((0, 0), (4, 0), (0, 4), (4, 4))))
            if (height, mask, flags) != (value, alpha_mask, (expected[1] | 4) if expected else 0):
                raise AssertionError(('Source coarse record', x, y, height, mask, flags, expected))
            if value:
                if not (10000 <= period < 20000 and 0 <= phase < period):
                    raise AssertionError('Original randomized phase/period bounds')
                counts[expected[1]] += 1
                covered_empty_faces += sum(not ((struct.unpack_from('<Q', terrain,
                    start+((x*4+dx)*1024+y*4+dy)*8)[0] >> 44) & 255)
                    for dx in range(4) for dy in range(4))
            verified += 1
    records = bytes(u.mem_read(base, 256**2*10))
    reference.parent.mkdir(parents=True, exist_ok=True)
    reference.write_bytes(b'PCWCO001'+records)
    pointer, vertices = heap+0x2000, heap+0x3000
    vertex_cases = 0
    for x, y in ((0, 0), (155, 146)):
        for frame in range(64):
            for mask in range(16):
                left, top = (frame % 8)*.125, (frame // 8)*.125
                right, bottom = left+.125, top+.125
                level = struct.unpack('<f', struct.pack('<f', (11+struct.unpack('<f', struct.pack('<f', .0025))[0])*.5))[0]
                u.mem_write(pointer, struct.pack('<I', vertices))
                args = struct.pack('<3I5fI', stop, pointer, x | (y << 16), level,
                                   left, top, right, bottom, mask)
                u.mem_write(stack+0x8000, args)
                u.reg_write(UC_X86_REG_ESP, stack+0x8000)
                u.emu_start(0x422980, stop, count=500)
                expected_vertices = b''.join(struct.pack('<3fI2f', vx, level, vz,
                    0xffffffff if mask & (1 << bit) else 0x20ffffff, uvx, uvy)
                    for bit, (vx, vz, uvx, uvy) in enumerate(((x*20, y*20, left, top),
                        (x*20+20, y*20, right, top), (x*20, y*20+20, left, bottom),
                        (x*20+20, y*20+20, right, bottom))))
                if bytes(u.mem_read(vertices, 96)) != expected_vertices or u.reg_read(UC_X86_REG_EIP) != stop:
                    raise AssertionError(('Original water vertex/alpha/UV writes', x, y, frame, mask))
                vertex_cases += 1
    report = dict(schema=1, status='PASS_SOURCE_MACHINE_ONLY', goal_complete=False,
        executable_sha256=EXE_SHA, terrain=dict(resource_id=4793, sha256=sha(terrain)),
        texture=dict(resource_id=4844, sha256=sha(water_texture), image_count=2,
                     frames_per_image=64, native_loader='422eb0 ->12ec -> WFTX'),
        native_functions=[dict(va=hex(va), bytes=n, sha256=sha(exe[va-0x400000:va-0x400000+n]))
                          for va, n in ((0x415e20, 0xfc), (0x415d80, 0x9e), (0x422980, 0x16f))],
        coarse_records_checked=verified, vertex_cases=vertex_cases,
        wet_flags=dict(counts), fine_zero_faces_covered_by_native_coarse_water=covered_empty_faces,
        corners='TL TR BL BR; wet alpha255, dry alpha32; full coarse quad, no fine-face polygon clipping',
        default_WaterAnimTime_ms=10000, native_visual_RNG='raw initial index625; native444150 auto-seeds4357',
        source_initialization_reference=dict(path=str(reference.relative_to(ROOT)), sha256=sha(reference.read_bytes())),
        limits=['Native registry setting/MOD memory overrides and PC rasterization are unverified',
                'Per-cell periods/phases are one authentic native default-seed run, not a captured PC session',
                'Android binding and installed evidence are separately recorded; this checker cannot accept them',
                'No gameplay RNG/state/save read or write'])
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    print(json.dumps({k: report[k] for k in ('status', 'coarse_records_checked', 'vertex_cases',
                'wet_flags', 'fine_zero_faces_covered_by_native_coarse_water')}))


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('installation', type=Path)
    p.add_argument('--output', type=Path, default=ROOT/'out/pc-visual/v146-water-mesh-source.json')
    p.add_argument('--reference', type=Path, default=ROOT/'out/pc-visual/v146-water-coarse-source.bin')
    a = p.parse_args()
    check(a.installation, a.output, a.reference)
