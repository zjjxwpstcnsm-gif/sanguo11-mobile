#!/usr/bin/env python3
"""Original normal exchange primitives and complete queue with model/RNG receipts."""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
CORPUS_SHA='2bb4e40c692c1f0532fe31ebde2364d13c690c96aaf966f8f12248f9c368d426'
def inspect(installation,corpus,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier native receipt')
 raw=corpus.read_bytes();assert sha(raw)==CORPUS_SHA;r=json.loads(raw);d,w,src,geo,people=prepare(installation);assert src==r['source']and people==r['people']
 for p in people:
  for injury in range(4):w.call(0x50c690,p['pointer'],injury,1,count=10000000)
 baseline=bytes(w.u.mem_read(0x7200000,0x300000));lifeValid=bool(w.call(0x47a630,0x7201958));lifeOption=struct.unpack('<i',w.u.mem_read(0x7201990,4))[0];rows=[]
 def execute(model,op,args,seed,facts):
  w.u.mem_write(d.fixture,model);w.u.mem_write(0x8a5d44,struct.pack('<I',seed));before=bytes(w.u.mem_read(d.fixture,0x59c));address={'side':0x508b50,'move':0x508d90,'hit':0x50b3b0,'queue':0x50b8a0}[op];result=w.call(address,*args,receiver=d.fixture,count=10000000);after=bytes(w.u.mem_read(d.fixture,0x59c));assert baseline==bytes(w.u.mem_read(0x7200000,0x300000));rows.append(dict(operation=op,args=args,seed=seed,returnValue=result if result<0x80000000 else result-0x100000000,nativeRng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0],beforeHex=before.hex(),afterHex=after.hex(),facts=facts,worldUnchanged=True))
 for number,case in enumerate(r['cases']):
  for snapshot in [case['modelHex'],case['trace'][len(case['trace'])//2]['modelHex'],case['trace'][-1]['modelHex']]:
   b=bytearray.fromhex(snapshot);w.u.mem_write(d.fixture,bytes(b));facts=[]
   for side in range(2):
    for slot in range(3):
     actor=w.call(0x505f70,side,slot,receiver=d.fixture);facts.append(dict(nativeId=w.call(0x4883c0,receiver=actor),personality=struct.unpack('<i',w.u.mem_read(actor+0xfc,4))[0],originalAge=w.call(0x488a20,receiver=actor),duelWar=w.call(0x506160,side,slot,1,receiver=d.fixture),valid=bool(w.call(0x47a600,actor))))
   for seed in [0,23,42,0xffffffff]:
    for orientation in range(2):
     c=bytearray(b);struct.pack_into('<i',c,0x1c,orientation);execute(bytes(c),'side',[],seed,facts)
    for side in range(2):
     team=0x24+side*0xec;active=struct.unpack_from('<i',b,team+0xc4)[0]
     for stance in range(4):
      for streak in [0,5,6,20]:
       c=bytearray(b);struct.pack_into('<i',c,team+active*64+0x10,stance);struct.pack_into('<i',c,team+0xe8,streak);execute(bytes(c),'move',[side,active],seed,facts)
     for move in range(8):execute(bytes(b),'hit',[side,active,move],seed,facts)
    execute(bytes(b),'queue',[],seed,facts)
  print('PASS original exchange case',number,flush=True)
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=src,geography=geo,people=people,corpusSha=CORPUS_SHA,lifeValid=lifeValid,lifeOption=lifeOption,cases=rows,limits=['Original508b50/508d90/50b3b0/50b8a0 complete numeric rules unchanged','Declared orientation/stance/streak/seeds, not normal controller input or deployed enemy admission','Six actual source people; special native pair/age exceptions not all dynamically covered','Full world checked unchanged after native current cache warm','Full battle/campaign/save/API/UI/APK remain incomplete'],completeGoal=False),indent=2)+'\n');print('PASS original exchange',len(rows),'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('corpus',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.corpus,a.output)
