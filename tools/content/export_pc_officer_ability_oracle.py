#!/usr/bin/env python3
"""Export original x86 age/current-ability results for independent Java checks.

All expected values come from original functions. A native memory-write hook
rejects every non-stack write, including transient state/RNG writes. Host writes
only prepare explicit fixtures; no original arithmetic or getter is replaced.
"""
import argparse
import csv
import io
import json
import struct
from pathlib import Path
from unicorn import UC_HOOK_MEM_WRITE
from unicorn.x86_const import UC_X86_REG_ECX
from inspect_pc_scenario_tail import NativeTailDecoder
from inspect_pc_scenario_officers import sha
from test_pc_city_action_costs import CityActionCostsTest


def export(exe, shared, output, audit):
    for p in (output, audit):
        if p.resolve() == exe.parent.resolve() or exe.parent.resolve() in p.resolve().parents:
            raise ValueError('PC installation is read-only')
    raw, shared_raw = exe.read_bytes(), shared.read_bytes()
    t = CityActionCostsTest()
    t.d = d = NativeTailDecoder(raw)
    d.decode_tail(shared_raw, True)
    u = d.u
    origin = d.root + 0xc0bc
    rank = d.root + 0x7d9dc
    for index in (0, 1, 699, 700, 799, 800, 1099):
        officer = origin + index * 0x190
        u.reg_write(UC_X86_REG_ECX, officer)
        t.call(0x489f10)
        u.reg_write(UC_X86_REG_ECX, officer)
        t.call(0x488470, 0)
        u.mem_write(officer + 0xa0, struct.pack('<i', 0))
    if t.call(0x47a630, rank) != 1 or t.call(0x47a630, origin + 0x190) != 1:
        raise ValueError('Original rank/spouse fixtures are invalid')
    u.mem_write(0x7201970, struct.pack('<I', 1))
    u.mem_write(0x7201960, struct.pack('<I', 200))
    u.mem_write(0x8a5d44, struct.pack('<I', 42))

    def write_guard(machine, access, address, size, value, user):
        if not d.stack-0x10000 <= address or address+size > d.stack+0x10000:
            raise ValueError('Pure ability calculation wrote non-stack memory: '+hex(address))

    hook = u.hook_add(UC_HOOK_MEM_WRITE, write_guard)
    rows = []
    functions = (0x488df0,0x488e10,0x488e50,0x488e90,0x488ec0,
                 0x488f00,0x488f40,0x488f60,0x488f80)
    try:
        for curve, function in enumerate(functions):
            for age in list(range(121))+[-2147483648,-1000,-1,121,1000,2147483647]:
                value = t.call(function, age & 0xffffffff)
                if value >= 2**31:
                    value -= 2**32
                rows.append(['curve',curve,age]+[0]*11+[value])

        def ability(index, base, curve, age, xp, stat, injury, rank_stat, bonus,
                    own_skill, spouse, spouse_skill, bypass):
            officer = origin + index * 0x190
            u.mem_write(officer+0xc8, bytes([base]*5))
            u.mem_write(officer+0xd0, struct.pack('<5i', *([curve]*5)))
            u.mem_write(officer+0x48, struct.pack('<i', 201-age))
            u.mem_write(officer+0x12a, struct.pack('<5H', *([xp]*5)))
            u.mem_write(officer+0xa4, struct.pack('<i', 0 if rank_stat >= 0 else -1))
            u.mem_write(officer+0xe8, struct.pack('<i', own_skill))
            u.mem_write(officer+0x60, struct.pack('<i', 1 if spouse else -1))
            u.mem_write(origin+0x190+0xe8, struct.pack('<i', spouse_skill))
            u.mem_write(rank+0x30, struct.pack('<i', rank_stat))
            u.mem_write(rank+0x34, bytes([bonus]))
            u.mem_write(0x7201980, struct.pack('<I', bypass))
            u.reg_write(UC_X86_REG_ECX, officer)
            value = t.call(0x48a110, stat, injury & 0xffffffff) & 255
            rows.append(['ability',index,base,curve,age,xp,stat,injury,rank_stat,
                         bonus,own_skill,spouse,spouse_skill,bypass,value])

        for curve in range(-1, 9):
            for age in (0,17,18,25,26,30,31,35,39,40,44,45,49,50,54,55,80,120):
                for stat in range(5):
                    ability(0, (1,50,80,99,100)[stat], curve, age,
                            (0,95,100,2995,3000)[age%5], stat, stat-1,
                            stat, 3, 99 if stat%2 else 0, 1, 0, 0)
        for stat in range(5):
            for injury in (-2,-1,0,1,2,3,4):
                for rank_stat, bonus in ((-1,0),(stat,5),((stat+1)%5,5),(stat,255)):
                    for own, spouse, other in ((0,0,0),(99,0,0),(99,1,0),(0,1,99),(99,1,99),(0,1,0)):
                        for base, xp in ((1,0),(80,100),(100,3000)):
                            ability(0,base,4,18,xp,stat,injury,rank_stat,bonus,own,spouse,other,0)
        for index in (699,700,799,800,1099):
            for base in (0,1,80,100,255):
                for bypass in (0,1):
                    for stat in range(5):
                        ability(index,base,8,55,3000,stat,3,stat,10,99,1,99,bypass)
        if bytes(u.mem_read(0x8a5d44,4)) != struct.pack('<I',42):
            raise ValueError('RNG state changed')
    finally:
        u.hook_del(hook)
    buf = io.StringIO()
    writer = csv.writer(buf, delimiter='\t', lineterminator='\n')
    writer.writerow(['kind']+['v'+str(i) for i in range(13)]+['result'])
    writer.writerows(rows)
    data = buf.getvalue().encode()
    output.write_bytes(data)
    report = dict(schema=1, executable_sha256=sha(raw), shared_sha256=sha(shared_raw),
                  output_sha256=sha(data), rows=len(rows),
                  counts={kind:sum(r[0]==kind for r in rows) for kind in ('curve','ability')},
                  input_columns=['native_index','base','curve','age','experience','stat','injury',
                                 'rank_stat','rank_bonus','own_skill','spouse_present','spouse_skill','curve_bypass'],
                  curves=[hex(a) for a in functions], current_ability='48a110',
                  mutation_check='Every emulated non-stack write forbidden; RNG42 unchanged',
                  limits=['Controlled age/date-source flags; original UI settings and date policy not mapped',
                          'Explicit rank/skill/spouse fixtures, not inferred project officer state',
                          'Special native indices700..799 are source identities, never project IDs',
                          'No gameplay persistence or content import is performed'])
    audit.write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps(report))


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    for name in ('exe','shared','output','audit'):
        p.add_argument('--'+name, type=Path, required=True)
    a = p.parse_args()
    export(a.exe,a.shared,a.output,a.audit)
