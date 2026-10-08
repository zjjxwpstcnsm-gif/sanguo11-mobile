#!/usr/bin/env python3
"""Original force player-slot setter and undeployed person virtual48 getter.

Declared player slot changes are fixtures, not a verified original menu startup.
"""
import argparse
import json
import struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA, sha, output_guard


def inspect(installation, output):
    output_guard(installation, output)
    if output.exists():
        raise ValueError('Preserve earlier receipt')
    d, w, source, geography, unused = prepare(installation)
    before = bytes(w.u.mem_read(w.root, 0x300000))
    rng = bytes(w.u.mem_read(0x8a5d44, 4))
    evidence = json.loads(Path('out/session-b/duel-force-control-source.json').read_text())
    for f in evidence['functions']:
        assert sha(bytes(w.u.mem_read(int(f['address'], 16), f['boundedBytes']))) == f['sha256']
    people = []
    for native in [116, 163, 195, 222, 558, 14, 517]:
        person = w.call(0x490b00, native, receiver=w.root)
        vtable = struct.unpack('<I', w.u.mem_read(person, 4))[0]
        owner = w.call(struct.unpack('<I', w.u.mem_read(vtable + 0x40, 4))[0], receiver=person)
        people.append(dict(nativeId=native, pointer=person, owner=owner, unitId=w.call(0x489220, receiver=person)))
    force = w.call(0x490aa0, 2, receiver=w.root)
    original = bytes(w.u.mem_read(force + 0x60, 4))
    rows = []
    for requested in [-2, -1, 0, 1, 7, 8]:
        w.u.mem_write(force + 0x60, original)
        w.call(0x481480, requested & 0xffffffff, receiver=force)
        slot = struct.unpack('<i', w.u.mem_read(force + 0x60, 4))[0]
        readings = []
        for p in people:
            readings.append(dict(nativeId=p['nativeId'], owner=p['owner'], unitId=p['unitId'], forceHuman=w.call(0x47a690, receiver=p['pointer']), manualArmy=w.call(0x47a6d0, receiver=p['pointer'])))
        rows.append(dict(requested=requested, actualSlot=slot, readings=readings))
        expected = bytearray(before)
        offset = force + 0x60 - w.root
        expected[offset:offset+4] = struct.pack('<i', slot)
        assert bytes(expected) == bytes(w.u.mem_read(w.root, 0x300000))
        assert rng == bytes(w.u.mem_read(0x8a5d44, 4))
    w.u.mem_write(w.root, before)
    assert before == bytes(w.u.mem_read(w.root, 0x300000)) and rng == bytes(w.u.mem_read(0x8a5d44, 4))
    output.write_text(json.dumps(dict(exeSha=EXE_SHA, source=source, geography=geography, force=2, initialSlot=struct.unpack('<i', original)[0], people=people, rows=rows, wholeWorldAndRngRestored=True, limits=['Original force setter and getters execute unchanged', 'Player slots are explicit fixtures; not original menu startup or delegated army acceptance', 'Valid registered persons are not all effectively active'], completeGoal=False), indent=2) + '\n')
    print('PASS original force control', sha(output.read_bytes()), flush=True)


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('installation', type=Path)
    p.add_argument('--output', type=Path, required=True)
    a = p.parse_args()
    inspect(a.installation, a.output)
