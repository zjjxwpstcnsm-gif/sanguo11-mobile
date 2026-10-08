#!/usr/bin/env python3
"""Original attack-mask bit8 admission; declared units are numeric fixtures."""
import argparse
import json
import struct
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_EAX
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA, sha, output_guard


def inspect(installation, output):
    output_guard(installation, output)
    if output.exists():
        raise ValueError('Preserve earlier receipt')
    receipt = Path('out/session-b/duel-upstream-callers-source-v5.json').read_bytes()
    assert sha(receipt) == '54d7bec8502a1a48385f50c12d131076d7c1b749e1f0e58088aad07c8fcbfc2c'
    context_raw = Path('out/session-b/duel-unit-context-source0-v4.json').read_bytes()
    assert sha(context_raw) == '49c1ed7a9e7453e9834ef58d08f2c8f7f906a278b5d1b43bc3e60328f5592c38'
    context = json.loads(context_raw)
    d, w, source, geography, unused = prepare(installation)
    for f in json.loads(receipt)['functions']:
        assert sha(bytes(w.u.mem_read(int(f['address'], 16), f['boundedBytes']))) == f['sha256']
    baseline = bytes(w.u.mem_read(w.root, 0x300000))
    rng = bytes(w.u.mem_read(0x8a5d44, 4))
    units = []
    for unit, case in zip(context['units'], context['cases']):
        pointer = unit['pointer']
        w.u.mem_write(pointer, bytes.fromhex(case['afterHex']))
        w.u.mem_write(pointer + 0x3c, struct.pack('<hh', 80 + unit['index'], 80))
        w.u.mem_write(pointer + 0x24, struct.pack('<i', 0))
        w.u.reg_write(UC_X86_REG_EAX, pointer)
        location = w.call(0x4a7530)
        for person in case['declaredCrew']:
            w.call(0x4a0cb0, person['pointer'], location, receiver=0x799895c, count=10000000)
        w.call(0x496f40, receiver=pointer, count=10000000)
        units.append(pointer)
    actor, target = units
    range_pointer = d.fixture + 0x7800
    w.u.mem_write(range_pointer, struct.pack('<2I', 0, 0))
    point = struct.unpack('<I', w.u.mem_read(target + 0x3c, 4))[0]
    rows = []
    for energy in [0, 1, 10, 100]:
        for distance in [1, 2]:
            for flags in [0, 8]:
                w.u.mem_write(actor + 0x1a, bytes([energy]))
                before = bytes(w.u.mem_read(w.root, 0x300000))
                w.u.reg_write(UC_X86_REG_EAX, range_pointer)
                result = w.call(0x5a4990, actor, 3, point, distance, flags, receiver=target, count=10000000)
                assert before == bytes(w.u.mem_read(w.root, 0x300000))
                assert rng == bytes(w.u.mem_read(0x8a5d44, 4))
                rows.append(dict(energy=energy, distance=distance, flags=flags, attackMask=result, challengeVisible=bool(result & 8)))
    w.u.mem_write(w.root, baseline)
    assert baseline == bytes(w.u.mem_read(w.root, 0x300000))
    assert rng == bytes(w.u.mem_read(0x8a5d44, 4))
    output.write_text(json.dumps(dict(exeSha=EXE_SHA, source=source, geography=geography, sourceReceiptSha=sha(receipt), contextSha=sha(context_raw), rows=rows, wholeWorldAndRngRestored=True, completeGoal=False, limits=['Original5a4990 attack-mask bit8, not separate strategy5a3cd0/495170 energy table', 'Declared adjacent units and numeric flags; no ordinary PC deployment/menu or Android duel proof', 'Visibility alone does not prove formal cost/action/terminal callbacks']), indent=2) + '\n')
    print('PASS original target admission', len(rows), sha(output.read_bytes()), flush=True)


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('installation', type=Path)
    p.add_argument('--output', type=Path, required=True)
    a = p.parse_args()
    inspect(a.installation, a.output)
