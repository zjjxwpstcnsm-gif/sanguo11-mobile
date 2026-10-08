#!/usr/bin/env python3
"""Original actual source heldtreasure AIbonus and reserve scores, no rule substitution."""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
CORPUS_SHA='2bb4e40c692c1f0532fe31ebde2364d13c690c96aaf966f8f12248f9c368d426'
def inspect(installation,corpus,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve prior originalreceipt')
 raw=corpus.read_bytes();assert sha(raw)==CORPUS_SHA;r=json.loads(raw);d,w,src,geo,people=prepare(installation);assert src==r['source']and people==r['people'];rows=[];held=[]
 for p in people:
  actor=p['pointer'];index=w.call(0x491310,actor,receiver=w.root);items=[]
  for native in range(100):
   item=w.call(0x490b30,native,receiver=w.root)
   if not w.call(0x47a630,item):continue
   b=bytes(w.u.mem_read(item,0x50));kind=struct.unpack_from('<i',b,0x38)[0];owner=struct.unpack_from('<i',b,0x40)[0]
   if owner==index:items.append([native,kind])
  bonus=w.call(0x4faa60,actor,count=10000000);held.append(dict(nativeId=p['nativeId'],originalRegistryIndex=index,heldNativeItems=items,originalBonus=bonus))
 # Warm original current ability first; pure checks start after source cache.
 for p in people:
  for injury in range(4):w.call(0x50c690,p['pointer'],injury,1,count=10000000)
 baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4))
 for number,case in enumerate(r['cases']):
  for snapshot in [case['modelHex'],case['trace'][len(case['trace'])//2]['modelHex'],case['trace'][-1]['modelHex']]:
   for side in range(2):
    for slot in range(3):
     w.u.mem_write(d.fixture,bytes.fromhex(snapshot));context=d.fixture+0x238+side*0x18;w.call(0x4faba0,receiver=context);w.u.mem_write(d.fixture+0x1000,struct.pack('<2I',context,d.fixture));before=bytes(w.u.mem_read(d.fixture,0x59c));score=w.call(0x4fb280,side,slot,receiver=d.fixture+0x1000,count=10000000);hp=w.call(0x506030,side,slot,receiver=d.fixture);war=w.call(0x506160,side,slot,1,receiver=d.fixture);actor=w.call(0x505f70,side,slot,receiver=d.fixture);native=w.call(0x4883c0,receiver=actor);bonus=w.call(0x4faa60,actor);assert before==bytes(w.u.mem_read(d.fixture,0x59c))and baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));rows.append(dict(case=number,side=side,slot=slot,nativeId=native,health=hp,duelWar=war,treasureBonus=bonus,score=score,fullModelWorldAndRngUnchanged=True))
  print('PASS original reserve score case',number,flush=True)
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=src,geography=geo,people=people,heldTreasures=held,cases=rows,corpusSha=CORPUS_SHA,limits=['Actual sourceoriginal treasury100domain queried byvalidity/owner index; not activated ancient/NPC or allsourcecurrentitem mapping','Original4faa60/4fb280/currentduelWAR/HP executed unchanged','Original currentability cachewarmed first; no saved globalbase rewrite','FullhigherAI/swap/gameplay/campaign/Save/APKstillrequired'],completeGoal=False),indent=2)+'\n');print('PASS originalscores',len(rows),'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('corpus',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.corpus,a.output)
