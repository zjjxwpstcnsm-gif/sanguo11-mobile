#!/usr/bin/env python3
"""Original best legal special selection and complete RNG with actual source models."""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
CORPUS_SHA='2bb4e40c692c1f0532fe31ebde2364d13c690c96aaf966f8f12248f9c368d426'
def inspect(installation,corpus,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve prior native receipt')
 raw=corpus.read_bytes();assert sha(raw)==CORPUS_SHA;r=json.loads(raw);d,w,src,geo,people=prepare(installation);assert src==r['source']and people==r['people'];baseline=bytes(w.u.mem_read(0x7200000,0x300000));rows=[]
 for number,case in enumerate(r['cases']):
  for snapshot in [case['modelHex'],case['trace'][len(case['trace'])//2]['modelHex'],case['trace'][-1]['modelHex']]:
   for side in range(2):
    for spirit in [0,100,200,300]:
     for seed in [0,23,42,0xffffffff]:
      b=bytearray.fromhex(snapshot);active=struct.unpack_from('<i',b,0xe8+side*0xec)[0];assert 0<=active<3;struct.pack_into('<i',b,0x24+side*0xec+active*0x40+8,spirit);w.u.mem_write(d.fixture,bytes(b));context=d.fixture+0x238+side*0x18;w.call(0x4faba0,receiver=context);before=bytes(w.u.mem_read(d.fixture,0x59c));w.u.mem_write(0x8a5d44,struct.pack('<I',seed));w.u.mem_write(d.fixture+0x1000,struct.pack('<2I',context,d.fixture));result=w.call(0x4fb3e0,receiver=d.fixture+0x1000,count=10000000)
      # Original4fb3e0 expects the two-pointer temporary wrapper, matching4fbff0.
      # Actual source context and temporary-wrapper layout are preserved.
      after=bytes(w.u.mem_read(d.fixture,0x59c));assert before==after and baseline==bytes(w.u.mem_read(0x7200000,0x300000));rows.append(dict(case=number,side=side,active=active,spiritFixture=spirit,seed=seed,result=result if result<0x80000000 else result-0x100000000,nativeRng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0],modelHex=before.hex(),modelWorldUnchanged=True))
  print('PASS originalbest special case',number,flush=True)
 output.write_text(json.dumps(dict(source=src,exeSha=EXE_SHA,geography=geo,people=people,corpusSha=CORPUS_SHA,cases=rows,limits=['Actualsource/current model snapshot with declaredspirit/seed fixtures','Original4fb3e0 numeric legality/buff/weighted selection/RNG unchanged','FullhigherAI priorities/normalplayer/admission/campaign/Save/APKstillrequired'],completeGoal=False),indent=2)+'\n');print('PASS original bestspecial',len(rows),'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('corpus',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.corpus,a.output)
