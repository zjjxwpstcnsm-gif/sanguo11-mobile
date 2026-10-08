#!/usr/bin/env python3
"""Original58a200 crew strength with current source/health/nominee relations."""
import argparse
import json
import struct
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_EAX
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA, sha, output_guard


def inspect(installation, output, table):
    output_guard(installation, output)
    if output.exists() or table.exists():
        raise ValueError('Preserve earlier receipt')
    raw = Path('out/session-b/duel-unit-context-source0-v4.json').read_bytes()
    assert sha(raw) == '49c1ed7a9e7453e9834ef58d08f2c8f7f906a278b5d1b43bc3e60328f5592c38'
    context = json.loads(raw)
    d, w, source, geography, unused = prepare(installation)
    baseline = bytes(w.u.mem_read(w.root, 0x300000))
    rng = bytes(w.u.mem_read(0x8a5d44, 4))
    units, crews = [], []
    for unit, case in zip(context['units'], context['cases']):
        pointer = unit['pointer']
        w.u.mem_write(pointer, bytes.fromhex(case['afterHex']))
        w.u.mem_write(pointer + 0x3c, struct.pack('<hh', 80 + unit['index'], 80))
        w.u.mem_write(pointer + 0x24, bytes(4))
        w.u.reg_write(UC_X86_REG_EAX, pointer)
        location = w.call(0x4a7530)
        for person in case['declaredCrew']:
            w.call(0x4a0cb0, person['pointer'], location, receiver=0x799895c, count=10000000)
            for injury in range(4):
                w.call(0x50c690, person['pointer'], injury, 1, count=10000000)
        w.call(0x496f40, receiver=pointer, count=10000000)
        units.append(pointer)
        crews.append(case['declaredCrew'])
    rows = []
    for side, (unit, crew) in enumerate(zip(units, crews)):
        for health in [10, 50, 80, 100]:
            for person in crew:
                w.u.mem_write(person['pointer'] + 0x128, bytes([health]))
            for nomination in [-1, 0, 1, 2]:
                person = crew[nomination] if nomination >= 0 else None
                nominated = person['pointer'] if person else 0
                before = bytes(w.u.mem_read(w.root, 0x300000))
                result = w.call(0x58a200, unit, nominated, count=10000000)
                assert before == bytes(w.u.mem_read(w.root, 0x300000)) and rng == bytes(w.u.mem_read(0x8a5d44, 4))
                actual = nominated if nominated else w.call(0x589ca0, unit, 0, count=10000000)
                nominee = w.call(0x4883c0, receiver=actual) if actual else -1
                facts = []
                for p in crew:
                    pointer = p['pointer']
                    facts.append(dict(nativeId=p['nativeId'], health=health, war=w.call(0x489080, receiver=pointer) & 255, personality=struct.unpack('<i', w.u.mem_read(pointer + 0xfc, 4))[0], treasureBonus=w.call(0x4faa60, pointer), ruler=bool(w.call(0x488c00, receiver=pointer)), dislikesNominee=bool(w.call(0x4889e0, nominee & 0xffffffff, receiver=pointer)) if nominee >= 0 else False))
                rows.append(dict(side=side, nomination=nomination, nominee=nominee, result=result, crew=facts))
    w.u.mem_write(w.root, baseline)
    w.u.mem_write(0x8a5d44, rng)
    assert baseline == bytes(w.u.mem_read(w.root, 0x300000)) and rng == bytes(w.u.mem_read(0x8a5d44, 4))
    output.write_text(json.dumps(dict(exeSha=EXE_SHA, source=source, geography=geography, contextSha=sha(raw), rows=rows, wholeWorldAndRngPure=True, wholeWorldAndRngRestored=True, limits=['Declared linked source units and mutable health boundary fixtures', 'Exact original58a200 body and getters; no score/relationship/RNG replacement', 'Not ordinary new-game command/human selection/APK proof'], completeGoal=False), indent=2) + '\n')
    table.write_text('# original58a200 source receipt SHA ' + sha(output.read_bytes()) + '\n' + ''.join('\t'.join([str(r['nominee']), str(r['result']), ';'.join(','.join(str(int(p[k])) for k in ['nativeId', 'health', 'war', 'personality', 'treasureBonus', 'ruler', 'dislikesNominee']) for p in r['crew'])]) + '\n' for r in rows))
    print('PASS original crew strength', len(rows), sha(output.read_bytes()), sha(table.read_bytes()), flush=True)


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('installation', type=Path)
    p.add_argument('--output', type=Path, required=True)
    p.add_argument('--table', type=Path, required=True)
    a = p.parse_args()
    inspect(a.installation, a.output, a.table)
