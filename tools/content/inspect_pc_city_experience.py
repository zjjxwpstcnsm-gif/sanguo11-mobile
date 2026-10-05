#!/usr/bin/env python3
"""Pin fixed city-command XP call sites without writing the PC installation."""
import argparse
import hashlib
import json
from pathlib import Path

EXE_SHA = '30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
CALLS = [
    ('recruit', 1, '徵兵', 0x5c3b36, 0x5c3b47, 4, '魅力經驗'),
    ('production', 2, '生產', 0x5c66aa, 0x5c66bb, 2, '智力經驗'),
    ('training', 6, '訓練', 0x5c4365, 0x5c4376, 1, '武力經驗'),
    ('patrol', 3, '巡察', 0x5cbee7, 0x5cbef8, 0, '統率經驗'),
]

def inspect(exe):
    raw = exe.read_bytes()
    if hashlib.sha256(raw).hexdigest() != EXE_SHA:
        raise ValueError('Unverified EXE')
    records = []
    for name, command, label, start, end, stat, property_label in CALLS:
        block = raw[start-0x400000:end-0x400000]
        if block[:7] != bytes([0x6a, 1, 0x6a, 2, 0x6a, stat, 0x56]):
            raise ValueError('Award call arguments differ')
        records.append(dict(name=name, command_id=command, command_label=label,
                            start=hex(start), end_exclusive=hex(end), native_stat=stat,
                            experience_property_id=30+stat, experience_property_label=property_label,
                            amount=2, refresh_current=True, bytes_hex=block.hex(),
                            sha256=hashlib.sha256(block).hexdigest()))
    functions = []
    for name, start, end in [('award_experience', 0x4a70d0, 0x4a7277), ('unit_companion_multiplier', 0x4a54a0, 0x4a553a)]:
        block = raw[start-0x400000:end-0x400000]
        functions.append(dict(name=name, start=hex(start), end_exclusive=hex(end), sha256=hashlib.sha256(block).hexdigest(), bytes_hex=block.hex()))
    merit_calls=[]
    for name, start, end in [('recruit',0x5c3b47,0x5c3b54),('production',0x5c66bb,0x5c66c8),('training',0x5c4376,0x5c4383),('patrol',0x5cbf25,0x5cbf32)]:
        block=raw[start-0x400000:end-0x400000]
        if block[:9]!=bytes.fromhex('6a3256b95c899907e8'):raise ValueError('Merit call differs')
        merit_calls.append(dict(name=name,start=hex(start),end_exclusive=hex(end),amount=50,cap=60000,sha256=hashlib.sha256(block).hexdigest(),bytes_hex=block.hex()))
    shared = exe.parent/'Media/scenario/Scenario.s11'
    return dict(schema=1, source_file='san11pk.exe', executable_sha256=EXE_SHA,
                shared_file='Media/scenario/Scenario.s11', shared_sha256=hashlib.sha256(shared.read_bytes()).hexdigest(),
                calls=records, merit_calls=merit_calls, functions=functions,
                verified=dict(original_world_observations=700, original_named_command_getter='48f1a0..48f1b0',
                              original_named_property_getter='73ca80 then4c86eb..4c86f6', memory_bytes=0x300000,
                              full_world_rng_compared=True, base_values=[1,50,80,99,100],
                              experience_values=[0,98,99,100,2998,2999,3000], injury_values=[-1,0,1,2,3],
                              nonunit_9c_fixture_values=[-1,0,41,86], award_cap=3000,original_xp_then_merit_world_observations=20),
                limits=['Android normal fixed rewards were integrated in R27; source evidence and installed flow checks remain separate',
                        'Full command admission, three-actor city formulas and production item/timing coverage remain unresolved',
                        'The original multiplier searches a valid unit and its other officers for native skill84; nonunit fixture returns0. Unit/skill identity integration is not claimed',
                        'Cultivation rewards are separate and not verified by these fixed city-call blocks',
                        'No rule RNG or UI changes in this source-evidence increment'])

if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--exe', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    a = p.parse_args()
    if a.exe.parent.resolve() in a.output.resolve().parents or a.output.resolve() == a.exe.parent.resolve():
        raise ValueError('Read-only PC installation')
    a.output.write_text(json.dumps(inspect(a.exe), ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
