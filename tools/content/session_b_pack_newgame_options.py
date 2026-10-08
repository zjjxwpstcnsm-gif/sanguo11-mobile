#!/usr/bin/env python3
"""Deterministic menu choices from complete original button/export callbacks."""
import argparse,hashlib,json,itertools
from pathlib import Path
p=argparse.ArgumentParser(description=__doc__);p.add_argument('input',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();raw=a.input.read_bytes();digest=hashlib.sha256(raw).hexdigest();assert digest=='ec3aa4c89e4edd0a78f4b7c92031e09ff0f46036f3a52c51034c18f0323539fc';j=json.loads(raw)
assert j['wholeWorldAndRngRestored'] and len(j['rows'])==27
assert {tuple(r['uiChoice'])for r in j['rows']}==set(itertools.product(range(3),repeat=3))
assert all(r['sourceFlag18']==0 and r['actualRootDifficultyDeathLife']==r['uiChoice']and r['rngPure']for r in j['rows'])
assert str(a.output)in ['core/src/main/resources/pc-duel/newgame-options.tsv','out/session-b/newgame-options-import1.tsv','out/session-b/newgame-options-import2.tsv'];assert not a.output.exists()
lines=['# original newgame option callbacks '+digest,'# executable '+j['exeSha']]
for t in j['tables']:
 for index,label in enumerate(t['labels']):
  assert '\t'not in label['text']and '\n'not in label['text']
  lines.append('\t'.join(map(str,[t['field'],index,label['text'],label['rawHex'],t['controls'][index],hex(t['uiOffset']),{'difficulty':'0x20','death':'0x24','life':'0x38'}[t['field']]])))
a.output.write_bytes(('\n'.join(lines)+'\n').encode('utf-8'));print(hashlib.sha256(a.output.read_bytes()).hexdigest())
