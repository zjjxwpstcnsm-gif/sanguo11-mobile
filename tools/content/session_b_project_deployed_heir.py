#!/usr/bin/env python3
"""Bounded test projection; full original receipt is preserved and SHA-linked."""
import argparse,hashlib,json
from pathlib import Path
p=argparse.ArgumentParser(description=__doc__);p.add_argument('input',type=Path);p.add_argument('--sha',required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
raw=a.input.read_bytes();assert hashlib.sha256(raw).hexdigest()==a.sha
j=json.loads(raw);assert j['wholeWorldAndRngRestored'] and all(r['rngPure']for r in j['rows']);assert not a.output.exists();assert str(a.output).startswith('out/session-b/')
rows=[]
for r in j['rows']:
 rows.append(dict(crewBefore=r['crewBefore'],declaredTroops=r.get('declaredTroops',5000),declaredEnergy=r.get('declaredEnergy',100),after=dict(unitHex=r['after']['unitHex'],sites=[dict(nativeId=x['nativeId'],governorRaw=x['governorRaw']if x['governorRaw']<2147483648 else x['governorRaw']-4294967296)for x in r['after'].get('sites',[])],people=[dict(nativeId=x['nativeId'],rawLoyalty=bytes.fromhex(x['hex'])[0xac])for x in r['after']['people']])))
a.output.write_text(json.dumps(dict(exeSha=j['exeSha'],originalReceiptPath=str(a.input),originalReceiptSha=a.sha,wholeWorldAndRngRestored=True,rows=rows,limits=j['limits']),indent=2)+'\n')
print('PASS SHA linked projection',hashlib.sha256(a.output.read_bytes()).hexdigest())
