#!/usr/bin/env python3
"""Original reset/fighter init; no completed full model or campaign claim."""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,corpus,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve receipt')
 raw=corpus.read_bytes();assert sha(raw)=='2bb4e40c692c1f0532fe31ebde2364d13c690c96aaf966f8f12248f9c368d426';r=json.loads(raw);d,w,src,geo,people=prepare(installation);assert src==r['source']and people==r['people'];items=[]
 for n in range(100):
  ptr=w.call(0x490b30,n,receiver=w.root);b=bytes(w.u.mem_read(ptr,0x50)) if ptr else b''
  if ptr and w.call(0x47a630,ptr):items.append(dict(nativeId=n,type=struct.unpack_from('<i',b,0x38)[0],owner=struct.unpack_from('<i',b,0x40)[0]))
 baseline=bytes(w.u.mem_read(0x7200000,0x300000));rows=[]
 for case in r['cases']:
  b=bytes.fromhex(case['trace'][-1]['modelHex']);w.u.mem_write(d.fixture,b);w.call(0x508120,receiver=d.fixture,count=10000000);after=bytes(w.u.mem_read(d.fixture,0x59c));assert baseline==bytes(w.u.mem_read(0x7200000,0x300000));rows.append(dict(operation='reset',beforeHex=b.hex(),afterHex=after.hex(),worldUnchanged=True))
  for side in range(2):
   for slot in range(3):
    person=people[side*3+slot]
    for hp,spirit,injury in [(0,0,0),(100,300,3),(45,100,1)]:
     w.u.mem_write(d.fixture,b);w.call(0x50ce00,person['pointer'],hp,spirit,injury,receiver=d.fixture+0x24+side*0xec+slot*64,count=10000000);after=bytes(w.u.mem_read(d.fixture,0x59c));assert baseline==bytes(w.u.mem_read(0x7200000,0x300000));held=[[x['nativeId'],x['type']]for x in items if x['owner']==person['nativeId']];rows.append(dict(operation='fighter',side=side,slot=slot,identity=person['pointer'],hp=hp,spirit=spirit,injury=injury,held=held,beforeHex=b.hex(),afterHex=after.hex(),worldUnchanged=True))
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=src,geography=geo,people=people,items=items,cases=rows,limits=['Actual source held items; six sample owners previously empty','Reset preserves fields original does not write','Full higher initializer/gear nonempty branches/normal admission/API/Save/APK pending'],completeGoal=False),indent=2)+'\n');print('PASS original initialization',len(rows),'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('corpus',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.corpus,a.output)
