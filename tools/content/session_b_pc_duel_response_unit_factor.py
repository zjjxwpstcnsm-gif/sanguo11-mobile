#!/usr/bin/env python3
"""Observe original58a8a0 unit contribution; no getter/rule/RNG replacement."""
import argparse
import json
import struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX, UC_X86_REG_ESI, UC_X86_REG_EBX, UC_X86_REG_EDI, UC_X86_REG_EBP, UC_X86_REG_ECX, UC_X86_REG_ESP
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA, sha, output_guard


def inspect(installation, output, table, detailed=False):
    output_guard(installation, output)
    if output.exists() or table.exists():
        raise ValueError('Preserve earlier receipt')
    raw = Path('out/session-b/duel-unit-context-source0-v4.json').read_bytes()
    assert sha(raw) == '49c1ed7a9e7453e9834ef58d08f2c8f7f906a278b5d1b43bc3e60328f5592c38'
    context = json.loads(raw)
    d, w, source, geography, unused = prepare(installation)
    baseline = bytes(w.u.mem_read(w.root, 0x300000))
    rng = bytes(w.u.mem_read(0x8a5d44, 4))
    units = []
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
    actor = w.call(0x490b00, 116, receiver=w.root)
    point = d.fixture + 0x7900
    w.u.mem_write(point, bytes(w.u.mem_read(units[1] + 0x3c, 4)))
    observations = []
    stages = []
    def observe(u, address, size, user):
        value = u.reg_read(UC_X86_REG_ESI)
        observations.append(value if value < 0x80000000 else value - 0x100000000)
    hook = w.u.hook_add(UC_HOOK_CODE, observe, begin=0x58a9d9, end=0x58a9d9)
    stage_hooks = []
    if detailed:
        for address in [0x58aa38, 0x58aaa3, 0x58ab21, 0x58ab47, 0x58ab97, 0x58abc6]:
            def stage(u, address, size, user):
                sp = u.reg_read(UC_X86_REG_ESP)
                stages.append(dict(address=hex(address), registers={name:u.reg_read(reg) for name,reg in [('eax',UC_X86_REG_EAX),('ebx',UC_X86_REG_EBX),('ecx',UC_X86_REG_ECX),('esi',UC_X86_REG_ESI),('edi',UC_X86_REG_EDI),('ebp',UC_X86_REG_EBP)]}, stack=list(struct.unpack('<20I',u.mem_read(sp,80)))))
            stage_hooks.append(w.u.hook_add(UC_HOOK_CODE, stage, begin=address, end=address))
    rows = []
    for left in [0, 999, 1000, 5000, 10000, 60000]:
        for right in [0, 999, 1000, 5000, 10000, 60000]:
            for powers in [(32, 33, 30, 27), (0, 255, 255, 0), (255, 0, 0, 255)]:
                for pointer, troops, pair in zip(units, [left, right], [powers[:2], powers[2:]]):
                    w.u.mem_write(pointer + 0x18, struct.pack('<H', troops))
                    w.u.mem_write(pointer + 0xc9, bytes(pair))
                before = bytes(w.u.mem_read(w.root, 0x300000))
                w.u.mem_write(0x8a5d44, struct.pack('<I', 23))
                observations.clear()
                stages.clear()
                response = w.call(0x58a8a0, actor, units[0], units[1], point, count=10000000)
                assert len(observations) == 1 and before == bytes(w.u.mem_read(w.root, 0x300000))
                rows.append(dict(leftTroops=left, rightTroops=right, powers=powers, contribution=observations[0], response=response, rngAfter=struct.unpack('<I', w.u.mem_read(0x8a5d44, 4))[0], stages=list(stages)))
    w.u.hook_del(hook)
    for h in stage_hooks:w.u.hook_del(h)
    w.u.mem_write(w.root, baseline)
    w.u.mem_write(0x8a5d44, rng)
    assert baseline == bytes(w.u.mem_read(w.root, 0x300000)) and rng == bytes(w.u.mem_read(0x8a5d44, 4))
    output.write_text(json.dumps(dict(exeSha=EXE_SHA, source=source, geography=geography, contextSha=sha(raw), rows=rows, wholeWorldAndRngRestored=True, limits=['Original complete58a8a0 with observer only', 'Troops/C9/CA are declared boundary fixtures; current original496570 bindings still required', 'No ordinary deployment/human picker/fee/APK proof'], completeGoal=False), indent=2) + '\n')
    table.write_text('# original58a8a0 unit contribution; source receipt SHA ' + sha(output.read_bytes()) + '\n' + ''.join('\t'.join(map(str, [r['leftTroops'], r['rightTroops'], *r['powers'], r['contribution']])) + '\n' for r in rows))
    print('PASS original response unit factor', len(rows), sha(output.read_bytes()), sha(table.read_bytes()), flush=True)


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('installation', type=Path)
    p.add_argument('--output', type=Path, required=True)
    p.add_argument('--table', type=Path, required=True)
    p.add_argument('--detailed', action='store_true')
    a = p.parse_args()
    inspect(a.installation, a.output, a.table, a.detailed)
