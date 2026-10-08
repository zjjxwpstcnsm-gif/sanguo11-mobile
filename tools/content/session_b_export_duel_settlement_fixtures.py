#!/usr/bin/env python3
"""Export pinned actual original no-casualty/draw callback receipts."""
import argparse,json,hashlib
from pathlib import Path
def export(source,output):
 raw=source.read_bytes();assert hashlib.sha256(raw).hexdigest()=='57873b814177de8cb227a987e41108d64a6f628961714a5aa3f574b936396c43'
 r=json.loads(raw);assert r['wholeWorldAndRngRestored']and r['bothCategory0']and not r['completeGoal']
 if output.exists():raise ValueError('Preserve corpus')
 model=Path('out/session-b/duel-campaign-source0-numeric-v1.json').read_bytes();assert hashlib.sha256(model).hexdigest()=='722a6e94c55eacd0a024aaefb7fc30076d78a88e197eaeae209a6eea97df45c7'
 m=json.loads(model);assert m['source']==r['source'];lines=['# Actual original callback matrix SHA57873b814177de8cb227a987e41108d64a6f628961714a5aa3f574b936396c43; declared terminal inputs, not ordinary gameplay','MANAGER\t'+m['managerHex'],'MODEL\t'+m['modelHex'],'NATIVES\t'+','.join(str(p['nativeId'])for p in r['crew'])]
 for x in r['rows']:
  before=x['before'];after=x['after'];fields=['ROW',x['winner'],x['managerHex'],x['troops'],x['energy'],x['seed'],after['rng']]
  for a,b in zip(before['persons'],after['persons']):fields.extend([b['health'],b['injury'],b['merit']-a['merit'],b['xp'][1]-a['xp'][1]])
  for a,b in zip(before['units'],after['units']):fields.extend([b['troops'],b['energy']])
  fields.extend(a['capacity']for a in before['units'])
  lines.append('\t'.join(map(str,fields)))
 output.write_text('\n'.join(lines)+'\n');print('PASS original callback export',len(r['rows']),hashlib.sha256(output.read_bytes()).hexdigest())
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('source',type=Path);p.add_argument('output',type=Path);a=p.parse_args();export(a.source,a.output)
