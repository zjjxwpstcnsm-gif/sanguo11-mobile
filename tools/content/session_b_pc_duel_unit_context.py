#!/usr/bin/env python3
"""Original constructed units with declared source crews; never opening truth."""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier receipt')
 d,w,src,geo,known=prepare(installation);people=[]
 for native in range(670):
  p=w.call(0x490b00,native,receiver=w.root)
  if not w.call(0x47a600,p):continue
  raw=bytes(w.u.mem_read(p,0x190));vt=struct.unpack_from('<I',raw)[0];ownerFn=struct.unpack('<I',w.u.mem_read(vt+0x40,4))[0];owner=w.call(ownerFn,receiver=p)
  if 0<=owner<42 and w.call(0x47a630,p):people.append(dict(nativeId=native,pointer=p,owner=owner,status=struct.unpack_from('<i',raw,0xa0)[0],runtimeRecordSha=sha(raw),health=raw[0x128],war=w.call(0x489080,receiver=p)&255))
 baseline=bytes(w.u.mem_read(0x7200000,0x300000));units=[];cases=[];counts={owner:sum(p['owner']==owner for p in people)for owner in range(42)};enemy=next(owner for owner,n in counts.items()if owner!=2 and n>=3)
 output.with_suffix('.partial.json').write_text(json.dumps(dict(source=src,geography=geo,sourcePeople=people,selectedEnemy=enemy,sourceCounts=counts,completeGoal=False),indent=2)+'\n')
 for index in range(2):
  ptr=w.root+0x169730+0xf4*index;raw=bytes(w.u.mem_read(ptr,0xf4));vt=struct.unpack_from('<I',raw)[0];getters=[]
  for offset in [0x20,0x3c,0x40,0x44,0x48,0x50]:
   fn=struct.unpack('<I',w.u.mem_read(vt+offset,4))[0];getters.append(dict(vtableOffset=offset,function=hex(fn),codeHex=bytes(w.u.mem_read(fn,48)).hex()))
  units.append(dict(index=index,pointer=ptr,beforeHex=raw.hex(),validBefore=bool(w.call(0x47a630,ptr)),getters=getters))
  owner=2 if index==0 else enemy;crew=sorted([p for p in people if p['owner']==owner],key=lambda p:-p['war'])[:3];assert len(crew)==3
  w.u.mem_write(ptr+8,struct.pack('<i',0));w.u.mem_write(ptr+12,struct.pack('<3i',*[p['nativeId']for p in crew]));after=bytes(w.u.mem_read(ptr,0xf4));cases.append(dict(index=index,sourceLeaderOwner=owner,arrayIndex=index,declaredCategory8=0,originalTroopCapacity=w.call(0x4957b0,receiver=ptr),declaredCrew=crew,afterHex=after.hex(),validAfter=bool(w.call(0x47a630,ptr)),originalOwner=w.call(0x4955a0,receiver=ptr),originalCrew=[w.call(0x495290,i,receiver=ptr)for i in range(3)]))
 declared=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));assert all(x['originalOwner']==x['sourceLeaderOwner']and x['originalCrew']==[p['nativeId']for p in x['declaredCrew']]for x in cases)
 output.with_suffix('.partial.json').write_text(json.dumps(dict(source=src,geography=geo,sourcePeople=people,units=units,cases=cases,completeGoal=False),indent=2)+'\n')
 args=d.fixture+0x1000;w.call(0x50ddd0,receiver=args);unitPtrs=[x['pointer']for x in units];actors=[x['declaredCrew'][0]['pointer']for x in cases];result=w.call(0x589f70,args,*unitPtrs,*actors,count=10000000);inputBytes=bytes(w.u.mem_read(args,0xcc));assert declared==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));w.u.mem_write(0x7200000,baseline);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=src,geography=geo,sourcePeople=people,units=units,cases=cases,originalInputConfigured=result,inputHex=inputBytes.hex(),worldRestored=True,limits=['Original array indices0/1, both field8 category0 and crews declared on real constructors; field8 is not object index/owner; original troop getter4957b0; no actual deployment/menu evidence','Actual source serving owner0/2 records unchanged, no role activation or rule/RNG replacement','Original589f70 numeric context only; normal58b640 and campaign callbacks remain required'],completeGoal=False),indent=2)+'\n');print('PASS original unitcontext',len(people),[x['validAfter']for x in cases],result,'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
