#!/usr/bin/env python3
"""Run original global officer countdown and readiness/dispatch for all98 source research IDs."""
import argparse
import gzip
import hashlib
import json
import struct
import sys
from pathlib import Path

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--output', required=True, type=Path)
a = p.parse_args()
source = Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版').resolve()
out = a.output.resolve()
if out == source or source in out.parents:
    raise ValueError('PC directory is read-only')
out.mkdir(parents=True, exist_ok=False)
sys.path.insert(0, str(Path(__file__).resolve().parent))
import test_pc_city_action_costs as support
from pc_original_pe_data import load_original_data
from unicorn.x86_const import UC_X86_REG_ECX, UC_X86_REG_ESP, UC_X86_REG_EIP, UC_X86_REG_ESI

support.CityActionCostsTest.setUpClass()
t, d = support.CityActionCostsTest(), support.CityActionCostsTest.d
u = d.u
mappings = load_original_data(u)
raw = (source / 'san11pk.exe').read_bytes()
shared = (source / 'Media/scenario/Scenario.s11').read_bytes()
assert hashlib.sha256(raw).hexdigest() == '30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
assert hashlib.sha256(shared).hexdigest() == 'dfca0316e019454e77bab5805e3d725ae35e34408e1699e869e5419e2f5fad6f'
t.actors()
force, officer = d.root + 0x7af8, d.root + 0xc0bc
u.mem_write(force, bytes(0x12c))
u.reg_write(UC_X86_REG_ECX, force)
t.call(0x481830)
u.mem_write(force + 4, struct.pack('<i', 0))
u.mem_write(force + 0x60, struct.pack('<i', -1))
u.mem_write(officer, bytes(0x190))
u.reg_write(UC_X86_REG_ECX, officer)
t.call(0x489f10)
for offset, value in [(0x94, 0), (0x9c, 0), (0xa0, 0), (0xa4, -1), (0x60, -1), (0x15c, 0)]:
    u.mem_write(officer + offset, struct.pack('<i', value))
u.mem_write(officer + 0xc8, bytes([50] * 5))
u.mem_write(officer + 0xd0, struct.pack('<5i', *([-1] * 5)))
u.mem_write(officer + 0xe8, struct.pack('<i', -1))
u.reg_write(UC_X86_REG_ECX, officer)
t.call(0x48a2d0)
for address, task in [(0x8ba130, 41), (0x8ba154, 42), (0x8ba16c, 43)]:
    u.mem_write(address, struct.pack('<i', task))
rows = [r for r in d.records if r['kind'] == 'table_86dd8']
assert [r['native_index'] for r in rows] == list(range(98))
for row in rows:
    u.reg_write(UC_X86_REG_ECX, row['actor_address'])
    t.call(0x494f50, row['native_index'])
# The original dispatcher table establishes41/42/43, independent of the
# explicitly populated completion-selector globals.
branches = []
for task, wanted in [(41, 0x5ba1f1), (42, 0x5ba21f), (43, 0x5ba24d)]:
    ordinal = u.mem_read(0x5ba2e8 + task - 2, 1)[0]
    destination = struct.unpack('<I', u.mem_read(0x5ba298 + 4 * ordinal, 4))[0]
    assert destination == wanted
    branches.append(dict(task=task, ordinal=ordinal, branch=hex(destination)))
