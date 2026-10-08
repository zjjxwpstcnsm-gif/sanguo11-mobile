#!/usr/bin/env python3
"""Original508290 fullterminal checks with actual initialized/fullframe sources.
Boundary HP and pending-hit fixtures are declared; no rule/RNG replacement.
"""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
CORPUS_SHA='2bb4e40c692c1f0532fe31ebde2364d13c690c96aaf966f8f12248f9c368d426'
def inspect(installation,corpus,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve prior native receipt')
 raw=corpus.read_bytes();assert sha(raw)==CORPUS_SHA;r=json.loads(raw);d,w,src,geo,people=prepare(installation);assert r['source']==src and people==r['people'];baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));rows=[]
 def one(snapshot,pending,label):
  w.u.mem_write(d.fixture,snapshot);value=w.call(0x508290,pending,d.fixture+0x2b4,receiver=d.fixture,count=10000000);value=value if value<0x80000000 else value-0x100000000;after=bytes(w.u.mem_read(d.fixture,0x59c));assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));rows.append(dict(label=label,pending=bool(pending),returnValue=value,beforeHex=snapshot.hex(),afterHex=after.hex(),wholeSourceWorldAndRngUnchanged=True))
 for number,case in enumerate(r['cases']):
  for label,snapshot in [('initial',case['modelHex'])]+[(str(n),case['trace'][n]['modelHex'])for n in sorted(set([0,len(case['trace'])//2,len(case['trace'])-1]))]:
   for pending in [0,1]:one(bytes.fromhex(snapshot),pending,str(number)+'/'+label)
 template=bytes.fromhex(r['cases'][0]['modelHex'])
 for orientation in [0,1]:
  for left,right in [(0,0),(0,1),(1,0),(1,1),(100,100)]:
   for damage in [0,1,100,101]:
    for pending in [0,1]:
     b=bytearray(template);struct.pack_into('<i',b,0x1c,orientation);struct.pack_into('<i',b,0x28,left);struct.pack_into('<i',b,0x114,right);struct.pack_into('<i',b,0x4c4,2);struct.pack_into('<6i',b,0x2b4,damage,0,0,0,0,0);struct.pack_into('<6i',b,0x2cc,damage,0,1,0,0,0);one(bytes(b),pending,'boundary/'+str((orientation,left,right,damage)))
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=src,geography=geo,people=people,corpusSha=CORPUS_SHA,cases=rows,limits=['Actualsource/fullframe snapshots retained','BoundaryHP/damage/orientation areexplicit fixture values, notforced normal outcomes','Original508290 unchanged; modelwinner/loser versusrelativeorientation return keptseparate','Full terminalhealth/injury/rewards/capture/unit/campaign normalAPKstillrequired'],completeGoal=False),indent=2)+'\n');print('PASS originalterminal',len(rows),'SHA',sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('corpus',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.corpus,a.output)
