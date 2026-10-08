#!/usr/bin/env python3
"""Complete original swap selector and RNG; source snapshots plus declared boundaries."""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
CORPUS_SHA='2bb4e40c692c1f0532fe31ebde2364d13c690c96aaf966f8f12248f9c368d426'
def inspect(installation,corpus,output,special=False,willing=False,arbitration=False,pose=False):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve previous native receipt')
 raw=corpus.read_bytes();assert sha(raw)==CORPUS_SHA;r=json.loads(raw);d,w,src,geo,people=prepare(installation);assert src==r['source']and people==r['people']
 for p in people:
  for injury in range(4):w.call(0x50c690,p['pointer'],injury,1,count=10000000)
 baseline=bytes(w.u.mem_read(0x7200000,0x300000));rows=[]
 tables=json.loads(Path('out/session-b/duel-ai-priority-source-v2.json').read_bytes())['aiTables']
 for number,case in enumerate(r['cases']):
  for snapshot in [case['modelHex'],case['trace'][len(case['trace'])//2]['modelHex'],case['trace'][-1]['modelHex']]:
   for side in range(2):
    for hp,counter,buff in [(None,None,None),(10,0,0),(20,2,0),(20,3,0),(30,0,0),(33,0,-1),(75,0,0),(100,1,0)]:
     b=bytearray.fromhex(snapshot);team=0x24+side*0xec;active=struct.unpack_from('<i',b,team+0xc4)[0];assert 0<=active<3
     if hp is not None:
      struct.pack_into('<i',b,team+active*0x40+4,hp);struct.pack_into('<i',b,team+0xd0,counter);struct.pack_into('<i',b,team+0xdc,buff)
     w.u.mem_write(d.fixture,bytes(b));ctx=d.fixture+0x238+side*0x18
     for refresh in range(2)if arbitration else[side]:w.call(0x4faba0,receiver=d.fixture+0x238+refresh*0x18)
     w.u.mem_write(d.fixture+0x1000,struct.pack('<2I',ctx,d.fixture))
     facts=[]
     for s in range(2):
      for slot in range(3):
       actor=w.call(0x505f70,s,slot,receiver=d.fixture);native=w.call(0x4883c0,receiver=actor);facts.append(dict(side=s,slot=slot,nativeId=native,personality=struct.unpack('<i',w.u.mem_read(actor+0xfc,4))[0],predicate488c00=w.call(0x488c00,receiver=actor),valid=bool(w.call(0x506490,s,slot,0xffffffff,receiver=d.fixture)),duelWar=w.call(0x506160,s,slot,1,receiver=d.fixture),treasureBonus=w.call(0x4faa60,actor)))
     before=bytes(w.u.mem_read(d.fixture,0x59c))
     for seed in [0,23,42,0xffffffff]:
      w.u.mem_write(0x8a5d44,struct.pack('<I',seed));result=w.call(0x4fbcb0 if pose else 0x5099a0 if arbitration else 0x4fafa0 if willing else 0x4fb8f0 if special else 0x4fb510,receiver=d.fixture if arbitration else d.fixture+0x1000,count=10000000)
      assert before==bytes(w.u.mem_read(d.fixture,0x59c))and baseline==bytes(w.u.mem_read(0x7200000,0x300000))
      rows.append(dict(case=number,side=side,fixture=[hp,counter,buff],seed=seed,result=result if result<0x80000000 else result-0x100000000,nativeRng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0],modelHex=before.hex(),facts=facts,modelWorldUnchanged=True))
  print('PASS original swap case',number,flush=True)
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=src,geography=geo,people=people,corpusSha=CORPUS_SHA,originalSelector='4fbcb0'if pose else'5099a0'if arbitration else'4fafa0'if willing else'4fb8f0'if special else'4fb510',priorityTables=tables,cases=rows,limits=['Complete original numeric selector no rule/RNG substitution','Source snapshots and explicit HP/counter/buff fixtures, not ordinary battle admission','NULL UI, no original511700 request simulated'if arbitration else'Original488c00 source status-zero predicate; true branch not covered by six source participants','Full engine/campaign/save/API/player/APK remain incomplete'],completeGoal=False),indent=2)+'\n');print('PASS original selector',len(rows),'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('corpus',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--special',action='store_true');p.add_argument('--willing',action='store_true');p.add_argument('--arbitration',action='store_true');p.add_argument('--pose',action='store_true');a=p.parse_args();assert sum([a.special,a.willing,a.arbitration,a.pose])<=1;inspect(a.installation,a.corpus,a.output,a.special,a.willing,a.arbitration,a.pose)