baseline = bytes(u.mem_read(0x7200000, 0x300000))
results = []
for row in rows:
    index, actor = row['native_index'], row['actor_address']
    u.mem_write(0x7200000, baseline)
    category = struct.unpack('<i', u.mem_read(actor + 0x48, 4))[0]
    assert category in [0, 1, 2]
    task = [41, 42, 43][category]
    if index >= 48:
        u.mem_write(force + 0xe8, struct.pack('<10i', index, *([-1] * 9)))
    if category == 1:
        u.reg_write(UC_X86_REG_ECX, actor)
        aptitude = t.call(0x494ff0)
        u.reg_write(UC_X86_REG_ECX, actor)
        rank = t.call(0x495010)
        u.mem_write(officer + 0xb0 + 4 * aptitude, struct.pack('<I', rank - 1))
    u.reg_write(UC_X86_REG_ECX, 0x780b3cc)
    assert t.call(0x49dc60, officer, actor) == 1
    u.reg_write(UC_X86_REG_ECX, officer)
    t.call(0x489bd0, task, index, 0, 0, 0, 0)
    u.mem_write(officer + 0x158, b'\x03')
    expected = bytearray(u.mem_read(0x7200000, 0x300000))
    rng = bytes(u.mem_read(0x8a5d44, 4))
    offset = officer - 0x7200000
    cycles = []
    for cycle in range(3):
        # Actual tail loop processes all1100 native officers, using its real
        # registry getter and validity checks. No single-officer replacement.
        u.reg_write(UC_X86_REG_ESP, d.stack)
        u.emu_start(0x59a862, 0x59a89d, count=1000000)
        assert u.reg_read(UC_X86_REG_EIP) == 0x59a89d
        expected[offset + 0x158] = 2 - cycle
        assert bytes(expected) == bytes(u.mem_read(0x7200000, 0x300000))
        # Execute the actual readiness condition and original5b9e10 dispatcher.
        u.reg_write(UC_X86_REG_ESI, officer)
        u.reg_write(UC_X86_REG_ESP, d.stack)
        u.emu_start(0x599ff7, 0x59a00c, count=1000000)
        assert u.reg_read(UC_X86_REG_EIP) == 0x59a00c
        if cycle == 2:
            if category == 0:
                stat = index // 3
                struct.pack_into('<H', expected, offset + 0x12a + 2 * stat, 500)
                expected[offset + 0x170 + stat] = 55
                expected[offset + 0x175 + stat] = 55
            elif category == 1:
                struct.pack_into('<I', expected, offset + 0xb0 + 4 * aptitude, rank)
            else:
                skill = struct.unpack('<i', u.mem_read(actor + 0x58, 4))[0]
                struct.pack_into('<i', expected, offset + 0xe8, skill)
            finite = struct.unpack('<i', u.mem_read(actor + 0x44, 4))[0] != 0
            if finite:
                count_address = force + (0xb8 + index if index < 48 else 0x114)
                expected[count_address - 0x7200000] = 1
            expected[offset + 0x13c:offset + 0x154] = struct.pack('<6i', -1, 0, 0, 0, 0, 0)
        assert bytes(expected) == bytes(u.mem_read(0x7200000, 0x300000))
        assert rng == bytes(u.mem_read(0x8a5d44, 4))
        cycles.append(dict(global_tick=cycle + 1, remaining=2 - cycle, completed=cycle == 2))
    # A later readiness scan cannot replay an already released task.
    u.reg_write(UC_X86_REG_ESI, officer)
    u.reg_write(UC_X86_REG_ESP, d.stack)
    u.emu_start(0x599ff7, 0x59a00c, count=1000000)
    assert u.reg_read(UC_X86_REG_EIP) == 0x59a00c
    assert bytes(expected) == bytes(u.mem_read(0x7200000, 0x300000))
    assert rng == bytes(u.mem_read(0x8a5d44, 4))
    results.append(dict(native_index=index, category=category, task=task, source_record_sha256=row['sha256'],
                        cycles=cycles, released_task_does_not_replay=True))
assert len(results) == 98 and sum(len(r['cycles']) for r in results) == 294
report = dict(exe_sha256=hashlib.sha256(raw).hexdigest(), shared_sha256=hashlib.sha256(shared).hexdigest(),
              results=results, original_dispatch_branches=branches, full_3mib_mutation_checked=True, rng_unchanged=True,
              original_data_mappings=mappings,
              explicit_inputs={'officer_stride':'0x190', 'officer_district_plus94':0, 'force_controller_plus60':-1,
                               'initial_counter':3, 'hidden_research':'explicitly chosen slot0, not random selection'},
              functions=[dict(start=hex(start),end=hex(end),sha256=hashlib.sha256(raw[start-0x400000:end-0x400000]).hexdigest(),
                              bytes_hex=raw[start-0x400000:end-0x400000].hex())
                         for start,end in [(0x59a862,0x59a89d),(0x599ff7,0x59a00c),(0x5b9e10,0x5ba292),(0x4883b0,0x4883b7)]],
              limits=['Original countdown tail, readiness gate and complete task dispatcher executed; preceding global-turn logic is not run',
                      'Counter starts at3 from controlled task setup; whole start command, live human UI, fees, cancellation and defeat remain separate',
                      'No effective official/MOD opening identity or complete PC boot claim; OS-import emulation follows existing native decoder',
                      'No Android rules, base-cultivation policy, saves or source PC files changed'])
encoded=(json.dumps(report, ensure_ascii=False, indent=2)+'\n').encode('utf-8')
(out/'native-countdown-dispatch.json').write_bytes(encoded)
with (out/'native-countdown-dispatch.json.gz').open('wb') as file:
    with gzip.GzipFile(filename='',mode='wb',fileobj=file,mtime=0) as packed:
        packed.write(encoded)
print('PASS original98 research countdown/readiness/dispatcher,294 ticks; full world/base/reward/RNG checks')
