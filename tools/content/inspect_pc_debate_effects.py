#!/usr/bin/env python3
"""Read-only original legality and immediate model effects; no UI callback claim."""
import argparse, gzip, itertools, json, struct
from pathlib import Path
from inspect_pc_debate_flow import NativeDebateFlow
from audit_pc_restoration_sources import EXE_SHA, output_guard, json_bytes, sha

def inspect(installation, output):
    installation = installation.resolve()
    output_guard(installation, output)
    d = NativeDebateFlow(installation, (installation / 'san11pk.exe').read_bytes())
    w, model = d.world, d.fixture
    def reset(temper=(0, 0), fury=(0, 0), available=1, hp=(1000, 1000), anger=(0, 0)):
        w.u.mem_write(model, bytes(0x1b0))
        for side in range(2):
            base = model + 0x10 + side * 0xa0
            for offset, value in [(4, hp[side]), (8, anger[side]), (12, fury[side]), (0x9c, temper[side])]:
                w.u.mem_write(base + offset, struct.pack('<i', value))
            w.u.mem_write(model + 0x184 + side * 4, struct.pack('<i', available))
        w.u.mem_write(0x8a5d44, struct.pack('<I', 23))
    def result():
        raw = bytes(w.u.mem_read(model, 0x1b0))
        return [struct.unpack_from('<i', raw, 0x10 + side * 0xa0 + offset)[0]
                for side in range(2) for offset in (4, 8, 12)]
    legality = []
    for lt, rt, lf, rf, available, card in itertools.product(range(4), range(4), range(2), range(2), range(2), range(-1, 16)):
        reset((lt, rt), (lf, rf), available)
        before = bytes(w.u.mem_read(model, 0x1b0))
        accepted = w.call(0x51e3c0, 0, card & 0xffffffff, receiver=model)
        if before != bytes(w.u.mem_read(model, 0x1b0)) or struct.unpack('<I', w.u.mem_read(0x8a5d44, 4))[0] != 23:
            raise ValueError('Original legality mutated state or RNG')
        legality.append([lt, rt, lf, rf, available, card, accepted])
    effects = []
    functions = {'tie': 0x51eb30, 'shout': 0x51eb70, 'ordinary': 0x51ebd0,
                 'ignore': 0x51ec90, 'calm': 0x51ecd0, 'rage': 0x51ed30}
    for side, health, anger, amount, reflected in itertools.product(range(2), (-99, 0, 999, 1000), (0, 1, 50, 99, 100), (-150, -30, 0, 40, 150), range(2)):
        for name, address in functions.items():
            reset(hp=(health, 1000-health), anger=(anger, 100-anger))
            args = {'tie': (), 'shout': (side, amount, 15), 'ordinary': (side, 9, amount, 20, reflected),
                    'ignore': (side, amount), 'calm': (side, amount, reflected), 'rage': (side, amount, reflected)}[name]
            w.call(address, *(value & 0xffffffff for value in args), receiver=model)
            rng = struct.unpack('<I', w.u.mem_read(0x8a5d44, 4))[0]
            if rng != 23: raise ValueError('Unexpected immediate-effect RNG consumption')
            effects.append([name, side, health, anger, amount, reflected, *result(), rng])
    ordinary_anger = [[card, w.call(0x51eaf0, card & 0xffffffff)] for card in range(-1, 16)]
    report = dict(schema=1, sourceExecutableSha256=EXE_SHA, legality=legality,
                  effects=effects, ordinaryAnger=ordinary_anger,
                  functions={name: hex(address) for name, address in functions.items()},
                  limits=['Explicit synthetic model inputs; original optional UI pointers remain zero',
                          'No fury counter, deck, UI queue, AI, campaign settlement or normal PC start certification'])
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(gzip.compress(json_bytes(report), mtime=0))
    print(json.dumps(dict(legality=len(legality), effects=len(effects), sha256=sha(output.read_bytes()))))
    return report

if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('installation', type=Path)
    p.add_argument('--output', required=True, type=Path)
    a = p.parse_args()
    inspect(a.installation, a.output)
