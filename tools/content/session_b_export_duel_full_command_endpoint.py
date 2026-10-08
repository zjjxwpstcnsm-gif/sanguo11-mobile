#!/usr/bin/env python3
"""Import the actual full58b640 selected/model/terminal/callback endpoint.

Declared source units, AI controls and render omissions remain explicit.
"""
import argparse
import json
from pathlib import Path
from audit_pc_restoration_sources import EXE_SHA, sha


def export(source, context, output):
    if output.exists():
        raise ValueError('Preserve earlier import')
    raw = source.read_bytes()
    assert sha(raw) == '435a4af419cb6cace405af4232fd7c857169c1279d8d7af5c828cfaa02e6f6ad'
    r = json.loads(raw)
    context_raw = context.read_bytes()
    assert sha(context_raw) == r['contextSha'] == '49c1ed7a9e7453e9834ef58d08f2c8f7f906a278b5d1b43bc3e60328f5592c38'
    c = json.loads(context_raw)
    assert r['exeSha'] == EXE_SHA and r['source'] == c['source'] and r['wholeWorldAndRngRestored'] and not r['completeGoal']
    assert len(r['terminalModels']) == 1 and r['result'] == 1
    required = {'0x58b400', '0x589f70', '0x50e3f0', '0x50de30', '0x50c030', '0x505eb0', '0x4d3a50', '0x4d3340'}
    assert required <= {entry['address'] for entry in r['entries']}
    endpoint = r['terminalModels'][0]
    natives = [p['nativeId'] for case in c['cases'] for p in case['declaredCrew']]
    assert natives == [365, 116, 466, 558, 14, 517]
    assert len(bytes.fromhex(endpoint['modelHex'])) == 0x59c and len(bytes.fromhex(r['managerHex'])) == 0xd0
    lines = ['# Actual complete original58b640 numeric endpoint SHA' + sha(raw),
             'MANAGER\t' + r['managerHex'], 'MODEL\t' + endpoint['modelHex'],
             'NATIVES\t' + ','.join(map(str, natives)), 'SEED\t' + str(endpoint['rngBefore']),
             'CAPTURE\t558', 'FINAL_RNG\t' + str(r['rngAfter'])]
    output.write_text('\n'.join(lines) + '\n')
    print('PASS full original command endpoint', sha(output.read_bytes()))


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('source', type=Path)
    p.add_argument('context', type=Path)
    p.add_argument('output', type=Path)
    a = p.parse_args()
    export(a.source, a.context, a.output)
