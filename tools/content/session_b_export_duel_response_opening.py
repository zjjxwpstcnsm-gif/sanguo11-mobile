#!/usr/bin/env python3
"""Pinned original response estimate facts/chance/result/RNG fixture conversion."""
import argparse,json,hashlib
from pathlib import Path
def export(source,output):
    if output.exists():raise ValueError('Preserve import')
    raw=source.read_bytes();assert hashlib.sha256(raw).hexdigest()in ['8a7fc2b13b4d20d51e0395fb2ff4967ae0483a3ca94fcf23198e2f2ad16aa302','3aa1a1650fb9f54595168ec0760366bc2222734474e78304a6fbb3573ce8861e'];r=json.loads(raw);assert r['wholeWorldPureAndRngRestored']and len(r['rows'])==2028
    lines=['# Original507e20 source receipt '+hashlib.sha256(raw).hexdigest(),'SETTINGS\t'+ '\t'.join(map(str,[int(r['settingsValid']),r['rawDifficulty'],r['rawLifeOption']]))]
    for index,p in enumerate(r['people']):lines.append('\t'.join(map(str,['P',index,p['native'],p['wars'][0],p['rawWar'],p['age'],int(p['virtual48'])])))
    for p in r['rows']:lines.append('\t'.join(map(str,['M'if r.get('modelEntry')else 'R',p['left'],p['right'],p['leftFlag'],p['rightFlag'],p['seed'],p['result'],p['chance'],p['rngAfter']])))
    output.write_text('\n'.join(lines)+'\n');print('PASS response import',hashlib.sha256(output.read_bytes()).hexdigest())
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('source',type=Path);p.add_argument('output',type=Path);a=p.parse_args();export(a.source,a.output)
