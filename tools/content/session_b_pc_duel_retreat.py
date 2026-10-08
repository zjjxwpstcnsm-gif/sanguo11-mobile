#!/usr/bin/env python3
"""Original retreat chance and numeric phase10 driver, no campaign substitution."""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,corpus,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier original receipt')
 raw=corpus.read_bytes();assert sha(raw)=='2bb4e40c692c1f0532fe31ebde2364d13c690c96aaf966f8f12248f9c368d426';r=json.loads(raw);d,w,src,geo,people=prepare(installation);assert src==r['source']and people==r['people'];wars=[];protection=[]
 for p in people:
  wars.append([w.call(0x50c690,p['pointer'],injury,1,count=10000000)for injury in range(4)]);protection.append(bool(w.call(0x4890f0,0x20,receiver=p['pointer'])))
 baseline=bytes(w.u.mem_read(0x7200000,0x300000));rows=[]
 for number,case in enumerate(r['cases']):
  for snapshot in [case['modelHex'],case['trace'][len(case['trace'])//2]['modelHex'],case['trace'][-1]['modelHex']]:
   for side in range(2):
    for roundNumber in [0,10,11,50]:
     for ownHorse,otherHorse in [(0,0),(1,0),(0,1),(1,1)]:
      for seed in [0,23,42,0xffffffff]:
       b=bytearray.fromhex(snapshot);own=struct.unpack_from('<i',b,0xe8+side*0xec)[0];other=struct.unpack_from('<i',b,0xe8+(1-side)*0xec)[0];struct.pack_into('<i',b,0x10,roundNumber)
       for s,slot,horse in [(side,own,ownHorse),(1-side,other,otherHorse)]:
        at=0x24+s*0xec+slot*64+0x1c;gear=struct.unpack_from('<i',b,at)[0];struct.pack_into('<i',b,at,(gear&~1)|horse)
       w.u.mem_write(d.fixture,bytes(b));w.u.mem_write(0x8a5d44,struct.pack('<I',seed));result=w.call(0x506a20,side,own,receiver=d.fixture,count=10000000);rng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0];assert b==bytes(w.u.mem_read(d.fixture,0x59c))and baseline==bytes(w.u.mem_read(0x7200000,0x300000));rows.append(dict(operation='roll',side=side,slot=own,seed=seed,result=result,nativeRng=rng,beforeHex=b.hex(),afterHex=b.hex(),worldUnchanged=True))
       if seed==23:
        struct.pack_into('<4i',b,0x578,side,own,3,0 if result else 1);struct.pack_into('<3i',b,4,10,-1,0);w.u.mem_write(d.fixture,bytes(b));before=bytes(b)
        for step in range(4):
         value=w.call(0x505e60,1,receiver=d.fixture,count=10000000);after=bytes(w.u.mem_read(d.fixture,0x59c));assert baseline==bytes(w.u.mem_read(0x7200000,0x300000));rows.append(dict(operation='frame',side=side,slot=own,seed=rng,result=value,nativeRng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0],beforeHex=before.hex(),afterHex=after.hex(),worldUnchanged=True));before=after
         if struct.unpack_from('<i',after,8)[0]==12:break
  print('PASS original retreat case',number,flush=True)
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=src,geography=geo,people=people,warByInjury=wars,sourceProtection=protection,cases=rows,limits=['Original506a20/phase10 untouched, explicit gear/round/seed/control-state fixtures','Six source people, source protected-true/other deployment/normalplayer not covered','Stop beforephase12 manager/campaign callbacks, capture/escape unit consequences remain required','Sourceworld unchanged, complete numeric model/RNG observed','Full campaign/save/API/UI/APK incomplete'],completeGoal=False),indent=2)+'\n');print('PASS original retreat',len(rows),'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('corpus',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.corpus,a.output)
