#!/usr/bin/env python3
"""Original normal damage/SP calculation and whole numeric queue settlement."""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
CORPUS_SHA='2bb4e40c692c1f0532fe31ebde2364d13c690c96aaf966f8f12248f9c368d426'
def inspect(installation,corpus,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier original receipt')
 raw=corpus.read_bytes();assert sha(raw)==CORPUS_SHA;r=json.loads(raw);d,w,src,geo,people=prepare(installation);assert src==r['source']and people==r['people']
 for p in people:
  for injury in range(4):w.call(0x50c690,p['pointer'],injury,1,count=10000000)
 baseline=bytes(w.u.mem_read(0x7200000,0x300000));difficultyValid=bool(w.call(0x47a630,0x7201958));difficulty=struct.unpack('<i',w.u.mem_read(0x7201978,4))[0];rows=[]
 def execute(before,op,args,facts,seed=23):
  w.u.mem_write(d.fixture,before);w.u.mem_write(0x8a5d44,struct.pack('<I',seed));outs=[]
  if op=='damage':
   w.u.mem_write(d.fixture+0x1000,bytes(8));value=w.call(0x50aee0,d.fixture+0x1000,d.fixture+0x1004,*args,receiver=d.fixture,count=10000000);outs=list(struct.unpack('<2i',w.u.mem_read(d.fixture+0x1000,8)))
  else:value=w.call(0x509820 if op=='spirit'else 0x50b980,*args,receiver=d.fixture,count=10000000)
  after=bytes(w.u.mem_read(d.fixture,0x59c));assert baseline==bytes(w.u.mem_read(0x7200000,0x300000));rows.append(dict(operation=op,args=args,facts=facts,seed=seed,returnValue=value if value<0x80000000 else value-0x100000000,outputs=outs,nativeRng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0],beforeHex=before.hex(),afterHex=after.hex(),worldUnchanged=True))
 for number,case in enumerate(r['cases']):
  for snapshot in [case['modelHex'],case['trace'][len(case['trace'])//2]['modelHex'],case['trace'][-1]['modelHex']]:
   b=bytes.fromhex(snapshot);w.u.mem_write(d.fixture,b);facts=[]
   for side in range(2):
    for slot in range(3):
     actor=w.call(0x505f70,side,slot,receiver=d.fixture);facts.append(dict(nativeId=w.call(0x4883c0,receiver=actor),valid=bool(w.call(0x47a600,actor))))
   for side in range(2):
    active=struct.unpack_from('<i',b,0xe8+side*0xec)[0]
    for move in range(8):
     for outcome in range(3):execute(b,'damage',[side,active,move,outcome],facts)
    for incoming in range(2):
     for basis in [0,1,3,5,10,40,80,100]:execute(b,'spirit',[side,active,incoming,basis],facts)
   for seed in [0,23,42,0xffffffff]:
    w.u.mem_write(d.fixture,b);w.u.mem_write(0x8a5d44,struct.pack('<I',seed));w.call(0x50b8a0,receiver=d.fixture,count=10000000);queue=bytes(w.u.mem_read(d.fixture,0x59c));rng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0];execute(queue,'settle',[],facts,rng)
  print('PASS original settlement case',number,flush=True)
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=src,geography=geo,people=people,corpusSha=CORPUS_SHA,difficultyValid=difficultyValid,difficulty=difficulty,cases=rows,limits=['Original50aee0/509820/50b980 and actual NULLpresentation numeric writers unchanged','Declared seed/type/outcome/SP basis; source snapshots and generated fullqueues, not normal unit/human admission','Six source people, actor exceptions/other settings/source activation not all covered','Fullworld unchanged after current cache warm; native outputs/model/RNG recorded','Full battle/init/remainingphases/campaign/save/API/UI/APK incomplete'],completeGoal=False),indent=2)+'\n');print('PASS original settlement',len(rows),'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('corpus',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.corpus,a.output)
