#!/usr/bin/env python3
"""Execute original STAT/APTITUDE/SKILL completion on controlled nonhuman actors."""
import argparse
import hashlib
import json
import struct
import sys
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--output', type=Path, required=True)
args = parser.parse_args()
source = Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版').resolve()
out = args.output.resolve()
if out == source or source in out.parents:
    raise ValueError('PC installation is read-only')
out.mkdir(parents=True, exist_ok=False)
sys.path.insert(0, str(Path(__file__).resolve().parent))
import test_pc_city_action_costs as support
from pc_original_pe_data import load_original_data
from unicorn.x86_const import UC_X86_REG_ECX

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
assert t.call(0x47a630, force) == 1
# Actual original virtual predicate: no player controller (-1), so no live UI.
u.reg_write(UC_X86_REG_ECX, force)
assert t.call(0x480fa0) == 0
u.mem_write(officer, bytes(0x180))
u.reg_write(UC_X86_REG_ECX, officer)
t.call(0x489f10)
for offset, value in [(0x9c, 0), (0xa0, 0), (0xa4, -1), (0x60, -1), (0x15c, 0)]:
    u.mem_write(officer + offset, struct.pack('<i', value))
u.mem_write(0x8ba130, struct.pack('<i', 41))
u.mem_write(0x8ba154, struct.pack('<i', 42))
u.mem_write(0x8ba16c, struct.pack('<i', 43))
for address, value in [(0x7201970, 1), (0x7201980, 0), (0x7201960, 200)]:
    u.mem_write(address, struct.pack('<I', value))
rows = [r for r in d.records if r['kind'] == 'table_86dd8']
assert [row['native_index'] for row in rows] == list(range(98))
for row in rows:
    u.reg_write(UC_X86_REG_ECX, row['actor_address'])
    t.call(0x494f50, row['native_index'])
