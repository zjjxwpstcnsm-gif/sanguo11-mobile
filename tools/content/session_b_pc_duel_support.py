#!/usr/bin/env python3
"""Original support admission and probability from actual source relation getters."""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
CORPUS_SHA='2bb4e40c692c1f0532fe31ebde2364d13c690c96aaf966f8f12248f9c368d426'
def inspect(installation,corpus,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve previous native evidence')
 raw=corpus.read_bytes();assert sha(raw)==CORPUS_SHA;r=json.loads(raw);d,w,src,geo,people=prepare(installation);assert src==r['source']and people==r['people'];baseline=bytes(w.u.mem_read(0x7200000,0x300000));rows=[]
 for number,case in enumerate(r['cases']):
  for snapshot in [case['modelHex'],case['trace'][len(case['trace'])//2]['modelHex'],case['trace'][-1]['modelHex']]:
   for side in range(2):
    for fixture in [None,2,3,4,6,15]:
     b=bytearray.fromhex(snapshot);team=0x24+side*0xec;active=struct.unpack_from('<i',b,team+0xc4)[0]
     if fixture is not None:
      struct.pack_into('<i',b,0x10,fixture)
      for offset in [0xcc,0xd4,0xdc,0xe0,0xe4]:struct.pack_into('<i',b,team+offset,0)
      for slot in range(3):
       if slot!=active:struct.pack_into('<2i',b,team+slot*64+0x14,2,100)
     w.u.mem_write(d.fixture,bytes(b));own=w.call(0x505fb0,side,receiver=d.fixture);other=w.call(0x505fb0,1-side,receiver=d.fixture);own_native=w.call(0x4883c0,receiver=own);other_native=w.call(0x4883c0,receiver=other)
     own_v=struct.unpack('<I',w.u.mem_read(own,4))[0];own_owner=w.call(struct.unpack('<I',w.u.mem_read(own_v+0x40,4))[0],receiver=own);facts=[]
     for slot in range(3):
      actor=w.call(0x505f70,side,slot,receiver=d.fixture);v=struct.unpack('<I',w.u.mem_read(actor,4))[0];ab=bytes(w.u.mem_read(actor,0x190));ob=bytes(w.u.mem_read(own,0x190))
      facts.append(dict(slot=slot,valid=bool(w.call(0x47a600,actor)),threshold=w.call(0x5079e0,actor),p4887d0=bool(w.call(0x4887d0,own_native,receiver=actor)),p488790=bool(w.call(0x488790,own_native,receiver=actor)),p4889e0Other=bool(w.call(0x4889e0,other_native,receiver=actor)),p4889e0Own=bool(w.call(0x4889e0,own_native,receiver=actor)),p488910=bool(w.call(0x488910,own_native,receiver=actor)),p48bb70=bool(w.call(0x48bb70,own_native,receiver=actor)),ownStatusZero=bool(w.call(0x488c00,receiver=own)),ownOwnerValid=bool(w.call(0x4123b0,own_owner)),sameOwner=own_owner==w.call(struct.unpack('<I',w.u.mem_read(v+0x40,4))[0],receiver=actor),rawLoyalty=ab[0xac],rawE4Equal=ab[0xe4:0xe8]==ob[0xe4:0xe8],affinityGapLowByte=w.call(0x489f80,own_native,receiver=actor)&255,eligible=bool(w.call(0x508820,side,slot,receiver=d.fixture))))
     before=bytes(w.u.mem_read(d.fixture,0x59c));assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))
     for seed in [0,23,42,0xffffffff]:
      results=[]
      for slot in range(3):
       w.u.mem_write(0x8a5d44,struct.pack('<I',seed));value=w.call(0x508890,side,slot,receiver=d.fixture,count=10000000);results.append([value,struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]])
      w.u.mem_write(0x8a5d44,struct.pack('<I',seed));result=w.call(0x50b270,side,receiver=d.fixture,count=10000000)
      assert before==bytes(w.u.mem_read(d.fixture,0x59c))and baseline==bytes(w.u.mem_read(0x7200000,0x300000))
      rows.append(dict(case=number,side=side,roundAndWaitingFixture=fixture,seed=seed,result=result if result<0x80000000 else result-0x100000000,nativeRng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0],individualSupport=results,facts=facts,modelHex=before.hex(),modelWorldUnchanged=True))
  print('PASS original support case',number,flush=True)
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=src,geography=geo,people=people,corpusSha=CORPUS_SHA,cases=rows,limits=['Original relation predicates retained by source addresses until separately decoded','Six genuine source people in declared teams, not normal opposing unit activation','Round/waiting/status/order fixtures explicit, no original rule/RNG replacement','Full battle/campaign/Save/API/UI/APK incomplete'],completeGoal=False),indent=2)+'\n');print('PASS original support',len(rows),'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('corpus',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.corpus,a.output)
