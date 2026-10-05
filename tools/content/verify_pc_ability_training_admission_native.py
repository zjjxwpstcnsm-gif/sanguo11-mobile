#!/usr/bin/env python3
"""Execute original stat-training admission; controlled actors, no PC startup claim."""
import argparse
import hashlib
import json
import struct
import sys
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--output', required=True, type=Path)
args = parser.parse_args()
source = Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版').resolve()
out = args.output.resolve()
if out == source or source in out.parents:
    raise ValueError('PC directory is read-only')
out.mkdir(parents=True, exist_ok=False)
sys.path.insert(0, str(Path(__file__).resolve().parent))
import test_pc_city_action_costs as support
from pc_original_pe_data import load_original_data
from unicorn import UC_HOOK_MEM_WRITE
from unicorn.x86_const import UC_X86_REG_ECX, UC_X86_REG_EBX, UC_X86_REG_EDI, UC_X86_REG_ESP, UC_X86_REG_EIP

support.CityActionCostsTest.setUpClass()
t = support.CityActionCostsTest()
d, u = t.d, t.d.u
mappings = load_original_data(u)
raw = (source / 'san11pk.exe').read_bytes()
shared = (source / 'Media/scenario/Scenario.s11').read_bytes()
assert hashlib.sha256(shared).hexdigest() == 'dfca0316e019454e77bab5805e3d725ae35e34408e1699e869e5419e2f5fad6f'
exe_sha = hashlib.sha256(raw).hexdigest()
assert exe_sha == '30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
officer = d.root + 0xc0bc
u.mem_write(officer, bytes(0x180))
u.reg_write(UC_X86_REG_ECX, officer)
t.call(0x489f10)
for offset, value in [(0xa0, 0), (0xa4, -1), (0x60, -1)]:
    u.mem_write(officer + offset, struct.pack('<i', value))
for address, value in [(0x7201970, 1), (0x7201980, 0), (0x7201960, 200)]:
    u.mem_write(address, struct.pack('<I', value))
assert t.call(0x47a630, officer) == 1
rows = [r for r in d.records if r['kind'] == 'table_86dd8'][:15]
writes = []
capture = False

def observe(machine, access, address, size, value, user):
    if capture and not d.stack - 0x10000 <= address < d.stack + 0x10000:
        writes.append((address, size))

u.hook_add(UC_HOOK_MEM_WRITE, observe)
results = []
awards = []
for row in rows:
    actor, index = row['actor_address'], row['native_index']
    u.reg_write(UC_X86_REG_ECX, actor)
    t.call(0x494f50, index)
    assert t.call(0x47a630, actor) == 1
    u.reg_write(UC_X86_REG_ECX, actor)
    stat = t.call(0x494fb0)
    u.reg_write(UC_X86_REG_ECX, actor)
    cap = t.call(0x494fd0)
    assert stat == index // 3 and cap == [70, 80, 95][index % 3]
    for curve, age in [(0, 20), (0, 70), (4, 20), (8, 70)]:
        u.mem_write(officer + 0x48, struct.pack('<i', 201 - age))
        u.mem_write(officer + 0xd0, struct.pack('<5i', *([curve] * 5)))
        for base in [cap - 1, cap, cap + 1]:
            u.mem_write(officer + 0xc8, bytes([base] * 5))
            for xp in [0, 99, 100, 1999, 2000, 2100]:
                u.mem_write(officer + 0x12a, struct.pack('<5H', *([xp] * 5)))
                before = bytes(u.mem_read(0x7200000, 0x300000))
                rng = bytes(u.mem_read(0x8a5d44, 4))
                writes.clear()
                capture = True
                u.reg_write(UC_X86_REG_ECX, officer)
                native_xp = t.call(0x489180, stat)
                u.reg_write(UC_X86_REG_ECX, officer)
                foundation = t.call(0x48a390, stat)
                u.reg_write(UC_X86_REG_ECX, 0x780b3cc)
                actual = t.call(0x49dc60, officer, actor) & 255
                capture = False
                assert native_xp == xp
                assert actual == int(xp < 2000 and foundation < cap)
                assert not writes and before == bytes(u.mem_read(0x7200000, 0x300000))
                assert rng == bytes(u.mem_read(0x8a5d44, 4))
                results.append(dict(native_index=index, stat=stat, cap=cap, curve=curve,
                                    age=age, base=base, xp=xp, foundation=foundation, allowed=bool(actual)))
                if actual:
                    # Execute the exact original completion award block through its
                    # original XP setter, stopping before usage counters/task/UI.
                    u.reg_write(UC_X86_REG_ECX, officer)
                    t.call(0x48a2d0)
                    expected = bytearray(u.mem_read(0x7200000, 0x300000))
                    u.reg_write(UC_X86_REG_EBX, officer)
                    u.reg_write(UC_X86_REG_EDI, actor)
                    u.reg_write(UC_X86_REG_ESP, d.stack)
                    u.emu_start(0x5d9268, 0x5d92ca, count=10000)
                    assert u.reg_read(UC_X86_REG_EIP) == 0x5d92ca
                    new_xp = min(3000, xp + 100 * min(5, cap - foundation))
                    offset = officer - 0x7200000
                    struct.pack_into('<H', expected, offset + 0x12a + 2 * stat, new_xp)
                    new_foundation = min(100, foundation + new_xp // 100 - xp // 100)
                    expected[offset + 0x170 + stat] = new_foundation
                    expected[offset + 0x175 + stat] = new_foundation
                    assert bytes(expected) == bytes(u.mem_read(0x7200000, 0x300000))
                    assert rng == bytes(u.mem_read(0x8a5d44, 4))
                    assert bytes(u.mem_read(officer + 0xc8, 5)) == bytes([base] * 5)
                    awards.append(dict(native_index=index, curve=curve, age=age, base=base,
                                       before_xp=xp, after_xp=new_xp, foundation=foundation,
                                       after_foundation=new_foundation, base_unchanged=True))
assert len(results) == 1080
report = dict(exe_sha256=exe_sha, shared_sha256=hashlib.sha256(shared).hexdigest(),
              admission_entry='0x49dc60', source_record_hashes=[r['sha256'] for r in rows],
              cases=results, full_world_rng_unchanged=True, nonstack_writes=0,
              completion_award_block=['0x5d9268', '0x5d92ca'], completion_awards=awards,
              completion_award_semantics='XP +=100*min(5, cap-foundation), capped3000; base unchanged; only XP/current caches change',
              original_data_mappings=mappings,
              limits=['Controlled original constructor actors and explicit date/curve inputs; no full PC boot or effective MOD identity claim',
                      'STAT completion award block verified; full completion dispatch, usage counters, task release, costs, duration and hidden selection remain unresolved',
                      'No Android rule or old save changed; user-requested cultivation writes base policy is preserved; native XP semantics need explicit versioned policy reconciliation'])
report['original_functions'] = [dict(start=hex(start), end=hex(end),
    sha256=hashlib.sha256(raw[start - 0x400000:end - 0x400000]).hexdigest(),
    bytes_hex=raw[start - 0x400000:end - 0x400000].hex())
    for start, end in [(0x49dc60, 0x49dd27), (0x489180, 0x48919e),
                       (0x5d91a0, 0x5d9422), (0x4a55a0, 0x4a55fa), (0x48a810, 0x48a854)]]
(out / 'native-admission.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
print('PASS original STAT admission1080 cases; completion XP awards=',len(awards),'; base/RNG unchanged and complete world mutation checked')