baseline = bytes(u.mem_read(0x7200000, 0x300000))
results = []
for row in rows[:15]:
    index, actor = row['native_index'], row['actor_address']
    stat, cap = index // 3, [70, 80, 95][index % 3]
    u.reg_write(UC_X86_REG_ECX, actor)
    uses = t.call(0x494f80) & 255
    for curve, age in [(-1, 20), (0, 20), (0, 70), (4, 70)]:
        for base in [cap - 6, cap - 1]:
            for xp in [0, 99, 1499, 1999]:
                for used in [0, uses - 1]:
                    u.mem_write(0x7200000, baseline)
                    u.mem_write(officer + 0xc8, bytes([base] * 5))
                    u.mem_write(officer + 0x48, struct.pack('<i', 201 - age))
                    u.mem_write(officer + 0xd0, struct.pack('<5i', *([curve] * 5)))
                    u.mem_write(officer + 0x12a, struct.pack('<5H', *([xp] * 5)))
                    u.reg_write(UC_X86_REG_ECX, officer)
                    t.call(0x48a2d0)
                    u.reg_write(UC_X86_REG_ECX, 0x780b3cc)
                    if not t.call(0x49dc60, officer, actor):
                        continue
                    u.reg_write(UC_X86_REG_ECX, officer)
                    foundation = t.call(0x48a390, stat) & 255
                    u.reg_write(UC_X86_REG_ECX, force)
                    t.call(0x481750, index, used)
                    u.reg_write(UC_X86_REG_ECX, officer)
                    t.call(0x489bd0, 41, index, 0, 0, 0, 0)
                    u.mem_write(officer + 0x158, b'\x01')
                    expected = bytearray(u.mem_read(0x7200000, 0x300000))
                    rng = bytes(u.mem_read(0x8a5d44, 4))
                    actual = t.call(0x5d91a0, officer, index)
                    new_xp = min(3000, xp + 100 * min(5, cap - foundation))
                    offset = officer - 0x7200000
                    struct.pack_into('<H', expected, offset + 0x12a + 2 * stat, new_xp)
                    new_value = min(100, foundation + new_xp // 100 - xp // 100)
                    expected[offset + 0x170 + stat] = new_value
                    expected[offset + 0x175 + stat] = new_value
                    expected[force + 0xb8 + index - 0x7200000] = used + 1
                    expected[offset + 0x13c:offset + 0x154] = struct.pack('<6i', -1, 0, 0, 0, 0, 0)
                    expected[offset + 0x158] = 0
                    assert actual == 1
                    assert bytes(expected) == bytes(u.mem_read(0x7200000, 0x300000))
                    assert rng == bytes(u.mem_read(0x8a5d44, 4))
                    results.append(dict(native_index=index, curve=curve, age=age, base=base,
                                        before_xp=xp, after_xp=new_xp, foundation=foundation,
                                        before_used=used, after_used=used + 1, task_released=True,
                                        field158=0, base_unchanged=True))
aptitude_admission, aptitude_completion = [], []
for row in rows[15:27]:
    index, actor = row['native_index'], row['actor_address']
    u.reg_write(UC_X86_REG_ECX, actor)
    aptitude = t.call(0x494ff0)
    u.reg_write(UC_X86_REG_ECX, actor)
    rank = t.call(0x495010)
    u.reg_write(UC_X86_REG_ECX, actor)
    uses = t.call(0x494f80) & 255
    assert aptitude == (index - 15) // 2 and rank == 1 + (index - 15) % 2
    for old in range(4):
        u.mem_write(0x7200000, baseline)
        u.mem_write(officer + 0xb0 + 4 * aptitude, struct.pack('<I', old))
        before = bytes(u.mem_read(0x7200000, 0x300000))
        rng = bytes(u.mem_read(0x8a5d44, 4))
        u.reg_write(UC_X86_REG_ECX, 0x780b3cc)
        allowed = t.call(0x49dc60, officer, actor)
        assert allowed == int(old == rank - 1)
        assert before == bytes(u.mem_read(0x7200000, 0x300000))
        assert rng == bytes(u.mem_read(0x8a5d44, 4))
        aptitude_admission.append(dict(native_index=index, aptitude=aptitude, rank=rank, before=old, allowed=bool(allowed)))
        if not allowed:
            continue
        for used in [0, uses - 1]:
            u.mem_write(0x7200000, baseline)
            u.mem_write(officer + 0xb0 + 4 * aptitude, struct.pack('<I', old))
            u.reg_write(UC_X86_REG_ECX, force)
            t.call(0x481750, index, used)
            u.reg_write(UC_X86_REG_ECX, officer)
            t.call(0x489bd0, 42, index, 0, 0, 0, 0)
            u.mem_write(officer + 0x158, b'\x01')
            expected = bytearray(u.mem_read(0x7200000, 0x300000))
            assert t.call(0x5da0c0, officer, index) == 1
            offset = officer - 0x7200000
            struct.pack_into('<I', expected, offset + 0xb0 + 4 * aptitude, rank)
            expected[force + 0xb8 + index - 0x7200000] = used + 1
            expected[offset + 0x13c:offset + 0x154] = struct.pack('<6i', -1, 0, 0, 0, 0, 0)
            expected[offset + 0x158] = 0
            assert bytes(expected) == bytes(u.mem_read(0x7200000, 0x300000))
            assert rng == bytes(u.mem_read(0x8a5d44, 4))
            aptitude_completion.append(dict(native_index=index, aptitude=aptitude, before=old, after=rank,
                                            before_used=used, after_used=used + 1, task_released=True, field158=0))
skill_admission, skill_completion = [], []
for row in rows[27:]:
    index, actor = row['native_index'], row['actor_address']
    skill = struct.unpack('<i', u.mem_read(actor + 0x58, 4))[0]
    finite = struct.unpack('<i', u.mem_read(actor + 0x44, 4))[0] != 0
    u.reg_write(UC_X86_REG_ECX, actor)
    uses = t.call(0x494f80) & 255
    assert 0 <= skill <= 99
    for old in [-1, skill, (skill + 1) % 100]:
        u.mem_write(0x7200000, baseline)
        u.mem_write(officer + 0xe8, struct.pack('<i', old))
        before = bytes(u.mem_read(0x7200000, 0x300000))
        rng = bytes(u.mem_read(0x8a5d44, 4))
        u.reg_write(UC_X86_REG_ECX, 0x780b3cc)
        allowed = t.call(0x49dc60, officer, actor)
        assert allowed == int(old != skill)
        assert before == bytes(u.mem_read(0x7200000, 0x300000))
        assert rng == bytes(u.mem_read(0x8a5d44, 4))
        skill_admission.append(dict(native_index=index, before=old, target=skill, allowed=bool(allowed)))
        if not allowed:
            continue
        for used in sorted({0, uses - 1} if finite else {0}):
            u.mem_write(0x7200000, baseline)
            u.mem_write(officer + 0xe8, struct.pack('<i', old))
            # Explicitly select this source research ID in hidden slot0; this
            # validates original counter addressing, not random hidden selection.
            if index >= 48:
                u.mem_write(force + 0xe8, struct.pack('<10i', index, *([-1] * 9)))
            u.reg_write(UC_X86_REG_ECX, force)
            t.call(0x481750, index, used)
            u.reg_write(UC_X86_REG_ECX, officer)
            t.call(0x489bd0, 43, index, 0, 0, 0, 0)
            u.mem_write(officer + 0x158, b'\x01')
            expected = bytearray(u.mem_read(0x7200000, 0x300000))
            assert t.call(0x5da9e0, officer, index) == 1
            offset = officer - 0x7200000
            struct.pack_into('<i', expected, offset + 0xe8, skill)
            count_field = force + (0xb8 + index if index < 48 else 0x114)
            expected[count_field - 0x7200000] = used + int(finite)
            expected[offset + 0x13c:offset + 0x154] = struct.pack('<6i', -1, 0, 0, 0, 0, 0)
            expected[offset + 0x158] = 0
            assert bytes(expected) == bytes(u.mem_read(0x7200000, 0x300000))
            assert rng == bytes(u.mem_read(0x8a5d44, 4))
            skill_completion.append(dict(native_index=index, before=old, after=skill, finite=finite,
                                         before_used=used, after_used=used + int(finite),
                                         hidden_slot=0 if index >= 48 else None, task_released=True, field158=0))
failures = []
for index, task in [(-1, 41), (98, 41), (0, 40), (0, -1)]:
    u.mem_write(0x7200000, baseline)
    u.reg_write(UC_X86_REG_ECX, officer)
    t.call(0x489bd0, task & 0xffffffff, 0, 0, 0, 0, 0)
    before = bytes(u.mem_read(0x7200000, 0x300000))
    rng = bytes(u.mem_read(0x8a5d44, 4))
    assert t.call(0x5d91a0, officer, index & 0xffffffff) == 0
    assert before == bytes(u.mem_read(0x7200000, 0x300000))
    assert rng == bytes(u.mem_read(0x8a5d44, 4))
    failures.append(dict(native_index=index, task=task, returned=0, world_rng_unchanged=True))
assert (len(results), len(failures), len(aptitude_admission), len(aptitude_completion),
        len(skill_admission), len(skill_completion)) == (480, 4, 48, 24, 213, 276)
report = dict(exe_sha256=hashlib.sha256(raw).hexdigest(), shared_sha256=hashlib.sha256(shared).hexdigest(),
              entry='0x5d91a0', successful_cases=results, rejected_cases=failures,
              aptitude_entry='0x5da0c0', aptitude_admission=aptitude_admission, aptitude_completion=aptitude_completion,
              skill_entry='0x5da9e0', skill_admission=skill_admission, skill_completion=skill_completion,
              source_record_sha256=[r['sha256'] for r in rows], original_data_mappings=mappings,
              full_3mib_mutation_guard=True, rng_unchanged=True, original_game_functions_unmodified=True,
              host_import_emulation={'pointer_probes':'IsBadReadPtr/IsBadWritePtr resolved against VM memory permissions',
                                     'critical_sections':'isolated single-thread VM; Enter/LeaveCriticalSection imports',
                                     'file_input':'original serializer plus source-byte reader during initial decode only'},
              explicit_inputs={'force_controller_plus60':-1, 'original_completion_selector_8ba130':41,
                               'original_completion_selector_8ba154':42, 'original_completion_selector_8ba16c':43,
                               'hidden_selection':'Each tested hidden research explicitly selected in slot0; other nine IDs=-1'},
              functions=[dict(start=hex(start), end=hex(end), bytes_hex=raw[start-0x400000:end-0x400000].hex(),
                              sha256=hashlib.sha256(raw[start-0x400000:end-0x400000]).hexdigest())
                         for start, end in [(0x5d91a0,0x5d9422),(0x5da0c0,0x5da320),(0x5da9e0,0x5dac19),(0x481750,0x4817a9),(0x4819e0,0x481a37),
                                            (0x489bd0,0x489c0f),(0x4a5780,0x4a57a9),(0x4a5660,0x4a5682)]],
              limits=['Actual complete nonhuman STAT/APTITUDE/SKILL completion functions; construction and selected preconditions are controlled inputs',
                      'No full PC boot, complete turn dispatcher or live human presentation; fees, duration and hidden research selection remain unresolved',
                      'No Android rules, cultivation base policy, saves or PC files changed'])
(out/'native-completion.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
print('PASS original complete STAT=',len(results),' rejected=',len(failures),' APT admission=',len(aptitude_admission),
      ' completion=',len(aptitude_completion),' SKILL admission=',len(skill_admission),' completion=',len(skill_completion),
      '; full world/base/RNG guards')
