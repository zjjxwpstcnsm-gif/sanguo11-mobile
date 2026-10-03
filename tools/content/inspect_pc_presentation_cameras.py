#!/usr/bin/env python3
"""Read-only camera ownership/caller evidence from the supplied EXE.

Internal manager defaults and temporary-camera builders are deliberately not
accepted as the camera for every fullscreen cue. No Wine or PC writes occur.
"""
import argparse
import hashlib
import json
import struct
from pathlib import Path
from capstone import Cs, CS_ARCH_X86, CS_MODE_32
from inspect_pc_effect_bindings import EXE_SHA

ROOT = Path(__file__).resolve().parents[2]


def inspect(installation, output):
    data = (installation / 'san11pk.exe').read_bytes()
    if hashlib.sha256(data).hexdigest() != EXE_SHA:
        raise ValueError('Reinspect changed source executable')
    md = Cs(CS_ARCH_X86, CS_MODE_32)

    def code(a, b):
        return list(md.disasm(data[a - 0x400000:b - 0x400000], a))

    def record(instructions):
        return [dict(va=hex(i.address), bytes=i.bytes.hex(),
                     mnemonic=i.mnemonic, operands=i.op_str) for i in instructions]

    def require(instructions, va, mnemonic, operands):
        if not any(i.address == va and i.mnemonic == mnemonic and
                   i.op_str == operands for i in instructions):
            raise ValueError('Camera instruction changed at ' + hex(va))

    if struct.unpack_from('<I', data, 0x379b90)[0] != 0x414fd0:
        raise ValueError('Map camera provider vtable changed')
    provider = code(0x414fd0, 0x414fd4)
    require(provider, 0x414fd0, 'lea', 'eax, [ecx + 0x40]')
    draw = code(0x413650, 0x41369a)
    require(draw, 0x413663, 'mov', 'ebx, dword ptr [esp + 0xc]')
    require(draw, 0x41366a, 'call', '0x44c650')
    require(draw, 0x413680, 'push', 'ebx')
    require(draw, 0x41368b, 'push', 'ebx')
    callers = []
    for call in [0x558ace, 0x5599f7, 0x5a1599, 0x5a1e08, 0x5a1fcf]:
        offset = call - 0x400000
        start = data.rfind(b'\xcc\xcc\xcc', max(0x1000, offset - 0x2000), offset)
        if start < 0:
            raise ValueError('Missing examined caller boundary')
        start += 3
        while data[start] == 0xcc:
            start += 1
        instructions = code(start + 0x400000, call + 5)
        if not instructions or instructions[-1].address != call:
            raise ValueError('Camera caller decoding does not reach CALL')
        require(instructions, call, 'call', '0x413650')
        if not any('0x32602b0' in i.op_str for i in instructions):
            raise ValueError('Expected original map camera provider')
        callers.append(dict(entry=hex(start + 0x400000), call=hex(call),
                            prefix_sha256=hashlib.sha256(data[start:offset + 5]).hexdigest(),
                            instructions=record(instructions)))
    temporary = code(0x5a13e0, 0x5a1616)
    require(temporary, 0x5a1457, 'mov', 'esi, 0x96a5fd0')
    require(temporary, 0x5a145e, 'rep movsd', 'dword ptr es:[edi], dword ptr [esi]')
    require(temporary, 0x5a15d8, 'rep movsd', 'dword ptr es:[edi], dword ptr [esi]')
    builder = code(0x5a0c20, 0x5a0d43)
    require(builder, 0x5a0c85, 'push', '0x96a5fd0')
    require(builder, 0x5a0cbe, 'call', '0x441f60')
    camera_builder = code(0x441f60, 0x441fdf)
    require(camera_builder, 0x441f80, 'push', '0x3edf66f3')
    output.parent.mkdir(parents=True, exist_ok=True)
    report = dict(schema=1, goal_complete=False, status='SOURCE_CAMERA_OWNERSHIP_VERIFIED_LENS_BINDING_PENDING',
                  source_executable_sha256=EXE_SHA,
                  provider=dict(global_address='0x32602b0', vtable='0x779b90',
                                camera_offset='0x40', instructions=record(provider)),
                  draw=dict(function='0x413650', instructions=record(draw),
                            contract='Caller camera configures original device via44c650 and is passed to both virtual draw methods'),
                  callers=callers,
                  temporary_camera=dict(storage='0x96a5fd0',
                                        build=record(builder), draw_and_restore=record(temporary),
                                        note='5a13e0 copies512B temporary camera into provider, draws, then restores512B saved camera; cue/event association is unresolved'),
                  camera_builder=dict(function='0x441f60', instructions=record(camera_builder),
                                      aperture_radian_bits='0x3edf66f3',
                                      aperture_radians=struct.unpack('<f', struct.pack('<I', 0x3edf66f3))[0]),
                  limits=['Default manager25deg is not proof of the active fullscreen cue lens',
                          'Temporary camera path is source evidence; which normal cue uses it still needs caller/event association',
                          'Android conversion30deg/4:3 remains explicit diagnostic input, not source fullscreen acceptance',
                          'No live PC/MOD camera values or performance are inferred; no Wine launched'])
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print('PASS source camera provider/draw/5 callers/temporary copy and restore; lens binding pending')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('installation', type=Path)
    parser.add_argument('--output', type=Path,
                        default=ROOT / 'docs/pc-visual/presentation-cameras-source-working.json')
    args = parser.parse_args()
    inspect(args.installation, args.output)
