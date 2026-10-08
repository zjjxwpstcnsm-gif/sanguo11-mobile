#!/usr/bin/env python3
"""Original pair matchup and injury matrix refresh with actual source actors."""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,corpus,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier matrix receipt')
 raw=corpus.read_bytes();assert sha(raw)=='2bb4e40c692c1f0532fe31ebde2364d13c690c96aaf966f8f12248f9c368d426';r=json.loads(raw);d,w,src,geo,people=prepare(installation);assert src==r['source']and people==r['people']
 for p in people:
  for injury in range(4):w.call(0x50c690,p['pointer'],injury,1,count=10000000)
 baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));rows=[];template=bytes.fromhex(r['cases'][0]['modelHex'])
 for own in range(3):
  for other in range(3):
   for inj0 in range(4):
    for inj1 in range(4):
     b=bytearray(template);struct.pack_into('<i',b,0x24+own*64+12,inj0);struct.pack_into('<i',b,0x110+other*64+12,inj1);w.u.mem_write(d.fixture,bytes(b));a=w.call(0x506160,0,own,1,receiver=d.fixture);c=w.call(0x506160,1,other,1,receiver=d.fixture);value=w.call(0x509530,0,own,1,other,receiver=d.fixture,count=10000000);assert b==bytes(w.u.mem_read(d.fixture,0x59c))and baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));rows.append(dict(operation='pair',own=own,other=other,injuries=[inj0,inj1],wars=[a,c],ratio=value,modelWorldAndRngUnchanged=True))
 for number,case in enumerate(r['cases']):
  for snapshot in [case['modelHex'],case['trace'][len(case['trace'])//2]['modelHex'],case['trace'][-1]['modelHex']]:
   w.u.mem_write(d.fixture,bytes.fromhex(snapshot));wars=[]
   for side in range(2):wars.append([w.call(0x506160,side,slot,1,receiver=d.fixture)for slot in range(3)])
   before=bytes(w.u.mem_read(d.fixture,0x59c));w.call(0x50acc0,receiver=d.fixture,count=10000000);after=bytes(w.u.mem_read(d.fixture,0x59c));assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));rows.append(dict(operation='refresh',wars=wars,beforeHex=before.hex(),afterHex=after.hex(),worldAndRngUnchanged=True))
  print('PASS original matrix case',number,flush=True)
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=src,geography=geo,people=people,cases=rows,limits=['Original509530/50acc0 unchanged; current native WAR warmed before purity fence','Actual six source people with declared model injury overrides, not ordinary deployment/activation','Full rule binding/campaign/Save/UI/APK incomplete'],completeGoal=False),indent=2)+'\n');print('PASS original matrix',len(rows),'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('corpus',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.corpus,a.output)
