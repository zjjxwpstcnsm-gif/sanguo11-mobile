#!/usr/bin/env python3
"""Original injury flags/effective WAR/cache refresh without global officer rewriting."""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,corpus,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve original receipt')
 raw=corpus.read_bytes();assert sha(raw)=='2bb4e40c692c1f0532fe31ebde2364d13c690c96aaf966f8f12248f9c368d426';r=json.loads(raw);d,w,src,geo,people=prepare(installation);assert src==r['source']and people==r['people'];war=[]
 for p in people:war.append([w.call(0x50c690,p['pointer'],injury,1,count=10000000)for injury in range(4)])
 baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));template=bytes.fromhex(r['cases'][0]['modelHex']);rows=[]
 for operation in ['set','add_refresh']:
  for orientation in range(2):
   for side in range(2):
    for slot in range(3):
     for beforeInjury in range(4):
      for value in [-4,-1,0,1,2,3,4,10]:
       b=bytearray(template);struct.pack_into('<i',b,0x1c,orientation);struct.pack_into('<i',b,0x24+side*0xec+slot*64+12,beforeInjury);w.u.mem_write(d.fixture,bytes(b));w.call(0x50acc0,receiver=d.fixture,count=10000000);before=bytes(w.u.mem_read(d.fixture,0x59c))
       w.call(0x507bb0 if operation=='set'else 0x507c60,side,slot,value&0xffffffff,receiver=d.fixture,count=10000000)
       if operation=='add_refresh'and value>0:w.call(0x50acc0,receiver=d.fixture,count=10000000)
       after=bytes(w.u.mem_read(d.fixture,0x59c));assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));rows.append(dict(operation=operation,orientation=orientation,side=side,slot=slot,old=beforeInjury,value=value,beforeHex=before.hex(),afterHex=after.hex(),worldAndRngUnchanged=True))
     print('PASS original injury',operation,orientation,side,slot,flush=True)
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=src,geography=geo,people=people,warByInjury=war,cases=rows,limits=['Original507bb0/507c60/50acc0 execute unchanged, no global current/base rewrite','Explicit injury/side/value fixtures on checked six source actors, not actual injury-producing battle','Special effects/human/normal campaign/Save/API/APK incomplete'],completeGoal=False),indent=2)+'\n');print('PASS original injury',len(rows),'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('corpus',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.corpus,a.output)
