#!/usr/bin/env python3
"""Full original4b1950 with declared source units in their home region.
No officer stats/owner/task/result, rule predicates or callback is replaced.
"""
import argparse,json,struct
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_EIP
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output):
 output_guard(installation,output)
 if output.exists()or output.with_suffix('.failure.json').exists():raise ValueError('Preserve original receipt')
 d,w,source,geo,_=prepare(installation)
 baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));stage='constructors';cases=[]
 def person(n):return bytes(w.u.mem_read(w.root+0xc0bc+n*0x190,0x190))
 def people():return b''.join(person(n)for n in range(1100))
 try:
  for f,p in [(0x415400,0x32602b0),(0x477810,0x6ee7888),(0x4f52d0,0x91ba558)]:w.call(f,receiver=p,count=50000000)
  assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
  for native,crew in [(558,[558,14,517]),(517,[517,-1,-1])]:
   w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,rng)
   target=w.call(0x490b00,native,receiver=w.root);home=struct.unpack_from('<i',person(native),0x98)[0];assert home==21
   decoded=bytes(w.u.mem_read(0x6fb0e68,40000*20));parents=bytes(w.u.mem_read(0x79c2b0,87));cell=next(i for i in range(40000)if (struct.unpack_from('<I',decoded,20*i+4)[0]>>5)&127<87 and parents[(struct.unpack_from('<I',decoded,20*i+4)[0]>>5)&127]==home)
   point=(cell%200,cell//200);unit=w.root+0x169730+0xf4
   w.u.mem_write(unit+8,struct.pack('<i',0));w.u.mem_write(unit+0xc,struct.pack('<3i',*crew));w.u.mem_write(unit+0x18,struct.pack('<H',5000));w.u.mem_write(unit+0x1a,bytes([100]));w.u.mem_write(unit+0x3c,struct.pack('<hh',*point));w.call(0x496250,997,receiver=unit);w.call(0x496280,17000,receiver=unit);assert w.call(0x47a630,unit)==1
   w.u.reg_write(UC_X86_REG_EAX,unit);location=w.call(0x4a7530)
   for n in crew:
    if n>=0:w.call(0x4a0cb0,w.call(0x490b00,n,receiver=w.root),location,receiver=0x799895c,count=50000000)
   before=people();row=dict(nativeId=native,crew=crew,homeNative=home,declaredPoint=point,personBefore=person(native).hex(),peopleBeforeSha=sha(before),unitBefore=bytes(w.u.mem_read(unit,0xf4)).hex())
   stage='release'+str(native);victor=w.root+0x169730;w.u.mem_write(victor+8,struct.pack('<i',0));w.u.mem_write(victor+0xc,struct.pack('<3i',365,116,466));w.u.mem_write(victor+0x3c,struct.pack('<hh',*point));wrapper=d.fixture+0x8000;w.u.mem_write(wrapper,struct.pack('<4I',unit,victor,1,0));row['declaredPlaceDescriptorHex']=bytes(w.u.mem_read(wrapper,16)).hex();w.call(0x4b1950,target,wrapper,1,receiver=0x799895c,count=50000000)
   after=people();row.update(personAfter=person(native).hex(),peopleAfterSha=sha(after),changedPeople=[dict(nativeId=n,beforeHex=before[n*0x190:(n+1)*0x190].hex(),afterHex=after[n*0x190:(n+1)*0x190].hex())for n in range(1100)if before[n*0x190:(n+1)*0x190]!=after[n*0x190:(n+1)*0x190]],unitAfter=bytes(w.u.mem_read(unit,0xf4)).hex(),rngAfter=int.from_bytes(w.u.mem_read(0x8a5d44,4),'little'),phases=[])
   for turn in range(2):
    stage='personnel'+str(native)+'/'+str(turn)
    for f in [0x598630,0x59a4b0,0x599cf0]:w.call(f,count=50000000)
    row['phases'].append(dict(turn=turn,personHex=person(native).hex(),peopleSha=sha(people()),rng=int.from_bytes(w.u.mem_read(0x8a5d44,4),'little')))
   cases.append(row);print('PASS full original same-region release',native,flush=True)
  w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,rng);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
  output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,geography=geo,cases=cases,nativeRngBefore=int.from_bytes(rng,'little'),wholeWorldAndRngRestored=True,limits=['Declared original unit1/crew/coordinates/resources and valid release destination, not ordinary deployment or human capture choice','Complete4b1950 and598630/59a4b0/599cf0 callbacks, no method omissions','Global calendar/full turn controller and GUI/APK not proved'],completeGoal=False),indent=2)+'\n');print('PASS receipt',sha(output.read_bytes()),flush=True)
 except Exception as e:
  output.with_suffix('.failure.json').write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,stage=stage,cases=cases,error=repr(e),ip=hex(w.u.reg_read(UC_X86_REG_EIP)),invalid=w.invalid),indent=2)+'\n');raise
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
