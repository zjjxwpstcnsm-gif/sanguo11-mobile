#!/usr/bin/env python3
"""Pin the actual198-frame original solo-ruler terminal; no result editing."""
import argparse, json, struct
from pathlib import Path
from audit_pc_restoration_sources import EXE_SHA, sha

def pack(source, output):
    if output.exists():
        raise ValueError('Preserve existing import')
    raw = source.read_bytes()
    assert sha(raw) == '12e70314e57c8e4c2fe6a7afada1a8e4e62e6bd971c58237f0b9bd12334ddddb'
    r = json.loads(raw)
    e = r['selected']
    assert r['exeSha'] == EXE_SHA and r['wholeWorldAndRngRestored'] and not r['completeGoal']
    assert r['natives'] == [365, 517] and e['seed'] == 1 and e['frames'] == 198
    assert e['outcomes'] == [0, 0, 0, 2, 0, 0] and e['deadNative'] == 517
    assert e['terminalRng'] == e['rngAfter'] == 773516763
    assert list(struct.unpack_from('<6i', bytes.fromhex(e['managerHex']), 0x64)) == e['outcomes']
    lines = ['# Actual original natural ruler terminal source SHA ' + sha(raw),
             'MODEL\t' + e['modelHex'], 'MANAGER\t' + e['managerHex'],
             'NATIVES\t365,-1,-1,517,-1,-1', 'OPTIONS\t0,2,0',
             'SEED\t773516763', 'FINAL_RNG\t773516763', 'DEATH\t517']
    output.write_text('\n'.join(lines) + '\n')
    print('PASS pinned natural-ruler import', sha(output.read_bytes()))

if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('source', type=Path)
    p.add_argument('output', type=Path)
    a = p.parse_args()
    pack(a.source, a.output)
